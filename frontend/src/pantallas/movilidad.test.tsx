import { fireEvent, renderHook, screen, waitFor, within } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';

import { ErrorApi } from '../api/errores';
import type { Movilidad as DatosMovilidad } from '../api/esquemas';
import { useResumenMovilidad } from '../movilidad/consultas';
import { HEURISTICA, NOTA_MOVILIDAD } from '../movilidad/tests';
import { apiFalsa } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { ProveedorSesion } from '../sesion/sesion';
import { Movilidad, VERSION_TEXTO_MEDIDAS } from './Movilidad';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

beforeEach(() => jest.clearAllMocks());

const HECHO = '2026-11-18T10:00:00Z';
const conMedidas = (movilidad: DatosMovilidad = {}, cambios: Parameters<typeof apiFalsa>[0] = {}) =>
  apiFalsa({ tieneConsentimientoMedidas: jest.fn().mockResolvedValue(true), movilidad: jest.fn().mockResolvedValue(movilidad), ...cambios });

const grupo = (pregunta: string) => screen.getByLabelText(pregunta);
const elegir = (pregunta: string, opcion: string) => fireEvent.press(within(grupo(pregunta)).getByRole('radio', { name: opcion }));

/** Palabras que la finalidad de fitness (ADR 0007) prohíbe en cualquier texto de la app. */
const PROHIBIDAS = /rehabilit|alivi|corrige|previene lesion|desequilibr|atrofia|seguro para tu lesi|tratamiento|diagn[oó]stic/i;

