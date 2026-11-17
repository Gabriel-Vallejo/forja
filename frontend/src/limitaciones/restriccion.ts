import type { DatosRestriccion, Restriccion } from '../api/esquemas';
import type { CodigoEstado, CodigoLado, CodigoPatron, CodigoRegion, CodigoTipo } from './catalogo';

export const MAXIMO_POR_USUARIO = 30; // ServicioRestricciones.MAXIMO_POR_USUARIO
export const MAXIMO_PARAMETRO = 40; // ServicioRestricciones.MAXIMO_PARAMETRO

/** Solo estos dos tipos usan el parámetro (el filtro del backend lo añade a sus anotaciones). */
export const TIPOS_CON_PARAMETRO: readonly CodigoTipo[] = ['limitar_carga', 'tempo_controlado'];

const UNIDADES = /kg|kilo|°|º|grado/i;

/** Lo que el formulario tiene a medias: todo puede faltar menos lo que ya tiene valor por defecto. */
export interface BorradorRestriccion {
  region?: CodigoRegion;
  lado?: CodigoLado;
  patron?: CodigoPatron;
  tipo?: CodigoTipo;
  parametro: string;
  estado?: CodigoEstado;
  profesional: boolean;
  textoOriginal?: string;
}

export interface ErroresRestriccion {
  ubicacion?: string;
  lado?: string;
  tipo?: string;
  parametro?: string;
  estado?: string;
}

/** Las reglas del backend (`Restriccion`, `ServicioRestricciones.validar`) en el cliente. `datos` solo si no hay errores. */
export function validarRestriccion(b: BorradorRestriccion): { errores: ErroresRestriccion; datos?: DatosRestriccion } {
  const parametro = b.tipo && TIPOS_CON_PARAMETRO.includes(b.tipo) ? b.parametro.trim() : '';
  const errores: ErroresRestriccion = {};
  if (!b.region && !b.patron) errores.ubicacion = 'Elige una región o un patrón de movimiento.';
  if (!b.lado) errores.lado = 'Elige el lado.';
  if (!b.tipo) errores.tipo = 'Elige qué te limita.';
  if (!b.estado) errores.estado = 'Elige cómo está ahora.';
  if (parametro.length > MAXIMO_PARAMETRO) errores.parametro = `Máximo ${MAXIMO_PARAMETRO} caracteres.`;
  else if (UNIDADES.test(parametro)) errores.parametro = 'Escribe una categoría o un RPE, sin kilos ni grados.';

  if (Object.keys(errores).length > 0 || !b.lado || !b.tipo || !b.estado) return { errores };
  return {
    errores,
    datos: {
      region: b.region,
      lado: b.lado,
      patron: b.patron,
      tipo: b.tipo,
      parametro: parametro === '' ? undefined : parametro,
      estado: b.estado,
      origen: b.profesional ? 'profesional' : 'usuario',
      textoOriginal: b.textoOriginal,
    },
  };
}

export function borradorDe(r: Restriccion): BorradorRestriccion {
  return {
    region: r.region,
    lado: r.lado,
    patron: r.patron,
    tipo: r.tipo,
    parametro: r.parametro ?? '',
    estado: r.estado,
    profesional: r.origen === 'profesional',
    textoOriginal: r.textoOriginal,
  };
}
