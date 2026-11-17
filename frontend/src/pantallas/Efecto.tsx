import { StyleSheet, View } from 'react-native';

import type { EfectoEjercicio } from '../api/esquemas';
import { ConConsulta } from '../componentes/ConConsulta';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { useEfecto } from '../limitaciones/consultas';
import { NOTA_PLAN } from '../limitaciones/textos';
import { type Theme, useTheme } from '../theme';
import { ConConsentimiento } from './ConConsentimiento';

const GRUPOS: readonly { decision: EfectoEjercicio['decision']; titulo: string }[] = [
  { decision: 'excluido', titulo: 'Excluidos' },
  { decision: 'sustituido', titulo: 'Sustituidos' },
  { decision: 'con_anotaciones', titulo: 'Con avisos' },
];

function Fila({ efecto }: { efecto: EfectoEjercicio }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const sustituido = efecto.decision === 'sustituido' && efecto.sustituto;
  return (
    <View style={estilos.fila}>
      <Texto peso="700">{sustituido ? `${efecto.nombre} → ${efecto.sustituto?.nombre}` : efecto.nombre}</Texto>
      {efecto.motivo ? <Texto estilo="nota" tono="secundario">{efecto.motivo}</Texto> : null}
      {efecto.anotaciones.map((a) => (
        <Texto key={a} estilo="nota" tono="secundario">{a}</Texto>
      ))}
    </View>
  );
}

function Grupos({ efectos }: { efectos: EfectoEjercicio[] }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  if (efectos.length === 0) return <Texto>Con tus limitaciones actuales no cambia ningún ejercicio.</Texto>;
  return (
    <>
      {GRUPOS.map(({ decision, titulo }) => {
        const delGrupo = efectos.filter((e) => e.decision === decision);
        if (delGrupo.length === 0) return null;
        return (
          <View key={decision} style={estilos.grupo}>
            <Texto estilo="etiquetaEstrecha" tono="secundario" encabezado>{titulo}</Texto>
            <View accessibilityRole="list" style={estilos.lista}>
              {delGrupo.map((e) => (
                <Fila key={e.idExterno} efecto={e} />
              ))}
            </View>
          </View>
        );
      })}
    </>
  );
}

function Contenido() {
  const efecto = useEfecto();
  return (
    <>
      <Texto tono="secundario">Los ejercicios del catálogo que cambian por las limitaciones que has indicado.</Texto>
      <Texto estilo="nota" tono="secundario">{NOTA_PLAN}</Texto>
      <ConConsulta consulta={efecto} cargando="Calculando qué cambia…">{(lista) => <Grupos efectos={lista} />}</ConConsulta>
    </>
  );
}

export function Efecto() {
  return (
    <Pantalla hueco="s4">
      <EncabezadoAtras titulo="Qué cambia en mis ejercicios" />
      <ConConsentimiento>
        <Contenido />
      </ConConsentimiento>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    grupo: { gap: tema.space.s2 },
    lista: { borderRadius: tema.radius.md, backgroundColor: tema.color.surface, overflow: 'hidden' },
    fila: { gap: tema.space.s05, padding: tema.space.s35, borderTopWidth: tema.border.fino, borderTopColor: tema.color.surfaceVariant },
  });
}
