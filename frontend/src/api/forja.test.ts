import { almacenEnMemoria, DISPOSITIVO_ID, datosSesion, respuesta } from '../pruebas/dobles';
import { crearCliente } from './cliente';
import { crearApiForja, RUTINA_POR_DEFECTO } from './forja';

const URL_BASE = 'https://api.forja.test';
const DISPOSITIVO = { nombre: 'Pixel de prueba', plataforma: 'movil' as const };

function montar(fetchFalso: jest.Mock, almacen = almacenEnMemoria()) {
  return { almacen, api: crearApiForja(crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso }), almacen, DISPOSITIVO) };
}

function llamada(fetchFalso: jest.Mock, n = 0): [string, RequestInit] {
  return fetchFalso.mock.calls[n] as unknown as [string, RequestInit];
}

function cuerpoEnviado(fetchFalso: jest.Mock, n = 0): unknown {
  return JSON.parse(llamada(fetchFalso, n)[1].body as string);
}

describe('API de Forja', () => {
  test('al entrar manda nombre, plataforma y el sid guardado, y guarda la sesión', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, datosSesion(1)));
    const { api, almacen } = montar(fetchFalso, almacenEnMemoria({ dispositivoId: DISPOSITIVO_ID }));

    await api.entrar({ correo: 'ana@forja.test', contrasena: 'doce-caracteres' });

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/auth/login`);
    expect(cuerpoEnviado(fetchFalso)).toEqual({
      correo: 'ana@forja.test',
      contrasena: 'doce-caracteres',
      dispositivoId: DISPOSITIVO_ID,
      nombreDispositivo: 'Pixel de prueba',
      plataforma: 'movil',
    });
    expect(almacen.leerAcceso()).toBe('acceso-1');
    await expect(api.haySesionGuardada()).resolves.toBe(true);
  });

  test('el primer inicio de sesión manda dispositivoId nulo', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, datosSesion(1)));
    const { api } = montar(fetchFalso);

    await api.entrar({ correo: 'ana@forja.test', contrasena: 'doce-caracteres' });

    expect(cuerpoEnviado(fetchFalso)).toMatchObject({ dispositivoId: null });
  });

  test('sin plan, /planes/actual da 404 y la portada recibe null', async () => {
    const fetchFalso = jest.fn(async () => respuesta(404, { status: 404, detail: 'Todavía no hay plan.' }));
    const { api } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await expect(api.planActual()).resolves.toBeNull();
  });

  test('otros errores del plan se propagan', async () => {
    const fetchFalso = jest.fn(async () => respuesta(500));
    const { api } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await expect(api.planActual()).rejects.toMatchObject({ estado: 500 });
  });

  test('generar el plan manda los valores por defecto', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { uuid: '0193a3c4-7b2e-7c1d-9f00-000000000001' }));
    const { api } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await api.generarPlan();

    expect(cuerpoEnviado(fetchFalso)).toEqual(RUTINA_POR_DEFECTO);
  });

  test('cerrar sesión borra la sesión local aunque falle la red', async () => {
    const fetchFalso = jest.fn(async () => {
      throw new TypeError('Network request failed');
    });
    const { api, almacen } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await expect(api.cerrarSesion()).rejects.toMatchObject({ estado: 0 });

    expect(almacen.leerAcceso()).toBeNull();
    await expect(api.haySesionGuardada()).resolves.toBe(false);
  });

  test('registrar devuelve el mensaje del backend', async () => {
    const fetchFalso = jest.fn(async () => respuesta(202, { mensaje: 'Revisa tu correo.' }));
    const { api } = montar(fetchFalso);

    await expect(
      api.registrar({ correo: 'ana@forja.test', contrasena: 'doce-caracteres', fechaNacimiento: '2000-01-31' }),
    ).resolves.toBe('Revisa tu correo.');
  });
});
