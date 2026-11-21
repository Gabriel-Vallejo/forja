import { fireEvent, screen, waitFor, within } from '@testing-library/react-native';

import { ErrorApi } from '../api/errores';
import { apiFalsa, cuestionarioDePrueba, efectoDePrueba, preguntasDePrueba, restriccion } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { Cribado } from './Cribado';
import { Dolor } from './Dolor';
import { Efecto } from './Efecto';
import { FormularioLimitacion } from './FormularioLimitacion';
import { Limitaciones } from './Limitaciones';
import { Perfil } from './Perfil';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

beforeEach(() => jest.clearAllMocks());

const RODILLA = restriccion();
const HOMBRO = restriccion({ uuid: '0193a3c4-7b2e-7c1d-9f00-0000000000a2', region: 'hombro', lado: 'derecho', tipo: 'evitar', origen: 'profesional' });
const ANTIGUA = restriccion({ uuid: '0193a3c4-7b2e-7c1d-9f00-0000000000a3', region: 'codo', estado: 'historico_sin_limitacion' });

const conConsentimiento = (cambios: Parameters<typeof apiFalsa>[0] = {}) =>
  apiFalsa({ tieneConsentimientoLimitaciones: jest.fn().mockResolvedValue(true), ...cambios });

const elegir = (grupo: string, opcion: string) =>
  fireEvent.press(within(screen.getByLabelText(grupo)).getByRole('radio', { name: opcion }));

/** Palabras que la finalidad de fitness (ADR 0007) prohíbe en cualquier texto de la app. */
const PROHIBIDAS = /rehabilit|alivi|corrige|previene lesion|desequilibr|atrofia|seguro para tu lesi|tratamiento|diagn[oó]stic/i;
const textoVisible = () => JSON.stringify(screen.toJSON());

describe('Limitaciones en el perfil', () => {
  test('sin consentimiento dice «Sin configurar» y lleva a la pantalla real', async () => {
    await renderizar(<Perfil />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Limitaciones: Sin configurar' }));

    expect(mockRouter.push).toHaveBeenCalledWith('/limitaciones');
  });

  test('con consentimiento cuenta las limitaciones activas, sin las antiguas', async () => {
    const api = conConsentimiento({ restricciones: jest.fn().mockResolvedValue([RODILLA, HOMBRO, ANTIGUA]) });
    await renderizar(<Perfil />, api);

    expect(await screen.findByRole('button', { name: 'Limitaciones: 2 activas' })).toBeOnTheScreen();
  });

  test('con una sola dice «1 activa» y con ninguna, «Ninguna»', async () => {
    await renderizar(<Perfil />, conConsentimiento({ restricciones: jest.fn().mockResolvedValue([RODILLA]) }));
    expect(await screen.findByRole('button', { name: 'Limitaciones: 1 activa' })).toBeOnTheScreen();
  });

  test('con consentimiento y sin limitaciones, «Ninguna»', async () => {
    await renderizar(<Perfil />, conConsentimiento());
    expect(await screen.findByRole('button', { name: 'Limitaciones: Ninguna' })).toBeOnTheScreen();
  });

  test('el «2 confirmadas» de ejemplo ya no existe', async () => {
    await renderizar(<Perfil />);
    await screen.findByRole('button', { name: 'Limitaciones: Sin configurar' });

    expect(screen.queryByText(/2 confirmadas/)).toBeNull();
  });

  test('si falla la consulta lo dice en vez de inventar un número', async () => {
    const api = apiFalsa({ tieneConsentimientoLimitaciones: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) });
    await renderizar(<Perfil />, api);

    expect(await screen.findByRole('button', { name: 'Limitaciones: No disponible' })).toBeOnTheScreen();
  });
});

