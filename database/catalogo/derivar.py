"""Reglas puras que derivan los atributos de un ejercicio desde un registro de free-exercise-db.

Son heurísticas: lo que no se puede deducir con confianza queda en el valor más prudente y el
ejercicio sale con `origen_etiquetado = derivado` y `revisado = False`. Lo fijado a mano en
`curado.yaml` manda sobre todo esto (ver `generar.py`).
"""

import re
from collections.abc import Mapping
from typing import Any

from modelo import Ejercicio

CATEGORIAS_FUERZA = frozenset({"strength", "powerlifting"})

_MUSCULOS = {
    "abdominals": "abdominales", "hamstrings": "isquiotibiales", "adductors": "aductores",
    "quadriceps": "cuadriceps", "biceps": "biceps", "shoulders": "hombros", "chest": "pecho",
    "middle back": "espalda_media", "calves": "gemelos", "glutes": "gluteos",
    "lower back": "lumbares", "lats": "dorsales", "triceps": "triceps", "traps": "trapecio",
    "forearms": "antebrazos", "neck": "cuello", "abductors": "abductores",
}  # fmt: skip

_MATERIALES = {
    "barbell": "barra", "dumbbell": "mancuerna", "machine": "maquina", "cable": "polea",
    "body only": "peso_corporal", "kettlebells": "kettlebell", "bands": "banda",
    "e-z curl bar": "barra_ez", "medicine ball": "balon_medicinal",
    "exercise ball": "fitball", "foam roll": "rodillo", "other": "otro",
    None: "peso_corporal",  # sin equipamiento declarado: se hace con el propio cuerpo
}  # fmt: skip

_NIVELES = {"beginner": "principiante", "intermediate": "intermedio", "expert": "avanzado"}
_MECANICAS = {"compound": "compuesto", "isolation": "aislamiento", None: None}

# Músculos cuyo aislamiento se etiqueta «aislamiento_brazo» (también hombro, trapecio y cuello).
_MUSCULOS_AISLAMIENTO_BRAZO = frozenset(
    {"biceps", "triceps", "forearms", "neck", "shoulders", "traps"}
)

_ACARREO = ("farmers", "carry", "yoke")
_TRACCION_HORIZONTAL_ESPECIAL = ("face pull", "rear delt", "reverse fly", "reverse flye")
_EMPUJE_VERTICAL = ("jerk", "push press", "military", "shoulder press", "overhead press",
                    "handstand", "arnold")  # fmt: skip
_ZANCADA = ("lunge", "split squat", "step up")
_SENTADILLA = ("squat", "leg press")
_BISAGRA = ("deadlift", "good morning", "hip thrust", "bridge", "hyperextension", "pull through",
            "swing", "clean", "snatch", "rack pull", "glute ham", "hip extension")  # fmt: skip
_TRACCION_VERTICAL = ("pulldown", "pull down", "pull up", "pullup", "chin up", "chins",
                      "muscle up", "rope climb")  # fmt: skip
_TRACCION_HORIZONTAL = ("row",)
_EMPUJE_HORIZONTAL = ("bench press", "push up", "pushup", "fly", "flye", "dip", "press",
                      "crossover", "cross over")  # fmt: skip

_POR_ENCIMA_CABEZA = ("overhead", "military", "shoulder press", "push press", "jerk", "handstand",
                      "snatch", "arnold", "above head", "behind neck")  # fmt: skip
_IMPACTO = ("jump", "hop", "bound", "sprint", "skip", "running", "jogging", "leap")
_UNILATERAL = ("one arm", "single arm", "one leg", "single leg", "one legged", "lunge",
               "split squat", "alternat", "step up", "pistol")  # fmt: skip
_AGARRE_PESADO = ("deadlift", "farmers", "carry", "yoke", "shrug", "pull up", "pullup", "chin up",
                  "chins", "clean", "snatch", "rack pull", "hang", "muscle up",
                  "rope climb")  # fmt: skip
_AGARRE_LIGERO = ("row", "pulldown", "pull down", "curl", "face pull", "pull through", "upright")
_RANGO_REDUCIDO = ("partial", "board press", "pin press", "rack pull", "isometric")

