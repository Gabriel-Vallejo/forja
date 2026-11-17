import { Seleccion } from './Seleccion';

const OPCIONES = [
  { valor: 'si', etiqueta: 'Sí' },
  { valor: 'no', etiqueta: 'No' },
] as const;

interface PropsSiNo {
  etiqueta: string;
  valor: boolean | undefined;
  alCambiar: (valor: boolean) => void;
  ayuda?: string;
}

/** Pregunta de sí o no; hasta que se contesta no tiene valor (nunca un «no» por omisión). */
export function SiNo({ etiqueta, valor, alCambiar, ayuda }: PropsSiNo) {
  return (
    <Seleccion
      etiqueta={etiqueta}
      ayuda={ayuda}
      opciones={OPCIONES}
      valor={valor === undefined ? undefined : valor ? 'si' : 'no'}
      alCambiar={(v) => v !== undefined && alCambiar(v === 'si')}
    />
  );
}
