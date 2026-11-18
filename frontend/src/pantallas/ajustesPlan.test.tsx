import { fireEvent, screen, waitFor, within } from '@testing-library/react-native';

import { ErrorApi } from '../api/errores';
import type { VistaPrevia } from '../api/esquemas';
import { NOTA_PREFERENCIAS, SIN_PREFERENCIAS } from '../plan/ListaPreferencias';
import { apiFalsa, planDePrueba, preferenciasDePrueba, vistaPreviaDePrueba } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { AjustesPlan } from './AjustesPlan';
import { Ejercicio } from './Ejercicio';
import { MiPlan } from './MiPlan';
import { CARGANDO_VISTA_PREVIA, SIN_CAMBIOS, VistaPreviaAjuste } from './VistaPreviaAjuste';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter, useFocusEffect: (efecto: () => void) => jest.requireActual('react').useEffect(efecto, [efecto]) }));

beforeEach(() => jest.clearAllMocks());

const conPlan = (cambios = {}) => apiFalsa({ planActual: jest.fn().mockResolvedValue(planDePrueba()), ...cambios });
const boton = (nombre: string) => screen.getByRole('button', { name: nombre });

describe('acciones que abren la vista previa', () => {
  test.each([
    ['Una serie más', 'mas'],
    ['Una serie menos', 'menos'],
    ['Reemplazar', 'reemplazar'],
    ['No recomendar', 'no_recomendar'],
  ])('«%s» en el detalle del ejercicio abre la vista previa de «%s»', async (etiqueta, tipo) => {
    await renderizar(<Ejercicio dia={1} idExterno="remo" />, conPlan());
    await screen.findByLabelText('Ajustar este ejercicio');

    await fireEvent.press(boton(etiqueta));

    expect(mockRouter.push).toHaveBeenCalledWith({ pathname: '/vista-previa', params: { tipo, id: 'remo' } });
  });

  test('«Más variedad» en Mi plan abre su vista previa', async () => {
    await renderizar(<MiPlan />, conPlan());

    await fireEvent.press(await screen.findByRole('button', { name: 'Más variedad' }));

    expect(mockRouter.push).toHaveBeenCalledWith({ pathname: '/vista-previa', params: { tipo: 'variedad' } });
  });

  test('«Enfocar músculos» parte de los guardados, deja 2 como mucho y abre la vista previa', async () => {
    await renderizar(<MiPlan />, conPlan());
    await fireEvent.press(await screen.findByRole('button', { name: 'Enfocar músculos' }));
    const grupo = within(screen.getByLabelText('Músculos a enfocar'));
    await waitFor(() => expect(grupo.getByRole('checkbox', { name: 'Bíceps' })).toBeChecked());

    await fireEvent.press(grupo.getByRole('checkbox', { name: 'Pecho' }));
    await fireEvent.press(grupo.getByRole('checkbox', { name: 'Gemelos' }));
    expect(grupo.getByRole('checkbox', { name: 'Gemelos' })).not.toBeChecked();
    await fireEvent.press(boton('Ver cómo queda'));

    expect(mockRouter.push).toHaveBeenCalledWith({ pathname: '/vista-previa', params: { tipo: 'enfocar', musculos: 'biceps,pecho' } });
  });
});

