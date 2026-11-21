// Esquemas Zod de las respuestas que usa la app. El `satisfies` ata cada uno a los tipos generados desde el
// OpenAPI (esquema.d.ts): si el backend cambia o quita un campo que la app lee, `tsc` falla aquí.
import { z } from 'zod';

import {
  codigos,
  ESTADOS,
  LADOS,
  ORIGENES,
  PATRONES,
  REGIONES,
  TIPOS,
  type CodigoEstado,
  type CodigoLado,
  type CodigoOrigen,
  type CodigoPatron,
  type CodigoRegion,
  type CodigoTipo,
} from '../limitaciones/catalogo';
import type { components } from './esquema';

type Esquemas = components['schemas'];

/**
 * Lo que la app lee de una respuesta: un subconjunto de sus campos, con sus tipos. Lo que el backend puede mandar a
 * `null` puede leerse como `undefined` (ver `opcional`).
 *
 * shortcut: no comprueba la otra dirección (un campo que el backend puede mandar a `null` y aquí no lo admite);
 * eso lo cazan los tests de cada pantalla. Si se cuela alguno, comprobarlo con un tipo condicional por esquema.
 */
type Lectura<T> = T extends readonly (infer U)[]
  ? Lectura<U>[]
  : T extends object
    ? { [K in keyof T]?: Lectura<NonNullable<T[K]>> | (null extends T[K] ? null | undefined : never) }
    : T;

export const esquemaMensaje = z.object({ mensaje: z.string() }) satisfies z.ZodType<Lectura<Esquemas['Mensaje']>>;

export const esquemaDatosSesion = z.object({
  tokenAcceso: z.string().min(1),
  tokenRefresco: z.string().min(1),
  expiraEn: z.iso.datetime(),
  dispositivoId: z.uuid(),
}) satisfies z.ZodType<Lectura<Esquemas['DatosSesion']>>;
export type DatosSesion = z.infer<typeof esquemaDatosSesion>;

const esquemaMusculo = z.object({
  grupo: z.enum([
    'pecho', 'hombros', 'brazos', 'triceps', 'antebrazos', 'abdomen', 'cuadriceps',
    'aductores', 'gemelos', 'trapecio', 'dorsal', 'lumbar', 'gluteos', 'isquios',
  ]),
  rol: z.enum(['primario', 'secundario']),
});
export type Musculo = z.infer<typeof esquemaMusculo>;

const esquemaObjetivoEjercicio = z.object({
  idExterno: z.string(),
  nombre: z.string(),
  orden: z.number().int(),
  series: z.number().int().positive(),
  repeticionesMin: z.number().int().nonnegative(),
  repeticionesMax: z.number().int().nonnegative(),
  rirObjetivo: z.number().int().nonnegative(),
  // Nula hasta que entrenas el ejercicio por primera vez.
  cargaKg: z
    .number()
    .nullable()
    .transform((carga) => carga ?? undefined),
  incrementoKg: z.number(),
  estado: z.enum(['acumulando', 'subir_carga', 'estancado', 'deload']),
  motivo: z.string(),
  musculos: z.array(esquemaMusculo),
  primeraSesion: z.boolean(),
}) satisfies z.ZodType<Lectura<Esquemas['ObjetivoEjercicio']>>;
export type ObjetivoEjercicio = z.infer<typeof esquemaObjetivoEjercicio>;

export const esquemaPlanActual = z.object({
  uuid: z.uuid(),
  creadoEn: z.iso.datetime(),
  estado: z.string(),
  diaDeHoy: z.number().int().positive(),
  dias: z.array(
    z.object({
      dia: z.number().int().positive(),
      nombre: z.string(),
      ejercicios: z.array(esquemaObjetivoEjercicio),
    }),
  ),
}) satisfies z.ZodType<Lectura<Esquemas['PlanActual']>>;
export type PlanActual = z.infer<typeof esquemaPlanActual>;

/** Niveles de una regla (ADR 0007): A-D, evidencia de la fuente; H, heurística de entrenadores; P, propuesta propia. */
export const NIVELES = ['A', 'B', 'C', 'D', 'H', 'P'] as const;
export type Nivel = (typeof NIVELES)[number];

