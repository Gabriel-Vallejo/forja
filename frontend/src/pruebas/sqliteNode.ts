// SQLite real para los tests: `node:sqlite` envuelto con la forma de expo-sqlite que usa el almacén.
import { DatabaseSync } from 'node:sqlite';

import type { BaseSqlite } from '../registro/migraciones';

export function baseSqliteEnMemoria(): BaseSqlite {
  const base = new DatabaseSync(':memory:');
  const sinPrototipo = <T>(fila: unknown) => (fila ? ({ ...(fila as object) } as T) : null);
  return {
    async execAsync(sql) {
      base.exec(sql);
    },
    async runAsync(sql, params) {
      return base.prepare(sql).run(...params);
    },
    async getAllAsync<T>(sql: string, params: (string | number | null)[]) {
      return base.prepare(sql).all(...params).map((f) => sinPrototipo<T>(f)!);
    },
    async getFirstAsync<T>(sql: string, params: (string | number | null)[]) {
      return sinPrototipo<T>(base.prepare(sql).get(...params));
    },
    async withTransactionAsync(tarea) {
      base.exec('BEGIN');
      try {
        await tarea();
        base.exec('COMMIT');
      } catch (e) {
        base.exec('ROLLBACK');
        throw e;
      }
    },
  };
}
