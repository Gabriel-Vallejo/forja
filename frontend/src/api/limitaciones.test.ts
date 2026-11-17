import { almacenEnMemoria, datosSesion, efectoDePrueba, cuestionarioDePrueba, respuesta, restriccion } from '../pruebas/dobles';
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

const cuerpo = (fetchFalso: jest.Mock) => JSON.parse(llamada(fetchFalso)[1].body as string) as unknown;

describe('API de limitaciones', () => {
  test('el consentimiento existe si /consentimientos lista «limitaciones»', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, [{ tipo: 'suplementos', versionTexto: 'v1', dadoEn: '2026-11-17T10:00:00Z' }]));
    const api = montar(fetchFalso);
    expect(await api.tieneConsentimientoLimitaciones()).toBe(false);

    const con = jest.fn(async () => respuesta(200, [{ tipo: 'limitaciones', versionTexto: 'v1', dadoEn: '2026-11-17T10:00:00Z' }]));
    expect(await montar(con).tieneConsentimientoLimitaciones()).toBe(true);
    expect(llamada(con)[0]).toBe(`${URL_BASE}/api/v1/consentimientos`);
  });

  test('dar el consentimiento manda tipo y versión del texto', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { tipo: 'limitaciones', versionTexto: 'v1', dadoEn: '2026-11-17T10:00:00Z' }));

    await montar(fetchFalso).darConsentimientoLimitaciones('v1');

    expect(llamada(fetchFalso)[1].method).toBe('POST');
    expect(cuerpo(fetchFalso)).toEqual({ tipo: 'limitaciones', versionTexto: 'v1' });
  });

  test('retirarlo es un DELETE a /consentimientos/limitaciones', async () => {
    const fetchFalso = jest.fn(async () => respuesta(204));

    await montar(fetchFalso).retirarConsentimientoLimitaciones();

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/consentimientos/limitaciones`);
    expect(llamada(fetchFalso)[1].method).toBe('DELETE');
  });

  test('las restricciones llegan validadas y los nulos pasan a undefined', async () => {
    const fetchFalso = jest.fn(async () =>
      respuesta(200, [{ ...restriccion(), patron: null, parametro: null, textoOriginal: null }]),
    );

    const lista = await montar(fetchFalso).restricciones();

    expect(lista).toEqual([restriccion()]);
    expect(lista[0]?.patron).toBeUndefined();
  });

  test('un código de región desconocido se rechaza como respuesta inesperada', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, [{ ...restriccion(), region: 'dedo_gordo' }]));

    await expect(montar(fetchFalso).restricciones()).rejects.toThrow('formato esperado');
  });

  test('crear manda el cuerpo sin uuid y lee la restricción creada', async () => {
    const fetchFalso = jest.fn(async () => respuesta(201, restriccion()));
    const { uuid: _uuid, ...datos } = restriccion();

    const creada = await montar(fetchFalso).crearRestriccion(datos);

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/restricciones`);
    expect(llamada(fetchFalso)[1].method).toBe('POST');
    expect(cuerpo(fetchFalso)).toEqual(datos);
    expect(creada.uuid).toBe(restriccion().uuid);
  });

  test('actualizar es un PUT a /restricciones/{uuid}', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, restriccion({ tipo: 'evitar' })));
    const { uuid, ...datos } = restriccion({ tipo: 'evitar' });

    await montar(fetchFalso).actualizarRestriccion(uuid, datos);

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/restricciones/${uuid}`);
    expect(llamada(fetchFalso)[1].method).toBe('PUT');
  });

  test('borrar es un DELETE a /restricciones/{uuid}', async () => {
    const fetchFalso = jest.fn(async () => respuesta(204));

    await montar(fetchFalso).borrarRestriccion(restriccion().uuid);

    expect(llamada(fetchFalso)[1].method).toBe('DELETE');
    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/restricciones/${restriccion().uuid}`);
  });

  test('las preguntas del cribado salen del backend', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, cuestionarioDePrueba()));

    const cuestionario = await montar(fetchFalso).preguntasCribado();

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/cribado/preguntas`);
    expect(cuestionario.preguntas).toHaveLength(7);
    expect(cuestionario.semanasReciente).toBe(4);
  });

  test('cribar manda respuestas y recientes y devuelve el nivel', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { nivel: 'parar_urgencias', motivos: ['Motivo.'] }));
    const solicitud = { respuestas: { embarazo: false }, recientes: [] };

    const resultado = await montar(fetchFalso).cribar(solicitud);

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/cribado`);
    expect(cuerpo(fetchFalso)).toEqual(solicitud);
    expect(resultado).toEqual({ nivel: 'parar_urgencias', motivos: ['Motivo.'] });
  });

  test('un nivel de cribado desconocido se rechaza', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { nivel: 'quizas', motivos: [] }));

    await expect(montar(fetchFalso).cribar({ respuestas: {}, recientes: [] })).rejects.toThrow('formato esperado');
  });

  test('las banderas llegan con su nivel, motivo y guía', async () => {
    const banderas = [{ nivel: 'urgencias_hoy', motivo: 'Motivo.', guia: 'Guía.' }];
    const fetchFalso = jest.fn(async () => respuesta(200, { bloquea: true, banderas }));

    const resultado = await montar(fetchFalso).evaluarBanderas({
      dolorPorZona: { rodilla: 4 },
      dolorEnReposo: false,
      dolorNocturnoQueDespierta: false,
      hormigueoOEntumecimiento: false,
      perdidaDeFuerza: false,
      perdidaControlEsfinteresOAnestesiaSillaDeMontar: false,
      fiebreConDolorDeEspalda: false,
      traumatismoReciente: false,
      perdidaDePesoSinExplicacion: false,
      dolorToracico: false,
      mareoODesmayo: false,
    });

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/banderas`);
    expect(resultado).toEqual({ bloquea: true, banderas });
  });

  test('el efecto trae las tres decisiones con sustituto y anotaciones', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, efectoDePrueba()));

    const efecto = await montar(fetchFalso).efectoRestricciones();

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/restricciones/efecto`);
    expect(efecto.map((e) => e.decision)).toEqual(['excluido', 'sustituido', 'con_anotaciones']);
    expect(efecto[1]?.sustituto?.nombre).toBe('Prensa de piernas');
  });
});

describe('Estado del bloqueo y errores con código', () => {
  test('el estado se lee de /banderas/estado y admite nivel nulo', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { bloquea: false, nivel: null }));

    const estado = await montar(fetchFalso).estadoBloqueo();

    expect(llamada(fetchFalso)[0]).toBe(`${URL_BASE}/api/v1/banderas/estado`);
    expect(estado).toEqual({ bloquea: false, nivel: null });
  });

  test('un nivel desconocido es una respuesta inesperada', async () => {
    const fetchFalso = jest.fn(async () => respuesta(200, { bloquea: true, nivel: 'inventado' }));

    await expect(montar(fetchFalso).estadoBloqueo()).rejects.toThrow('formato esperado');
  });

  test('el 409 de generar conserva el código estable y el mensaje del backend', async () => {
    const problema = { status: 409, detail: 'No generamos tu plan por ahora.', codigo: 'bandera_roja_activa' };
    const fetchFalso = jest.fn(async () => respuesta(409, problema));

    const error = await montar(fetchFalso).generarPlan().catch((e: unknown) => e);

    expect(error).toMatchObject({ estado: 409, codigo: 'bandera_roja_activa', message: 'No generamos tu plan por ahora.' });
  });
});
