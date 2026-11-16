"""Une lo derivado con lo curado y escribe la semilla SQL del catálogo (repetible de Flyway).

Salida determinista: ejercicios ordenados por `id_externo` y fecha tomada de `fuente.json`, así que
dos ejecuciones seguidas dan exactamente el mismo archivo. Es un `INSERT ... ON DUPLICATE KEY
UPDATE` por `id_externo`: nunca borra ni recrea filas de `ejercicio`, para no romper claves
foráneas futuras (plan, sesiones). Las tablas hijas (músculo, región, material) sí se rehacen,
porque nada las referencia.
"""

import dataclasses
import json
from collections.abc import Iterable, Mapping
from pathlib import Path
from typing import Any

import yaml

import descargar
from derivar import derivar, regiones_de
from modelo import PESO_PRIMARIO, PESO_SECUNDARIO, Ejercicio

AQUI = Path(__file__).parent
CURADO = AQUI / "curado.yaml"
SALIDA = AQUI.parent.parent / "backend/src/main/resources/db/migration/R__catalogo_semilla.sql"

OBLIGATORIAS = ("nombre_es", "patron", "mecanica", "familia_sustitucion", "implemento",
                "musculos_primarios")  # fmt: skip
# Claves del YAML que se copian tal cual al ejercicio.
_ESCALARES = ("nombre_es", "patron", "mecanica", "familia_sustitucion", "implemento", "nivel",
              "carga_axial", "impacto", "agarre", "lateralidad", "por_encima_cabeza",
              "posicion_estirada", "rango_movimiento", "apto_fuerza", "revisado")  # fmt: skip
_LISTAS = ("musculos_primarios", "musculos_secundarios", "regiones", "materiales")
PERMITIDAS = frozenset({"id", *_ESCALARES, *_LISTAS})

_COLUMNAS = (
    "id_externo", "nombre_original", "nombre_es", "patron_id", "mecanica", "nivel", "categoria",
    "carga_axial", "impacto", "agarre", "lateralidad", "por_encima_cabeza", "posicion_estirada",
    "familia_sustitucion", "rango_movimiento", "implemento_id", "apto_fuerza",
    "origen_etiquetado", "revisado", "fuente", "licencia", "autor", "version_fuente",
)  # fmt: skip
# El id_externo es la clave del upsert: no se actualiza.
_ACTUALIZABLES = _COLUMNAS[1:]


# --- curado -----------------------------------------------------------------------------------


def validar_entrada(entrada: Mapping[str, Any]) -> None:
    """Falla en alto si falta un campo obligatorio, sobra una clave o no hay músculo primario."""
    desconocidas = set(entrada) - PERMITIDAS
    if desconocidas:
        raise ValueError(f"{entrada.get('id')}: clave desconocida {sorted(desconocidas)}")
    for clave in OBLIGATORIAS:
        if clave not in entrada:
            raise ValueError(f"{entrada.get('id')}: falta {clave}")
    if not entrada["musculos_primarios"]:
        raise ValueError(f"{entrada.get('id')}: necesita al menos un músculo primario")


def cargar_curado(ruta: Path = CURADO) -> tuple[dict[str, dict[str, Any]], list[str]]:
    """Lee curado.yaml: entradas por id (con los valores por defecto) y la lista «faltan»."""
    datos = yaml.safe_load(ruta.read_text(encoding="utf-8"))
    por_defecto: dict[str, Any] = datos["por_defecto"]
    entradas: dict[str, dict[str, Any]] = {}
    for crudo in datos["ejercicios"]:
        validar_entrada(crudo)
        if crudo["id"] in entradas:
            raise ValueError(f"{crudo['id']}: repetido en curado.yaml")
        entradas[crudo["id"]] = {**por_defecto, **crudo}
    return entradas, list(datos.get("faltan", []))


def fusionar(derivado: Ejercicio, curado: Mapping[str, Any]) -> Ejercicio:
    """Aplica lo fijado a mano sobre lo derivado. Lo curado manda; las regiones se recalculan."""
    validar_entrada(curado)
    cambios: dict[str, Any] = {c: curado[c] for c in _ESCALARES if c in curado}
    for clave in ("musculos_primarios", "musculos_secundarios"):
        cambios[clave] = tuple(curado.get(clave, ()))
    carga_axial = cambios.get("carga_axial", derivado.carga_axial)
    patron = cambios["patron"]
    implemento = cambios["implemento"]
    cambios["regiones"] = tuple(
        curado.get("regiones") or regiones_de(patron, carga_axial, cambios["musculos_primarios"])
    )
    cambios["materiales"] = tuple(curado.get("materiales") or (implemento,))
    cambios["origen_etiquetado"] = "curado"
    cambios.setdefault("revisado", False)
    return dataclasses.replace(derivado, **cambios)


def catalogo(
    registros: Iterable[Mapping[str, Any]], curados: Mapping[str, Mapping[str, Any]]
) -> list[Ejercicio]:
    """Todos los ejercicios etiquetados, ordenados por id. Un id curado inexistente es un error."""
    por_id = {r["id"]: derivar(r) for r in registros}
    sobrantes = sorted(set(curados) - set(por_id))
    if sobrantes:
        raise ValueError(f"ids curados que no existen en la fuente: {sobrantes}")
    for id_, entrada in curados.items():
        por_id[id_] = fusionar(por_id[id_], entrada)
    return [por_id[id_] for id_ in sorted(por_id)]


# --- SQL --------------------------------------------------------------------------------------


