import { useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import Svg, { Circle, G, Line, Path, Text as TextoSvg } from 'react-native-svg';

import { Aviso } from '../componentes/Aviso';
import { mensajeDeError } from '../api/errores';
import { Boton } from '../componentes/Boton';
import { BotonIcono } from '../componentes/BotonIcono';
import { Emblema } from '../componentes/Emblema';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { useConsentimientoMedidas } from '../movilidad/consultas';
import { useMedidas, usePerfilDatos } from '../perfil/consultas';
import { useEstandares } from '../rango/consultas';
import { e1rmEpley, kgConComa } from '../rango/e1rm';
import { type Estimacion, estimar, type Levantamiento, tablaPara } from '../rango/estandares';
import { nombreRangoMayusculas } from '../rango/rango';
import { letra, type Theme, useTheme } from '../theme';

// Rayos de fondo, círculo y curva del boceto (RangoDetalle.dc.html), en sus coordenadas.
const CENTRO_RAYOS = { x: 195, y: 230 };
const PUNTAS_RAYOS = [
  [195, -40], [330, -10], [430, 90], [440, 230], [430, 370], [330, 480],
  [195, 520], [60, 480], [-40, 370], [-50, 230], [-40, 90], [60, -10],
] as const;
const OPACIDAD_RAYOS = 0.12;
const RAYOS_ARRIBA = 60;
const DIAMETRO = 250;
const EMBLEMA = 190;
const CURVA = { ancho: 358, alto: 110 };
const GUIAS = [60, 120, 179, 238, 298];
const CURVA_D = 'M2 100 C 70 100, 112 10, 179 10 S 288 100, 356 100';
const MARCA = { r: 7, sobre: 18 };
// La marca «Tú» va sobre la curva según el percentil. La curva es la del boceto, una campana entre x 2 y 356 con el
// pico en 179; su forma no es la de los datos, solo dice dónde caes.
const CURVA_X = { inicio: 2, fin: 356, pico: 179, ancho: 62 };
const CURVA_Y = { base: 100, pico: 10 };

function puntoEnCurva(percentil: number): { x: number; y: number } {
  const x = CURVA_X.inicio + (percentil / 100) * (CURVA_X.fin - CURVA_X.inicio);
  const campana = Math.exp(-((x - CURVA_X.pico) ** 2) / (2 * CURVA_X.ancho ** 2));
  return { x, y: CURVA_Y.base - (CURVA_Y.base - CURVA_Y.pico) * campana };
}
const TAM_TU = 14;

interface PropsRangoDetalle {
  ejercicio: string;
  levantamiento: Levantamiento;
  peso: number;
  repeticiones: number;
}

/** Resultado de la calculadora: el percentil sale de las tablas de OpenPowerlifting con tu sexo y tu peso. */
export function RangoDetalle({ ejercicio, levantamiento, peso, repeticiones }: PropsRangoDetalle) {
  const router = useRouter();
  const estandares = useEstandares();
  const datos = usePerfilDatos();
  const consentimiento = useConsentimientoMedidas();
  const medidas = useMedidas(consentimiento.data === true);
  const cargando =
    estandares.isPending || datos.isPending || consentimiento.isPending || (consentimiento.data === true && medidas.isPending);
  const error = estandares.error ?? datos.error;

  if (cargando || error) {
    return (
      <Pantalla titulo="Rango estimado">
        {error ? (
          <>
            <Aviso tipo="error" mensaje={mensajeDeError(error)} />
            <Boton variante="secundario" etiqueta="Reintentar" onPress={() => void Promise.all([estandares.refetch(), datos.refetch()])} />
          </>
        ) : (
          <Texto tono="secundario">Cargando…</Texto>
        )}
      </Pantalla>
    );
  }
  const sexo = datos.data?.sexo;
  const pesoCorporal = medidas.data?.pesoKg ?? null;
  const falta = [sexo === 'hombre' || sexo === 'mujer' ? null : 'tu sexo', pesoCorporal ? null : 'tu peso'].filter(
    (f): f is string => f !== null,
  );
  if (falta.length > 0 || (sexo !== 'hombre' && sexo !== 'mujer') || !pesoCorporal) {
    return (
      <Pantalla titulo="Rango estimado">
        <Texto>{`Para compararte nos falta ${falta.join(' y ')}: los estándares de fuerza van por sexo y peso corporal.`}</Texto>
        <Boton etiqueta="Completar mi perfil" onPress={() => router.push('/perfil-datos')} />
        <Boton variante="secundario" etiqueta="Volver" onPress={() => router.back()} />
      </Pantalla>
    );
  }
  const tabla = tablaPara(estandares.data ?? [], levantamiento, sexo, pesoCorporal);
  if (!tabla) {
    return (
      <Pantalla titulo="Rango estimado">
        <Texto>No hay suficientes personas de tu sexo y tu peso en OpenPowerlifting para darte un percentil con este ejercicio.</Texto>
        <Boton variante="secundario" etiqueta="Volver" onPress={() => router.back()} />
      </Pantalla>
    );
  }
  return (
    <Resultado ejercicio={ejercicio} peso={peso} repeticiones={repeticiones}
      estimacion={estimar(tabla, e1rmEpley(peso, repeticiones))} />
  );
}

interface PropsResultado {
  ejercicio: string;
  peso: number;
  repeticiones: number;
  estimacion: Estimacion;
}

function Resultado({ ejercicio, peso, repeticiones, estimacion: r }: PropsResultado) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const rango = nombreRangoMayusculas(r.tipo, r.grado);
  const marca = puntoEnCurva(r.percentil);

  return (
    <Pantalla hueco="s25" fondo={tema.destaque.resultado}>
      <View style={estilos.rayos} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
        <Svg width="100%" height="100%" viewBox="0 0 390 520" preserveAspectRatio="xMidYMin slice">
          <G stroke={tema.color.primary} strokeWidth={2} opacity={OPACIDAD_RAYOS}>
            {PUNTAS_RAYOS.map(([x, y]) => (
              <Line key={`${x},${y}`} x1={CENTRO_RAYOS.x} y1={CENTRO_RAYOS.y} x2={x} y2={y} />
            ))}
          </G>
        </Svg>
      </View>
      <View style={estilos.cabecera}>
        <BotonIcono icono="cerrar" etiqueta="Cerrar" relleno onPress={() => router.back()} />
        <View style={estilos.pastilla}>
          <Texto estilo="etiquetaEstrecha">Rango estimado</Texto>
        </View>
        <View style={estilos.hueco} />
      </View>
      <View style={estilos.centro}>
        <Texto estilo="rango" color={tema.destaque.titulo} encabezado centrado>{rango}</Texto>
        <View style={estilos.circulo}>
          <Emblema tipo={r.tipo} grado={r.grado} tam={EMBLEMA} />
        </View>
        <Texto estilo="destacado" centrado>{ejercicio}</Texto>
        <Texto estilo="data" tono="secundario" centrado>
          {`${kgConComa(peso)} kg × ${repeticiones} reps · e1RM ≈ ${e1rmEpley(peso, repeticiones)} kg`}
        </Texto>
        <Svg width={CURVA.ancho} height={CURVA.alto} viewBox={`0 0 ${CURVA.ancho} ${CURVA.alto}`}
          accessibilityLabel={`Tu marca, por encima del ${r.percentil} % de la curva`}>
          <G stroke={tema.color.surfaceVariant} strokeDasharray="2 4">
            {GUIAS.map((x) => <Line key={x} x1={x} y1={8} x2={x} y2={100} />)}
          </G>
          <Path d={`${CURVA_D} Z`} fill={tema.destaque.curva} />
          <Path d={CURVA_D} fill="none" stroke={tema.color.primary} strokeWidth={2.5} />
          <Circle cx={marca.x} cy={marca.y} r={MARCA.r} fill={tema.color.primary} stroke={tema.destaque.resultado} strokeWidth={2} />
          <TextoSvg x={marca.x} y={marca.y - MARCA.sobre} textAnchor="middle" fontFamily={letra(tema, 'sans', '800').fontFamily}
            fontSize={TAM_TU} fill={tema.color.onBackground}>Tú</TextoSvg>
        </Svg>
      </View>
      <View style={estilos.entre}>
        <Texto estilo="etiquetaEstrecha" tono="secundario">Otras personas</Texto>
        <Texto estilo="dataMedio" peso="700" color={tema.destaque.titulo}>{`TOP ${r.top} %`}</Texto>
      </View>
      <Texto estilo="nota" tono="secundario" centrado>
        {`Frente a ${r.tabla.muestra.toLocaleString('es-ES')} personas de tu sexo y tu peso que compiten en powerlifting sin equipación. Es un listón alto: quien compite levanta más que la media del gimnasio. Datos de OpenPowerlifting (openpowerlifting.org), de dominio público.`}
      </Texto>
      <Boton etiqueta="Empezar a entrenar" grande onPress={() => router.navigate('/entreno')} />
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  const glow = tema.shadow.glowPrimary;
  return StyleSheet.create({
    rayos: { ...StyleSheet.absoluteFill, top: RAYOS_ARRIBA },
    cabecera: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    pastilla: {
      paddingHorizontal: tema.space.s3,
      paddingVertical: tema.space.s15,
      borderRadius: tema.radius.hoja,
      backgroundColor: tema.color.surfaceHigh,
    },
    hueco: { width: tema.hit.icono },
    centro: { alignItems: 'center', gap: tema.space.s25 },
    circulo: {
      width: DIAMETRO,
      height: DIAMETRO,
      borderRadius: DIAMETRO / 2,
      borderWidth: tema.border.marca,
      borderColor: tema.color.primary,
      backgroundColor: tema.destaque.circulo,
      alignItems: 'center',
      justifyContent: 'center',
      shadowColor: glow.color,
      shadowOpacity: glow.opacity,
      shadowRadius: glow.radius,
      shadowOffset: { width: 0, height: 0 },
    },
    entre: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  });
}
