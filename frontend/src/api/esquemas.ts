// Esquemas Zod de las respuestas que usa la app. El `satisfies` ata cada uno a los tipos generados desde el
// OpenAPI (esquema.d.ts): si el backend cambia un campo, `tsc` falla aquí.
// Son más estrictos que el OpenAPI, que no marca obligatorios los campos de las respuestas.
import { z } from 'zod';

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
}) satisfies z.ZodType<Esquemas['ObjetivoEjercicio']>;
export type ObjetivoEjercicio = z.infer<typeof esquemaObjetivoEjercicio>;

export const esquemaPlanActual = z.object({
  uuid: z.uuid(),
  creadoEn: z.iso.datetime(),
  estado: z.string(),
  dias: z.array(
    z.object({
      dia: z.number().int().positive(),
      ejercicios: z.array(esquemaObjetivoEjercicio),
    }),
  ),
}) satisfies z.ZodType<Esquemas['PlanActual']>;
export type PlanActual = z.infer<typeof esquemaPlanActual>;

// De la respuesta de generar solo hace falta saber que se guardó: el plan se vuelve a leer de /planes/actual.
export const esquemaPlanGenerado = z.object({ uuid: z.uuid() }) satisfies z.ZodType<Esquemas['PlanGenerado']>;

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
