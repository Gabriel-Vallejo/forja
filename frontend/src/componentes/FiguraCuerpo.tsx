import { Ellipse, G, Path } from 'react-native-svg';

import { useTheme } from '../theme';
import { CABEZA, type Cara, CUELLO, ESPEJO, type Grupo } from './cuerpoTrazados';

const LINEA = 1.8;
const LINEA_SELECCION = 3.5;
const LINEA_DETALLE = 1.4;
const LINEA_CABEZA = 2;
const MITADES = ['', ESPEJO] as const;

export type ColoresCuerpo = Partial<Record<Grupo, string>>;

interface PropsFiguraCuerpo {
  cara: Cara;
  colores?: ColoresCuerpo;
  seleccion?: Grupo | '';
  alTocar?: (grupo: Grupo) => void;
  /** Sufijo de los `testID` («frente» o «espalda»). */
  lado: string;
}

/** Una cara del cuerpo como grupo SVG en el lienzo de 300 × 600: se puede meter en cualquier `<Svg>`. */
export function FiguraCuerpo({ cara, colores = {}, seleccion = '', alTocar, lado }: PropsFiguraCuerpo) {
  const tema = useTheme();
  const { piel, trazo, trazoSeleccion, sinColor } = tema.cuerpo;
  const rodilla = cara.rodilla;

  return (
    <G>
      {MITADES.map((transformacion, mitad) => (
        <G key={mitad} transform={transformacion || undefined} strokeLinejoin="round">
          {cara.piel.map((d) => (
            <Path key={d} d={d} fill={piel} stroke={trazo} strokeWidth={LINEA} />
          ))}
          {rodilla.tipo === 'elipse' ? (
            <Ellipse cx={rodilla.cx} cy={rodilla.cy} rx={rodilla.rx} ry={rodilla.ry} fill="none" stroke={trazo} strokeWidth={LINEA_DETALLE} />
          ) : (
            <Path d={rodilla.d} fill="none" stroke={trazo} strokeWidth={LINEA_DETALLE} strokeLinecap="round" />
          )}
          {cara.grupos.map(({ grupo, d }) => {
            const elegido = grupo === seleccion;
            return (
              <Path
                key={grupo}
                testID={`grupo-${grupo}-${lado}`}
                d={d}
                fill={colores[grupo] ?? sinColor}
                stroke={elegido ? trazoSeleccion : trazo}
                strokeWidth={elegido ? LINEA_SELECCION : LINEA}
                onPress={alTocar ? () => alTocar(grupo) : undefined}
              />
            );
          })}
        </G>
      ))}
      <Path d={CABEZA} fill={piel} stroke={trazo} strokeWidth={LINEA_CABEZA} />
      <Path d={CUELLO} stroke={trazo} strokeWidth={LINEA_CABEZA} strokeLinecap="round" />
    </G>
  );
}
