"""Reglas de derivación sobre ejercicios reales de free-exercise-db (registros copiados a mano)."""

from typing import Any

import pytest

from derivar import derivar
from modelo import MATERIALES, MUSCULOS, PATRONES, REGIONES


def registro(
    id_: str,
    category: str = "strength",
    equipment: str | None = "barbell",
    mechanic: str | None = "compound",
    force: str | None = "push",
    primary: tuple[str, ...] = ("quadriceps",),
    secondary: tuple[str, ...] = (),
    level: str = "beginner",
) -> dict[str, Any]:
    return {
        "id": id_,
        "name": id_.replace("_", " "),
        "category": category,
        "equipment": equipment,
        "mechanic": mechanic,
        "force": force,
        "level": level,
        "primaryMuscles": list(primary),
        "secondaryMuscles": list(secondary),
    }


def test_sentadilla_con_barra_es_sentadilla_con_carga_axial() -> None:
    e = derivar(registro("Barbell_Full_Squat", secondary=("glutes", "hamstrings")))
    assert e.patron == "sentadilla"
    assert e.carga_axial
    assert e.implemento == "barra"
    assert e.lateralidad == "bilateral"
    assert e.musculos_primarios == ("cuadriceps",)
    assert e.musculos_secundarios == ("gluteos", "isquiotibiales")


def test_peso_muerto_es_bisagra_con_agarre_pesado_y_carga_axial() -> None:
    e = derivar(registro("Barbell_Deadlift", primary=("lower back",), secondary=("glutes",)))
    assert (e.patron, e.agarre, e.carga_axial) == ("bisagra", "pesado", True)
    assert "columna_lumbar" in e.regiones


def test_prensa_de_piernas_no_carga_la_columna() -> None:
    e = derivar(registro("Leg_Press", equipment="machine"))
    assert e.patron == "sentadilla"
    assert not e.carga_axial
    assert e.implemento == "maquina"


@pytest.mark.parametrize("nombre", ["Dumbbell_Lunges", "Split_Squat_with_Dumbbells"])
def test_zancadas_y_split_son_unilaterales(nombre: str) -> None:
    e = derivar(registro(nombre, equipment="dumbbell"))
    assert (e.patron, e.lateralidad) == ("zancada", "unilateral")


@pytest.mark.parametrize(
    "nombre", ["One-Arm_Dumbbell_Row", "Single-Leg_Leg_Extension", "One_Arm_Lat_Pulldown"]
)
def test_una_mano_o_una_pierna_es_unilateral(nombre: str) -> None:
    assert derivar(registro(nombre, equipment="dumbbell")).lateralidad == "unilateral"


def test_press_militar_es_empuje_vertical_por_encima_de_la_cabeza() -> None:
    e = derivar(registro("Standing_Military_Press", primary=("shoulders",)))
    assert e.patron == "empuje_vertical"
    assert e.por_encima_cabeza
    assert e.carga_axial


def test_press_sentado_con_mancuernas_es_vertical_sin_carga_axial() -> None:
    e = derivar(registro("Seated_Dumbbell_Press", equipment="dumbbell", primary=("shoulders",)))
    assert (e.patron, e.por_encima_cabeza, e.carga_axial) == ("empuje_vertical", True, False)


def test_press_de_banca_es_empuje_horizontal() -> None:
    e = derivar(registro("Barbell_Bench_Press_-_Medium_Grip", primary=("chest",)))
    assert (e.patron, e.por_encima_cabeza, e.carga_axial) == ("empuje_horizontal", False, False)


def test_remo_con_barra_es_traccion_horizontal_con_agarre_ligero() -> None:
    e = derivar(registro("Bent_Over_Barbell_Row", primary=("middle back",), force="pull"))
    assert (e.patron, e.agarre) == ("traccion_horizontal", "ligero")


def test_dominadas_son_traccion_vertical_con_agarre_pesado() -> None:
    e = derivar(registro("Pullups", equipment="body only", primary=("lats",), force="pull"))
    assert (e.patron, e.agarre, e.implemento) == ("traccion_vertical", "pesado", "peso_corporal")