describe('Tests de movilidad', () => {
  test('sin consentimiento enseña qué se guarda y no pide los tests', async () => {
    const api = apiFalsa();
    await renderizar(<Movilidad />, api);

    expect(await screen.findByText('Tus tests de movilidad, solo si tú quieres')).toBeOnTheScreen();
    expect(screen.queryByText('¿A qué distancia de la pared llega tu pie peor?')).toBeNull();
    expect(api.movilidad).not.toHaveBeenCalled();
  });

  test('dar el consentimiento manda la versión del texto y abre los tests', async () => {
    const api = apiFalsa();
    await renderizar(<Movilidad />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Acepto' }));

    expect(api.darConsentimientoMedidas).toHaveBeenCalledWith(VERSION_TEXTO_MEDIDAS);
    expect(await screen.findByText('¿A qué distancia de la pared llega tu pie peor?')).toBeOnTheScreen();
  });

  test('«Ahora no» vuelve atrás sin dar nada', async () => {
    const api = apiFalsa();
    await renderizar(<Movilidad />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Ahora no' }));

    expect(mockRouter.back).toHaveBeenCalled();
    expect(api.darConsentimientoMedidas).not.toHaveBeenCalled();
  });

  test('sin tests hechos los tres dicen «Sin hacer», con instrucciones, heurística y nota del plan', async () => {
    await renderizar(<Movilidad />, conMedidas());

    expect(await screen.findByText('Rodilla a la pared')).toBeOnTheScreen();
    expect(screen.getByText('Rotación de cadera sentado')).toBeOnTheScreen();
    expect(screen.getByText('Flexión de hombro tumbado')).toBeOnTheScreen();
    expect(screen.getAllByText('Resultado: sin hacer')).toHaveLength(3);
    expect(screen.getByText(HEURISTICA)).toBeOnTheScreen();
    expect(screen.getByText(NOTA_MOVILIDAD)).toBeOnTheScreen();
    expect(screen.getByText(/^1\. Descalzo, de cara a una pared/)).toBeOnTheScreen();
    expect(JSON.stringify(screen.toJSON())).not.toMatch(PROHIBIDAS);
  });

  test('carga lo guardado: tramo marcado y resultado con fecha', async () => {
    await renderizar(<Movilidad />, conMedidas({ tobillo: { tramo: 'limitado', hechoEn: HECHO } }));

    await screen.findByText('Rodilla a la pared');
    expect(within(grupo('¿A qué distancia de la pared llega tu pie peor?')).getByRole('radio', { name: 'Menos de 8 cm' })).toBeSelected();
    expect(screen.getByText('Resultado: limitado · 18 de noviembre de 2026')).toBeOnTheScreen();
  });

  test('elige un tramo por test, guarda los tres y enseña el resultado', async () => {
    const api = conMedidas({ hombro: { tramo: 'bueno', hechoEn: HECHO } });
    await renderizar(<Movilidad />, api);
    await screen.findByText('Rodilla a la pared');

    await elegir('¿A qué distancia de la pared llega tu pie peor?', 'Entre 8 y 12 cm');
    await elegir('¿Hasta dónde llega tu giro más corto?', 'Menos de 30°: lejos de la diagonal');
    // Volver a pulsar la elegida la quita: ese test queda sin hacer.
    await elegir('¿Hasta dónde llegan tus brazos sin despegar la zona lumbar?', 'Tocan el suelo con los codos estirados');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(api.guardarMovilidad).toHaveBeenCalledWith({ tobillo: 'medio', cadera: 'limitado', hombro: null }));
    expect(await screen.findByText('Guardado. Cuenta la próxima vez que generes o regeneres tu plan.')).toBeOnTheScreen();
    expect(screen.getByText('Resultado: medio · 18 de noviembre de 2026')).toBeOnTheScreen();
    expect(screen.getAllByText('Resultado: sin hacer')).toHaveLength(1);
  });

  test('si el guardado falla enseña el error y deja reintentar', async () => {
    const guardarMovilidad = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue({});
    await renderizar(<Movilidad />, conMedidas({}, { guardarMovilidad }));

    await fireEvent.press(await screen.findByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText(/^Guardado\./)).toBeOnTheScreen();
  });

  test('si los tests no cargan enseña el error y deja reintentar, sin formulario', async () => {
    const movilidad = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue({});
    await renderizar(<Movilidad />, conMedidas({}, { movilidad }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Guardar' })).toBeNull();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('button', { name: 'Guardar' })).toBeOnTheScreen();
  });

  test('retirar el consentimiento pide confirmación, borra y vuelve a la pantalla de consentimiento', async () => {
    const api = conMedidas({ tobillo: { tramo: 'medio', hechoEn: HECHO } });
    await renderizar(<Movilidad />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar mi consentimiento' }));
    expect(screen.getByText(/Se borrarán tus tests de movilidad guardados\./)).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, retirar y borrar' }));

    await waitFor(() => expect(api.retirarConsentimientoMedidas).toHaveBeenCalled());
    expect(await screen.findByText('Tus tests de movilidad, solo si tú quieres')).toBeOnTheScreen();
  });

  test('vuelve atrás con el botón de la cabecera', async () => {
    await renderizar(<Movilidad />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Volver' }));

    expect(mockRouter.back).toHaveBeenCalled();
  });
});

describe('useResumenMovilidad', () => {
  function resumir(api: ReturnType<typeof apiFalsa>) {
    const consultas = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } });
    const envoltorio = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={consultas}>
        <ProveedorSesion api={api}>{children}</ProveedorSesion>
      </QueryClientProvider>
    );
    return renderHook(() => useResumenMovilidad(), { wrapper: envoltorio });
  }
  const hecho = { tramo: 'bueno' as const, hechoEn: HECHO };

  test('sin consentimiento, «Pendientes» en aviso', async () => {
    const { result } = await resumir(apiFalsa());
    await waitFor(() => expect(result.current).toEqual({ valor: 'Pendientes', pendiente: true }));
  });

  test('cuenta los hechos: ninguno, 2 de 3 y todos', async () => {
    const ninguno = await resumir(conMedidas());
    await waitFor(() => expect(ninguno.result.current).toEqual({ valor: 'Pendientes', pendiente: true }));
    const dos = await resumir(conMedidas({ tobillo: hecho, hombro: hecho }));
    await waitFor(() => expect(dos.result.current).toEqual({ valor: '2 de 3', pendiente: true }));
    const todos = await resumir(conMedidas({ tobillo: hecho, cadera: hecho, hombro: hecho }));
    await waitFor(() => expect(todos.result.current).toEqual({ valor: 'Hechos', pendiente: false }));
  });

  test('si algo falla no inventa: «No disponible»', async () => {
    const sinConsentimiento = await resumir(apiFalsa({ tieneConsentimientoMedidas: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) }));
    await waitFor(() => expect(sinConsentimiento.result.current.valor).toBe('No disponible'));
    const sinTests = await resumir(conMedidas({}, { movilidad: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) }));
    await waitFor(() => expect(sinTests.result.current.valor).toBe('No disponible'));
  });
});
