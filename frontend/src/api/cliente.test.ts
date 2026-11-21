import { z } from 'zod';

import { almacenEnMemoria, datosSesion, respuesta, tokenDe, USUARIO_ID } from '../pruebas/dobles';
import { crearCliente, RUTA_REFRESCO } from './cliente';
import { ErrorApi, RESPUESTA_INESPERADA, SESION_CADUCADA, SIN_CONEXION } from './errores';

const URL_BASE = 'https://api.forja.test';
const RUTA = '/api/v1/algo';
const esquemaAlgo = z.object({ valor: z.number() });

type Llamada = [string, RequestInit];

function cabeceraAuth(llamada: Llamada): string | undefined {
  return (llamada[1].headers as Record<string, string>).Authorization;
}

function llamadasA(fetchFalso: jest.Mock, ruta: string): Llamada[] {
  return (fetchFalso.mock.calls as Llamada[]).filter(([url]) => url === URL_BASE + ruta);
}

/** Servidor falso: acepta solo el último token de acceso emitido; el refresco tarda un poco. */
function servidorConTokenCaducado() {
  let emitidos = 1;
  return jest.fn(async (url: string, init: RequestInit): Promise<Response> => {
    if (url === URL_BASE + RUTA_REFRESCO) {
      await new Promise((r) => setTimeout(r, 10));
      emitidos += 1;
      return respuesta(200, datosSesion(emitidos));
    }
    const auth = (init.headers as Record<string, string>).Authorization;
    return auth === `Bearer acceso-${emitidos}` && emitidos > 1
      ? respuesta(200, { valor: emitidos })
      : respuesta(401, { status: 401, detail: 'Hace falta iniciar sesión.' });
  });
}

