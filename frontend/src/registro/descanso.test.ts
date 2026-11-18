import { act, renderHook } from '@testing-library/react-native';

import { DESCANSO_MS, formatoDescanso, SUMA_DESCANSO_MS, useDescanso } from './descanso';

describe('temporizador de descanso', () => {
  beforeEach(() => jest.useFakeTimers({ now: Date.UTC(2026, 10, 18, 10) }));
  afterEach(() => jest.useRealTimers());

  test('cuenta atrás de 2:00, +30 s y saltar', async () => {
    const { result } = await renderHook(() => useDescanso());
    expect(result.current.restanteMs).toBeNull();

    await act(async () => result.current.empezar());
    expect(result.current.restanteMs).toBe(DESCANSO_MS);

    await act(async () => jest.advanceTimersByTime(30_000));
    expect(formatoDescanso(result.current.restanteMs!)).toBe('1:30');

    await act(async () => result.current.sumar());
    expect(result.current.restanteMs).toBe(90_000 + SUMA_DESCANSO_MS);

    await act(async () => result.current.saltar());
    expect(result.current.restanteMs).toBeNull();
  });

  test('no se desfasa en segundo plano: cuenta con la hora de fin, no con los ticks', async () => {
    const { result } = await renderHook(() => useDescanso());
    await act(async () => result.current.empezar());

    // La app se va a segundo plano: el reloj avanza 90 s sin que corra ningún intervalo.
    jest.setSystemTime(Date.now() + 90_000);
    await act(async () => jest.advanceTimersByTime(1_000));

    expect(formatoDescanso(result.current.restanteMs!)).toBe('0:29');
  });

  test('al llegar a cero se acaba solo', async () => {
    const { result } = await renderHook(() => useDescanso());
    await act(async () => result.current.empezar());

    await act(async () => jest.advanceTimersByTime(DESCANSO_MS + 1_000));

    expect(result.current.restanteMs).toBeNull();
  });

  test('+30 s sin descanso en marcha arranca uno de 30 s', async () => {
    const { result } = await renderHook(() => useDescanso());

    await act(async () => result.current.sumar());

    expect(result.current.restanteMs).toBe(SUMA_DESCANSO_MS);
  });

  test('formato m:ss redondeando hacia arriba', () => {
    expect(formatoDescanso(120_000)).toBe('2:00');
    expect(formatoDescanso(59_001)).toBe('1:00');
    expect(formatoDescanso(5_000)).toBe('0:05');
    expect(formatoDescanso(0)).toBe('0:00');
  });
});
