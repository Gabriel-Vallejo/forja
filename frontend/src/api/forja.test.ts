import { almacenEnMemoria, DISPOSITIVO_ID, datosSesion, materialesDePrueba, perfilDePrueba, respuesta } from '../pruebas/dobles';
import { crearCliente } from './cliente';
import { crearApiForja } from './forja';

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

  test('generar sin ajustes lee los del perfil y los manda al generador', async () => {
    const perfil = perfilDePrueba({ diasPorSemana: 5, minutosPorSesion: 45, material: ['barra', 'banda'], porDefecto: false });
    const fetchFalso = jest.fn(async (url: string) =>
      url.endsWith('/perfil/entreno') ? respuesta(200, perfil) : respuesta(200, { uuid: '0193a3c4-7b2e-7c1d-9f00-000000000001' }),
    );
    const { api } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await api.generarPlan();

    expect(llamada(fetchFalso, 0)[0]).toBe(`${URL_BASE}/api/v1/perfil/entreno`);
    expect(llamada(fetchFalso, 1)[0]).toBe(`${URL_BASE}/api/v1/rutinas/generar`);
    expect(cuerpoEnviado(fetchFalso, 1)).toEqual({
      diasPorSemana: 5,
      minutosPorSesion: 45,
      material: ['barra', 'banda'],
      exclusiones: [],
      prioritarios: [],
    });
  });

  test('generar con ajustes explícitos no pide el perfil', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { uuid: '0193a3c4-7b2e-7c1d-9f00-000000000001' }));
    const { api } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await api.generarPlan({ diasPorSemana: 2, minutosPorSesion: 30, material: ['polea'] });

    expect(fetchFalso).toHaveBeenCalledTimes(1);
    expect(cuerpoEnviado(fetchFalso)).toMatchObject({ diasPorSemana: 2, minutosPorSesion: 30, material: ['polea'] });
  });

  test('si el perfil no carga, generar falla sin inventar ajustes', async () => {
    const fetchFalso = jest.fn(async () => respuesta(500));
    const { api } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await expect(api.generarPlan()).rejects.toMatchObject({ estado: 500 });

    expect(fetchFalso).toHaveBeenCalledTimes(1);
  });

  test('el perfil de entreno se lee de /perfil/entreno y se valida', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, perfilDePrueba()));
    const { api } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await expect(api.perfilEntreno()).resolves.toEqual(perfilDePrueba());

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/perfil/entreno`);
  });

  test('un perfil con la forma equivocada se rechaza en el borde', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { diasPorSemana: '3' }));
    const { api } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await expect(api.perfilEntreno()).rejects.toBeDefined();
  });

  test('guardar el perfil hace PUT con los ajustes y devuelve lo guardado', async () => {
    const ajustes = { diasPorSemana: 4, minutosPorSesion: 75, material: ['mancuerna'] };
    const fetchFalso = jest.fn(async () => respuesta(200, { ...ajustes, porDefecto: false }));
    const { api } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await expect(api.guardarPerfilEntreno(ajustes)).resolves.toEqual({ ...ajustes, porDefecto: false });

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/perfil/entreno`);
    expect(llamada(fetchFalso)[1].method).toBe('PUT');
    expect(cuerpoEnviado(fetchFalso)).toEqual(ajustes);
  });

  test('los materiales salen de /catalogo/materiales', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, materialesDePrueba()));
    const { api } = montar(fetchFalso, almacenEnMemoria(datosSesion(1)));

    await expect(api.materiales()).resolves.toEqual(materialesDePrueba());

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/catalogo/materiales`);
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
