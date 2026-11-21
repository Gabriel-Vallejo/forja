import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';

import type { DolorRepetido, PlanActual } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { FilaEjercicio } from '../componentes/FilaEjercicio';
import { Icono } from '../componentes/Icono';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import {
  AVISO_PRIMERA_SESION,
  calidadPlan,
  numeroDeOrden,
  fechaCorta,
  hayPrimeraSesion,
  prescripcion,
  seriesDe,
} from '../plan/plan';
import { AjustarPlan } from '../plan/AjustarPlan';
import { textoDolorRepetido, useDolorRepetido } from '../plan/dolorRepetido';
import { RegenerarPlan } from '../plan/RegenerarPlan';
import { useCargasSiguientes } from '../registro/sincronizacion';
import { type Theme, useTheme } from '../theme';
import { ConPlan } from './ConPlan';

const ICONO_CALIDAD = 13;
const ALTO_DIA = 84;
/** Hasta tres días caben repartiéndose el ancho; con más, cada pestaña mide esto y la fila se desplaza. */
const DIAS_SIN_DESPLAZAR = 3;
/** Algo menos que un tercio de la pantalla: la pestaña siguiente asoma por el borde y avisa de que la fila se desplaza. */
const ANCHO_DIA_DESPLAZABLE = 112;

/** La línea de abajo de la fila: el dolor repetido delante de qué toca, si lo hay. */
const notaDe = (motivo: string | undefined, dolor: DolorRepetido | undefined) =>
  [dolor ? textoDolorRepetido(dolor) : undefined, motivo].filter(Boolean).join('. ') || undefined;

function Plan({ plan }: { plan: PlanActual }) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const cargas = useCargasSiguientes(plan);
  const dolor = useDolorRepetido();
  const [elegido, setElegido] = useState(plan.diaDeHoy);
  const dia = plan.dias.find((d) => d.dia === elegido) ?? plan.dias[0];
  const optimo = plan.estado === 'optimo';

  const desplaza = plan.dias.length > DIAS_SIN_DESPLAZAR;
  const tablist = { accessibilityRole: 'tablist', accessibilityLabel: 'Días' } as const;
  const pestanas = plan.dias.map((d) => {
    const sel = d.dia === dia?.dia;
    const hoy = d.dia === plan.diaDeHoy;
    return (
      <Pressable
        key={d.dia}
        accessibilityRole="tab"
        accessibilityLabel={`Día ${d.dia}, ${d.nombre}${hoy ? ', hoy' : ''}`}
        accessibilityState={{ selected: sel }}
        onPress={() => setElegido(d.dia)}
        style={[estilos.dia, desplaza && estilos.diaDesplazable, sel && estilos.diaElegido]}
      >
        <Texto estilo="etiquetaDia" color={sel ? tema.color.primary : tema.color.onSurfaceVariant}>
          {hoy ? `Día ${d.dia} · hoy` : `Día ${d.dia}`}
        </Texto>
        <Texto estilo="nota" peso="800" tono={sel ? 'principal' : 'secundario'} lineas={1}>
          {d.nombre}
        </Texto>
      </Pressable>
    );
  });

  return (
    <>
      <View style={estilos.resumen}>
        <View style={estilos.calidad}>
          <Icono nombre={optimo ? 'check' : 'info'} tam={ICONO_CALIDAD} color={optimo ? tema.color.success : tema.color.info} />
          <Texto estilo="nota">{calidadPlan(plan.estado)}</Texto>
        </View>
        <Texto estilo="data" tono="secundario">{`${plan.dias.length} días por semana`}</Texto>
      </View>
      {desplaza ? (
        <ScrollView horizontal showsHorizontalScrollIndicator={false} {...tablist} style={estilos.deBordeABorde} contentContainerStyle={estilos.diasDesplazables}>
{pestanas}
        </ScrollView>
      ) : (
        <View {...tablist} style={estilos.dias}>
{pestanas}
        </View>
      )}
      {dia && hayPrimeraSesion(dia) ? <Aviso tipo="info" mensaje={AVISO_PRIMERA_SESION} /> : null}
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
                prescripcion={prescripcion(e, cargas.get(e.idExterno))}
                nota={notaDe(e.primeraSesion ? undefined : e.motivo, dolor.get(e.idExterno))}
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
            <AjustarPlan />
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
    deBordeABorde: { marginHorizontal: -tema.space.s4 },
    diasDesplazables: { flexDirection: 'row', gap: tema.space.s2, paddingHorizontal: tema.space.s4 },
    diaDesplazable: { flex: 0, width: ANCHO_DIA_DESPLAZABLE },
    diaElegido: { backgroundColor: tema.color.surfaceHigh, borderColor: tema.color.primary },
    cabeceraLista: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline' },
    lista: { borderRadius: tema.radius.md, backgroundColor: tema.color.surface, overflow: 'hidden' },
  });
}
