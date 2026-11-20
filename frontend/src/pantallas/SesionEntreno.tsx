import { useRouter } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import type { ObjetivoEjercicio, PlanActual } from '../api/esquemas';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { Confirmacion } from '../componentes/Confirmacion';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Icono } from '../componentes/Icono';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { kilos, pesoCorporal, prescripcion } from '../plan/plan';
import type { SerieLocal, SesionLocal } from '../registro/almacen';
import { type Descanso, formatoDescanso, useDescanso } from '../registro/descanso';
import { ErrorRegistro, mensajeDeRegistro, type Outbox } from '../registro/outbox';
import { useCargasSiguientes, useSincronizacion } from '../registro/sincronizacion';
import { type Theme, useTheme } from '../theme';
import { ConOutbox } from './ConOutbox';
import { ConPlanParaEntrenar } from './ConPlanParaEntrenar';
import { FilaSerie, type ValoresIniciales, type ValoresSerie } from './FilaSerie';

const ICONO_HECHA = 14;

const plural = (n: number, una: string, varias: string) => `${n} ${n === 1 ? una : varias}`;

/** «80 kg × 8 · RIR 2» (o «tu peso × 8» sin lastre). */
export function resumenSerie(s: SerieLocal): string {
  const peso = s.pesoKg === 0 ? 'tu peso' : `${kilos(s.pesoKg)} kg`;
  return `${peso} × ${s.repeticiones}${s.rir === null ? '' : ` · RIR ${s.rir}`}`;
}

const pesoObjetivo = (e: ObjetivoEjercicio, pesoKg: number | undefined) =>
  pesoCorporal(e) ? 'tu peso' : pesoKg === undefined ? 'elige carga' : `${kilos(pesoKg)} kg`;

/** Lo que se precarga en la fila: la última serie de ese ejercicio o, si no hay, el objetivo del plan. */
function valoresIniciales(e: ObjetivoEjercicio, hechas: readonly SerieLocal[], carga: number | undefined): ValoresIniciales {
  const ultima = hechas.at(-1);
  if (ultima) return { pesoKg: ultima.pesoKg, repeticiones: ultima.repeticiones, rir: ultima.rir ?? e.rirObjetivo, calentamiento: false };
  // Primera semana: sin carga en el plan ni calculada, el peso se deja vacío para que lo elijas (no «0 kg»).
  const pesoKg = pesoCorporal(e) ? 0 : (carga ?? e.cargaKg);
  return { pesoKg, repeticiones: e.repeticionesMax, rir: e.rirObjetivo, calentamiento: false };
}

const aDatos = (ejercicio: string, orden: number, v: ValoresSerie) => ({
  ejercicio,
  orden,
  pesoKg: v.pesoKg,
  repeticiones: v.repeticiones,
  rir: v.rir,
  calentamiento: v.calentamiento,
  ...(v.dolor !== undefined && { dolor: v.dolor }),
});

/** Lo de la base local nunca llega tal cual a la interfaz (podría llevar datos de las series). */
async function conMensaje<T>(accion: Promise<T>): Promise<T> {
  try {
    return await accion;
  } catch (e) {
    throw new Error(mensajeDeRegistro(e));
  }
}

