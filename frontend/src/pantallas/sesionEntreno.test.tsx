import { ScrollView, Vibration } from 'react-native';
import { act, fireEvent, screen, waitFor } from '@testing-library/react-native';

import { ErrorApi } from '../api/errores';
import type { AlmacenRegistro } from '../registro/almacen';
import { crearAlmacenMemoria } from '../registro/almacenMemoria';
import { crearOutbox } from '../registro/outbox';
import { apiFalsa, objetivo, planDePrueba, USUARIO_ID } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { SIN_REGISTRO_LOCAL } from './ConOutbox';
import { ESPERA_RED_MS, SIN_PLAN_SIN_RED } from './ConPlanParaEntrenar';
import { DESHACER_MS, SesionEntreno } from './SesionEntreno';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn(), dismissTo: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

const PLAN = planDePrueba();
const conPlan = (cambios = {}) => apiFalsa({ planActual: jest.fn().mockResolvedValue(PLAN), ...cambios });
const outboxDe = (almacen: AlmacenRegistro) => crearOutbox({ almacen, usuarioActual: async () => USUARIO_ID });
const PRESS = 'Press de banca, serie 1';

async function abrir(almacen: AlmacenRegistro = crearAlmacenMemoria(), api = conPlan()) {
  await renderizar(<SesionEntreno />, api, almacen);
  await screen.findByRole('button', { name: `Guardar serie, ${PRESS}` });
  return almacen;
}

const guardar = (contexto = PRESS) => fireEvent.press(screen.getByRole('button', { name: `Guardar serie, ${contexto}` }));

beforeEach(() => jest.clearAllMocks());

