import { Redirect } from 'expo-router';

import { Entrar } from '../src/pantallas/Entrar';
import { useSesion } from '../src/sesion/sesion';

export default function RutaEntrar() {
  return useSesion().estado === 'dentro' ? <Redirect href="/" /> : <Entrar />;
}
