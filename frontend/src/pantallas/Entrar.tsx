import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';

import { mensajeDeError } from '../api/errores';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { CampoTexto } from '../componentes/CampoTexto';
import { Pantalla } from '../componentes/Pantalla';
import { useSesion } from '../sesion/sesion';

export function Entrar() {
  const router = useRouter();
  const { entrar } = useSesion();
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const envio = useMutation({
    mutationFn: () => entrar({ correo: correo.trim(), contrasena }),
    onSuccess: () => router.replace('/'),
  });

  return (
    <Pantalla titulo="Entra en Forja">
      <CampoTexto
        etiqueta="Correo"
        valor={correo}
        alCambiar={setCorreo}
        keyboardType="email-address"
        autoComplete="email"
        textContentType="emailAddress"
      />
      <CampoTexto
        etiqueta="Contraseña"
        valor={contrasena}
        alCambiar={setContrasena}
        secureTextEntry
        autoComplete="current-password"
        textContentType="password"
      />
      {envio.isError ? <Aviso tipo="error" mensaje={mensajeDeError(envio.error)} /> : null}
      <Boton
        etiqueta="Entrar"
        etiquetaCargando="Entrando…"
        cargando={envio.isPending}
        deshabilitado={correo.trim() === '' || contrasena === ''}
        onPress={() => envio.mutate()}
      />
      <Boton variante="texto" etiqueta="Crear una cuenta" onPress={() => router.push('/registro')} />
    </Pantalla>
  );
}
