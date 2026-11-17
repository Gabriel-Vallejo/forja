import { Redirect, useLocalSearchParams } from 'expo-router';
import { z } from 'zod';

import { FormularioLimitacion } from '../../src/pantallas/FormularioLimitacion';

// Sin `id`, una limitación nueva; con él, la edición de esa. El `id` es un identificador, no un dato de salud.
const Parametros = z.object({ id: z.uuid().optional() });

export default function RutaLimitacion() {
  const p = Parametros.safeParse(useLocalSearchParams());
  if (!p.success) return <Redirect href="/limitaciones" />;
  return <FormularioLimitacion id={p.data.id} />;
}
