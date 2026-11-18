import type { SerieLocal } from './almacen';
import { ultimaVersion, ultimaVersionValida } from './versiones';

function serie(uuid: string, cambios: Partial<SerieLocal> = {}): SerieLocal {
  return {
    uuid,
    sesion: 's1',
    ejercicio: 'press_banca',
    orden: 1,
    pesoKg: 80,
    repeticiones: 8,
    rir: 2,
    rpe: null,
    dolor: null,
    calentamiento: false,
    corrigeA: null,
    anulada: false,
    realizadaEn: '2026-11-18T10:00:00.000Z',
    ...cambios,
  };
}

describe('ultimaVersionValida', () => {
  test('sin correcciones devuelve las series tal cual y en su orden', () => {
    const series = [serie('a'), serie('b', { orden: 2 })];

    expect(ultimaVersionValida(series)).toEqual(series);
  });

  test('una corrección sustituye a la original en su sitio', () => {
    const correccion = serie('a2', { corrigeA: 'a', repeticiones: 10 });

    expect(ultimaVersionValida([serie('a'), serie('b'), correccion]).map((s) => s.uuid)).toEqual(['a2', 'b']);
  });

  test('una anulación quita la serie', () => {
    const anulacion = serie('a2', { corrigeA: 'a', anulada: true });

    expect(ultimaVersionValida([serie('a'), serie('b'), anulacion]).map((s) => s.uuid)).toEqual(['b']);
  });

  test('en una cadena de correcciones vale la última, y si acaba anulada no queda nada', () => {
    const cadena = [serie('a'), serie('a2', { corrigeA: 'a', pesoKg: 82.5 }), serie('a3', { corrigeA: 'a2', pesoKg: 85 })];

    expect(ultimaVersionValida(cadena)).toEqual([cadena[2]]);
    expect(ultimaVersionValida([...cadena, serie('a4', { corrigeA: 'a3', anulada: true })])).toEqual([]);
  });

  test('una corrección de una serie que no está en la lista cuenta como serie suelta', () => {
    const suelta = serie('x', { corrigeA: 'de-otra-sesion' });

    expect(ultimaVersionValida([suelta])).toEqual([suelta]);
  });

  test('ultimaVersion sigue la cadena desde cualquier eslabón', () => {
    const cadena = [serie('a'), serie('a2', { corrigeA: 'a' }), serie('a3', { corrigeA: 'a2', anulada: true })];

    expect(ultimaVersion(cadena, 'a')?.uuid).toBe('a3');
    expect(ultimaVersion(cadena, 'a2')?.uuid).toBe('a3');
    expect(ultimaVersion(cadena, 'no-existe')).toBeUndefined();
  });
});
