// Lo poco de `node:sqlite` que usan los tests (sin meter los tipos de Node en toda la app).
declare module 'node:sqlite' {
  type Valor = string | number | null;
  interface Sentencia {
    run(...params: Valor[]): unknown;
    all(...params: Valor[]): unknown[];
    get(...params: Valor[]): unknown;
  }
  export class DatabaseSync {
    constructor(ruta: string);
    exec(sql: string): void;
    prepare(sql: string): Sentencia;
  }
}
