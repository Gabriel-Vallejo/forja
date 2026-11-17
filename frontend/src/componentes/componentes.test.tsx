import { fireEvent, render as renderRtl, screen } from '@testing-library/react-native';
import type { ReactElement } from 'react';

import { Marco } from '../pruebas/renderizar';

import { Cuerpo } from './Cuerpo';
import { Emblema, etiquetaEmblema, type Grado, TIPOS_RANGO } from './Emblema';
import { TabBar } from './TabBar';

const trazo = (testID: string) => screen.getAllByTestId(testID)[0]?.props;

const render = (ui: ReactElement) => renderRtl(ui, { wrapper: Marco });

describe('Cuerpo', () => {
  test('tocar un grupo llama a alTocar con su nombre', async () => {
    const alTocar = jest.fn();
    await render(<Cuerpo alTocar={alTocar} />);

    await fireEvent.press(screen.getAllByTestId('grupo-pecho-frente')[0]!);
    await fireEvent.press(screen.getAllByTestId('grupo-isquios-espalda')[0]!);

    expect(alTocar).toHaveBeenNthCalledWith(1, 'pecho');
    expect(alTocar).toHaveBeenNthCalledWith(2, 'isquios');
  });

  test('el grupo seleccionado cambia su trazo', async () => {
    await render(<Cuerpo seleccion="pecho" />);

    expect(trazo('grupo-pecho-frente')).toMatchObject({ strokeWidth: 3.5 });
    expect(trazo('grupo-hombros-frente')).toMatchObject({ strokeWidth: 1.8 });
    expect(trazo('grupo-pecho-frente')?.stroke).not.toEqual(trazo('grupo-hombros-frente')?.stroke);
  });

  test('cada grupo de la cara visible es una acción del lector de pantalla', async () => {
    const alTocar = jest.fn();
    await render(<Cuerpo alTocar={alTocar} etiqueta="Mapa" />);
    const mapa = screen.getByLabelText('Mapa · frente');

    expect(mapa.props.accessibilityActions).toContainEqual({ name: 'cuadriceps', label: 'Cuádriceps' });
    await fireEvent(mapa, 'accessibilityAction', { nativeEvent: { actionName: 'cuadriceps' } });

    expect(alTocar).toHaveBeenCalledWith('cuadriceps');
  });

  test('el botón de giro pasa a la espalda y vuelve', async () => {
    await render(<Cuerpo alTocar={jest.fn()} etiqueta="Mapa" />);

    await fireEvent.press(screen.getByRole('button', { name: 'Ver la espalda' }));
    expect(screen.getByLabelText('Mapa · espalda').props.accessibilityActions).toContainEqual({ name: 'dorsal', label: 'Dorsal' });
    await fireEvent.press(screen.getByRole('button', { name: 'Ver de frente' }));

    expect(screen.getByLabelText('Mapa · frente')).toBeOnTheScreen();
  });

  test('sin girar no hay botón de giro', async () => {
    await render(<Cuerpo girable={false} etiqueta="Miniatura" />);

    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByLabelText('Miniatura')).toBeOnTheScreen();
  });

  test('sin girar se puede dibujar la espalda', async () => {
    await render(<Cuerpo girable={false} cara="espalda" />);

    expect(screen.getAllByTestId('grupo-dorsal-espalda').length).toBeGreaterThan(0);
    expect(screen.queryAllByTestId('grupo-pecho-frente')).toHaveLength(0);
  });
});

describe('Emblema', () => {
  test.each(TIPOS_RANGO.flatMap((tipo) => ([1, 2, 3] as Grado[]).map((grado) => [tipo, grado] as const)))(
    'renderiza %s %i',
    async (tipo, grado) => {
      await render(<Emblema tipo={tipo} grado={grado} tam={48} />);

      expect(screen.getByRole('image', { name: etiquetaEmblema(tipo, grado) })).toBeOnTheScreen();
    },
  );

  test('las etiquetas dicen nivel y grado', () => {
    expect(etiquetaEmblema('oro', 3)).toBe('Emblema Oro III');
    expect(etiquetaEmblema('forjado', 1)).toBe('Emblema Forjado');
  });
});

describe('TabBar', () => {
  test('tiene las cinco pestañas y marca la activa', async () => {
    const alElegir = jest.fn();
    await render(<TabBar activo="progreso" alElegir={alElegir} />);

    const pestanas = screen.getAllByRole('tab');
    expect(pestanas.map((p) => p.props.accessibilityLabel)).toEqual(['Hoy', 'Entreno', 'Forja IA', 'Progreso', 'Perfil']);
    expect(screen.getByRole('tab', { name: 'Progreso' })).toBeSelected();
    await fireEvent.press(screen.getByRole('tab', { name: 'Forja IA' }));

    expect(alElegir).toHaveBeenCalledWith('ia');
  });
});