/** Retoma la sesión en curso (aunque se cerrara la app) o empieza una del día de hoy. */
function useSesionDelDia(outbox: Outbox, plan: PlanActual) {
  const [sesion, setSesion] = useState<SesionLocal | null>(null);
  const [series, setSeries] = useState<SerieLocal[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [intento, setIntento] = useState(0);
  // Un solo arranque por intento: si el efecto se repite (StrictMode), no se crean dos sesiones.
  const arranque = useRef<{ intento: number; promesa: Promise<SesionLocal> } | null>(null);

  useEffect(() => {
    let vigente = true;
    if (arranque.current?.intento !== intento) {
      const promesa = (async () => (await outbox.sesionEnCurso()) ?? outbox.empezarSesion({ diaPlan: plan.diaDeHoy, planVersion: plan.uuid }))();
      arranque.current = { intento, promesa };
    }
    arranque.current.promesa
      .then(async (s) => {
        const validas = await outbox.seriesValidas(s.uuid);
        if (vigente) {
          setSesion(s);
          setSeries(validas);
        }
      })
      .catch((e: unknown) => {
        if (vigente) setError(mensajeDeRegistro(e));
      });
    return () => {
      vigente = false;
    };
  }, [outbox, plan.diaDeHoy, plan.uuid, intento]);

  const recargar = async () => {
    if (sesion) setSeries(await outbox.seriesValidas(sesion.uuid));
  };
  const reintentar = () => {
    setError(null);
    setIntento((n) => n + 1);
  };
  return { sesion, series, error, recargar, reintentar };
}

type Activo = { ejercicio: string; corrige?: SerieLocal } | null;

interface PropsBloque {
  e: ObjetivoEjercicio;
  carga: number | undefined;
  hechas: SerieLocal[];
  activo: Activo;
  porAnular: string | null;
  alActivar: (activo: Activo) => void;
  alGuardar: (e: ObjetivoEjercicio, v: ValoresSerie, corrige?: SerieLocal) => Promise<void>;
  alPedirAnular: (serie: string | null) => void;
  alAnular: (serie: SerieLocal) => Promise<void>;
}

function BloqueEjercicio({ e, carga, hechas, activo, porAnular, alActivar, alGuardar, alPedirAnular, alAnular }: PropsBloque) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const [anulando, setAnulando] = useState(false);
  const [errorAnular, setErrorAnular] = useState<string>();
  const trabajo = hechas.filter((s) => !s.calentamiento).length;
  const esActivo = activo?.ejercicio === e.idExterno;
  const nuevaActiva = esActivo && !activo?.corrige;
  const pendientes = Math.max(0, e.series - trabajo);
  const iniciales = valoresIniciales(e, hechas, carga);
  let numero = 0;

  return (
    <View style={estilos.bloque}>
      <View>
        <Texto estilo="seccion" encabezado>{e.nombre}</Texto>
        <Texto estilo="data" tono="secundario">{prescripcion(e, carga)}</Texto>
      </View>
      {hechas.map((s) => {
        const titulo = s.calentamiento ? 'Calentamiento' : `Serie ${++numero}`;
        if (esActivo && activo?.corrige?.uuid === s.uuid) {
          return (
            <FilaSerie
              key={s.uuid}
              objetivo={e}
              titulo={`${titulo} (corrección)`}
              inicial={{ pesoKg: s.pesoKg, repeticiones: s.repeticiones, rir: s.rir ?? e.rirObjetivo, calentamiento: s.calentamiento, dolor: s.dolor ?? undefined }}
              etiquetaGuardar="Guardar corrección"
              alGuardar={(v) => alGuardar(e, v, s)}
              alCancelar={() => alActivar(null)}
            />
          );
        }
        const contexto = `${titulo.toLowerCase()} de ${e.nombre}`;
        return (
          <View key={s.uuid} style={estilos.hecha}>
            <View style={estilos.hechaDatos} accessible accessibilityLabel={`${titulo} hecha: ${resumenSerie(s)}`}>
              <View style={estilos.marca}>
                <Icono nombre="check" tam={ICONO_HECHA} color={tema.color.success} />
                <Texto estilo="etiquetaEstrecha" tono="secundario">{`${titulo} hecha`}</Texto>
              </View>
              <Texto estilo="dataMedio">{resumenSerie(s)}</Texto>
            </View>
            <View style={estilos.acciones}>
              <Boton variante="texto" etiqueta="Corregir" etiquetaAccesible={`Corregir ${contexto}`} onPress={() => alActivar({ ejercicio: e.idExterno, corrige: s })} />
              <Boton
                variante="texto"
                etiqueta="Anular"
                etiquetaAccesible={`Anular ${contexto}`}
                onPress={() => {
                  setErrorAnular(undefined);
                  alPedirAnular(s.uuid);
                }}
              />
            </View>
            {porAnular === s.uuid ? (
              <Confirmacion
                mensaje={`¿Anular la ${contexto}? Dejará de contar; queda registrado que la anulaste.`}
                etiquetaConfirmar="Sí, anular"
                etiquetaCargando="Anulando…"
                cargando={anulando}
                error={errorAnular}
                alConfirmar={() => {
                  setAnulando(true);
                  alAnular(s)
                    .catch((err: unknown) => setErrorAnular(err instanceof Error ? err.message : undefined))
                    .finally(() => setAnulando(false));
                }}
                alCancelar={() => alPedirAnular(null)}
              />
            ) : null}
          </View>
        );
      })}
      {Array.from({ length: pendientes }, (_, i) => {
        const titulo = `Serie ${trabajo + i + 1}`;
        if (i === 0 && nuevaActiva) {
          return (
            <FilaSerie key={`nueva-${hechas.length}`} objetivo={e} titulo={titulo} inicial={iniciales} etiquetaGuardar="Guardar serie" alGuardar={(v) => alGuardar(e, v)} />
          );
        }
        return (
          <Pressable
            key={titulo}
            accessibilityRole="button"
            accessibilityLabel={`${titulo} de ${e.nombre}, pendiente. Registrarla`}
            onPress={() => alActivar({ ejercicio: e.idExterno })}
            style={({ pressed }) => [estilos.pendiente, pressed && estilos.pulsada]}
          >
            <Texto estilo="etiquetaEstrecha" tono="secundario">{titulo}</Texto>
            <Texto estilo="dataMedio" tono="secundario">{`${pesoObjetivo(e, iniciales.pesoKg)} × ${e.repeticionesMin}-${e.repeticionesMax} · RIR ${e.rirObjetivo}`}</Texto>
          </Pressable>
        );
      })}
      {pendientes === 0 && nuevaActiva ? (
        <FilaSerie key={`extra-${hechas.length}`} objetivo={e} titulo="Serie extra" inicial={iniciales} etiquetaGuardar="Guardar serie" alGuardar={(v) => alGuardar(e, v)} alCancelar={() => alActivar(null)} />
      ) : null}
      {pendientes === 0 && !esActivo ? (
        <Boton variante="texto" etiqueta="Añadir serie extra" etiquetaAccesible={`Añadir serie extra de ${e.nombre}`} onPress={() => alActivar({ ejercicio: e.idExterno })} />
      ) : null}
    </View>
  );
}