export const esquemaPorQue = z.object({
  idExterno: z.string(),
  dia: z.number().int().positive(),
  porQue: z.array(z.object({ codigo: z.string(), texto: z.string(), fuente: z.string(), nivel: z.enum(NIVELES) })),
}) satisfies z.ZodType<Lectura<Esquemas['PorQueEjercicio']>>;
export type Razon = z.infer<typeof esquemaPorQue>['porQue'][number];

// De la respuesta de generar solo hace falta saber que se guardó: el plan se vuelve a leer de /planes/actual.
export const esquemaPlanGenerado = z.object({ uuid: z.uuid() }) satisfies z.ZodType<Lectura<Esquemas['PlanGenerado']>>;

// El backend manda `null` (o nada) en lo que no aplica; en la app es `undefined`.
const opcional = <T extends z.ZodType>(esquema: T) =>
  esquema
    .nullish()
    .transform((v) => v ?? undefined)
    .optional();

/** Los cinco tipos de consentimiento del backend (`TipoConsentimiento`); el código es el de la tabla. */
export const TIPOS_CONSENTIMIENTO = ['fotos_cuerpo', 'medidas_salud', 'lesiones', 'limitaciones', 'suplementos'] as const;
export type TipoConsentimiento = (typeof TIPOS_CONSENTIMIENTO)[number];

// `tipo` queda como texto: un tipo nuevo del backend no debe romper la lista (la pantalla solo enseña los conocidos).
export const esquemaConsentimientos = z.array(
  z.object({ tipo: z.string(), versionTexto: z.string(), dadoEn: z.iso.datetime() }),
) satisfies z.ZodType<Lectura<Esquemas['RespuestaConsentimiento'][]>>;
export type Consentimiento = z.infer<typeof esquemaConsentimientos>[number];

/** RF-27: un ejercicio que ha dolido en varias de sus últimas sesiones. Es dato de salud: solo en memoria. */
export const esquemaDolorRepetido = z.array(
  z.object({ idExterno: z.string(), sesionesConDolor: z.number().int(), sesionesMiradas: z.number().int() }),
) satisfies z.ZodType<Lectura<Esquemas['DolorRepetido'][]>>;
export type DolorRepetido = z.infer<typeof esquemaDolorRepetido>[number];

export const esquemaDispositivos = z.array(
  z.object({
    id: z.uuid(),
    nombre: z.string(),
    plataforma: z.enum(['movil', 'pc']),
    ultimaActividad: opcional(z.iso.datetime()),
    actual: z.boolean(),
  }),
) satisfies z.ZodType<Lectura<Esquemas['DatosDispositivo'][]>>;
/** Un dispositivo de la cuenta tal como lo lista el backend (distinto de `Dispositivo`, que es el de este móvil al entrar). */
export type DispositivoCuenta = z.infer<typeof esquemaDispositivos>[number];

/** Versión del JSON de «Descargar mis datos» que entiende la app; el backend la sube si cambia de forma. */
export const FORMATO_EXPORTACION = 1;

// Solo se comprueba el primer nivel: el contenido se guarda tal cual para el usuario (`looseObject` no quita nada).
// El `satisfies` obliga a nombrar aquí cada sección del OpenAPI: si el backend añade una, `tsc` falla.
const seccionesExportadas = {
  formato: z.literal(FORMATO_EXPORTACION),
  generadoEn: z.iso.datetime(),
  cuenta: z.looseObject({ correo: z.string() }),
  dispositivos: z.array(z.unknown()),
  consentimientos: z.array(z.unknown()),
  perfilDatos: z.looseObject({}),
  medidas: z.looseObject({}).nullable(),
  perfilEntreno: z.looseObject({}),
  movilidad: z.looseObject({}).nullable(),
  limitaciones: z.array(z.unknown()),
  bloqueos: z.array(z.unknown()),
  preferenciasPlan: z.looseObject({}),
  planes: z.array(z.unknown()),
  sesiones: z.array(z.unknown()),
} satisfies Record<keyof Esquemas['DatosExportados'], z.ZodType>;
export const esquemaDatosExportados = z.looseObject(seccionesExportadas);
export type DatosExportados = z.infer<typeof esquemaDatosExportados>;

