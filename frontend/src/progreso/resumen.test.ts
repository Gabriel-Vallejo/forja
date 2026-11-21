import { formatoCarga } from './resumen';

describe('formatoCarga', () => {
  test('por debajo de una tonelada en kg; desde ahí en toneladas con un decimal hasta 100', () => {
    expect(formatoCarga(0)).toEqual({ valor: '0', unidad: 'kg' });
    expect(formatoCarga(850.4)).toEqual({ valor: '850', unidad: 'kg' });
    expect(formatoCarga(12_400)).toEqual({ valor: '12,4', unidad: 't' });
    expect(formatoCarga(312_400)).toEqual({ valor: '312', unidad: 't' });
  });
});
