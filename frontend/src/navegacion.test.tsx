import { fireEvent, renderRouter, screen } from 'expo-router/testing-library';
import { Text } from 'react-native';

import Pestanas from '../app/(dentro)/(tabs)/_layout';
import { PESTANAS } from './componentes/TabBar';
import { Marco } from './pruebas/renderizar';

// Las rutas reales de las pestañas con pantallas mínimas: lo que se prueba es la navegación.
const rutas = Object.fromEntries(
  PESTANAS.map((p) => [`(tabs)/${p.id}`, () => <Text>{`Pantalla ${p.id}`}</Text>]),
);

test('las cinco pestañas existen y llevan a su pantalla', async () => {
  const app = await renderRouter({ '(tabs)/_layout': Pestanas, ...rutas }, { initialUrl: '/hoy', wrapper: Marco });

  expect(screen.getAllByRole('tab').map((t) => t.props.accessibilityLabel)).toEqual(PESTANAS.map((p) => p.etiqueta));
  expect(screen.getByRole('tab', { name: 'Hoy' })).toBeSelected();
  await fireEvent.press(screen.getByRole('tab', { name: 'Progreso' }));

  expect(app.getPathname()).toBe('/progreso');
  expect(screen.getByRole('tab', { name: 'Progreso' })).toBeSelected();
  expect(screen.getByText('Pantalla progreso')).toBeOnTheScreen();
});
