import { fireEvent, screen, waitFor } from '@testing-library/react-native';
import { Linking, processColor } from 'react-native';

import { ErrorApi } from '../api/errores';
import { AVISO_PRIMERA_SESION } from '../plan/plan';
import { apiFalsa, planDePrueba } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { dark } from '../theme';
import { Ejercicio } from './Ejercicio';
import { Hoy } from './Hoy';
import { MiPlan } from './MiPlan';
import { Perfil } from './Perfil';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter, useFocusEffect: (efecto: () => void) => jest.requireActual('react').useEffect(efecto, [efecto]) }));

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

  test('la racha de la cabecera es la real y ya no lleva aviso de ejemplo', async () => {
    await renderizar(<Hoy />, conPlan());

    expect(await screen.findByLabelText('Racha de 4 semanas')).toBeOnTheScreen();
    expect(screen.queryByLabelText('Datos de ejemplo')).toBeNull();
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

    expect(await screen.findAllByText(AVISO_PRIMERA_SESION)).toHaveLength(1);
    expect(screen.queryByText(/elige una carga con la que te queden/)).toBeNull();
  });

  test('el aviso va por día: no sale en un día con carga aunque otro tenga ejercicios nuevos', async () => {
    const base = planDePrueba(false);
    const [torso, pierna] = base.dias;
    const mixto = { ...base, dias: [torso!, { ...pierna!, ejercicios: pierna!.ejercicios.map((e) => ({ ...e, primeraSesion: true })) }] };
    await renderizar(<MiPlan />, apiFalsa({ planActual: jest.fn().mockResolvedValue(mixto) }));

    expect(await screen.findAllByText(/^Repite 80 kg/)).not.toHaveLength(0);
    expect(screen.queryByText(AVISO_PRIMERA_SESION)).toBeNull();
    await fireEvent.press(screen.getByRole('tab', { name: /^Día 2/ }));
    expect(await screen.findByText(AVISO_PRIMERA_SESION)).toBeOnTheScreen();
  });

  test('sin primera semana no hay aviso y cada fila dice qué toca', async () => {
    await renderizar(<MiPlan />, conPlan());

    expect(await screen.findAllByText(/^Repite 80 kg/)).toHaveLength(3);
    expect(screen.queryByText(AVISO_PRIMERA_SESION)).toBeNull();
  });

  test('una fila que ha dolido de forma repetida lo dice', async () => {
    const api = apiFalsa({
      planActual: jest.fn().mockResolvedValue(planDePrueba()),
      dolorRepetido: jest.fn().mockResolvedValue([{ idExterno: 'remo', sesionesConDolor: 3, sesionesMiradas: 3 }]),
    });
    await renderizar(<MiPlan />, api);

    expect(await screen.findByRole('button', { name: /^02\. Remo en máquina.*Te ha dolido en 3 de tus 3 últimas sesiones/ })).toBeOnTheScreen();
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

  test('si le ha dolido de forma repetida lo dice y ofrece ver un cambio, sin cambiar nada sin preguntar', async () => {
    const api = apiFalsa({
      planActual: jest.fn().mockResolvedValue(planDePrueba()),
      dolorRepetido: jest.fn().mockResolvedValue([{ idExterno: 'remo', sesionesConDolor: 2, sesionesMiradas: 3 }]),
    });
    await renderizar(<Ejercicio dia={1} idExterno="remo" />, api);

    expect(await screen.findByText(/Te ha dolido en 2 de tus 3 últimas sesiones/)).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Ver con qué cambiarlo' }));

    expect(mockRouter.push).toHaveBeenCalledWith({ pathname: '/vista-previa', params: { tipo: 'reemplazar', id: 'remo' } });
  });

  test('sin dolor repetido (o si no se puede saber) no avisa', async () => {
    const api = apiFalsa({
      planActual: jest.fn().mockResolvedValue(planDePrueba()),
      dolorRepetido: jest.fn().mockRejectedValue(new ErrorApi(0, 'sin red')),
    });
    await renderizar(<Ejercicio dia={1} idExterno="remo" />, api);

    expect(await screen.findByRole('header', { name: 'REMO EN MÁQUINA' })).toBeOnTheScreen();
    expect(screen.queryByText(/Te ha dolido/)).toBeNull();
  });

  test('si el ejercicio ya no está, lo dice', async () => {
    await renderizar(<Ejercicio dia={1} idExterno="no-existe" />, conPlan());

    expect(await screen.findByText('Este ejercicio ya no está en tu plan.')).toBeOnTheScreen();
  });
});