def _lit(valor: object) -> str:
    """Literal SQL de MySQL (con barra invertida como escape, el modo por defecto)."""
    if valor is None:
        return "NULL"
    if isinstance(valor, bool):
        return "1" if valor else "0"
    texto = str(valor).replace("\\", "\\\\").replace("'", "''")
    return f"'{texto}'"


def _fila_ejercicio(e: Ejercicio, fuente: Mapping[str, Any]) -> str:
    valores = (
        _lit(e.id_externo), _lit(e.nombre_original), _lit(e.nombre_es),
        f"(SELECT id FROM patron_movimiento WHERE codigo = {_lit(e.patron)})",
        _lit(e.mecanica), _lit(e.nivel), _lit(e.categoria), _lit(e.carga_axial), _lit(e.impacto),
        _lit(e.agarre), _lit(e.lateralidad), _lit(e.por_encima_cabeza), _lit(e.posicion_estirada),
        _lit(e.familia_sustitucion), _lit(e.rango_movimiento),
        f"(SELECT id FROM material WHERE codigo = {_lit(e.implemento)})",
        _lit(e.apto_fuerza), _lit(e.origen_etiquetado), _lit(e.revisado), _lit(fuente["fuente"]),
        _lit(fuente["licencia"]), "NULL", _lit(fuente["commit"]),
    )  # fmt: skip
    return "(" + ", ".join(valores) + ")"


def _sql_ejercicios(ejercicios: list[Ejercicio], fuente: Mapping[str, Any]) -> str:
    filas = ",\n".join(_fila_ejercicio(e, fuente) for e in ejercicios)
    actualizar = ",\n    ".join(f"{c} = nuevo.{c}" for c in _ACTUALIZABLES)
    return (
        f"INSERT INTO ejercicio ({', '.join(_COLUMNAS)})\nVALUES\n{filas}\nAS nuevo\n"
        f"ON DUPLICATE KEY UPDATE\n    {actualizar};\n"
    )


def _sql_hija(
    tabla: str,
    destino: str,
    columnas_v: str,
    filas: list[str],
    seleccion: str,
    cruce: str,
    fuente: str,
) -> str:
    """Rehace las filas hijas de los ejercicios de esta fuente (nada las referencia todavía).

    `destino` son las columnas de la tabla; `seleccion` y `cruce` resuelven los códigos a ids.
    """
    return (
        f"DELETE h FROM {tabla} h JOIN ejercicio e ON e.id = h.ejercicio_id "
        f"WHERE e.fuente = {fuente};\n"
        f"INSERT INTO {tabla} ({destino})\nSELECT {seleccion}\n"
        f"FROM (VALUES\n  {',\n  '.join(filas)}\n) AS v({columnas_v})\n"
        f"JOIN ejercicio e ON e.id_externo = v.id_externo\n{cruce};\n"
    )


def _sql_hijas(ejercicios: list[Ejercicio], fuente: Mapping[str, Any]) -> str:
    musculos = [
        f"ROW({_lit(e.id_externo)}, {_lit(m)}, {_lit(rol)}, {peso})"
        for e in ejercicios
        for rol, peso, lista in (
            ("primario", PESO_PRIMARIO, e.musculos_primarios),
            ("secundario", PESO_SECUNDARIO, e.musculos_secundarios),
        )
        for m in lista
    ]
    regiones = [f"ROW({_lit(e.id_externo)}, {_lit(r)})" for e in ejercicios for r in e.regiones]
    materiales = [f"ROW({_lit(e.id_externo)}, {_lit(m)})" for e in ejercicios for m in e.materiales]
    origen = _lit(fuente["fuente"])
    return "\n".join((
        _sql_hija("ejercicio_musculo", "ejercicio_id, musculo_id, rol, peso",
                  "id_externo, musculo, rol, peso", musculos, "e.id, m.id, v.rol, v.peso",
                  "JOIN musculo m ON m.codigo = v.musculo", origen),
        _sql_hija("ejercicio_region", "ejercicio_id, region_id", "id_externo, region",
                  regiones, "e.id, r.id", "JOIN region r ON r.codigo = v.region", origen),
        _sql_hija("ejercicio_material", "ejercicio_id, material_id", "id_externo, material",
                  materiales, "e.id, m.id", "JOIN material m ON m.codigo = v.material", origen),
    ))  # fmt: skip


def sql(ejercicios: Iterable[Ejercicio], fuente: Mapping[str, Any]) -> str:
    """El contenido completo de R__catalogo_semilla.sql (ordenado, sin marcas de tiempo)."""
    ordenados = sorted(ejercicios, key=lambda e: e.id_externo)
    cabecera = (
        "-- Generado por database/catalogo/generar.py; no editar a mano.\n"
        f"-- Fuente: {fuente['fuente']} @ {fuente['commit']} ({fuente['licencia']}), "
        f"descargada el {fuente['fecha_descarga']}.\n"
        "-- Migración repetible: Flyway la vuelve a ejecutar cuando cambia su contenido.\n\n"
    )
    return cabecera + _sql_ejercicios(ordenados, fuente) + "\n" + _sql_hijas(ordenados, fuente)


def main(salida: Path = SALIDA) -> None:
    fuente = json.loads(descargar.FUENTE.read_text(encoding="utf-8"))
    registros = json.loads(descargar.asegurar().read_text(encoding="utf-8"))
    curados, faltan = cargar_curado()
    ejercicios = catalogo(registros, curados)
    salida.parent.mkdir(parents=True, exist_ok=True)
    salida.write_text(sql(ejercicios, fuente), encoding="utf-8", newline="\n")
    print(f"{len(ejercicios)} ejercicios ({len(curados)} curados) -> {salida}")
    for pendiente in faltan:
        print(f"falta en la fuente: {pendiente}")


if __name__ == "__main__":
    main()
