import { fireEvent, screen, waitFor, within } from '@testing-library/react-native';
import { Text } from 'react-native';

import { ErrorApi } from '../api/errores';
import { useResumenLimitaciones } from '../limitaciones/consultas';
import { apiFalsa, consentimiento, dispositivoCuenta, restriccion, USUARIO_ID } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { crearAlmacenMemoria } from '../registro/almacenMemoria';
import type { SesionLocal } from '../registro/almacen';
import { useSesion } from '../sesion/sesion';
import { BorrarCuenta } from './BorrarCuenta';
import { Perfil } from './Perfil';
import { Privacidad } from './Privacidad';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

beforeEach(() => jest.clearAllMocks());

/** Palabras que la finalidad de fitness (ADR 0007) prohíbe en cualquier texto de la app. */
const PROHIBIDAS = /rehabilit|alivi|corrige|previene lesion|desequilibr|atrofia|seguro para tu lesi|tratamiento|diagn[oó]stic/i;
const textoVisible = () => JSON.stringify(screen.toJSON());

const OTRO = '0193a3c4-7b2e-7c1d-9f00-5a1b2c3d4e60';
const PC = dispositivoCuenta({ id: OTRO, nombre: 'PC de casa', plataforma: 'pc', actual: false, ultimaActividad: '2026-11-10T12:00:00Z' });

/** Consentimientos que cambian al retirar uno: la lista que vuelve a pedir ya no lo trae. */
function consentimientosVivos(inicial: string[]) {
  let vivos = inicial;
  return {
    consentimientos: jest.fn(async () => vivos.map((t) => consentimiento(t))),
    retirarConsentimiento: jest.fn(async (tipo: string) => {
      vivos = vivos.filter((t) => t !== tipo);
    }),
  };
}

const tarjeta = (nombre: string) => within(screen.getByLabelText(`Consentimiento: ${nombre}`));

