import { useRouter } from 'expo-router';

import type { NivelBloqueo } from '../api/esquemas';
import { etiquetaRepetir, mensajeBloqueo } from '../limitaciones/textos';
import { Aviso } from './Aviso';
import { Boton } from './Boton';

interface PropsAvisoBloqueo {
  nivel: NivelBloqueo | null | undefined;
  /** Con enlace al hub de limitaciones para repetir el cuestionario; sin él, si ya estás ahí. */
  conEnlace?: boolean;
}

/** Explica por qué «Generar mi plan» está deshabilitado (RF-04: con bandera roja no se genera la rutina). */
export function AvisoBloqueo({ nivel, conEnlace = true }: PropsAvisoBloqueo) {
  const router = useRouter();
  return (
    <>
      <Aviso tipo="aviso" mensaje={mensajeBloqueo(nivel)} />
      {conEnlace ? (
        <Boton variante="secundario" etiqueta={etiquetaRepetir(nivel)} onPress={() => router.push('/limitaciones')} />
      ) : null}
    </>
  );
}
