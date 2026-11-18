import { almacenTokens, MENSAJE_NO_DISPONIBLE } from './almacenTokens.web';

describe('almacén de tokens web', () => {
  test('no guarda nada todavía: cada operación dice que no está disponible', async () => {
    expect(() => almacenTokens.leerAcceso()).toThrow(MENSAJE_NO_DISPONIBLE);
    await expect(almacenTokens.leerRefresco()).rejects.toThrow(MENSAJE_NO_DISPONIBLE);
    await expect(almacenTokens.leerDispositivo()).rejects.toThrow(MENSAJE_NO_DISPONIBLE);
    await expect(almacenTokens.leerUsuario()).rejects.toThrow(MENSAJE_NO_DISPONIBLE);
    await expect(
      almacenTokens.guardar({ tokenAcceso: 'a', tokenRefresco: 'r', dispositivoId: 'd' }),
    ).rejects.toThrow(MENSAJE_NO_DISPONIBLE);
    await expect(almacenTokens.borrar()).rejects.toThrow(MENSAJE_NO_DISPONIBLE);
  });
});