describe('Privacidad: consentimientos', () => {
  test('lista los cinco tipos: el dado con su fecha, los demás sin darse', async () => {
    await renderizar(<Privacidad />, apiFalsa({ consentimientos: jest.fn().mockResolvedValue([consentimiento('limitaciones')]) }));

    expect(await screen.findByLabelText('Consentimiento: Limitaciones')).toBeOnTheScreen();
    for (const nombre of ['Fotos de cuerpo', 'Medidas y peso', 'Lesiones', 'Limitaciones', 'Suplementos']) {
      expect(screen.getByLabelText(`Consentimiento: ${nombre}`)).toBeOnTheScreen();
    }
    expect(tarjeta('Limitaciones').getByText('Dado el 17 de noviembre de 2026')).toBeOnTheScreen();
    expect(tarjeta('Limitaciones').getByText(/limitaciones funcionales/i)).toBeOnTheScreen();
    expect(tarjeta('Suplementos').getByText('Sin usar todavía')).toBeOnTheScreen();
  });

  test('los que aún no tienen función se enseñan «sin usar todavía», sin botón de darlos', async () => {
    await renderizar(<Privacidad />);
    await screen.findByLabelText('Consentimiento: Suplementos');

    for (const nombre of ['Fotos de cuerpo', 'Medidas y peso', 'Lesiones', 'Suplementos']) {
      expect(tarjeta(nombre).getByText('Sin usar todavía')).toBeOnTheScreen();
      expect(tarjeta(nombre).queryByRole('button')).toBeNull();
    }
  });

  test('limitaciones sin dar enlaza a su pantalla de consentimiento', async () => {
    await renderizar(<Privacidad />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Dar consentimiento de limitaciones' }));

    expect(tarjeta('Limitaciones').getByText('No lo has dado')).toBeOnTheScreen();
    expect(mockRouter.push).toHaveBeenCalledWith('/limitaciones');
  });

  test('retirar pide confirmación y dice qué se borra y que la app sigue funcionando', async () => {
    const api = apiFalsa(consentimientosVivos(['limitaciones']));
    await renderizar(<Privacidad />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar consentimiento de limitaciones' }));

    expect(screen.getByText(/Se borrarán tus limitaciones guardadas/)).toBeOnTheScreen();
    expect(screen.getByText(/dolor que hayas anotado/)).toBeOnTheScreen();
    expect(screen.getByText(/Forja sigue funcionando sin ellas/)).toBeOnTheScreen();
    expect(api.retirarConsentimiento).not.toHaveBeenCalled();
  });

  test('cancelar no retira nada', async () => {
    const api = apiFalsa(consentimientosVivos(['limitaciones']));
    await renderizar(<Privacidad />, api);
    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar consentimiento de limitaciones' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Cancelar' }));

    expect(api.retirarConsentimiento).not.toHaveBeenCalled();
    expect(screen.queryByText(/Se borrarán/)).toBeNull();
    expect(tarjeta('Limitaciones').getByText('Dado el 17 de noviembre de 2026')).toBeOnTheScreen();
  });

  test('confirmar retira el consentimiento y la lista pasa a «No lo has dado»', async () => {
    const api = apiFalsa(consentimientosVivos(['limitaciones']));
    await renderizar(<Privacidad />, api);
    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar consentimiento de limitaciones' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Sí, retirar y borrar' }));

    await waitFor(() => expect(api.retirarConsentimiento).toHaveBeenCalledWith('limitaciones'));
    expect(await tarjeta('Limitaciones').findByText('No lo has dado')).toBeOnTheScreen();
    expect(screen.queryByText(/Se borrarán/)).toBeNull();
  });

  test('retirar limitaciones deja sin datos viejos lo que ya estaba en caché (la fila de Limitaciones)', async () => {
    function Sonda() {
      const { valor } = useResumenLimitaciones();
      return <Text>{`Limitaciones: ${valor}`}</Text>;
    }
    const api = apiFalsa({
      ...consentimientosVivos(['limitaciones']),
      tieneConsentimientoLimitaciones: jest.fn().mockResolvedValue(true),
      restricciones: jest.fn().mockResolvedValue([restriccion()]),
    });
    await renderizar(
      <>
        <Sonda />
        <Privacidad />
      </>,
      api,
    );
    expect(await screen.findByText('Limitaciones: 1 activa')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Retirar consentimiento de limitaciones' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Sí, retirar y borrar' }));

    expect(await screen.findByText('Limitaciones: Sin configurar')).toBeOnTheScreen();
    expect(api.restricciones).toHaveBeenCalledTimes(1);
  });

  test('retirar uno que no guarda nada lo dice y no habla de borrar datos', async () => {
    const api = apiFalsa(consentimientosVivos(['suplementos']));
    await renderizar(<Privacidad />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar consentimiento de suplementos' }));

    expect(screen.getByText(/no guarda nada que dependa de este permiso/)).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, retirar' }));
    await waitFor(() => expect(api.retirarConsentimiento).toHaveBeenCalledWith('suplementos'));
  });

  test('si retirar falla, enseña el error y sigue pidiendo confirmación', async () => {
    const api = apiFalsa({
      ...consentimientosVivos(['limitaciones']),
      retirarConsentimiento: jest.fn().mockRejectedValue(new ErrorApi(0, 'No hay conexión con el servidor.')),
    });
    await renderizar(<Privacidad />, api);
    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar consentimiento de limitaciones' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Sí, retirar y borrar' }));

    expect(await screen.findByText('No hay conexión con el servidor.')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Sí, retirar y borrar' })).toBeOnTheScreen();
  });

  test('si no cargan los consentimientos lo dice y deja reintentar', async () => {
    const consentimientos = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue([]);
    await renderizar(<Privacidad />, apiFalsa({ consentimientos }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByLabelText('Consentimiento: Suplementos')).toBeOnTheScreen();
  });

  test('no usa palabras de finalidad sanitaria', async () => {
    await renderizar(<Privacidad />, apiFalsa(consentimientosVivos(['limitaciones', 'suplementos'])));
    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar consentimiento de limitaciones' }));

    expect(textoVisible()).not.toMatch(PROHIBIDAS);
  });
});

