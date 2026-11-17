import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { Pressable, StyleSheet, View } from 'react-native';

import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { BotonIcono } from '../componentes/BotonIcono';
import { Cabecera } from '../componentes/Cabecera';
import { Emblema } from '../componentes/Emblema';
import { Icono } from '../componentes/Icono';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { PERFIL, RACHA, RANGO_GLOBAL } from '../datos/ejemplo';
import { mensajeDeError } from '../api/errores';
import { nombreRangoMayusculas } from '../rango/rango';
import { useSesion } from '../sesion/sesion';
import { type Theme, useTheme } from '../theme';

const EMBLEMA = 64;
const ICONO_BORRAR = 18;

interface Opcion {
  etiqueta: string;
  valor: string;
  dato?: boolean;
  pendiente?: boolean;
}

function Grupo({ titulo, opciones, peligro }: { titulo: string; opciones: readonly Opcion[]; peligro?: string }) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const ir = (titulo: string) => router.push({ pathname: '/proximamente', params: { titulo } });
  return (
    <>
      <Texto estilo="etiquetaEstrecha" tono="secundario" encabezado>{titulo}</Texto>
      <View style={estilos.grupo}>
        {opciones.map((o, i) => (
          <Pressable
            key={o.etiqueta}
            accessibilityRole="button"
            accessibilityLabel={o.valor ? `${o.etiqueta}: ${o.valor}` : o.etiqueta}
            onPress={() => ir(o.etiqueta)}
            style={({ pressed }) => [estilos.opcion, i > 0 && estilos.separada, pressed && estilos.pulsada]}
          >
            <Texto estilo="body">{o.etiqueta}</Texto>
            <Texto estilo={o.dato ? 'dataMedio' : 'texto'} color={o.pendiente ? tema.color.warning : tema.color.onSurfaceVariant}>
              {`${o.valor} ›`.trim()}
            </Texto>
          </Pressable>
        ))}
        {peligro ? (
          <Pressable
            accessibilityRole="button"
            onPress={() => ir(peligro)}
            style={({ pressed }) => [estilos.opcion, estilos.separada, pressed && estilos.pulsada]}
          >
            <View style={estilos.peligro}>
              <Icono nombre="papelera" tam={ICONO_BORRAR} color={tema.color.error} />
              <Texto estilo="body" color={tema.color.error}>{peligro}</Texto>
            </View>
            <Texto estilo="texto" color={tema.color.error}>›</Texto>
          </Pressable>
        ) : null}
      </View>
    </>
  );
}

export function Perfil() {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const { salir } = useSesion();
  const cierre = useMutation({ mutationFn: salir });
  const g = RANGO_GLOBAL;
  const estadisticas = [
    { valor: PERFIL.sesiones, texto: 'sesiones' },
    { valor: RACHA.semanas, texto: 'semanas de racha', acento: true },
    { valor: PERFIL.toneladas, texto: 't movidas' },
  ];

  return (
    <Pantalla bordes={['top']} hueco="s2">
      <Cabecera
        ejemplo
        derecha={
          <BotonIcono icono="ajustes" etiqueta="Ajustes" onPress={() => router.push({ pathname: '/proximamente', params: { titulo: 'Ajustes' } })} />
        }
      />
      <View style={estilos.tarjeta}>
        <View style={estilos.fila}>
          <Emblema tipo={g.tipo} grado={g.grado} tam={EMBLEMA} />
          <View style={estilos.textos}>
            <Texto estilo="pantalla" encabezado>Tu perfil</Texto>
            <Texto estilo="texto" peso="700" color={tema.rango.acero.texto}>{`${nombreRangoMayusculas(g.tipo, g.grado)} · top ${g.top} %`}</Texto>
            <Texto estilo="mini" tono="secundario">{PERFIL.resumen}</Texto>
          </View>
        </View>
        <View style={estilos.estadisticas}>
          {estadisticas.map((e, i) => (
            <View key={e.texto} style={[estilos.estadistica, i === 1 && estilos.central]}>
              <Texto estilo="cifraPequena" color={e.acento ? tema.destaque.titulo : undefined}>{e.valor}</Texto>
              <Texto estilo="mini" tono="secundario">{e.texto}</Texto>
            </View>
          ))}
        </View>
      </View>
      <Grupo titulo="Entreno" opciones={PERFIL.entreno} />
      <Grupo titulo="Personaliza" opciones={PERFIL.personaliza} />
      <Grupo titulo="Privacidad" opciones={PERFIL.privacidad} peligro="Borrar mi cuenta" />
      {cierre.isError ? <Aviso tipo="error" mensaje={mensajeDeError(cierre.error)} /> : null}
      <Boton
        variante="secundario"
        etiqueta="Cerrar sesión"
        etiquetaCargando="Cerrando sesión…"
        cargando={cierre.isPending}
        onPress={() => cierre.mutate()}
      />
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    tarjeta: {
      gap: tema.space.s35,
      padding: tema.space.s4,
      borderRadius: tema.radius.md,
      backgroundColor: tema.destaque.fondo,
      borderWidth: tema.border.fino,
      borderColor: tema.rango.acero.borde,
    },
    fila: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s35 },
    textos: { gap: tema.space.s075 },
    estadisticas: { flexDirection: 'row' },
    estadistica: { flex: 1, alignItems: 'center', gap: tema.space.s05 },
    central: { borderLeftWidth: tema.border.fino, borderRightWidth: tema.border.fino, borderColor: tema.rango.acero.borde },
    grupo: { borderRadius: tema.radius.md, backgroundColor: tema.color.surface },
    opcion: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: tema.space.s2,
      minHeight: tema.hit.min,
      paddingHorizontal: tema.space.s35,
    },
    separada: { borderTopWidth: tema.border.fino, borderTopColor: tema.color.surfaceVariant },
    pulsada: { backgroundColor: tema.color.surfaceHigh },
    peligro: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s2 },
  });
}
