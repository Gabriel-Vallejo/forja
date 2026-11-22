import { fireEvent, screen, waitFor, within } from '@testing-library/react-native';

import { ErrorApi } from '../api/errores';
import { apiFalsa } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { Perfil } from './Perfil';
import { PerfilDatos } from './PerfilDatos';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

beforeEach(() => jest.clearAllMocks());

const conMedidas = (cambios: Parameters<typeof apiFalsa>[0] = {}) =>
  apiFalsa({ tieneConsentimientoMedidas: jest.fn().mockResolvedValue(true), ...cambios });
const grupo = (nombre: string) => screen.getByLabelText(nombre);
const elegir = (nombre: string, opcion: string) => fireEvent.press(within(grupo(nombre)).getByRole('radio', { name: opcion }));
const campo = (nombre: string) => screen.getByLabelText(nombre);

/** Palabras que la finalidad de fitness (ADR 0007) prohíbe en cualquier texto de la app. */
const PROHIBIDAS = /rehabilit|alivi|corrige|previene lesion|desequilibr|atrofia|seguro para tu lesi|tratamiento|diagn[oó]stic|saludable|obes|sobrepeso/i;

describe('Perfil manual: sexo, experiencia y objetivo', () => {
  test('carga lo guardado y explica cada nivel de experiencia en una línea', async () => {
    const api = conMedidas({ perfilDatos: jest.fn().mockResolvedValue({ sexo: 'mujer', experiencia: 'avanzado', objetivo: 'hipertrofia' }) });
    await renderizar(<PerfilDatos />, api);

    await screen.findByLabelText('Sexo');
    expect(within(grupo('Sexo')).getByRole('radio', { name: 'Mujer' })).toBeSelected();
    expect(within(grupo('Experiencia')).getByRole('radio', { name: 'Avanzado' })).toBeSelected();
    expect(within(grupo('Objetivo')).getByRole('radio', { name: 'Hipertrofia' })).toBeSelected();
    expect(screen.getByText(/^Principiante: Menos de un año/)).toBeOnTheScreen();
    expect(screen.getByText(/^Intermedio: Entre uno y tres años/)).toBeOnTheScreen();
    expect(screen.getByText(/^Avanzado: Más de tres años/)).toBeOnTheScreen();
  });

  test('ofrece «Prefiero no decirlo» y los tres objetivos del producto', async () => {
    await renderizar(<PerfilDatos />);
    await screen.findByLabelText('Sexo');

    expect(within(grupo('Sexo')).getAllByRole('radio').map((o) => o.props.accessibilityLabel)).toEqual(['Hombre', 'Mujer', 'Prefiero no decirlo']);
    expect(within(grupo('Objetivo')).getAllByRole('radio').map((o) => o.props.accessibilityLabel)).toEqual(['Fuerza', 'Hipertrofia', 'Fuerza e hipertrofia']);
  });

  test('guarda lo elegido, con nulo en lo que queda sin definir', async () => {
    const api = conMedidas();
    await renderizar(<PerfilDatos />, api);
    await screen.findByLabelText('Sexo');

    await elegir('Sexo', 'Hombre');
    await elegir('Objetivo', 'Fuerza e hipertrofia');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(api.guardarPerfilDatos).toHaveBeenCalledWith({ sexo: 'hombre', experiencia: null, objetivo: 'fuerza_hipertrofia' }));
    expect(await screen.findByText('Guardado.')).toBeOnTheScreen();
  });

  test('volver a pulsar la experiencia la deja sin definir', async () => {
    const api = conMedidas({ perfilDatos: jest.fn().mockResolvedValue({ sexo: 'hombre', experiencia: 'intermedio' }) });
    await renderizar(<PerfilDatos />, api);
    await screen.findByLabelText('Sexo');

    await elegir('Experiencia', 'Intermedio');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(api.guardarPerfilDatos).toHaveBeenCalledWith({ sexo: 'hombre', experiencia: null, objetivo: null }));
  });

  test('si falla al guardar enseña el error', async () => {
    const api = conMedidas({ guardarPerfilDatos: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) });
    await renderizar(<PerfilDatos />, api);
    await screen.findByLabelText('Sexo');

    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText(/Sin red/)).toBeOnTheScreen();
  });

  test('si falla al cargar enseña el error en vez del formulario', async () => {
    await renderizar(<PerfilDatos />, apiFalsa({ perfilDatos: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) }));

    expect(await screen.findByText(/Sin red/)).toBeOnTheScreen();
    expect(screen.queryByLabelText('Sexo')).toBeNull();
  });

  test('vuelve atrás con el botón de la cabecera', async () => {
    await renderizar(<PerfilDatos />);

    await fireEvent.press(await screen.findByRole('button', { name: 'Volver' }));

    expect(mockRouter.back).toHaveBeenCalled();
  });
});

