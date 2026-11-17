import { useRouter } from 'expo-router';
import { Pressable, ScrollView, StyleSheet, View } from 'react-native';

import type { PlanActual } from '../api/esquemas';
import { Cabecera } from '../componentes/Cabecera';
import { Icono, type NombreIcono } from '../componentes/Icono';
import { Pantalla } from '../componentes/Pantalla';
import { TarjetaSesion } from '../componentes/TarjetaSesion';
import { Texto } from '../componentes/Texto';
import { RACHA } from '../datos/ejemplo';
import { gruposPrincipales, musculosDelDia } from '../plan/musculos';
import { diaDeLaSemana, diasDesdeHoy, seriesDe } from '../plan/plan';
import { type Theme, useTheme } from '../theme';
import { ConPlan } from './ConPlan';

const ICONO_FILA = 20;
const LADO_ICONO = 40;

const OTRAS_FORMAS: readonly { icono: NombreIcono; titulo: string; detalle: string }[] = [
  { icono: 'plantillas', titulo: 'Plantillas', detalle: 'Full body, torso-pierna, empuje-tracción-pierna' },
  { icono: 'ajustar', titulo: 'Sesión exprés de 30 min', detalle: 'Forja la arma con tus días y tu material' },
  { icono: 'mas', titulo: 'Entreno libre', detalle: 'Añade ejercicios sobre la marcha' },
];

function Semana({ plan }: { plan: PlanActual }) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [hoy, ...despues] = diasDesdeHoy(plan);
  if (!hoy) return null;
  const total = plan.dias.length;

  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={estilos.carrusel} contentContainerStyle={estilos.carruselDentro}>
      <TarjetaSesion
        cuando={`Hoy · ${diaDeLaSemana(new Date())}`}
        etiqueta={`Día ${hoy.dia} de ${total}`}
        titulo={hoy.nombre}
        datos={`${hoy.ejercicios.length} ejercicios · ${seriesDe(hoy)} series`}
        musculos={gruposPrincipales(hoy)}
        cuerpo={musculosDelDia(hoy)}
        alEmpezar={() => router.push('/sesion')}
      />
      {despues.map((d) => (
        <TarjetaSesion key={d.dia} etiqueta={`Después · Día ${d.dia}`} titulo={d.nombre} />
      ))}
    </ScrollView>
  );
}

export function Hoy() {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);

  return (
    <Pantalla bordes={['top']} hueco="s4">
      <Cabecera racha={RACHA.semanas} ejemplo alPulsarRacha={() => router.push('/racha')} />
      <View style={estilos.titulo}>
        <Texto estilo="pantalla" encabezado>Tu semana</Texto>
        <Pressable accessibilityRole="link" onPress={() => router.navigate('/entreno')} style={estilos.enlace}>
          <Texto estilo="texto" tono="secundario">Ver plan ›</Texto>
        </Pressable>
      </View>
      <ConPlan>{(plan) => <Semana plan={plan} />}</ConPlan>
      <View style={estilos.otras}>
        <Texto estilo="body" peso="700" encabezado>¿Otra forma de entrenar hoy?</Texto>
        <View style={estilos.grupo}>
          {OTRAS_FORMAS.map((f, i) => (
            <Pressable
              key={f.titulo}
              accessibilityRole="button"
              accessibilityLabel={`${f.titulo}. ${f.detalle}`}
              onPress={() => router.push({ pathname: '/proximamente', params: { titulo: f.titulo } })}
              style={({ pressed }) => [estilos.fila, i > 0 && estilos.separada, pressed && estilos.pulsada]}
            >
              <View style={estilos.icono}>
                <Icono nombre={f.icono} tam={ICONO_FILA} />
              </View>
              <View style={estilos.textos}>
                <Texto estilo="body" peso="700">{f.titulo}</Texto>
                <Texto estilo="nota" tono="secundario">{f.detalle}</Texto>
              </View>
              <Icono nombre="chevron" tam={ICONO_FILA} color={tema.color.onSurfaceVariant} />
            </Pressable>
          ))}
        </View>
      </View>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    titulo: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    enlace: { minHeight: tema.hit.icono, justifyContent: 'center' },
    carrusel: { marginHorizontal: -tema.space.s4 },
    carruselDentro: { gap: tema.space.s25, paddingHorizontal: tema.space.s4 },
    otras: { gap: tema.space.s2 },
    grupo: { borderRadius: tema.radius.md, backgroundColor: tema.color.surface },
    fila: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s35, minHeight: tema.hit.min + tema.space.s4, paddingHorizontal: tema.space.s35 },
    separada: { borderTopWidth: tema.border.fino, borderTopColor: tema.color.surfaceVariant },
    pulsada: { backgroundColor: tema.color.surfaceHigh },
    icono: { width: LADO_ICONO, height: LADO_ICONO, borderRadius: tema.radius.sm, backgroundColor: tema.color.surfaceHigh, alignItems: 'center', justifyContent: 'center' },
    textos: { flex: 1 },
  });
}
