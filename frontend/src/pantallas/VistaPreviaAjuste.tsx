import { useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { mensajeDeError } from '../api/errores';
import type { Ajuste, VistaPrevia } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { numeroSeries, TITULO_AJUSTE, useAplicarAjuste, useVistaPrevia } from '../plan/preferencias';
import { type Theme, useTheme } from '../theme';

export const CARGANDO_VISTA_PREVIA = 'Resolviendo tu plan con el ajuste. Tarda unos segundos…';
export const SIN_CAMBIOS = 'Con este ajuste el plan sale igual que el que tienes.';

type Cambio = VistaPrevia['ejercicios'][number];

function ListaCambios({ titulo, cambios, texto }: { titulo: string; cambios: Cambio[]; texto: (c: Cambio) => string }) {
  const estilos = crearEstilos(useTheme());
  if (cambios.length === 0) return null;
  return (
    <View style={estilos.tarjeta} accessibilityRole="list" accessibilityLabel={titulo}>
      <Texto estilo="etiquetaEstrecha" tono="secundario">{titulo}</Texto>
      {cambios.map((c) => (
        <Texto key={c.idExterno} estilo="texto">{texto(c)}</Texto>
      ))}
    </View>
  );
}

function Diferencias({ vista }: { vista: VistaPrevia }) {
  const estilos = crearEstilos(useTheme());
  const entran = vista.ejercicios.filter((c) => c.antes === 0);
  const salen = vista.ejercicios.filter((c) => c.despues === 0);
  const cambian = vista.ejercicios.filter((c) => c.antes > 0 && c.despues > 0);
  if (vista.ejercicios.length === 0 && vista.musculos.length === 0) return <Aviso tipo="info" mensaje={SIN_CAMBIOS} />;
  return (
    <>
      <ListaCambios titulo="Entran" cambios={entran} texto={(c) => `${c.nombre} · ${c.despues} series a la semana`} />
      <ListaCambios titulo="Salen" cambios={salen} texto={(c) => `${c.nombre} · tenía ${c.antes} series`} />
      <ListaCambios titulo="Cambian de series" cambios={cambian} texto={(c) => `${c.nombre} · de ${c.antes} a ${c.despues} series`} />
      {vista.musculos.length > 0 ? (
        <View style={estilos.tarjeta} accessibilityRole="list" accessibilityLabel="Series por músculo">
          <Texto estilo="etiquetaEstrecha" tono="secundario">Series por músculo a la semana</Texto>
          {vista.musculos.map((m) => (
            <View key={m.musculo} style={estilos.fila}>
              <Texto estilo="texto">{m.nombre}</Texto>
              <Texto estilo="data" tono="secundario">{`${numeroSeries(m.antes)} → ${numeroSeries(m.despues)}`}</Texto>
            </View>
          ))}
        </View>
      ) : null}
    </>
  );
}

function Contenido({ ajuste }: { ajuste: Ajuste }) {
  const router = useRouter();
  const vista = useVistaPrevia(ajuste);
  const aplicar = useAplicarAjuste();

  if (vista.isPending) {
    return (
      <View accessibilityLiveRegion="polite">
        <Texto tono="secundario">{CARGANDO_VISTA_PREVIA}</Texto>
      </View>
    );
  }
  if (vista.isError) {
    return (
      <>
        <Aviso tipo="error" mensaje={mensajeDeError(vista.error)} />
        <Boton variante="secundario" etiqueta="Reintentar" onPress={() => vista.refetch()} />
      </>
    );
  }
  const datos = vista.data;
  return (
    <>
      <Texto estilo="nota" tono="secundario">{`${datos.plan.resumen} Aún no se ha guardado nada.`}</Texto>
      {datos.plan.notas.map((n) => (
        <Aviso key={n} tipo="aviso" mensaje={n} />
      ))}
      <Diferencias vista={datos} />
      {aplicar.isError ? <Aviso tipo="error" mensaje={mensajeDeError(aplicar.error)} /> : null}
      <Boton
        etiqueta="Aplicar"
        etiquetaCargando="Aplicando… tarda unos segundos"
        cargando={aplicar.isPending}
        onPress={() => aplicar.mutate({ ...ajuste, version: datos.version }, { onSuccess: () => router.navigate('/entreno') })}
      />
      <Boton variante="secundario" etiqueta="Descartar" deshabilitado={aplicar.isPending} onPress={() => router.back()} />
    </>
  );
}

/** Vista previa de un ajuste del plan (PRD v0.1): qué entra, qué sale y cómo quedan las series, antes de guardarlo. */
export function VistaPreviaAjuste({ ajuste }: { ajuste: Ajuste }) {
  return (
    <Pantalla hueco="s3">
      <EncabezadoAtras titulo={TITULO_AJUSTE[ajuste.tipo] ?? 'Ajuste del plan'} />
      <Contenido ajuste={ajuste} />
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    tarjeta: {
      gap: tema.space.s2,
      paddingVertical: tema.space.s35,
      paddingHorizontal: tema.space.s4,
      borderRadius: tema.radius.md,
      backgroundColor: tema.color.surface,
    },
    fila: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline', gap: tema.space.s2 },
  });
}
