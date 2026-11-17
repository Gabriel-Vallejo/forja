import { useRouter } from 'expo-router';
import type { ReactNode } from 'react';

import { mensajeDeError } from '../api/errores';
import type { PlanActual } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { AvisoBloqueo } from '../componentes/AvisoBloqueo';
import { Boton } from '../componentes/Boton';
import { Texto } from '../componentes/Texto';
import { useBloqueoSalud } from '../limitaciones/consultas';
import { frasePerfil, usePerfilEntreno } from '../plan/ajustes';
import { useGenerarPlan } from '../plan/generar';
import { usePlanActual } from '../plan/plan';

function SinPlan() {
  const generar = useGenerarPlan();
  // Si el estado no carga no se bloquea aquí: el backend responde 409 y se enseña su mensaje.
  const bloqueo = useBloqueoSalud();
  const bloqueado = bloqueo.data?.bloquea === true;
  const ajustes = usePerfilEntreno();
  const router = useRouter();

  return (
    <>
      <Texto>Todavía no tienes plan.</Texto>
      {ajustes.isPending ? <Texto tono="secundario">Cargando tus ajustes…</Texto> : null}
      {ajustes.isError ? (
        <>
          <Aviso tipo="error" mensaje={mensajeDeError(ajustes.error)} />
          <Boton variante="secundario" etiqueta="Reintentar" onPress={() => ajustes.refetch()} />
        </>
      ) : null}
      {ajustes.data ? (
        <>
          <Texto tono="secundario">{`Lo armamos con ${frasePerfil(ajustes.data)}.`}</Texto>
          <Boton variante="secundario" etiqueta="Cambiar ajustes del plan" onPress={() => router.push('/ajustes-plan')} />
        </>
      ) : null}
      {bloqueado ? <AvisoBloqueo nivel={bloqueo.data?.nivel} /> : null}
      {generar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(generar.error)} /> : null}
      {ajustes.data ? (
        <Boton
          etiqueta="Generar mi plan"
          deshabilitado={bloqueado}
          cargando={generar.isPending}
          onPress={() => generar.mutate()}
        />
      ) : null}
    </>
  );
}

/** Carga el plan actual y resuelve cargando, error y «sin plan»; con plan, pinta `children`. */
export function ConPlan({ children }: { children: (plan: PlanActual) => ReactNode }) {
  const plan = usePlanActual();
  if (plan.isPending) return <Texto tono="secundario">Cargando tu plan…</Texto>;
  if (plan.isError) {
    return (
      <>
        <Aviso tipo="error" mensaje={mensajeDeError(plan.error)} />
        <Boton variante="secundario" etiqueta="Reintentar" onPress={() => plan.refetch()} />
      </>
    );
  }
  return plan.data === null ? <SinPlan /> : <>{children(plan.data)}</>;
}
