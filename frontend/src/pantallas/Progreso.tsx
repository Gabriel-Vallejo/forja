import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { Boton } from '../componentes/Boton';
import { Cabecera } from '../componentes/Cabecera';
import { Cuerpo } from '../componentes/Cuerpo';
import { GRUPOS, type Grupo } from '../componentes/cuerpoTrazados';
import { Emblema, NOMBRE_RANGO } from '../componentes/Emblema';
import type { ColoresCuerpo } from '../componentes/FiguraCuerpo';
import { FilaRango } from '../componentes/FilaRango';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { NIVELES_DE_ARRIBA_ABAJO, ZONAS } from '../datos/ejemplo';
import { useResumenProgreso } from '../progreso/resumen';
import { useRangos } from '../rango/consultas';
import { nombreRango, nombreRangoMayusculas, tipoEmblema } from '../rango/rango';
import type { Rangos } from '../rango/rangos';
import { type Theme, useTheme } from '../theme';

const ANCHO_CUERPO = 262;
const EMBLEMA_GLOBAL = 72;
const EMBLEMA_PANEL = 64;
const EMBLEMA_FILA = 36;
const MUESTRA = 10;
const LEYENDA = [...NIVELES_DE_ARRIBA_ABAJO].reverse();

function Pestanas() {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const otras = [
    { etiqueta: 'Calculadora', ir: () => router.push('/calculadora') },
    { etiqueta: 'Marcas', ir: () => router.push({ pathname: '/proximamente', params: { titulo: 'Marcas' } }) },
  ];
  return (
    <View accessibilityRole="tablist" accessibilityLabel="Vista" style={estilos.pestanas}>
      <View accessibilityRole="tab" accessibilityState={{ selected: true }} style={[estilos.pestana, estilos.pestanaActiva]}>
        <Texto estilo="body" peso="700">Rangos</Texto>
      </View>
      {otras.map((o) => (
        <Pressable key={o.etiqueta} accessibilityRole="tab" accessibilityState={{ selected: false }} onPress={o.ir} style={estilos.pestana}>
          <Texto estilo="body" tono="secundario">{o.etiqueta}</Texto>
        </Pressable>
      ))}
    </View>
  );
}

function Panel({ grupo, musculos }: { grupo: Grupo; musculos: Rangos['musculos'] }) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const mus = musculos[grupo];
  const paleta = tema.rango[mus.tipo];
  const top = mus.top === null ? 'Sin datos todavía' : `Top ${mus.top} % · estimación`;
  const nota = mus.ejercicios.length === 0
    ? 'Aún no has registrado ejercicios de este músculo. Tienen estándar la sentadilla, la banca y el peso muerto con barra (OpenPowerlifting), y las dominadas y los fondos (OpenStreetlifting).'
    : 'El rango del músculo sale de su mejor ejercicio con estándar: sentadilla, banca y peso muerto con barra, dominadas y fondos (con su lastre). Son estándares de quien compite: el listón es alto. El resto suma volumen, no rango.';

  return (
    <View accessibilityLiveRegion="polite" accessibilityLabel={`Rango de ${mus.nombre}`} style={[estilos.panel, { borderTopColor: paleta.base }]}>
      <View style={estilos.asa} />
      <View style={estilos.panelCabecera}>
        <Emblema tipo={tipoEmblema(mus.tipo)} grado={mus.grado} tam={EMBLEMA_PANEL} />
        <View style={estilos.flex}>
          <Texto estilo="subtitulo" encabezado>{mus.nombre}</Texto>
          <Texto estilo="body" peso="700" color={paleta.texto}>{nombreRangoMayusculas(mus.tipo, mus.grado)}</Texto>
          <Texto estilo="dataMini" tono="secundario">{top}</Texto>
        </View>
      </View>
      <View style={estilos.entre}>
        <Texto estilo="etiquetaEstrecha" tono="secundario">Tus ejercicios</Texto>
        <Texto estilo="dataMini" tono="secundario">mejor serie</Texto>
      </View>
      <View accessibilityRole="list">
        {mus.ejercicios.map((e, i) => (
          <View key={e.nombre} style={[estilos.ejercicio, i > 0 && estilos.separado]}>
            <Emblema tipo={tipoEmblema(e.tipo)} grado={e.tipo === 'sin' ? 1 : e.grado} tam={EMBLEMA_FILA} />
            <View style={estilos.flex}>
              <Texto estilo="texto" peso="700">{e.nombre}</Texto>
              <Texto estilo="dataMini" tono="secundario">{e.serie}</Texto>
            </View>
            <Texto estilo="nota" peso="700" color={tema.rango[e.tipo].texto}>{nombreRango(e.tipo, e.grado)}</Texto>
          </View>
        ))}
      </View>
      <Texto estilo="mini" tono="secundario">{nota}</Texto>
      <Boton variante="secundario" etiqueta="Calcular con otro ejercicio" onPress={() => router.push('/calculadora')} />
    </View>
  );
}

