import * as SecureStore from 'expo-secure-store';

// Jest con jest-expo resuelve como iOS: este import carga almacenTokens.native.ts.
import { almacenTokens } from './almacenTokens';
import { tokenDe } from '../pruebas/dobles';

jest.mock('expo-secure-store', () => {
  const datos = new Map<string, string>();
  return {
    setItemAsync: jest.fn(async (clave: string, valor: string) => void datos.set(clave, valor)),
    getItemAsync: jest.fn(async (clave: string) => datos.get(clave) ?? null),
    deleteItemAsync: jest.fn(async (clave: string) => void datos.delete(clave)),
  };
});

const SESION = { tokenAcceso: 'acceso-1', tokenRefresco: 'refresco-1', dispositivoId: 'disp-1' };

describe('almacén de tokens nativo', () => {
  beforeEach(() => jest.clearAllMocks());

  test('guarda el refresco y el dispositivo en secure-store y el acceso solo en memoria', async () => {
    await almacenTokens.guardar(SESION);

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('forja.tokenRefresco', 'refresco-1');
    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('forja.dispositivoId', 'disp-1');
    expect(SecureStore.setItemAsync).not.toHaveBeenCalledWith(expect.anything(), 'acceso-1');
    expect(almacenTokens.leerAcceso()).toBe('acceso-1');
    await expect(almacenTokens.leerRefresco()).resolves.toBe('refresco-1');
  });

  test('al borrar, olvida acceso y refresco pero conserva el dispositivo para reutilizarlo', async () => {
    await almacenTokens.guardar(SESION);

    await almacenTokens.borrar();

    expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('forja.tokenRefresco');
    expect(almacenTokens.leerAcceso()).toBeNull();
    await expect(almacenTokens.leerRefresco()).resolves.toBeNull();
    await expect(almacenTokens.leerDispositivo()).resolves.toBe('disp-1');
  });

  test('recuerda el usuario del token (sub) y lo olvida al cerrar sesión', async () => {
    const usuario = '0193a3c4-7b2e-7c1d-9f00-00000000abcd';
    await almacenTokens.guardar({ ...SESION, tokenAcceso: tokenDe(usuario) });

    expect(SecureStore.setItemAsync).toHaveBeenCalledWith('forja.usuario', usuario);
    await expect(almacenTokens.leerUsuario()).resolves.toBe(usuario);

    await almacenTokens.borrar();

    expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('forja.usuario');
    await expect(almacenTokens.leerUsuario()).resolves.toBeNull();
  });
});
