import { ErrorApi } from '../api/errores';
import type { SesionEntrada } from '../api/esquemas';
import type { AlmacenRegistro } from './almacen';
import { crearAlmacenMemoria } from './almacenMemoria';
import { crearOutbox, type Outbox } from './outbox';
import {
  aSesionEntrada,
  crearSincronizador,
  DIAS_RETENCION_CONFIRMADAS,
  ESPERA_MAXIMA_MS,
  esperaReintento,
  type Sincronizador,
} from './sincronizador';

const A = '0193a3c4-7b2e-7c1d-9f00-00000000000a';
const B = '0193a3c4-7b2e-7c1d-9f00-00000000000b';
const DIA_MS = 24 * 60 * 60 * 1000;

/**
 * Backend falso con las reglas de `ServicioSesiones`: upsert por uuid, series que solo se añaden y una serie ya
 * guardada tiene que llegar idéntica (si no, 409). `fallo` simula la red o el servidor en la siguiente llamada.
 */
function backendFalso() {
  const sesiones = new Map<string, SesionEntrada>();
  const llamadas: string[] = [];
  let siguiente: ErrorApi | null = null;
  let siempre: number | null = null;
  return {
    sesiones,
    llamadas,
    /** Solo la siguiente llamada falla. */
    fallarCon(estado: number, mensaje = 'fallo') {
      siguiente = new ErrorApi(estado, mensaje);
    },
    /** Todas fallan hasta llamar a lo que devuelve. */
    fallarSiempre(estado: number) {
      siempre = estado;
      return () => {
        siempre = null;
      };
    },
    guardarSesion: jest.fn(async (uuid: string, sesion: SesionEntrada) => {
      llamadas.push(uuid);
      const error = siguiente ?? (siempre === null ? null : new ErrorApi(siempre, 'fallo'));
      siguiente = null;
      if (error) throw error;
      const previa = sesiones.get(uuid);
      for (const serie of previa?.series ?? []) {
        const llegada = sesion.series.find((s) => s.uuid === serie.uuid);
        if (llegada && JSON.stringify(llegada) !== JSON.stringify(serie)) throw new ErrorApi(409, 'Esa serie ya existe con otros datos.');
      }
      const series = [...(previa?.series ?? [])];
      for (const s of sesion.series) if (!series.some((p) => p.uuid === s.uuid)) series.push(s);
      sesiones.set(uuid, { ...sesion, series });
    }),
  };
}