function EstadoEnvio({ outbox, sesion }: { outbox: Outbox; sesion: string }) {
  const { revision } = useSincronizacion();
  const [estado, setEstado] = useState<SesionLocal['estado'] | 'confirmada' | null>(null);
  useEffect(() => {
    let vigente = true;
    outbox
      .porRevisar()
      .then((lista) => vigente && setEstado(lista.find((s) => s.uuid === sesion)?.estado ?? 'confirmada'))
      .catch(() => vigente && setEstado(null));
    return () => {
      vigente = false;
    };
  }, [outbox, sesion, revision]);
  if (estado === 'confirmada') return <Aviso tipo="exito" mensaje="Enviada." />;
  if (estado === 'rechazada') return <Aviso tipo="aviso" mensaje="El servidor no la ha aceptado: revísala en «Sesiones por revisar»." />;
  return <Aviso tipo="info" mensaje="Guardada en el móvil. Se envía sola en cuanto haya conexión." />;
}

function Resumen({ outbox, sesion, series }: { outbox: Outbox; sesion: string; series: SerieLocal[] }) {
  const router = useRouter();
  const trabajo = series.filter((s) => !s.calentamiento).length;
  const ejercicios = new Set(series.map((s) => s.ejercicio)).size;
  return (
    <>
      <Texto estilo="subtitulo" encabezado>Sesión terminada</Texto>
      <Texto estilo="dataMedio">{`${plural(trabajo, 'serie', 'series')} · ${plural(ejercicios, 'ejercicio', 'ejercicios')}`}</Texto>
      <EstadoEnvio outbox={outbox} sesion={sesion} />
      <Boton etiqueta="Volver a Hoy" grande onPress={() => router.dismissTo('/hoy')} />
    </>
  );
}

type Final = 'terminar' | 'descartar' | null;

interface PropsSesionDelDia {
  outbox: Outbox;
  plan: PlanActual;
  alGuardarSerie: () => void;
  /** Avisa de si hay una confirmación abierta abajo del todo, para bajar hasta ella. */
  alConfirmar: (abierta: boolean) => void;
}

