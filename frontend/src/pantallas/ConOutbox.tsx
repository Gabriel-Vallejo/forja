import { useRouter } from 'expo-router';
import type { ReactNode } from 'react';

import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { Texto } from '../componentes/Texto';
import type { Outbox } from '../registro/outbox';
import { useSincronizacion } from '../registro/sincronizacion';

export const SIN_REGISTRO_LOCAL = 'El registro de series necesita la app instalada; en Expo Go y en el navegador no está disponible.';

/** Resuelve «abriendo la base» y «sin registro local» (web, Expo Go o base que no abre); con outbox, pinta `children`. */
export function ConOutbox({ children }: { children: (outbox: Outbox) => ReactNode }) {
  const { outbox, abriendo } = useSincronizacion();
  const router = useRouter();
  if (outbox) return <>{children(outbox)}</>;
  if (abriendo) return <Texto tono="secundario">Abriendo el registro del móvil…</Texto>;
  return (
    <>
      <Aviso tipo="info" mensaje={SIN_REGISTRO_LOCAL} />
      <Boton variante="secundario" etiqueta="Volver atrás" onPress={() => router.back()} />
    </>
  );
}
