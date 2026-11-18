import { useState } from 'react';

import { mensajeDeError } from '../api/errores';
import { Boton } from '../componentes/Boton';
import { CampoTexto } from '../componentes/CampoTexto';
import { Confirmacion } from '../componentes/Confirmacion';
import { EncabezadoAtras } from '../componentes/EncabezadoAtras';
import { Pantalla } from '../componentes/Pantalla';
import { Texto } from '../componentes/Texto';
import { useBorrarCuenta } from '../privacidad/consultas';

/** Borrar la cuenta (RF-47): la contraseña se pide aquí y viaja solo en la petición; no se guarda ni se registra. */
export function BorrarCuenta() {
  const [contrasena, setContrasena] = useState('');
  const [confirmando, setConfirmando] = useState(false);
  const borrar = useBorrarCuenta();

  return (
    <Pantalla hueco="s4">
      <EncabezadoAtras titulo="Borrar mi cuenta" />
      <Texto>
        Se borra todo lo tuyo: tu cuenta, tu plan, tus sesiones y series, tus limitaciones, tus consentimientos y tus dispositivos. También
        lo que tengas guardado sin enviar en este móvil. No se puede deshacer.
      </Texto>
      <CampoTexto
        etiqueta="Contraseña"
        ayuda="La pedimos otra vez para confirmar que eres tú."
        valor={contrasena}
        alCambiar={setContrasena}
        secureTextEntry
        autoComplete="current-password"
        textContentType="password"
      />
      {confirmando ? (
        <Confirmacion
          mensaje="¿Seguro? Se borrará tu cuenta con todo lo que hay en ella y saldrás de Forja. No hay vuelta atrás."
          etiquetaConfirmar="Sí, borrar mi cuenta"
          etiquetaCargando="Borrando…"
          cargando={borrar.isPending}
          error={borrar.isError ? mensajeDeError(borrar.error) : undefined}
          alConfirmar={() => borrar.mutate(contrasena)}
          alCancelar={() => {
            borrar.reset();
            setConfirmando(false);
          }}
        />
      ) : (
        <Boton etiqueta="Borrar mi cuenta" deshabilitado={contrasena === ''} onPress={() => setConfirmando(true)} />
      )}
    </Pantalla>
  );
}
