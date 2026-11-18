import { almacenEnMemoria, datosSesion, respuesta } from '../pruebas/dobles';
import { crearCliente } from './cliente';
import { crearApiForja } from './forja';

const URL_BASE = 'https://api.forja.test';
const DISPOSITIVO = { nombre: 'Pixel de prueba', plataforma: 'movil' as const };

function montar(fetchFalso: jest.Mock) {
  const almacen = almacenEnMemoria(datosSesion(1));
  return crearApiForja(crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso }), almacen, DISPOSITIVO);
}

function llamada(fetchFalso: jest.Mock, n = 0): [string, RequestInit] {
  return fetchFalso.mock.calls[n] as unknown as [string, RequestInit];
}

describe('API del perfil manual', () => {
  test('los datos sin definir llegan a undefined', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { sexo: 'no_indica', experiencia: null }));

    expect(await montar(fetchFalso).perfilDatos()).toEqual({ sexo: 'no_indica', experiencia: undefined, objetivo: undefined });
    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/perfil/datos`);
  });

  test('guardar los datos hace PUT con nulos explícitos', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { sexo: 'mujer', objetivo: 'fuerza' }));

    const guardados = await montar(fetchFalso).guardarPerfilDatos({ sexo: 'mujer', experiencia: null, objetivo: 'fuerza' });

    expect(guardados.objetivo).toBe('fuerza');
    expect(llamada(fetchFalso)[1].method).toBe('PUT');
    expect(JSON.parse(llamada(fetchFalso)[1].body as string)).toEqual({ sexo: 'mujer', experiencia: null, objetivo: 'fuerza' });
  });

  test('rechaza un sexo que el backend no define', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { sexo: 'otro' }));

    await expect(montar(fetchFalso).perfilDatos()).rejects.toThrow();
  });

  test('las medidas llegan con la estimación de grasa y su margen', async () => {
    const fetchFalso = jest.fn(async () =>
      respuesta(200, { alturaCm: 180, pesoKg: 78.5, caderaCm: null, grasaEstimada: { porcentaje: 16.1, margen: 4 } }),
    );

    expect(await montar(fetchFalso).medidas()).toEqual({
      alturaCm: 180,
      pesoKg: 78.5,
      cuelloCm: undefined,
      cinturaCm: undefined,
      caderaCm: undefined,
      grasaEstimada: { porcentaje: 16.1, margen: 4 },
    });
    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/perfil/medidas`);
  });

  test('guardar las medidas hace PUT con las cinco, nulas incluidas', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { alturaCm: 180 }));
    const cuerpo = { alturaCm: 180, pesoKg: null, cuelloCm: null, cinturaCm: null, caderaCm: null };

    await montar(fetchFalso).guardarMedidas(cuerpo);

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/perfil/medidas`);
    expect(llamada(fetchFalso)[1].method).toBe('PUT');
    expect(JSON.parse(llamada(fetchFalso)[1].body as string)).toEqual(cuerpo);
  });
});
