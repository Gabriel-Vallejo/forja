import { ErrorApi } from './errores';
import { almacenEnMemoria, consentimiento, datosSesion, dispositivoCuenta, respuesta } from '../pruebas/dobles';
import { crearCliente } from './cliente';
import { crearApiForja } from './forja';

const URL_BASE = 'https://api.forja.test';
const DISPOSITIVO = { nombre: 'Pixel de prueba', plataforma: 'movil' as const };

function montar(fetchFalso: jest.Mock) {
  const almacen = almacenEnMemoria(datosSesion(1));
  return { almacen, api: crearApiForja(crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso }), almacen, DISPOSITIVO) };
}

const llamada = (fetchFalso: jest.Mock, n = 0) => fetchFalso.mock.calls[n] as unknown as [string, RequestInit];

describe('API de privacidad', () => {
  test('lista los consentimientos con su fecha', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, [consentimiento('limitaciones')]));

    const lista = await montar(fetchFalso).api.consentimientos();

    expect(lista).toEqual([consentimiento('limitaciones')]);
    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/consentimientos`);
  });

  test('una fecha que no es ISO se rechaza como respuesta inesperada', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, [consentimiento('limitaciones', { dadoEn: 'ayer' })]));

    await expect(montar(fetchFalso).api.consentimientos()).rejects.toBeInstanceOf(ErrorApi);
  });

  test('retirar un consentimiento es un DELETE a su tipo', async () => {
    const fetchFalso = jest.fn(async () => respuesta(204));

    await montar(fetchFalso).api.retirarConsentimiento('suplementos');

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/consentimientos/suplementos`);
    expect(llamada(fetchFalso)[1].method).toBe('DELETE');
  });

  test('lista los dispositivos y pasa los nulos a undefined', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, [dispositivoCuenta(), { ...dispositivoCuenta({ id: '0193a3c4-7b2e-7c1d-9f00-5a1b2c3d4e60', actual: false }), ultimaActividad: null }]));

    const lista = await montar(fetchFalso).api.dispositivos();

    expect(lista[0]).toEqual(dispositivoCuenta());
    expect(lista[1]?.ultimaActividad).toBeUndefined();
    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/dispositivos`);
  });

  test('cerrar un dispositivo es un DELETE a su id', async () => {
    const fetchFalso = jest.fn(async () => respuesta(204));

    await montar(fetchFalso).api.cerrarDispositivo('0193a3c4-7b2e-7c1d-9f00-5a1b2c3d4e60');

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/dispositivos/0193a3c4-7b2e-7c1d-9f00-5a1b2c3d4e60`);
    expect(llamada(fetchFalso)[1].method).toBe('DELETE');
  });

  test('borrar la cuenta manda la contraseña y, si va bien, olvida la sesión local', async () => {
    const fetchFalso = jest.fn(async () => respuesta(204));
    const { api, almacen } = montar(fetchFalso);

    await api.borrarCuenta('doce-caracteres');

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/cuenta`);
    expect(llamada(fetchFalso)[1].method).toBe('DELETE');
    expect(JSON.parse(llamada(fetchFalso)[1].body as string)).toEqual({ contrasena: 'doce-caracteres' });
    expect(await api.haySesionGuardada()).toBe(false);
    expect(almacen.estado.tokenAcceso).toBeUndefined();
  });

  test('con la contraseña mal, el mensaje del backend sale tal cual y la sesión sigue', async () => {
    const fetchFalso = jest.fn(async () => respuesta(403, { detail: 'La contraseña no es correcta.' }));
    const { api } = montar(fetchFalso);

    await expect(api.borrarCuenta('mala')).rejects.toMatchObject({ estado: 403, message: 'La contraseña no es correcta.' });

    expect(await api.haySesionGuardada()).toBe(true);
  });
});
