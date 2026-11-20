/**
 * DOI de las citas que el backend pone en la fuente de «por qué este ejercicio» (`ReglasExplicadas.java`),
 * copiados de docs/referencias.md, donde están verificadas.
 *
 * shortcut: el mapa vive en la app; una cita nueva del backend sale como texto hasta que se añada aquí.
 * Cuando haya más de una decena, que la API devuelva el DOI junto a la fuente.
 */
const DOI: Record<string, string> = {
  'Kinoshita 2023': '10.3389/fphys.2023.1272106',
  'Kobayashi 2026': '10.1080/02640414.2026.2741751',
  'Maeo 2021': '10.1249/MSS.0000000000002523',
  'Maeo 2023': '10.1080/17461391.2022.2100279',
  'Pedrosa 2026': '10.1055/a-2733-7605',
  'Pelland 2026': '10.1007/s40279-025-02344-w',
  'Schoenfeld 2019': '10.1080/02640414.2018.1555906',
};

const CITA = new RegExp(`(${Object.keys(DOI).join('|')})`);

export interface TrozoFuente {
  texto: string;
  /** Solo en las citas con artículo: el DOI como enlace. */
  url?: string;
}

/** Parte la fuente en texto y citas, para que cada cita sea un enlace a su artículo. */
export function trozosFuente(fuente: string): TrozoFuente[] {
  return fuente
    .split(CITA)
    .filter((texto) => texto !== '')
    .map((texto) => {
      const doi = DOI[texto];
      return doi ? { texto, url: `https://doi.org/${doi}` } : { texto };
    });
}