describe('sincronizador', () => {
  let almacen: AlmacenRegistro;
  let backend: ReturnType<typeof backendFalso>;
  let usuario: string | null;
  let ahora: number;
  let outbox: Outbox;
  let sinc: Sincronizador;

  beforeEach(() => {
    almacen = crearAlmacenMemoria();
    backend = backendFalso();
    usuario = A;
    ahora = Date.UTC(2026, 10, 18, 10);
    const usuarioActual = async () => usuario;
    outbox = crearOutbox({ almacen, usuarioActual, reloj: () => ahora });
    sinc = crearSincronizador({ almacen, api: { guardarSesion: backend.guardarSesion, usuarioActual }, reloj: () => ahora, azar: () => 1 });
  });

  async function sesionCerrada(repeticiones = [8, 8]): Promise<string> {
    const { uuid } = await outbox.empezarSesion({ diaPlan: 1 });
    for (const r of repeticiones) {
      ahora += 1;
      await outbox.registrarSerie(uuid, { ejercicio: 'press_banca', orden: 1, pesoKg: 80, repeticiones: r, rir: 2, dolor: 2 });
    }
    await outbox.cerrarSesion(uuid);
    return uuid;
  }

  const estado = async (uuid: string) => (await almacen.leerSesion(uuid))?.estado;

  test('sin red se registra, se corrige, vuelve la red y el servidor queda con el mismo estado', async () => {
    backend.fallarCon(0);
    const uuid = await sesionCerrada();
    await sinc.sincronizar();
    expect(await estado(uuid)).toBe('pendiente');

    const [primera] = await almacen.listarSeries(uuid);
    await outbox.corregirSerie(uuid, primera!.uuid, { ejercicio: 'press_banca', orden: 1, pesoKg: 80, repeticiones: 10, rir: 1 });
    await outbox.anularSerie(uuid, (await almacen.listarSeries(uuid))[1]!.uuid);
    const resultado = await sinc.sincronizar({ forzar: true });

    expect(resultado.confirmadas).toBe(1);
    expect(await estado(uuid)).toBe('confirmada');
    expect(backend.sesiones.get(uuid)).toEqual(aSesionEntrada((await almacen.leerSesion(uuid))!, await almacen.listarSeries(uuid)));
    expect(backend.sesiones.get(uuid)!.series).toHaveLength(4);
  });

  test('mapea la sesión local a SesionEntrada con todas sus series y sin campos vacíos', async () => {
    const uuid = await sesionCerrada([8]);
    const sesion = (await almacen.leerSesion(uuid))!;
    const [serie] = await almacen.listarSeries(uuid);

    expect(aSesionEntrada(sesion, [serie!])).toEqual({
      realizadaEn: sesion.realizadaEn,
      diaPlan: 1,
      series: [
        {
          uuid: serie!.uuid,
          ejercicio: 'press_banca',
          orden: 1,
          pesoKg: 80,
          repeticiones: 8,
          rir: 2,
          dolor: 2,
          calentamiento: false,
          anulada: false,
          realizadaEn: serie!.realizadaEn,
        },
      ],
    });
  });

  test('reenviar dos veces no duplica nada', async () => {
    const uuid = await sesionCerrada();
    await sinc.sincronizar();
    await almacen.actualizarSesion(uuid, { estado: 'pendiente' });
    await sinc.sincronizar();

    expect(backend.llamadas).toEqual([uuid, uuid]);
    expect(backend.sesiones.get(uuid)!.series).toHaveLength(2);
  });

  test('un 4xx de validación la deja rechazada con el mensaje del backend y no se reenvía sola', async () => {
    const uuid = await sesionCerrada();
    backend.fallarCon(422, 'Hay ejercicios que no existen.');

    const resultado = await sinc.sincronizar({ forzar: true });
    await sinc.sincronizar({ forzar: true });

    expect(resultado.rechazadas).toBe(1);
    expect(await almacen.leerSesion(uuid)).toMatchObject({ estado: 'rechazada', ultimoError: 'Hay ejercicios que no existen.' });
    expect(backend.llamadas).toHaveLength(1);
    expect(await outbox.contadores()).toEqual({ pendientes: 0, rechazadas: 1 });
  });

  test('un 401 a mitad no pierde nada: vuelve a pendiente sin gastar intentos y se envía tras renovar', async () => {
    const primera = await sesionCerrada();
    const segunda = await sesionCerrada();
    backend.fallarCon(401, 'Tu sesión ha caducado.');

    await sinc.sincronizar();

    expect(await almacen.leerSesion(primera)).toMatchObject({ estado: 'pendiente', intentos: 0 });
    expect(await estado(segunda)).toBe('pendiente');
    expect(backend.llamadas).toEqual([primera]);

    await sinc.sincronizar();
    expect(await estado(primera)).toBe('confirmada');
    expect(await estado(segunda)).toBe('confirmada');
  });

  test('un 410 (cuenta inexistente) avisa y descarta todo lo de esa cuenta, y nada de las demás', async () => {
    const propia = await sesionCerrada();
    usuario = B;
    const ajena = await sesionCerrada();
    usuario = A;
    await almacen.guardarPlan({ usuario: A, planUuid: 'pa', json: '{}', guardadoEn: 1 });
    await almacen.guardarPlan({ usuario: B, planUuid: 'pb', json: '{}', guardadoEn: 1 });
    backend.fallarCon(410);

    const resultado = await sinc.sincronizar();

    expect(resultado.cuentaEliminada).toBe(true);
    expect(await almacen.leerSesion(propia)).toBeNull();
    expect(await estado(ajena)).toBe('pendiente');
    expect(await almacen.leerPlan(A)).toBeNull();
    expect(await almacen.leerPlan(B)).not.toBeNull();
  });

  test('una sesión que se quedó en «enviando» (la app se cerró a mitad) vuelve a pendiente y se envía', async () => {
    const uuid = await sesionCerrada();
    await almacen.actualizarSesion(uuid, { estado: 'enviando' });

    await crearSincronizador({ almacen, api: { guardarSesion: backend.guardarSesion, usuarioActual: async () => A } }).sincronizar();

    expect(await estado(uuid)).toBe('confirmada');
  });

  test('nunca envía lo de otra cuenta con el token actual, ni nada sin sesión iniciada', async () => {
    const deA = await sesionCerrada();
    usuario = B;

    await sinc.sincronizar();
    expect(backend.llamadas).toEqual([]);
    expect(await estado(deA)).toBe('pendiente');

    usuario = null;
    expect(await sinc.sincronizar()).toMatchObject({ confirmadas: 0 });
    expect(backend.llamadas).toEqual([]);
  });

  test('si cambia la cuenta a mitad de una vuelta, para antes de enviar lo siguiente', async () => {
    const primera = await sesionCerrada();
    const segunda = await sesionCerrada();
    backend.guardarSesion.mockImplementationOnce(async () => {
      usuario = B;
    });

    await sinc.sincronizar();

    expect(await estado(primera)).toBe('confirmada');
    expect(await estado(segunda)).toBe('pendiente');
  });

  test('red caída o 5xx: vuelve a pendiente con intentos+1 y no se reintenta antes de tiempo salvo forzando', async () => {
    const uuid = await sesionCerrada();
    const reponer = backend.fallarSiempre(503);

    const resultado = await sinc.sincronizar();
    const tras1 = (await almacen.leerSesion(uuid))!;
    expect(tras1).toMatchObject({ estado: 'pendiente', intentos: 1, ultimoError: 'fallo' });
    expect(resultado.proximoIntento).toBe(tras1.proximoIntento);
    expect(tras1.proximoIntento! - ahora).toBe(esperaReintento(1, 1));

    await sinc.sincronizar();
    expect(backend.llamadas).toHaveLength(1);

    ahora = tras1.proximoIntento!;
    await sinc.sincronizar();
    expect((await almacen.leerSesion(uuid))!.intentos).toBe(2);

    reponer();
    await sinc.sincronizar({ forzar: true });
    expect(await almacen.leerSesion(uuid)).toMatchObject({ estado: 'confirmada', intentos: 2, ultimoError: null });
  });

  test('una serie añadida mientras se enviaba deja la sesión pendiente para el siguiente envío', async () => {
    const uuid = await sesionCerrada();
    backend.guardarSesion.mockImplementationOnce(async () => {
      await outbox.registrarSerie(uuid, { ejercicio: 'press_banca', orden: 2, pesoKg: 80, repeticiones: 8 });
    });

    await sinc.sincronizar();
    expect(await estado(uuid)).toBe('pendiente');

    await sinc.sincronizar();
    expect(await estado(uuid)).toBe('confirmada');
    expect(backend.sesiones.get(uuid)!.series).toHaveLength(3);
  });

  test('un solo envío a la vez; lo que se pide durante un envío se hace en otra vuelta al acabar', async () => {
    const primera = await sesionCerrada();
    let soltar!: () => void;
    backend.guardarSesion.mockImplementationOnce(() => new Promise<void>((r) => (soltar = r)));

    const envio = sinc.sincronizar();
    await Promise.resolve();
    const segunda = await sesionCerrada();
    const otro = sinc.sincronizar();
    await new Promise((r) => setTimeout(r, 0));
    soltar();
    await Promise.all([envio, otro]);

    expect(backend.guardarSesion).toHaveBeenCalledTimes(2);
    expect(await estado(primera)).toBe('confirmada');
    expect(await estado(segunda)).toBe('confirmada');
  });

  test('purga las confirmadas pasados los días de retención; las pendientes y rechazadas nunca', async () => {
    const confirmada = await sesionCerrada();
    await sinc.sincronizar();
    const rechazada = await sesionCerrada();
    backend.fallarCon(400);
    await sinc.sincronizar();
    backend.fallarSiempre(0);
    const pendiente = await sesionCerrada();
    await sinc.sincronizar();

    const confirmadaEn = (await almacen.leerSesion(confirmada))!.confirmadaEn!;
    ahora = confirmadaEn + DIAS_RETENCION_CONFIRMADAS * DIA_MS;
    await sinc.sincronizar();
    expect(await estado(confirmada)).toBe('confirmada');

    ahora += 1;
    await sinc.sincronizar();
    expect(await almacen.leerSesion(confirmada)).toBeNull();
    expect(await estado(rechazada)).toBe('rechazada');
    expect(await estado(pendiente)).toBe('pendiente');
  });
});

describe('esperaReintento', () => {
  test('crece exponencialmente, con jitter entre la mitad y el total, y con tope', () => {
    const maximas = [1, 2, 3, 4, 5, 6].map((n) => esperaReintento(n, 1));

    expect(maximas).toEqual([...maximas].sort((a, b) => a - b));
    expect(maximas[1]).toBe(maximas[0]! * 2);
    expect(esperaReintento(3, 0)).toBe(esperaReintento(3, 1) / 2);
    expect(esperaReintento(50, 1)).toBe(ESPERA_MAXIMA_MS);
    expect(esperaReintento(1000, 0.5)).toBeLessThanOrEqual(ESPERA_MAXIMA_MS);
  });
});