describe('Perfil manual: medidas', () => {
  test('sin consentimiento enseña qué se guarda y no pide las medidas', async () => {
    const api = apiFalsa();
    await renderizar(<PerfilDatos />, api);

    expect(await screen.findByText('Tus medidas, solo si tú quieres')).toBeOnTheScreen();
    expect(screen.getByText(/tu altura, tu peso y las circunferencias/)).toBeOnTheScreen();
    expect(screen.queryByLabelText('Altura (cm)')).toBeNull();
    expect(api.medidas).not.toHaveBeenCalled();
  });

  test('dar el consentimiento abre los campos de medidas', async () => {
    const api = apiFalsa();
    await renderizar(<PerfilDatos />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Acepto' }));

    expect(api.darConsentimientoMedidas).toHaveBeenCalledWith('v2');
    expect(await screen.findByLabelText('Altura (cm)')).toBeOnTheScreen();
  });

  test('carga lo guardado y enseña la estimación de grasa con su margen', async () => {
    const api = conMedidas({
      perfilDatos: jest.fn().mockResolvedValue({ sexo: 'hombre' }),
      medidas: jest.fn().mockResolvedValue({ alturaCm: 180, pesoKg: 78.5, cuelloCm: 38, cinturaCm: 85, grasaEstimada: { porcentaje: 16.1, margen: 4 } }),
    });
    await renderizar(<PerfilDatos />, api);

    expect(await screen.findByDisplayValue('78,5')).toBeOnTheScreen();
    expect(campo('Altura (cm)').props.value).toBe('180');
    expect(campo('Cadera (cm)').props.value).toBe('');
    expect(screen.getByText('≈ 16,1 % (± 4 puntos)')).toBeOnTheScreen();
    expect(screen.getByText(/margen de error ronda los 4 puntos/)).toBeOnTheScreen();
  });

  test('sin estimación explica qué falta según el sexo', async () => {
    await renderizar(<PerfilDatos />, conMedidas({ perfilDatos: jest.fn().mockResolvedValue({ sexo: 'mujer' }) }));

    expect(await screen.findByText('Para estimar tu % de grasa necesitas altura, cuello, cintura y cadera.')).toBeOnTheScreen();
  });

  test('guarda las cinco medidas, con nulo en las vacías y la coma como decimal', async () => {
    const api = conMedidas();
    await renderizar(<PerfilDatos />, api);
    await screen.findByLabelText('Altura (cm)');

    await fireEvent.changeText(campo('Altura (cm)'), '180');
    await fireEvent.changeText(campo('Peso (kg)'), '78,5');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar medidas' }));

    await waitFor(() =>
      expect(api.guardarMedidas).toHaveBeenCalledWith({ alturaCm: 180, pesoKg: 78.5, cuelloCm: null, cinturaCm: null, caderaCm: null }),
    );
    expect(await screen.findByText('Guardado.')).toBeOnTheScreen();
  });

  test('un valor fuera de rango no se envía y se explica junto al campo', async () => {
    const api = conMedidas();
    await renderizar(<PerfilDatos />, api);
    await screen.findByLabelText('Altura (cm)');

    await fireEvent.changeText(campo('Altura (cm)'), '90');
    await fireEvent.changeText(campo('Cuello (cm)'), '40');
    await fireEvent.changeText(campo('Cintura (cm)'), '40');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar medidas' }));

    expect(await screen.findByText('Error: Altura: un número entre 100 y 250 cm.')).toBeOnTheScreen();
    expect(screen.getByText('Error: La cintura tiene que ser mayor que el cuello.')).toBeOnTheScreen();
    expect(api.guardarMedidas).not.toHaveBeenCalled();
  });

  test('cambiar el sexo vuelve a leer las medidas, porque cambia la estimación', async () => {
    const medidas = jest.fn().mockResolvedValue({ alturaCm: 180 });
    const api = conMedidas({ medidas });
    await renderizar(<PerfilDatos />, api);
    await screen.findByLabelText('Altura (cm)');
    expect(medidas).toHaveBeenCalledTimes(1);

    await elegir('Sexo', 'Hombre');
    await fireEvent.press(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(medidas).toHaveBeenCalledTimes(2));
  });

  test('retirar el consentimiento avisa de que borra también las medidas', async () => {
    const api = conMedidas();
    await renderizar(<PerfilDatos />, api);

    await fireEvent.press(await screen.findByRole('button', { name: 'Retirar mi consentimiento' }));
    expect(screen.getByText(/También tus medidas: altura, peso y circunferencias\./)).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, retirar y borrar' }));

    await waitFor(() => expect(api.retirarConsentimientoMedidas).toHaveBeenCalled());
    expect(await screen.findByText('Tus medidas, solo si tú quieres')).toBeOnTheScreen();
  });

  test('si fallan las medidas enseña el error', async () => {
    await renderizar(<PerfilDatos />, conMedidas({ medidas: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) }));

    expect(await screen.findByText(/Sin red/)).toBeOnTheScreen();
  });

  test('ningún texto usa lenguaje sanitario', async () => {
    const api = conMedidas({ medidas: jest.fn().mockResolvedValue({ alturaCm: 180, cuelloCm: 38, cinturaCm: 85, grasaEstimada: { porcentaje: 16.1, margen: 4 } }) });
    await renderizar(<PerfilDatos />, api);
    await screen.findByLabelText('Altura (cm)');

    expect(JSON.stringify(screen.toJSON())).not.toMatch(PROHIBIDAS);
  });
});