export function Progreso() {
  const router = useRouter();
  const resumen = useResumenProgreso();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [sel, setSel] = useState<Grupo>('pecho');
  const { global, musculos, faltaPerfil } = useRangos();
  const colores = Object.fromEntries(GRUPOS.map((g) => [g, tema.rango[musculos[g].tipo].base])) as ColoresCuerpo;
  const sinRango = faltaPerfil
    ? 'Completa tu sexo y tu peso en el perfil para compararte'
    : 'Registra sentadilla, banca, peso muerto, dominadas o fondos, o usa la calculadora';

  return (
    <Pantalla bordes={['top']} hueco="s35">
      <Cabecera racha={resumen.data?.semanasDeRacha} alPulsarRacha={() => router.push('/racha')} />
      <Pestanas />
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={global ? `Tu rango Forja: ${nombreRango(global.tipo, global.grado)}, top ${global.top} %. Ver la escala` : `Aún sin rango Forja. ${sinRango}. Ver la escala`}
        onPress={() => router.push('/escala')}
        style={estilos.global}
      >
        <View style={estilos.globalTextos}>
          <Texto estilo="etiquetaEstrecha" tono="secundario">Rango Forja · ver escala ›</Texto>
          <Texto estilo="rangoMedio" color={tema.destaque.titulo}>{global ? nombreRangoMayusculas(global.tipo, global.grado) : 'SIN RANGO'}</Texto>
          <Texto estilo="nota" color={global ? tema.rango[global.tipo].texto : tema.color.onSurfaceVariant}>
            {global ? `Estás en el top ${global.top} % · estimación` : sinRango}
          </Texto>
        </View>
        {global ? <Emblema tipo={global.tipo} grado={global.grado} tam={EMBLEMA_GLOBAL} /> : null}
      </Pressable>
      <View style={estilos.mapa}>
        <Texto estilo="nota" tono="secundario">Toca un músculo · arrastra para girar</Texto>
        <Cuerpo ancho={ANCHO_CUERPO} seleccion={sel} alTocar={setSel} colores={colores} etiqueta="Mapa de rangos por músculo" />
        <View accessibilityRole="list" accessibilityLabel="Escala de colores" style={estilos.leyenda}>
          {LEYENDA.map((n) => (
            <View key={n} style={estilos.muestraFila}>
              <View style={[estilos.muestra, { backgroundColor: tema.rango[n].base }]} />
              <Texto estilo="mini" tono="secundario">{NOMBRE_RANGO[n]}</Texto>
            </View>
          ))}
        </View>
      </View>
      <Panel grupo={sel} musculos={musculos} />
      <View style={estilos.entre}>
        <Texto estilo="seccion" encabezado>Por zona</Texto>
        <Pressable accessibilityRole="link" onPress={() => router.push('/escala')} style={estilos.enlace}>
          <Texto estilo="nota" tono="secundario">Escala completa ›</Texto>
        </Pressable>
      </View>
      <View accessibilityRole="list" style={estilos.zonas}>
        {ZONAS.map((z) => {
          const mus = musculos[z.clave];
          const paleta = tema.rango[mus.tipo];
          const pintados = Object.fromEntries(
            GRUPOS.map((g) => [g, z.partes.includes(g) ? paleta.base : tema.cuerpo.neutro]),
          ) as ColoresCuerpo;
          return (
            <FilaRango
              key={z.nombre}
              nombre={z.nombre}
              rango={nombreRangoMayusculas(mus.tipo, mus.grado)}
              colores={paleta}
              musculos={pintados}
              desplazamiento={z.desplazamiento}
              alPulsar={() => setSel(z.clave)}
            />
          );
        })}
      </View>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    flex: { flex: 1, gap: tema.space.s075 },
    pestanas: { flexDirection: 'row', borderBottomWidth: tema.border.fino, borderBottomColor: tema.color.surfaceVariant },
    pestana: {
      flex: 1,
      minHeight: tema.hit.icono,
      alignItems: 'center',
      justifyContent: 'center',
      borderBottomWidth: tema.border.foco,
      borderBottomColor: 'transparent',
    },
    pestanaActiva: { borderBottomColor: tema.color.primary },
    global: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingVertical: tema.space.s35,
      paddingHorizontal: tema.space.s4,
      borderRadius: tema.radius.md,
      backgroundColor: tema.destaque.fondo,
      borderWidth: tema.border.fino,
      borderColor: tema.color.primary,
    },
    globalTextos: { gap: tema.space.s1 },
    mapa: { alignItems: 'center', gap: tema.space.s25 },
    leyenda: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'center', gap: tema.space.s25 },
    muestraFila: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s1 },
    muestra: { width: MUESTRA, height: MUESTRA, borderRadius: tema.radius.xs / 2 },
    panel: {
      gap: tema.space.s25,
      paddingTop: tema.space.s25,
      paddingHorizontal: tema.space.s4,
      paddingBottom: tema.space.s35,
      borderTopLeftRadius: tema.radius.hoja,
      borderTopRightRadius: tema.radius.hoja,
      borderBottomLeftRadius: tema.radius.md,
      borderBottomRightRadius: tema.radius.md,
      borderTopWidth: tema.border.fino,
      backgroundColor: tema.color.surface,
    },
    asa: { alignSelf: 'center', width: tema.hit.icono, height: tema.space.s1, borderRadius: tema.radius.xs, backgroundColor: tema.color.outline },
    panelCabecera: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s35 },
    entre: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    enlace: { minHeight: tema.hit.icono, justifyContent: 'center' },
    ejercicio: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s3, paddingVertical: tema.space.s25 },
    separado: { borderTopWidth: tema.border.fino, borderTopColor: tema.color.surfaceVariant },
    zonas: { gap: tema.space.s2 },
  });
}