export const esquemaRestriccion = z.object({
  uuid: z.uuid(),
  region: opcional(z.enum(codigos(REGIONES))),
  lado: z.enum(codigos(LADOS)),
  patron: opcional(z.enum(codigos(PATRONES))),
  tipo: z.enum(codigos(TIPOS)),
  parametro: opcional(z.string()),
  estado: z.enum(codigos(ESTADOS)),
  origen: z.enum(codigos(ORIGENES)),
  textoOriginal: opcional(z.string()),
}) satisfies z.ZodType<Lectura<Esquemas['RespuestaRestriccion']>>;
export type Restriccion = z.infer<typeof esquemaRestriccion>;
export const esquemaRestricciones = z.array(esquemaRestriccion);

/** Lo que el usuario declara (cuerpo de POST y PUT de `/restricciones`); el `uuid` lo pone el servidor. */
export interface DatosRestriccion {
  region?: CodigoRegion;
  lado: CodigoLado;
  patron?: CodigoPatron;
  tipo: CodigoTipo;
  parametro?: string;
  estado: CodigoEstado;
  origen: CodigoOrigen;
  textoOriginal?: string;
}

const esquemaPreguntaCribado = z.object({ codigo: z.string(), texto: z.string(), admiteReciente: z.boolean() }) satisfies z.ZodType<
  Lectura<Esquemas['RespuestaPregunta']>
>;
export type PreguntaCribado = z.infer<typeof esquemaPreguntaCribado>;

/** Las preguntas y cuántas semanas cuenta un síntoma como reciente: las dos cosas las pone el backend. */
export const esquemaCuestionarioCribado = z.object({
  semanasReciente: z.number().int(),
  preguntas: z.array(esquemaPreguntaCribado),
}) satisfies z.ZodType<Lectura<Esquemas['RespuestaPreguntas']>>;
export type CuestionarioCribado = z.infer<typeof esquemaCuestionarioCribado>;

export const esquemaResultadoCribado = z.object({
  nivel: z.enum(['seguir', 'consultar_profesional', 'parar_urgencias']),
  motivos: z.array(z.string()),
}) satisfies z.ZodType<Lectura<Esquemas['RespuestaCribado']>>;
export type ResultadoCribado = z.infer<typeof esquemaResultadoCribado>;

/** Las siete respuestas por código de pregunta y, aparte, cuáles de los síntomas fueron recientes. */
export interface SolicitudCribado {
  respuestas: Record<string, boolean>;
  recientes: string[];
}

export const esquemaResultadoBanderas = z.object({
  bloquea: z.boolean(),
  banderas: z.array(
    z.object({ nivel: z.enum(['urgencias_hoy', 'profesional_en_dias']), motivo: z.string(), guia: z.string() }),
  ),
}) satisfies z.ZodType<Lectura<Esquemas['RespuestaBanderas']>>;
export type ResultadoBanderas = z.infer<typeof esquemaResultadoBanderas>;

/** Qué nivel de bloqueo recuerda el backend tras un cribado urgente o una bandera roja (`GET /banderas/estado`). */
export const esquemaEstadoBloqueo = z.object({
  bloquea: z.boolean(),
  nivel: z.enum(['urgencias_hoy', 'profesional_en_dias', 'parar_urgencias']).nullish(),
}) satisfies z.ZodType<Lectura<Esquemas['EstadoBloqueo']>>;
export type EstadoBloqueo = z.infer<typeof esquemaEstadoBloqueo>;
export type NivelBloqueo = NonNullable<EstadoBloqueo['nivel']>;

/** Dolor de 0 a 10 por código de región (solo las marcadas) y las diez señales, todas obligatorias. */
export type SolicitudBanderas = Required<Esquemas['SolicitudBanderas']>;

export const esquemaEfecto = z.array(
  z.object({
    idExterno: z.string(),
    nombre: z.string(),
    decision: z.enum(['excluido', 'sustituido', 'con_anotaciones']),
    motivo: opcional(z.string()),
    sustituto: opcional(z.object({ idExterno: z.string(), nombre: z.string() })),
    anotaciones: z.array(z.string()),
  }),
) satisfies z.ZodType<Lectura<Esquemas['RespuestaEfecto'][]>>;
export type EfectoEjercicio = z.infer<typeof esquemaEfecto>[number];

