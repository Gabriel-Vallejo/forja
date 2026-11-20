import * as Sharing from 'expo-sharing';

// Jest con jest-expo resuelve como iOS: este import carga archivo.native.ts.
import { guardarJson, SIN_COMPARTIR } from './archivo';

const mockArchivos: { uri: string; exists: boolean; create: jest.Mock; write: jest.Mock; delete: jest.Mock }[] = [];
const mockCarpetas: { uri: string; exists: boolean; create: jest.Mock; delete: jest.Mock }[] = [];
let mockCarpetaExiste = false;

jest.mock('expo-file-system', () => ({
  Paths: { cache: { uri: 'file:///cache/' } },
  Directory: jest.fn().mockImplementation((padre: { uri: string }, nombre: string) => {
    const carpeta = { uri: `${padre.uri}${nombre}/`, exists: mockCarpetaExiste, create: jest.fn(), delete: jest.fn() };
    mockCarpetas.push(carpeta);
    return carpeta;
  }),
  File: jest.fn().mockImplementation((carpeta: { uri: string }, nombre: string) => {
    const archivo = {
      uri: carpeta.uri + nombre,
      exists: false,
      create: jest.fn(),
      write: jest.fn(async () => undefined),
      delete: jest.fn(),
    };
    mockArchivos.push(archivo);
    return archivo;
  }),
}));

jest.mock('expo-sharing', () => ({
  isAvailableAsync: jest.fn(async () => true),
  shareAsync: jest.fn(async () => undefined),
}));

beforeEach(() => {
  jest.clearAllMocks();
  mockArchivos.length = 0;
  mockCarpetas.length = 0;
  mockCarpetaExiste = false;
});

describe('guardar el JSON en el móvil', () => {
  test('lo escribe en su carpeta de la caché, abre la hoja de compartir y lo deja para la app que lo lea después', async () => {
    await guardarJson('forja-mis-datos-2026-11-20.json', '{"formato":1}');

    const [archivo] = mockArchivos;
    expect(archivo?.uri).toBe('file:///cache/mis-datos/forja-mis-datos-2026-11-20.json');
    expect(archivo?.write).toHaveBeenCalledWith('{"formato":1}');
    expect(Sharing.shareAsync).toHaveBeenCalledWith(archivo?.uri, expect.objectContaining({ mimeType: 'application/json' }));
    expect(mockCarpetas[0]?.delete).not.toHaveBeenCalled();
  });

  test('una exportación nueva vacía antes la carpeta: nunca queda más de una', async () => {
    mockCarpetaExiste = true;

    await guardarJson('b.json', '{}');

    const [carpeta] = mockCarpetas;
    expect(carpeta?.delete).toHaveBeenCalledTimes(1);
    expect(carpeta?.create).toHaveBeenCalledTimes(1);
    expect(carpeta!.delete.mock.invocationCallOrder[0]).toBeLessThan(carpeta!.create.mock.invocationCallOrder[0]!);
  });

  test('si compartir falla, se borra en el momento', async () => {
    jest.mocked(Sharing.shareAsync).mockRejectedValueOnce(new Error('cancelado'));

    await expect(guardarJson('a.json', '{}')).rejects.toThrow('cancelado');

    expect(mockCarpetas[0]?.delete).toHaveBeenCalledTimes(1);
  });

  test('sin hoja de compartir no escribe nada y lo dice', async () => {
    jest.mocked(Sharing.isAvailableAsync).mockResolvedValueOnce(false);

    await expect(guardarJson('a.json', '{}')).rejects.toThrow(SIN_COMPARTIR);

    expect(mockArchivos).toHaveLength(0);
  });
});
