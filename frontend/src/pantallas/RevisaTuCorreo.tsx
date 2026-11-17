import { useRouter } from 'expo-router';

import { Boton } from '../componentes/Boton';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';

export function RevisaTuCorreo() {
  const router = useRouter();

  return (
    <Pantalla titulo="Revisa tu correo">
      <Texto>Si el correo es válido, te hemos mandado un enlace para verificar la cuenta. Caduca en 24 horas.</Texto>
      <Texto>Ábrelo y vuelve aquí para entrar.</Texto>
      <Boton etiqueta="Ir a entrar" onPress={() => router.replace('/entrar')} />
    </Pantalla>
  );
}