describe('vista previa', () => {
  test('mientras resuelve lo dice, y luego enseña qué entra, qué sale y las series por músculo', async () => {
    let resolver: (v: VistaPrevia) => void = () => undefined;
    const api = apiFalsa({ vistaPrevia: jest.fn().mockReturnValue(new Promise<VistaPrevia>((r) => (resolver = r))) });
    await renderizar(<VistaPreviaAjuste ajuste={{ tipo: 'no_recomendar', idExterno: 'press_banca' }} />, api);

    expect(screen.getByText(CARGANDO_VISTA_PREVIA)).toBeOnTheScreen();
    resolver(vistaPreviaDePrueba());
    expect(await screen.findByText('Press inclinado · 4 series a la semana')).toBeOnTheScreen();
    expect(within(screen.getByLabelText('Salen')).getByText('Press de banca · tenía 4 series')).toBeOnTheScreen();
    expect(screen.getByText('Remo con barra · de 3 a 4 series')).toBeOnTheScreen();
    expect(within(screen.getByLabelText('Series por músculo')).getByText('12 → 10,5')).toBeOnTheScreen();
    expect(api.vistaPrevia).toHaveBeenCalledWith({ tipo: 'no_recomendar', idExterno: 'press_banca' });
    expect(api.aplicarAjuste).not.toHaveBeenCalled();
  });

  test('«Aplicar» guarda con la versión de la vista previa y vuelve a Mi plan', async () => {
    const api = apiFalsa();
    await renderizar(<VistaPreviaAjuste ajuste={{ tipo: 'variedad' }} />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Aplicar' }));

    await waitFor(() =>
      expect(api.aplicarAjuste).toHaveBeenCalledWith({ tipo: 'variedad', version: vistaPreviaDePrueba().version }),
    );
    await waitFor(() => expect(mockRouter.navigate).toHaveBeenCalledWith('/entreno'));
  });

  test('«Descartar» vuelve atrás sin guardar', async () => {
    const api = apiFalsa();
    await renderizar(<VistaPreviaAjuste ajuste={{ tipo: 'variedad' }} />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Descartar' }));

    expect(mockRouter.back).toHaveBeenCalled();
    expect(api.aplicarAjuste).not.toHaveBeenCalled();
  });

  test('si no cabe, enseña el porqué del backend y deja volver', async () => {
    const porQue = 'Remo con barra ya tiene 15 series a la semana, el tope de 5 por sesión en tus días.';
    const api = apiFalsa({ vistaPrevia: jest.fn().mockRejectedValue(new ErrorApi(422, porQue)) });
    await renderizar(<VistaPreviaAjuste ajuste={{ tipo: 'mas', idExterno: 'remo' }} />, api);

    expect(await screen.findByText(porQue)).toBeOnTheScreen();
    await fireEvent.press(boton('Volver'));
    expect(mockRouter.back).toHaveBeenCalled();
  });

  test('sin cambios lo dice; si aplicar falla, enseña el error', async () => {
    const api = apiFalsa({
      vistaPrevia: jest.fn().mockResolvedValue(vistaPreviaDePrueba({ ejercicios: [], musculos: [] })),
      aplicarAjuste: jest.fn().mockRejectedValue(new ErrorApi(409, 'Tu plan ha cambiado desde la vista previa.')),
    });
    await renderizar(<VistaPreviaAjuste ajuste={{ tipo: 'variedad' }} />, api);

    expect(await screen.findByText(SIN_CAMBIOS)).toBeOnTheScreen();
    await fireEvent.press(boton('Aplicar'));
    expect(await screen.findByText('Tu plan ha cambiado desde la vista previa.')).toBeOnTheScreen();
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });
});

describe('lo que has ajustado', () => {
  test('lista lo guardado y «Quitar» manda el resto', async () => {
    const api = apiFalsa();
    await renderizar(<AjustesPlan />, api);

    expect(await screen.findByText('No recomendar: Press de banca')).toBeOnTheScreen();
    expect(screen.getByText('Enfocado: Bíceps')).toBeOnTheScreen();
    expect(screen.getByText('Remo con barra: 5 series a la semana')).toBeOnTheScreen();
    expect(screen.getByText(NOTA_PREFERENCIAS)).toBeOnTheScreen();
    await fireEvent.press(boton('Quitar No recomendar: Press de banca'));

    await waitFor(() =>
      expect(api.guardarPreferencias).toHaveBeenCalledWith({ exclusiones: [], prioritarios: ['biceps'], series: { remo: 5 }, variedad: true }),
    );
    await waitFor(() => expect(screen.queryByText('No recomendar: Press de banca')).toBeNull());
  });

  test('quitar «Más variedad» y unas series fijadas', async () => {
    const api = apiFalsa();
    await renderizar(<AjustesPlan />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Quitar Más variedad' }));
    await waitFor(() => expect(api.guardarPreferencias).toHaveBeenLastCalledWith(expect.objectContaining({ variedad: false })));
    await fireEvent.press(await screen.findByRole('button', { name: 'Quitar Remo con barra: 5 series a la semana' }));
    await waitFor(() => expect(api.guardarPreferencias).toHaveBeenLastCalledWith(expect.objectContaining({ series: {} })));
  });

  test('sin nada guardado lo dice', async () => {
    const vacias = preferenciasDePrueba({ exclusiones: [], prioritarios: [], series: [], variedad: false });
    await renderizar(<AjustesPlan />, apiFalsa({ preferencias: jest.fn().mockResolvedValue(vacias) }));

    expect(await screen.findByText(SIN_PREFERENCIAS)).toBeOnTheScreen();
  });
});
