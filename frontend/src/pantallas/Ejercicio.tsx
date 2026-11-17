import { useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import type { PlanActual } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { BotonIcono } from '../componentes/BotonIcono';
import { Cuerpo } from '../componentes/Cuerpo';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { NOMBRE_ESTADO, numeroDeOrden, prescripcion } from '../plan/plan';
import { type Theme, useTheme } from '../theme';
import { ConPlan } from './ConPlan';

const ANCHO_FIGURA = 96;
const ALTO_FIGURA = 150;
const ANCHO_CUERPO = 126;
const CUERPO_IZQUIERDA = -15;
const CUERPO_ARRIBA = -32;

interface PropsEjercicio {
  dia: number;
  idExterno: string;
}

function Detalle({ plan, dia, idExterno }: PropsEjercicio & { plan: PlanActual }) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const ejercicios = plan.dias.find((d) => d.dia === dia)?.ejercicios ?? [];
  const indice = ejercicios.findIndex((e) => e.idExterno === idExterno);
  const e = ejercicios[indice];
  if (!e) return <Aviso tipo="aviso" mensaje="Este ejercicio ya no está en tu plan." />;

  return (
    <>
      <Texto estilo="etiquetaEstrecha" tono="secundario" centrado>{`Día ${dia} · ejercicio ${numeroDeOrden(indice)}`}</Texto>
      <View style={estilos.ficha}>
        <View style={estilos.fichaTextos}>
          <Texto estilo="ejercicio" encabezado>{e.nombre.toUpperCase()}</Texto>
          <Texto estilo="data" tono="secundario">{prescripcion(e)}</Texto>
        </View>
        <View style={estilos.figura}>
          <View style={estilos.cuerpo}>
            <Cuerpo ancho={ANCHO_CUERPO} girable={false} decorativo />
          </View>
        </View>
      </View>
      <View style={estilos.tarjeta} accessibilityLabel="Qué toca">
        <View style={estilos.cabecera}>
          <Texto estilo="etiquetaEstrecha" tono="secundario">Qué toca</Texto>
          <View style={estilos.estado}>
            <Texto estilo="mini">{NOMBRE_ESTADO[e.estado]}</Texto>
          </View>
        </View>
        <Texto estilo="texto">{e.motivo}</Texto>
      </View>
      <Boton etiqueta="Empezar" onPress={() => router.push('/sesion')} />
    </>
  );
}

export function Ejercicio({ dia, idExterno }: PropsEjercicio) {
  const router = useRouter();
  return (
    <Pantalla hueco="s3">
      <BotonIcono icono="atras" etiqueta="Volver" onPress={() => router.back()} />
      <ConPlan>{(plan) => <Detalle plan={plan} dia={dia} idExterno={idExterno} />}</ConPlan>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    ficha: {
      flexDirection: 'row',
      gap: tema.space.s3,
      padding: tema.space.s4,
      borderRadius: tema.radius.md,
      backgroundColor: tema.color.surface,
      overflow: 'hidden',
    },
    fichaTextos: { flex: 1, gap: tema.space.s2 },
    figura: { width: ANCHO_FIGURA, height: ALTO_FIGURA, overflow: 'hidden' },
    cuerpo: { position: 'absolute', left: CUERPO_IZQUIERDA, top: CUERPO_ARRIBA },
    tarjeta: {
      gap: tema.space.s25,
      paddingVertical: tema.space.s35,
      paddingHorizontal: tema.space.s4,
      borderRadius: tema.radius.md,
      backgroundColor: tema.color.surface,
    },
    cabecera: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    estado: {
      paddingHorizontal: tema.space.s15,
      paddingVertical: tema.space.s05,
      borderRadius: tema.radius.sm,
      backgroundColor: tema.color.surfaceVariant,
      borderWidth: tema.border.control,
      borderColor: tema.color.onSurfaceVariant,
    },
  });
}
