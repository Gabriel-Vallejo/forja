import { z } from 'zod';

export const LONGITUD_MINIMA_CONTRASENA = 12;
export const LONGITUD_MAXIMA_CONTRASENA = 128;
export const EDAD_MINIMA = 18;

const FORMATO_FECHA = /^(\d{4})-(\d{2})-(\d{2})$/;

export interface DatosRegistro {
  correo: string;
  contrasena: string;
  fechaNacimiento: string;
}

export type ErroresRegistro = Partial<Record<keyof DatosRegistro, string>>;

function leerFecha(texto: string): { anio: number; mes: number; dia: number } | null {
  const partes = FORMATO_FECHA.exec(texto);
  if (!partes) return null;
  const [anio, mes, dia] = partes.slice(1).map(Number) as [number, number, number];
  const fecha = new Date(Date.UTC(anio, mes - 1, dia));
  const existe = fecha.getUTCFullYear() === anio && fecha.getUTCMonth() === mes - 1 && fecha.getUTCDate() === dia;
  return existe ? { anio, mes, dia } : null;
}

/** Años cumplidos el día `hoy` (fecha local del móvil). */
export function edadEn(nacimiento: { anio: number; mes: number; dia: number }, hoy: Date): number {
  const mes = hoy.getMonth() + 1;
  const yaCumplidos = mes > nacimiento.mes || (mes === nacimiento.mes && hoy.getDate() >= nacimiento.dia);
  return hoy.getFullYear() - nacimiento.anio - (yaCumplidos ? 0 : 1);
}

function esquemaRegistro(hoy: Date) {
  return z.object({
    correo: z.email('Escribe un correo válido.'),
    contrasena: z
      .string()
      .min(LONGITUD_MINIMA_CONTRASENA, `La contraseña necesita al menos ${LONGITUD_MINIMA_CONTRASENA} caracteres.`)
      .max(LONGITUD_MAXIMA_CONTRASENA, `La contraseña puede tener como mucho ${LONGITUD_MAXIMA_CONTRASENA} caracteres.`),
    fechaNacimiento: z
      .string()
      .refine((texto) => leerFecha(texto) !== null, 'Escribe la fecha como AAAA-MM-DD, por ejemplo 2001-04-23.')
      .refine((texto) => {
        const fecha = leerFecha(texto);
        return fecha === null || edadEn(fecha, hoy) >= EDAD_MINIMA;
      }, `Forja es para mayores de ${EDAD_MINIMA} años.`),
  });
}

/** Errores por campo, o un objeto vacío si todo vale. El backend vuelve a validar. */
export function validarRegistro(datos: DatosRegistro, hoy: Date = new Date()): ErroresRegistro {
  const resultado = esquemaRegistro(hoy).safeParse({ ...datos, correo: datos.correo.trim() });
  if (resultado.success) return {};
  const { fieldErrors } = z.flattenError(resultado.error);
  return {
    correo: fieldErrors.correo?.[0],
    contrasena: fieldErrors.contrasena?.[0],
    fechaNacimiento: fieldErrors.fechaNacimiento?.[0],
  };
}

export function hayErrores(errores: ErroresRegistro): boolean {
  return Object.values(errores).some(Boolean);
}
