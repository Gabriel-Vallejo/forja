import { mensajeBloqueo } from './textos';

describe('mensajes de bloqueo', () => {
  test('cada nivel explica su motivo: el cuestionario de dolor, el cribado y el profesional en días', () => {
    expect(mensajeBloqueo('urgencias_hoy')).toMatch(/cuestionario/);
    expect(mensajeBloqueo('parar_urgencias')).toMatch(/cribado/);
    expect(mensajeBloqueo('profesional_en_dias')).toMatch(/profesional sanitario/);
  });

  test('los dos urgentes mandan a urgencias hoy y no se confunden entre sí', () => {
    expect(mensajeBloqueo('urgencias_hoy')).toMatch(/urgencias/);
    expect(mensajeBloqueo('parar_urgencias')).toMatch(/urgencias/);
    expect(mensajeBloqueo('urgencias_hoy')).not.toBe(mensajeBloqueo('parar_urgencias'));
    expect(mensajeBloqueo('profesional_en_dias')).not.toMatch(/112/);
  });

  test('sin nivel conocido se queda del lado prudente', () => {
    expect(mensajeBloqueo(null)).toMatch(/urgencias/);
  });
});
