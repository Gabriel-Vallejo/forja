// Los tres tests caseros de movilidad (RF-08, ADR 0007): cómo se hacen en casa y una pregunta cerrada por tramo.
// [propuesta] Los umbrales son propios: ni el ADR ni docs/ los fijan. Rodilla a la pared: 8 y 12 cm, con una franja
// media de 4 cm, el doble del error mínimo detectable del test (1,6-1,9 cm, ADR 0007). Rotación de cadera: 30 y 40°,
// por debajo de los ~35-45° de referencia de rotación interna y externa. Hombro: si los brazos llegan al suelo con la
// zona lumbar pegada. Es heurística de entrenadores, no ciencia, y la app lo dice.
import type { TestMovilidad, Tramo } from '../api/esquemas';

export interface DefinicionTest {
  test: TestMovilidad;
  titulo: string;
  /** Qué mide, en palabras de gimnasio. */
  mide: string;
  pasos: readonly string[];
  pregunta: string;
  /** Una respuesta cerrada por tramo, en el orden limitado, medio y bueno. */
  opciones: Readonly<Record<Tramo, string>>;
}

export const TESTS: readonly DefinicionTest[] = [
  {
    test: 'tobillo',
    titulo: 'Rodilla a la pared',
    mide: 'Cuánto se dobla tu tobillo hacia delante. Decide la variante de sentadilla.',
    pasos: [
      'Descalzo, de cara a una pared, con un pie delante y el dedo gordo a unos centímetros de ella.',
      'Lleva la rodilla hacia delante hasta tocar la pared, sin levantar el talón y sin que la rodilla se vaya hacia dentro.',
      'Si llegas, aleja el pie un poco y repite. Busca la distancia más larga a la que aún tocas con el talón apoyado.',
      'Mide con una regla del dedo gordo a la pared. Hazlo con los dos pies y quédate con el peor.',
    ],
    pregunta: '¿A qué distancia de la pared llega tu pie peor?',
    opciones: { limitado: 'Menos de 8 cm', medio: 'Entre 8 y 12 cm', bueno: 'Más de 12 cm' },
  },
  {
    test: 'cadera',
    titulo: 'Rotación de cadera sentado',
    mide: 'Cuánto gira tu cadera. Decide la postura de pies del peso muerto.',
    pasos: [
      'Siéntate en el borde de una mesa o una silla alta, con las rodillas dobladas a 90° y los pies colgando.',
      'Sin mover el muslo ni levantar la cadera del asiento, gira la espinilla para llevar el pie hacia fuera y luego hacia dentro.',
      'Compara cada giro con la diagonal de 45°: la mitad de un ángulo recto, como la esquina de un folio doblado.',
      'Hazlo con las dos piernas y quédate con el giro más corto.',
    ],
    pregunta: '¿Hasta dónde llega tu giro más corto?',
    opciones: {
      limitado: 'Menos de 30°: lejos de la diagonal',
      medio: 'Entre 30 y 40°: cerca de la diagonal',
      bueno: '40° o más: llega a la diagonal',
    },
  },
  {
    test: 'hombro',
    titulo: 'Flexión de hombro tumbado',
    mide: 'Cuánto suben tus brazos por encima de la cabeza. Decide el press de hombro.',
    pasos: [
      'Túmbate boca arriba con las rodillas dobladas y los pies apoyados.',
      'Pega la zona lumbar al suelo, sin hueco para meter la mano, y mantenla así todo el rato.',
      'Con los codos estirados y las palmas mirándose, sube los brazos por delante hasta llevarlos por encima de la cabeza, hacia el suelo.',
    ],
    pregunta: '¿Hasta dónde llegan tus brazos sin despegar la zona lumbar?',
    opciones: {
      limitado: 'Se quedan a más de un palmo del suelo, o solo llegan arqueando la espalda',
      medio: 'Se quedan cerca, a menos de un palmo',
      bueno: 'Tocan el suelo con los codos estirados',
    },
  },
];

export const NOMBRE_TRAMO: Readonly<Record<Tramo, string>> = { limitado: 'Limitado', medio: 'Medio', bueno: 'Bueno' };

export const INTRO =
  'Tres pruebas de un par de minutos, en casa y sin material. No hace falta medir exacto: elige la respuesta que más se parezca. Si una prueba te molesta, párala y déjala sin marcar.';

export const HEURISTICA =
  'Los tramos y lo que cambian en tu plan son una heurística de entrenadores, no ciencia: sirven para elegir variantes de ejercicios, no dicen nada de tu salud.';

export const NOTA_MOVILIDAD = 'Los tests cuentan al generar o regenerar tu plan: el plan que tienes ahora no cambia solo.';
