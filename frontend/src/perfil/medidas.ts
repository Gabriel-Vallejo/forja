// Validación de las medidas antes de enviarlas. Mismos rangos que `MedidasCuerpo` en el backend, que es quien manda:
// aquí solo se evita el viaje de ida y vuelta y se explica el error junto al campo.
import { CAMPOS_MEDIDAS, type CampoMedida, type CuerpoMedidas, type Medidas } from '../api/esquemas';

interface DefinicionMedida {
  etiqueta: string;
  unidad: 'cm' | 'kg';
  min: number;
  max: number;
}

export const MEDIDAS: Readonly<Record<CampoMedida, DefinicionMedida>> = {
  alturaCm: { etiqueta: 'Altura', unidad: 'cm', min: 100, max: 250 },
  pesoKg: { etiqueta: 'Peso', unidad: 'kg', min: 30, max: 300 },
  cuelloCm: { etiqueta: 'Cuello', unidad: 'cm', min: 20, max: 70 },
  cinturaCm: { etiqueta: 'Cintura', unidad: 'cm', min: 40, max: 200 },
  caderaCm: { etiqueta: 'Cadera', unidad: 'cm', min: 50, max: 200 },
};

export type ValoresMedidas = Record<CampoMedida, string>;
export type ErroresMedidas = Partial<Record<CampoMedida, string>>;

export const ERROR_CINTURA_CUELLO = 'La cintura tiene que ser mayor que el cuello.';

/** Lo guardado en texto de campo: coma decimal y sin ceros de más. */
export function valoresDe(medidas: Medidas): ValoresMedidas {
  return Object.fromEntries(
    CAMPOS_MEDIDAS.map((c) => [c, medidas[c] === undefined ? '' : String(medidas[c]).replace('.', ',')]),
  ) as ValoresMedidas;
}

const aNumero = (texto: string): number => Number(texto.trim().replace(',', '.'));

/** Cuerpo para el PUT, o los errores por campo. Un campo vacío es «sin dar» (`null`). */
export function validarMedidas(valores: ValoresMedidas): { cuerpo: CuerpoMedidas } | { errores: ErroresMedidas } {
  const cuerpo = {} as CuerpoMedidas;
  const errores: ErroresMedidas = {};
  for (const campo of CAMPOS_MEDIDAS) {
    const { etiqueta, min, max, unidad } = MEDIDAS[campo];
    const texto = valores[campo].trim();
    if (texto === '') {
      cuerpo[campo] = null;
      continue;
    }
    const valor = aNumero(texto);
    if (!Number.isFinite(valor) || valor < min || valor > max) {
      errores[campo] = `${etiqueta}: un número entre ${min} y ${max} ${unidad}.`;
    } else {
      cuerpo[campo] = valor;
    }
  }
  const { cuelloCm, cinturaCm } = cuerpo;
  if (!errores.cuelloCm && !errores.cinturaCm && cuelloCm != null && cinturaCm != null && cinturaCm <= cuelloCm) {
    errores.cinturaCm = ERROR_CINTURA_CUELLO;
  }
  return Object.keys(errores).length > 0 ? { errores } : { cuerpo };
}
