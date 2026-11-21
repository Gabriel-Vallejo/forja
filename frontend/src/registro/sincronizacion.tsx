import { useQueryClient } from '@tanstack/react-query';
import { createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { AppState } from 'react-native';

import type { PlanActual } from '../api/esquemas';
import { CLAVE_PLAN_ACTUAL } from '../plan/plan';
import { useSesion } from '../sesion/sesion';
import type { AlmacenRegistro } from './almacen';
import { type Contadores, crearOutbox, type Outbox } from './outbox';
import { crearSincronizador, type Sincronizador } from './sincronizador';

// Enganche del outbox con la app (ADR 0008 §4): envía al abrir la app, al volver a primer plano, al recuperar la
// red, al cerrar una sesión de entreno y cuando vence el backoff. Un solo envío a la vez (lo garantiza el
// sincronizador) y nada sin sesión iniciada.
//
// Al cerrar sesión de usuario, lo pendiente y lo rechazado NO se borra: queda en la base local con su `usuario` y
// se envía cuando esa misma cuenta vuelva a entrar en este móvil. Otra cuenta nunca lo ve ni lo envía.

export const ERROR_ALMACEN = 'No se ha podido abrir el registro local de entrenos.';
export const ERROR_SINCRONIZAR = 'No se ha podido revisar el registro local de entrenos.';

interface ValorSincronizacion {
  /** Null mientras se abre la base, en web/PC (no hay registro sin conexión) o si no se pudo abrir. */
  outbox: Outbox | null;
  /** False en web/PC o si la base local no abrió. */
  disponible: boolean;
  /** True mientras se abre la base local: aún no se sabe si habrá outbox. */
  abriendo: boolean;
  /** Lanza un envío ya (p. ej., un botón «Reintentar»). */
  sincronizar(): Promise<void>;
  enviando: boolean;
  contadores: Contadores;
  /** El servidor dijo que la cuenta ya no existe: se descartó su outbox. Hay que avisar. */
  cuentaEliminada: boolean;
  error: string | null;
  /** Sube tras cada envío: para que las pantallas relean el outbox. */
  revision: number;
}

const ContextoSincronizacion = createContext<ValorSincronizacion | null>(null);

interface PropsProveedorSincronizacion {
  abrirAlmacen: () => Promise<AlmacenRegistro | null>;
  /** Avisa de cada cambio de conectividad; devuelve la función para dejar de escuchar. */
  escucharRed: (oyente: (conectada: boolean) => void) => () => void;
  children: ReactNode;
}

const SIN_CONTADORES: Contadores = { pendientes: 0, rechazadas: 0 };

interface Servicios {
  outbox: Outbox;
  sincronizador: Sincronizador;
}

export function ProveedorSincronizacion({ abrirAlmacen, escucharRed, children }: PropsProveedorSincronizacion) {
  const { estado, api } = useSesion();
  const consultas = useQueryClient();
  const [servicios, setServicios] = useState<Servicios | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [abriendo, setAbriendo] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [contadores, setContadores] = useState<Contadores>(SIN_CONTADORES);
  const [cuentaEliminada, setCuentaEliminada] = useState(false);
  const [revision, setRevision] = useState(0);
  const dentro = estado === 'dentro';
  const dentroRef = useRef(dentro);
  const temporizador = useRef<ReturnType<typeof setTimeout> | null>(null);
  const desmontado = useRef(false);
  const lanzarRef = useRef<(forzar?: boolean) => Promise<void>>(async () => undefined);

  useEffect(() => {
    let vigente = true;
    abrirAlmacen()
      .then(
        (almacen) => {
          if (!vigente || !almacen) return;
          setServicios({
            outbox: crearOutbox({ almacen, usuarioActual: () => api.usuarioActual(), alCerrar: () => void lanzarRef.current() }),
            sincronizador: crearSincronizador({ almacen, api }),
          });
        },
        () => {
          if (vigente) setError(ERROR_ALMACEN);
        },
      )
      .finally(() => {
        if (vigente) setAbriendo(false);
      });
    return () => {
      vigente = false;
    };
  }, [abrirAlmacen, api]);
  const outbox = servicios?.outbox ?? null;
  const sincronizador = servicios?.sincronizador ?? null;

  // Cuenta borrada desde otro dispositivo: lo que este móvil guarda de ella (pendiente, plan, cargas) también se va.
  useEffect(() => {
    if (!outbox) return undefined;
    return api.alBorrarseLaCuenta(async (usuario) => {
      await outbox.borrarDatosDe(usuario);
      setCuentaEliminada(true);
    });
  }, [api, outbox]);

  const lanzar = useCallback(
    async (forzar = false) => {
      if (!outbox || !sincronizador) return;
      if (temporizador.current) clearTimeout(temporizador.current);
      temporizador.current = null;
      setEnviando(true);
      try {
        if (dentroRef.current) {
          const resultado = await sincronizador.sincronizar({ forzar });
          if (resultado.cuentaEliminada) setCuentaEliminada(true);
          if (resultado.confirmadas > 0) void consultas.invalidateQueries({ queryKey: CLAVE_PLAN_ACTUAL });
          // Si el proveedor ya no está, un envío que acaba tarde no deja un reintento colgado.
          if (resultado.proximoIntento !== null && dentroRef.current && !desmontado.current) {
            // Vence el backoff: se reintenta solo, aunque no pase nada más.
            const espera = Math.max(0, resultado.proximoIntento - Date.now());
            // Dos envíos solapados acaban los dos aquí: solo queda un reintento programado.
            if (temporizador.current) clearTimeout(temporizador.current);
            temporizador.current = setTimeout(() => void lanzarRef.current(), espera);
          }
        }
        setContadores(await outbox.contadores());
        setError(null);
      } catch {
        // Solo falla la base local (la red ya la gestiona el sincronizador). Nada de log: hay datos de salud.
        setError(ERROR_SINCRONIZAR);
      } finally {
        setEnviando(false);
        setRevision((r) => r + 1);
      }
    },
    [outbox, sincronizador, consultas],
  );
  useEffect(() => {
    dentroRef.current = dentro;
    lanzarRef.current = lanzar;
  });

  // Al abrir la app, al entrar en la cuenta y en cuanto la base está lista.
  useEffect(() => {
    if (servicios) void lanzarRef.current();
  }, [servicios, dentro]);

  useEffect(() => {
    const suscripcion = AppState.addEventListener('change', (estadoApp) => {
      if (estadoApp === 'active') void lanzarRef.current();
    });
    return () => suscripcion.remove();
  }, []);

  useEffect(() => {
    let conectada = true;
    return escucharRed((ahora) => {
      // Al volver la red se fuerza: no tiene sentido esperar el backoff de cuando no había.
      if (ahora && !conectada) void lanzarRef.current(true);
      conectada = ahora;
    });
  }, [escucharRed]);

  useEffect(() => {
    desmontado.current = false;
    return () => {
      desmontado.current = true;
      if (temporizador.current) clearTimeout(temporizador.current);
    };
  }, []);

  const sincronizar = useCallback(() => lanzar(true), [lanzar]);
  const valor = useMemo(
    () => ({ outbox, disponible: Boolean(outbox), abriendo, sincronizar, enviando, contadores, cuentaEliminada, error, revision }),
    [outbox, abriendo, sincronizar, enviando, contadores, cuentaEliminada, error, revision],
  );
  return <ContextoSincronizacion.Provider value={valor}>{children}</ContextoSincronizacion.Provider>;
}

export function useSincronizacion(): ValorSincronizacion {
  const valor = useContext(ContextoSincronizacion);
  if (!valor) {
    throw new Error('useSincronizacion necesita un ProveedorSincronizacion por encima.');
  }
  return valor;
}

/**
 * Carga siguiente calculada sin red (`idExterno` → kg) que sigue valiendo frente a `plan`, el del servidor. Al
 * cambiar el plan concilia: con otro plan o con la sesión ya confirmada, manda el servidor y la local se borra.
 * Lo que no esté en el mapa se toma de `cargaKg` del plan.
 */
export function useCargasSiguientes(plan: PlanActual | null | undefined): Map<string, number> {
  const { outbox, revision } = useSincronizacion();
  const [cargas, setCargas] = useState<Map<string, number>>(() => new Map());

  useEffect(() => {
    if (!outbox) return;
    let vigente = true;
    (async () => {
      if (plan) await outbox.conciliarConPlan(plan);
      const leidas = await outbox.cargasSiguientes();
      if (vigente) setCargas(leidas);
    })().catch(() => {
      // Sin base local se queda con lo que hubiera: la pantalla cae a la carga del plan.
    });
    return () => {
      vigente = false;
    };
  }, [outbox, plan, revision]);

  return cargas;
}
