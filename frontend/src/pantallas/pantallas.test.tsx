import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import type { PlanActual } from '../api/esquemas';
import { ErrorApi } from '../api/errores';
import { apiFalsa } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { Entrar } from './Entrar';
import { Portada } from './Portada';
import { Registro } from './Registro';
import { RevisaTuCorreo } from './RevisaTuCorreo';

const mockRouter = { replace: jest.fn(), push: jest.fn() };
const router = mockRouter;
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

const PLAN: PlanActual = {
  uuid: '0193a3c4-7b2e-7c1d-9f00-000000000001',
  creadoEn: '2026-11-16T10:00:00Z',
  estado: 'OPTIMAL',
  dias: [
    {
      dia: 1,
      ejercicios: [
        {
          idExterno: 'press_banca',
          nombre: 'Press de banca',
          orden: 1,
          series: 4,
          repeticionesMin: 6,
          repeticionesMax: 10,
          rirObjetivo: 2,
          cargaKg: 80,
          incrementoKg: 2.5,
          estado: 'acumulando',
          motivo: 'Repite la carga hasta completar el rango alto.',
        },
      ],
    },
  ],
};

beforeEach(() => jest.clearAllMocks());

describe('Registro', () => {
  async function rellenar(contrasena: string, fecha: string) {
    await fireEvent.changeText(screen.getByLabelText('Correo'), 'ana@forja.test');
    await fireEvent.changeText(screen.getByLabelText('Contraseña'), contrasena);
    await fireEvent.changeText(screen.getByLabelText('Fecha de nacimiento'), fecha);
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

describe('Portada', () => {
  test('enseña el plan actual con su prescripción', async () => {
    await renderizar(<Portada />, apiFalsa({ planActual: jest.fn().mockResolvedValue(PLAN) }));

    expect(await screen.findByText('Press de banca')).toBeOnTheScreen();
    expect(screen.getByText('4 × 6-10 · RIR 2 · 80 kg')).toBeOnTheScreen();
    expect(screen.getByRole('header', { name: 'Día 1' })).toBeOnTheScreen();
  });

  test('sin plan, ofrece generarlo y vuelve a pedir el plan', async () => {
    const planActual = jest.fn().mockResolvedValueOnce(null).mockResolvedValue(PLAN);
    const api = apiFalsa({ planActual });
    await renderizar(<Portada />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Generar mi plan' }));

    expect(await screen.findByText('Press de banca')).toBeOnTheScreen();
    expect(api.generarPlan).toHaveBeenCalledTimes(1);
  });

  test('si falla la carga, enseña el error y deja reintentar', async () => {
    const planActual = jest.fn().mockRejectedValueOnce(new ErrorApi(0, 'Sin red.')).mockResolvedValue(PLAN);
    await renderizar(<Portada />, apiFalsa({ planActual }));

    await fireEvent.press(await screen.findByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Press de banca')).toBeOnTheScreen();
  });

  test('cerrar sesión llama a la API', async () => {
    const api = apiFalsa();
    await renderizar(<Portada />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Cerrar sesión' }));

    await waitFor(() => expect(api.cerrarSesion).toHaveBeenCalledTimes(1));
  });
});
