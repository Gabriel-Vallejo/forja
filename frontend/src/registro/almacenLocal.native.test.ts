// Expo Go no trae el módulo nativo de SQLite: importar `expo-sqlite` lanza al evaluarse. Eso no puede tumbar la app
// entera al arrancar; el proveedor de sincronización ya trata un fallo al abrir y se queda sin outbox.
jest.mock('expo-sqlite', () => {
  throw new Error("Cannot find native module 'ExpoSQLite'");
});

describe('almacén local nativo sin el módulo de SQLite', () => {
  test('importar el archivo no lanza y abrir falla de forma controlada', async () => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { abrirAlmacenLocal } = require('./almacenLocal.native') as typeof import('./almacenLocal.native');

    await expect(abrirAlmacenLocal()).rejects.toThrow(/ExpoSQLite/);
  });
});
