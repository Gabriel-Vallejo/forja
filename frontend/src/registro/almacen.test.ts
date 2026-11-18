import { baseSqliteEnMemoria } from '../pruebas/sqliteNode';
import type { AlmacenRegistro, SerieLocal, SesionLocal } from './almacen';
import { crearAlmacenMemoria } from './almacenMemoria';
import { crearAlmacenSqlite } from './almacenSqlite';
import { migrar, MIGRACIONES } from './migraciones';

const A = 'usuario-a';
const B = 'usuario-b';

function sesion(uuid: string, cambios: Partial<SesionLocal> = {}): SesionLocal {
  return {
    uuid,
    usuario: A,
    diaPlan: 1,
    planVersion: 'plan-1',
    realizadaEn: '2026-11-18T10:00:00.000Z',
    cerradaEn: null,
    estado: 'en_curso',
    intentos: 0,
    proximoIntento: null,
    ultimoError: null,
    confirmadaEn: null,
    ...cambios,
  };
}

function serie(uuid: string, sesionUuid: string, cambios: Partial<SerieLocal> = {}): SerieLocal {
  return {
    uuid,
    sesion: sesionUuid,
    ejercicio: 'press_banca',
    orden: 1,
    pesoKg: 80.5,
    repeticiones: 8,
    rir: 2,
    rpe: 8.5,
    dolor: 3,
    calentamiento: false,
    corrigeA: null,
    anulada: false,
    realizadaEn: '2026-11-18T10:05:00.000Z',
    ...cambios,
  };
}

/** Inserta con el SQL de la versión 1 sin pasar por `migrar` (que subiría a la última). */
async function crearAlmacenSqliteSinMigrar(base: ReturnType<typeof baseSqliteEnMemoria>) {
  return {
    insertarSesion: (s: SesionLocal) =>
      base.runAsync(
        `INSERT INTO sesion (uuid, usuario, dia_plan, plan_version, realizada_en, cerrada_en, estado, intentos,
          proximo_intento, ultimo_error, confirmada_en) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [s.uuid, s.usuario, s.diaPlan, s.planVersion, s.realizadaEn, s.cerradaEn, s.estado, s.intentos, s.proximoIntento, s.ultimoError, s.confirmadaEn],
      ),
    insertarSerie: (x: SerieLocal) =>
      base.runAsync(
        `INSERT INTO serie (uuid, sesion, ejercicio, orden, peso_kg, repeticiones, rir, rpe, dolor, calentamiento,
          corrige_a, anulada, realizada_en) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [x.uuid, x.sesion, x.ejercicio, x.orden, x.pesoKg, x.repeticiones, x.rir, x.rpe, x.dolor, x.calentamiento ? 1 : 0, x.corrigeA, x.anulada ? 1 : 0, x.realizadaEn],
      ),
  };
}

const implementaciones: [string, () => Promise<AlmacenRegistro>][] = [
  ['en memoria', async () => crearAlmacenMemoria()],
  ['SQLite', () => crearAlmacenSqlite(baseSqliteEnMemoria())],
];

