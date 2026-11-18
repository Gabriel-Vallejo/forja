// Migraciones de la base local, versionadas con `PRAGMA user_version`. Solo se añaden al final: nunca se edita una
// que ya salió en una versión de la app.

/** Lo que el almacén SQLite necesita de `SQLiteDatabase` de expo-sqlite (y lo que imitan los tests). */
export interface BaseSqlite {
  execAsync(sql: string): Promise<void>;
  runAsync(sql: string, params: (string | number | null)[]): Promise<unknown>;
  getAllAsync<T>(sql: string, params: (string | number | null)[]): Promise<T[]>;
  getFirstAsync<T>(sql: string, params: (string | number | null)[]): Promise<T | null>;
  withTransactionAsync(tarea: () => Promise<void>): Promise<void>;
}

export const MIGRACIONES: readonly string[] = [
  // 1. Sesiones, series que solo se añaden y la carga siguiente calculada sin red (ADR 0008).
  `
  CREATE TABLE sesion (
    uuid TEXT PRIMARY KEY NOT NULL,
    usuario TEXT NOT NULL,
    dia_plan INTEGER,
    plan_version TEXT,
    realizada_en TEXT NOT NULL,
    cerrada_en TEXT,
    estado TEXT NOT NULL CHECK (estado IN ('en_curso', 'pendiente', 'enviando', 'confirmada', 'rechazada')),
    intentos INTEGER NOT NULL DEFAULT 0,
    proximo_intento INTEGER,
    ultimo_error TEXT,
    confirmada_en INTEGER
  );
  CREATE INDEX sesion_usuario_estado ON sesion (usuario, estado);
  CREATE TABLE serie (
    uuid TEXT PRIMARY KEY NOT NULL,
    sesion TEXT NOT NULL REFERENCES sesion (uuid) ON DELETE CASCADE,
    ejercicio TEXT NOT NULL,
    orden INTEGER NOT NULL,
    peso_kg REAL NOT NULL,
    repeticiones INTEGER NOT NULL,
    rir INTEGER,
    rpe REAL,
    dolor INTEGER,
    calentamiento INTEGER NOT NULL DEFAULT 0,
    corrige_a TEXT,
    anulada INTEGER NOT NULL DEFAULT 0,
    realizada_en TEXT NOT NULL
  );
  CREATE INDEX serie_sesion ON serie (sesion);
  CREATE TABLE carga_siguiente (
    usuario TEXT NOT NULL,
    ejercicio TEXT NOT NULL,
    carga_kg REAL NOT NULL,
    plan_version TEXT NOT NULL,
    sesion TEXT NOT NULL REFERENCES sesion (uuid) ON DELETE CASCADE,
    PRIMARY KEY (usuario, ejercicio)
  );
  `,
  // 2. Último plan bueno de cada usuario, para entrenar sin red (la sesión no depende de GET /planes/actual).
  `
  CREATE TABLE plan_guardado (
    usuario TEXT PRIMARY KEY NOT NULL,
    plan_uuid TEXT NOT NULL,
    json TEXT NOT NULL,
    guardado_en INTEGER NOT NULL
  );
  `,
];

/** Aplica las migraciones que falten, cada una en su transacción junto con su número de versión. */
export async function migrar(base: BaseSqlite, migraciones: readonly string[] = MIGRACIONES): Promise<void> {
  await base.execAsync('PRAGMA journal_mode = WAL; PRAGMA foreign_keys = ON;');
  const fila = await base.getFirstAsync<{ user_version: number }>('PRAGMA user_version', []);
  const actual = fila?.user_version ?? 0;
  for (let version = actual + 1; version <= migraciones.length; version++) {
    await base.withTransactionAsync(async () => {
      await base.execAsync(migraciones[version - 1]!);
      // PRAGMA no admite parámetros; `version` es un entero nuestro.
      await base.execAsync(`PRAGMA user_version = ${version}`);
    });
  }
}
