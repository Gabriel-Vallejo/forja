import { useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { BotonIcono } from '../componentes/BotonIcono';
import { Emblema, type Grado, type TipoRango } from '../componentes/Emblema';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { NIVELES_DE_ARRIBA_ABAJO } from '../datos/ejemplo';
import { useRangos } from '../rango/consultas';
import { nombreRango, topDe } from '../rango/rango';
import { type Theme, useTheme } from '../theme';

const TAM_POR_GRADO: Record<Grado, number> = { 1: 36, 2: 42, 3: 48 };
const HUECO_EMBLEMA = 52;
const ALTO_FILA = 52;
const GRADOS_DE_ARRIBA_ABAJO: readonly Grado[] = [3, 2, 1];

const ESCALONES: readonly { tipo: TipoRango; grado: Grado }[] = [
  { tipo: 'forjado', grado: 3 },
  ...NIVELES_DE_ARRIBA_ABAJO.flatMap((tipo) => GRADOS_DE_ARRIBA_ABAJO.map((grado) => ({ tipo, grado }))),
];

export function Escala() {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const global = useRangos().global;

  return (
    <Pantalla hueco="s1" fondo={tema.color.surface}>
      <View style={estilos.arriba}>
        <View style={estilos.asa} />
        <View style={estilos.cerrar}>
          <BotonIcono icono="cerrar" etiqueta="Cerrar" onPress={() => router.back()} />
        </View>
      </View>
      <Texto estilo="pantalla" encabezado centrado>Escala de rangos</Texto>
      <Texto estilo="nota" tono="secundario" centrado>
        {'Como el metal en la forja: cuanto más arriba, más caliente.\nPercentil estimado entre quienes compiten en powerlifting.'}
      </Texto>
      <View accessibilityRole="list" style={estilos.lista}>
        {ESCALONES.map(({ tipo, grado }) => {
          const yo = global !== null && tipo === global.tipo && (tipo === 'forjado' || grado === global.grado);
          const top = topDe(tipo === 'forjado' ? 'forjado' : tipo, grado);
          const nombre = nombreRango(tipo, grado);
          return (
            <View
              key={`${tipo}-${grado}`}
              accessible
              accessibilityLabel={`${nombre}, top ${top} %${yo ? ', tu rango' : ''}`}
              style={[estilos.fila, yo && estilos.filaYo]}
            >
              <View style={estilos.emblema}>
                <Emblema tipo={tipo} grado={grado} tam={tipo === 'forjado' ? TAM_POR_GRADO[3] : TAM_POR_GRADO[grado]} />
              </View>
              <Texto estilo="seccion" peso={yo ? '800' : '700'} color={yo ? tema.color.secondary : undefined} estiloExtra={estilos.flex}>
                {nombre}
              </Texto>
              <Texto estilo="dataMedio" color={yo ? tema.color.secondary : tema.color.onSurfaceVariant}>{`Top ${top} %`}</Texto>
            </View>
          );
        })}
      </View>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    arriba: { alignItems: 'center', minHeight: tema.hit.icono, justifyContent: 'center' },
    asa: { width: tema.hit.icono, height: tema.space.s1, borderRadius: tema.radius.xs, backgroundColor: tema.color.outline },
    cerrar: { position: 'absolute', right: 0, top: 0 },
    lista: { gap: tema.space.s05 },
    fila: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: tema.space.s4,
      minHeight: ALTO_FILA,
      paddingHorizontal: tema.space.s35,
      borderRadius: tema.radius.md,
      borderWidth: tema.border.fino,
      borderColor: 'transparent',
    },
    filaYo: { backgroundColor: tema.destaque.fondo, borderColor: tema.color.primary },
    emblema: { width: HUECO_EMBLEMA, alignItems: 'center' },
    flex: { flex: 1 },
  });
}
