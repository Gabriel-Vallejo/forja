import { z } from 'zod';

const esquemaCarga = z.object({ sub: z.uuid() });

/**
 * El usuario (`sub`) del token de acceso (ADR 0009 §2). La firma no se comprueba aquí: la comprueba el backend en
 * cada petición; en el móvil solo sirve para saber de quién son las sesiones del outbox.
 */
export function usuarioDeToken(token: string): string | null {
  const carga = token.split('.')[1];
  if (!carga) return null;
  try {
    const json = atob(carga.replace(/-/g, '+').replace(/_/g, '/'));
    const resultado = esquemaCarga.safeParse(JSON.parse(json));
    return resultado.success ? resultado.data.sub : null;
  } catch {
    return null;
  }
}
