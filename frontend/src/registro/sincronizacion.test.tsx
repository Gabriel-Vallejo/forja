import { act, renderHook, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { AppState, type AppStateStatus } from 'react-native';

import { ErrorApi } from '../api/errores';
import type { PlanActual } from '../api/esquemas';
import type { ApiForja } from '../api/forja';
import { CLAVE_PLAN_ACTUAL } from '../plan/plan';
import { apiFalsa, objetivo, planDePrueba, USUARIO_ID } from '../pruebas/dobles';
import { ProveedorSesion } from '../sesion/sesion';
import type { AlmacenRegistro } from './almacen';
import { crearAlmacenMemoria } from './almacenMemoria';
import { crearOutbox } from './outbox';
import { ProveedorSincronizacion, useCargasSiguientes, useSincronizacion } from './sincronizacion';

const SERIE = { ejercicio: 'press_banca', orden: 1, pesoKg: 80, repeticiones: 10, rir: 2 };

/** Deja en el almacén una sesión cerrada del usuario de prueba, sin pasar por el proveedor. */
async function sesionPendiente(almacen: AlmacenRegistro): Promise<string> {
  const outbox = crearOutbox({ almacen, usuarioActual: async () => USUARIO_ID });
  const { uuid } = await outbox.empezarSesion();
  await outbox.registrarSerie(uuid, SERIE);
  await outbox.cerrarSesion(uuid);
  return uuid;
}

function montar({ api = apiFalsa(), almacen = crearAlmacenMemoria() as AlmacenRegistro | null } = {}) {
  let avisarRed: (conectada: boolean) => void = () => undefined;
  const escucharRed = jest.fn((oyente: (c: boolean) => void) => {
    avisarRed = oyente;
    return () => undefined;
  });
  let avisarApp: (estado: AppStateStatus) => void = () => undefined;
  jest.spyOn(AppState, 'addEventListener').mockImplementation((_tipo, oyente) => {
    avisarApp = oyente as (estado: AppStateStatus) => void;
    return { remove: () => undefined } as ReturnType<typeof AppState.addEventListener>;
  });
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } });
  const invalidar = jest.spyOn(consultas, 'invalidateQueries');
  const envoltorio = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={consultas}>
      <ProveedorSesion api={api}>
        <ProveedorSincronizacion abrirAlmacen={async () => almacen} escucharRed={escucharRed}>
          {children}
        </ProveedorSincronizacion>
      </ProveedorSesion>
    </QueryClientProvider>
  );
  return {
    api: api as jest.Mocked<ApiForja>,
    invalidar,
    envoltorio,
    red: (conectada: boolean) => act(async () => avisarRed(conectada)),
    app: (estado: AppStateStatus) => act(async () => avisarApp(estado)),
  };
}