describe('Privacidad: dispositivos', () => {
  const dosDispositivos = () => apiFalsa({ dispositivos: jest.fn().mockResolvedValue([dispositivoCuenta(), PC]) });

  test('lista nombre, plataforma y último uso, y marca cuál es este dispositivo', async () => {
    await renderizar(<Privacidad />, dosDispositivos());

    const este = within(await screen.findByLabelText('Dispositivo: Pixel 8'));
    expect(este.getByText('Este dispositivo')).toBeOnTheScreen();
    expect(este.getByText('Móvil · último uso el 18 de noviembre de 2026')).toBeOnTheScreen();
    const otro = within(screen.getByLabelText('Dispositivo: PC de casa'));
    expect(otro.getByText('PC · último uso el 10 de noviembre de 2026')).toBeOnTheScreen();
    expect(otro.queryByText('Este dispositivo')).toBeNull();
  });

  test('este dispositivo no se cierra desde aquí: remite a «Cerrar sesión»', async () => {
    await renderizar(<Privacidad />, dosDispositivos());

    const este = within(await screen.findByLabelText('Dispositivo: Pixel 8'));

    expect(este.queryByRole('button')).toBeNull();
    expect(este.getByText(/Cerrar sesión/)).toBeOnTheScreen();
  });

  test('cerrar otro pide confirmación y no llama a la API hasta confirmar', async () => {
    const api = dosDispositivos();
    await renderizar(<Privacidad />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Cerrar sesión en PC de casa' }));

    expect(screen.getByText(/volver a entrar con tu contraseña/)).toBeOnTheScreen();
    expect(api.cerrarDispositivo).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Cancelar' }));
    expect(api.cerrarDispositivo).not.toHaveBeenCalled();
  });

  test('confirmar cierra esa sesión y la lista se vuelve a pedir', async () => {
    let vivos = [dispositivoCuenta(), PC];
    const api = apiFalsa({
      dispositivos: jest.fn(async () => vivos),
      cerrarDispositivo: jest.fn(async (id: string) => {
        vivos = vivos.filter((d) => d.id !== id);
      }),
    });
    await renderizar(<Privacidad />, api);
    await fireEvent.press(await screen.findByRole('button', { name: 'Cerrar sesión en PC de casa' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Sí, cerrar sesión' }));

    await waitFor(() => expect(api.cerrarDispositivo).toHaveBeenCalledWith(OTRO));
    await waitFor(() => expect(screen.queryByLabelText('Dispositivo: PC de casa')).toBeNull());
    expect(screen.getByLabelText('Dispositivo: Pixel 8')).toBeOnTheScreen();
  });

  test('si falla cerrar la sesión de otro dispositivo, enseña el error', async () => {
    const api = apiFalsa({
      dispositivos: jest.fn().mockResolvedValue([dispositivoCuenta(), PC]),
      cerrarDispositivo: jest.fn().mockRejectedValue(new ErrorApi(404, 'No existe ese dispositivo.')),
    });
    await renderizar(<Privacidad />, api);
    await fireEvent.press(await screen.findByRole('button', { name: 'Cerrar sesión en PC de casa' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Sí, cerrar sesión' }));

    expect(await screen.findByText('No existe ese dispositivo.')).toBeOnTheScreen();
  });

  test('sin último uso lo dice, y si no cargan los dispositivos deja reintentar', async () => {
    const dispositivos = jest
      .fn()
      .mockRejectedValueOnce(new ErrorApi(0, 'Sin red.'))
      .mockResolvedValue([dispositivoCuenta({ ultimaActividad: undefined })]);
    await renderizar(<Privacidad />, apiFalsa({ dispositivos }));

    expect(await screen.findByText('Sin red.')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Móvil · sin actividad registrada')).toBeOnTheScreen();
  });
});

describe('Perfil: fila de privacidad', () => {
  test('enseña cuántos consentimientos y dispositivos hay de verdad y lleva a /privacidad', async () => {
    const api = apiFalsa({
      consentimientos: jest.fn().mockResolvedValue([consentimiento('limitaciones'), consentimiento('suplementos')]),
      dispositivos: jest.fn().mockResolvedValue([dispositivoCuenta(), PC]),
    });
    await renderizar(<Perfil />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Consentimientos y dispositivos: 2 de 5 · 2 dispositivos' }));

    expect(mockRouter.push).toHaveBeenCalledWith('/privacidad');
  });

  test('con un solo dispositivo y ningún consentimiento, en singular', async () => {
    await renderizar(<Perfil />);

    expect(await screen.findByRole('button', { name: 'Consentimientos y dispositivos: 0 de 5 · 1 dispositivo' })).toBeOnTheScreen();
  });

  test('si no cargan, no inventa números', async () => {
    await renderizar(<Perfil />, apiFalsa({ dispositivos: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) }));

    expect(await screen.findByRole('button', { name: 'Consentimientos y dispositivos: No disponible' })).toBeOnTheScreen();
    expect(screen.queryByText(/2 de 5 · 2/)).toBeNull();
  });

  test('«Borrar mi cuenta» lleva a su pantalla', async () => {
    await renderizar(<Perfil />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Borrar mi cuenta' }));

    expect(mockRouter.push).toHaveBeenCalledWith('/borrar-cuenta');
  });
});

describe('Borrar mi cuenta', () => {
  const sesionLocal = (uuid: string, usuario: string): SesionLocal => ({
    uuid,
    usuario,
    diaPlan: 1,
    planVersion: null,
    realizadaEn: '2026-11-18T10:00:00.000Z',
    cerradaEn: '2026-11-18T11:00:00.000Z',
    estado: 'pendiente',
    intentos: 0,
    proximoIntento: null,
    ultimoError: null,
    confirmadaEn: null,
  });

  function EstadoSesion() {
    return <Text>{`Sesión: ${useSesion().estado}`}</Text>;
  }

  /** La API falsa en la que, tras borrar la cuenta, ya no hay usuario (como con los tokens reales). */
  function apiConCuenta(cambios: Parameters<typeof apiFalsa>[0] = {}) {
    let borrada = false;
    return apiFalsa({
      usuarioActual: jest.fn(async () => (borrada ? null : USUARIO_ID)),
      borrarCuenta: jest.fn(async () => {
        borrada = true;
      }),
      ...cambios,
    });
  }

  const escribirContrasena = (texto: string) => fireEvent.changeText(screen.getByLabelText('Contraseña'), texto);

  test('explica que se borra todo y que no se puede deshacer, y pide la contraseña', async () => {
    await renderizar(<BorrarCuenta />);

    expect(await screen.findByText(/Se borra todo lo tuyo/)).toBeOnTheScreen();
    expect(screen.getByText(/No se puede deshacer/)).toBeOnTheScreen();
    expect(screen.getByLabelText('Contraseña').props.secureTextEntry).toBe(true);
    expect(textoVisible()).not.toMatch(PROHIBIDAS);
  });

  test('sin contraseña no se puede seguir', async () => {
    await renderizar(<BorrarCuenta />);

    expect(screen.getByRole('button', { name: 'Borrar mi cuenta' })).toBeDisabled();
  });

  test('con contraseña pide una confirmación final antes de llamar a la API', async () => {
    const api = apiConCuenta();
    await renderizar(<BorrarCuenta />, api);
    await escribirContrasena('doce-caracteres');

    await fireEvent.press(screen.getByRole('button', { name: 'Borrar mi cuenta' }));

    expect(screen.getByText(/¿Seguro\?/)).toBeOnTheScreen();
    expect(api.borrarCuenta).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Cancelar' }));
    expect(api.borrarCuenta).not.toHaveBeenCalled();
    expect(screen.getByLabelText('Contraseña').props.value).toBe('doce-caracteres');
  });

  test('con la contraseña mal enseña el mensaje del backend y no borra nada, ni la sesión ni lo local', async () => {
    const almacen = crearAlmacenMemoria();
    await almacen.insertarSesion(sesionLocal('s1', USUARIO_ID));
    const api = apiConCuenta({ borrarCuenta: jest.fn().mockRejectedValue(new ErrorApi(403, 'La contraseña no es correcta.')) });
    await renderizar(
      <>
        <EstadoSesion />
        <BorrarCuenta />
      </>,
      api,
      almacen,
    );
    await escribirContrasena('mala-contrasena');
    await fireEvent.press(screen.getByRole('button', { name: 'Borrar mi cuenta' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Sí, borrar mi cuenta' }));

    expect(await screen.findByText('La contraseña no es correcta.')).toBeOnTheScreen();
    expect(screen.getByText('Sesión: dentro')).toBeOnTheScreen();
    expect(await almacen.leerSesion('s1')).not.toBeNull();
  });

  test('si va bien, borra la cuenta, cierra la sesión local y limpia el outbox de esa cuenta (y solo el de esa)', async () => {
    const almacen = crearAlmacenMemoria();
    await almacen.insertarSesion(sesionLocal('mia', USUARIO_ID));
    await almacen.insertarSesion(sesionLocal('ajena', 'otro-usuario'));
    await almacen.guardarPlan({ usuario: USUARIO_ID, planUuid: 'p', json: '{}', guardadoEn: 1 });
    const api = apiConCuenta();
    await renderizar(
      <>
        <EstadoSesion />
        <BorrarCuenta />
      </>,
      api,
      almacen,
    );
    expect(await screen.findByText('Sesión: dentro')).toBeOnTheScreen();
    await escribirContrasena('doce-caracteres');
    await fireEvent.press(screen.getByRole('button', { name: 'Borrar mi cuenta' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Sí, borrar mi cuenta' }));

    await waitFor(() => expect(api.borrarCuenta).toHaveBeenCalledWith('doce-caracteres'));
    expect(await screen.findByText('Sesión: fuera')).toBeOnTheScreen();
    expect(await almacen.leerSesion('mia')).toBeNull();
    expect(await almacen.leerPlan(USUARIO_ID)).toBeNull();
    expect(await almacen.leerSesion('ajena')).not.toBeNull();
  });

  test('sin registro local (web) también borra la cuenta y sale', async () => {
    const api = apiConCuenta();
    await renderizar(
      <>
        <EstadoSesion />
        <BorrarCuenta />
      </>,
      api,
    );
    await escribirContrasena('doce-caracteres');
    await fireEvent.press(screen.getByRole('button', { name: 'Borrar mi cuenta' }));

    await fireEvent.press(screen.getByRole('button', { name: 'Sí, borrar mi cuenta' }));

    expect(await screen.findByText('Sesión: fuera')).toBeOnTheScreen();
  });
});
