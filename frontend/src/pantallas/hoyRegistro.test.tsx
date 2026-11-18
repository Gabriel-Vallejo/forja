import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { ErrorApi } from '../api/errores';
import type { AlmacenRegistro } from '../registro/almacen';
import { crearAlmacenMemoria } from '../registro/almacenMemoria';
import { crearOutbox } from '../registro/outbox';
import { apiFalsa, objetivo, planDePrueba, USUARIO_ID } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { SIN_PLAN_SIN_RED } from './ConPlanParaEntrenar';
import { Ejercicio } from './Ejercicio';
import { Hoy } from './Hoy';
import { MiPlan } from './MiPlan';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn(), dismissTo: jest.fn() };
jest.mock('expo-router', () => ({
  useRouter: () => mockRouter,
  useFocusEffect: (efecto: () => void) => jest.requireActual('react').useEffect(efecto, [efecto]),
}));

const PLAN = planDePrueba();
const SIN_RED = () => jest.fn().mockRejectedValue(new ErrorApi(0, 'sin red'));
const conPlan = (cambios = {}) => apiFalsa({ planActual: jest.fn().mockResolvedValue(PLAN), ...cambios });
const outboxDe = (almacen: AlmacenRegistro) => crearOutbox({ almacen, usuarioActual: async () => USUARIO_ID });

/** Sesión cerrada con las 4 series de press de banca cumplidas: deja una carga siguiente de 82,5 kg. */
async function sesionCerrada(almacen: AlmacenRegistro) {
  const outbox = outboxDe(almacen);
  const { uuid } = await outbox.empezarSesion({ diaPlan: 1, planVersion: PLAN.uuid });
  for (let i = 1; i <= 4; i++) await outbox.registrarSerie(uuid, { ejercicio: 'press_banca', orden: i, pesoKg: 80, repeticiones: 10, rir: 2 });
  await outbox.cerrarSesion(uuid, [objetivo()]);
  return uuid;
}

beforeEach(() => jest.clearAllMocks());

describe('Hoy con el registro del móvil', () => {
  test('sin outbox no enseña nada del registro y el botón es «Empezar sesión»', async () => {
    await renderizar(<Hoy />, conPlan());

    expect(await screen.findByRole('button', { name: 'Empezar sesión' })).toBeOnTheScreen();
    expect(screen.queryByText(/por enviar/)).toBeNull();
  });

  test('con una sesión en curso el botón pasa a «Continuar sesión»', async () => {
    const almacen = crearAlmacenMemoria();
    await outboxDe(almacen).empezarSesion({ diaPlan: 1, planVersion: PLAN.uuid });

    await renderizar(<Hoy />, conPlan(), almacen);

    await fireEvent.press(await screen.findByRole('button', { name: 'Continuar sesión' }));
    expect(mockRouter.push).toHaveBeenCalledWith('/sesion');
  });

  test('«N sesiones por enviar» con «Reintentar ahora»', async () => {
    const almacen = crearAlmacenMemoria();
    await sesionCerrada(almacen);
    const guardarSesion = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'sin red')).mockResolvedValue(undefined);
    await renderizar(<Hoy />, conPlan({ guardarSesion }), almacen);

    expect(await screen.findByText('1 sesión por enviar')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar ahora' }));

    await waitFor(() => expect(screen.queryByText('1 sesión por enviar')).toBeNull());
    expect(guardarSesion).toHaveBeenCalledTimes(2);
  });

  test('con rechazadas avisa y lleva a revisarlas', async () => {
    const almacen = crearAlmacenMemoria();
    const uuid = await sesionCerrada(almacen);
    await almacen.actualizarSesion(uuid, { estado: 'rechazada', ultimoError: 'revisa esta sesión' });
    await renderizar(<Hoy />, conPlan(), almacen);

    await fireEvent.press(await screen.findByRole('link', { name: /^1 sesión por revisar/ }));

    expect(mockRouter.push).toHaveBeenCalledWith('/sesiones-por-revisar');
  });
});

describe('Hoy sin conexión', () => {
  const sinRed = () => conPlan({ planActual: SIN_RED(), guardarSesion: SIN_RED() });

  test('con el plan guardado enseña la semana y «Continuar sesión»; los indicadores siguen', async () => {
    const almacen = crearAlmacenMemoria();
    const outbox = outboxDe(almacen);
    await outbox.guardarPlan(PLAN);
    await sesionCerrada(almacen);
    await outbox.empezarSesion({ diaPlan: 1, planVersion: PLAN.uuid });

    await renderizar(<Hoy />, sinRed(), almacen);

    expect(await screen.findByText(/^Sin conexión: usas el plan guardado el/)).toBeOnTheScreen();
    await fireEvent.press(await screen.findByRole('button', { name: 'Continuar sesión' }));
    expect(mockRouter.push).toHaveBeenCalledWith('/sesion');
    expect(await screen.findByText('1 sesión por enviar')).toBeOnTheScreen();
  });

  test('sin plan guardado lo explica', async () => {
    await renderizar(<Hoy />, sinRed(), crearAlmacenMemoria());

    expect(await screen.findByText(SIN_PLAN_SIN_RED)).toBeOnTheScreen();
  });

  test('sin registro local (web) el fallo de red se ve como siempre', async () => {
    await renderizar(<Hoy />, sinRed());

    expect(await screen.findByText('sin red')).toBeOnTheScreen();
    expect(screen.queryByText(SIN_PLAN_SIN_RED)).toBeNull();
  });
});

describe('carga siguiente calculada sin red en el plan', () => {
  test('Mi plan usa la carga siguiente si la hay', async () => {
    const almacen = crearAlmacenMemoria();
    await sesionCerrada(almacen);

    await renderizar(<MiPlan />, conPlan({ guardarSesion: SIN_RED() }), almacen);

    expect(await screen.findByText('4 × 6-10 · RIR 2 · 82,5 kg')).toBeOnTheScreen();
    expect(screen.getByText('4 × 6-10 · RIR 2 · 60 kg')).toBeOnTheScreen();
  });

  test('el detalle del ejercicio también', async () => {
    const almacen = crearAlmacenMemoria();
    await sesionCerrada(almacen);

    await renderizar(<Ejercicio dia={1} idExterno="press_banca" />, conPlan({ guardarSesion: SIN_RED() }), almacen);

    expect(await screen.findByText('4 × 6-10 · RIR 2 · 82,5 kg')).toBeOnTheScreen();
  });
});