_PATRONES_AXIALES = frozenset({"sentadilla", "bisagra", "zancada"})
_IMPLEMENTOS_AXIALES = frozenset({"barra", "barra_ez", "mancuerna", "kettlebell", "otro"})
_SIN_CARGA_AXIAL = ("thrust", "bridge", "hyperextension", "pull through", "glute ham")
_SIN_CARGA_AXIAL_ENCIMA = ("seated", "lying", "incline")

_REGIONES_PATRON = {
    "sentadilla": ("cadera", "rodilla", "tobillo"),
    "zancada": ("cadera", "rodilla", "tobillo"),
    "pliometria": ("cadera", "rodilla", "tobillo"),
    "cardio": ("cadera", "rodilla", "tobillo"),
    "bisagra": ("cadera", "columna_lumbar"),
    "empuje_horizontal": ("codo", "hombro", "muneca"),
    "empuje_vertical": ("codo", "hombro", "muneca"),
    "traccion_horizontal": ("codo", "hombro"),
    "traccion_vertical": ("codo", "hombro"),
    "core": ("columna_lumbar",),
    "acarreo": ("columna_lumbar", "hombro", "muneca"),
    "movilidad": (),
}  # fmt: skip
_REGIONES_AISLAMIENTO = {
    "biceps": ("codo",), "triceps": ("codo",), "antebrazos": ("muneca",), "hombros": ("hombro",),
    "trapecio": ("columna_cervical", "hombro"), "cuello": ("columna_cervical",),
    "cuadriceps": ("rodilla",), "isquiotibiales": ("rodilla",), "gemelos": ("tobillo",),
    "aductores": ("cadera",), "abductores": ("cadera",),
}  # fmt: skip


def _texto(nombre: str) -> str:
    """Minúsculas y sin signos: «Pull-Up» -> «pull up»."""
    return re.sub(r"[^a-z0-9]+", " ", nombre.lower()).strip()


def _tiene(texto: str, claves: tuple[str, ...]) -> bool:
    """Alguna clave aparece al principio de una palabra (así «row» no casa con «narrow»)."""
    return any(re.search(r"\b" + re.escape(clave), texto) for clave in claves)


def _traducir(tabla: Mapping[Any, Any], clave: Any, que: str) -> Any:
    if clave not in tabla:
        raise ValueError(f"{que} desconocido: {clave!r}")
    return tabla[clave]


def _musculos(nombres: list[str]) -> tuple[str, ...]:
    """Códigos de músculo sin repetidos y en el orden de la fuente."""
    return tuple(dict.fromkeys(_traducir(_MUSCULOS, n, "músculo") for n in nombres))


def _es_empuje_vertical(texto: str, primarios: list[str], mecanica: str | None) -> bool:
    if _tiene(texto, _EMPUJE_VERTICAL):
        return True
    return primarios[:1] == ["shoulders"] and mecanica == "compound" and _tiene(texto, ("press",))


def _patron_por_musculo(primario: str, mecanica: str | None) -> str:
    """Último recurso cuando el nombre no dice nada: manda el músculo principal."""
    aislado = mecanica == "isolation"
    por_musculo = {
        "lats": "traccion_vertical", "traps": "traccion_vertical",
        "middle back": "traccion_horizontal", "chest": "empuje_horizontal",
        "glutes": "bisagra", "lower back": "bisagra",
        "adductors": "aislamiento_pierna", "abductors": "aislamiento_pierna",
    }  # fmt: skip
    if primario in por_musculo:
        return por_musculo[primario]
    if primario in ("quadriceps", "hamstrings"):
        if aislado:
            return "aislamiento_pierna"
        return "sentadilla" if primario == "quadriceps" else "bisagra"
    if primario == "shoulders":
        return "aislamiento_brazo" if aislado else "empuje_vertical"
    return "aislamiento_brazo"


