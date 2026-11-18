import type { AlmacenRegistro, CambiosSesion, CargaSiguiente, PlanGuardado, SerieLocal, SesionLocal } from './almacen';
import { type BaseSqlite, migrar } from './migraciones';

interface FilaSesion {
  uuid: string;
  usuario: string;
  dia_plan: number | null;
  plan_version: string | null;
  realizada_en: string;
  cerrada_en: string | null;
  estado: SesionLocal['estado'];
  intentos: number;
  proximo_intento: number | null;
  ultimo_error: string | null;
  confirmada_en: number | null;
}

interface FilaSerie {
  uuid: string;
  sesion: string;
  ejercicio: string;
  orden: number;
  peso_kg: number;
  repeticiones: number;
  rir: number | null;
  rpe: number | null;
  dolor: number | null;
  calentamiento: number;
  corrige_a: string | null;
  anulada: number;
  realizada_en: string;
}

interface FilaCarga {
  usuario: string;
  ejercicio: string;
  carga_kg: number;
  plan_version: string;
  sesion: string;
}

// Campo del modelo → columna. Solo estas columnas se pueden actualizar (y nombrar en el SQL).
const COLUMNAS_SESION: Record<keyof CambiosSesion, string> = {
  diaPlan: 'dia_plan',
  planVersion: 'plan_version',
  realizadaEn: 'realizada_en',
  cerradaEn: 'cerrada_en',
  estado: 'estado',
  intentos: 'intentos',
  proximoIntento: 'proximo_intento',
  ultimoError: 'ultimo_error',
  confirmadaEn: 'confirmada_en',
};

const aSesion = (f: FilaSesion): SesionLocal => ({
  uuid: f.uuid,
  usuario: f.usuario,
  diaPlan: f.dia_plan,
  planVersion: f.plan_version,
  realizadaEn: f.realizada_en,
  cerradaEn: f.cerrada_en,
  estado: f.estado,
  intentos: f.intentos,
  proximoIntento: f.proximo_intento,
  ultimoError: f.ultimo_error,
  confirmadaEn: f.confirmada_en,
});

const aSerie = (f: FilaSerie): SerieLocal => ({
  uuid: f.uuid,
  sesion: f.sesion,
  ejercicio: f.ejercicio,
  orden: f.orden,
  pesoKg: f.peso_kg,
  repeticiones: f.repeticiones,
  rir: f.rir,
  rpe: f.rpe,
  dolor: f.dolor,
  calentamiento: f.calentamiento === 1,
  corrigeA: f.corrige_a,
  anulada: f.anulada === 1,
  realizadaEn: f.realizada_en,
});

const aCarga = (f: FilaCarga): CargaSiguiente => ({
  usuario: f.usuario,
  ejercicio: f.ejercicio,
  cargaKg: f.carga_kg,
  planVersion: f.plan_version,
  sesion: f.sesion,
});

interface FilaPlan {
  usuario: string;
  plan_uuid: string;
  json: string;
  guardado_en: number;
}

const aPlan = (f: FilaPlan): PlanGuardado => ({ usuario: f.usuario, planUuid: f.plan_uuid, json: f.json, guardadoEn: f.guardado_en });

