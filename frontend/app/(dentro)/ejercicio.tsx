import { Redirect, useLocalSearchParams } from 'expo-router';
import { z } from 'zod';

import { Ejercicio } from '../../src/pantallas/Ejercicio';

const LARGO_MAXIMO = 120;
const Parametros = z.object({ dia: z.coerce.number().int().positive(), id: z.string().min(1).max(LARGO_MAXIMO) });

export default function RutaEjercicio() {
  const p = Parametros.safeParse(useLocalSearchParams());
  if (!p.success) return <Redirect href="/entreno" />;
  return <Ejercicio dia={p.data.dia} idExterno={p.data.id} />;
}