function SesionDelDia({ outbox, plan, alGuardarSerie, alConfirmar }: PropsSesionDelDia) {
  const router = useRouter();
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const cargas = useCargasSiguientes(plan);
  const { sesion, series, error, recargar, reintentar } = useSesionDelDia(outbox, plan);
  const [elegido, setElegido] = useState<Activo>(null);
  const [porAnular, setPorAnular] = useState<string | null>(null);
  const [final, setFinalLocal] = useState<Final>(null);
  const setFinal = (f: Final) => {
    setFinalLocal(f);
    alConfirmar(f !== null);
  };
  const [cerrando, setCerrando] = useState(false);
  const [errorFinal, setErrorFinal] = useState<string>();
  const [terminada, setTerminada] = useState(false);

  if (error) {
    return (
      <>
        <Aviso tipo="error" mensaje={error} />
        <Boton variante="secundario" etiqueta="Reintentar" onPress={reintentar} />
      </>
    );
  }
  if (!sesion) return <Texto tono="secundario">Preparando la sesión…</Texto>;
  if (terminada) return <Resumen outbox={outbox} sesion={sesion.uuid} series={series} />;

  const dia = plan.dias.find((d) => d.dia === sesion.diaPlan) ?? plan.dias.find((d) => d.dia === plan.diaDeHoy);
  const ejercicios = dia?.ejercicios ?? [];
  const deEjercicio = (id: string) => series.filter((s) => s.ejercicio === id);
  // Sin elección, la fila activa es la siguiente serie prevista: el primer ejercicio al que le faltan series.
  const activo: Activo =
    elegido ??
    (() => {
      const siguiente = ejercicios.find((e) => deEjercicio(e.idExterno).filter((s) => !s.calentamiento).length < e.series);
      return siguiente ? { ejercicio: siguiente.idExterno } : null;
    })();

  const guardar = async (e: ObjetivoEjercicio, v: ValoresSerie, corrige?: SerieLocal) => {
    if (corrige) {
      await conMensaje(outbox.corregirSerie(sesion.uuid, corrige.uuid, aDatos(e.idExterno, corrige.orden, v)));
    } else {
      const orden = Math.max(0, ...series.map((s) => s.orden)) + 1;
      await conMensaje(outbox.registrarSerie(sesion.uuid, aDatos(e.idExterno, orden, v)));
      alGuardarSerie();
    }
    await recargar();
    setElegido(null);
  };
  const anular = async (s: SerieLocal) => {
    await conMensaje(outbox.anularSerie(sesion.uuid, s.uuid));
    await recargar();
    setPorAnular(null);
  };
  const acabar = async () => {
    setCerrando(true);
    setErrorFinal(undefined);
    try {
      if (final === 'terminar') {
        await outbox.cerrarSesion(sesion.uuid, ejercicios);
        setTerminada(true);
        return;
      }
      try {
        await outbox.descartarSesion(sesion.uuid);
      } catch (e) {
        // Todas sus series están anuladas: no se puede descartar, se cierra (el servidor recibe las anulaciones).
        if (!(e instanceof ErrorRegistro)) throw e;
        await outbox.cerrarSesion(sesion.uuid, ejercicios);
      }
      router.dismissTo('/hoy');
    } catch (e) {
      setErrorFinal(mensajeDeRegistro(e));
    } finally {
      setCerrando(false);
    }
  };

  const trabajo = series.filter((s) => !s.calentamiento).length;
  const calentamiento = series.length - trabajo;
  return (
    <>
      <View>
        {dia ? <Texto estilo="etiqueta" tono="secundario">{`Día ${dia.dia} · ${dia.nombre}`}</Texto> : null}
        <Texto estilo="nota" tono="secundario">Cada serie se guarda en el móvil al momento, con o sin conexión.</Texto>
      </View>
      {ejercicios.length === 0 ? <Aviso tipo="aviso" mensaje="Este día ya no está en tu plan." /> : null}
      {ejercicios.map((e) => (
        <BloqueEjercicio
          key={e.idExterno}
          e={e}
          carga={cargas.get(e.idExterno)}
          hechas={deEjercicio(e.idExterno)}
          activo={activo}
          porAnular={porAnular}
          alActivar={setElegido}
          alGuardar={guardar}
          alPedirAnular={setPorAnular}
          alAnular={anular}
        />
      ))}
      <View style={estilos.final}>
        {final ? (
          <Confirmacion
            mensaje={
              final === 'terminar'
                ? `¿Terminar la sesión? Llevas ${plural(trabajo, 'serie', 'series')}${calentamiento > 0 ? ` de trabajo y ${calentamiento} de calentamiento` : ''}.`
                : 'No has guardado ninguna serie: no hay nada que terminar. ¿Descartas la sesión?'
            }
            etiquetaConfirmar={final === 'terminar' ? 'Sí, terminar' : 'Sí, descartar'}
            etiquetaCargando={final === 'terminar' ? 'Terminando…' : 'Descartando…'}
            cargando={cerrando}
            error={errorFinal}
            alConfirmar={acabar}
            alCancelar={() => setFinal(null)}
          />
        ) : (
          <Boton variante="secundario" etiqueta="Terminar sesión" onPress={() => setFinal(series.length > 0 ? 'terminar' : 'descartar')} />
        )}
      </View>
    </>
  );
}

