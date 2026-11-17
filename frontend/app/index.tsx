import { Redirect } from 'expo-router';

import { Pantalla } from '../src/componentes/Pantalla';
import { Texto } from '../src/componentes/Texto';
import { Portada } from '../src/pantallas/Portada';
import { useSesion } from '../src/sesion/sesion';

export default function Inicio() {
  const { estado } = useSesion();
  if (estado === 'cargando') {
    return (
      <Pantalla>
        <Texto tono="secundario">Cargando…</Texto>
      </Pantalla>
    );
  }
  return estado === 'dentro' ? <Portada /> : <Redirect href="/entrar" />;
}