/** Cuerpo de error RFC 9457 del backend, con la lista `errores` de los fallos de validación. */
export const esquemaProblema = z.object({
  status: z.number().optional(),
  title: z.string().optional(),
  detail: z.string().optional(),
  /** Código estable de algunos errores de negocio (p. ej. `bandera_roja_activa`). */
  codigo: z.string().optional(),
  errores: z.array(z.object({ campo: z.string(), mensaje: z.string() })).optional(),
});

/** Ajustes del plan: lo que se guarda con `PUT /perfil/entreno` y lo que usa «Generar mi plan». */
export type AjustesEntreno = Required<Esquemas['AjustesEntreno']>;

export const esquemaPerfilEntreno = z.object({
  diasPorSemana: z.number().int(),
  minutosPorSesion: z.number().int(),
  material: z.array(z.string()),
  // True mientras el usuario no ha guardado los suyos: son los valores por defecto del backend.
  porDefecto: z.boolean(),
}) satisfies z.ZodType<Lectura<Esquemas['PerfilEntreno']>>;
export type PerfilEntreno = z.infer<typeof esquemaPerfilEntreno>;

/** Material que el backend deja elegir (`GET /catalogo/materiales`). */
export const esquemaMateriales = z.array(z.object({ codigo: z.string(), nombre: z.string() })) satisfies z.ZodType<
  Lectura<Esquemas['Opcion'][]>
>;
export type Material = z.infer<typeof esquemaMateriales>[number];

export type SolicitudRegistro = Required<Esquemas['Registro']>;
export type SolicitudRutina = Esquemas['SolicitudRutina'];
export type Plataforma = NonNullable<Esquemas['Login']['plataforma']>;

/** Cuerpo de `PUT /sesiones/{uuid}`: la sesión entera, con todas sus series (correcciones y anulaciones incluidas). */
export type SesionEntrada = Esquemas['SesionEntrada'];
export type SerieEntrada = Esquemas['SerieEntrada'];

/** De la respuesta solo hace falta saber que el servidor guardó esa sesión: lo demás ya está en el móvil. */
export const esquemaSesionGuardada = z.object({ uuid: z.uuid() }) satisfies z.ZodType<Lectura<Esquemas['SesionVista']>>;

/** Los tres tests de movilidad (RF-08) y sus tramos, con los mismos códigos que el backend. */
export const TESTS_MOVILIDAD = ['tobillo', 'cadera', 'hombro'] as const;
export type TestMovilidad = (typeof TESTS_MOVILIDAD)[number];
export const TRAMOS = ['limitado', 'medio', 'bueno'] as const;
export type Tramo = (typeof TRAMOS)[number];

const esquemaResultadoMovilidad = z.object({ tramo: z.enum(TRAMOS), hechoEn: z.iso.datetime() }) satisfies z.ZodType<
  Lectura<Esquemas['ResultadoMovilidad']>
>;

/** `GET` y `PUT /movilidad`: un resultado por test; sin hacer, `undefined`. */
export const esquemaMovilidad = z.object({
  tobillo: opcional(esquemaResultadoMovilidad),
  cadera: opcional(esquemaResultadoMovilidad),
  hombro: opcional(esquemaResultadoMovilidad),
}) satisfies z.ZodType<Lectura<Esquemas['Movilidad']>>;
export type Movilidad = z.infer<typeof esquemaMovilidad>;

/** Cuerpo de `PUT /movilidad`: reemplaza los tres; `null` deja ese test sin hacer. */
export type TramosMovilidad = Record<TestMovilidad, Tramo | null>;

/** Perfil manual: sexo, experiencia y objetivo (preferencias) con los mismos códigos que el backend. */
export const SEXOS = ['hombre', 'mujer', 'no_indica'] as const;
export type Sexo = (typeof SEXOS)[number];
export const EXPERIENCIAS = ['principiante', 'intermedio', 'avanzado'] as const;
export type Experiencia = (typeof EXPERIENCIAS)[number];
export const OBJETIVOS = ['fuerza', 'hipertrofia', 'fuerza_hipertrofia'] as const;
export type ObjetivoPerfil = (typeof OBJETIVOS)[number];