describe('Por qué este ejercicio', () => {
  const conPorQue = (porQue: jest.Mock) =>
    apiFalsa({ planActual: jest.fn().mockResolvedValue(planDePrueba(false, 1)), porQue });
  const razones = [
    { codigo: 'posicion_estirada', texto: 'Carga el músculo estirado.', fuente: 'Maeo 2021', nivel: 'B' },
    { codigo: 'movilidad', texto: 'Encaja con tu tramo de tobillo (limitado).', fuente: 'Heurística de entrenadores, no ciencia (ADR 0007)', nivel: 'H' },
    { codigo: 'material', texto: 'Encaja con tu material: máquina.', fuente: 'Filtro previo de Forja (ADR 0002)', nivel: 'P' },
  ];

  test('pide las razones de ese día y ejercicio y enseña texto, fuente y nivel con su leyenda', async () => {
    const porQue = jest.fn().mockResolvedValue(razones);
    await renderizar(<Ejercicio dia={1} idExterno="remo" />, conPorQue(porQue));

    expect(await screen.findByText('Carga el músculo estirado.')).toBeOnTheScreen();
    expect(porQue).toHaveBeenCalledWith(1, 'remo');
    expect(screen.getByRole('link', { name: 'Maeo 2021' })).toBeOnTheScreen();
    expect(screen.getByText('Evidencia B')).toBeOnTheScreen();
    expect(screen.getByText('Heurística')).toBeOnTheScreen();
    expect(screen.getByText('Propuesta propia')).toBeOnTheScreen();
    expect(screen.getByLabelText('Nivel H: heurística de entrenadores, no ciencia')).toBeOnTheScreen();
    expect(screen.getByLabelText('Nivel B: evidencia B de la fuente')).toBeOnTheScreen();
    expect(screen.getByLabelText('Nivel P: propuesta propia de Forja')).toBeOnTheScreen();
  });

  test('la cita y su insignia abren el artículo; una heurística no es enlace', async () => {
    const abrir = jest.spyOn(Linking, 'openURL').mockResolvedValue(undefined);
    await renderizar(<Ejercicio dia={1} idExterno="remo" />, conPorQue(jest.fn().mockResolvedValue(razones)));

    await fireEvent.press(await screen.findByRole('link', { name: 'Maeo 2021' }));
    await fireEvent.press(screen.getByRole('link', { name: 'Nivel B: evidencia B de la fuente' }));

    expect(abrir).toHaveBeenCalledTimes(2);
    expect(abrir).toHaveBeenNthCalledWith(1, 'https://doi.org/10.1249/MSS.0000000000002523');
    expect(abrir).toHaveBeenNthCalledWith(2, 'https://doi.org/10.1249/MSS.0000000000002523');
    expect(screen.queryByRole('link', { name: /Nivel H/ })).toBeNull();
    expect(screen.queryByRole('link', { name: /Heurística de entrenadores/ })).toBeNull();
    abrir.mockRestore();
  });

  test('sin razones lo dice', async () => {
    const porQue = jest.fn().mockResolvedValue([]);
    await renderizar(<Ejercicio dia={1} idExterno="remo" />, conPorQue(porQue));

    expect(await screen.findByText('No hay razones guardadas para este ejercicio.')).toBeOnTheScreen();
  });

  test('si falla, avisa y deja reintentar', async () => {
    const porQue = jest
      .fn()
      .mockRejectedValueOnce(new ErrorApi(500, 'Algo ha fallado.'))
      .mockResolvedValue(razones);
    await renderizar(<Ejercicio dia={1} idExterno="remo" />, conPorQue(porQue));

    await fireEvent.press(await screen.findByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Carga el músculo estirado.')).toBeOnTheScreen();
  });

  test('mientras carga lo dice', async () => {
    const porQue = jest.fn().mockReturnValue(new Promise(() => undefined));
    await renderizar(<Ejercicio dia={1} idExterno="remo" />, conPorQue(porQue));

    expect(await screen.findByText('Buscando por qué…')).toBeOnTheScreen();
  });
});

describe('Perfil', () => {
  test('las cifras de arriba salen de las sesiones reales', async () => {
    await renderizar(<Perfil />, apiFalsa());

    expect(await screen.findByText('54')).toBeOnTheScreen();
    expect(screen.getByText('sesiones')).toBeOnTheScreen();
    expect(screen.getByText('312')).toBeOnTheScreen();
    expect(screen.getByText('t movidos')).toBeOnTheScreen();
  });

  test('cerrar sesión llama a la API', async () => {
    const api = apiFalsa();
    await renderizar(<Perfil />, api);

    await fireEvent.press(screen.getByRole('button', { name: 'Cerrar sesión' }));

    await waitFor(() => expect(api.cerrarSesion).toHaveBeenCalledTimes(1));
  });

  test('una cuenta nueva sale sin rango', async () => {
    await renderizar(<Perfil />);

    expect(await screen.findByText('Sin rango todavía')).toBeOnTheScreen();
    expect(screen.queryByText(/ACERO/)).toBeNull();
  });
});
