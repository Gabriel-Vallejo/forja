import { useRouter } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import { AvisoEjemplo } from '../componentes/AvisoEjemplo';
import { BotonIcono } from '../componentes/BotonIcono';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { RACHA } from '../datos/ejemplo';
import { type Theme, useTheme } from '../theme';

const DIAS = ['L', '', 'X', '', 'V', '', 'D'];
const DIAS_SEMANA = DIAS.length;
const ALTO_BARRA = 10;
const MUESTRA = 12;
const ANCHO_DIAS = 14;
const ANCHO_TEXTO_RACHA = 220;

export function Racha() {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const { estaSemana } = RACHA;
  const columnas = Array.from({ length: RACHA.semanasMapa }, (_, s) => RACHA.celdas.slice(s * DIAS_SEMANA, (s + 1) * DIAS_SEMANA));
  const estadisticas = [
    { valor: String(RACHA.mejor), texto: 'Mejor racha' },
    { valor: String(RACHA.diasEntrenados), texto: 'Días entrenados' },
    { valor: `${RACHA.cumplidos} %`, texto: 'Días cumplidos' },
  ];

  return (
    <Pantalla hueco="s4">
      <Text style={estilos.kanji} accessibilityElementsHidden importantForAccessibility="no">炎</Text>
      <BotonIcono icono="atras" etiqueta="Volver" onPress={() => router.back()} />
      <AvisoEjemplo />
      <View>
        <Texto estilo="cifraGigante" encabezado>{RACHA.semanas}</Texto>
        <Texto estilo="heading">Semanas de racha</Texto>
        <Texto estilo="nota" tono="secundario" estiloExtra={estilos.explicacion}>
          Cuenta la semana si cumples tus días. Descansar no la rompe.
        </Texto>
      </View>
      <View style={estilos.bloque}>
        <Texto estilo="etiqueta" tono="secundario">{`Esta semana · ${estaSemana.hechos} de ${estaSemana.objetivo}`}</Texto>
        <View style={estilos.barras} accessible accessibilityLabel={`${estaSemana.hechos} de ${estaSemana.objetivo} días hechos`}>
          {Array.from({ length: estaSemana.objetivo }, (_, i) => (
            <View key={i} style={[estilos.barra, i < estaSemana.hechos ? estilos.hecha : estilos.pendiente]} />
          ))}
        </View>
      </View>
      <View style={estilos.estadisticas}>
        {estadisticas.map((e) => (
          <View key={e.texto} style={estilos.estadistica}>
            <Texto estilo="cifraMedia">{e.valor}</Texto>
            <Texto estilo="nota" tono="secundario">{e.texto}</Texto>
          </View>
        ))}
      </View>
      <View style={estilos.mapa} accessibilityLabel="Últimas 12 semanas">
        <View style={estilos.entre}>
          <Texto estilo="body" peso="700">Últimas 12 semanas</Texto>
          <Texto estilo="dataMini" tono="secundario">{RACHA.periodo}</Texto>
        </View>
        <View style={estilos.rejilla}>
          <View style={estilos.columnaDias}>
            {DIAS.map((d, i) => (
              <View key={i} style={estilos.celdaDia}>
                <Texto estilo="micro" tono="secundario">{d}</Texto>
              </View>
            ))}
          </View>
          <View style={estilos.semanas} accessible accessibilityRole="image" accessibilityLabel="Días entrenados: casi todas las semanas, 3 días">
            {columnas.map((semana, s) => (
              <View key={s} style={estilos.semana}>
                {semana.map((hecho, d) => (
                  <View key={d} style={[estilos.celda, hecho ? estilos.hecha : estilos.descanso]} />
                ))}
              </View>
            ))}
          </View>
        </View>
        <View style={estilos.leyenda}>
          {[{ t: 'Entrenado', e: estilos.hecha }, { t: 'Descanso', e: estilos.descanso }].map((l) => (
            <View key={l.t} style={estilos.leyendaFila}>
              <View style={[estilos.muestra, l.e]} />
              <Texto estilo="mini" tono="secundario">{l.t}</Texto>
            </View>
          ))}
        </View>
      </View>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  const glow = tema.shadow.glowPrimary;
  return StyleSheet.create({
    kanji: {
      position: 'absolute',
      right: tema.space.s4,
      top: tema.space.s12,
      fontFamily: tema.font.kanji,
      fontSize: tema.kanji.xxl,
      lineHeight: tema.kanji.xxl,
      color: tema.color.primary,
      textShadowColor: glow.color,
      textShadowRadius: glow.radius,
    },
    explicacion: { maxWidth: ANCHO_TEXTO_RACHA },
    bloque: { gap: tema.space.s2 },
    barras: { flexDirection: 'row', gap: tema.space.s2 },
    barra: { flex: 1, height: ALTO_BARRA, borderRadius: ALTO_BARRA / 2 },
    hecha: { backgroundColor: tema.color.primary },
    pendiente: {
      backgroundColor: tema.color.surfaceVariant,
      borderWidth: tema.border.control,
      borderStyle: 'dashed',
      borderColor: tema.color.onSurfaceVariant,
    },
    descanso: { backgroundColor: tema.color.surfaceVariant },
    estadisticas: { flexDirection: 'row', gap: tema.space.s25 },
    estadistica: { flex: 1, padding: tema.space.s35, borderRadius: tema.radius.md, backgroundColor: tema.color.surface, gap: tema.space.s05 },
    mapa: { padding: tema.space.s35, borderRadius: tema.radius.md, backgroundColor: tema.color.surface, gap: tema.space.s25 },
    entre: { flexDirection: 'row', justifyContent: 'space-between' },
    rejilla: { flexDirection: 'row', gap: tema.space.s15 },
    columnaDias: { width: ANCHO_DIAS, gap: tema.space.s1 },
    celdaDia: { flex: 1, justifyContent: 'center' },
    semanas: { flex: 1, flexDirection: 'row', gap: tema.space.s1 },
    semana: { flex: 1, gap: tema.space.s1 },
    celda: { aspectRatio: 1, borderRadius: tema.radius.celda },
    leyenda: { flexDirection: 'row', gap: tema.space.s3 },
    leyendaFila: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s15 },
    muestra: { width: MUESTRA, height: MUESTRA, borderRadius: tema.radius.celda },
  });
}