/** Almacén sobre SQLite. Migra la base antes de devolverlo. */
export async function crearAlmacenSqlite(base: BaseSqlite): Promise<AlmacenRegistro> {
  await migrar(base);
  return {
    async insertarSesion(s) {
      await base.runAsync(
        `INSERT INTO sesion (uuid, usuario, dia_plan, plan_version, realizada_en, cerrada_en, estado, intentos,
          proximo_intento, ultimo_error, confirmada_en) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [s.uuid, s.usuario, s.diaPlan, s.planVersion, s.realizadaEn, s.cerradaEn, s.estado, s.intentos,
          s.proximoIntento, s.ultimoError, s.confirmadaEn],
      );
    },
    async actualizarSesion(uuid, cambios) {
      const campos = (Object.keys(cambios) as (keyof CambiosSesion)[]).filter((c) => c in COLUMNAS_SESION);
      if (campos.length === 0) return;
      const asignaciones = campos.map((c) => `${COLUMNAS_SESION[c]} = ?`).join(', ');
      await base.runAsync(`UPDATE sesion SET ${asignaciones} WHERE uuid = ?`, [
        ...campos.map((c) => cambios[c] ?? null),
        uuid,
      ]);
    },
    async borrarSesion(uuid) {
      // Las series y las cargas caen en cascada (foreign_keys = ON en migrar).
      await base.runAsync('DELETE FROM sesion WHERE uuid = ?', [uuid]);
    },
    async leerSesion(uuid) {
      const fila = await base.getFirstAsync<FilaSesion>('SELECT * FROM sesion WHERE uuid = ?', [uuid]);
      return fila ? aSesion(fila) : null;
    },
    async listarSesiones({ usuario, estados } = {}) {
      if (estados?.length === 0) return [];
      const condiciones: string[] = [];
      const params: string[] = [];
      if (usuario !== undefined) {
        condiciones.push('usuario = ?');
        params.push(usuario);
      }
      if (estados) {
        condiciones.push(`estado IN (${estados.map(() => '?').join(', ')})`);
        params.push(...estados);
      }
      const donde = condiciones.length ? `WHERE ${condiciones.join(' AND ')}` : '';
      const filas = await base.getAllAsync<FilaSesion>(`SELECT * FROM sesion ${donde} ORDER BY rowid`, params);
      return filas.map(aSesion);
    },
    async insertarSerie(s) {
      await base.runAsync(
        `INSERT INTO serie (uuid, sesion, ejercicio, orden, peso_kg, repeticiones, rir, rpe, dolor, calentamiento,
          corrige_a, anulada, realizada_en) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [s.uuid, s.sesion, s.ejercicio, s.orden, s.pesoKg, s.repeticiones, s.rir, s.rpe, s.dolor,
          s.calentamiento ? 1 : 0, s.corrigeA, s.anulada ? 1 : 0, s.realizadaEn],
      );
    },
    async listarSeries(sesion) {
      const filas = await base.getAllAsync<FilaSerie>('SELECT * FROM serie WHERE sesion = ? ORDER BY rowid', [sesion]);
      return filas.map(aSerie);
    },
    async guardarCarga(c) {
      await base.runAsync(
        `INSERT OR REPLACE INTO carga_siguiente (usuario, ejercicio, carga_kg, plan_version, sesion)
          VALUES (?, ?, ?, ?, ?)`,
        [c.usuario, c.ejercicio, c.cargaKg, c.planVersion, c.sesion],
      );
    },
    async listarCargas(usuario) {
      const filas = await base.getAllAsync<FilaCarga>(
        'SELECT * FROM carga_siguiente WHERE usuario = ? ORDER BY ejercicio',
        [usuario],
      );
      return filas.map(aCarga);
    },
    async borrarCarga(usuario, ejercicio) {
      await base.runAsync('DELETE FROM carga_siguiente WHERE usuario = ? AND ejercicio = ?', [usuario, ejercicio]);
    },
    async guardarPlan(p) {
      await base.runAsync('INSERT OR REPLACE INTO plan_guardado (usuario, plan_uuid, json, guardado_en) VALUES (?, ?, ?, ?)', [
        p.usuario,
        p.planUuid,
        p.json,
        p.guardadoEn,
      ]);
    },
    async leerPlan(usuario) {
      const fila = await base.getFirstAsync<FilaPlan>('SELECT * FROM plan_guardado WHERE usuario = ?', [usuario]);
      return fila ? aPlan(fila) : null;
    },
    async borrarPlan(usuario) {
      await base.runAsync('DELETE FROM plan_guardado WHERE usuario = ?', [usuario]);
    },
  };
}
