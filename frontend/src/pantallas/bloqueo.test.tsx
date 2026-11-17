import { fireEvent, screen, waitFor, within } from '@testing-library/react-native';
import { Text } from 'react-native';

import { ErrorApi } from '../api/errores';
import { useBloqueoSalud } from '../limitaciones/consultas';
import { apiFalsa, cuestionarioDePrueba, planDePrueba, preguntasDePrueba } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { Cribado } from './Cribado';
import { Dolor } from './Dolor';
import { Hoy } from './Hoy';
import { Limitaciones } from './Limitaciones';
import { MiPlan } from './MiPlan';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

beforeEach(() => jest.clearAllMocks());

const URGENTE = { bloquea: true, nivel: 'urgencias_hoy' as const };
const PROFESIONAL = { bloquea: true, nivel: 'profesional_en_dias' as const };
const LIBRE = { bloquea: false, nivel: null };
const PALABRAS_PROHIBIDAS = /rehabilit|alivi|corrige|previene lesion|desequilibr|atrofia|diagn[oó]stic/i;

const sinPlan = (cambios: Parameters<typeof apiFalsa>[0] = {}) =>
  apiFalsa({ planActual: jest.fn().mockResolvedValue(null), ...cambios });
const conPlan = (cambios: Parameters<typeof apiFalsa>[0] = {}) =>
  apiFalsa({ planActual: jest.fn().mockResolvedValue(planDePrueba()), ...cambios });
const conConsentimiento = (cambios: Parameters<typeof apiFalsa>[0] = {}) =>
  apiFalsa({ tieneConsentimientoLimitaciones: jest.fn().mockResolvedValue(true), ...cambios });
const elegir = (grupo: string, opcion: string) =>
  fireEvent.press(within(screen.getByLabelText(grupo)).getByRole('radio', { name: opcion }));

/** Mantiene viva la consulta del bloqueo para comprobar que una pantalla la invalida. */
function Sonda() {
  const bloqueo = useBloqueoSalud();
  return <Text>{`sonda:${bloqueo.data?.nivel ?? 'libre'}`}</Text>;
}

describe('Generar mi plan con bloqueo', () => {
  test('con un cribado urgente el aviso habla del cribado y el botón lo repite', async () => {
    const api = sinPlan({ estadoBloqueo: jest.fn().mockResolvedValue({ bloquea: true, nivel: 'parar_urgencias' }) });
    await renderizar(<Hoy />, api);

    expect(await screen.findByText(/por lo que has marcado en el cribado/)).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Repetir el cuestionario' })).toBeNull();
    await fireEvent.press(screen.getByRole('button', { name: 'Repetir el cribado' }));
    expect(mockRouter.push).toHaveBeenCalledWith('/limitaciones');
    expect(JSON.stringify(screen.toJSON())).not.toMatch(PALABRAS_PROHIBIDAS);
  });

  test('con bandera urgente queda deshabilitado, explica por qué y lleva a repetir el cuestionario', async () => {
    const api = sinPlan({ estadoBloqueo: jest.fn().mockResolvedValue(URGENTE) });
    await renderizar(<Hoy />, api);

    expect(await screen.findByText(/acude hoy a urgencias o llama al 112/)).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Generar mi plan' })).toBeDisabled();
    await fireEvent.press(screen.getByRole('button', { name: 'Generar mi plan' }));
    expect(api.generarPlan).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Repetir el cuestionario' }));
    expect(mockRouter.push).toHaveBeenCalledWith('/limitaciones');
    expect(JSON.stringify(screen.toJSON())).not.toMatch(PALABRAS_PROHIBIDAS);
  });

  test('con nivel de profesional en días pide hablar con un profesional sanitario', async () => {
    await renderizar(<Hoy />, sinPlan({ estadoBloqueo: jest.fn().mockResolvedValue(PROFESIONAL) }));

    expect(await screen.findByText(/antes habla con un profesional sanitario/)).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Generar mi plan' })).toBeDisabled();
  });

  test('sin bloqueo se puede generar', async () => {
    const api = sinPlan({ estadoBloqueo: jest.fn().mockResolvedValue(LIBRE) });
    await renderizar(<Hoy />, api);

    await waitFor(() => expect(api.estadoBloqueo).toHaveBeenCalled());
    await fireEvent.press(await screen.findByRole('button', { name: 'Generar mi plan' }));

    await waitFor(() => expect(api.generarPlan).toHaveBeenCalledTimes(1));
  });

  test('si el estado no carga no se bloquea en el cliente: manda el backend', async () => {
    const api = sinPlan({ estadoBloqueo: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) });
    await renderizar(<Hoy />, api);

    const boton = await screen.findByRole('button', { name: 'Generar mi plan' });
    expect(boton).toBeEnabled();
    await fireEvent.press(boton);
    await waitFor(() => expect(api.generarPlan).toHaveBeenCalled());
  });

  test('un 409 bandera_roja_activa enseña el mensaje del backend y vuelve a leer el estado', async () => {
    const estadoBloqueo = jest.fn().mockResolvedValueOnce(LIBRE).mockResolvedValue(URGENTE);
    const generarPlan = jest
      .fn()
      .mockRejectedValue(new ErrorApi(409, 'No generamos tu plan por ahora: antes habla con un profesional sanitario.', 'bandera_roja_activa'));
    await renderizar(<Hoy />, sinPlan({ estadoBloqueo, generarPlan }));

    await fireEvent.press(await screen.findByRole('button', { name: 'Generar mi plan' }));

    expect(await screen.findByText('No generamos tu plan por ahora: antes habla con un profesional sanitario.')).toBeOnTheScreen();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Generar mi plan' })).toBeDisabled());
    expect(estadoBloqueo).toHaveBeenCalledTimes(2);
  });
});

