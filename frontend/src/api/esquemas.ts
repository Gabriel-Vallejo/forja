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

// De la respuesta de generar solo hace falta saber que se guardó: el plan se vuelve a leer de /planes/actual.
export const esquemaPlanGenerado = z.object({ uuid: z.uuid() }) satisfies z.ZodType<Esquemas['PlanGenerado']>;

// El backend manda `null` (o nada) en lo que no aplica; en la app es `undefined`.
const opcional = <T extends z.ZodType>(esquema: T) =>
  esquema
    .nullish()
    .transform((v) => v ?? undefined)
    .optional();

export const esquemaConsentimientos = z.array(z.object({ tipo: z.string() })) satisfies z.ZodType<
  Esquemas['RespuestaConsentimiento'][]
>;

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
  errores: z.array(z.object({ campo: z.string(), mensaje: z.string() })).optional(),
});

export type SolicitudRegistro = Required<Esquemas['Registro']>;
export type SolicitudRutina = Esquemas['SolicitudRutina'];
export type Plataforma = NonNullable<Esquemas['Login']['plataforma']>;
