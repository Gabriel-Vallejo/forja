import type { ReactNode } from 'react';

import { mensajeDeError } from '../api/errores';
import type { PlanActual } from '../api/esquemas';
import { RUTINA_POR_DEFECTO } from '../api/forja';
import { Aviso } from '../componentes/Aviso';
import { AvisoBloqueo } from '../componentes/AvisoBloqueo';
import { Boton } from '../componentes/Boton';
import { Texto } from '../componentes/Texto';
import { useBloqueoSalud } from '../limitaciones/consultas';
import { useGenerarPlan } from '../plan/generar';
import { usePlanActual } from '../plan/plan';

function SinPlan() {
  const generar = useGenerarPlan();
  // Si el estado no carga no se bloquea aquí: el backend responde 409 y se enseña su mensaje.
  const bloqueo = useBloqueoSalud();
  const bloqueado = bloqueo.data?.bloquea === true;
  const { diasPorSemana, minutosPorSesion } = RUTINA_POR_DEFECTO;

  return (
    <>
      <Texto>Todavía no tienes plan.</Texto>
      <Texto tono="secundario">
        {`Lo armamos con ${diasPorSemana} días por semana, ${minutosPorSesion} minutos por sesión y el material de un gimnasio completo. Podrás cambiarlo más adelante.`}
      </Texto>
      {bloqueado ? <AvisoBloqueo nivel={bloqueo.data?.nivel} /> : null}
      {generar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(generar.error)} /> : null}
      <Boton
        etiqueta="Generar mi plan"
        deshabilitado={bloqueado}
        cargando={generar.isPending}
        onPress={() => generar.mutate()}
      />
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