describe('Regenerar mi plan', () => {
  test('pide confirmación, dice qué pasa con las sesiones y no genera hasta confirmar', async () => {
    const api = conPlan();
    await renderizar(<MiPlan />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Regenerar mi plan' }));

    expect(
      screen.getByText('Se crea un plan nuevo con tus limitaciones actuales y con 3 días por semana, 60 minutos por sesión y el material de un gimnasio completo. Las sesiones que ya has registrado se conservan.'),
    ).toBeOnTheScreen();
    expect(api.generarPlan).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Cancelar' }));
    expect(screen.queryByRole('button', { name: 'Sí, regenerar' })).toBeNull();
    expect(api.generarPlan).not.toHaveBeenCalled();
  });

  test('al confirmar regenera, vuelve a pedir el plan actual y cierra la confirmación', async () => {
    const planActual = jest.fn().mockResolvedValue(planDePrueba());
    const api = apiFalsa({ planActual });
    await renderizar(<MiPlan />, api);
    await fireEvent.press(await screen.findByRole('button', { name: 'Regenerar mi plan' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Sí, regenerar' }));

    await waitFor(() => expect(api.generarPlan).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(planActual).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Sí, regenerar' })).toBeNull());
  });

  test('si falla, enseña el error y deja reintentar', async () => {
    const generarPlan = jest.fn().mockRejectedValueOnce(new ErrorApi(500, 'Algo ha fallado.')).mockResolvedValue(undefined);
    await renderizar(<MiPlan />, conPlan({ generarPlan }));
    await fireEvent.press(await screen.findByRole('button', { name: 'Regenerar mi plan' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Sí, regenerar' }));

    expect(await screen.findByText('Algo ha fallado.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, regenerar' }));
    await waitFor(() => expect(generarPlan).toHaveBeenCalledTimes(2));
  });

  test('con bloqueo queda deshabilitado y explica por qué', async () => {
    const api = conPlan({ estadoBloqueo: jest.fn().mockResolvedValue(URGENTE) });
    await renderizar(<MiPlan />, api);

    expect(await screen.findByText(/acude hoy a urgencias/)).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Regenerar mi plan' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Sí, regenerar' })).toBeNull();
  });

  test('si no hay plan, Mi plan sigue ofreciendo generarlo y no regenerar', async () => {
    await renderizar(<MiPlan />, sinPlan());

    expect(await screen.findByRole('button', { name: 'Generar mi plan' })).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Regenerar mi plan' })).toBeNull();
  });
});

describe('Hub de limitaciones con bloqueo y regeneración', () => {
  test('con bloqueo enseña un aviso arriba sin enlace propio y deshabilita regenerar', async () => {
    const api = conConsentimiento({ estadoBloqueo: jest.fn().mockResolvedValue(URGENTE) });
    await renderizar(<Limitaciones />, api);

    expect(await screen.findByText(/acude hoy a urgencias/)).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Repetir el cuestionario' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Regenerar mi plan con mis limitaciones' })).toBeDisabled();
  });

  test('la nota ya no promete nada falso y la acción regenera tras confirmar', async () => {
    const api = conConsentimiento();
    await renderizar(<Limitaciones />, api);

    expect(await screen.findByText(/cuentan cuando regeneras tu plan/)).toBeOnTheScreen();
    await fireEvent.press(await screen.findByRole('button', { name: 'Regenerar mi plan con mis limitaciones' }));
    expect(api.generarPlan).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, regenerar' }));

    await waitFor(() => expect(api.generarPlan).toHaveBeenCalledTimes(1));
  });

  test('retirar el consentimiento deja el estado sin bloqueo en memoria', async () => {
    const estadoBloqueo = jest.fn().mockResolvedValue(URGENTE);
    await renderizar(
      <>
        <Limitaciones />
        <Sonda />
      </>,
      conConsentimiento({ estadoBloqueo }),
    );
    await screen.findByText('sonda:urgencias_hoy');

    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar mi consentimiento' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, retirar y borrar' }));

    expect(await screen.findByText('sonda:libre')).toBeOnTheScreen();
  });
});

