# 0005. Fuentes de datos y cómo se cargan

- **Estado:** propuesta
- **Fecha:** 2026-11-15

## Contexto
Forja necesita cinco catálogos que no puede inventar: ejercicios, estándares de fuerza, alimentos, precios y sustancias o alertas. Cada uno tiene su licencia y su riesgo de desaparecer. La regla del proyecto es comprobar la licencia antes de usar nada, apuntarla y no copiar código AGPL ([CLAUDE.md](../../CLAUDE.md) §4). Las licencias se comprobaron en la fase 0 ([fase-0.md](../fase-0.md) §2).

## Decisión

### Fuentes
| Dato | Fuente principal | Licencia | Respaldo | Cómo entra |
|---|---|---|---|---|
| Ejercicios | **free-exercise-db** | Unlicense | wger (CC-BY-SA por ejercicio) | Script de carga a `ejercicio*` + etiquetado biomecánico propio |
| Estándares de fuerza | **OpenPowerlifting**, solo el CSV de `meet-data/` | Dominio público (el código es AGPLv3: no se copia) | — | Percentiles calculados fuera de línea → `estandar_fuerza` |
| Alimentos y nutrición | **Open Food Facts** | Base de datos ODbL, contenidos DbCL, imágenes CC BY-SA | Etiqueta nutricional por foto (OCR) | Por código de barras, a demanda, con caché en `nutricion_100g` |
| Precios | **mercaapi autoalojado** (MIT, no oficial) | MIT (el código); los datos son de Mercadona | Precios aportados por usuarios con foto del lineal | Detrás de la interfaz `FuentePrecios` |
| Sustancias prohibidas | **Lista española del BOE** (BOE-A-2025-25042 para 2026), **no** la web de la AMA | Resolución pública (art. 13 LPI, interpretación propia) | — | Carga anual a `sustancia_prohibida` + `version_lista` |
| Alertas de complementos | **CELAD y AESAN** (PDF) | Información del sector público **[verificar el aviso legal de cada web]** | — | Job programado; extracción de tablas → `alerta` con el PDF de origen |

### Reglas para todas las fuentes
1. **Procedencia en cada fila:** `fuente`, `id_externo`, `version` o `fecha_descarga` y, si la licencia lo pide, `licencia` y `autor` (wger por ejercicio, imágenes de OFF).
2. **Instantáneas versionadas:** los ficheros de carga (CSV/JSON) se descargan con un script reproducible en `database/` que guarda la URL, el hash y la fecha. Los volcados grandes no se suben al repo: se apunta cómo obtenerlos.
3. **Atribución visible:** una pantalla «Datos y licencias» en la app y una sección en el README, con:
   - «© Open Food Facts contributors»;
   - la frase que pide OpenPowerlifting;
   - el autor de cada ejercicio de wger;
   - el identificador del BOE.
4. **Adaptadores, no dependencias directas:** precios, alimentos y alertas entran por interfaces (`FuentePrecios`, `FuenteNutricion` y `FuenteAlertas`). Si mercaapi cae, se cambia el adaptador y no el dominio.
5. **Share-alike vigilado:** si algún día publicamos una base de datos derivada de OFF, tiene que ir en ODbL, y la de wger en CC-BY-SA. Usarlas dentro de la app no obliga a cambiar la licencia MIT del código.
6. **Sesgo declarado:** los percentiles de OpenPowerlifting salen de levantadores de competición y están sesgados hacia arriba. La app lo dice junto al mapa muscular.

## Alternativas
| Opción | A favor | En contra |
|---|---|---|
| Base de ejercicios de wger como principal | Más ejercicios e imágenes | CC-BY-SA por ejercicio: más trabajo de atribución; free-exercise-db es dominio público |
| ExerciseDB y otras APIs de pago | Vídeos y GIF | De pago y con licencia restrictiva |
| Lista de la AMA desde wada-ama.org | Internacional | Sin términos de reutilización claros (fase 0); la del BOE es la misma lista, oficial en España y reutilizable |
| Scraping de supermercados | Más cadenas | Riesgo legal y técnico; se sustituye por precios aportados por usuarios |
| Usar la instancia pública de mercaapi | Cero mantenimiento | Depender de un tercero no oficial; autoalojarla es un contenedor más |
| BEDCA (base española de composición de alimentos) | Datos oficiales de alimentos genéricos | No tiene productos de marca con código de barras; puede complementar para alimentos frescos **[evaluar en la fase 3]** |

## Consecuencias
- **Gana:** todo dato tiene origen, licencia y versión, y se puede explicar en una entrevista o ante una reclamación. Además, la caída de una fuente no rompe la app.
- **Cuesta:** scripts de carga y su mantenimiento anual (lista del BOE, actualización de OpenPowerlifting), la pantalla de atribución y las columnas de procedencia en el esquema.
- **Riesgo que queda:** mercaapi tira de la API interna de Mercadona, que no es pública. Es una zona gris: se mantiene autoalojado y desactivable con un flag, y los precios de usuarios son el camino de largo plazo.
- **A cambiar:** [CONTEXT.md](../../CONTEXT.md) §1.2.C dice «lista prohibida de la AMA». Pasa a «lista de sustancias prohibidas (la de la AMA, en su publicación del BOE)».

## Fuentes
- [Open Food Facts: licencias](https://openfoodfacts.github.io/openfoodfacts-server/api/tutorials/license-be-on-the-legal-side)
- [BOE-A-2025-25042](https://www.boe.es/boe/dias/2025/12/09/pdfs/BOE-A-2025-25042.pdf)
- [opl-data](https://gitlab.com/openpowerlifting/opl-data) · [free-exercise-db](https://github.com/yuhonas/free-exercise-db) · [mercaapi](https://github.com/m0wer/mercaapi)
