import { Redirect, useLocalSearchParams } from 'expo-router';
import { z } from 'zod';

import { VistaPreviaAjuste } from '../../src/pantallas/VistaPreviaAjuste';

const LARGO_MAXIMO = 120;
const CODIGO_MUSCULO = /^[a-z_]{1,30}$/;
const Parametros = z.object({
  tipo: z.enum(['mas', 'menos', 'no_recomendar', 'reemplazar', 'variedad', 'enfocar']),
  id: z.string().min(1).max(LARGO_MAXIMO).optional(),
  // «pecho,biceps»; vacío quita el enfoque.
  musculos: z
    .string()
    .max(LARGO_MAXIMO)
    .optional()
    .transform((m) => (m ? m.split(',') : []))
    .pipe(z.array(z.string().regex(CODIGO_MUSCULO)).max(2)),
});

export default function RutaVistaPrevia() {
  const p = Parametros.safeParse(useLocalSearchParams());
  if (!p.success) return <Redirect href="/entreno" />;
  const { tipo, id, musculos } = p.data;
  return <VistaPreviaAjuste ajuste={tipo === 'enfocar' ? { tipo, musculos } : { tipo, idExterno: id }} />;
}
