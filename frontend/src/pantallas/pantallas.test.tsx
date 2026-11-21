import { act, fireEvent, screen, waitFor } from '@testing-library/react-native';

import { ErrorApi } from '../api/errores';
import { apiFalsa } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { AVISO_CUENTA_BORRADA } from '../sesion/sesion';
import { Entrar } from './Entrar';
import { Registro } from './Registro';
import { RevisaTuCorreo } from './RevisaTuCorreo';

const mockRouter = { replace: jest.fn(), push: jest.fn() };
const router = mockRouter;
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

// La ruleta nativa no existe en Jest: un botón que «elige» la fecha que diga el test.
let mockFechaElegida = '2000-01-01';
jest.mock('@react-native-community/datetimepicker', () => {
  const { createElement } = jest.requireActual<typeof import('react')>('react');
  const { Pressable, Text } = jest.requireActual<typeof import('react-native')>('react-native');
  return {
    __esModule: true,
    default: ({ onChange }: { onChange: (evento: { type: string }, fecha?: Date) => void }) => {
      const [anio, mes, dia] = mockFechaElegida.split('-').map(Number) as [number, number, number];
      return createElement(
        Pressable,
        { accessibilityRole: 'button', accessibilityLabel: 'Confirmar fecha', onPress: () => onChange({ type: 'set' }, new Date(anio, mes - 1, dia)) },
        createElement(Text, null, 'Confirmar'),
      );
    },
  };
});

beforeEach(() => jest.clearAllMocks());

describe('Registro', () => {
  async function rellenar(contrasena: string, fecha: string) {
    await fireEvent.changeText(screen.getByLabelText('Correo'), 'ana@forja.test');
    await fireEvent.changeText(screen.getByLabelText('Contraseña'), contrasena);
    mockFechaElegida = fecha;
    await fireEvent.press(screen.getByRole('button', { name: /^Fecha de nacimiento/ }));
    await fireEvent.press(screen.getByRole('button', { name: 'Confirmar fecha' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Crear cuenta' }));
  }

  test('con contraseña corta y menor de edad enseña los dos errores y no llama a la API', async () => {
    const api = apiFalsa();
    await renderizar(<Registro />, api);

    await rellenar('corta', '2015-05-05');

    expect(await screen.findByText('Error: La contraseña necesita al menos 12 caracteres.')).toBeOnTheScreen();
    expect(screen.getByText('Error: Forja es para mayores de 18 años.')).toBeOnTheScreen();
    expect(api.registrar).not.toHaveBeenCalled();
  });

  test('con datos válidos crea la cuenta y lleva a «revisa tu correo»', async () => {
    const api = apiFalsa();
    await renderizar(<Registro />, api);

    await rellenar('doce-caracteres', '2000-01-31');

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith('/revisa-tu-correo'));
    expect(api.registrar).toHaveBeenCalledWith({
      correo: 'ana@forja.test',
      contrasena: 'doce-caracteres',
      fechaNacimiento: '2000-01-31',
    });
  });

  test('si el backend rechaza el alta, enseña su mensaje', async () => {
    const api = apiFalsa({ registrar: jest.fn().mockRejectedValue(new ErrorApi(400, 'El correo no es válido.')) });
    await renderizar(<Registro />, api);

    await rellenar('doce-caracteres', '2000-01-31');

    expect(await screen.findByText('El correo no es válido.')).toBeOnTheScreen();
  });
});

describe('Entrar', () => {
  test('manda las credenciales y vuelve a la portada', async () => {
    const api = apiFalsa();
    await renderizar(<Entrar />, api);

    await fireEvent.changeText(screen.getByLabelText('Correo'), ' ana@forja.test ');
    await fireEvent.changeText(screen.getByLabelText('Contraseña'), 'doce-caracteres');
    await fireEvent.press(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => expect(router.replace).toHaveBeenCalledWith('/'));
    expect(api.entrar).toHaveBeenCalledWith({ correo: 'ana@forja.test', contrasena: 'doce-caracteres' });
  });

  test('enseña el error del backend', async () => {
    const api = apiFalsa({ entrar: jest.fn().mockRejectedValue(new ErrorApi(401, 'Correo o contraseña incorrectos.')) });
    await renderizar(<Entrar />, api);

    await fireEvent.changeText(screen.getByLabelText('Correo'), 'ana@forja.test');
    await fireEvent.changeText(screen.getByLabelText('Contraseña'), 'mal');
    await fireEvent.press(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByText('Correo o contraseña incorrectos.')).toBeOnTheScreen();
  });

  test('si la sesión se cerró porque la cuenta se borró en otro dispositivo, lo dice', async () => {
    const oyentes: ((usuario: string) => Promise<void>)[] = [];
    const api = apiFalsa({
      alBorrarseLaCuenta: jest.fn((oyente: (usuario: string) => Promise<void>) => {
        oyentes.push(oyente);
        return () => undefined;
      }),
    });
    await renderizar(<Entrar />, api);
    expect(screen.queryByText(AVISO_CUENTA_BORRADA)).toBeNull();

    await act(async () => {
      await Promise.all(oyentes.map((o) => o('usuario')));
    });

    expect(screen.getByText(AVISO_CUENTA_BORRADA)).toBeOnTheScreen();
  });

  test('sin datos, el botón está deshabilitado', async () => {
    await renderizar(<Entrar />, apiFalsa());

    expect(screen.getByRole('button', { name: 'Entrar' })).toBeDisabled();
  });
});

describe('Revisa tu correo', () => {
  test('explica el siguiente paso y lleva a entrar', async () => {
    await renderizar(<RevisaTuCorreo />, apiFalsa());

    expect(screen.getByRole('header', { name: 'Revisa tu correo' })).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Ir a entrar' }));
    expect(router.replace).toHaveBeenCalledWith('/entrar');
  });
});