describe.each(implementaciones)('almacén del registro %s', (_nombre, crear) => {
  let almacen: AlmacenRegistro;
  beforeEach(async () => {
    almacen = await crear();
  });

  test('guarda y lee una sesión, y la actualiza por partes', async () => {
    await almacen.insertarSesion(sesion('s1'));
    await almacen.actualizarSesion('s1', { estado: 'pendiente', intentos: 2, ultimoError: 'x', proximoIntento: 123 });

    expect(await almacen.leerSesion('s1')).toEqual(
      sesion('s1', { estado: 'pendiente', intentos: 2, ultimoError: 'x', proximoIntento: 123 }),
    );
    expect(await almacen.leerSesion('no-existe')).toBeNull();
  });

  test('lista las sesiones por usuario y estado en orden de creación', async () => {
    await almacen.insertarSesion(sesion('s2'));
    await almacen.insertarSesion(sesion('s1', { estado: 'pendiente' }));
    await almacen.insertarSesion(sesion('s3', { usuario: B, estado: 'pendiente' }));

    expect((await almacen.listarSesiones()).map((s) => s.uuid)).toEqual(['s2', 's1', 's3']);
    expect((await almacen.listarSesiones({ usuario: A })).map((s) => s.uuid)).toEqual(['s2', 's1']);
    expect((await almacen.listarSesiones({ estados: ['pendiente'] })).map((s) => s.uuid)).toEqual(['s1', 's3']);
    expect((await almacen.listarSesiones({ usuario: A, estados: [] })).map((s) => s.uuid)).toEqual([]);
  });

  test('guarda las series con todos sus campos y las lista en orden de inserción', async () => {
    await almacen.insertarSesion(sesion('s1'));
    const minima = serie('z', 's1', { rir: null, rpe: null, dolor: null });
    const correccion = serie('a', 's1', { corrigeA: 'z', anulada: true, calentamiento: true });
    await almacen.insertarSerie(minima);
    await almacen.insertarSerie(correccion);

    expect(await almacen.listarSeries('s1')).toEqual([minima, correccion]);
    expect(await almacen.listarSeries('otra')).toEqual([]);
  });

  test('una carga siguiente por usuario y ejercicio: la nueva sustituye a la anterior', async () => {
    await almacen.insertarSesion(sesion('s1'));
    await almacen.insertarSesion(sesion('s2'));
    await almacen.guardarCarga({ usuario: A, ejercicio: 'press_banca', cargaKg: 80, planVersion: 'p', sesion: 's1' });
    await almacen.guardarCarga({ usuario: A, ejercicio: 'press_banca', cargaKg: 82.5, planVersion: 'p', sesion: 's2' });
    await almacen.guardarCarga({ usuario: A, ejercicio: 'remo', cargaKg: 60, planVersion: 'p', sesion: 's2' });

    expect(await almacen.listarCargas(A)).toEqual([
      { usuario: A, ejercicio: 'press_banca', cargaKg: 82.5, planVersion: 'p', sesion: 's2' },
      { usuario: A, ejercicio: 'remo', cargaKg: 60, planVersion: 'p', sesion: 's2' },
    ]);
    expect(await almacen.listarCargas(B)).toEqual([]);

    await almacen.borrarCarga(A, 'remo');
    expect((await almacen.listarCargas(A)).map((c) => c.ejercicio)).toEqual(['press_banca']);
  });

  test('borrar una sesión se lleva sus series y sus cargas, y deja lo demás', async () => {
    await almacen.insertarSesion(sesion('s1'));
    await almacen.insertarSesion(sesion('s2'));
    await almacen.insertarSerie(serie('x1', 's1'));
    await almacen.insertarSerie(serie('x2', 's2'));
    await almacen.guardarCarga({ usuario: A, ejercicio: 'press_banca', cargaKg: 80, planVersion: 'p', sesion: 's1' });
    await almacen.guardarCarga({ usuario: A, ejercicio: 'remo', cargaKg: 60, planVersion: 'p', sesion: 's2' });

    await almacen.borrarSesion('s1');

    expect(await almacen.leerSesion('s1')).toBeNull();
    expect(await almacen.listarSeries('s1')).toEqual([]);
    expect(await almacen.listarSeries('s2')).toHaveLength(1);
    expect((await almacen.listarCargas(A)).map((c) => c.ejercicio)).toEqual(['remo']);
  });

  test('un plan guardado por usuario: el nuevo sustituye al anterior y se puede borrar', async () => {
    await almacen.guardarPlan({ usuario: A, planUuid: 'p1', json: '{"v":1}', guardadoEn: 1 });
    await almacen.guardarPlan({ usuario: A, planUuid: 'p2', json: '{"v":2}', guardadoEn: 2 });
    await almacen.guardarPlan({ usuario: B, planUuid: 'pb', json: '{}', guardadoEn: 3 });

    expect(await almacen.leerPlan(A)).toEqual({ usuario: A, planUuid: 'p2', json: '{"v":2}', guardadoEn: 2 });
    await almacen.borrarPlan(A);
    expect(await almacen.leerPlan(A)).toBeNull();
    expect(await almacen.leerPlan(B)).not.toBeNull();
  });

  test('lo que devuelve es una copia: tocarla no cambia lo guardado', async () => {
    await almacen.insertarSesion(sesion('s1'));
    const leida = (await almacen.leerSesion('s1'))!;
    leida.estado = 'confirmada';

    expect((await almacen.leerSesion('s1'))!.estado).toBe('en_curso');
  });
});

describe('migraciones', () => {
  test('aplican solo las que faltan y dejan user_version en la última', async () => {
    const base = baseSqliteEnMemoria();
    await migrar(base, ['CREATE TABLE a (x INTEGER)']);
    await migrar(base, ['CREATE TABLE a (x INTEGER)', 'CREATE TABLE b (y INTEGER)']);

    expect(await base.getFirstAsync('PRAGMA user_version', [])).toEqual({ user_version: 2 });
    expect(await base.getAllAsync("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name", [])).toEqual([
      { name: 'a' },
      { name: 'b' },
    ]);
  });

  test('una migración que falla no deja nada a medias ni sube la versión', async () => {
    const base = baseSqliteEnMemoria();

    await expect(migrar(base, ['CREATE TABLE a (x INTEGER); ESTO NO ES SQL'])).rejects.toThrow();

    expect(await base.getFirstAsync('PRAGMA user_version', [])).toEqual({ user_version: 0 });
    expect(await base.getAllAsync("SELECT name FROM sqlite_master WHERE type = 'table'", [])).toEqual([]);
  });

  test('de la 1 a la 2 conserva lo que había y añade el plan guardado', async () => {
    const base = baseSqliteEnMemoria();
    await migrar(base, MIGRACIONES.slice(0, 1));
    const v1 = await crearAlmacenSqliteSinMigrar(base);
    await v1.insertarSesion(sesion('s1'));
    await v1.insertarSerie(serie('x1', 's1'));

    const almacen = await crearAlmacenSqlite(base);

    expect(await base.getFirstAsync('PRAGMA user_version', [])).toEqual({ user_version: 2 });
    expect(await almacen.leerSesion('s1')).toEqual(sesion('s1'));
    expect(await almacen.listarSeries('s1')).toEqual([serie('x1', 's1')]);
    await almacen.guardarPlan({ usuario: A, planUuid: 'p1', json: '{}', guardadoEn: 1 });
    expect(await almacen.leerPlan(A)).not.toBeNull();
  });

  test('la base real arranca en la última versión', async () => {
    const base = baseSqliteEnMemoria();
    await crearAlmacenSqlite(base);

    expect(await base.getFirstAsync('PRAGMA user_version', [])).toEqual({ user_version: MIGRACIONES.length });
  });
});
