import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';

import { mensajeDeError } from '../api/errores';
import { Aviso } from '../componentes/Aviso';
import { Boton } from '../componentes/Boton';
import { CampoTexto } from '../componentes/CampoTexto';
import { Pantalla } from '../componentes/Pantalla';
import { useSesion } from '../sesion/sesion';
import {
  type DatosRegistro,
  type ErroresRegistro,
  hayErrores,
  LONGITUD_MINIMA_CONTRASENA,
  validarRegistro,
} from '../sesion/validarRegistro';

const VACIO: DatosRegistro = { correo: '', contrasena: '', fechaNacimiento: '' };

export function Registro() {
  const router = useRouter();
  const { api } = useSesion();
  const [datos, setDatos] = useState<DatosRegistro>(VACIO);
  const [errores, setErrores] = useState<ErroresRegistro>({});
  const alta = useMutation({
    mutationFn: () => api.registrar({ ...datos, correo: datos.correo.trim() }),
    onSuccess: () => router.replace('/revisa-tu-correo'),
  });

  const cambiar = (campo: keyof DatosRegistro) => (valor: string) => setDatos({ ...datos, [campo]: valor });

  function crearCuenta() {
    const encontrados = validarRegistro(datos);
    setErrores(encontrados);
    if (!hayErrores(encontrados)) {
      alta.mutate();
    }
  }

  return (
    <Pantalla titulo="Crea tu cuenta">
      <CampoTexto
        etiqueta="Correo"
        valor={datos.correo}
        alCambiar={cambiar('correo')}
        error={errores.correo}
        keyboardType="email-address"
        autoComplete="email"
        textContentType="emailAddress"
      />
      <CampoTexto
        etiqueta="Contraseña"
        valor={datos.contrasena}
        alCambiar={cambiar('contrasena')}
        error={errores.contrasena}
        ayuda={`Mínimo ${LONGITUD_MINIMA_CONTRASENA} caracteres.`}
        secureTextEntry
        autoComplete="new-password"
        textContentType="newPassword"
      />
      <CampoTexto
        etiqueta="Fecha de nacimiento"
        valor={datos.fechaNacimiento}
        alCambiar={cambiar('fechaNacimiento')}
        error={errores.fechaNacimiento}
        ayuda="AAAA-MM-DD. Solo la usamos para comprobar que tienes 18 años o más, y guardamos el año."
        keyboardType="numbers-and-punctuation"
        autoComplete="birthdate-full"
      />
      {alta.isError ? <Aviso tipo="error" mensaje={mensajeDeError(alta.error)} /> : null}
      <Boton etiqueta="Crear cuenta" etiquetaCargando="Creando…" cargando={alta.isPending} onPress={crearCuenta} />
      <Boton variante="texto" etiqueta="Ya tengo cuenta" onPress={() => router.replace('/entrar')} />
    </Pantalla>
  );
}
