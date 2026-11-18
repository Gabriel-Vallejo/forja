import { addNetworkStateListener, getNetworkStateAsync } from 'expo-network';

import { escucharRed } from './red';

jest.mock('expo-network', () => ({
  getNetworkStateAsync: jest.fn(),
  addNetworkStateListener: jest.fn(),
}));

describe('escucharRed', () => {
  test('avisa del estado inicial y de cada cambio, y deja de escuchar', async () => {
    const remove = jest.fn();
    let cambio: (estado: object) => void = () => undefined;
    (getNetworkStateAsync as jest.Mock).mockResolvedValue({ isConnected: true, isInternetReachable: undefined });
    (addNetworkStateListener as jest.Mock).mockImplementation((o) => {
      cambio = o;
      return { remove };
    });
    const oyente = jest.fn();

    const dejar = escucharRed(oyente);
    await Promise.resolve();
    cambio({ isConnected: true, isInternetReachable: false });
    cambio({ isConnected: false });
    dejar();

    expect(oyente.mock.calls).toEqual([[true], [false], [false]]);
    expect(remove).toHaveBeenCalled();
  });

  test('si no puede leer el estado inicial, sigue con los cambios', async () => {
    (getNetworkStateAsync as jest.Mock).mockRejectedValue(new Error('x'));
    (addNetworkStateListener as jest.Mock).mockReturnValue({ remove: jest.fn() });
    const oyente = jest.fn();

    escucharRed(oyente)();
    await Promise.resolve();

    expect(oyente).not.toHaveBeenCalled();
  });
});
