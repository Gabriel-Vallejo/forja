import { StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import type { PreferenciasGuardadas, PreferenciasPlan } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { ConConsulta } from '../componentes/ConConsulta';
import { Texto } from '../componentes/Texto';
import { type Theme, useTheme } from '../theme';
import { aCodigos, useGuardarPreferencias, usePreferencias } from './preferencias';

export const SIN_PREFERENCIAS = 'No has ajustado nada todavía. Desde un ejercicio o desde Mi plan puedes quitar, enfocar o cambiar series.';
export const NOTA_PREFERENCIAS = 'Quitar algo de aquí no cambia el plan que tienes: cuenta la próxima vez que lo regeneres.';

interface Fila {
  clave: string;
  texto: string;
  /** Las preferencias sin esta. */
  sin: (p: PreferenciasPlan) => PreferenciasPlan;
}

function filasDe(p: PreferenciasGuardadas): Fila[] {
  return [
    ...p.exclusiones.map((e) => ({
      clave: `x-${e.codigo}`,
      texto: `No recomendar: ${e.nombre}`,
      sin: (c: PreferenciasPlan) => ({ ...c, exclusiones: c.exclusiones.filter((x) => x !== e.codigo) }),
    })),
    ...p.prioritarios.map((m) => ({
      clave: `m-${m.codigo}`,
      texto: `Enfocado: ${m.nombre}`,
      sin: (c: PreferenciasPlan) => ({ ...c, prioritarios: c.prioritarios.filter((x) => x !== m.codigo) }),
    })),
    ...p.series.map((s) => ({
      clave: `s-${s.idExterno}`,
      texto: `${s.nombre}: ${s.series} series a la semana`,
      sin: (c: PreferenciasPlan) => ({ ...c, series: Object.fromEntries(Object.entries(c.series).filter(([id]) => id !== s.idExterno)) }),
    })),
    ...(p.variedad ? [{ clave: 'variedad', texto: 'Más variedad', sin: (c: PreferenciasPlan) => ({ ...c, variedad: false }) }] : []),
  ];
}

function Lista({ preferencias }: { preferencias: PreferenciasGuardadas }) {
  const estilos = crearEstilos(useTheme());
  const guardar = useGuardarPreferencias();
  const filas = filasDe(preferencias);
  if (filas.length === 0) return <Texto tono="secundario">{SIN_PREFERENCIAS}</Texto>;
  return (
    <>
      <View accessibilityRole="list" style={estilos.lista}>
        {filas.map((f) => (
          <View key={f.clave} style={estilos.fila}>
            <Texto estilo="texto" estiloExtra={estilos.texto}>{f.texto}</Texto>
            <Boton
              variante="secundario"
              etiqueta="Quitar"
              etiquetaAccesible={`Quitar ${f.texto}`}
              deshabilitado={guardar.isPending}
              onPress={() => guardar.mutate(f.sin(aCodigos(preferencias)))}
            />
          </View>
        ))}
      </View>
      {guardar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(guardar.error)} /> : null}
      <Texto estilo="nota" tono="secundario">{NOTA_PREFERENCIAS}</Texto>
    </>
  );
}

/** Lo que el usuario ha ajustado de su plan y respeta cada «Regenerar», con su botón para quitarlo. */
export function ListaPreferencias() {
  const preferencias = usePreferencias();
  return (
    <View accessibilityLabel="Lo que has ajustado">
      <Texto peso="700">Lo que has ajustado</Texto>
      <ConConsulta consulta={preferencias} cargando="Cargando lo que has ajustado…">
        {(datos) => <Lista preferencias={datos} />}
      </ConConsulta>
    </View>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    lista: { gap: tema.space.s2 },
    fila: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s2 },
    texto: { flex: 1 },
  });
}
