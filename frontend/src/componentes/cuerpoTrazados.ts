// Trazados del cuerpo copiados tal cual del boceto aprobado por Gabriel (Cuerpo.dc.html, 2026-11-17).
// No los retoques a mano: se validaron tras varias rondas. Lienzo de 300 × 600; cada cara dibuja la mitad
// izquierda y la repite en espejo.

export type Grupo =
  | 'hombros'
  | 'pecho'
  | 'brazos'
  | 'antebrazos'
  | 'abdomen'
  | 'cuadriceps'
  | 'aductores'
  | 'gemelos'
  | 'trapecio'
  | 'dorsal'
  | 'lumbar'
  | 'triceps'
  | 'gluteos'
  | 'isquios';

export const GRUPOS: readonly Grupo[] = [
  'hombros', 'pecho', 'brazos', 'antebrazos', 'abdomen', 'cuadriceps', 'aductores',
  'gemelos', 'trapecio', 'dorsal', 'lumbar', 'triceps', 'gluteos', 'isquios',
];

/** Nombre que se lee y se oye de cada grupo. */
export const NOMBRE_GRUPO: Record<Grupo, string> = {
  hombros: 'Hombros',
  pecho: 'Pecho',
  brazos: 'Bíceps',
  antebrazos: 'Antebrazos',
  abdomen: 'Abdomen',
  cuadriceps: 'Cuádriceps',
  aductores: 'Aductores',
  gemelos: 'Gemelos',
  trapecio: 'Trapecio',
  dorsal: 'Dorsal',
  lumbar: 'Espalda baja',
  triceps: 'Tríceps',
  gluteos: 'Glúteos',
  isquios: 'Isquiotibiales',
};

export const LIENZO = { ancho: 300, alto: 600 } as const;
export const ESPEJO = 'translate(300 0) scale(-1 1)';

const MANO =
  'M41 281 C 28 290, 12 304, 6 318 C 8 324, 14 322, 18 314 C 16 326, 22 332, 28 326 L 32 316 C 32 328, 38 332, 42 324 C 44 330, 50 330, 50 320 C 52 306, 56 294, 56 286 Z';
const PIE = 'M98 536 C 90 550, 74 566, 70 578 C 74 584, 106 584, 114 578 C 116 562, 116 548, 116 536 Z';
const HOMBROS =
  'M112 114 C 94 110, 70 116, 62 136 C 58 150, 62 164, 72 170 C 84 168, 94 164, 99 158 C 100 142, 104 126, 112 114 Z';
const BRAZO =
  'M72 170 C 64 176, 58 188, 58 200 C 58 208, 60 214, 64 218 L 78 220 C 78 204, 79 186, 80 168 C 77 169, 74 170, 72 170 Z M80 168 C 79 186, 78 204, 78 220 L 87 221 C 92 206, 97 190, 98 176 L 99 158 C 94 164, 88 167, 80 168 Z';
const ANTEBRAZOS =
  'M64 218 C 56 230, 48 252, 41 281 L 50 284 C 60 262, 70 240, 78 220 Z M78 220 L 87 221 C 88 232, 80 252, 56 286 L 50 284 C 60 262, 70 240, 78 220 Z';

export const CABEZA =
  'M150 20 C 170 20, 180 36, 179 56 C 178 74, 166 90, 150 92 C 134 90, 122 74, 121 56 C 120 36, 130 20, 150 20 Z';
export const CUELLO = 'M136 86 L 138 98 M 164 86 L 162 98';

export interface Cara {
  /** Manos, pies y rodillas: sin color de grupo. */
  piel: readonly string[];
  /** Detalle de la rodilla (rótula o corva), solo trazo. */
  rodilla: { tipo: 'elipse'; cx: number; cy: number; rx: number; ry: number } | { tipo: 'linea'; d: string };
  grupos: readonly { grupo: Grupo; d: string }[];
}

