import { useRouter } from 'expo-router';

import { BotonIcono } from './BotonIcono';
import { Texto } from './Texto';

/** Botón de volver y título de pantalla, para las pantallas de segundo nivel. */
export function EncabezadoAtras({ titulo }: { titulo: string }) {
  const router = useRouter();
  return (
    <>
      <BotonIcono icono="atras" etiqueta="Volver" onPress={() => router.back()} />
      <Texto estilo="pantalla" encabezado>{titulo}</Texto>
    </>
  );
}
