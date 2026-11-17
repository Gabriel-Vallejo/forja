import { objetivo } from '../pruebas/dobles';
import { calidadPlan, numeroDeOrden, prescripcion, sinSesiones } from './plan';

describe('plan', () => {
  test('la prescripción lleva la carga, o «tu peso» si es de peso corporal', () => {
    expect(prescripcion(objetivo())).toBe('4 × 6-10 · RIR 2 · 80 kg');
    expect(prescripcion(objetivo({ cargaKg: undefined }))).toBe('4 × 6-10 · RIR 2');
    expect(prescripcion(objetivo({ incrementoKg: 0, cargaKg: 0 }))).toBe('4 × 6-10 · RIR 2 · tu peso');
  });

  test('reconoce el motivo de un ejercicio sin sesiones', () => {
    expect(sinSesiones(objetivo({ motivo: 'Todavía no hay sesiones de este ejercicio: haz…' }))).toBe(true);
    expect(sinSesiones(objetivo())).toBe(false);
  });

  test('calidad del plan y número de orden', () => {
    expect(calidadPlan('optimo')).toBe('El mejor plan');
    expect(calidadPlan('factible')).toBe('Un buen plan');
    expect(numeroDeOrden(0)).toBe('01');
  });
});
