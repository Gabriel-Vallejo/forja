import { useState } from 'react';

import { mensajeDeError } from '../api/errores';
import { AvisoBloqueo } from '../componentes/AvisoBloqueo';
import { Boton } from '../componentes/Boton';
import { Confirmacion } from '../componentes/Confirmacion';
import { CONFIRMAR_REGENERAR } from '../limitaciones/textos';
import { useBloqueoSalud } from '../limitaciones/consultas';
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
  const [confirmando, setConfirmando] = useState(false);

  if (bloqueo.data?.bloquea) {
    return (
      <>
        {avisoBloqueo ? <AvisoBloqueo nivel={bloqueo.data.nivel} /> : null}
        <Boton variante="secundario" etiqueta={etiqueta} deshabilitado onPress={() => undefined} />
      </>
    );
  }
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
      mensaje={CONFIRMAR_REGENERAR}
      etiquetaConfirmar="Sí, regenerar"
      etiquetaCargando="Generando…"
      cargando={generar.isPending}
      error={generar.isError ? mensajeDeError(generar.error) : undefined}
      alConfirmar={() => generar.mutate(undefined, { onSuccess: () => setConfirmando(false) })}
      alCancelar={() => setConfirmando(false)}
    />
  );
}
