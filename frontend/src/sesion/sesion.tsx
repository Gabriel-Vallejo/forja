import { useQueryClient } from '@tanstack/react-query';
import { createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import type { ApiForja, Credenciales } from '../api/forja';

export type EstadoSesion = 'cargando' | 'fuera' | 'dentro';

interface ValorSesion {
  estado: EstadoSesion;
  api: ApiForja;
  entrar(credenciales: Credenciales): Promise<void>;
  salir(): Promise<void>;
  /**
   * Borra la cuenta (pide la contraseña al backend) y sale. `limpiarLocal` corre tras borrar y recibe el usuario, que
   * hay que leer antes porque al borrar la sesión local ya no hay token de donde sacarlo. Si falla, se sale igual.
   */
  borrarCuenta(contrasena: string, limpiarLocal?: (usuario: string | null) => Promise<void>): Promise<void>;
}

const ContextoSesion = createContext<ValorSesion | null>(null);

interface PropsProveedorSesion {
  api: ApiForja;
  children: ReactNode;
}

export function ProveedorSesion({ api, children }: PropsProveedorSesion) {
  const consultas = useQueryClient();
  const [estado, setEstado] = useState<EstadoSesion>('cargando');

  useEffect(() => {
    let vigente = true;
    // Con un refresco guardado se entra directo: el primer 401 renueva el acceso (cliente.ts).
    api
      .haySesionGuardada()
      .catch(() => false)
      .then((hay) => {
        if (vigente) setEstado(hay ? 'dentro' : 'fuera');
      });
    const dejarDeEscuchar = api.alCaducar(() => {
      consultas.clear();
      setEstado('fuera');
    });
    return () => {
      vigente = false;
      dejarDeEscuchar();
    };
  }, [api, consultas]);

  const entrar = useCallback(
    async (credenciales: Credenciales) => {
      await api.entrar(credenciales);
      setEstado('dentro');
    },
    [api],
  );

  const salir = useCallback(async () => {
    try {
      await api.cerrarSesion();
    } finally {
      consultas.clear();
      setEstado('fuera');
    }
  }, [api, consultas]);

  const borrarCuenta = useCallback(
    async (contrasena: string, limpiarLocal?: (usuario: string | null) => Promise<void>) => {
      const usuario = await api.usuarioActual();
      await api.borrarCuenta(contrasena);
      try {
        await limpiarLocal?.(usuario);
      } finally {
        consultas.clear();
        setEstado('fuera');
      }
    },
    [api, consultas],
  );

  const valor = useMemo(() => ({ estado, api, entrar, salir, borrarCuenta }), [estado, api, entrar, salir, borrarCuenta]);
  return <ContextoSesion.Provider value={valor}>{children}</ContextoSesion.Provider>;
}

export function useSesion(): ValorSesion {
  const valor = useContext(ContextoSesion);
  if (!valor) {
    throw new Error('useSesion necesita un ProveedorSesion por encima.');
  }
  return valor;
}
