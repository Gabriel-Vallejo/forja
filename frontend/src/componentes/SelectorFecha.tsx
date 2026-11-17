import { CampoTexto } from './CampoTexto';

export interface PropsSelectorFecha {
  etiqueta: string;
  /** Fecha como `AAAA-MM-DD`, o vacía si aún no se ha elegido. */
  valor: string;
  alCambiar: (valor: string) => void;
  error?: string;
  ayuda?: string;
}

/**
 * En la web (PC con Tauri) no hay selector nativo: la fecha se escribe a mano.
 * En el móvil manda `SelectorFecha.native.tsx`, con la ruleta de día, mes y año.
 */
export function SelectorFecha({ ayuda, ...resto }: PropsSelectorFecha) {
  return (
    <CampoTexto
      {...resto}
      ayuda={ayuda ? `AAAA-MM-DD. ${ayuda}` : 'AAAA-MM-DD.'}
      keyboardType="numbers-and-punctuation"
      autoComplete="birthdate-full"
    />
  );
}
