import { useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import type { PlanActual, Razon } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { BotonIcono } from '../componentes/BotonIcono';
import { Chips } from '../componentes/Chips';
import { ConConsulta } from '../componentes/ConConsulta';
import { CuerpoDoble } from '../componentes/CuerpoDoble';
import { InsigniaNivel } from '../componentes/InsigniaNivel';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { etiquetaMusculo } from '../plan/musculos';
import { NOMBRE_ESTADO, numeroDeOrden, prescripcion } from '../plan/plan';
import { usePorQue } from '../plan/porQue';
import { useCargasSiguientes } from '../registro/sincronizacion';
import { type Theme, useTheme } from '../theme';
import { ConPlan } from './ConPlan';

// Frente y espalda lado a lado: así se ve cualquier músculo, también gemelos o espalda.
const ANCHO_CUERPO = 56;

interface PropsEjercicio {
  dia: number;
  idExterno: string;
}

function Detalle({ plan, dia, idExterno }: PropsEjercicio & { plan: PlanActual }) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const cargas = useCargasSiguientes(plan);
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
          <Texto estilo="data" tono="secundario">{prescripcion(e, cargas.get(e.idExterno))}</Texto>
          <Chips textos={e.musculos.map(etiquetaMusculo)} />
        </View>
        <CuerpoDoble musculos={e.musculos} ancho={ANCHO_CUERPO} />
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
      <PorQue plan={plan.uuid} dia={dia} idExterno={idExterno} />
      <Boton etiqueta="Empezar" onPress={() => router.push('/sesion')} />
      <Ajustar idExterno={idExterno} />
    </>
  );
}

const AJUSTES_EJERCICIO = [
  { tipo: 'mas', etiqueta: 'Una serie más' },
  { tipo: 'menos', etiqueta: 'Una serie menos' },
  { tipo: 'reemplazar', etiqueta: 'Reemplazar' },
  { tipo: 'no_recomendar', etiqueta: 'No recomendar' },
] as const;

/** Ajustes del ejercicio (PRD v0.1): cada uno abre la vista previa del plan con el cambio, sin guardar nada. */
function Ajustar({ idExterno }: { idExterno: string }) {
  const router = useRouter();
  const estilos = crearEstilos(useTheme());
  return (
    <View style={estilos.tarjeta} accessibilityLabel="Ajustar este ejercicio">
      <Texto estilo="etiquetaEstrecha" tono="secundario">Ajustar este ejercicio</Texto>
      <Texto estilo="nota" tono="secundario">Antes de guardar verás cómo queda el plan.</Texto>
      {AJUSTES_EJERCICIO.map((a) => (
        <Boton
          key={a.tipo}
          variante="secundario"
          etiqueta={a.etiqueta}
          onPress={() => router.push({ pathname: '/vista-previa', params: { tipo: a.tipo, id: idExterno } })}
        />
      ))}
    </View>
  );
}

/** «Por qué este ejercicio»: cada razón con su fuente y su nivel (PRD v0.1). */
function PorQue({ plan, dia, idExterno }: PropsEjercicio & { plan: string }) {
  const estilos = crearEstilos(useTheme());
  const consulta = usePorQue(plan, dia, idExterno);
  return (
    <View style={estilos.tarjeta} accessibilityLabel="Por qué este ejercicio">
      <Texto estilo="etiquetaEstrecha" tono="secundario">Por qué este ejercicio</Texto>
      <ConConsulta consulta={consulta} cargando="Buscando por qué…">
        {(razones) =>
          razones.length === 0 ? (
            <Texto tono="secundario">No hay razones guardadas para este ejercicio.</Texto>
          ) : (
            razones.map((r) => <FilaRazon key={`${r.codigo}-${r.texto}`} razon={r} />)
          )
        }
      </ConConsulta>
    </View>
  );
}

function FilaRazon({ razon }: { razon: Razon }) {
  const estilos = crearEstilos(useTheme());
  return (
    <View style={estilos.razon}>
      <Texto estilo="texto">{razon.texto}</Texto>
      <View style={estilos.pie}>
        <Texto estilo="mini" tono="secundario" estiloExtra={estilos.fuente}>{razon.fuente}</Texto>
        <InsigniaNivel nivel={razon.nivel} />
      </View>
    </View>
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
    tarjeta: {
      gap: tema.space.s25,
      paddingVertical: tema.space.s35,
      paddingHorizontal: tema.space.s4,
      borderRadius: tema.radius.md,
      backgroundColor: tema.color.surface,
    },
    cabecera: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    razon: { gap: tema.space.s1 },
    pie: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s2 },
    fuente: { flexShrink: 1 },
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