describe('Los cuestionarios invalidan el estado del bloqueo', () => {
  test('el cribado vuelve a leer el estado al dar el resultado', async () => {
    const estadoBloqueo = jest.fn().mockResolvedValueOnce(LIBRE).mockResolvedValue({ bloquea: true, nivel: 'parar_urgencias' });
    const api = conConsentimiento({
      estadoBloqueo,
      preguntasCribado: jest.fn().mockResolvedValue(cuestionarioDePrueba()),
    });
    await renderizar(
      <>
        <Cribado />
        <Sonda />
      </>,
      api,
    );
    await screen.findByText('Pregunta 7');
    for (const p of preguntasDePrueba()) {
      await elegir(p.texto, 'No');
    }

    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('sonda:parar_urgencias')).toBeOnTheScreen();
  });

  test('el cuestionario de dolor vuelve a leer el estado al enviarlo', async () => {
    const estadoBloqueo = jest.fn().mockResolvedValueOnce(URGENTE).mockResolvedValue(LIBRE);
    const api = conConsentimiento({ estadoBloqueo });
    await renderizar(
      <>
        <Dolor />
        <Sonda />
      </>,
      api,
    );
    await screen.findByText('sonda:urgencias_hoy');
    const senales = [
      '¿Te duele aunque estés en reposo, sin moverte?',
      '¿Te despierta el dolor por la noche?',
      '¿Notas hormigueo o la zona dormida?',
      '¿Has notado que pierdes fuerza en un brazo o una pierna?',
      '¿Has perdido el control de la vejiga o el intestino, o notas dormida la zona genital o anal?',
      '¿Tienes fiebre y, a la vez, dolor de espalda?',
      '¿Has tenido un golpe o una caída fuerte hace poco?',
      '¿Has perdido peso sin explicación, sin dieta ni cambios en tu vida?',
      '¿Notas dolor u opresión en el pecho?',
      '¿Te has mareado o desmayado?',
    ];
    for (const s of senales) {
      await elegir(s, 'No');
    }

    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('sonda:libre')).toBeOnTheScreen();
  });
});
