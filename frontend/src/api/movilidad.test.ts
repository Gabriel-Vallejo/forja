import { almacenEnMemoria, datosSesion, respuesta } from '../pruebas/dobles';
import { crearCliente } from './cliente';
import { ErrorApi } from './errores';
import { crearApiForja } from './forja';

const URL_BASE = 'https://api.forja.test';
const DISPOSITIVO = { nombre: 'Pixel de prueba', plataforma: 'movil' as const };
const HECHO = '2026-11-18T10:00:00.123456Z';

function montar(fetchFalso: jest.Mock) {
  const almacen = almacenEnMemoria(datosSesion(1));
  return crearApiForja(crearCliente({ urlBase: URL_BASE, almacen, fetch: fetchFalso }), almacen, DISPOSITIVO);
}

function llamada(fetchFalso: jest.Mock, n = 0): [string, RequestInit] {
  return fetchFalso.mock.calls[n] as unknown as [string, RequestInit];
}

describe('API de movilidad', () => {
  test('el consentimiento de medidas de salud se mira, se da y se retira con su tipo', async () => {
    const lista = jest.fn(async () => respuesta(200, [{ tipo: 'medidas_salud', versionTexto: 'v1', dadoEn: HECHO }]));
    expect(await montar(lista).tieneConsentimientoMedidas()).toBe(true);
    const otra = jest.fn(async () => respuesta(200, [{ tipo: 'limitaciones', versionTexto: 'v1', dadoEn: HECHO }]));
    expect(await montar(otra).tieneConsentimientoMedidas()).toBe(false);

    const dar = jest.fn(async () => respuesta(200, { tipo: 'medidas_salud', versionTexto: 'v1', dadoEn: HECHO }));
    await montar(dar).darConsentimientoMedidas('v1');
    expect(JSON.parse(llamada(dar)[1].body as string)).toEqual({ tipo: 'medidas_salud', versionTexto: 'v1' });

    const retirar = jest.fn(async () => respuesta(204));
    await montar(retirar).retirarConsentimientoMedidas();
    expect(llamada(retirar)[0]).toBe(`${URL_BASE}/api/v1/consentimientos/medidas_salud`);
    expect(llamada(retirar)[1].method).toBe('DELETE');
  });

  test('los tests sin hacer llegan a undefined y los hechos, validados', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { tobillo: { tramo: 'limitado', hechoEn: HECHO }, cadera: null }));

    expect(await montar(fetchFalso).movilidad()).toEqual({ tobillo: { tramo: 'limitado', hechoEn: HECHO }, cadera: undefined, hombro: undefined });
    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/movilidad`);
  });

  test('un tramo desconocido del backend no se da por bueno', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { tobillo: { tramo: 'excelente', hechoEn: HECHO } }));

    await expect(montar(fetchFalso).movilidad()).rejects.toBeInstanceOf(ErrorApi);
  });

  test('guardar es un PUT con los tres tramos, nulos incluidos', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { hombro: { tramo: 'bueno', hechoEn: HECHO } }));

    await montar(fetchFalso).guardarMovilidad({ tobillo: null, cadera: null, hombro: 'bueno' });

    expect(llamada(fetchFalso)[1].method).toBe('PUT');
    expect(JSON.parse(llamada(fetchFalso)[1].body as string)).toEqual({ tobillo: null, cadera: null, hombro: 'bueno' });
  });
});
