// Esquemas Zod de las respuestas que usa la app. El `satisfies` ata cada uno a los tipos generados desde el
// OpenAPI (esquema.d.ts): si el backend cambia un campo, `tsc` falla aquí.
// Son más estrictos que el OpenAPI, que no marca obligatorios los campos de las respuestas.
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

export const esquemaMensaje = z.object({ mensaje: z.string() }) satisfies z.ZodType<Esquemas['Mensaje']>;

export const esquemaDatosSesion = z.object({
  tokenAcceso: z.string().min(1),
  tokenRefresco: z.string().min(1),
  expiraEn: z.iso.datetime(),
  dispositivoId: z.uuid(),
}) satisfies z.ZodType<Esquemas['DatosSesion']>;
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
}) satisfies z.ZodType<Esquemas['ObjetivoEjercicio']>;
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
}) satisfies z.ZodType<Esquemas['PlanActual']>;
export type PlanActual = z.infer<typeof esquemaPlanActual>;

/** Niveles de una regla (ADR 0007): A-D, evidencia de la fuente; H, heurística de entrenadores; P, propuesta propia. */
export const NIVELES = ['A', 'B', 'C', 'D', 'H', 'P'] as const;
export type Nivel = (typeof NIVELES)[number];

export const esquemaPorQue = z.object({
  idExterno: z.string(),
  dia: z.number().int().positive(),
  porQue: z.array(z.object({ codigo: z.string(), texto: z.string(), fuente: z.string(), nivel: z.enum(NIVELES) })),
}) satisfies z.ZodType<Esquemas['PorQueEjercicio']>;
export type Razon = z.infer<typeof esquemaPorQue>['porQue'][number];

// De la respuesta de generar solo hace falta saber que se guardó: el plan se vuelve a leer de /planes/actual.
export const esquemaPlanGenerado = z.object({ uuid: z.uuid() }) satisfies z.ZodType<Esquemas['PlanGenerado']>;

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
) satisfies z.ZodType<Esquemas['RespuestaConsentimiento'][]>;
export type Consentimiento = z.infer<typeof esquemaConsentimientos>[number];

export const esquemaDispositivos = z.array(
  z.object({
    id: z.uuid(),
    nombre: z.string(),
    plataforma: z.enum(['movil', 'pc']),
    ultimaActividad: opcional(z.iso.datetime()),
    actual: z.boolean(),
  }),
) satisfies z.ZodType<Esquemas['DatosDispositivo'][]>;
/** Un dispositivo de la cuenta tal como lo lista el backend (distinto de `Dispositivo`, que es el de este móvil al entrar). */
export type DispositivoCuenta = z.infer<typeof esquemaDispositivos>[number];

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
}) satisfies z.ZodType<Esquemas['RespuestaRestriccion']>;
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
  Esquemas['RespuestaPregunta']
>;
export type PreguntaCribado = z.infer<typeof esquemaPreguntaCribado>;

/** Las preguntas y cuántas semanas cuenta un síntoma como reciente: las dos cosas las pone el backend. */
export const esquemaCuestionarioCribado = z.object({
  semanasReciente: z.number().int(),
  preguntas: z.array(esquemaPreguntaCribado),
}) satisfies z.ZodType<Esquemas['RespuestaPreguntas']>;
export type CuestionarioCribado = z.infer<typeof esquemaCuestionarioCribado>;

export const esquemaResultadoCribado = z.object({
  nivel: z.enum(['seguir', 'consultar_profesional', 'parar_urgencias']),
  motivos: z.array(z.string()),
}) satisfies z.ZodType<Esquemas['RespuestaCribado']>;
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
}) satisfies z.ZodType<Esquemas['RespuestaBanderas']>;
export type ResultadoBanderas = z.infer<typeof esquemaResultadoBanderas>;

/** Qué nivel de bloqueo recuerda el backend tras un cribado urgente o una bandera roja (`GET /banderas/estado`). */
export const esquemaEstadoBloqueo = z.object({
  bloquea: z.boolean(),
  // Sin `satisfies` contra el OpenAPI generado: ahí `nivel` es `string` sin nulo y el backend manda `null`.
  nivel: z.enum(['urgencias_hoy', 'profesional_en_dias', 'parar_urgencias']).nullish(),
});
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
) satisfies z.ZodType<Esquemas['RespuestaEfecto'][]>;
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
}) satisfies z.ZodType<Esquemas['PerfilEntreno']>;
export type PerfilEntreno = z.infer<typeof esquemaPerfilEntreno>;

/** Material que el backend deja elegir (`GET /catalogo/materiales`). */
export const esquemaMateriales = z.array(z.object({ codigo: z.string(), nombre: z.string() })) satisfies z.ZodType<
  Esquemas['Opcion'][]
>;
export type Material = z.infer<typeof esquemaMateriales>[number];

export type SolicitudRegistro = Required<Esquemas['Registro']>;
export type SolicitudRutina = Esquemas['SolicitudRutina'];
export type Plataforma = NonNullable<Esquemas['Login']['plataforma']>;

/** Cuerpo de `PUT /sesiones/{uuid}`: la sesión entera, con todas sus series (correcciones y anulaciones incluidas). */
export type SesionEntrada = Esquemas['SesionEntrada'];
export type SerieEntrada = Esquemas['SerieEntrada'];

/** De la respuesta solo hace falta saber que el servidor guardó esa sesión: lo demás ya está en el móvil. */
export const esquemaSesionGuardada = z.object({ uuid: z.uuid() }) satisfies z.ZodType<Esquemas['SesionVista']>;

/** Los tres tests de movilidad (RF-08) y sus tramos, con los mismos códigos que el backend. */
export const TESTS_MOVILIDAD = ['tobillo', 'cadera', 'hombro'] as const;
export type TestMovilidad = (typeof TESTS_MOVILIDAD)[number];
export const TRAMOS = ['limitado', 'medio', 'bueno'] as const;
export type Tramo = (typeof TRAMOS)[number];

const esquemaResultadoMovilidad = z.object({ tramo: z.enum(TRAMOS), hechoEn: z.iso.datetime() }) satisfies z.ZodType<
  Esquemas['ResultadoMovilidad']
>;

/** `GET` y `PUT /movilidad`: un resultado por test; sin hacer, `undefined`. */
export const esquemaMovilidad = z.object({
  tobillo: opcional(esquemaResultadoMovilidad),
  cadera: opcional(esquemaResultadoMovilidad),
  hombro: opcional(esquemaResultadoMovilidad),
}) satisfies z.ZodType<Esquemas['Movilidad']>;
export type Movilidad = z.infer<typeof esquemaMovilidad>;

/** Cuerpo de `PUT /movilidad`: reemplaza los tres; `null` deja ese test sin hacer. */
export type TramosMovilidad = Record<TestMovilidad, Tramo | null>;
