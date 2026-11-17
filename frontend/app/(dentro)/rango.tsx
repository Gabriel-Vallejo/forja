import { useLocalSearchParams } from 'expo-router';
import { z } from 'zod';

import { EJERCICIOS_CALCULADORA, SERIE_INICIAL } from '../../src/datos/ejemplo';
import { RangoDetalle } from '../../src/pantallas/RangoDetalle';
import { PESO, REPETICIONES } from '../../src/pantallas/Calculadora';

const LARGO_MAXIMO = 120;
const Parametros = z.object({
  ejercicio: z.string().min(1).max(LARGO_MAXIMO),
  peso: z.coerce.number().min(PESO.min).max(PESO.max),
  reps: z.coerce.number().int().min(REPETICIONES.min).max(REPETICIONES.max),
});

export default function RutaRango() {
  const p = Parametros.safeParse(useLocalSearchParams());
  // Sin parámetros válidos (enlace directo), la serie de ejemplo.
  const serie = p.success
    ? { ejercicio: p.data.ejercicio, peso: p.data.peso, repeticiones: p.data.reps }
    : { ejercicio: EJERCICIOS_CALCULADORA[0]?.nombre ?? '', peso: SERIE_INICIAL.peso, repeticiones: SERIE_INICIAL.repeticiones };
  return <RangoDetalle {...serie} />;
}