describe('cliente HTTP', () => {
  test('tres peticiones con 401 a la vez provocan un solo POST /refresco y todas se reintentan', async () => {
    const almacen = almacenEnMemoria(datosSesion(1));
    const fetchFalso = servidorConTokenCaducado();
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });

    const resultados = await Promise.all([1, 2, 3].map(() => cliente.pedir({ ruta: RUTA, esquema: esquemaAlgo })));

    expect(llamadasA(fetchFalso, RUTA_REFRESCO)).toHaveLength(1);
    expect(resultados).toEqual([{ valor: 2 }, { valor: 2 }, { valor: 2 }]);
    expect(llamadasA(fetchFalso, RUTA).map(cabeceraAuth)).toEqual([
      'Bearer acceso-1',
      'Bearer acceso-1',
      'Bearer acceso-1',
      'Bearer acceso-2',
      'Bearer acceso-2',
      'Bearer acceso-2',
    ]);
    expect(almacen.estado.tokenRefresco).toBe('refresco-2');
  });

  test('un 401 que vuelve cuando otra petición ya renovó reintenta sin volver a refrescar', async () => {
    const almacen = almacenEnMemoria(datosSesion(1));
    const servidor = servidorConTokenCaducado();
    let conTokenViejo = 0;
    // La segunda petición con el token viejo tarda más que el refresco: su 401 llega con el token nuevo ya guardado.
    const fetchFalso = jest.fn(async (url: string, init: RequestInit) => {
      const auth = (init.headers as Record<string, string>).Authorization;
      if (url === URL_BASE + RUTA && auth === 'Bearer acceso-1' && ++conTokenViejo === 2) {
        await new Promise((r) => setTimeout(r, 50));
      }
      return servidor(url, init);
    });
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });

    const primera = cliente.pedir({ ruta: RUTA, esquema: esquemaAlgo });
    const rezagada = cliente.pedir({ ruta: RUTA, esquema: esquemaAlgo });

    await expect(Promise.all([primera, rezagada])).resolves.toEqual([{ valor: 2 }, { valor: 2 }]);
    expect(llamadasA(fetchFalso, RUTA_REFRESCO)).toHaveLength(1);
  });

  test('la validación Zod rechaza una respuesta mal formada', async () => {
    const almacen = almacenEnMemoria(datosSesion(1));
    const fetchFalso = jest.fn(async () => respuesta(200, { valor: 'no es un número' }));
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });

    const promesa = cliente.pedir({ ruta: RUTA, esquema: esquemaAlgo });

    await expect(promesa).rejects.toThrow(RESPUESTA_INESPERADA);
    await expect(promesa).rejects.toBeInstanceOf(ErrorApi);
  });

  test('un refresco mal formado tampoco se guarda', async () => {
    const almacen = almacenEnMemoria(datosSesion(1));
    const fetchFalso = jest.fn(async (url: string) =>
      url.endsWith(RUTA_REFRESCO) ? respuesta(200, { tokenAcceso: 'x' }) : respuesta(401),
    );
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });

    await expect(cliente.pedir({ ruta: RUTA })).rejects.toThrow(RESPUESTA_INESPERADA);
    expect(almacen.estado.tokenRefresco).toBe('refresco-1');
  });

  test('si el refresco da 401, borra la sesión local y avisa a quien escucha', async () => {
    const almacen = almacenEnMemoria(datosSesion(1));
    const fetchFalso = jest.fn(async () => respuesta(401, { status: 401, detail: 'Hace falta iniciar sesión.' }));
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });
    const oyente = jest.fn();
    cliente.alCaducar(oyente);

    await expect(cliente.pedir({ ruta: RUTA })).rejects.toMatchObject({ estado: 401 });

    expect(oyente).toHaveBeenCalledTimes(1);
    expect(almacen.estado.tokenRefresco).toBeUndefined();
    expect(almacen.estado.dispositivoId).toBe(datosSesion(1).dispositivoId);
  });

  test('si el refresco da 410 (cuenta borrada en otro dispositivo), avisa con el usuario antes de cerrar la sesión', async () => {
    const almacen = almacenEnMemoria({ ...datosSesion(1), tokenAcceso: tokenDe(USUARIO_ID) });
    const fetchFalso = jest.fn(async (url: string) =>
      url.endsWith(RUTA_REFRESCO)
        ? respuesta(410, { status: 410, detail: 'Tu cuenta se ha borrado.', codigo: 'cuenta_borrada' })
        : respuesta(401, { status: 401, detail: 'Hace falta iniciar sesión.' }),
    );
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });
    const orden: string[] = [];
    cliente.alBorrarseLaCuenta(async (usuario) => {
      orden.push(`borrar ${usuario} con sesión ${almacen.estado.tokenRefresco ?? 'ninguna'}`);
    });
    cliente.alCaducar(() => orden.push('caducar'));

    await expect(cliente.pedir({ ruta: RUTA })).rejects.toMatchObject({ estado: 410 });

    expect(orden).toEqual([`borrar ${USUARIO_ID} con sesión refresco-1`, 'caducar']);
    expect(almacen.estado.tokenRefresco).toBeUndefined();
  });

  test('con varias peticiones a la vez y un 410, se avisa y se cierra la sesión una sola vez', async () => {
    const almacen = almacenEnMemoria({ ...datosSesion(1), tokenAcceso: tokenDe(USUARIO_ID) });
    const fetchFalso = jest.fn(async (url: string) => respuesta(url.endsWith(RUTA_REFRESCO) ? 410 : 401));
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });
    const borrado = jest.fn(async () => undefined);
    const caducar = jest.fn();
    cliente.alBorrarseLaCuenta(borrado);
    cliente.alCaducar(caducar);

    const todas = [cliente.pedir({ ruta: RUTA }), cliente.pedir({ ruta: RUTA }), cliente.pedir({ ruta: RUTA })];
    await Promise.allSettled(todas);

    expect(borrado).toHaveBeenCalledTimes(1);
    expect(borrado).toHaveBeenCalledWith(USUARIO_ID);
    expect(caducar).toHaveBeenCalledTimes(1);
  });

  test('tras un 410 sin usuario legible se avisa igual (con null) para que la app lo diga', async () => {
    const almacen = almacenEnMemoria(datosSesion(1));
    const fetchFalso = jest.fn(async (url: string) => respuesta(url.endsWith(RUTA_REFRESCO) ? 410 : 401));
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });
    const borrado = jest.fn(async () => undefined);
    cliente.alBorrarseLaCuenta(borrado);

    await expect(cliente.pedir({ ruta: RUTA })).rejects.toMatchObject({ estado: 410 });

    expect(borrado).toHaveBeenCalledWith(null);
  });

  test('si borrar lo local falla tras un 410, la sesión se cierra igual', async () => {
    const almacen = almacenEnMemoria({ ...datosSesion(1), tokenAcceso: tokenDe(USUARIO_ID) });
    const fetchFalso = jest.fn(async (url: string) => respuesta(url.endsWith(RUTA_REFRESCO) ? 410 : 401));
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });
    const caducar = jest.fn();
    cliente.alBorrarseLaCuenta(async () => {
      throw new Error('base local rota');
    });
    cliente.alCaducar(caducar);

    await expect(cliente.pedir({ ruta: RUTA })).rejects.toMatchObject({ estado: 410 });

    expect(caducar).toHaveBeenCalledTimes(1);
    expect(almacen.estado.tokenRefresco).toBeUndefined();
  });

  test('sin refresco guardado, un 401 caduca la sesión sin llamar a /refresco', async () => {
    const almacen = almacenEnMemoria();
    const fetchFalso = jest.fn(async () => respuesta(401));
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });

    await expect(cliente.pedir({ ruta: RUTA })).rejects.toThrow(SESION_CADUCADA);
    expect(llamadasA(fetchFalso, RUTA_REFRESCO)).toHaveLength(0);
  });

  test('sin red durante el refresco, la sesión no se borra', async () => {
    const almacen = almacenEnMemoria(datosSesion(1));
    const fetchFalso = jest.fn(async (url: string) => {
      if (url.endsWith(RUTA_REFRESCO)) throw new TypeError('Network request failed');
      return respuesta(401);
    });
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });

    await expect(cliente.pedir({ ruta: RUTA })).rejects.toThrow(SIN_CONEXION);
    expect(almacen.estado.tokenRefresco).toBe('refresco-1');
  });

  test('un ProblemDetail se convierte en el mensaje para el usuario', async () => {
    const almacen = almacenEnMemoria();
    const fetchFalso = jest.fn(async () =>
      respuesta(400, {
        status: 400,
        detail: 'Hay datos que no son válidos.',
        errores: [{ campo: 'contrasena', mensaje: 'La contraseña debe tener entre 12 y 128 caracteres.' }],
      }),
    );
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });

    await expect(cliente.pedir({ metodo: 'POST', ruta: RUTA, cuerpo: {}, autenticada: false })).rejects.toThrow(
      'La contraseña debe tener entre 12 y 128 caracteres.',
    );
  });

  test('las rutas abiertas no llevan Authorization y el cuerpo va en JSON', async () => {
    const almacen = almacenEnMemoria(datosSesion(1));
    const fetchFalso = jest.fn(async () => respuesta(200, { valor: 1 }));
    const cliente = crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso });

    await cliente.pedir({ metodo: 'POST', ruta: RUTA, cuerpo: { a: 1 }, esquema: esquemaAlgo, autenticada: false });

    const [url, init] = fetchFalso.mock.calls[0] as unknown as Llamada;
    expect(url).toBe(URL_BASE + RUTA);
    expect(init.body).toBe('{"a":1}');
    expect(init.headers).toEqual({ Accept: 'application/json', 'Content-Type': 'application/json' });
  });
});
