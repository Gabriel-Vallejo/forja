import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import type { ObjetivoEjercicio, PlanActual } from '../api/esquemas';
import { RUTINA_POR_DEFECTO } from '../api/forja';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { useSesion } from '../sesion/sesion';
import { useTheme } from '../theme';

export const CLAVE_PLAN_ACTUAL = ['plan', 'actual'] as const;

export function prescripcion(e: ObjetivoEjercicio): string {
  const carga = e.cargaKg === undefined ? '' : ` · ${e.cargaKg} kg`;
  return `${e.series} × ${e.repeticionesMin}-${e.repeticionesMax} · RIR ${e.rirObjetivo}${carga}`;
}

function Plan({ plan }: { plan: PlanActual }) {
  const tema = useTheme();
  return (
    <View style={{ gap: tema.space.s6 }}>
      {plan.dias.map((dia) => (
        <View key={dia.dia} style={{ gap: tema.space.s4 }}>
          <Texto estilo="heading" encabezado>{`Día ${dia.dia}`}</Texto>
          {dia.ejercicios.map((e) => (
            <View key={e.idExterno} style={{ gap: tema.space.s1 }}>
              <Texto>{e.nombre}</Texto>
              <Texto estilo="data">{prescripcion(e)}</Texto>
              <Texto tono="secundario">{e.motivo}</Texto>
            </View>
          ))}
        </View>
      ))}
    </View>
  );
}

function SinPlan() {
  const { api } = useSesion();
  const consultas = useQueryClient();
  const generar = useMutation({
    mutationFn: () => api.generarPlan(),
    onSuccess: () => consultas.invalidateQueries({ queryKey: CLAVE_PLAN_ACTUAL }),
  });
  const { diasPorSemana, minutosPorSesion } = RUTINA_POR_DEFECTO;

  return (
    <>
      <Texto>Todavía no tienes plan.</Texto>
      <Texto tono="secundario">
        {`Lo armamos con ${diasPorSemana} días por semana, ${minutosPorSesion} minutos por sesión y el material de un gimnasio completo. Podrás cambiarlo más adelante.`}
      </Texto>
      {generar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(generar.error)} /> : null}
      <Boton etiqueta="Generar mi plan" cargando={generar.isPending} onPress={() => generar.mutate()} />
    </>
  );
}

export function Portada() {
  const { api, salir } = useSesion();
  const plan = useQuery({ queryKey: CLAVE_PLAN_ACTUAL, queryFn: () => api.planActual() });
  const cierre = useMutation({ mutationFn: salir });

  return (
    <Pantalla titulo="Tu plan">
      {plan.isPending ? <Texto tono="secundario">Cargando tu plan…</Texto> : null}
      {plan.isError ? (
        <>
          <Aviso tipo="error" mensaje={mensajeDeError(plan.error)} />
          <Boton variante="secundario" etiqueta="Reintentar" onPress={() => plan.refetch()} />
        </>
      ) : null}
      {plan.isSuccess && plan.data === null ? <SinPlan /> : null}
      {plan.isSuccess && plan.data ? <Plan plan={plan.data} /> : null}
      <Boton
        variante="secundario"
        etiqueta="Cerrar sesión"
        etiquetaCargando="Cerrando sesión…"
        cargando={cierre.isPending}
        onPress={() => cierre.mutate()}
      />
    </Pantalla>
  );
}
