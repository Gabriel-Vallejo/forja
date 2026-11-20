import { objetivo, planDePrueba } from '../pruebas/dobles';
import { calidadPlan, diasDesdeHoy, hayPrimeraSesion, numeroDeOrden, prescripcion } from './plan';

describe('plan', () => {
  test('la prescripción lleva la carga, o «tu peso» si es de peso corporal', () => {
    expect(prescripcion(objetivo())).toBe('4 × 6-10 · RIR 2 · 80 kg');
    expect(prescripcion(objetivo({ cargaKg: undefined }))).toBe('4 × 6-10 · RIR 2');
    expect(prescripcion(objetivo({ incrementoKg: 0, cargaKg: 0 }))).toBe('4 × 6-10 · RIR 2 · tu peso');
  });

  test('la primera sesión sale de `primeraSesion` del día, no del texto del motivo', () => {
    const [nuevo] = planDePrueba(true).dias;
    const [conCarga] = planDePrueba(false).dias;
    expect(hayPrimeraSesion(nuevo!)).toBe(true);
    expect(hayPrimeraSesion(conCarga!)).toBe(false);
  });

  test('los días salen empezando por el de hoy y siguiendo el ciclo', () => {
    expect(diasDesdeHoy(planDePrueba(false, 1)).map((d) => d.dia)).toEqual([1, 2]);
    expect(diasDesdeHoy(planDePrueba(false, 2)).map((d) => d.dia)).toEqual([2, 1]);
  });

  test('calidad del plan y número de orden', () => {
    expect(calidadPlan('optimo')).toBe('El mejor plan');
    expect(calidadPlan('factible')).toBe('Un buen plan');
    expect(numeroDeOrden(0)).toBe('01');
  });
});
