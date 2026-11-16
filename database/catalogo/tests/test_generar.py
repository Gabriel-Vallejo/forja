import dataclasses
import re
from collections import defaultdict
from pathlib import Path
from typing import Any

import pytest

import generar
from derivar import derivar
from modelo import Ejercicio

CURADO = Path(__file__).parent.parent / "curado.yaml"
FUENTE = {
    "fuente": "free-exercise-db",
    "commit": "abc123",
    "licencia": "Unlicense",
    "fecha_descarga": "2026-11-16",
}


def registro(id_: str = "Barbell_Squat", nombre: str = "Barbell Squat") -> dict[str, Any]:
    return {
        "id": id_, "name": nombre, "category": "strength", "equipment": "barbell",
        "mechanic": "compound", "force": "push", "level": "beginner",
        "primaryMuscles": ["quadriceps"], "secondaryMuscles": ["glutes"],
    }  # fmt: skip


def entrada(**extra: Any) -> dict[str, Any]:
    base: dict[str, Any] = {
        "id": "Barbell_Squat", "nombre_es": "Sentadilla con barra", "patron": "sentadilla",
        "mecanica": "compuesto", "familia_sustitucion": "sentadilla", "implemento": "barra",
        "musculos_primarios": ["cuadriceps", "gluteos"], "musculos_secundarios": ["lumbares"],
    }  # fmt: skip
    return {**base, **extra}


# --- fusión -----------------------------------------------------------------------------------


def test_lo_curado_manda_sobre_lo_derivado() -> None:
    derivado = derivar(registro("Goblet_Squat", "Goblet Squat"))
    curado = generar.fusionar(derivado, entrada(carga_axial=False, agarre="ligero"))
    assert curado.origen_etiquetado == "curado"
    assert curado.revisado is False
    assert curado.nombre_es == "Sentadilla con barra"
    assert curado.familia_sustitucion == "sentadilla"
    assert curado.agarre == "ligero"
    assert curado.carga_axial is False
    assert curado.musculos_primarios == ("cuadriceps", "gluteos")
    # lo que no se fija a mano se queda como lo derivó la regla
    assert curado.nombre_original == "Goblet Squat"
    assert curado.categoria == "strength"


def test_las_regiones_curadas_se_recalculan_con_el_patron_curado() -> None:
    curado = generar.fusionar(derivar(registro()), entrada(carga_axial=True))
    assert curado.regiones == ("cadera", "columna_lumbar", "rodilla", "tobillo")


def test_las_regiones_se_pueden_fijar_a_mano() -> None:
    curado = generar.fusionar(derivar(registro()), entrada(regiones=["codo"]))
    assert curado.regiones == ("codo",)


def test_el_implemento_curado_entra_en_los_materiales() -> None:
    curado = generar.fusionar(derivar(registro()), entrada(implemento="polea"))
    assert curado.materiales == ("polea",)


# --- validación del YAML ------------------------------------------------------------------------


def test_clave_desconocida_en_el_curado_falla() -> None:
    with pytest.raises(ValueError, match="clave"):
        generar.validar_entrada(entrada(patrn="x"))


@pytest.mark.parametrize(
    "falta",
    ["nombre_es", "patron", "mecanica", "familia_sustitucion", "implemento", "musculos_primarios"],
)
def test_obligatorios_del_curado(falta: str) -> None:
    e = entrada()
    del e[falta]
    with pytest.raises(ValueError, match=falta):
        generar.validar_entrada(e)


def test_curado_sin_musculo_primario_falla() -> None:
    with pytest.raises(ValueError, match="primario"):
        generar.validar_entrada(entrada(musculos_primarios=[]))


def test_modelo_rechaza_codigos_invalidos() -> None:
    with pytest.raises(ValueError, match="patron"):
        dataclasses.replace(derivar(registro()), patron="volar")


def test_modelo_rechaza_musculo_primario_y_secundario() -> None:
    with pytest.raises(ValueError, match="a la vez"):
        dataclasses.replace(derivar(registro()), musculos_secundarios=("cuadriceps",))


# --- catálogo ----------------------------------------------------------------------------------


def test_catalogo_ordena_por_id_y_aplica_lo_curado() -> None:
    regs = [registro("B_Ej", "B Ej"), registro("A_Ej", "A Ej")]
    cat = generar.catalogo(regs, {"B_Ej": entrada(id="B_Ej")})
    assert [e.id_externo for e in cat] == ["A_Ej", "B_Ej"]
    assert [e.origen_etiquetado for e in cat] == ["derivado", "curado"]


def test_id_curado_inexistente_falla() -> None:
    with pytest.raises(ValueError, match="Fantasma"):
        generar.catalogo([registro()], {"Fantasma": entrada(id="Fantasma")})


# --- SQL ---------------------------------------------------------------------------------------


