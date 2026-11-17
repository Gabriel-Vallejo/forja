import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import type { PlanActual } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { FilaEjercicio } from '../componentes/FilaEjercicio';
import { Icono } from '../componentes/Icono';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import {
  AVISO_PRIMERA_SEMANA,
  calidadPlan,
  numeroDeOrden,
  esPrimeraSemana,
  prescripcion,
  seriesDe,
} from '../plan/plan';
import { RegenerarPlan } from '../plan/RegenerarPlan';
import { type Theme, useTheme } from '../theme';
import { ConPlan } from './ConPlan';

const ICONO_CALIDAD = 13;
const ALTO_DIA = 84;
const MESES = ['ene.', 'feb.', 'mar.', 'abr.', 'may.', 'jun.', 'jul.', 'ago.', 'sep.', 'oct.', 'nov.', 'dic.'];

const fechaCorta = (iso: string) => {
  const f = new Date(iso);
  return `${f.getDate()} ${MESES[f.getMonth()] ?? ''}`;
};

function Plan({ plan }: { plan: PlanActual }) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [elegido, setElegido] = useState(plan.diaDeHoy);
  const dia = plan.dias.find((d) => d.dia === elegido) ?? plan.dias[0];
  const primeraSemana = esPrimeraSemana(plan);
  const optimo = plan.estado === 'optimo';

  return (
    <>
      <View style={estilos.resumen}>
        <View style={estilos.calidad}>
          <Icono nombre={optimo ? 'check' : 'info'} tam={ICONO_CALIDAD} color={optimo ? tema.color.success : tema.color.info} />
          <Texto estilo="nota">{calidadPlan(plan.estado)}</Texto>
        </View>
        <Texto estilo="data" tono="secundario">{`${plan.dias.length} días por semana`}</Texto>
      </View>
      <View accessibilityRole="tablist" accessibilityLabel="Días" style={estilos.dias}>
        {plan.dias.map((d) => {
          const sel = d.dia === dia?.dia;
          const hoy = d.dia === plan.diaDeHoy;
          return (
            <Pressable
              key={d.dia}
              accessibilityRole="tab"
              accessibilityLabel={`Día ${d.dia}, ${d.nombre}${hoy ? ', hoy' : ''}`}
              accessibilityState={{ selected: sel }}
              onPress={() => setElegido(d.dia)}
              style={[estilos.dia, sel && estilos.diaElegido]}
            >
              <Texto estilo="etiquetaDia" color={sel ? tema.color.primary : tema.color.onSurfaceVariant}>
                {hoy ? `Día ${d.dia} · hoy` : `Día ${d.dia}`}
              </Texto>
              <Texto estilo="nota" peso="800" tono={sel ? 'principal' : 'secundario'} lineas={1}>
                {d.nombre}
              </Texto>
            </Pressable>
          );
        })}
      </View>
      {primeraSemana ? <Aviso tipo="info" mensaje={AVISO_PRIMERA_SEMANA} /> : null}
      {dia ? (
        <>
          <View style={estilos.cabeceraLista}>
            <Texto estilo="body" peso="700">{`${dia.ejercicios.length} ejercicios`}</Texto>
            <Texto estilo="data" tono="secundario">{`${seriesDe(dia)} series`}</Texto>
          </View>
          <View accessibilityRole="list" style={estilos.lista}>
            {dia.ejercicios.map((e, i) => (
              <FilaEjercicio
                key={e.idExterno}
                primera={i === 0}
                numero={numeroDeOrden(i)}
                nombre={e.nombre}
                prescripcion={prescripcion(e)}
                nota={e.primeraSesion ? undefined : e.motivo}
                alPulsar={() => router.push({ pathname: '/ejercicio', params: { dia: String(dia.dia), id: e.idExterno } })}
              />
            ))}
          </View>
        </>
      ) : null}
    </>
  );
}

export function MiPlan() {
  return (
    <Pantalla bordes={['top']} hueco="s35">
      <View>
        <Texto estilo="etiqueta" tono="secundario">Tu plan actual</Texto>
        <Texto estilo="title" encabezado>Mi plan</Texto>
      </View>
      <ConPlan>
        {(plan) => (
          <>
            <Texto estilo="mini" tono="secundario">{`Generado el ${fechaCorta(plan.creadoEn)}`}</Texto>
            <Plan plan={plan} />
            <RegenerarPlan etiqueta="Regenerar mi plan" />
          </>
        )}
      </ConPlan>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    resumen: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s2 },
    calidad: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: tema.space.s15,
      paddingHorizontal: tema.space.s2,
      paddingVertical: tema.space.s075,
      borderRadius: tema.radius.sm,
      backgroundColor: tema.color.surfaceHigh,
    },
    dias: { flexDirection: 'row', gap: tema.space.s2 },
    dia: {
      flex: 1,
      height: ALTO_DIA,
      padding: tema.space.s25,
      borderRadius: tema.radius.md,
      backgroundColor: tema.color.surface,
      borderWidth: tema.border.control,
      borderColor: 'transparent',
      justifyContent: 'space-between',
    },
    diaElegido: { backgroundColor: tema.color.surfaceHigh, borderColor: tema.color.primary },
    cabeceraLista: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline' },
    lista: { borderRadius: tema.radius.md, backgroundColor: tema.color.surface, overflow: 'hidden' },
  });
}
