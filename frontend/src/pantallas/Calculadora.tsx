import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { Boton } from '../componentes/Boton';
import { BotonIcono } from '../componentes/BotonIcono';
import { Cuerpo } from '../componentes/Cuerpo';
import { FRENTE, type Grupo } from '../componentes/cuerpoTrazados';
import type { ColoresCuerpo } from '../componentes/FiguraCuerpo';
import { Icono } from '../componentes/Icono';
import { Pantalla } from '../componentes/Pantalla';
import { Regla } from '../componentes/Regla';
import { Texto } from '../componentes/Texto';
import { EJERCICIOS_CON_ESTANDAR, SERIE_INICIAL } from '../rango/ejercicios';
import { e1rmEpley, kgConComa } from '../rango/e1rm';
import { type Theme, useTheme } from '../theme';

export const PESO = { min: 20, max: 200, paso: 0.5, separacion: 8 } as const;
export const REPETICIONES = { min: 1, max: 30, paso: 1, separacion: 16 } as const;
const CADA_MAYOR = 5;
const MINI = { lado: 52, cuerpo: 70, izquierda: -9 };
const GRANDE = { lado: 88, cuerpo: 112, izquierda: -12 };
const FLECHA = 20;

const marcaPeso = (v: number) => (v % CADA_MAYOR === 0 ? 'mayor' : v % 1 === 0 ? 'media' : 'menor');
const marcaReps = (v: number) => (v % CADA_MAYOR === 0 ? 'mayor' : 'menor');

/** El músculo del ejercicio en carmesí y el resto del frente apagado, sin el trapecio (como el boceto). */
function coloresDe(grupo: Grupo, tema: Theme): ColoresCuerpo {
  const vistos = FRENTE.grupos.map((g) => g.grupo).filter((g) => g !== 'trapecio');
  return Object.fromEntries(vistos.map((g) => [g, g === grupo ? tema.color.primary : tema.cuerpo.neutro]));
}

function Miniatura({ grupo, arriba, medida }: { grupo: Grupo; arriba: number; medida: typeof MINI }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  return (
    <View style={[estilos.mini, { width: medida.lado, height: medida.lado }]}>
      <View style={{ position: 'absolute', left: medida.izquierda, top: arriba }}>
        <Cuerpo ancho={medida.cuerpo} girable={false} colores={coloresDe(grupo, tema)} decorativo />
      </View>
    </View>
  );
}

export function Calculadora() {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [elegido, setElegido] = useState<number | null>(null);
  const [peso, setPeso] = useState<number>(SERIE_INICIAL.peso);
  const [reps, setReps] = useState<number>(SERIE_INICIAL.repeticiones);
  const actual = elegido === null ? undefined : EJERCICIOS_CON_ESTANDAR[elegido];

  return (
    <Pantalla hueco="s2" fija={actual !== undefined}>
      <View style={estilos.progreso}>
        <BotonIcono icono="atras" etiqueta="Atrás" onPress={() => (actual ? setElegido(null) : router.back())} />
        <View accessible accessibilityLabel={`Paso ${actual ? 2 : 1} de 2`} style={estilos.barras}>
          <View style={[estilos.barra, estilos.barraHecha]} />
          <View style={[estilos.barra, actual && estilos.barraHecha]} />
        </View>
        <View style={estilos.hueco} />
      </View>
      {actual ? (
        <View style={estilos.serie}>
          <Texto estilo="subtitulo" encabezado>Dinos tu mejor serie</Texto>
          <Miniatura grupo={actual.grupo} arriba={actual.desplazamientoGrande} medida={GRANDE} />
          <Texto estilo="destacado" centrado>{actual.nombre}</Texto>
          <View style={estilos.linea} />
          <Texto estilo="etiquetaEstrecha" tono="secundario">Peso</Texto>
          <Texto estilo="cifra">{kgConComa(peso)}<Texto estilo="body" peso="700"> kg</Texto></Texto>
          <Regla etiqueta="Peso en kilos" valor={peso} {...PESO} marca={marcaPeso} textoValor={`${kgConComa(peso)} kilos`} alCambiar={setPeso} />
          <Texto estilo="etiquetaEstrecha" tono="secundario">Repeticiones</Texto>
          <Texto estilo="cifra">{reps}<Texto estilo="body" peso="700"> reps</Texto></Texto>
          <Regla etiqueta="Repeticiones" valor={reps} {...REPETICIONES} marca={marcaReps} textoValor={`${reps} repeticiones`} alCambiar={setReps} />
          <Texto estilo="data" tono="secundario">{`e1RM ≈ ${e1rmEpley(peso, reps)} kg · Epley`}</Texto>
          <View style={estilos.pie}>
            <Boton
              etiqueta="Obtener mi rango"
              grande
              onPress={() => router.push({ pathname: '/rango', params: { ejercicio: actual.nombre, levantamiento: actual.levantamiento, peso: String(peso), reps: String(reps) } })}
            />
          </View>
        </View>
      ) : (
        <>
          <Texto estilo="pantalla" encabezado>Elige un ejercicio y dinos tu mejor serie</Texto>
          <Texto estilo="texto" tono="secundario">En 20 segundos tienes tu rango estimado frente a quienes compiten en powerlifting.</Texto>
          <View accessibilityRole="list">
            {EJERCICIOS_CON_ESTANDAR.map((e, i) => (
              <Pressable
                key={e.nombre}
                accessibilityRole="button"
                accessibilityLabel={`${e.nombre}. ${e.musculo}`}
                onPress={() => setElegido(i)}
                style={({ pressed }) => [estilos.fila, pressed && estilos.pulsada]}
              >
                <Miniatura grupo={e.grupo} arriba={e.desplazamiento} medida={MINI} />
                <View style={estilos.textos}>
                  <Texto estilo="body" peso="700">{e.nombre}</Texto>
                  <Texto estilo="mini" tono="secundario">{e.musculo}</Texto>
                </View>
                <Icono nombre="flecha" tam={FLECHA} color={tema.color.onSurfaceVariant} />
              </Pressable>
            ))}
          </View>
        </>
      )}
    </Pantalla>
  );
}

const ALTO_FILA = 72;
const ALTO_BARRA = 4;

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    progreso: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s3 },
    barras: { flex: 1, flexDirection: 'row', gap: tema.space.s15 },
    barra: { flex: 1, height: ALTO_BARRA, borderRadius: ALTO_BARRA / 2, backgroundColor: tema.color.surfaceVariant },
    barraHecha: { backgroundColor: tema.color.primary },
    hueco: { width: tema.hit.icono },
    serie: { flex: 1, alignItems: 'center', gap: tema.space.s25 },
    linea: { alignSelf: 'stretch', height: tema.border.fino, backgroundColor: tema.color.surfaceVariant },
    pie: { marginTop: 'auto', alignSelf: 'stretch' },
    fila: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: tema.space.s35,
      minHeight: ALTO_FILA,
      paddingVertical: tema.space.s2,
      borderBottomWidth: tema.border.fino,
      borderBottomColor: tema.color.surfaceVariant,
    },
    pulsada: { backgroundColor: tema.color.surface },
    textos: { flex: 1, gap: tema.space.s05 },
    mini: { borderRadius: tema.radius.mini, backgroundColor: tema.color.surfaceHigh, overflow: 'hidden' },
  });
}