describe('sincronización en la app', () => {
  afterEach(() => jest.restoreAllMocks());

  test('al abrir la app con sesión iniciada envía lo pendiente y relee el plan', async () => {
    const almacen = crearAlmacenMemoria();
    const uuid = await sesionPendiente(almacen);
    const { api, envoltorio, invalidar } = montar({ almacen });

    const { result } = await renderHook(() => useSincronizacion(), { wrapper: envoltorio });

    await waitFor(() => expect((api.guardarSesion as jest.Mock).mock.calls).toHaveLength(1));
    expect(api.guardarSesion).toHaveBeenCalledWith(uuid, expect.objectContaining({ series: [expect.objectContaining({ repeticiones: 10 })] }));
    await waitFor(() => expect(result.current.enviando).toBe(false));
    expect(result.current.contadores).toEqual({ pendientes: 0, rechazadas: 0 });
    expect(invalidar).toHaveBeenCalledWith({ queryKey: CLAVE_PLAN_ACTUAL });
  });

  test('mientras se abre la base local dice que está abriendo', async () => {
    let abrir: (a: AlmacenRegistro | null) => void = () => undefined;
    const { envoltorio } = montar();
    const abrirAlmacen = () => new Promise<AlmacenRegistro | null>((r) => (abrir = r));
    const { result } = await renderHook(() => useSincronizacion(), {
      wrapper: ({ children }) =>
        envoltorio({ children: <ProveedorSincronizacion abrirAlmacen={abrirAlmacen} escucharRed={() => () => undefined}>{children}</ProveedorSincronizacion> }),
    });
    expect(result.current.abriendo).toBe(true);

    await act(async () => abrir(crearAlmacenMemoria()));

    expect(result.current.abriendo).toBe(false);
    expect(result.current.outbox).not.toBeNull();
  });

  test('sin sesión iniciada no envía nada y lo pendiente se queda', async () => {
    const almacen = crearAlmacenMemoria();
    await sesionPendiente(almacen);
    const { api, envoltorio } = montar({ almacen, api: apiFalsa({ haySesionGuardada: jest.fn().mockResolvedValue(false) }) });

    const { result } = await renderHook(() => useSincronizacion(), { wrapper: envoltorio });

    await waitFor(() => expect(result.current.outbox).not.toBeNull());
    await waitFor(() => expect(result.current.contadores).toEqual({ pendientes: 1, rechazadas: 0 }));
    expect(api.guardarSesion).not.toHaveBeenCalled();
  });

  test('al volver la red reenvía aunque estuviera esperando el backoff', async () => {
    const almacen = crearAlmacenMemoria();
    await sesionPendiente(almacen);
    const api = apiFalsa({ guardarSesion: jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'sin red')).mockResolvedValue(undefined) });
    const { envoltorio, red } = montar({ almacen, api });
    const { result } = await renderHook(() => useSincronizacion(), { wrapper: envoltorio });
    await waitFor(() => expect(result.current.contadores.pendientes).toBe(1));
    expect(api.guardarSesion).toHaveBeenCalledTimes(1);

    await red(false);
    await red(true);

    await waitFor(() => expect(result.current.contadores.pendientes).toBe(0));
    expect(api.guardarSesion).toHaveBeenCalledTimes(2);
  });

  test('si se desmonta con un envío en vuelo, no deja programado el reintento', async () => {
    const almacen = crearAlmacenMemoria();
    await sesionPendiente(almacen);
    let fallar: (e: Error) => void = () => undefined;
    const api = apiFalsa({ guardarSesion: jest.fn(() => new Promise<void>((_, rechazar) => (fallar = rechazar))) });
    const { envoltorio } = montar({ almacen, api });
    const { unmount } = await renderHook(() => useSincronizacion(), { wrapper: envoltorio });
    await waitFor(() => expect(api.guardarSesion).toHaveBeenCalledTimes(1));
    const programar = jest.spyOn(globalThis, 'setTimeout');

    await unmount();
    await act(async () => fallar(new ErrorApi(0, 'sin red')));

    expect(programar.mock.calls.filter((llamada) => (llamada[1] ?? 0) >= 1000)).toEqual([]);
  });

  test('al volver a primer plano lo intenta otra vez', async () => {
    const almacen = crearAlmacenMemoria();
    const { api, envoltorio, app } = montar({ almacen });
    const { result } = await renderHook(() => useSincronizacion(), { wrapper: envoltorio });
    await waitFor(() => expect(result.current.outbox).not.toBeNull());
    await waitFor(() => expect(result.current.enviando).toBe(false));
    await sesionPendiente(almacen);

    await app('background');
    expect(api.guardarSesion).not.toHaveBeenCalled();
    await app('active');

    await waitFor(() => expect(api.guardarSesion).toHaveBeenCalledTimes(1));
  });

  test('cerrar una sesión de entreno lanza el envío', async () => {
    const { api, envoltorio } = montar();
    const { result } = await renderHook(() => useSincronizacion(), { wrapper: envoltorio });
    await waitFor(() => expect(result.current.outbox).not.toBeNull());
    const outbox = result.current.outbox!;

    await act(async () => {
      const { uuid } = await outbox.empezarSesion();
      await outbox.registrarSerie(uuid, SERIE);
      await outbox.cerrarSesion(uuid);
    });

    await waitFor(() => expect(api.guardarSesion).toHaveBeenCalledTimes(1));
  });

  test('si la cuenta ya no existe, lo avisa', async () => {
    const almacen = crearAlmacenMemoria();
    await sesionPendiente(almacen);
    const { envoltorio } = montar({ almacen, api: apiFalsa({ guardarSesion: jest.fn().mockRejectedValue(new ErrorApi(410, 'ya no existe')) }) });

    const { result } = await renderHook(() => useSincronizacion(), { wrapper: envoltorio });

    await waitFor(() => expect(result.current.cuentaEliminada).toBe(true));
    expect(await almacen.listarSesiones()).toEqual([]);
  });

  test('si la base local no abre, la app sigue sin registro y lo dice', async () => {
    const { envoltorio } = montar();
    const abrirAlmacen = jest.fn().mockRejectedValue(new Error('disco lleno'));
    const { result } = await renderHook(() => useSincronizacion(), {
      wrapper: ({ children }) =>
        envoltorio({ children: <ProveedorSincronizacion abrirAlmacen={abrirAlmacen} escucharRed={() => () => undefined}>{children}</ProveedorSincronizacion> }),
    });

    await waitFor(() => expect(result.current.error).toBe('No se ha podido abrir el registro local de entrenos.'));
    expect(result.current.outbox).toBeNull();
    expect(result.current.abriendo).toBe(false);
  });

  test('en web no hay almacén: sin outbox y sin romper', async () => {
    const { api, envoltorio } = montar({ almacen: null });

    const { result } = await renderHook(() => useSincronizacion(), { wrapper: envoltorio });

    await waitFor(() => expect(result.current.abriendo).toBe(false));
    expect(result.current.disponible).toBe(false);
    expect(result.current.outbox).toBeNull();
    await act(() => result.current.sincronizar());
    expect(api.guardarSesion).not.toHaveBeenCalled();
  });

  test('useCargasSiguientes concilia con el plan del servidor y da las cargas locales que siguen valiendo', async () => {
    const almacen = crearAlmacenMemoria();
    const outbox = crearOutbox({ almacen, usuarioActual: async () => USUARIO_ID });
    const plan = planDePrueba();
    const { uuid } = await outbox.empezarSesion({ diaPlan: 1, planVersion: plan.uuid });
    for (let i = 0; i < 4; i++) await outbox.registrarSerie(uuid, SERIE);
    await outbox.cerrarSesion(uuid, [objetivo()]);
    const api = apiFalsa({ guardarSesion: jest.fn().mockRejectedValue(new ErrorApi(0, 'sin red')) });
    const { envoltorio } = montar({ almacen, api });

    const { result, rerender } = await renderHook(({ p }: { p: PlanActual }) => useCargasSiguientes(p), { wrapper: envoltorio, initialProps: { p: plan } });
    await waitFor(() => expect(result.current.get('press_banca')).toBe(82.5));

    await rerender({ p: { ...plan, uuid: '0193a3c4-7b2e-7c1d-9f00-000000000002' } });
    await waitFor(() => expect(result.current.size).toBe(0));
  });

  test('useSincronizacion fuera del proveedor avisa', async () => {
    jest.spyOn(console, 'error').mockImplementation(() => undefined);
    await expect(renderHook(() => useSincronizacion())).rejects.toThrow('useSincronizacion necesita un ProveedorSincronizacion por encima.');
  });
});
