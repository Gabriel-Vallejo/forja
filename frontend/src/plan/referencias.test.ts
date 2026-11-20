import { trozosFuente } from './referencias';

describe('trozos de la fuente de una razón', () => {
  test('una cita conocida es un enlace a su DOI', () => {
    expect(trozosFuente('Maeo 2021')).toEqual([
      { texto: 'Maeo 2021', url: 'https://doi.org/10.1249/MSS.0000000000002523' },
    ]);
  });

  test('con varias citas, cada una lleva su enlace y el texto de en medio queda igual', () => {
    expect(trozosFuente('Renaissance Periodization; secundarios a 0,5 por Pelland 2026')).toEqual([
      { texto: 'Renaissance Periodization; secundarios a 0,5 por ' },
      { texto: 'Pelland 2026', url: 'https://doi.org/10.1007/s40279-025-02344-w' },
    ]);
    expect(trozosFuente('Schoenfeld 2019; Pelland 2026').map((t) => t.url)).toEqual([
      'https://doi.org/10.1080/02640414.2018.1555906',
      undefined,
      'https://doi.org/10.1007/s40279-025-02344-w',
    ]);
  });

  test('una heurística o una cita desconocida se queda en texto', () => {
    expect(trozosFuente('Heurística de entrenadores, no ciencia (ADR 0007)')).toEqual([
      { texto: 'Heurística de entrenadores, no ciencia (ADR 0007)' },
    ]);
    expect(trozosFuente('Fulano 2030')).toEqual([{ texto: 'Fulano 2030' }]);
  });
});
