# Forja: fase 0 (competencia y datos)

Comprobado el 2026-11-15 con búsqueda web y peticiones directas a las APIs. **No he instalado ni probado las apps**: lo de la competencia sale de sus webs, centros de ayuda, fichas de tienda y reseñas. La prueba a mano (crear cuenta y pasar el onboarding) sigue pendiente para Gravl, Fitbod y BodyLeveling.

## Resumen

| Punto | Resultado | Efecto en el plan |
|---|---|---|
| ¿Alguna app elige ejercicios por proporciones? | **Sí, al menos una lo afirma** (BodyLeveling). Las de rutinas clásicas no. | Hueco de Forja baja de 4-5 a **4**. Objetivo total: **24**, no 25. |
| mercaapi | **En línea** (HTTP 200 con datos reales), MIT, último push 2026-07-28. No oficial. | Se mantiene, con la capa de adaptadores como estaba. |
| Lista de la AMA | Sin licencia clara de WADA. **Alternativa mejor: la lista española del BOE.** | Usar el BOE como fuente. |
| NØDopApp | **Solo medicamentos.** Excluye expresamente complementos. | Confirma el hueco del módulo de suplementos. |
| OpenPowerlifting | Datos (`meet-data/*.csv`) en **dominio público**; código AGPLv3. Piden atribución (no obligatoria). | Se usa el CSV; el código no se copia. |
| free-exercise-db | **Unlicense** (dominio público), 800+ ejercicios en JSON. | Catálogo base. |
| wger | Código AGPLv3; datos de ejercicios con **licencia por entrada** (en general CC-BY-SA). | Solo como respaldo, guardando licencia y autor por ejercicio. |

## 1. Competencia

