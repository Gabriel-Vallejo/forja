"""Modelo del catálogo y códigos válidos (los mismos que siembra V1__catalogo_ejercicios.sql)."""

from dataclasses import dataclass

MUSCULOS = frozenset({
    "abdominales", "isquiotibiales", "aductores", "cuadriceps", "biceps", "hombros", "pecho",
    "espalda_media", "gemelos", "gluteos", "lumbares", "dorsales", "triceps", "trapecio",
    "antebrazos", "cuello", "abductores",
})  # fmt: skip

PATRONES = frozenset({
    "sentadilla", "bisagra", "zancada", "empuje_horizontal", "empuje_vertical",
    "traccion_horizontal", "traccion_vertical", "aislamiento_brazo", "aislamiento_pierna",
    "core", "acarreo", "pliometria", "movilidad", "cardio",
})  # fmt: skip

REGIONES = frozenset({
    "columna_lumbar", "columna_cervical", "hombro", "codo", "muneca", "cadera", "rodilla",
    "tobillo",
})  # fmt: skip

MATERIALES = frozenset({
    "barra", "mancuerna", "maquina", "polea", "peso_corporal", "kettlebell", "banda", "barra_ez",
    "balon_medicinal", "fitball", "rodillo", "otro",
})  # fmt: skip

MECANICAS = frozenset({"compuesto", "aislamiento"})
NIVELES = frozenset({"principiante", "intermedio", "avanzado"})
AGARRES = frozenset({"ninguno", "ligero", "pesado"})
LATERALIDADES = frozenset({"bilateral", "unilateral"})
RANGOS = frozenset({"completo", "reducido"})
ORIGENES = frozenset({"derivado", "curado"})

PESO_PRIMARIO = "1.0"
PESO_SECUNDARIO = "0.5"


@dataclass(frozen=True)
class Ejercicio:
    """Un ejercicio ya etiquetado, listo para volcarse a SQL."""

    id_externo: str
    nombre_original: str
    nombre_es: str | None
    patron: str
    mecanica: str | None  # compuesto, aislamiento o None
    nivel: str  # principiante, intermedio o avanzado
    categoria: str  # la de la fuente, tal cual
    carga_axial: bool
    impacto: bool
    agarre: str  # ninguno, ligero o pesado
    lateralidad: str  # bilateral o unilateral
    por_encima_cabeza: bool
    posicion_estirada: bool
    familia_sustitucion: str | None
    rango_movimiento: str  # completo o reducido
    implemento: str
    apto_fuerza: bool
    origen_etiquetado: str  # derivado o curado
    revisado: bool
    musculos_primarios: tuple[str, ...]
    musculos_secundarios: tuple[str, ...]
    regiones: tuple[str, ...]
    materiales: tuple[str, ...]

    def __post_init__(self) -> None:
        """Rechaza cualquier valor fuera de los códigos de V1 (también protege el SQL generado)."""
        self._exigir("patron", self.patron, PATRONES)
        self._exigir("mecanica", self.mecanica, MECANICAS | {None})
        self._exigir("nivel", self.nivel, NIVELES)
        self._exigir("agarre", self.agarre, AGARRES)
        self._exigir("lateralidad", self.lateralidad, LATERALIDADES)
        self._exigir("rango_movimiento", self.rango_movimiento, RANGOS)
        self._exigir("origen_etiquetado", self.origen_etiquetado, ORIGENES)
        self._exigir("implemento", self.implemento, MATERIALES)
        for campo, valores, validos in (
            ("musculos_primarios", self.musculos_primarios, MUSCULOS),
            ("musculos_secundarios", self.musculos_secundarios, MUSCULOS),
            ("regiones", self.regiones, REGIONES),
            ("materiales", self.materiales, MATERIALES),
        ):
            for valor in valores:
                self._exigir(campo, valor, validos)
        repetidos = set(self.musculos_primarios) & set(self.musculos_secundarios)
        if repetidos:
            raise ValueError(
                f"{self.id_externo}: músculo primario y secundario a la vez: {repetidos}"
            )

    def _exigir(
        self, campo: str, valor: str | None, validos: frozenset[str | None] | frozenset[str]
    ) -> None:
        if valor not in validos:
            raise ValueError(f"{self.id_externo}: {campo} inválido: {valor!r}")