function PanelDescanso({ descanso, restanteMs }: { descanso: Descanso; restanteMs: number }) {
  const tema = useTheme();
  const estilos = crearEstilos(tema);
  const total = Math.ceil(restanteMs / 1000);
  return (
    <View style={estilos.descanso}>
      <View accessible accessibilityRole="timer" accessibilityLabel={`Descanso: quedan ${Math.floor(total / 60)} min ${total % 60} s`} style={estilos.reloj}>
        <Texto estilo="etiquetaEstrecha" tono="secundario">Descanso</Texto>
        <Texto estilo="cifraMedia">{formatoDescanso(restanteMs)}</Texto>
      </View>
      <Boton variante="secundario" etiqueta="+30 s" onPress={descanso.sumar} />
      <Boton variante="texto" etiqueta="Saltar" onPress={descanso.saltar} />
    </View>
  );
}

/** Sesión en curso del día de hoy (RF-21 a RF-25): registro de series sin conexión. */
export function SesionEntreno() {
  const descanso = useDescanso();
  const { restanteMs } = descanso;
  const [confirmando, setConfirmando] = useState(false);
  return (
    <Pantalla hueco="s4" seguirFinal={confirmando} pie={restanteMs === null ? undefined : <PanelDescanso descanso={descanso} restanteMs={restanteMs} />}>
      <EncabezadoAtras titulo="Sesión de hoy" />
      <ConOutbox>
        {(outbox) => <ConPlanParaEntrenar>{(plan) => <SesionDelDia outbox={outbox} plan={plan} alGuardarSerie={descanso.empezar} alConfirmar={setConfirmando} />}</ConPlanParaEntrenar>}
      </ConOutbox>
    </Pantalla>
  );
}

function crearEstilos(tema: Theme) {
  return StyleSheet.create({
    bloque: { gap: tema.space.s2, padding: tema.space.s35, borderRadius: tema.radius.md, backgroundColor: tema.color.surface },
    hecha: { gap: tema.space.s1, padding: tema.space.s25, borderRadius: tema.radius.sm, backgroundColor: tema.color.surfaceHigh },
    hechaDatos: { gap: tema.space.s05 },
    marca: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s1 },
    acciones: { flexDirection: 'row', flexWrap: 'wrap', gap: tema.space.s2 },
    pendiente: { minHeight: tema.hit.min, justifyContent: 'center', gap: tema.space.s05, paddingHorizontal: tema.space.s25, borderRadius: tema.radius.sm },
    pulsada: { backgroundColor: tema.color.surfaceHigh },
    final: { gap: tema.space.s2 },
    descanso: { flexDirection: 'row', alignItems: 'center', gap: tema.space.s2, paddingTop: tema.space.s2, borderTopWidth: tema.border.fino, borderTopColor: tema.color.outline },
    reloj: { flex: 1 },
  });
}
