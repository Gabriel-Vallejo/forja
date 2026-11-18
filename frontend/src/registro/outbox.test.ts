import { objetivo, planDePrueba } from '../pruebas/dobles';
import { crearAlmacenMemoria } from './almacenMemoria';
import type { AlmacenRegistro } from './almacen';
import { crearOutbox, type DatosSerie, ErrorRegistro, mensajeDeRegistro, type Outbox } from './outbox';

const A = '0193a3c4-7b2e-7c1d-9f00-00000000000a';
const B = '0193a3c4-7b2e-7c1d-9f00-00000000000b';
const PLAN = planDePrueba().uuid;
const AHORA = Date.UTC(2026, 10, 18, 10);

const serie = (cambios: Partial<DatosSerie> = {}): DatosSerie => ({
  ejercicio: 'press_banca',
  orden: 1,
  pesoKg: 80,
  repeticiones: 10,
  rir: 2,
  ...cambios,
});

describe('outbox del registro', () => {
  let almacen: AlmacenRegistro;
  let usuario: string | null;
  let alCerrar: jest.Mock;
  let outbox: Outbox;
  let n: number;

  beforeEach(() => {
    almacen = crearAlmacenMemoria();
    usuario = A;
    alCerrar = jest.fn();
    n = 0;
    outbox = crearOutbox({
      almacen,
      usuarioActual: async () => usuario,
      reloj: () => AHORA,
      nuevoId: () => `id-${++n}`,
      alCerrar,
    });
  });

  test('empezar una sesión la crea en curso, del usuario actual y con su plan', async () => {
    const sesion = await outbox.empezarSesion({ diaPlan: 1, planVersion: PLAN });

    expect(sesion).toMatchObject({
      uuid: 'id-1',
      usuario: A,
      diaPlan: 1,
      planVersion: PLAN,
      estado: 'en_curso',
      realizadaEn: new Date(AHORA).toISOString(),
      intentos: 0,
    });
    expect(await outbox.sesionEnCurso()).toEqual(sesion);
  });

  test('sin sesión iniciada no se puede registrar, y no hay dos sesiones en curso a la vez', async () => {
    usuario = null;
    await expect(outbox.empezarSesion()).rejects.toThrow(ErrorRegistro);
    expect(await outbox.sesionEnCurso()).toBeNull();

    usuario = A;
    await outbox.empezarSesion();
    await expect(outbox.empezarSesion()).rejects.toThrow('Ya tienes una sesión en curso.');
  });

  test('registra series con UUID propio; valida los rangos y redondea el peso a centésimas', async () => {
    const { uuid } = await outbox.empezarSesion();
    const registrada = await outbox.registrarSerie(uuid, serie({ pesoKg: 82.499, rpe: 8.46, dolor: 3, calentamiento: true }));

    expect(registrada).toMatchObject({ uuid: 'id-2', sesion: uuid, pesoKg: 82.5, rpe: 8.5, dolor: 3, calentamiento: true, corrigeA: null, anulada: false });
    await expect(outbox.registrarSerie(uuid, serie({ repeticiones: 101 }))).rejects.toThrow('Las repeticiones van de 0 a 100.');
    await expect(outbox.registrarSerie(uuid, serie({ dolor: 11 }))).rejects.toThrow('El dolor va de 0 a 10.');
    await expect(outbox.registrarSerie(uuid, serie({ ejercicio: '' }))).rejects.toThrow(ErrorRegistro);
    await expect(outbox.registrarSerie('no-existe', serie())).rejects.toThrow('No existe esa sesión.');
  });

  test('corregir y anular añaden series nuevas que apuntan a la última versión; nada se edita ni se borra', async () => {
    const { uuid } = await outbox.empezarSesion();
    const original = await outbox.registrarSerie(uuid, serie({ repeticiones: 8 }));
    const otra = await outbox.registrarSerie(uuid, serie({ orden: 2 }));
    const correccion = await outbox.corregirSerie(uuid, original.uuid, serie({ repeticiones: 9 }));
    const segunda = await outbox.corregirSerie(uuid, original.uuid, serie({ repeticiones: 10 }));
    const anulacion = await outbox.anularSerie(uuid, otra.uuid);

    expect(correccion.corrigeA).toBe(original.uuid);
    expect(segunda.corrigeA).toBe(correccion.uuid);
    expect(anulacion).toMatchObject({ corrigeA: otra.uuid, anulada: true, ejercicio: 'press_banca', dolor: null });
    expect(await almacen.listarSeries(uuid)).toHaveLength(5);
    expect(await outbox.seriesValidas(uuid)).toEqual([segunda]);
    await expect(outbox.anularSerie(uuid, otra.uuid)).rejects.toThrow('Esa serie ya está anulada.');
    await expect(outbox.corregirSerie(uuid, 'no-existe', serie())).rejects.toThrow('No existe esa serie en la sesión.');
  });

  test('cerrar pasa la sesión a pendiente, guarda la carga siguiente y avisa para sincronizar', async () => {
    const { uuid } = await outbox.empezarSesion({ diaPlan: 1, planVersion: PLAN });
    for (let i = 0; i < 4; i++) await outbox.registrarSerie(uuid, serie());
    await outbox.registrarSerie(uuid, serie({ ejercicio: 'remo', pesoKg: 60, repeticiones: 5 }));
    await outbox.registrarSerie(uuid, serie({ ejercicio: 'fondos', pesoKg: 0 }));
    const objetivos = planDePrueba().dias[0]!.ejercicios;

    await outbox.cerrarSesion(uuid, objetivos);

    expect(await almacen.leerSesion(uuid)).toMatchObject({ estado: 'pendiente', cerradaEn: new Date(AHORA).toISOString() });
    expect(await outbox.cargasSiguientes()).toEqual(new Map([['press_banca', 82.5], ['remo', 60]]));
    expect(alCerrar).toHaveBeenCalledTimes(1);
    expect(await outbox.sesionEnCurso()).toBeNull();
    await expect(outbox.cerrarSesion(uuid)).rejects.toThrow('Esa sesión ya está cerrada.');
  });

  test('cerrar una sesión vacía no se puede: se descarta', async () => {
    const { uuid } = await outbox.empezarSesion();
    await expect(outbox.cerrarSesion(uuid)).rejects.toThrow('No has registrado ninguna serie: descarta la sesión.');

    await outbox.descartarSesion(uuid);

    expect(await almacen.leerSesion(uuid)).toBeNull();
  });

  test('solo se descarta una sesión en curso y sin series, o una rechazada', async () => {
    const { uuid } = await outbox.empezarSesion();
    await outbox.registrarSerie(uuid, serie());

    await expect(outbox.descartarSesion(uuid)).rejects.toThrow('Solo se descarta una sesión en curso y sin series, o una rechazada.');
    await outbox.cerrarSesion(uuid);
    await expect(outbox.descartarSesion(uuid)).rejects.toThrow(ErrorRegistro);
    expect(await almacen.leerSesion(uuid)).not.toBeNull();

    await almacen.actualizarSesion(uuid, { estado: 'rechazada', ultimoError: 'revisa esta sesión' });
    await outbox.descartarSesion(uuid);

    expect(await almacen.leerSesion(uuid)).toBeNull();
    expect(await almacen.listarSeries(uuid)).toEqual([]);
  });

  test('reintentar una rechazada la deja pendiente desde cero y lanza el envío; otra cosa no se reintenta', async () => {
    const { uuid } = await outbox.empezarSesion();
    await outbox.registrarSerie(uuid, serie());
    await outbox.cerrarSesion(uuid);
    alCerrar.mockClear();
    await expect(outbox.reintentarSesion(uuid)).rejects.toThrow('Solo se reintenta una sesión rechazada.');
    await almacen.actualizarSesion(uuid, { estado: 'rechazada', intentos: 3, ultimoError: 'mal', proximoIntento: 1 });

    await outbox.reintentarSesion(uuid);

    expect(await almacen.leerSesion(uuid)).toMatchObject({ estado: 'pendiente', intentos: 0, ultimoError: null, proximoIntento: null });
    expect(alCerrar).toHaveBeenCalledTimes(1);
    usuario = B;
    await expect(outbox.reintentarSesion(uuid)).rejects.toThrow('No existe esa sesión.');
  });

  test('mensajeDeRegistro: el de un ErrorRegistro tal cual; cualquier otro, uno genérico sin detalles', () => {
    expect(mensajeDeRegistro(new ErrorRegistro('Falta el ejercicio.'))).toBe('Falta el ejercicio.');
    expect(mensajeDeRegistro(new Error('SQLITE_FULL dolor=7'))).toBe('No se ha podido usar el registro del móvil. Inténtalo otra vez.');
  });

  test('añadir a una sesión ya cerrada (p. ej. rechazada) la vuelve a dejar pendiente desde cero', async () => {
    const { uuid } = await outbox.empezarSesion();
    const s = await outbox.registrarSerie(uuid, serie());
    await outbox.cerrarSesion(uuid);
    await almacen.actualizarSesion(uuid, { estado: 'rechazada', intentos: 3, ultimoError: 'mal', proximoIntento: 1 });

    await outbox.corregirSerie(uuid, s.uuid, serie({ repeticiones: 9 }));

    expect(await almacen.leerSesion(uuid)).toMatchObject({ estado: 'pendiente', intentos: 0, ultimoError: null, proximoIntento: null });
  });

  test('una serie añadida mientras se envía no toca el estado: el sincronizador lo detecta', async () => {
    const { uuid } = await outbox.empezarSesion();
    await outbox.registrarSerie(uuid, serie());
    await outbox.cerrarSesion(uuid);
    await almacen.actualizarSesion(uuid, { estado: 'enviando' });

    await outbox.registrarSerie(uuid, serie({ orden: 2 }));

    expect((await almacen.leerSesion(uuid))!.estado).toBe('enviando');
  });

  test('no deja tocar ni ver las sesiones de otra cuenta', async () => {
    const { uuid } = await outbox.empezarSesion();
    await outbox.registrarSerie(uuid, serie());
    await outbox.cerrarSesion(uuid);
    usuario = B;

    await expect(outbox.registrarSerie(uuid, serie())).rejects.toThrow('No existe esa sesión.');
    expect(await outbox.porRevisar()).toEqual([]);
    expect(await outbox.contadores()).toEqual({ pendientes: 0, rechazadas: 0 });
  });

  test('porRevisar y contadores: lo que falta por subir y lo rechazado, del usuario actual', async () => {
    for (const estado of ['pendiente', 'rechazada', 'enviando', 'confirmada'] as const) {
      const { uuid } = await outbox.empezarSesion();
      await outbox.registrarSerie(uuid, serie());
      await outbox.cerrarSesion(uuid);
      await almacen.actualizarSesion(uuid, { estado });
    }
    await outbox.empezarSesion();

    expect((await outbox.porRevisar()).map((s) => s.estado)).toEqual(['pendiente', 'rechazada', 'enviando']);
    expect(await outbox.contadores()).toEqual({ pendientes: 2, rechazadas: 1 });
  });

  test('con el plan del servidor manda el servidor: se borran las cargas de otro plan o de sesiones confirmadas', async () => {
    const crearConCarga = async (ejercicio: string, planVersion: string) => {
      const { uuid } = await outbox.empezarSesion({ diaPlan: 1, planVersion });
      for (let i = 0; i < 4; i++) await outbox.registrarSerie(uuid, serie({ ejercicio }));
      await outbox.cerrarSesion(uuid, [objetivo({ idExterno: ejercicio })]);
      return uuid;
    };
    const confirmada = await crearConCarga('press_banca', PLAN);
    await crearConCarga('remo', PLAN);
    await crearConCarga('sentadilla', 'plan-viejo');
    await almacen.actualizarSesion(confirmada, { estado: 'confirmada' });

    await outbox.conciliarConPlan(planDePrueba());

    expect(await outbox.cargasSiguientes()).toEqual(new Map([['remo', 82.5]]));
  });

  test('guarda el último plan bueno del usuario actual; null (sin plan en el servidor) lo borra', async () => {
    const plan = planDePrueba();
    await outbox.guardarPlan(plan);

    expect(await outbox.planGuardado()).toEqual({ plan, guardadoEn: AHORA });
    usuario = B;
    expect(await outbox.planGuardado()).toBeNull();
    usuario = A;
    await outbox.guardarPlan(null);
    expect(await outbox.planGuardado()).toBeNull();
  });

  test('un plan guardado que no cumple el esquema no se usa', async () => {
    await almacen.guardarPlan({ usuario: A, planUuid: 'x', json: '{"roto":true', guardadoEn: 1 });
    expect(await outbox.planGuardado()).toBeNull();
    await almacen.guardarPlan({ usuario: A, planUuid: 'x', json: '{"uuid":"no"}', guardadoEn: 1 });
    expect(await outbox.planGuardado()).toBeNull();
  });

  test('sin usuario las consultas salen vacías', async () => {
    usuario = null;

    expect(await outbox.porRevisar()).toEqual([]);
    expect(await outbox.cargasSiguientes()).toEqual(new Map());
    await outbox.conciliarConPlan(planDePrueba());
    await outbox.guardarPlan(planDePrueba());
    expect(await outbox.planGuardado()).toBeNull();
  });
});
