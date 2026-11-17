import { fireEvent, screen } from '@testing-library/react-native';

import { renderizar } from '../pruebas/renderizar';
import { Calculadora } from './Calculadora';
import { Escala } from './Escala';
import { ForjaIA, LLEGA } from './ForjaIA';
import { Progreso } from './Progreso';
import { Proximamente } from './Proximamente';
import { Racha } from './Racha';
import { RangoDetalle } from './RangoDetalle';

const mockRouter = { replace: jest.fn(), push: jest.fn(), back: jest.fn(), navigate: jest.fn() };
jest.mock('expo-router', () => ({ useRouter: () => mockRouter }));

beforeEach(() => jest.clearAllMocks());

const avisoDeEjemplo = () => expect(screen.getAllByLabelText('Datos de ejemplo').length).toBeGreaterThan(0);

describe('Progreso', () => {
  test('rango global, aviso de ejemplo y panel del músculo tocado', async () => {
    await renderizar(<Progreso />);

    avisoDeEjemplo();
    expect(screen.getByRole('button', { name: 'Tu rango Forja: Acero II, top 26 %. Ver la escala' })).toBeOnTheScreen();
    expect(screen.getByRole('header', { name: 'Pecho' })).toBeOnTheScreen();
    await fireEvent.press(screen.getAllByTestId('grupo-cuadriceps-frente')[0]!);

    expect(screen.getByRole('header', { name: 'Cuádriceps' })).toBeOnTheScreen();
    expect(screen.getByText('Sentadilla con barra')).toBeOnTheScreen();
  });

  test('una fila de «Por zona» abre su músculo en el panel', async () => {
    await renderizar(<Progreso />);

    await fireEvent.press(screen.getByRole('button', { name: 'Core: BRONCE II' }));

    expect(screen.getByRole('header', { name: 'Abdomen' })).toBeOnTheScreen();
  });

  test('la tarjeta del rango abre la escala', async () => {
    await renderizar(<Progreso />);

    await fireEvent.press(screen.getByRole('button', { name: /^Tu rango Forja/ }));

    expect(mockRouter.push).toHaveBeenCalledWith('/escala');
  });
});

describe('Escala', () => {
  test('25 escalones con el tuyo marcado', async () => {
    await renderizar(<Escala />);

    avisoDeEjemplo();
    expect(screen.getByLabelText('Forjado, top 1 %')).toBeOnTheScreen();
    expect(screen.getByLabelText('Acero II, top 26 %, tu rango')).toBeOnTheScreen();
    expect(screen.getAllByLabelText(/^Emblema /)).toHaveLength(25);
  });
});

describe('Resultado del rango', () => {
  test('enseña el rango, la serie y su e1RM', async () => {
    await renderizar(<RangoDetalle ejercicio="Press de banca con barra" peso={80.5} repeticiones={8} />);

    avisoDeEjemplo();
    expect(screen.getByRole('header', { name: 'ACERO III' })).toBeOnTheScreen();
    expect(screen.getByText('80,5 kg × 8 reps · e1RM ≈ 102 kg')).toBeOnTheScreen();
  });
});

describe('Calculadora', () => {
  async function elegirBanca() {
    await renderizar(<Calculadora />);
    await fireEvent.press(screen.getByRole('button', { name: /^Press de banca con barra/ }));
  }

  test('con 80,5 kg × 8 el e1RM de Epley es 102 kg', async () => {
    await elegirBanca();

    avisoDeEjemplo();
    expect(screen.getByText('e1RM ≈ 102 kg · Epley')).toBeOnTheScreen();
  });

  test('mover las reglas recalcula el e1RM', async () => {
    await elegirBanca();

    const peso = screen.getByRole('adjustable', { name: 'Peso en kilos' });
    const reps = screen.getByRole('adjustable', { name: 'Repeticiones' });
    await fireEvent(peso, 'accessibilityAction', { nativeEvent: { actionName: 'decrement' } });
    await fireEvent(reps, 'accessibilityAction', { nativeEvent: { actionName: 'increment' } });

    // 80 × (1 + 9 / 30) = 104
    expect(screen.getByText('e1RM ≈ 104 kg · Epley')).toBeOnTheScreen();
  });

  test('«Obtener mi rango» pasa la serie al resultado', async () => {
    await elegirBanca();

    await fireEvent.press(screen.getByRole('button', { name: 'Obtener mi rango' }));

    expect(mockRouter.push).toHaveBeenCalledWith({
      pathname: '/rango',
      params: { ejercicio: 'Press de banca con barra', peso: '80.5', reps: '8' },
    });
  });

  test('«Atrás» en la serie vuelve a la lista', async () => {
    await elegirBanca();

    await fireEvent.press(screen.getByRole('button', { name: 'Atrás' }));

    expect(screen.getByRole('header', { name: 'Elige un ejercicio y dinos tu mejor serie' })).toBeOnTheScreen();
  });
});

describe('Racha', () => {
  test('semanas, estadísticas y aviso de ejemplo', async () => {
    await renderizar(<Racha />);

    avisoDeEjemplo();
    expect(screen.getByRole('header', { name: '4' })).toBeOnTheScreen();
    expect(screen.getByText('Mejor racha')).toBeOnTheScreen();
    expect(screen.getByLabelText('1 de 3 días hechos')).toBeOnTheScreen();
  });
});

describe('Forja IA', () => {
  test('enviar dice que llega en la v0.2', async () => {
    await renderizar(<ForjaIA />);

    await fireEvent.changeText(screen.getByLabelText('Escribe a Forja'), 'Me molesta el hombro');
    await fireEvent.press(screen.getByRole('button', { name: 'Enviar' }));

    expect(screen.getByLabelText(`Información: ${LLEGA}`)).toBeOnTheScreen();
  });
});

describe('Próximamente', () => {
  test('dice qué llega y deja volver', async () => {
    await renderizar(<Proximamente titulo="Sesión en curso" />);

    expect(screen.getByRole('header', { name: 'Sesión en curso' })).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Volver' }));

    expect(mockRouter.back).toHaveBeenCalled();
  });
});
