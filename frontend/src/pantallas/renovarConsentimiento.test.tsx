import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { apiFalsa } from '../pruebas/dobles';
import { renderizar } from '../pruebas/renderizar';
import { RenovarConsentimiento } from './RenovarConsentimiento';

jest.mock('expo-router', () => ({ useRouter: () => ({ replace: jest.fn(), push: jest.fn(), back: jest.fn() }) }));

beforeEach(() => jest.clearAllMocks());

describe('Renovar un consentimiento con texto nuevo', () => {
  test('enseña el texto nuevo, sin «Ahora no», y aceptarlo da la versión vigente', async () => {
    const api = apiFalsa();
    await renderizar(<RenovarConsentimiento tipo="medidas_salud" />, api);

    expect(screen.getByRole('header', { name: 'Hemos cambiado un texto' })).toBeOnTheScreen();
    expect(screen.getByText(/Lo hecho antes de retirarlo sigue siendo válido/)).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Ahora no' })).toBeNull();
    await fireEvent.press(screen.getByRole('button', { name: 'Acepto el texto nuevo' }));

    await waitFor(() => expect(api.darConsentimientoMedidas).toHaveBeenCalledWith('v2'));
    expect(api.retirarConsentimiento).not.toHaveBeenCalled();
  });

  test('no aceptarlo pide confirmar, dice qué se borra y luego lo retira', async () => {
    const api = apiFalsa();
    await renderizar(<RenovarConsentimiento tipo="limitaciones" />, api);

    await fireEvent.press(screen.getByRole('button', { name: 'No acepto: retirar y borrar' }));
    expect(screen.getByText(/Se borrarán tus limitaciones guardadas/)).toBeOnTheScreen();
    expect(api.retirarConsentimiento).not.toHaveBeenCalled();
    await fireEvent.press(screen.getByRole('button', { name: 'Sí, retirar y borrar' }));

    await waitFor(() => expect(api.retirarConsentimiento).toHaveBeenCalledWith('limitaciones'));
    expect(api.darConsentimientoLimitaciones).not.toHaveBeenCalled();
  });
});
