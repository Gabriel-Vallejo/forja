"""Percentiles de fuerza por sexo y franja de peso: sentadilla, banca y peso muerto desde OpenPowerlifting, y
dominada y fondo con lastre desde OpenStreetlifting (datos CC0; ver docs/investigacion/estandares-fuerza.md).

Genera la semilla SQL `R__estandares_fuerza.sql` del backend (RF-30, ADR 0005). Criterios:

- Solo *raw* (sin equipación): es lo más parecido a entrenar en un gimnasio.
- Mayores de 18 (o sin edad indicada) y sexo H o M.
- De cada persona, su mejor marca válida en cada levantamiento, con el peso corporal de ese día.
- Percentiles 1-99 de cada grupo (sexo, levantamiento, franja de peso); los grupos con menos de
  `MUESTRA_MINIMA` personas no se publican.

OpenStreetlifting: el mejor lastre de cada atleta que compitió (kg añadidos, no la carga total). Entran también los
pesos corporales «recovered»: el proyecto los despeja de la puntuación RIS publicada, que es una fórmula exacta del peso
y el total, así que no son una estimación.

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
LASTRE = {"dominada_lastre": "BestPullUpKg", "fondo_lastre": "BestDipsKg"}


def metrica(levantamiento: str) -> str:
    return "kg_lastre" if levantamiento in LASTRE else "kg_1rm"
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


def mejores_lastres(filas: Iterable[dict[str, str]]) -> dict[tuple[str, str, str], tuple[float, float]]:
    """El mejor lastre de cada atleta de OpenStreetlifting que compitió: {(atleta, sexo, levantamiento): (kg, peso)}."""
    mejores: dict[tuple[str, str, str], tuple[float, float]] = {}
    for fila in filas:
        sexo = SEXOS.get(fila.get("Sex", ""))
        peso = _numero(fila.get("BodyweightKg", ""))
        if sexo is None or fila.get("Status") != "competed" or peso is None or peso <= 0:
            continue
        atleta = "|".join(fila.get(c, "") for c in ("FirstName", "LastName", "Disambiguation"))
        for levantamiento, columna in LASTRE.items():
            kg = _numero(fila.get(columna, ""))
            if kg is None or kg <= 0:
                continue
            clave = (atleta, sexo, levantamiento)
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
        "-- Y OpenStreetlifting (https://openstreetlifting.org), datos CC0, "
        f"commit {procedencia.get('osl_commit', '?')}.\n"
        "-- OPL: raw, mayores de 18, mejor marca de cada persona. OSL: mejor lastre de cada atleta que compitió.\n"
        "-- Percentiles 1-99 por sexo y franja de peso.\n"
        "DELETE FROM estandar_fuerza;\n"
    )
    filas = []
    for e in lista:
        g = e.grupo
        hasta = "NULL" if g.hasta_kg is None else f"{g.hasta_kg:.1f}"
        for percentil, kg in enumerate(e.percentiles, start=1):
            filas.append(
                f"('{g.levantamiento}','{metrica(g.levantamiento)}','{g.sexo}',{g.desde_kg:.1f},{hasta},{percentil},"
                f"{kg:.1f},{e.muestra})"
            )
    insert = (
        "INSERT INTO estandar_fuerza "
        "(levantamiento, metrica, sexo, peso_desde_kg, peso_hasta_kg, percentil, kg, muestra) VALUES\n"
        + ",\n".join(filas)
        + ";\n"
    )
    return cabecera + insert


def leer_zip(ruta: Path) -> Iterator[dict[str, str]]:
    with zipfile.ZipFile(ruta) as z:
        nombre = next(n for n in z.namelist() if n.endswith(".csv"))
        with z.open(nombre) as binario:
            yield from csv.DictReader(io.TextIOWrapper(binario, encoding="utf-8", newline=""))


def leer_osl(raiz: Path) -> Iterator[dict[str, str]]:
    for ruta in sorted(raiz.glob("backend/data/competitions/**/entries.csv")):
        with ruta.open(encoding="utf-8", newline="") as f:
            yield from csv.DictReader(f)


def commit_de(repo: Path) -> str:
    """El commit del clon de OpenStreetlifting, leído de .git sin lanzar git."""
    cabeza = (repo / ".git/HEAD").read_text(encoding="utf-8").strip()
    if not cabeza.startswith("ref: "):
        return cabeza
    referencia = repo / ".git" / cabeza.removeprefix("ref: ")
    if referencia.exists():
        return referencia.read_text(encoding="utf-8").strip()
    for linea in (repo / ".git/packed-refs").read_text(encoding="utf-8").splitlines():
        if linea.endswith(" " + cabeza.removeprefix("ref: ")):
            return linea.split()[0]
    return "?"


def main() -> None:  # pragma: no cover
    procedencia = json.loads((CACHE / "procedencia.json").read_text(encoding="utf-8"))
    osl = CACHE / "openstreetlifting"
    procedencia["osl_commit"] = commit_de(osl)
    lista = estandares(mejores_marcas(leer_zip(CACHE / "openpowerlifting-latest.zip")))
    lista += estandares(mejores_lastres(leer_osl(osl)))
    SALIDA.write_text(sql(lista, procedencia), encoding="utf-8")
    for e in lista:
        g = e.grupo
        print(f"{g.levantamiento:12} {g.sexo:7} {g.desde_kg:>5}-{g.hasta_kg or '':<5} n={e.muestra:>7} "
              f"p50={e.percentiles[49]:>6} p90={e.percentiles[89]:>6}")


if __name__ == "__main__":  # pragma: no cover
    main()
