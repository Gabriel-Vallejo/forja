import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';

import { mensajeDeError } from '../api/errores';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { CampoTexto } from '../componentes/CampoTexto';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { useSesion } from '../sesion/sesion';

export function Recuperar() {
  const router = useRouter();
  const { api } = useSesion();
  const [correo, setCorreo] = useState('');
  const envio = useMutation({ mutationFn: () => api.recuperar(correo.trim()) });

  if (envio.isSuccess) {
    return (
      <Pantalla titulo="Revisa tu correo">
        <Texto>Si hay una cuenta con ese correo, te hemos mandado un enlace para elegir una contraseña nueva. Caduca en 30 minutos.</Texto>
        <Boton etiqueta="Ir a entrar" onPress={() => router.replace('/entrar')} />
      </Pantalla>
    );
  }
  return (
    <Pantalla titulo="Cambia tu contraseña">
      <Texto>Escribe el correo de tu cuenta y te mandamos un enlace para elegir una nueva.</Texto>
      <CampoTexto
        etiqueta="Correo"
        valor={correo}
        alCambiar={setCorreo}
        keyboardType="email-address"
        autoComplete="email"
        textContentType="emailAddress"
      />
      {envio.isError ? <Aviso tipo="error" mensaje={mensajeDeError(envio.error)} /> : null}
      <Boton
        etiqueta="Mandar el enlace"
        etiquetaCargando="Mandando…"
        cargando={envio.isPending}
        deshabilitado={correo.trim() === ''}
        onPress={() => envio.mutate()}
      />
      <Boton variante="texto" etiqueta="Volver" onPress={() => router.replace('/entrar')} />
    </Pantalla>
  );
}
