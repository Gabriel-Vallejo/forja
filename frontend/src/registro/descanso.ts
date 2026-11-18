import { useEffect, useState } from 'react';

// Temporizador de descanso entre series (RF-21). Guarda la hora de fin, no un contador: si la app pasa a segundo
// plano y los intervalos se paran, al volver lo que queda sigue siendo exacto.

export const DESCANSO_MS = 2 * 60 * 1000;
export const SUMA_DESCANSO_MS = 30 * 1000;
/** Cada cuánto se repinta; no afecta a la exactitud. */
const REPINTADO_MS = 500;

export interface Descanso {
  /** Milisegundos que faltan, o null sin descanso en marcha. */
  restanteMs: number | null;
  empezar(): void;
  sumar(): void;
  saltar(): void;
}

export function useDescanso(reloj: () => number = Date.now): Descanso {
  const [finEn, setFinEn] = useState<number | null>(null);
  const [, setRepintado] = useState(0);

  useEffect(() => {
    if (finEn === null) return;
    const id = setInterval(() => {
      if (reloj() >= finEn) setFinEn(null);
      else setRepintado((n) => n + 1);
    }, REPINTADO_MS);
    return () => clearInterval(id);
  }, [finEn, reloj]);

  return {
    restanteMs: finEn === null ? null : Math.max(0, finEn - reloj()),
    empezar: () => setFinEn(reloj() + DESCANSO_MS),
    sumar: () => setFinEn((fin) => Math.max(fin ?? 0, reloj()) + SUMA_DESCANSO_MS),
    saltar: () => setFinEn(null),
  };
}

/** «1:30»: minutos y segundos, redondeando los segundos hacia arriba (no enseña 0:00 mientras queda algo). */
export function formatoDescanso(ms: number): string {
  const total = Math.ceil(ms / 1000);
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`;
}