describe('Perfil: datos reales', () => {
  test('la cabecera y la fila Objetivo salen del perfil guardado y llevan a la pantalla de datos', async () => {
    const api = apiFalsa({ perfilDatos: jest.fn().mockResolvedValue({ sexo: 'hombre', experiencia: 'intermedio', objetivo: 'fuerza_hipertrofia' }) });
    await renderizar(<Perfil />, api);

    expect(await screen.findByText('Fuerza e hipertrofia · Intermedio')).toBeOnTheScreen();
    expect(screen.queryByText(/desde nov/)).toBeNull();
    await fireEvent.press(screen.getByRole('button', { name: 'Objetivo: Fuerza e hipertrofia' }));
    expect(mockRouter.push).toHaveBeenCalledWith('/perfil-datos');
    await fireEvent.press(screen.getByRole('button', { name: 'Experiencia: Intermedio' }));
    expect(mockRouter.push).toHaveBeenCalledTimes(2);
  });

  test('sin definir invita a completarlo', async () => {
    await renderizar(<Perfil />);

    expect(await screen.findByText('Completa tu objetivo y tu experiencia')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Objetivo: Sin definir' })).toBeOnTheScreen();
  });

  test('la fila Medidas resume el estado: pendientes sin consentimiento y la estimación con él', async () => {
    await renderizar(<Perfil />);
    expect(await screen.findByRole('button', { name: 'Medidas: Pendientes' })).toBeOnTheScreen();
  });

  test('con medidas y estimación, la fila las enseña', async () => {
    const api = conMedidas({ medidas: jest.fn().mockResolvedValue({ alturaCm: 180, grasaEstimada: { porcentaje: 16.1, margen: 4 } }) });
    await renderizar(<Perfil />, api);

    expect(await screen.findByRole('button', { name: 'Medidas: ≈ 16,1 % (± 4 puntos)' })).toBeOnTheScreen();
  });

  test('con medidas sin estimación dice «Guardadas»', async () => {
    const api = conMedidas({ medidas: jest.fn().mockResolvedValue({ alturaCm: 180 }) });
    await renderizar(<Perfil />, api);

    expect(await screen.findByRole('button', { name: 'Medidas: Guardadas' })).toBeOnTheScreen();
  });

  test('si el perfil no carga lo dice en las filas', async () => {
    await renderizar(<Perfil />, apiFalsa({ perfilDatos: jest.fn().mockRejectedValue(new ErrorApi(0, 'Sin red.')) }));

    expect(await screen.findByRole('button', { name: 'Objetivo: No disponible' })).toBeOnTheScreen();
  });
});