describe('Consentimiento de limitaciones', () => {
  test('sin él explica qué se guarda y no enseña la lista', async () => {
    const api = apiFalsa();
    await renderizar(<Limitaciones />, api);

    expect(await screen.findByText(/limitación funcional/)).toBeOnTheScreen();
    expect(screen.getByText(/Cifrado/)).toBeOnTheScreen();
    // v2: dice también que cubre el dolor de las series y el nivel de aviso, y que retirarlo no anula lo anterior.
    expect(screen.getByText(/dolor de 0 a 10 que anotes en tus series/)).toBeOnTheScreen();
    expect(screen.getByText(/sigue siendo válido/)).toBeOnTheScreen();
    expect(screen.getByText(/sigue funcionando sin que lo des/)).toBeOnTheScreen();
    expect(screen.getByText(/borra todo lo guardado/)).toBeOnTheScreen();
    expect(api.restricciones).not.toHaveBeenCalled();
    expect(textoVisible()).not.toMatch(PROHIBIDAS);
  });

  test('«Acepto» lo da con la versión v1 y pasa a la lista', async () => {
    const api = apiFalsa();
    await renderizar(<Limitaciones />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Acepto' }));

    expect(api.darConsentimientoLimitaciones).toHaveBeenCalledWith('v2');
    expect(await screen.findByRole('button', { name: 'Añadir limitación' })).toBeOnTheScreen();
  });

  test('«Ahora no» vuelve atrás sin dar nada', async () => {
    const api = apiFalsa();
    await renderizar(<Limitaciones />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Ahora no' }));

    expect(mockRouter.back).toHaveBeenCalled();
    expect(api.darConsentimientoLimitaciones).not.toHaveBeenCalled();
  });

  test('si falla al darlo, enseña el error y sigue sin consentimiento', async () => {
    const api = apiFalsa({ darConsentimientoLimitaciones: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) });
    await renderizar(<Limitaciones />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Acepto' }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Añadir limitación' })).toBeNull();
  });

  test('retirarlo pide confirmación, dice que borra las limitaciones y vuelve a pedir el consentimiento', async () => {
    const api = conConsentimiento({ restricciones: jest.fn().mockResolvedValue([RODILLA, HOMBRO]) });
    await renderizar(<Limitaciones />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar mi consentimiento' }));
    expect(screen.getByText(/Se borrarán tus 2 limitaciones/)).toBeOnTheScreen();
    expect(api.retirarConsentimientoLimitaciones).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, retirar y borrar' }));

    await waitFor(() => expect(api.retirarConsentimientoLimitaciones).toHaveBeenCalledTimes(1));
    expect(await screen.findByRole('button', { name: 'Acepto' })).toBeOnTheScreen();
    expect(screen.queryByText('Rodilla')).toBeNull();
  });

  test('cancelar la retirada no llama a la API', async () => {
    const api = conConsentimiento();
    await renderizar(<Limitaciones />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar mi consentimiento' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Cancelar' }));

    expect(api.retirarConsentimientoLimitaciones).not.toHaveBeenCalled();
    expect(screen.queryByText(/Se borrarán/)).toBeNull();
  });
});

