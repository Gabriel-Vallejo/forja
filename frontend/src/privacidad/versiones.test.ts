import { consentimiento } from '../pruebas/dobles';
import { porRenovar } from './versiones';

describe('porRenovar', () => {
  test('pide de nuevo los que se dieron con un texto anterior y deja los vigentes', () => {
    const lista = [
      consentimiento('limitaciones', { versionTexto: 'v2' }),
      consentimiento('medidas_salud', { versionTexto: 'v1' }),
    ];

    expect(porRenovar(lista)).toEqual(['medidas_salud']);
  });

  test('los tipos sin texto propio en la app (fotos, lesiones…) no se piden', () => {
    expect(porRenovar([consentimiento('lesiones', { versionTexto: 'v0' })])).toEqual([]);
  });

  test('sin consentimientos no hay nada que renovar', () => {
    expect(porRenovar([])).toEqual([]);
  });
});