describe('Sesión en curso', () => {
  test('en Expo Go o en el navegador (sin outbox) lo explica y deja volver', async () => {
    await renderizar(<SesionEntreno />, conPlan());

    expect(await screen.findByText(SIN_REGISTRO_LOCAL)).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Volver atrás' }));
    expect(mockRouter.back).toHaveBeenCalled();
  });

  test('sin sesión en curso empieza una del día de hoy con el plan y enseña el objetivo de cada ejercicio', async () => {
    const almacen = await abrir();

    expect(screen.getByText('Día 1 · TORSO')).toBeOnTheScreen();
    expect(screen.getByText('4 × 6-10 · RIR 2 · 80 kg')).toBeOnTheScreen();
    expect(screen.getByText('4 × 6-10 · RIR 2 · tu peso')).toBeOnTheScreen();
    const [sesion, ...otras] = await almacen.listarSesiones();
    expect(otras).toEqual([]);
    expect(sesion).toMatchObject({ estado: 'en_curso', diaPlan: 1, planVersion: PLAN.uuid, usuario: USUARIO_ID });
  });

  test('guardar serie usa los valores del objetivo, se pueden cambiar con −/+ y arranca el descanso', async () => {
    const almacen = await abrir();
    expect(screen.getByLabelText(`Peso, ${PRESS}`).props.value).toBe('80');
    expect(screen.getByLabelText(`Repeticiones, ${PRESS}`).props.value).toBe('10');
    expect(screen.getByLabelText(`RIR, ${PRESS}`).props.value).toBe('2');

    await fireEvent.press(screen.getByRole('button', { name: `Subir peso, ${PRESS}` }));
    await fireEvent.press(screen.getByRole('button', { name: `Bajar repeticiones, ${PRESS}` }));
    await guardar();

    expect(await screen.findByText('82,5 kg × 9 · RIR 2')).toBeOnTheScreen();
    const [sesion] = await almacen.listarSesiones();
    expect(await almacen.listarSeries(sesion!.uuid)).toEqual([
      expect.objectContaining({ ejercicio: 'press_banca', orden: 1, pesoKg: 82.5, repeticiones: 9, rir: 2, calentamiento: false, dolor: null }),
    ]);
    // La siguiente serie se precarga con la última guardada.
    expect(screen.getByLabelText('Peso, Press de banca, serie 2').props.value).toBe('82,5');
    expect(screen.getByText('2:00')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: '+30 s' }));
    expect(screen.getByText('2:30')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Saltar' }));
    expect(screen.queryByText('Descanso')).toBeNull();
  });

  test('el peso también se teclea; si no es un número no guarda y lo dice', async () => {
    const almacen = await abrir();

    await fireEvent.changeText(screen.getByLabelText(`Peso, ${PRESS}`), 'ochenta');
    await guardar();

    expect(await screen.findByText('Escribe peso, repeticiones y RIR con números.')).toBeOnTheScreen();
    const [sesion] = await almacen.listarSesiones();
    expect(await almacen.listarSeries(sesion!.uuid)).toEqual([]);

    await fireEvent.changeText(screen.getByLabelText(`Peso, ${PRESS}`), '77,5');
    await guardar();
    expect(await screen.findByText('77,5 kg × 10 · RIR 2')).toBeOnTheScreen();
  });

  test('una serie de calentamiento con dolor: se guarda marcada y no cuenta como serie de trabajo', async () => {
    const almacen = await abrir();

    await fireEvent.press(screen.getByRole('checkbox', { name: 'Calentamiento' }));
    await fireEvent.press(screen.getByRole('checkbox', { name: '¿Te ha dolido?' }));
    await fireEvent.press(screen.getByRole('radio', { name: '3' }));
    await guardar();

    expect(await screen.findByText('Calentamiento hecha')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: `Guardar serie, ${PRESS}` })).toBeOnTheScreen();
    const [sesion] = await almacen.listarSesiones();
    expect(await almacen.listarSeries(sesion!.uuid)).toEqual([expect.objectContaining({ calentamiento: true, dolor: 3 })]);

    await fireEvent.press(screen.getByRole('button', { name: 'Terminar sesión' }));
    expect(screen.getByText('¿Terminar la sesión? Llevas 0 series de trabajo y 1 de calentamiento.')).toBeOnTheScreen();
  });

  test('al abrir la confirmación de terminar, la pantalla baja hasta ella', async () => {
    const bajar = jest.spyOn(ScrollView.prototype, 'scrollToEnd');
    await abrir();
    await guardar();
    await screen.findByText('Serie 1 hecha');
    const desplazable = screen.getByTestId('pantalla-desplazable');

    fireEvent(desplazable, 'contentSizeChange', 400, 2000);
    expect(bajar).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Terminar sesión' }));
    fireEvent(desplazable, 'contentSizeChange', 400, 2200);
    expect(bajar).toHaveBeenCalled();
    bajar.mockRestore();
  });

  test('retoma la sesión en curso con sus series aunque se cerrara la app', async () => {
    const almacen = crearAlmacenMemoria();
    const outbox = outboxDe(almacen);
    const { uuid } = await outbox.empezarSesion({ diaPlan: 2, planVersion: PLAN.uuid });
    await outbox.registrarSerie(uuid, { ejercicio: 'sentadilla', orden: 1, pesoKg: 100, repeticiones: 8, rir: 2 });

    await renderizar(<SesionEntreno />, conPlan(), almacen);

    expect(await screen.findByText('100 kg × 8 · RIR 2')).toBeOnTheScreen();
    expect(screen.getByText('Día 2 · PIERNA')).toBeOnTheScreen();
    expect(await almacen.listarSesiones()).toHaveLength(1);
  });

  test('con una carga siguiente calculada sin red, el objetivo y la fila la usan', async () => {
    const almacen = crearAlmacenMemoria();
    const outbox = outboxDe(almacen);
    const previa = await outbox.empezarSesion({ diaPlan: 1, planVersion: PLAN.uuid });
    for (let i = 1; i <= 4; i++) await outbox.registrarSerie(previa.uuid, { ejercicio: 'press_banca', orden: i, pesoKg: 80, repeticiones: 10, rir: 2 });
    await outbox.cerrarSesion(previa.uuid, [objetivo()]);

    await abrir(almacen, conPlan({ guardarSesion: jest.fn().mockRejectedValue(new ErrorApi(0, 'sin red')) }));

    expect(await screen.findByText('4 × 6-10 · RIR 2 · 82,5 kg')).toBeOnTheScreen();
    expect(screen.getByLabelText(`Peso, ${PRESS}`).props.value).toBe('82,5');
  });

  test('corregir una serie añade una versión nueva y la lista enseña la última', async () => {
    const almacen = await abrir();
    await guardar();
    await fireEvent.press(await screen.findByRole('button', { name: 'Corregir serie 1 de Press de banca' }));

    await fireEvent.changeText(screen.getByLabelText('Repeticiones, Press de banca, serie 1 (corrección)'), '7');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar corrección, Press de banca, serie 1 (corrección)' }));

    expect(await screen.findByText('80 kg × 7 · RIR 2')).toBeOnTheScreen();
    expect(screen.queryByText('80 kg × 10 · RIR 2')).toBeNull();
    const [sesion] = await almacen.listarSesiones();
    const [original, correccion] = await almacen.listarSeries(sesion!.uuid);
    expect(correccion).toMatchObject({ corrigeA: original!.uuid, repeticiones: 7, orden: 1 });
  });

  test('anular una serie pide confirmación y la quita de la lista sin borrar nada', async () => {
    const almacen = await abrir();
    await guardar();
    await fireEvent.press(await screen.findByRole('button', { name: 'Anular serie 1 de Press de banca' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Cancelar' }));
    expect(screen.getByText('80 kg × 10 · RIR 2')).toBeOnTheScreen();

    await fireEvent.press(screen.getByRole('button', { name: 'Anular serie 1 de Press de banca' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, anular' }));

    await waitFor(() => expect(screen.queryByText('80 kg × 10 · RIR 2')).toBeNull());
    const [sesion] = await almacen.listarSesiones();
    expect(await almacen.listarSeries(sesion!.uuid)).toEqual([expect.anything(), expect.objectContaining({ anulada: true })]);
  });

  test('al guardar vibra y durante 5 s se puede deshacer sin confirmar; deshacer para el descanso', async () => {
    const vibrar = jest.spyOn(Vibration, 'vibrate').mockImplementation(() => {});
    const almacen = await abrir();
    jest.useFakeTimers({ doNotFake: ['nextTick', 'setImmediate', 'queueMicrotask'], advanceTimers: true });
    try {
      await guardar();
      await fireEvent.press(await screen.findByRole('button', { name: 'Deshacer serie 1 de Press de banca' }));

      await waitFor(() => expect(screen.queryByText('80 kg × 10 · RIR 2')).toBeNull());
      expect(vibrar).toHaveBeenCalledTimes(1);
      expect(screen.queryByText('Descanso')).toBeNull();
      const [sesion] = await almacen.listarSesiones();
      expect(await almacen.listarSeries(sesion!.uuid)).toEqual([expect.anything(), expect.objectContaining({ anulada: true })]);

      await guardar();
      await screen.findByText('Serie 1 hecha');
      await act(async () => jest.advanceTimersByTime(DESHACER_MS));
      expect(screen.queryByRole('button', { name: 'Deshacer serie 1 de Press de banca' })).toBeNull();
    } finally {
      jest.useRealTimers();
      vibrar.mockRestore();
    }
  });

  test('el RPE es opcional y se guarda con la serie', async () => {
    const almacen = await abrir();

    await fireEvent.press(screen.getByRole('checkbox', { name: 'Anotar RPE' }));
    await fireEvent.press(screen.getByRole('radio', { name: 'RPE 8,5' }));
    await guardar();

    expect(await screen.findByText('80 kg × 10 · RIR 2 · RPE 8,5')).toBeOnTheScreen();
    const [sesion] = await almacen.listarSesiones();
    expect(await almacen.listarSeries(sesion!.uuid)).toEqual([expect.objectContaining({ rpe: 8.5 })]);
  });

  test('con dolor 7 o más avisa antes de guardar y, al guardar, para el registro hasta pulsar «Entendido»', async () => {
    await abrir();

    await fireEvent.press(screen.getByRole('checkbox', { name: '¿Te ha dolido?' }));
    await fireEvent.press(screen.getByRole('radio', { name: '6' }));
    expect(screen.queryByText(/Dolor alto/)).toBeNull();
    await fireEvent.press(screen.getByRole('radio', { name: '8' }));
    expect(screen.getByText(/Dolor alto/)).toBeOnTheScreen();
    await guardar();

    expect(await screen.findByText('Para y consulta')).toBeOnTheScreen();
    expect(screen.getByText('80 kg × 10 · RIR 2 · dolor 8')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Guardar serie, Press de banca, serie 2' })).toBeNull();
    await fireEvent.press(screen.getByRole('button', { name: 'Entendido' }));

    expect(screen.queryByText('Para y consulta')).toBeNull();
    expect(screen.getByRole('button', { name: 'Guardar serie, Press de banca, serie 2' })).toBeOnTheScreen();
  });

  test('terminar con confirmación cierra la sesión, la envía y enseña el resumen', async () => {
    const api = conPlan();
    const almacen = await abrir(crearAlmacenMemoria(), api);
    await guardar();
    await screen.findByText('Serie 1 hecha');

    await fireEvent.press(screen.getByRole('button', { name: 'Terminar sesión' }));
    expect(screen.getByText('¿Terminar la sesión? Llevas 1 serie.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, terminar' }));

    expect(await screen.findByText('Sesión terminada')).toBeOnTheScreen();
    expect(screen.getByText('1 serie · 1 ejercicio')).toBeOnTheScreen();
    expect(await screen.findByText('Enviada.')).toBeOnTheScreen();
    expect(api.guardarSesion).toHaveBeenCalledTimes(1);
    expect((await almacen.listarSesiones())[0]!.estado).toBe('confirmada');
    await fireEvent.press(screen.getByRole('button', { name: 'Volver a Hoy' }));
    expect(mockRouter.dismissTo).toHaveBeenCalledWith('/hoy');
  });

  test('sin conexión, el resumen dice que queda guardada en el móvil', async () => {
    await abrir(crearAlmacenMemoria(), conPlan({ guardarSesion: jest.fn().mockRejectedValue(new ErrorApi(0, 'sin red')) }));
    await guardar();
    await screen.findByText('Serie 1 hecha');

    await fireEvent.press(screen.getByRole('button', { name: 'Terminar sesión' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, terminar' }));

    expect(await screen.findByText('Guardada en el móvil. Se envía sola en cuanto haya conexión.')).toBeOnTheScreen();
  });

  test('una sesión sin series no se termina: se ofrece descartarla', async () => {
    const almacen = await abrir();

    await fireEvent.press(screen.getByRole('button', { name: 'Terminar sesión' }));
    expect(screen.getByText('No has guardado ninguna serie: no hay nada que terminar. ¿Descartas la sesión?')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, descartar' }));

    await waitFor(() => expect(mockRouter.dismissTo).toHaveBeenCalledWith('/hoy'));
    expect(await almacen.listarSesiones()).toEqual([]);
  });

  test('con las series previstas hechas se puede añadir una extra', async () => {
    const almacen = crearAlmacenMemoria();
    const outbox = outboxDe(almacen);
    const { uuid } = await outbox.empezarSesion({ diaPlan: 1, planVersion: PLAN.uuid });
    for (let i = 1; i <= 4; i++) await outbox.registrarSerie(uuid, { ejercicio: 'press_banca', orden: i, pesoKg: 80, repeticiones: 10, rir: 2 });
    await renderizar(<SesionEntreno />, conPlan(), almacen);

    await fireEvent.press(await screen.findByRole('button', { name: 'Añadir serie extra de Press de banca' }));
    await guardar('Press de banca, serie extra');

    expect(await screen.findByText('Serie 5 hecha')).toBeOnTheScreen();
    expect((await outbox.seriesValidas(uuid)).at(-1)).toMatchObject({ orden: 5 });
  });
});

describe('Sesión sin conexión con el plan guardado', () => {
  const sinRed = () => apiFalsa({ planActual: jest.fn().mockRejectedValue(new ErrorApi(0, 'No hay conexión con el servidor.')), guardarSesion: jest.fn().mockRejectedValue(new ErrorApi(0, 'sin red')) });

  test('cada plan que carga bien queda guardado en el móvil', async () => {
    const almacen = await abrir();

    await waitFor(async () => expect(await outboxDe(almacen).planGuardado()).toMatchObject({ plan: { uuid: PLAN.uuid } }));
  });

  test('si la red falla, abre la sesión con el plan guardado, lo dice y deja registrar', async () => {
    const almacen = crearAlmacenMemoria();
    await outboxDe(almacen).guardarPlan(PLAN);

    await abrir(almacen, sinRed());

    expect(screen.getByText(/^Sin conexión: usas el plan guardado el \d+ \w+\.$/)).toBeOnTheScreen();
    await guardar();
    expect(await screen.findByText('Serie 1 hecha')).toBeOnTheScreen();
    const [sesion] = await almacen.listarSesiones();
    expect(sesion).toMatchObject({ planVersion: PLAN.uuid, diaPlan: 1 });
  });

  test('si la red tarda, no espera: pasado el margen usa el plan guardado', async () => {
    // Solo se falsean los temporizadores: con nextTick, setImmediate y queueMicrotask falsos, en el runner de GitHub
    // (más lento) un await de React o del almacén se quedaba esperando para siempre y el CI se cancelaba a las 6 h.
    jest.useFakeTimers({ doNotFake: ['nextTick', 'setImmediate', 'queueMicrotask'], advanceTimers: true });
    try {
      const almacen = crearAlmacenMemoria();
      await outboxDe(almacen).guardarPlan(PLAN);
      await renderizar(<SesionEntreno />, conPlan({ planActual: jest.fn(() => new Promise(() => undefined)) }), almacen);
      expect(await screen.findByText('Cargando tu plan…')).toBeOnTheScreen();

      await act(async () => jest.advanceTimersByTime(ESPERA_RED_MS));

      expect(await screen.findByRole('button', { name: `Guardar serie, ${PRESS}` })).toBeOnTheScreen();
    } finally {
      jest.useRealTimers();
    }
  });

  test('sin red y sin plan guardado lo explica, sin pantalla rota', async () => {
    await renderizar(<SesionEntreno />, sinRed(), crearAlmacenMemoria());

    expect(await screen.findByText(SIN_PLAN_SIN_RED)).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeOnTheScreen();
  });

  test('primera semana: sin carga conocida dice «elige carga», no «0 kg», y el peso va vacío', async () => {
    const primera = planDePrueba(true);
    primera.dias[0]!.ejercicios[0] = { ...primera.dias[0]!.ejercicios[0]!, cargaKg: undefined };
    await abrir(crearAlmacenMemoria(), conPlan({ planActual: jest.fn().mockResolvedValue(primera) }));

    expect(screen.getAllByText('elige carga × 6-10 · RIR 2')).toHaveLength(3);
    expect(screen.queryByText(/^0 kg/)).toBeNull();
    const peso = screen.getByLabelText(`Peso, ${PRESS}`);
    expect(peso.props.value).toBe('');
    expect(peso.props.placeholder).toBe('elige');
    await guardar();
    expect(await screen.findByText('Falta el peso: escribe con cuántos kg haces esta serie.')).toBeOnTheScreen();
  });
});
