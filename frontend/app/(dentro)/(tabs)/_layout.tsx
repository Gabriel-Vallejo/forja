import { Tabs } from 'expo-router/js-tabs';

import { type IdPestana, PESTANAS, TabBar } from '../../../src/componentes/TabBar';

/** Las cinco pestañas con la barra propia del boceto. */
export default function Pestanas() {
  return (
    <Tabs
      screenOptions={{ headerShown: false }}
      tabBar={({ state, navigateToTab }) => {
        const activa = state.routes[state.index]?.name as IdPestana;
        const ir = (id: IdPestana) => {
          const ruta = state.routes.find((r) => r.name === id);
          if (ruta) navigateToTab(ruta.key);
        };
        return <TabBar activo={activa} alElegir={ir} />;
      }}
    >
      {PESTANAS.map((p) => (
        <Tabs.Screen key={p.id} name={p.id} options={{ title: p.etiqueta }} />
      ))}
    </Tabs>
  );
}