describe('Lista de limitaciones', () => {
  test('cada limitación enseña región, tipo, lado, estado y origen', async () => {
    const api = conConsentimiento({ restricciones: jest.fn().mockResolvedValue([RODILLA, HOMBRO]) });
    await renderizar(<Limitaciones />, api);

    expect(await screen.findByText('Rodilla')).toBeOnTheScreen();
    expect(screen.getByText('Sin impacto · Izquierdo · Ahora mismo')).toBeOnTheScreen();
    expect(screen.getByText('Evitar · Derecho · Ahora mismo · Me la indicó un profesional')).toBeOnTheScreen();
    expect(textoVisible()).not.toMatch(PROHIBIDAS);
  });

  test('una limitación solo de patrón se nombra por el patrón', async () => {
    const solo = restriccion({ region: undefined, patron: 'sentadilla' });
    await renderizar(<Limitaciones />, conConsentimiento({ restricciones: jest.fn().mockResolvedValue([solo]) }));

    expect(await screen.findByText('Sentadilla')).toBeOnTheScreen();
  });

  test('sin limitaciones, estado vacío amable', async () => {
    await renderizar(<Limitaciones />, conConsentimiento());

    expect(await screen.findByText('Todavía no has añadido ninguna limitación.')).toBeOnTheScreen();
  });

  test('mientras carga y si falla (con reintento)', async () => {
    const restricciones = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue([RODILLA]);
    await renderizar(<Limitaciones />, conConsentimiento({ restricciones }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Rodilla')).toBeOnTheScreen();
  });

  test('si falla la consulta del consentimiento, error con reintento', async () => {
    const tiene = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue(true);
    await renderizar(<Limitaciones />, apiFalsa({ tieneConsentimientoLimitaciones: tiene }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('button', { name: 'Añadir limitación' })).toBeOnTheScreen();
  });

  test('«Añadir limitación», editar y los accesos llevan a su pantalla', async () => {
    await renderizar(<Limitaciones />, conConsentimiento({ restricciones: jest.fn().mockResolvedValue([RODILLA]) }));

    await fireEvent.press(await screen.findByRole('button', { name: 'Añadir limitación' }));
    expect(mockRouter.push).toHaveBeenLastCalledWith('/limitacion');
    await fireEvent.press(screen.getByRole('button', { name: 'Editar limitación de Rodilla' }));
    expect(mockRouter.push).toHaveBeenLastCalledWith({ pathname: '/limitacion', params: { id: RODILLA.uuid } });
    await fireEvent.press(screen.getByRole('button', { name: 'Qué cambia en mis ejercicios' }));
    expect(mockRouter.push).toHaveBeenLastCalledWith('/efecto');
    await fireEvent.press(screen.getByRole('button', { name: 'Cribado previo al ejercicio' }));
    expect(mockRouter.push).toHaveBeenLastCalledWith('/cribado');
    await fireEvent.press(screen.getByRole('button', { name: 'Cuestionario de dolor' }));
    expect(mockRouter.push).toHaveBeenLastCalledWith('/dolor');
  });

  test('borrar pide confirmación y, al confirmar, borra solo esa y recarga', async () => {
    const restricciones = jest.fn().mockResolvedValueOnce([RODILLA, HOMBRO]).mockResolvedValue([HOMBRO]);
    const api = conConsentimiento({ restricciones });
    await renderizar(<Limitaciones />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Borrar limitación de Rodilla' }));
    expect(api.borrarRestriccion).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, borrar' }));

    await waitFor(() => expect(api.borrarRestriccion).toHaveBeenCalledWith(RODILLA.uuid));
    await waitFor(() => expect(screen.queryByText('Rodilla')).toBeNull());
    expect(screen.getByText('Hombro')).toBeOnTheScreen();
  });

  test('con 30 limitaciones ya no deja añadir más', async () => {
    const treinta = Array.from({ length: 30 }, (_, i) => restriccion({ uuid: `0193a3c4-7b2e-7c1d-9f00-${String(i).padStart(12, '0')}` }));
    await renderizar(<Limitaciones />, conConsentimiento({ restricciones: jest.fn().mockResolvedValue(treinta) }));

    expect(await screen.findByText('Has llegado al máximo de 30 limitaciones.')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Añadir limitación' })).toBeNull();
  });
});

describe('Cribado', () => {
  const conPreguntas = (cambios: Parameters<typeof apiFalsa>[0] = {}) =>
    conConsentimiento({ preguntasCribado: jest.fn().mockResolvedValue(cuestionarioDePrueba()), ...cambios });

  async function contestarTodas(sinSintomas = true) {
    for (const p of preguntasDePrueba()) {
      await elegir(p.texto, sinSintomas ? 'No' : 'Sí');
    }
  }

  test('las siete preguntas salen del backend y el resultado no se pide hasta contestarlas', async () => {
    await renderizar(<Cribado />, conPreguntas());

    expect(await screen.findByText('Pregunta 7')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Ver resultado' })).toBeDisabled();
    await contestarTodas();
    expect(screen.getByRole('button', { name: 'Ver resultado' })).toBeEnabled();
  });

  test('«¿Fue hace poco?» solo sale con un sí en las preguntas que lo admiten y hay que contestarlo', async () => {
    await renderizar(<Cribado />, conPreguntas());
    await screen.findByText('Pregunta 1');

    expect(screen.queryByLabelText('¿Fue hace poco?')).toBeNull();
    await elegir('Pregunta 3', 'Sí');
    expect(screen.queryByLabelText('¿Fue hace poco?')).toBeNull();
    await elegir('Pregunta 1', 'Sí');
    expect(screen.getAllByLabelText('¿Fue hace poco?')).toHaveLength(1);

    await contestarTodas(false);
    expect(screen.getByRole('button', { name: 'Ver resultado' })).toBeDisabled();
  });

  test('las semanas de «reciente» las dice el backend, no la app', async () => {
    const preguntasCribado = jest.fn().mockResolvedValue({ ...cuestionarioDePrueba(), semanasReciente: 6 });
    await renderizar(<Cribado />, conPreguntas({ preguntasCribado }));
    await screen.findByText('Pregunta 1');

    await elegir('Pregunta 1', 'Sí');

    expect(screen.getByText('Cuenta lo de las últimas 6 semanas.')).toBeOnTheScreen();
  });

  test('cambiar un sí a no quita lo de «reciente» de lo que se envía', async () => {
    const api = conPreguntas();
    await renderizar(<Cribado />, api);
    await screen.findByText('Pregunta 1');

    await elegir('Pregunta 1', 'Sí');
    await fireEvent.press(within(screen.getByLabelText('¿Fue hace poco?')).getByRole('radio', { name: 'Sí' }));
    await elegir('Pregunta 1', 'No');
    await contestarTodas();
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    await waitFor(() => expect(api.cribar).toHaveBeenCalledTimes(1));
    expect(api.cribar.mock.calls[0]?.[0].recientes).toEqual([]);
  });

  test('envía las siete respuestas y los síntomas recientes', async () => {
    const api = conPreguntas({ cribar: jest.fn().mockResolvedValue({ nivel: 'seguir', motivos: [] }) });
    await renderizar(<Cribado />, api);
    await screen.findByText('Pregunta 1');

    await contestarTodas();
    await elegir('Pregunta 2', 'Sí');
    await fireEvent.press(within(screen.getByLabelText('¿Fue hace poco?')).getByRole('radio', { name: 'Sí' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    await waitFor(() => expect(api.cribar).toHaveBeenCalledTimes(1));
    expect(api.cribar).toHaveBeenCalledWith({
      respuestas: {
        sintomas_en_esfuerzo: false,
        desmayo_o_mareo: true,
        enfermedad_conocida: false,
        medicacion: false,
        problema_musculoesqueletico: false,
        indicacion_supervisada: false,
        embarazo: false,
      },
      recientes: ['desmayo_o_mareo'],
    });
  });

  test('«seguir»: puedes continuar', async () => {
    await renderizar(<Cribado />, conPreguntas());
    await screen.findByText('Pregunta 1');
    await contestarTodas();
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('Puedes seguir')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Listo' }));
    expect(mockRouter.back).toHaveBeenCalled();
  });

  test('«consultar_profesional»: recomienda hablar con un profesional, enseña los motivos y no bloquea', async () => {
    const api = conPreguntas({
      cribar: jest.fn().mockResolvedValue({ nivel: 'consultar_profesional', motivos: ['Tomas medicación que condiciona el ejercicio.'] }),
    });
    await renderizar(<Cribado />, api);
    await screen.findByText('Pregunta 1');
    await contestarTodas();
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('Habla con un profesional antes de entrenar')).toBeOnTheScreen();
    expect(screen.getByText('Tomas medicación que condiciona el ejercicio.')).toBeOnTheScreen();
    expect(screen.getByText(/no te impide usar Forja/)).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Entendido' })).toBeEnabled();
    expect(textoVisible()).not.toMatch(PROHIBIDAS);
  });

  test('«parar_urgencias»: bloquea, pide urgencias hoy y no deja continuar', async () => {
    const api = conPreguntas({
      cribar: jest.fn().mockResolvedValue({ nivel: 'parar_urgencias', motivos: ['Has notado dolor u opresión al hacer esfuerzo hace poco.'] }),
    });
    await renderizar(<Cribado />, api);
    await screen.findByText('Pregunta 1');
    await contestarTodas(false);
    for (const n of [1, 2]) {
      await fireEvent.press(within(screen.getAllByLabelText('¿Fue hace poco?')[n - 1] as never).getByRole('radio', { name: 'Sí' }));
    }
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('No entrenes hoy y acude a urgencias')).toBeOnTheScreen();
    expect(screen.getByText('Has notado dolor u opresión al hacer esfuerzo hace poco.')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Entendido' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Listo' })).toBeNull();
    expect(screen.queryByRole('button', { name: /plan|rutina/i })).toBeNull();
  });

  test('«Volver a contestar» vacía las respuestas', async () => {
    await renderizar(<Cribado />, conPreguntas());
    await screen.findByText('Pregunta 1');
    await contestarTodas();
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));
    await fireEvent.press(await screen.findByRole('button', { name: 'Volver a contestar' }));

    expect(await screen.findByText('Pregunta 1')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Ver resultado' })).toBeDisabled();
  });

  test('si el servidor falla al cribar, enseña el error', async () => {
    const api = conPreguntas({ cribar: jest.fn().mockRejectedValue(new ErrorApi(403, 'No tienes permiso para hacer esto.')) });
    await renderizar(<Cribado />, api);
    await screen.findByText('Pregunta 1');
    await contestarTodas();
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('No tienes permiso para hacer esto.')).toBeOnTheScreen();
  });

  test('si no cargan las preguntas, error con reintento', async () => {
    const preguntasCribado = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue(cuestionarioDePrueba());
    await renderizar(<Cribado />, conConsentimiento({ preguntasCribado }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Pregunta 1')).toBeOnTheScreen();
  });

  test('sin consentimiento no enseña el cuestionario y lleva a Limitaciones', async () => {
    const api = apiFalsa({ preguntasCribado: jest.fn().mockResolvedValue(cuestionarioDePrueba()) });
    await renderizar(<Cribado />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Ir a Limitaciones' }));

    expect(mockRouter.replace).toHaveBeenCalledWith('/limitaciones');
    expect(screen.queryByText('Pregunta 1')).toBeNull();
  });
});

describe('Cuestionario de dolor', () => {
  const SENALES = [
    '¿Te duele aunque estés en reposo, sin moverte?',
    '¿Te despierta el dolor por la noche?',
    '¿Notas hormigueo o la zona dormida?',
    '¿Has notado una pérdida de fuerza nueva, o que va a más, en un brazo o una pierna?',
    '¿Has perdido el control de la vejiga o el intestino, o notas dormida la zona genital o anal?',
    '¿Tienes fiebre y, a la vez, dolor de espalda?',
    '¿Has tenido un golpe o una caída fuerte hace poco?',
    '¿Has perdido peso sin explicación, sin dieta ni cambios en tu vida?',
    '¿Notas dolor u opresión en el pecho?',
    '¿Te has mareado o desmayado?',
  ];

  async function contestarSenales(sies: readonly string[] = []) {
    for (const s of SENALES) {
      await elegir(s, sies.includes(s) ? 'Sí' : 'No');
    }
  }

  test('no deja ver el resultado hasta contestar las diez señales', async () => {
    await renderizar(<Dolor />, conConsentimiento());

    expect(await screen.findByRole('button', { name: 'Ver resultado' })).toBeDisabled();
    await contestarSenales();
    expect(screen.getByRole('button', { name: 'Ver resultado' })).toBeEnabled();
  });

  test('el dolor se pide solo de las zonas que marcas y viaja con las diez señales', async () => {
    const api = conConsentimiento();
    await renderizar(<Dolor />, api);
    await screen.findByRole('button', { name: 'Ver resultado' });

    expect(screen.queryByLabelText('Dolor en rodilla, de 0 a 10')).toBeNull();
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Rodilla' }));
    await elegir('Dolor en rodilla, de 0 a 10', '4');
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Hombro' }));
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Hombro' }));
    await contestarSenales([SENALES[1] as string]);
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    await waitFor(() => expect(api.evaluarBanderas).toHaveBeenCalledTimes(1));
    expect(api.evaluarBanderas).toHaveBeenCalledWith({
      dolorPorZona: { rodilla: 4 },
      dolorEnReposo: false,
      dolorNocturnoQueDespierta: true,
      hormigueoOEntumecimiento: false,
      perdidaDeFuerza: false,
      perdidaControlEsfinteresOAnestesiaSillaDeMontar: false,
      fiebreConDolorDeEspalda: false,
      traumatismoReciente: false,
      perdidaDePesoSinExplicacion: false,
      dolorToracico: false,
      mareoODesmayo: false,
    });
  });

  test('una zona marcada exige poner su dolor', async () => {
    await renderizar(<Dolor />, conConsentimiento());
    await screen.findByRole('button', { name: 'Ver resultado' });

    await fireEvent.press(screen.getByRole('checkbox', { name: 'Codo' }));
    await contestarSenales();

    expect(screen.getByRole('button', { name: 'Ver resultado' })).toBeDisabled();
  });

  test('sin banderas no bloquea, lo dice y «Volver a limitaciones» lleva al hub', async () => {
    await renderizar(<Dolor />, conConsentimiento());
    await screen.findByRole('button', { name: 'Ver resultado' });
    await contestarSenales();
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('Sin señales de aviso')).toBeOnTheScreen();
    expect(mockRouter.back).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Volver a limitaciones' }));
    expect(mockRouter.back).toHaveBeenCalled();
  });

  test('bloquea con urgencias_hoy: no entrenes hoy, motivo y guía, y ninguna salida hacia la rutina', async () => {
    const api = conConsentimiento({
      evaluarBanderas: jest.fn().mockResolvedValue({
        bloquea: true,
        banderas: [{ nivel: 'urgencias_hoy', motivo: 'Notas dolor u opresión en el pecho: no entrenes y acude hoy a urgencias.', guia: 'ACSM, cribado previo a la participación' }],
      }),
    });
    await renderizar(<Dolor />, api);
    await screen.findByRole('button', { name: 'Ver resultado' });
    await contestarSenales([SENALES[8] as string]);
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('No entrenes hoy y acude a urgencias')).toBeOnTheScreen();
    expect(screen.getByText('Notas dolor u opresión en el pecho: no entrenes y acude hoy a urgencias.')).toBeOnTheScreen();
    expect(screen.getByText('Guía: ACSM, cribado previo a la participación')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: /plan|rutina|seguir|continuar/i })).toBeNull();
    expect(mockRouter.back).not.toHaveBeenCalled();
    expect(textoVisible()).not.toMatch(PROHIBIDAS);
  });

  test('bloquea con profesional_en_dias: habla con un profesional en los próximos días', async () => {
    const api = conConsentimiento({
      evaluarBanderas: jest.fn().mockResolvedValue({
        bloquea: true,
        banderas: [{ nivel: 'profesional_en_dias', motivo: 'Te despierta el dolor por la noche.', guia: 'NICE NG59' }],
      }),
    });
    await renderizar(<Dolor />, api);
    await screen.findByRole('button', { name: 'Ver resultado' });
    await contestarSenales([SENALES[1] as string]);
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('Habla con un profesional en los próximos días')).toBeOnTheScreen();
    expect(screen.queryByText('No entrenes hoy y acude a urgencias')).toBeNull();
    expect(screen.getByText('Te despierta el dolor por la noche.')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: /plan|rutina|seguir|continuar/i })).toBeNull();
  });

  test('con urgencias y días a la vez manda lo más urgente', async () => {
    const api = conConsentimiento({
      evaluarBanderas: jest.fn().mockResolvedValue({
        bloquea: true,
        banderas: [
          { nivel: 'profesional_en_dias', motivo: 'Motivo B.', guia: 'Guía B' },
          { nivel: 'urgencias_hoy', motivo: 'Motivo A.', guia: 'Guía A' },
        ],
      }),
    });
    await renderizar(<Dolor />, api);
    await screen.findByRole('button', { name: 'Ver resultado' });
    await contestarSenales();
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('No entrenes hoy y acude a urgencias')).toBeOnTheScreen();
    expect(screen.getByText('Motivo B.')).toBeOnTheScreen();
  });

  test('si falla el servidor, enseña el error y se puede reintentar', async () => {
    const evaluarBanderas = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue({ bloquea: false, banderas: [] });
    await renderizar(<Dolor />, conConsentimiento({ evaluarBanderas }));
    await screen.findByRole('button', { name: 'Ver resultado' });
    await contestarSenales();
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Ver resultado' }));
    expect(await screen.findByText('Sin señales de aviso')).toBeOnTheScreen();
  });

  test('el texto de las preguntas es de fitness, sin palabras clínicas ni prohibidas', async () => {
    await renderizar(<Dolor />, conConsentimiento());
    await screen.findByRole('button', { name: 'Ver resultado' });

    expect(textoVisible()).not.toMatch(PROHIBIDAS);
  });
});

