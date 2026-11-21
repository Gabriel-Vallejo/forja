import { useRouter } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import type { ResumenProgreso } from '../api/esquemas';
import { BotonIcono } from '../componentes/BotonIcono';
import { ConConsulta } from '../componentes/ConConsulta';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { useResumenProgreso } from '../progreso/resumen';
import { type Theme, useTheme } from '../theme';

const DIAS = ['L', '', 'X', '', 'V', '', 'D'];
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const DOMINGO = 6;
const ALTO_BARRA = 10;
const MUESTRA = 12;
const ANCHO_DIAS = 14;
const ANCHO_TEXTO_RACHA = 220;

/** «2026-11-16» → «16 nov», sin pasar por la zona horaria (la fecha ya viene en la del móvil). */
function diaMes(fecha: string, masDias = 0): string {
  const [a, m, d] = fecha.split('-').map(Number) as [number, number, number];
  const dia = new Date(Date.UTC(a, m - 1, d + masDias));
  return `${dia.getUTCDate()} ${MESES[dia.getUTCMonth()]}`;
}

export function Racha() {
  const router = useRouter();
  const estilos = crearEstilos(useTheme());
  const consulta = useResumenProgreso();
  return (
    <Pantalla hueco="s4">
      <Text style={estilos.kanji} accessibilityElementsHidden importantForAccessibility="no">炎</Text>
      <BotonIcono icono="atras" etiqueta="Volver" onPress={() => router.back()} />
      <ConConsulta consulta={consulta} cargando="Contando tus semanas…">
        {(resumen) => <Contenido resumen={resumen} />}
      </ConConsulta>
    </Pantalla>
  );
}

function Contenido({ resumen }: { resumen: ResumenProgreso }) {
  const estilos = crearEstilos(useTheme());
  const columnas = resumen.ultimasSemanas.map((s) => s.dias);
  const objetivo = resumen.objetivoSemanal;
  const enMapa = columnas.flat().filter(Boolean).length;
  const primera = resumen.ultimasSemanas.at(0);
  const ultima = resumen.ultimasSemanas.at(-1);
  const periodo = primera && ultima ? `${diaMes(primera.lunes)} – ${diaMes(ultima.lunes, DOMINGO)}` : '';
  const estadisticas = [
    { valor: String(resumen.mejorRacha), texto: 'Mejor racha' },
    { valor: String(resumen.diasEntrenados), texto: 'Días entrenados' },
    { valor: `${resumen.porcentajeCumplido} %`, texto: 'Días cumplidos' },
  ];

  return (
    <>
      <View>
        <Texto estilo="cifraGigante" encabezado>{resumen.semanasDeRacha}</Texto>
        <Texto estilo="heading">{resumen.semanasDeRacha === 1 ? 'Semana de racha' : 'Semanas de racha'}</Texto>
        <Texto estilo="nota" tono="secundario" estiloExtra={estilos.explicacion}>
          {`Cuenta la semana si entrenas tus ${objetivo} días. Descansar no la rompe, y la semana en curso no corta la racha hasta que acaba.`}
        </Texto>
      </View>
      <View style={estilos.bloque}>
        <Texto estilo="etiqueta" tono="secundario">{`Esta semana · ${resumen.hechosEstaSemana} de ${objetivo}`}</Texto>
        <View style={estilos.barras} accessible accessibilityLabel={`${resumen.hechosEstaSemana} de ${objetivo} días hechos`}>
          {Array.from({ length: Math.max(objetivo, resumen.hechosEstaSemana) }, (_, i) => (
            <View key={i} style={[estilos.barra, i < resumen.hechosEstaSemana ? estilos.hecha : estilos.pendiente]} />
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
          <Texto estilo="dataMini" tono="secundario">{periodo}</Texto>
        </View>
        <View style={estilos.rejilla}>
          <View style={estilos.columnaDias}>
            {DIAS.map((d, i) => (
              <View key={i} style={estilos.celdaDia}>
                <Texto estilo="micro" tono="secundario">{d}</Texto>
              </View>
            ))}
          </View>
          <View style={estilos.semanas} accessible accessibilityRole="image" accessibilityLabel={`Has entrenado ${enMapa} ${enMapa === 1 ? 'día' : 'días'} en las últimas 12 semanas`}>
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
    </>
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
