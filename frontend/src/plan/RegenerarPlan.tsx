import { useState } from 'react';

import { mensajeDeError } from '../api/errores';
import { Aviso } from '../componentes/Aviso';
import { AvisoBloqueo } from '../componentes/AvisoBloqueo';
import { Boton } from '../componentes/Boton';
import { Confirmacion } from '../componentes/Confirmacion';
import { Texto } from '../componentes/Texto';
import { confirmarRegenerar } from '../limitaciones/textos';
import { useBloqueoSalud } from '../limitaciones/consultas';
import { frasePerfil, usePerfilEntreno } from './ajustes';
import { useGenerarPlan } from './generar';

interface PropsRegenerarPlan {
  etiqueta: string;
  /** Explicar aquí el bloqueo; el hub ya lo explica arriba y lo deja en solo el botón deshabilitado. */
  avisoBloqueo?: boolean;
}

/** Regenera el plan tras confirmarlo en pantalla. Con bloqueo activo queda deshabilitado y se explica por qué. */
export function RegenerarPlan({ etiqueta, avisoBloqueo = true }: PropsRegenerarPlan) {
  const bloqueo = useBloqueoSalud();
  const generar = useGenerarPlan();
  const ajustes = usePerfilEntreno();
  const [confirmando, setConfirmando] = useState(false);

  if (bloqueo.data?.bloquea) {
    return (
      <>
        {avisoBloqueo ? <AvisoBloqueo nivel={bloqueo.data.nivel} /> : null}
        <Boton variante="secundario" etiqueta={etiqueta} deshabilitado onPress={() => undefined} />
      </>
    );
  }
  // Sin los ajustes no se sabe con qué se regenera: se enseña el fallo y se deja reintentar, sin inventar nada.
  if (ajustes.isError) {
    return (
      <>
        <Aviso tipo="error" mensaje={mensajeDeError(ajustes.error)} />
        <Boton variante="secundario" etiqueta="Reintentar" onPress={() => ajustes.refetch()} />
      </>
    );
  }
  if (!ajustes.data) return <Texto tono="secundario">Cargando tus ajustes…</Texto>;
  if (!confirmando) {
    return (
      <Boton
        variante="secundario"
        etiqueta={etiqueta}
        onPress={() => {
          generar.reset();
          setConfirmando(true);
        }}
      />
    );
  }
  return (
    <Confirmacion
      mensaje={confirmarRegenerar(frasePerfil(ajustes.data))}
      etiquetaConfirmar="Sí, regenerar"
      etiquetaCargando="Generando…"
      cargando={generar.isPending}
      error={generar.isError ? mensajeDeError(generar.error) : undefined}
      alConfirmar={() => generar.mutate(undefined, { onSuccess: () => setConfirmando(false) })}
      alCancelar={() => setConfirmando(false)}
    />
  );
}
