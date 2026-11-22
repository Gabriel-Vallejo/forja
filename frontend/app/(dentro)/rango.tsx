import { useLocalSearchParams } from 'expo-router';
import { z } from 'zod';

import { RangoDetalle } from '../../src/pantallas/RangoDetalle';
import { PESO, REPETICIONES } from '../../src/pantallas/Calculadora';
import { EJERCICIOS_CON_ESTANDAR, SERIE_INICIAL } from '../../src/rango/ejercicios';
import { LEVANTAMIENTOS } from '../../src/rango/estandares';

const LARGO_MAXIMO = 120;
const Parametros = z.object({
  ejercicio: z.string().min(1).max(LARGO_MAXIMO),
  levantamiento: z.enum(LEVANTAMIENTOS),
  peso: z.coerce.number().min(PESO.min).max(PESO.max),
  reps: z.coerce.number().int().min(REPETICIONES.min).max(REPETICIONES.max),
});
const PRIMERO = EJERCICIOS_CON_ESTANDAR[0];

export default function RutaRango() {
  const p = Parametros.safeParse(useLocalSearchParams());
  // Sin parámetros válidos (enlace directo), la serie de partida de la calculadora.
  const serie = p.success
    ? { ejercicio: p.data.ejercicio, levantamiento: p.data.levantamiento, peso: p.data.peso, repeticiones: p.data.reps }
    : { ejercicio: PRIMERO?.nombre ?? '', levantamiento: PRIMERO?.levantamiento ?? 'banca', peso: SERIE_INICIAL.peso, repeticiones: SERIE_INICIAL.repeticiones };
  return <RangoDetalle {...serie} />;
}