### Apps de rutinas (no usan proporciones)
- **Fitbod:** puntúa cada ejercicio por recuperación muscular, objetivo, nivel, material, lesiones y preferencias. No menciona medidas corporales. [Algoritmo](https://fitbod.me/blog/fitbod-algorithm) · [FAQ](https://fitbod.me/faqs/)
- **Gravl:** usa onboarding, sexo, peso, edad, nivel y recuperación; puntúa los ejercicios con 8 subpuntuaciones comparando con gente parecida. Nada de longitudes de segmentos. [Ayuda](https://intercom.help/gravl/en/articles/10185534-gravl-essentials-set-up-your-fitness-plan) · [App Store](https://apps.apple.com/gb/app/id6450921637)
- **Alpha Progression:** objetivo, experiencia, frecuencia, duración, material y músculos prioritarios. Registra medidas, pero nada indica que las use para elegir ejercicios. [Guía](https://alphaprogression.com/en/blog/alpha-progression-guide)
- **JuggernautAI:** sexo, edad, altura, peso y experiencia. No se ve que pregunte longitudes de extremidades (el onboarding tiene 22 pasos y no están todos publicados). [Ayuda](https://help.jtsstrength.com/en/articles/3-how-juggernautai-is-individualized-to-you)
- **RP Hypertrophy:** plantillas y Meso Builder por prioridades; los ejercicios los eliges tú de una lista. [App Store](https://apps.apple.com/sg/app/id1555614554)
- **Dr. Muscle y Hevy:** no revisados en esta pasada. Hevy es sobre todo un registro de series, así que es poco probable que choque.

### Apps de escaneo corporal (aquí está la competencia real)
- **BodyLeveling** (iPhone, 7,99 €/mes o 24,99 €/año, web con versión en francés): tres fotos, 14 puntuaciones musculares, y dice mapear «inserciones, estructura y proporciones». Afirma que los ratios extremidad/torso deciden la palanca y que la palanca decide rangos de repeticiones y ángulos. **No explica el método** y su propia sección de entreno da un rango genérico de 8-15 repeticiones. [Web](https://bodyleveling.com/)
- **TrueForm AI:** genera programas a partir del escaneo, con análisis de proporciones y simetría. [App Store](https://apps.apple.com/us/app/trueform-ai-body-scanner/id6745799401)
- **Thelo:** el escaneo «BioScan» (desequilibrios, estructura y proporciones) alimenta su planificador; no se sabe si las proporciones cambian los ejercicios. Desde 9,99 $/semana. [Reseña](https://gainframe.app/blog/thelo-app-review/)
- **Pelaris, LeanLens, GainFrame:** escanean, pero no escriben programas. [Pelaris](https://pelaris.io/body-scanning/) · [LeanLens](https://leanlens.ai/)

### Qué queda de hueco
La idea «fotos → proporciones → rutina» **ya está en el mercado** como reclamo, así que no puedes venderla como nueva. Lo que no he visto en ninguna:
1. **Error medido y publicado**: ratio de la foto frente a cinta métrica en N personas, con el MAE en el README y en el CI.
2. **Reglas explicables y abiertas**: cada cambio de ejercicio cita la regla y la medida («fémur/torso = 0,92 > 0,85 → sentadilla hack»), y las reglas están en un ADR con sus fuentes.
3. **Android y gratis**: las tres que lo hacen son de iPhone y de pago.
4. **Dieta por precio en España + suplementos** en la misma app.

Nueva frase de venta propuesta: *«Las apps de escaneo te dicen que usan tus proporciones. Forja te enseña cuánto se equivoca midiéndolas y por qué te cambia cada ejercicio.»*

> [!warning] La premisa científica es débil
> No hay estudios revisados por pares que demuestren que elegir ejercicios por proporciones mejore los resultados. Hasta los umbrales difieren entre fuentes (fémur > 27,5 % de la altura frente a > 80 % del torso). [The Workout Mag](https://theworkoutmag.com/training/how-to-measure-femur-length-guide) · [Thibarmy](https://thibarmy.com/the-main-purpose-of-client-assessment-exercise-selection/). En Forja, las reglas por palancas deben presentarse como **heurísticas de entrenadores**, no como ciencia, y es mejor que afecten sobre todo a la comodidad y a la técnica (variante de sentadilla o de peso muerto) que a la cantidad de volumen.

## 2. Datos y licencias

### mercaapi
- `GET https://mercaapi.sgn.space/api/products/?limit=1` → **200** con un producto real (Petit de fresa Hacendado, 1,70 €).
- Repo `m0wer/mercaapi`: MIT (el README lo dice; GitHub no lo detecta en los metadatos), 12 estrellas, último push 2026-07-28, no archivado. Trae `docker-compose`, así que puedes **alojar tu propia instancia** en vez de depender de la suya.
- Sigue siendo no oficial: Mercadona puede cambiar su API interna. Alternativas si cae: `mercapy`, `merca_ex` y los scrapers de Apify (de pago). [Repo](https://github.com/m0wer/mercaapi) · [mercapy](https://github.com/jtayped/mercapy)

### Lista de sustancias prohibidas
- **WADA:** no he encontrado sus términos de reutilización (la página de términos no cargó). Solo piden avisar a `code@wada-ama.org` para traducirla.
- **España:** la lista vigente para 2026 la aprobó el CSD por resolución del 2 de diciembre de 2025, publicada en el **BOE núm. 295 del 9 de diciembre de 2025** ([BOE-A-2025-25042](https://www.boe.es/boe/dias/2025/12/09/pdfs/BOE-A-2025-25042.pdf)). El texto de la AMA aprobado en París el 15-11-2025 también salió en el BOE del 24-12-2025.
- **Propuesta:** usar el BOE como fuente. **(Interpretación propia, no asesoría legal):** el art. 13 de la Ley de Propiedad Intelectual excluye de protección las disposiciones legales y las resoluciones de los órganos públicos, así que una resolución del BOE se puede reutilizar. Guarda en la BD el identificador BOE y la fecha de cada versión.

### NØDopApp (CELAD)
Consulta si un **medicamento** contiene sustancias prohibidas y **no** informa de alimentos, complementos alimenticios, plantas ni homeopáticos. La propia CELAD avisa de que un complemento no controlado puede llevar sustancias prohibidas y recomienda sellos como Informed Sport o NSF. Eso es justo lo que cubriría el módulo de suplementos. [Vitónica](https://www.vitonica.com/equipamiento/nodopapp-la-aplicacion-que-te-pone-en-guardia-frente-al-dopaje-involuntario) · [Guía CELAD](https://celad.educacionfpydeportes.gob.es/en/dam/jcr:44c0a89c-5cda-4a07-b606-be3d2220acc2/guia-prevencion-dopaje-profesionales-sanitarios.pdf)

> [!warning] Cuidado con lo que promete el escáner
> Leer la etiqueta no detecta contaminación. La app debe decir «riesgo por ingredientes declarados + si tiene sello Informed Sport/NSF», nunca «libre de dopaje».

### OpenPowerlifting
README de `opl-data`: *«OpenPowerlifting data (`*.csv`) under `meet-data/` is contributed to the public domain»*. El código es AGPLv3, así que no copies su código, solo el CSV. Piden (sin obligar) una frase de atribución: ponla en la app y en el README. [Repo](https://gitlab.com/openpowerlifting/opl-data)

### free-exercise-db y wger
- **free-exercise-db:** Unlicense, 800+ ejercicios con `primaryMuscles`, `secondaryMuscles`, `equipment`, `level`, `mechanic`, `force`, instrucciones e imágenes. Algunos campos vienen a `null`. [Repo](https://github.com/yuhonas/free-exercise-db)
- **wger:** código AGPLv3; datos «Creative Commons, see individual entries», en la práctica CC-BY-SA 4.0 por ejercicio. Las imágenes tienen su propia fuente. Si lo usas, guarda `license` y `license_author` por fila y respeta el share-alike. [README](https://cdn.jsdelivr.net/gh/wger-project/wger@master/README.md)

## 3. Lo que queda de la fase 0
- Probar a mano Gravl, Fitbod y BodyLeveling (onboarding completo, capturas para `docs/competencia.md`).
- Escribir los ADR 0001-0005 y crear la plantilla `repo-base` con el CI vacío en verde.
- Escribir a `code@wada-ama.org` solo si al final quieres la lista internacional además de la del BOE.
