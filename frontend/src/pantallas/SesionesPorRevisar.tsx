import { useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { Confirmacion } from '../componentes/Confirmacion';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { usePlanActual } from '../plan/plan';
import type { SerieLocal, SesionLocal } from '../registro/almacen';
import { mensajeDeRegistro, type Outbox } from '../registro/outbox';
import { useSincronizacion } from '../registro/sincronizacion';
import { type Theme, useTheme } from '../theme';
import { ConOutbox } from './ConOutbox';
import { resumenSerie } from './SesionEntreno';

export const AVISO_CUENTA_ELIMINADA =
  'Tu cuenta ya no existe en el servidor: los entrenos que no se habían enviado se han borrado de este móvil.';
const SIN_MOTIVO = 'El servidor no ha aceptado esta sesión: revísala.';

interface Rechazada {
  sesion: SesionLocal;
  series: SerieLocal[];
}

const fecha = (iso: string) => new Date(iso).toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' });

function TarjetaRechazada({ outbox, rechazada, nombres, alCambiar }: { outbox: Outbox; rechazada: Rechazada; nombres: Map<string, string>; alCambiar: () => void }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const { sesion, series } = rechazada;
  const [descartando, setDescartando] = useState(false);
  const [ocupada, setOcupada] = useState(false);
  const [error, setError] = useState<string>();
  const cuando = fecha(sesion.realizadaEn);

  const hacer = async (accion: () => Promise<void>) => {
    setOcupada(true);
    setError(undefined);
    try {
      await accion();
      alCambiar();
    } catch (e) {
      setError(mensajeDeRegistro(e));
      setOcupada(false);
    }
  };

  return (
    <View style={estilos.tarjeta}>
      <Texto estilo="seccion" encabezado>{`Sesión del ${cuando}${sesion.diaPlan === null ? '' : ` · día ${sesion.diaPlan}`}`}</Texto>
      <Aviso tipo="aviso" mensaje={sesion.ultimoError ?? SIN_MOTIVO} />
      <View accessibilityRole="list" style={estilos.series}>
        {series.length === 0 ? <Texto estilo="nota" tono="secundario">Sin series válidas.</Texto> : null}
        {series.map((s) => (
          <Texto key={s.uuid} estilo="dataMedio">{`${nombres.get(s.ejercicio) ?? s.ejercicio}${s.calentamiento ? ' (calentamiento)' : ''} · ${resumenSerie(s)}`}</Texto>
        ))}
      </View>
      {error && !descartando ? <Aviso tipo="error" mensaje={error} /> : null}
      {descartando ? (
        <Confirmacion
          mensaje="¿Descartar esta sesión? Se borra del móvil y no se puede deshacer."
          etiquetaConfirmar="Sí, descartar"
          etiquetaCargando="Descartando…"
          cargando={ocupada}
          error={error}
          alConfirmar={() => hacer(() => outbox.descartarSesion(sesion.uuid))}
          alCancelar={() => setDescartando(false)}
        />
      ) : (
        <View style={estilos.acciones}>
          <Boton
            etiqueta="Reintentar"
            etiquetaAccesible={`Reintentar el envío de la sesión del ${cuando}`}
            cargando={ocupada}
            etiquetaCargando="Reintentando…"
            onPress={() => hacer(() => outbox.reintentarSesion(sesion.uuid))}
          />
          <Boton variante="secundario" etiqueta="Descartar" etiquetaAccesible={`Descartar la sesión del ${cuando}`} onPress={() => setDescartando(true)} />
        </View>
      )}
    </View>
  );
}

function Lista({ outbox }: { outbox: Outbox }) {
  const { revision } = useSincronizacion();
  const plan = usePlanActual();
  const [rechazadas, setRechazadas] = useState<Rechazada[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [lectura, setLectura] = useState(0);
  const nombres = new Map((plan.data?.dias ?? []).flatMap((d) => d.ejercicios.map((e) => [e.idExterno, e.nombre] as const)));

  useEffect(() => {
    let vigente = true;
    (async () => {
      const sesiones = (await outbox.porRevisar()).filter((s) => s.estado === 'rechazada');
      return Promise.all(sesiones.map(async (sesion) => ({ sesion, series: await outbox.seriesValidas(sesion.uuid) })));
    })().then(
      (lista) => {
        if (!vigente) return;
        setRechazadas(lista);
        setError(null);
      },
      (e: unknown) => {
        if (vigente) setError(mensajeDeRegistro(e));
      },
    );
    return () => {
      vigente = false;
    };
  }, [outbox, revision, lectura]);

  const releer = () => setLectura((n) => n + 1);
  if (error) {
    return (
      <>
        <Aviso tipo="error" mensaje={error} />
        <Boton variante="secundario" etiqueta="Reintentar" onPress={releer} />
      </>
    );
  }
  if (!rechazadas) return <Texto tono="secundario">Cargando tus sesiones…</Texto>;
  if (rechazadas.length === 0) return <Texto tono="secundario">No tienes sesiones por revisar.</Texto>;
  return (
    <>
      {rechazadas.map((r) => (
        <TarjetaRechazada key={r.sesion.uuid} outbox={outbox} rechazada={r} nombres={nombres} alCambiar={releer} />
      ))}
    </>
  );
}

/** Sesiones que el servidor rechazó (RF-24): se enseñan, se reintentan o se descartan; nunca se borran solas. */
export function SesionesPorRevisar() {
  const { cuentaEliminada } = useSincronizacion();
  return (
    <Pantalla hueco="s4">
      <EncabezadoAtras titulo="Sesiones por revisar" />
      {cuentaEliminada ? <Aviso tipo="aviso" mensaje={AVISO_CUENTA_ELIMINADA} /> : null}
      <ConOutbox>{(outbox) => <Lista outbox={outbox} />}</ConOutbox>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    tarjeta: { gap: tema.space.s3, padding: tema.space.s35, borderRadius: tema.radius.md, backgroundColor: tema.color.surface },
    series: { gap: tema.space.s1 },
    acciones: { gap: tema.space.s2 },
  });
}
