import { useRouter } from 'expo-router';

import { BotonIcono } from '../componentes/BotonIcono';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';

/** Hueco de lo que llega en otro bloque (la sesión en curso, ajustes…). */
export function Proximamente({ titulo }: { titulo: string }) {
  const router = useRouter();
  return (
    <Pantalla hueco="s4">
      <BotonIcono icono="atras" etiqueta="Volver" onPress={() => router.back()} />
      <Texto estilo="pantalla" encabezado>{titulo}</Texto>
      <Texto estilo="heading">Próximamente</Texto>
      <Texto tono="secundario">Esta parte llega en un próximo bloque.</Texto>
    </Pantalla>
  );
}
