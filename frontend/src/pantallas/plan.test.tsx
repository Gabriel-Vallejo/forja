import { fireEvent, screen, waitFor } from '@testing-library/react-native';
import { processColor } from 'react-native';

import { ErrorApi } from '../api/errores';
import { AVISO_PRIMERA_SEMANA } from '../plan/plan';
import { apiFalsa, planDePrueba } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { dark } from '../theme';
import { Ejercicio } from './Ejercicio';
import { Hoy } from './Hoy';
import { MiPlan } from './MiPlan';
import { Perfil } from './Perfil';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

beforeEach(() => jest.clearAllMocks());

/** Relleno del primer trazo del grupo (el cuerpo es decorativo y su espejo repite el testID). */
const fill = (testID: string) => screen.getAllByTestId(testID, { includeHiddenElements: true })[0]?.props.fill?.payload;

const conPlan = (primeraSemana = false, diaDeHoy = 1) =>
  apiFalsa({ planActual: jest.fn().mockResolvedValue(planDePrueba(primeraSemana, diaDeHoy)) });

describe('Hoy', () => {
  test('la sesión de hoy sale del plan y «Empezar sesión» lleva a la sesión', async () => {
    await renderizar(<Hoy />, conPlan());

    expect(await screen.findByText('Día 1 de 2')).toBeOnTheScreen();
    expect(screen.getByText('3 ejercicios · 12 series')).toBeOnTheScreen();
    expect(screen.getByText('TORSO')).toBeOnTheScreen();
    expect(screen.getByLabelText('Después · Día 2: PIERNA')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Empezar sesión' }));

    expect(mockRouter.push).toHaveBeenCalledWith('/sesion');
  });

  test('hoy es el día que dice el servidor, con su nombre, sus grupos y el cuerpo coloreado', async () => {
    await renderizar(<Hoy />, conPlan(false, 2));

    expect(await screen.findByText('Día 2 de 2')).toBeOnTheScreen();
    expect(screen.getByLabelText('Después · Día 1: TORSO')).toBeOnTheScreen();
    expect(screen.getByText('Cuádriceps')).toBeOnTheScreen();
    expect(fill('grupo-cuadriceps-frente')).toBe(processColor(dark.color.primary));
    expect(fill('grupo-abdomen-frente')).toBe(processColor(dark.cuerpo.neutro));
  });

  test('avisa de que la racha es de ejemplo', async () => {
    await renderizar(<Hoy />, conPlan());

    expect(await screen.findByLabelText('Datos de ejemplo')).toBeOnTheScreen();
  });

  test('sin plan, ofrece generarlo y vuelve a pedirlo', async () => {
    const planActual = jest.fn().mockResolvedValueOnce(null).mockResolvedValue(planDePrueba());
    const api = apiFalsa({ planActual });
    await renderizar(<Hoy />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Generar mi plan' }));

    expect(await screen.findByText('Día 1 de 2')).toBeOnTheScreen();
    expect(api.generarPlan).toHaveBeenCalledTimes(1);
  });

  test('si falla la carga, enseña el error y deja reintentar', async () => {
    const planActual = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue(planDePrueba());
    await renderizar(<Hoy />, apiFalsa({ planActual }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Día 1 de 2')).toBeOnTheScreen();
  });
});

describe('Plan', () => {
  test('con cinco días salen todas las pestañas, en una fila que se desplaza, y se puede elegir la última', async () => {
    const base = planDePrueba();
    const molde = base.dias[0]!;
    const dias = [1, 2, 3, 4, 5].map((n) => ({ ...molde, dia: n, nombre: `DÍA-${n}` }));
    await renderizar(<MiPlan />, apiFalsa({ planActual: jest.fn().mockResolvedValue({ ...base, dias }) }));

    expect(await screen.findAllByRole('tab')).toHaveLength(5);
    expect(screen.getByLabelText('Días').props.horizontal).toBe(true);
    await fireEvent.press(screen.getByRole('tab', { name: 'Día 5, DÍA-5' }));

    expect(screen.getByRole('tab', { name: 'Día 5, DÍA-5' })).toBeSelected();
  });

  test('con tres días o menos las pestañas se reparten el ancho sin desplazarse', async () => {
    await renderizar(<MiPlan />, conPlan());
    await screen.findAllByRole('tab');

    expect(screen.getByLabelText('Días').props.horizontal).toBeFalsy();
  });

  test('filas numeradas con su prescripción y el estado del plan', async () => {
    await renderizar(<MiPlan />, conPlan());

    expect(await screen.findByText('Un buen plan')).toBeOnTheScreen();
    expect(screen.getByText('01')).toBeOnTheScreen();
    expect(screen.getByText('4 × 6-10 · RIR 2 · 80 kg')).toBeOnTheScreen();
    expect(screen.getByText('4 × 6-10 · RIR 2 · tu peso')).toBeOnTheScreen();
  });

  test('el aviso de primera semana sale una sola vez y no se repite en las filas', async () => {
    await renderizar(<MiPlan />, conPlan(true));

    expect(await screen.findAllByText(AVISO_PRIMERA_SEMANA)).toHaveLength(1);
    expect(screen.queryByText(/elige una carga con la que te queden/)).toBeNull();
  });

  test('sin primera semana no hay aviso y cada fila dice qué toca', async () => {
    await renderizar(<MiPlan />, conPlan());

    expect(await screen.findAllByText(/^Repite 80 kg/)).toHaveLength(3);
    expect(screen.queryByText(AVISO_PRIMERA_SEMANA)).toBeNull();
  });

  test('los días son pestañas y una fila abre su detalle', async () => {
    await renderizar(<MiPlan />, conPlan());

    expect(await screen.findByRole('tab', { name: 'Día 1, TORSO, hoy' })).toBeSelected();
    await fireEvent.press(screen.getByRole('tab', { name: 'Día 2, PIERNA' }));
    expect(screen.getByRole('tab', { name: 'Día 2, PIERNA' })).toBeSelected();
    await fireEvent.press(screen.getByRole('button', { name: /^01\. Sentadilla/ }));

    expect(mockRouter.push).toHaveBeenCalledWith({ pathname: '/ejercicio', params: { dia: '2', id: 'sentadilla' } });
  });
});

describe('Detalle de ejercicio', () => {
  test('chips con los músculos (los secundarios con «½») y el cuerpo con ellos resaltados', async () => {
    await renderizar(<Ejercicio dia={1} idExterno="remo" />, conPlan());

    expect(await screen.findByText('Dorsal')).toBeOnTheScreen();
    expect(screen.getByText('Bíceps ½')).toBeOnTheScreen();
    expect(fill('grupo-dorsal-espalda')).toBe(processColor(dark.color.primary));
    expect(fill('grupo-lumbar-espalda')).toBe(processColor(dark.cuerpo.neutro));
  });

  test('enseña el ejercicio del plan con lo que toca y «Empezar» lleva a la sesión', async () => {
    await renderizar(<Ejercicio dia={1} idExterno="remo" />, conPlan());

    expect(await screen.findByRole('header', { name: 'REMO EN MÁQUINA' })).toBeOnTheScreen();
    expect(screen.getByText('Día 1 · ejercicio 02')).toBeOnTheScreen();
    expect(screen.getByText('4 × 6-10 · RIR 2 · 60 kg')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Empezar' }));

    expect(mockRouter.push).toHaveBeenCalledWith('/sesion');
  });

  test('si el ejercicio ya no está, lo dice', async () => {
    await renderizar(<Ejercicio dia={1} idExterno="no-existe" />, conPlan());

    expect(await screen.findByText('Este ejercicio ya no está en tu plan.')).toBeOnTheScreen();
  });
});

describe('Perfil', () => {
  test('cerrar sesión llama a la API', async () => {
    const api = apiFalsa();
    await renderizar(<Perfil />, api);

    await fireEvent.press(screen.getByRole('button', { name: 'Cerrar sesión' }));

    await waitFor(() => expect(api.cerrarSesion).toHaveBeenCalledTimes(1));
  });

  test('avisa de que el rango y las cifras son de ejemplo', async () => {
    await renderizar(<Perfil />);

    expect(screen.getByLabelText('Datos de ejemplo')).toBeOnTheScreen();
    expect(screen.getByText('ACERO II · top 26 %')).toBeOnTheScreen();
  });
});
