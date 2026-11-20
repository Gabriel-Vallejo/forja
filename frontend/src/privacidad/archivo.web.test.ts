import { guardarJson, SIN_COMPARTIR } from './archivo.web';

describe('guardar el JSON en el navegador', () => {
  const enlace = { href: '', download: '', click: jest.fn() };
  const original = { createObjectURL: URL.createObjectURL, revokeObjectURL: URL.revokeObjectURL };

  beforeEach(() => {
    jest.useFakeTimers();
    URL.createObjectURL = jest.fn(() => 'blob:forja/1');
    URL.revokeObjectURL = jest.fn();
    (globalThis as { document?: unknown }).document = { createElement: jest.fn(() => enlace) };
  });

  afterEach(() => {
    jest.useRealTimers();
    Object.assign(URL, original);
    delete (globalThis as { document?: unknown }).document;
  });

  test('lo descarga con su nombre y suelta la URL temporal', async () => {
    await guardarJson('forja-mis-datos-2026-11-20.json', '{"formato":1}');

    const blob = jest.mocked(URL.createObjectURL).mock.calls[0]?.[0] as Blob;
    expect(blob.type).toBe('application/json');
    expect(enlace).toMatchObject({ href: 'blob:forja/1', download: 'forja-mis-datos-2026-11-20.json' });
    expect(enlace.click).toHaveBeenCalledTimes(1);
    jest.runAllTimers();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:forja/1');
  });

  test('sin documento (fuera del navegador) lo dice', async () => {
    delete (globalThis as { document?: unknown }).document;

    await expect(guardarJson('a.json', '{}')).rejects.toThrow(SIN_COMPARTIR);
  });
});