def _patron(registro: Mapping[str, Any], texto: str) -> str:
    categoria = registro["category"]
    primarios: list[str] = registro["primaryMuscles"]
    mecanica = registro.get("mechanic")
    primario = primarios[0] if primarios else ""
    if categoria == "stretching":
        return "movilidad"
    if categoria == "plyometrics":
        return "pliometria"
    if categoria == "cardio":
        return "cardio"
    if primario == "calves":
        return "aislamiento_pierna"
    if _tiene(texto, _ACARREO):
        return "acarreo"
    if _tiene(texto, _TRACCION_HORIZONTAL_ESPECIAL):
        return "traccion_horizontal"
    if mecanica == "isolation" and primario in _MUSCULOS_AISLAMIENTO_BRAZO:
        return "aislamiento_brazo"
    if primario == "abdominals":
        return "core"
    reglas = (
        (_es_empuje_vertical(texto, primarios, mecanica), "empuje_vertical"),
        (_tiene(texto, _ZANCADA), "zancada"),
        (_tiene(texto, _SENTADILLA), "sentadilla"),
        (_tiene(texto, _BISAGRA), "bisagra"),
        (_tiene(texto, _TRACCION_VERTICAL), "traccion_vertical"),
        (_tiene(texto, _TRACCION_HORIZONTAL), "traccion_horizontal"),
        (_tiene(texto, _EMPUJE_HORIZONTAL), "empuje_horizontal"),
    )
    for coincide, patron in reglas:
        if coincide:
            return patron
    return _patron_por_musculo(primario, mecanica)


def _carga_axial(texto: str, patron: str, implemento: str, por_encima_cabeza: bool) -> bool:
    carga = implemento in _IMPLEMENTOS_AXIALES or "smith" in texto
    if patron in _PATRONES_AXIALES:
        return carga and not _tiene(texto, _SIN_CARGA_AXIAL)
    return carga and por_encima_cabeza and not _tiene(texto, _SIN_CARGA_AXIAL_ENCIMA)


def regiones_de(
    patron: str, carga_axial: bool, musculos_primarios: tuple[str, ...]
) -> tuple[str, ...]:
    """Regiones que carga el ejercicio, según su patrón (y su músculo, si es de aislamiento)."""
    if patron in ("aislamiento_brazo", "aislamiento_pierna"):
        regiones: tuple[str, ...] = ()
        for musculo in musculos_primarios[:1]:
            regiones = _REGIONES_AISLAMIENTO.get(musculo, ())
    else:
        regiones = _REGIONES_PATRON[patron]
    if carga_axial:
        regiones = (*regiones, "columna_lumbar")
    return tuple(sorted(set(regiones)))


def derivar(registro: Mapping[str, Any]) -> Ejercicio:
    """Etiqueta un registro de free-exercise-db con reglas automáticas."""
    texto = _texto(registro["name"])
    categoria: str = registro["category"]
    implemento: str = _traducir(_MATERIALES, registro.get("equipment"), "equipamiento")
    primarios = _musculos(registro["primaryMuscles"])
    secundarios = tuple(m for m in _musculos(registro["secondaryMuscles"]) if m not in primarios)
    patron = _patron(registro, texto)
    encima = patron == "empuje_vertical" or _tiene(texto, _POR_ENCIMA_CABEZA)
    axial = _carga_axial(texto, patron, implemento, encima)
    if _tiene(texto, _AGARRE_PESADO):
        agarre = "pesado"
    else:
        agarre = "ligero" if _tiene(texto, _AGARRE_LIGERO) else "ninguno"
    return Ejercicio(
        id_externo=registro["id"],
        nombre_original=registro["name"],
        nombre_es=None,
        patron=patron,
        mecanica=_traducir(_MECANICAS, registro.get("mechanic"), "mecánica"),
        nivel=_traducir(_NIVELES, registro["level"], "nivel"),
        categoria=categoria,
        carga_axial=axial,
        impacto=categoria == "plyometrics" or _tiene(texto, _IMPACTO),
        agarre=agarre,
        lateralidad="unilateral" if _tiene(texto, _UNILATERAL) else "bilateral",
        por_encima_cabeza=encima,
        posicion_estirada=False,  # sin evidencia, el valor prudente
        familia_sustitucion=None,
        rango_movimiento="reducido" if _tiene(texto, _RANGO_REDUCIDO) else "completo",
        implemento=implemento,
        apto_fuerza=categoria in CATEGORIAS_FUERZA,
        origen_etiquetado="derivado",
        revisado=False,
        musculos_primarios=primarios,
        musculos_secundarios=secundarios,
        regiones=regiones_de(patron, axial, primarios),
        materiales=(implemento,),
    )
