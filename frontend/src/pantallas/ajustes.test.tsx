import { fireEvent, screen, waitFor, within } from '@testing-library/react-native';

import { ErrorApi } from '../api/errores';
import { esMaterialCompleto } from '../plan/ajustes';
import { apiFalsa, materialesDePrueba, perfilDePrueba, planDePrueba } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { AjustesPlan, ERROR_SIN_MATERIAL, NOTA_AJUSTES } from './AjustesPlan';
import { Hoy } from './Hoy';
import { MiPlan } from './MiPlan';
import { Perfil } from './Perfil';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter, useFocusEffect: (efecto: () => void) => jest.requireActual('react').useEffect(efecto, [efecto]) }));

beforeEach(() => jest.clearAllMocks());

const elegir = (grupo: string, opcion: string) =>
  fireEvent.press(within(screen.getByLabelText(grupo)).getByRole('radio', { name: opcion }));
const ficha = (nombre: string) => within(screen.getByLabelText('Material')).getByRole('checkbox', { name: nombre });

describe('Ajustes del plan', () => {
  test('carga lo guardado: días, minutos y material marcados, y avisa de cuándo cuenta', async () => {
    const perfil = perfilDePrueba({ diasPorSemana: 4, minutosPorSesion: 75, material: ['mancuerna', 'banda'], porDefecto: false });
    await renderizar(<AjustesPlan />, apiFalsa({ perfilEntreno: jest.fn().mockResolvedValue(perfil) }));

    expect(await screen.findByText(NOTA_AJUSTES)).toBeOnTheScreen();
    expect(within(screen.getByLabelText('Días por semana')).getByRole('radio', { name: '4' })).toBeSelected();
    expect(within(screen.getByLabelText('Minutos por sesión')).getByRole('radio', { name: '75 min' })).toBeSelected();
    expect(ficha('Mancuerna')).toBeChecked();
    expect(ficha('Banda elástica')).toBeChecked();
    expect(ficha('Barra')).not.toBeChecked();
    expect(screen.queryByRole('checkbox', { name: 'Otro' })).toBeNull();
  });

  test('ofrece días de 2 a 6 y minutos de 30 a 120 de 15 en 15', async () => {
    await renderizar(<AjustesPlan />);
    await screen.findByLabelText('Días por semana');

    const dias = within(screen.getByLabelText('Días por semana')).getAllByRole('radio');
    const minutos = within(screen.getByLabelText('Minutos por sesión')).getAllByRole('radio');

    expect(dias.map((d) => d.props.accessibilityLabel)).toEqual(['2', '3', '4', '5', '6']);
    expect(minutos.map((m) => m.props.accessibilityLabel)).toEqual(['30 min', '45 min', '60 min', '75 min', '90 min', '105 min', '120 min']);
  });

  test('cambia los ajustes, guarda lo elegido y lo confirma', async () => {
    const api = apiFalsa();
    await renderizar(<AjustesPlan />, api);
    await screen.findByLabelText('Días por semana');

    await elegir('Días por semana', '5');
    await elegir('Minutos por sesión', '90 min');
    await fireEvent.press(ficha('Fitball'));
    await fireEvent.press(ficha('Kettlebell'));
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(api.guardarPerfilEntreno).toHaveBeenCalledWith({
        diasPorSemana: 5,
        minutosPorSesion: 90,
        material: ['barra', 'mancuerna', 'maquina', 'polea', 'peso_corporal', 'banda', 'barra_ez', 'fitball'],
      }),
    );
    expect(await screen.findByText('Guardado. Se aplica la próxima vez que generes o regeneres tu plan.')).toBeOnTheScreen();
  });

  test('sin material no guarda y dice por qué', async () => {
    const api = apiFalsa({ perfilEntreno: jest.fn().mockResolvedValue(perfilDePrueba({ material: ['barra'] })) });
    await renderizar(<AjustesPlan />, api);
    await screen.findByLabelText('Material');

    await fireEvent.press(ficha('Barra'));
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    expect(screen.getByText(`Error: ${ERROR_SIN_MATERIAL}`)).toBeOnTheScreen();
    expect(api.guardarPerfilEntreno).not.toHaveBeenCalled();
    await fireEvent.press(ficha('Polea'));
    expect(screen.queryByText(`Error: ${ERROR_SIN_MATERIAL}`)).toBeNull();
  });

  test('si el guardado falla enseña el error y deja volver a guardar', async () => {
    const guardarPerfilEntreno = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockImplementation(async (a) => ({ ...a, porDefecto: false }));
    await renderizar(<AjustesPlan />, apiFalsa({ guardarPerfilEntreno }));
    await screen.findByLabelText('Días por semana');

    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText(/^Guardado\./)).toBeOnTheScreen();
    expect(guardarPerfilEntreno).toHaveBeenCalledTimes(2);
  });

  test('si no cargan los ajustes enseña el error y deja reintentar, sin formulario inventado', async () => {
    const perfilEntreno = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue(perfilDePrueba());
    await renderizar(<AjustesPlan />, apiFalsa({ perfilEntreno }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Guardar' })).toBeNull();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('button', { name: 'Guardar' })).toBeOnTheScreen();
  });

  test('si no carga el catálogo de material también deja reintentar', async () => {
    const materiales = jest.fn().mockRejectedValueOnce(new ErrorApi(500, 'Algo ha fallado.')).mockResolvedValue(materialesDePrueba());
    await renderizar(<AjustesPlan />, apiFalsa({ materiales }));

    expect(await screen.findByText('Algo ha fallado.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('button', { name: 'Guardar' })).toBeOnTheScreen();
  });

  test('vuelve atrás con el botón de la cabecera', async () => {
    await renderizar(<AjustesPlan />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Volver' }));

    expect(mockRouter.back).toHaveBeenCalled();
  });
});

describe('Material completo', () => {
  const SIETE = ['barra', 'barra_ez', 'mancuerna', 'maquina', 'polea', 'kettlebell', 'peso_corporal'];

  test('son los 7 con ejercicios; un perfil antiguo con banda sigue siendo completo y si falta uno no', () => {
    expect(esMaterialCompleto(SIETE)).toBe(true);
    expect(esMaterialCompleto([...SIETE, 'banda'])).toBe(true);
    expect(esMaterialCompleto(SIETE.filter((m) => m !== 'polea'))).toBe(false);
  });
});

describe('Ajustes en el perfil', () => {
  test('«Días y minutos» y «Material y gimnasio» salen del backend y llevan a los ajustes', async () => {
    await renderizar(<Perfil />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Días y minutos: 3 × 60 min' }));
    expect(mockRouter.push).toHaveBeenLastCalledWith('/ajustes-plan');
    await fireEvent.press(screen.getByRole('button', { name: 'Material y gimnasio: Completo' }));
    expect(mockRouter.push).toHaveBeenLastCalledWith('/ajustes-plan');
  });

  test('con otros ajustes guardados lo dice: 4 × 75 min y N tipos', async () => {
    const perfil = perfilDePrueba({ diasPorSemana: 4, minutosPorSesion: 75, material: ['barra', 'banda', 'polea'], porDefecto: false });
    await renderizar(<Perfil />, apiFalsa({ perfilEntreno: jest.fn().mockResolvedValue(perfil) }));

    expect(await screen.findByRole('button', { name: 'Días y minutos: 4 × 75 min' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Material y gimnasio: 3 tipos' })).toBeOnTheScreen();
  });

  test('si el perfil no carga no inventa datos', async () => {
    await renderizar(<Perfil />, apiFalsa({ perfilEntreno: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) }));

    expect(await screen.findByRole('button', { name: 'Días y minutos: No disponible' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Material y gimnasio: No disponible' })).toBeOnTheScreen();
  });
});

describe('Generar y regenerar usan los ajustes guardados', () => {
  const guardados = perfilDePrueba({ diasPorSemana: 5, minutosPorSesion: 45, material: ['barra', 'mancuerna'], porDefecto: false });

  test('«Generar mi plan» cuenta con qué se arma y manda a cambiar los ajustes', async () => {
    const api = apiFalsa({ perfilEntreno: jest.fn().mockResolvedValue(guardados) });
    await renderizar(<Hoy />, api);

    expect(await screen.findByText('Lo armamos con 5 días por semana, 45 minutos por sesión y 2 tipos de material.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Cambiar ajustes del plan' }));
    expect(mockRouter.push).toHaveBeenCalledWith('/ajustes-plan');
    await fireEvent.press(screen.getByRole('button', { name: 'Generar mi plan' }));

    await waitFor(() => expect(api.generarPlan).toHaveBeenCalledTimes(1));
  });

  test('con los de por defecto habla de gimnasio completo', async () => {
    await renderizar(<Hoy />);

    expect(
      await screen.findByText('Lo armamos con 3 días por semana, 60 minutos por sesión y el material de un gimnasio completo.'),
    ).toBeOnTheScreen();
  });

  test('si el perfil no carga no deja generar con datos inventados y se puede reintentar', async () => {
    const perfilEntreno = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue(perfilDePrueba());
    const api = apiFalsa({ perfilEntreno });
    await renderizar(<Hoy />, api);

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Generar mi plan' })).toBeNull();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('button', { name: 'Generar mi plan' })).toBeOnTheScreen();
    expect(api.generarPlan).not.toHaveBeenCalled();
  });

  test('«Regenerar mi plan» dice con qué ajustes regenera', async () => {
    const api = apiFalsa({
      planActual: jest.fn().mockResolvedValue(planDePrueba()),
      perfilEntreno: jest.fn().mockResolvedValue(guardados),
    });
    await renderizar(<MiPlan />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Regenerar mi plan' }));

    expect(screen.getByText(/con 5 días por semana, 45 minutos por sesión y 2 tipos de material\./)).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, regenerar' }));
    await waitFor(() => expect(api.generarPlan).toHaveBeenCalledTimes(1));
  });

  test('si el perfil no carga, regenerar enseña el error y deja reintentar', async () => {
    const perfilEntreno = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue(guardados);
    await renderizar(<MiPlan />, apiFalsa({ planActual: jest.fn().mockResolvedValue(planDePrueba()), perfilEntreno }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Regenerar mi plan' })).toBeNull();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('button', { name: 'Regenerar mi plan' })).toBeOnTheScreen();
  });
});
