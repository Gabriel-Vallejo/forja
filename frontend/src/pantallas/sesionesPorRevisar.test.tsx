import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { ErrorApi } from '../api/errores';
import type { AlmacenRegistro } from '../registro/almacen';
import { crearAlmacenMemoria } from '../registro/almacenMemoria';
import { crearOutbox } from '../registro/outbox';
import { apiFalsa, planDePrueba, USUARIO_ID } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { SIN_REGISTRO_LOCAL } from './ConOutbox';
import { AVISO_CUENTA_ELIMINADA, SesionesPorRevisar } from './SesionesPorRevisar';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn(), dismissTo: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

const MOTIVO = 'Revisa esta sesión: la serie 2 no es válida.';
const conPlan = (cambios = {}) => apiFalsa({ planActual: jest.fn().mockResolvedValue(planDePrueba()), ...cambios });

/** Una sesión cerrada del usuario de prueba; con `estado`, se deja así en el almacén. */
async function sesionCerrada(almacen: AlmacenRegistro, estado?: 'rechazada') {
  const outbox = crearOutbox({ almacen, usuarioActual: async () => USUARIO_ID });
  const { uuid } = await outbox.empezarSesion({ diaPlan: 1 });
  await outbox.registrarSerie(uuid, { ejercicio: 'press_banca', orden: 1, pesoKg: 80, repeticiones: 10, rir: 2 });
  await outbox.cerrarSesion(uuid);
  if (estado) await almacen.actualizarSesion(uuid, { estado, ultimoError: MOTIVO });
  return uuid;
}

beforeEach(() => jest.clearAllMocks());

describe('Sesiones por revisar', () => {
  test('sin outbox lo explica', async () => {
    await renderizar(<SesionesPorRevisar />, conPlan());

    expect(await screen.findByText(SIN_REGISTRO_LOCAL)).toBeOnTheScreen();
  });

  test('sin rechazadas lo dice', async () => {
    await renderizar(<SesionesPorRevisar />, conPlan(), crearAlmacenMemoria());

    expect(await screen.findByText('No tienes sesiones por revisar.')).toBeOnTheScreen();
  });

  test('enseña cada rechazada con el mensaje del servidor y sus series', async () => {
    const almacen = crearAlmacenMemoria();
    await sesionCerrada(almacen, 'rechazada');

    await renderizar(<SesionesPorRevisar />, conPlan(), almacen);

    expect(await screen.findByText(MOTIVO)).toBeOnTheScreen();
    expect(await screen.findByText('Press de banca · 80 kg × 10 · RIR 2')).toBeOnTheScreen();
  });

  test('reintentar la vuelve a enviar y, si el servidor la acepta, sale de la lista', async () => {
    const almacen = crearAlmacenMemoria();
    const uuid = await sesionCerrada(almacen, 'rechazada');
    const api = conPlan();
    await renderizar(<SesionesPorRevisar />, api, almacen);

    await fireEvent.press(await screen.findByRole('button', { name: /^Reintentar el envío de la sesión del/ }));

    expect(await screen.findByText('No tienes sesiones por revisar.')).toBeOnTheScreen();
    expect(api.guardarSesion).toHaveBeenCalledWith(uuid, expect.anything());
    expect((await almacen.leerSesion(uuid))!.estado).toBe('confirmada');
  });

  test('descartar pide confirmación y borra solo esa sesión', async () => {
    const almacen = crearAlmacenMemoria();
    const rechazada = await sesionCerrada(almacen, 'rechazada');
    const otra = await sesionCerrada(almacen);
    await renderizar(<SesionesPorRevisar />, conPlan({ guardarSesion: jest.fn().mockRejectedValue(new ErrorApi(0, 'sin red')) }), almacen);

    await fireEvent.press(await screen.findByRole('button', { name: /^Descartar la sesión del/ }));
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, descartar' }));

    expect(await screen.findByText('No tienes sesiones por revisar.')).toBeOnTheScreen();
    expect(await almacen.leerSesion(rechazada)).toBeNull();
    expect(await almacen.leerSesion(otra)).not.toBeNull();
  });

  test('si la cuenta ya no existe, avisa', async () => {
    const almacen = crearAlmacenMemoria();
    await sesionCerrada(almacen);

    await renderizar(<SesionesPorRevisar />, conPlan({ guardarSesion: jest.fn().mockRejectedValue(new ErrorApi(410, 'ya no existe')) }), almacen);

    expect(await screen.findByText(AVISO_CUENTA_ELIMINADA)).toBeOnTheScreen();
  });

  test('si la base local falla al leer, lo dice y deja reintentar', async () => {
    const almacen = crearAlmacenMemoria();
    await sesionCerrada(almacen, 'rechazada');
    const listar = almacen.listarSesiones.bind(almacen);
    let fallar = true;
    almacen.listarSesiones = async (filtro) => {
      if (fallar) throw new Error('SQLITE_IOERR');
      return listar(filtro);
    };
    await renderizar(<SesionesPorRevisar />, conPlan(), almacen);

    expect(await screen.findByText('No se ha podido usar el registro del móvil. Inténtalo otra vez.')).toBeOnTheScreen();
    fallar = false;
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    await waitFor(() => expect(screen.getByText(MOTIVO)).toBeOnTheScreen());
  });
});
