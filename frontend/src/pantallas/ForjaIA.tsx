import { useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { Aviso } from '../componentes/Aviso';
import { Icono } from '../componentes/Icono';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { letra, texto, type Theme, useTheme } from '../theme';

// La pantalla tal cual el boceto (IA.dc.html), sin backend: la conversación es la del boceto.
export const LLEGA = 'Llega en la v0.2';
const PREGUNTAS = ['Cuéntame qué te limita', '¿Por qué este ejercicio y no otro?', 'Ajusta mi plan con palabras'];
const PUNTO = 8;
const ALTO_CAMPO = 50;
const ICONO_ENVIAR = 22;
const ICONO_CHECK = 16;
const TRAZO_ENVIAR = 2.4;
const TRAZO_CHECK = 3;
const ANCHO_BURBUJA = '86%';
const ANCHO_LEMA = 240;

function Cita({ texto: cita }: { texto: string }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <Texto estilo="nota" tono="secundario">
      Tu texto: «<Text style={estilos.marca}>{cita}</Text>»
    </Texto>
  );
}

export function ForjaIA() {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [mensaje, setMensaje] = useState('');
  const [aviso, setAviso] = useState(false);
  const avisar = () => setAviso(true);

  return (
    <Pantalla
      bordes={['top']}
      hueco="s3"
      pie={
        <>
          {aviso ? <Aviso tipo="info" mensaje={LLEGA} /> : null}
          <View style={estilos.escribir}>
            <TextInput
              accessibilityLabel="Escribe a Forja"
              placeholder="Escribe con tus palabras…"
              placeholderTextColor={tema.color.onSurfaceVariant}
              value={mensaje}
              onChangeText={setMensaje}
              style={estilos.campo}
            />
            <Pressable accessibilityRole="button" accessibilityLabel="Enviar" onPress={avisar} style={estilos.enviar}>
              <Icono nombre="enviar" tam={ICONO_ENVIAR} color={tema.color.onPrimary} trazo={TRAZO_ENVIAR} />
            </Pressable>
          </View>
        </>
      }
    >
      <Text style={estilos.kanji} accessibilityElementsHidden importantForAccessibility="no">問</Text>
      <View style={estilos.titulo}>
        <Texto estilo="etiquetaEstrecha" tono="secundario">{LLEGA}</Texto>
        <Texto estilo="titular" encabezado>FORJA IA</Texto>
        <Texto estilo="nota" tono="secundario" estiloExtra={estilos.lema}>
          Te escucha y te explica. Las reglas y el solver deciden; tú confirmas.
        </Texto>
      </View>
      <View style={estilos.preguntas}>
        {PREGUNTAS.map((p, i) => (
          <Pressable key={p} accessibilityRole="button" onPress={avisar} style={estilos.pregunta}>
            <View style={[estilos.punto, i === 0 && estilos.puntoActivo]} />
            <Texto estilo="texto" peso="700">{p}</Texto>
          </Pressable>
        ))}
      </View>
      <View style={estilos.burbuja}>
        <Texto estilo="texto">
          Me operaron del menisco derecho hace un año. La sentadilla profunda me molesta y el fisio me dijo que nada de saltos.
        </Texto>
      </View>
      <View style={estilos.hoja} accessibilityLabel="Esto es lo que he entendido">
        <View style={estilos.asa} />
        <Texto estilo="destacado" peso="800" encabezado>Esto es lo que he entendido</Texto>
        <View style={[estilos.chip, estilos.chipPorConfirmar]}>
          <Texto estilo="texto" peso="700">Rodilla derecha · limitar el rango en sentadilla</Texto>
          <Cita texto="la sentadilla profunda me molesta" />
          <View style={estilos.botones}>
            {['Confirmar', 'Editar', 'Quitar'].map((b, i) => (
              <Pressable key={b} accessibilityRole="button" onPress={avisar} style={[estilos.boton, i === 0 && estilos.botonPrincipal]}>
                <Texto estilo="nota" peso="700" color={i === 0 ? tema.color.onPrimary : undefined}>{b}</Texto>
              </Pressable>
            ))}
          </View>
        </View>
        <View style={[estilos.chip, estilos.chipConfirmado]}>
          <View style={estilos.confirmado} accessible accessibilityLabel="Confirmada: sin impacto, lo dijo un profesional">
            <Icono nombre="check" tam={ICONO_CHECK} trazo={TRAZO_CHECK} />
            <Texto estilo="texto" peso="700">Sin impacto · lo dijo un profesional</Texto>
          </View>
          <Cita texto="nada de saltos" />
        </View>
        <Texto estilo="mini" tono="secundario">
          Guardo la limitación que confirmas, no «menisco». Forja elige ejercicios compatibles con las limitaciones que tú indicas; no evalúa ni diagnostica.
        </Texto>
      </View>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  const glow = tema.shadow.glowPrimary;
  return StyleSheet.create({
    kanji: {
      position: 'absolute',
      right: tema.space.s35,
      top: tema.space.s2,
      fontFamily: tema.font.kanji,
      fontSize: tema.kanji.l,
      lineHeight: tema.kanji.l,
      color: tema.color.primary,
      textShadowColor: glow.color,
      textShadowRadius: glow.radius,
    },
    titulo: { gap: tema.space.s1 },
    lema: { maxWidth: ANCHO_LEMA },
    preguntas: { gap: tema.space.s15 },
    pregunta: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: tema.space.s25,
      minHeight: tema.hit.min,
      paddingHorizontal: tema.space.s35,
      borderRadius: tema.radius.md,
      backgroundColor: tema.color.surface,
    },
    punto: { width: PUNTO, height: PUNTO, borderRadius: PUNTO / 2, backgroundColor: tema.color.outline },
    puntoActivo: { backgroundColor: tema.color.primary },
    burbuja: {
      alignSelf: 'flex-end',
      maxWidth: ANCHO_BURBUJA,
      paddingVertical: tema.space.s25,
      paddingHorizontal: tema.space.s3,
      borderRadius: tema.radius.burbuja,
      borderBottomRightRadius: tema.radius.xs,
      backgroundColor: tema.color.surfaceVariant,
    },
    hoja: {
      gap: tema.space.s2,
      paddingTop: tema.space.s25,
      paddingHorizontal: tema.space.s35,
      paddingBottom: tema.space.s35,
      borderTopLeftRadius: tema.radius.hoja,
      borderTopRightRadius: tema.radius.hoja,
      borderBottomLeftRadius: tema.radius.md,
      borderBottomRightRadius: tema.radius.md,
      backgroundColor: tema.color.surface,
    },
    asa: { alignSelf: 'center', width: tema.hit.icono, height: tema.space.s1, borderRadius: tema.radius.xs, backgroundColor: tema.color.outline },
    chip: { gap: tema.space.s15, padding: tema.space.s3, borderRadius: tema.radius.md, borderWidth: tema.border.control },
    chipPorConfirmar: { borderStyle: 'dashed', borderColor: tema.color.onSurfaceVariant },
    chipConfirmado: { backgroundColor: tema.destaque.fondo, borderColor: tema.color.primary },
    confirmado: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s15 },
    marca: { backgroundColor: tema.color.primaryContainer, color: tema.color.onBackground },
    botones: { flexDirection: 'row', gap: tema.space.s15 },
    boton: {
      flex: 1,
      minHeight: tema.hit.icono,
      alignItems: 'center',
      justifyContent: 'center',
      borderRadius: tema.radius.sm,
      borderWidth: tema.border.control,
      // El boceto usa `outline`, que no llega a 3:1: el borde de un control va en `onSurfaceVariant` (§2.3).
      borderColor: tema.color.onSurfaceVariant,
    },
    botonPrincipal: { backgroundColor: tema.color.primary, borderColor: tema.color.primary },
    escribir: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s2 },
    campo: {
      ...texto(tema, 'body'),
      ...letra(tema, 'sans', '400'),
      flex: 1,
      minHeight: ALTO_CAMPO,
      paddingHorizontal: tema.space.s4,
      borderRadius: ALTO_CAMPO / 2,
      borderWidth: tema.border.control,
      borderColor: tema.color.onSurfaceVariant,
      backgroundColor: tema.color.surface,
      color: tema.color.onBackground,
    },
    enviar: {
      width: ALTO_CAMPO,
      height: ALTO_CAMPO,
      borderRadius: ALTO_CAMPO / 2,
      backgroundColor: tema.color.primary,
      alignItems: 'center',
      justifyContent: 'center',
    },
  });
}