describe('Formulario de limitación', () => {
  const alta = () => conConsentimiento({ restricciones: jest.fn().mockResolvedValue([RODILLA, HOMBRO]) });

  test('crear sin rellenar enseña los errores y no llama a la API', async () => {
    const api = alta();
    await renderizar(<FormularioLimitacion />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Error: Elige una región o un patrón de movimiento.')).toBeOnTheScreen();
    expect(screen.getByText('Error: Elige el lado.')).toBeOnTheScreen();
    expect(screen.getByText('Error: Elige qué te limita.')).toBeOnTheScreen();
    expect(api.crearRestriccion).not.toHaveBeenCalled();
  });

  test('crear con región, lado y tipo manda los códigos y vuelve atrás', async () => {
    const api = alta();
    await renderizar(<FormularioLimitacion />, api);
    await screen.findByRole('button', { name: 'Guardar' });

    await elegir('Región', 'Rodilla');
    await elegir('Lado', 'Izquierdo');
    await elegir('Qué te limita', 'Sin impacto');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(api.crearRestriccion).toHaveBeenCalledTimes(1));
    expect(api.crearRestriccion).toHaveBeenCalledWith({
      region: 'rodilla',
      lado: 'izquierdo',
      tipo: 'sin_impacto',
      estado: 'actual',
      origen: 'usuario',
    });
    await waitFor(() => expect(mockRouter.back).toHaveBeenCalled());
  });

  test('con patrón en vez de región también vale y «profesional» pasa a origen', async () => {
    const api = alta();
    await renderizar(<FormularioLimitacion />, api);
    await screen.findByRole('button', { name: 'Guardar' });

    await elegir('Patrón de movimiento', 'Sentadilla');
    await elegir('Lado', 'Ambos');
    await elegir('Qué te limita', 'Evitar');
    await elegir('Cómo está ahora', 'En los últimos 12 meses');
    await elegir('¿Te la indicó un profesional?', 'Sí');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(api.crearRestriccion).toHaveBeenCalledWith({
      patron: 'sentadilla',
      lado: 'ambos',
      tipo: 'evitar',
      estado: 'ultimos_12_meses',
      origen: 'profesional',
    }));
  });

  test('el parámetro solo se pide con «limitar carga» o «tempo controlado» y se valida', async () => {
    const api = alta();
    await renderizar(<FormularioLimitacion />, api);
    await screen.findByRole('button', { name: 'Guardar' });

    expect(screen.queryByLabelText('Parámetro')).toBeNull();
    await elegir('Región', 'Hombro');
    await elegir('Lado', 'Derecho');
    await elegir('Qué te limita', 'Limitar carga');
    await fireEvent.changeText(screen.getByLabelText('Parámetro'), '20 kg');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Error: Escribe una categoría o un RPE, sin kilos ni grados.')).toBeOnTheScreen();
    expect(api.crearRestriccion).not.toHaveBeenCalled();
    await fireEvent.changeText(screen.getByLabelText('Parámetro'), 'RPE 6');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(api.crearRestriccion).toHaveBeenCalledWith(expect.objectContaining({ tipo: 'limitar_carga', parametro: 'RPE 6' })));
  });

  test('el error del servidor se enseña con su mensaje y no se vuelve atrás', async () => {
    const api = conConsentimiento({
      restricciones: jest.fn().mockResolvedValue([]),
      crearRestriccion: jest.fn().mockRejectedValue(new ErrorApi(409, 'Has llegado al máximo de 30 limitaciones.')),
    });
    await renderizar(<FormularioLimitacion />, api);
    await screen.findByRole('button', { name: 'Guardar' });
    await elegir('Región', 'Rodilla');
    await elegir('Lado', 'Izquierdo');
    await elegir('Qué te limita', 'Evitar');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Has llegado al máximo de 30 limitaciones.')).toBeOnTheScreen();
    expect(mockRouter.back).not.toHaveBeenCalled();
  });

  test('editar carga los valores, y guardar hace un PUT con lo cambiado', async () => {
    const api = alta();
    await renderizar(<FormularioLimitacion id={RODILLA.uuid} />, api);

    await screen.findByRole('button', { name: 'Guardar' });
    expect(within(screen.getByLabelText('Región')).getByRole('radio', { name: 'Rodilla' })).toBeSelected();
    expect(within(screen.getByLabelText('Qué te limita')).getByRole('radio', { name: 'Sin impacto' })).toBeSelected();
    await elegir('Qué te limita', 'Evitar');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(api.actualizarRestriccion).toHaveBeenCalledWith(RODILLA.uuid, {
      region: 'rodilla',
      lado: 'izquierdo',
      tipo: 'evitar',
      estado: 'actual',
      origen: 'usuario',
    }));
    expect(api.crearRestriccion).not.toHaveBeenCalled();
  });

  test('editar y borrar pide confirmación y luego borra', async () => {
    const api = alta();
    await renderizar(<FormularioLimitacion id={HOMBRO.uuid} />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Borrar esta limitación' }));
    expect(api.borrarRestriccion).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, borrar' }));

    await waitFor(() => expect(api.borrarRestriccion).toHaveBeenCalledWith(HOMBRO.uuid));
    await waitFor(() => expect(mockRouter.back).toHaveBeenCalled());
  });

  test('si la limitación ya no existe, lo dice', async () => {
    await renderizar(<FormularioLimitacion id="0193a3c4-7b2e-7c1d-9f00-ffffffffffff" />, alta());

    expect(await screen.findByText('Esta limitación ya no existe.')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Guardar' })).toBeNull();
  });

  test('sin consentimiento no enseña el formulario', async () => {
    await renderizar(<FormularioLimitacion />, apiFalsa());

    expect(await screen.findByRole('button', { name: 'Ir a Limitaciones' })).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Guardar' })).toBeNull();
  });

  test('los textos del formulario no usan palabras prohibidas', async () => {
    await renderizar(<FormularioLimitacion />, alta());
    await screen.findByRole('button', { name: 'Guardar' });

    expect(textoVisible()).not.toMatch(PROHIBIDAS);
  });
});

