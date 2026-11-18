import type { AlmacenRegistro } from './almacen';
import { crearAlmacenSqlite } from './almacenSqlite';

const NOMBRE_BASE = 'forja-registro.db';

/**
 * Abre la base local. `expo-sqlite` se carga aquí y no al arrancar: Expo Go no trae su módulo nativo y un `import`
 * estático tumbaría la app entera. Si falta, esto rechaza y el proveedor de sincronización se queda sin outbox.
 */
export async function abrirAlmacenLocal(): Promise<AlmacenRegistro | null> {
  // `require` diferido a propósito: es lo que permite que el módulo falte sin romper el arranque (y que Jest lo pruebe).
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const { openDatabaseAsync } = require('expo-sqlite') as typeof import('expo-sqlite');
  return crearAlmacenSqlite(await openDatabaseAsync(NOMBRE_BASE));
}