def _ejercicios() -> list[Ejercicio]:
    regs = [registro("A_Ej", "A Ej"), registro("B_Ej", "Farmer's Walk \\ ; test")]
    return generar.catalogo(regs, {"B_Ej": entrada(id="B_Ej")})


def test_sql_es_determinista_y_no_depende_del_orden_de_entrada() -> None:
    uno = generar.sql(_ejercicios(), FUENTE)
    otro = generar.sql(list(reversed(_ejercicios())), FUENTE)
    assert uno == otro == generar.sql(_ejercicios(), FUENTE)


def test_sql_escapa_comillas_y_barras() -> None:
    texto = generar.sql(_ejercicios(), FUENTE)
    assert "'Farmer''s Walk \\\\ ; test'" in texto


def test_sql_es_un_upsert_por_id_externo_y_guarda_la_procedencia() -> None:
    texto = generar.sql(_ejercicios(), FUENTE)
    assert "ON DUPLICATE KEY UPDATE" in texto
    assert "'free-exercise-db', 'Unlicense', NULL, 'abc123'" in texto
    assert "DELETE h FROM ejercicio_musculo" in texto


def test_sql_pesos_son_uno_y_medio() -> None:
    texto = generar.sql(_ejercicios(), FUENTE)
    pesos = re.findall(r"ROW\('[^']+', '[a-z_]+', '(primario|secundario)', ([0-9.]+)\)", texto)
    assert pesos
    assert {(rol, peso) for rol, peso in pesos} == {("primario", "1.0"), ("secundario", "0.5")}


# --- con los datos reales ------------------------------------------------------------------------


@pytest.fixture(scope="module")
def curado() -> tuple[dict[str, dict[str, Any]], list[str]]:
    return generar.cargar_curado(CURADO)


def test_no_hay_ids_curados_inexistentes(
    registros: list[dict[str, Any]], curado: tuple[dict[str, dict[str, Any]], list[str]]
) -> None:
    existentes = {r["id"] for r in registros}
    assert set(curado[0]) <= existentes


def test_hay_unos_sesenta_curados(curado: tuple[dict[str, dict[str, Any]], list[str]]) -> None:
    assert 55 <= len(curado[0]) <= 70


def test_todo_curado_tiene_patron_familia_implemento_y_musculo_primario(
    registros: list[dict[str, Any]], curado: tuple[dict[str, dict[str, Any]], list[str]]
) -> None:
    cat = {e.id_externo: e for e in generar.catalogo(registros, curado[0])}
    for id_ in curado[0]:
        e = cat[id_]
        assert e.patron and e.familia_sustitucion and e.implemento
        assert e.musculos_primarios
        assert e.origen_etiquetado == "curado"
        assert e.revisado is False
        assert e.nombre_es


def test_cada_familia_tiene_al_menos_dos_variantes_y_varios_implementos(
    curado: tuple[dict[str, dict[str, Any]], list[str]],
) -> None:
    familias: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for entrada_ in curado[0].values():
        familias[entrada_["familia_sustitucion"]].append(entrada_)
    for nombre, variantes in familias.items():
        assert len(variantes) >= 2, nombre
    con_varios_implementos = [
        f for f, v in familias.items() if len({x["implemento"] for x in v}) > 1
    ]
    assert len(con_varios_implementos) >= len(familias) * 0.7


def test_posicion_estirada_solo_con_fuente_en_el_yaml() -> None:
    lineas = [
        x for x in CURADO.read_text(encoding="utf-8").splitlines() if "posicion_estirada: true" in x
    ]
    assert len(lineas) >= 8
    for linea in lineas:
        assert "fuente:" in linea, linea


def test_las_familias_de_la_escalera_estan_definidas(
    curado: tuple[dict[str, dict[str, Any]], list[str]],
) -> None:
    familias = {e["familia_sustitucion"] for e in curado[0].values()}
    assert {"sentadilla", "press_horizontal", "press_vertical", "traccion_vertical",
            "traccion_horizontal", "peso_muerto", "curl_femoral", "gemelo"} <= familias  # fmt: skip


def test_lista_faltan_no_esta_vacia(curado: tuple[dict[str, dict[str, Any]], list[str]]) -> None:
    assert any("búlgara" in f for f in curado[1])


def test_catalogo_completo_genera_876_y_sql_determinista(
    registros: list[dict[str, Any]], curado: tuple[dict[str, dict[str, Any]], list[str]]
) -> None:
    cat = generar.catalogo(registros, curado[0])
    assert len(cat) == len(registros) == 876
    assert generar.sql(cat, FUENTE) == generar.sql(list(reversed(cat)), FUENTE)


def test_main_escribe_el_archivo(tmp_path: Path) -> None:
    salida = tmp_path / "R__catalogo_semilla.sql"
    generar.main(salida)
    primero = salida.read_bytes()
    generar.main(salida)
    assert salida.read_bytes() == primero
    assert primero.startswith(b"-- Generado por")