describe('Qué cambia en mis ejercicios', () => {
  test('agrupa por decisión: excluidos, sustituidos con «X → Y» y su motivo, y con avisos', async () => {
    await renderizar(<Efecto />, conConsentimiento({ efectoRestricciones: jest.fn().mockResolvedValue(efectoDePrueba()) }));

    expect(await screen.findByText('Excluidos')).toBeOnTheScreen();
    expect(screen.getByText('Sentadilla con barra')).toBeOnTheScreen();
    expect(screen.getByText('Sin carga axial en la rodilla.')).toBeOnTheScreen();
    expect(screen.getByText('Sustituidos')).toBeOnTheScreen();
    expect(screen.getByText('Zancada con barra → Prensa de piernas')).toBeOnTheScreen();
    expect(screen.getByText('Sin impacto en la rodilla.')).toBeOnTheScreen();
    expect(screen.getByText('Con avisos')).toBeOnTheScreen();
    expect(screen.getByText('Extensión de cuádriceps')).toBeOnTheScreen();
    expect(screen.getByText('Limitar carga: ligera')).toBeOnTheScreen();
    expect(textoVisible()).not.toMatch(PROHIBIDAS);
  });

  test('no enseña un grupo que no tiene ejercicios', async () => {
    const soloExcluido = efectoDePrueba().slice(0, 1);
    await renderizar(<Efecto />, conConsentimiento({ efectoRestricciones: jest.fn().mockResolvedValue(soloExcluido) }));

    expect(await screen.findByText('Excluidos')).toBeOnTheScreen();
    expect(screen.queryByText('Sustituidos')).toBeNull();
    expect(screen.queryByText('Con avisos')).toBeNull();
  });

  test('sin cambios, estado vacío amable', async () => {
    await renderizar(<Efecto />, conConsentimiento());

    expect(await screen.findByText('Con tus limitaciones actuales no cambia ningún ejercicio.')).toBeOnTheScreen();
  });

  test('si falla, error con reintento', async () => {
    const efectoRestricciones = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue(efectoDePrueba());
    await renderizar(<Efecto />, conConsentimiento({ efectoRestricciones }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Excluidos')).toBeOnTheScreen();
  });

  test('avisa de que el plan actual no cambia solo', async () => {
    await renderizar(<Efecto />, conConsentimiento());

    expect(await screen.findByText(/Tu plan actual no se modifica solo/)).toBeOnTheScreen();
  });

  test('sin consentimiento no lo pide y lleva a Limitaciones', async () => {
    const api = apiFalsa();
    await renderizar(<Efecto />, api);

    expect(await screen.findByRole('button', { name: 'Ir a Limitaciones' })).toBeOnTheScreen();
    expect(api.efectoRestricciones).not.toHaveBeenCalled();
  });
});