export const FRENTE: Cara = {
  piel: [
    MANO,
    PIE,
    'M100 418 C 96 428, 96 438, 100 444 C 110 448, 122 448, 130 444 C 134 436, 134 426, 132 416 Z',
  ],
  rodilla: { tipo: 'elipse', cx: 115, cy: 434, rx: 8, ry: 7 },
  grupos: [
    { grupo: 'trapecio', d: 'M138 96 C 132 104, 122 110, 112 114 C 124 116, 138 118, 150 120 L 150 102 Z' },
    { grupo: 'hombros', d: HOMBROS },
    {
      grupo: 'pecho',
      d: 'M150 120 C 138 116, 122 114, 112 114 C 106 126, 102 136, 100 146 C 118 142, 134 140, 150 142 Z M150 142 C 134 140, 118 142, 100 146 C 98 154, 98 160, 99 166 C 110 178, 128 182, 150 180 Z',
    },
    { grupo: 'brazos', d: BRAZO },
    { grupo: 'antebrazos', d: ANTEBRAZOS },
    {
      grupo: 'abdomen',
      d: 'M99 166 C 110 178, 120 182, 126 182 L 126 268 C 120 270, 114 272, 108 278 C 112 262, 110 236, 106 212 C 104 196, 100 180, 99 166 Z M127 183 C 134 182, 144 182, 150 183 L 150 205 L 127 206 Z M127 207 L 150 206 L 150 229 L 127 230 Z M127 231 L 150 230 L 150 251 L 127 252 Z M127 253 L 150 252 L 150 270 L 127 270 Z M108 280 C 114 274, 120 271, 127 271 L 150 271 L 150 312 C 136 304, 120 292, 108 280 Z',
    },
    {
      grupo: 'cuadriceps',
      d: 'M107 282 C 94 296, 84 330, 84 362 C 84 390, 92 410, 102 424 C 104 392, 106 352, 112 302 C 112 294, 110 286, 107 282 Z M114 296 C 108 350, 105 392, 103 425 C 110 431, 122 431, 128 425 C 128 384, 124 336, 119 298 C 117 295, 115 294, 114 296 Z',
    },
    {
      grupo: 'aductores',
      d: 'M120 296 C 130 300, 140 306, 149 314 C 148 350, 142 390, 134 418 C 132 422, 130 424, 129 424 C 129 384, 125 336, 120 296 Z',
    },
    {
      grupo: 'gemelos',
      d: 'M100 436 C 88 452, 84 476, 88 500 C 90 514, 94 526, 98 536 L 108 536 C 108 504, 110 470, 112 438 C 108 436, 104 436, 100 436 Z M112 438 C 110 470, 108 504, 108 536 L 116 536 C 124 510, 130 476, 128 450 C 126 442, 120 438, 112 438 Z',
    },
  ],
};

export const ESPALDA: Cara = {
  piel: [
    MANO,
    PIE,
    'M94 414 C 92 426, 94 438, 100 444 C 110 448, 124 448, 132 442 C 136 432, 136 422, 134 414 Z',
  ],
  rodilla: { tipo: 'linea', d: 'M104 434 C 110 438, 120 438, 128 433' },
  grupos: [
    {
      grupo: 'dorsal',
      d: 'M112 118 C 104 130, 100 146, 99 166 C 104 196, 108 228, 112 256 C 126 258, 140 258, 150 258 L 150 194 C 138 156, 124 128, 112 118 Z',
    },
    { grupo: 'trapecio', d: 'M150 88 C 140 98, 124 108, 112 114 C 124 124, 138 148, 150 190 Z' },
    { grupo: 'hombros', d: HOMBROS },
    { grupo: 'triceps', d: BRAZO },
    { grupo: 'antebrazos', d: ANTEBRAZOS },
    { grupo: 'lumbar', d: 'M112 258 L 150 258 L 150 294 C 136 292, 122 286, 110 278 C 112 270, 112 264, 112 258 Z' },
    {
      grupo: 'gluteos',
      d: 'M110 280 C 124 288, 138 294, 150 296 L 150 344 C 134 354, 112 352, 98 342 C 88 330, 88 306, 98 290 C 102 286, 106 282, 110 280 Z',
    },
    {
      grupo: 'isquios',
      d: 'M98 344 C 112 352, 120 354, 124 356 C 122 384, 118 408, 112 426 C 104 428, 96 424, 92 414 C 86 396, 86 368, 98 344 Z M124 356 C 134 356, 142 352, 149 346 C 148 376, 142 404, 134 424 C 126 428, 118 428, 112 426 C 118 408, 122 384, 124 356 Z',
    },
    {
      grupo: 'gemelos',
      d: 'M100 436 C 88 450, 84 476, 92 500 C 96 510, 102 516, 106 518 C 108 492, 110 464, 112 438 C 108 436, 104 436, 100 436 Z M112 438 C 110 464, 108 492, 106 518 C 114 514, 124 500, 128 480 C 130 464, 126 446, 120 440 C 118 438, 115 438, 112 438 Z M100 520 L 116 520 L 116 536 L 100 536 Z',
    },
  ],
};