def test_curl_de_biceps_es_aislamiento_de_brazo() -> None:
    e = derivar(registro("Barbell_Curl", mechanic="isolation", primary=("biceps",), force="pull"))
    assert (e.patron, e.mecanica) == ("aislamiento_brazo", "aislamiento")
    assert e.regiones == ("codo",)


def test_extension_de_cuadriceps_es_aislamiento_de_pierna() -> None:
    e = derivar(registro("Leg_Extensions", equipment="machine", mechanic="isolation"))
    assert (e.patron, e.regiones) == ("aislamiento_pierna", ("rodilla",))


def test_pliometria_tiene_impacto_y_no_es_apta_para_fuerza() -> None:
    e = derivar(registro("Front_Box_Jump", category="plyometrics", equipment="other"))
    assert (e.patron, e.impacto, e.apto_fuerza) == ("pliometria", True, False)


def test_estiramiento_es_movilidad_y_no_apta_para_fuerza() -> None:
    e = derivar(registro("Hamstring_Stretch", category="stretching", equipment=None, mechanic=None))
    assert (e.patron, e.apto_fuerza, e.impacto) == ("movilidad", False, False)
    assert e.implemento == "peso_corporal"


def test_paseo_del_granjero_es_acarreo_con_agarre_pesado() -> None:
    e = derivar(registro("Farmers_Walk", category="strongman", equipment="other"))
    assert (e.patron, e.agarre, e.apto_fuerza) == ("acarreo", "pesado", False)


def test_plancha_es_core() -> None:
    e = derivar(
        registro("Plank", equipment="body only", mechanic="isolation", primary=("abdominals",))
    )
    assert e.patron == "core"


def test_cardio_en_cinta_sin_impacto_por_nombre_neutro() -> None:
    e = derivar(registro("Walking_Treadmill", category="cardio", equipment="machine"))
    assert (e.patron, e.impacto, e.apto_fuerza) == ("cardio", False, False)


def test_correr_tiene_impacto() -> None:
    e = derivar(registro("Running_Treadmill", category="cardio", equipment="machine"))
    assert e.impacto


def test_lo_dudoso_queda_en_el_valor_prudente() -> None:
    e = derivar(registro("Barbell_Curl", mechanic="isolation", primary=("biceps",)))
    assert not e.posicion_estirada
    assert e.origen_etiquetado == "derivado"
    assert not e.revisado
    assert e.familia_sustitucion is None
    assert e.nombre_es is None
    assert e.rango_movimiento == "completo"


def test_nivel_se_traduce() -> None:
    assert derivar(registro("X", level="expert")).nivel == "avanzado"
    assert derivar(registro("X", level="intermediate")).nivel == "intermedio"


def test_los_codigos_producidos_son_validos() -> None:
    muestras = [
        registro("Barbell_Full_Squat", secondary=("glutes",)),
        registro("Plank", mechanic="isolation", primary=("abdominals",)),
        registro("Neck_Stretch", category="stretching", equipment="foam roll", primary=("neck",)),
        registro("Jog", category="cardio", equipment=None, mechanic=None),
    ]
    for r in muestras:
        e = derivar(r)
        assert e.patron in PATRONES
        assert e.implemento in MATERIALES
        assert set(e.musculos_primarios + e.musculos_secundarios) <= MUSCULOS
        assert set(e.regiones) <= REGIONES
        assert set(e.materiales) <= MATERIALES


def test_material_desconocido_falla_en_alto() -> None:
    with pytest.raises(ValueError, match="equipamiento"):
        derivar(registro("X", equipment="trineo"))


def test_musculo_desconocido_falla_en_alto() -> None:
    with pytest.raises(ValueError, match="músculo"):
        derivar(registro("X", primary=("orejas",)))


def test_un_musculo_no_es_primario_y_secundario_a_la_vez() -> None:
    e = derivar(registro("X", primary=("chest", "chest"), secondary=("chest", "triceps")))
    assert e.musculos_primarios == ("pecho",)
    assert e.musculos_secundarios == ("triceps",)