/** `GET` y `PUT /perfil/datos`: sin definir, experiencia y objetivo son `undefined`. */
export const esquemaDatosPerfil = z.object({
  sexo: z.enum(SEXOS),
  experiencia: opcional(z.enum(EXPERIENCIAS)),
  objetivo: opcional(z.enum(OBJETIVOS)),
}) satisfies z.ZodType<Lectura<Esquemas['DatosPerfil']>>;
export type DatosPerfil = z.infer<typeof esquemaDatosPerfil>;

/** Cuerpo de `PUT /perfil/datos`: `null` deja experiencia u objetivo sin definir. */
export interface CuerpoDatosPerfil {
  sexo: Sexo;
  experiencia: Experiencia | null;
  objetivo: ObjetivoPerfil | null;
}

/** % de grasa estimado con la fórmula de la US Navy, con su margen de error en puntos porcentuales. */
const esquemaGrasaEstimada = z.object({ porcentaje: z.number(), margen: z.number() }) satisfies z.ZodType<
  Lectura<Esquemas['GrasaEstimada']>
>;

/** `GET` y `PUT /perfil/medidas` (cm y kg): lo que falta es `undefined`. La estimación la calcula el backend. */
export const esquemaMedidas = z.object({
  alturaCm: opcional(z.number()),
  pesoKg: opcional(z.number()),
  cuelloCm: opcional(z.number()),
  cinturaCm: opcional(z.number()),
  caderaCm: opcional(z.number()),
  grasaEstimada: opcional(esquemaGrasaEstimada),
}) satisfies z.ZodType<Lectura<Esquemas['MedidasPerfil']>>;
export type Medidas = z.infer<typeof esquemaMedidas>;

export const CAMPOS_MEDIDAS = ['alturaCm', 'pesoKg', 'cuelloCm', 'cinturaCm', 'caderaCm'] as const;
export type CampoMedida = (typeof CAMPOS_MEDIDAS)[number];
/** Cuerpo de `PUT /perfil/medidas`: reemplaza las cinco; `null` borra esa medida. */
export type CuerpoMedidas = Record<CampoMedida, number | null>;

// Ajustes del plan (PRD v0.1). Preferencias de entreno, no datos de salud.
/** Un ajuste: `mas`, `menos`, `no_recomendar` y `reemplazar` llevan `idExterno`; `enfocar`, `musculos` (0 a 2). */
export type Ajuste = Esquemas['Ajuste'];
/** Preferencias con códigos: el cuerpo que reemplaza las guardadas (para quitar una se manda el resto). */
export type PreferenciasPlan = Required<Esquemas['PreferenciasPlan']>;

const esquemaPreferenciasPlan = z.object({
  exclusiones: z.array(z.string()),
  prioritarios: z.array(z.string()),
  series: z.record(z.string(), z.number().int()),
  variedad: z.boolean(),
}) satisfies z.ZodType<PreferenciasPlan>;

// Del plan propuesto solo se enseñan los días con sus ejercicios y las notas; el resto de la traza se descarta.
export const esquemaVistaPrevia = z.object({
  version: z.uuid(),
  plan: z.object({
    resumen: z.string(),
    notas: z.array(z.string()),
    dias: z.array(
      z.object({
        dia: z.number().int(),
        ejercicios: z.array(z.object({ idExterno: z.string(), nombre: z.string(), series: z.number().int() })),
      }),
    ),
  }),
  ejercicios: z.array(z.object({ idExterno: z.string(), nombre: z.string(), antes: z.number().int(), despues: z.number().int() })),
  musculos: z.array(z.object({ musculo: z.string(), nombre: z.string(), antes: z.number(), despues: z.number() })),
  preferencias: esquemaPreferenciasPlan,
}) satisfies z.ZodType<Lectura<Esquemas['VistaPrevia']>>;
export type VistaPrevia = z.infer<typeof esquemaVistaPrevia>;

const esquemaElemento = z.object({ codigo: z.string(), nombre: z.string() });
export const esquemaPreferencias = z.object({
  exclusiones: z.array(esquemaElemento),
  prioritarios: z.array(esquemaElemento),
  series: z.array(z.object({ idExterno: z.string(), nombre: z.string(), series: z.number().int() })),
  variedad: z.boolean(),
}) satisfies z.ZodType<Lectura<Esquemas['PreferenciasGuardadas']>>;
export type PreferenciasGuardadas = z.infer<typeof esquemaPreferencias>;
