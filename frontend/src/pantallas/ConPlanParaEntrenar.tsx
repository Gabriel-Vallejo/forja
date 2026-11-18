import { type ReactNode, useEffect, useState } from 'react';

import type { PlanActual } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { Texto } from '../componentes/Texto';
import { fechaCorta, usePlanActual } from '../plan/plan';
import { useSincronizacion } from '../registro/sincronizacion';
import { ConPlan } from './ConPlan';

// Entrenar no puede depender de la red (ADR 0008): con registro local, cada plan que llega bien se guarda en el
// móvil y, si `GET /planes/actual` falla o tarda, se entrena con el guardado.

/** Lo que se espera a la red antes de tirar del plan guardado (no se esperan los reintentos de TanStack Query). */
export const ESPERA_RED_MS = 4000;
export const SIN_PLAN_SIN_RED =
  'No hay conexión y este móvil todavía no tiene tu plan guardado. Conéctate una vez para descargarlo; después podrás entrenar sin red.';

export interface PlanParaEntrenar {
  consulta: ReturnType<typeof usePlanActual>;
  /** La red ha fallado o tarda: toca el plan guardado. */
  sinRed: boolean;
  /** undefined mientras se lee; null si no hay (o no hay registro local). */
  guardado: { plan: PlanActual; guardadoEn: number } | null | undefined;
}

export function usePlanParaEntrenar(): PlanParaEntrenar {
  const { outbox } = useSincronizacion();
  const consulta = usePlanActual();
  const [guardado, setGuardado] = useState<PlanParaEntrenar['guardado']>(undefined);
  const [agotada, setAgotada] = useState(false);
  const { isSuccess, isPending, data } = consulta;

  useEffect(() => {
    // Si no se puede guardar, se sigue con el de red: solo se pierde el respaldo sin conexión.
    if (outbox && isSuccess) outbox.guardarPlan(data).catch(() => undefined);
  }, [outbox, isSuccess, data]);

  useEffect(() => {
    if (!outbox) return undefined;
    let vigente = true;
    outbox.planGuardado().then(
      (g) => vigente && setGuardado(g),
      () => vigente && setGuardado(null),
    );
    return () => {
      vigente = false;
    };
  }, [outbox]);

  useEffect(() => {
    if (!isPending) return undefined;
    const id = setTimeout(() => setAgotada(true), ESPERA_RED_MS);
    return () => clearTimeout(id);
  }, [isPending]);

  const sinRed = !isSuccess && (consulta.isError || consulta.failureCount > 0 || agotada);
  return { consulta, sinRed, guardado: outbox ? guardado : null };
}

/** Como `ConPlan`, pero con registro local y sin red pinta `children` con el plan guardado y lo dice. */
export function ConPlanParaEntrenar({ children }: { children: (plan: PlanActual) => ReactNode }) {
  const { outbox } = useSincronizacion();
  const { consulta, sinRed, guardado } = usePlanParaEntrenar();
  if (!outbox || !sinRed) return <ConPlan>{children}</ConPlan>;
  if (guardado === undefined) return <Texto tono="secundario">Cargando tu plan…</Texto>;
  if (guardado === null) {
    return (
      <>
        <Aviso tipo="aviso" mensaje={SIN_PLAN_SIN_RED} />
        <Boton variante="secundario" etiqueta="Reintentar" onPress={() => consulta.refetch()} />
      </>
    );
  }
  return (
    <>
      <Texto estilo="nota" tono="secundario">{`Sin conexión: usas el plan guardado el ${fechaCorta(guardado.guardadoEn)}`}</Texto>
      {children(guardado.plan)}
    </>
  );
}
