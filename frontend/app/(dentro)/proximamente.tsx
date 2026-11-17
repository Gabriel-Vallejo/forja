import { useLocalSearchParams } from 'expo-router';
import { z } from 'zod';

import { Proximamente } from '../../src/pantallas/Proximamente';

const LARGO_MAXIMO = 60;
const Parametros = z.object({ titulo: z.string().min(1).max(LARGO_MAXIMO) });

export default function RutaProximamente() {
  const p = Parametros.safeParse(useLocalSearchParams());
  return <Proximamente titulo={p.success ? p.data.titulo : 'Próximamente'} />;
}
