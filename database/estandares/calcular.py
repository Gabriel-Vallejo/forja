"""Percentiles de sentadilla, banca y peso muerto por sexo y franja de peso, desde el CSV de OpenPowerlifting.

Genera la semilla SQL `R__estandares_fuerza.sql` del backend (RF-30, ADR 0005). Criterios:

- Solo *raw* (sin equipación): es lo más parecido a entrenar en un gimnasio.
- Mayores de 18 (o sin edad indicada) y sexo H o M.
- De cada persona, su mejor marca válida en cada levantamiento, con el peso corporal de ese día.
- Percentiles 1-99 de cada grupo (sexo, levantamiento, franja de peso); los grupos con menos de
  `MUESTRA_MINIMA` personas no se publican.

Sesgo: son levantadores de competición, así que los percentiles quedan por encima de la población que entrena.
La app lo dice junto al rango (ADR 0005 §6).
"""

from __future__ import annotations

import csv
import io
import json
import statistics
import zipfile
from collections.abc import Iterable, Iterator
from dataclasses import dataclass
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SALIDA = RAIZ / "backend/src/main/resources/db/migration/R__estandares_fuerza.sql"
CACHE = Path(__file__).parent / ".cache"
EDAD_MINIMA = 18
MUESTRA_MINIMA = 200

LEVANTAMIENTOS = {"sentadilla": "Best3SquatKg", "banca": "Best3BenchKg", "peso_muerto": "Best3DeadliftKg"}
SEXOS = {"M": "hombre", "F": "mujer"}
# Límites inferiores de cada franja de peso corporal (kg); la última no tiene techo.
FRANJAS: dict[str, tuple[float, ...]] = {
    "hombre": (0, 60, 70, 80, 90, 100, 110, 125),
    "mujer": (0, 50, 60, 70, 80, 90, 100),
}


@dataclass(frozen=True)
class Grupo:
    levantamiento: str
    sexo: str
    desde_kg: float
    hasta_kg: float | None


@dataclass(frozen=True)
class Estandar:
    grupo: Grupo
    muestra: int
    percentiles: tuple[float, ...]  # kg en los percentiles 1..99


def _numero(texto: str) -> float | None:
    try:
        return float(texto) if texto else None
    except ValueError:
        return None


def franja(sexo: str, peso: float) -> tuple[float, float | None]:
    limites = FRANJAS[sexo]
    for i in range(len(limites) - 1, -1, -1):
        if peso >= limites[i]:
            return limites[i], limites[i + 1] if i + 1 < len(limites) else None
    return limites[0], limites[1]


def mejores_marcas(filas: Iterable[dict[str, str]]) -> dict[tuple[str, str, str], tuple[float, float]]:
    """La mejor marca de cada persona en cada levantamiento: {(nombre, sexo, levantamiento): (kg, peso corporal)}."""
    mejores: dict[tuple[str, str, str], tuple[float, float]] = {}
    for fila in filas:
        sexo = SEXOS.get(fila.get("Sex", ""))
        if sexo is None or fila.get("Equipment") != "Raw":
            continue
        edad = _numero(fila.get("Age", ""))
        peso = _numero(fila.get("BodyweightKg", ""))
        if (edad is not None and edad < EDAD_MINIMA) or peso is None or peso <= 0:
            continue
        for levantamiento, columna in LEVANTAMIENTOS.items():
            kg = _numero(fila.get(columna, ""))
            if kg is None or kg <= 0:  # negativo = nulo en OpenPowerlifting
                continue
            clave = (fila["Name"], sexo, levantamiento)
            previo = mejores.get(clave)
            if previo is None or kg > previo[0]:
                mejores[clave] = (kg, peso)
    return mejores


def estandares(mejores: dict[tuple[str, str, str], tuple[float, float]]) -> list[Estandar]:
    grupos: dict[Grupo, list[float]] = {}
    for (_, sexo, levantamiento), (kg, peso) in mejores.items():
        desde, hasta = franja(sexo, peso)
        grupos.setdefault(Grupo(levantamiento, sexo, desde, hasta), []).append(kg)
    salida = []
    for grupo in sorted(grupos, key=lambda g: (g.levantamiento, g.sexo, g.desde_kg)):
        valores = grupos[grupo]
        if len(valores) < MUESTRA_MINIMA:
            continue
        cortes = statistics.quantiles(valores, n=100, method="inclusive")
        salida.append(Estandar(grupo, len(valores), tuple(round(c, 1) for c in cortes)))
    return salida


def sql(lista: list[Estandar], procedencia: dict[str, str]) -> str:
    cabecera = (
        "-- Generado por database/estandares/calcular.py: no editar a mano.\n"
        "-- Fuente: OpenPowerlifting (https://www.openpowerlifting.org), datos de dominio público.\n"
        f"-- Volcado: {procedencia.get('url', '?')} ({procedencia.get('last_modified', '?')}), "
        f"sha256 {procedencia.get('sha256', '?')}.\n"
        "-- Raw, mayores de 18, mejor marca de cada persona; percentiles 1-99 por sexo y franja de peso.\n"
        "DELETE FROM estandar_fuerza;\n"
    )
    filas = []
    for e in lista:
        g = e.grupo
        hasta = "NULL" if g.hasta_kg is None else f"{g.hasta_kg:.1f}"
        for percentil, kg in enumerate(e.percentiles, start=1):
            filas.append(
                f"('{g.levantamiento}','{g.sexo}',{g.desde_kg:.1f},{hasta},{percentil},{kg:.1f},{e.muestra})"
            )
    insert = (
        "INSERT INTO estandar_fuerza (levantamiento, sexo, peso_desde_kg, peso_hasta_kg, percentil, kg, muestra) "
        "VALUES\n" + ",\n".join(filas) + ";\n"
    )
    return cabecera + insert


def leer_zip(ruta: Path) -> Iterator[dict[str, str]]:
    with zipfile.ZipFile(ruta) as z:
        nombre = next(n for n in z.namelist() if n.endswith(".csv"))
        with z.open(nombre) as binario:
            yield from csv.DictReader(io.TextIOWrapper(binario, encoding="utf-8", newline=""))


def main() -> None:  # pragma: no cover
    procedencia = json.loads((CACHE / "procedencia.json").read_text(encoding="utf-8"))
    lista = estandares(mejores_marcas(leer_zip(CACHE / "openpowerlifting-latest.zip")))
    SALIDA.write_text(sql(lista, procedencia), encoding="utf-8")
    for e in lista:
        g = e.grupo
        print(f"{g.levantamiento:12} {g.sexo:7} {g.desde_kg:>5}-{g.hasta_kg or '':<5} n={e.muestra:>7} "
              f"p50={e.percentiles[49]:>6} p90={e.percentiles[89]:>6}")


if __name__ == "__main__":  # pragma: no cover
    main()
