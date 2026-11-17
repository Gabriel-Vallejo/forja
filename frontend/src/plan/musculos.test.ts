import { dark } from '../theme';
import { caraDelCuerpo, coloresCuerpo, etiquetaMusculo, gruposPrincipales, musculosDelDia } from './musculos';
import { objetivo, planDePrueba } from '../pruebas/dobles';

const [torso, pierna] = planDePrueba().dias;

describe('músculos del plan', () => {
  test('los grupos principales del día son los primarios con más series', () => {
    expect(gruposPrincipales(torso!)).toEqual(['Pecho', 'Dorsal']);
    expect(gruposPrincipales(pierna!)).toEqual(['Cuádriceps']);
  });

  test('los músculos del día no se repiten y el primario gana al secundario', () => {
    const dia = { dia: 1, nombre: 'TORSO', ejercicios: [
      objetivo({ musculos: [{ grupo: 'triceps', rol: 'secundario' }] }),
      objetivo({ musculos: [{ grupo: 'triceps', rol: 'primario' }] }),
    ] };

    expect(musculosDelDia(dia)).toEqual([{ grupo: 'triceps', rol: 'primario' }]);
  });

  test('los secundarios llevan «½» en la etiqueta', () => {
    expect(etiquetaMusculo({ grupo: 'pecho', rol: 'primario' })).toBe('Pecho');
    expect(etiquetaMusculo({ grupo: 'triceps', rol: 'secundario' })).toBe('Tríceps ½');
  });

  test('se dibuja la cara donde se ven más grupos trabajados, y el frente si empatan', () => {
    expect(caraDelCuerpo([{ grupo: 'pecho', rol: 'primario' }])).toBe('frente');
    expect(caraDelCuerpo([{ grupo: 'dorsal', rol: 'primario' }, { grupo: 'lumbar', rol: 'primario' }])).toBe('espalda');
    expect(caraDelCuerpo([])).toBe('frente');
  });

  test('primarios en primary, secundarios en secondary y el resto en el neutro', () => {
    const colores = coloresCuerpo(
      [
        { grupo: 'pecho', rol: 'primario' },
        { grupo: 'hombros', rol: 'secundario' },
        { grupo: 'dorsal', rol: 'primario' },
      ],
      dark,
      'frente',
    );

    expect(colores.pecho).toBe(dark.color.primary);
    expect(colores.hombros).toBe(dark.color.secondary);
    expect(colores.abdomen).toBe(dark.cuerpo.neutro);
    expect(colores.dorsal).toBeUndefined();
  });
});
