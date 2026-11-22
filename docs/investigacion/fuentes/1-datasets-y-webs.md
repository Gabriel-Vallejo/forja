> Informe de investigación del 2026-11-22 (agente de investigación, revisado por Claude). Lo que pone «comprobado» se leyó en la fuente; lo demás, como se indica. Resumen y recomendación: [../estandares-fuerza.md](../estandares-fuerza.md).

# Investigación 1: datasets abiertos (tipo 1) y webs de estándares (tipo 5)

Fecha: 2026-11-22. Encargo: `docs/investigacion/estandares-fuerza-encargo.md`, tipos de fuente 1 y 5.

Convenciones:
- **Comprobado** quiere decir que lo he leído en la fuente primaria (repo, licencia, términos) o que he calculado la cifra yo sobre los datos descargados. **No comprobado** quiere decir que sale de un buscador o de un resumen y no lo he podido abrir.
- Las citas literales tienen menos de 15 palabras.
- Se distingue siempre la **licencia de los datos** de los **términos de la web**.
- No he raspado ninguna web que lo prohíba. Solo he descargado datos con licencia abierta (OpenStreetlifting CC0, OpenWeightlifting dominio público, Zenodo CC BY) y un dataset de Kaggle (CC BY, según quien lo subió) para ver qué columnas trae. De las webs de estándares he leído páginas sueltas, nada en bloque.

---

## Resumen

| Fuente | Tipo | Ejercicios útiles para Forja | Licencia (datos) | Veredicto |
|---|---|---|---|---|
| **OpenStreetlifting** | 1 | Dominadas con lastre, fondos con lastre (y muscle-up y sentadilla) | **CC0 1.0** | **Usar con aviso** |
| OpenWeightlifting | 1 | Arrancada y dos tiempos (no están en el catálogo) | Dominio público (renuncia de derechos) | Descartar por ahora (no cubre lo pedido) |
| CrossFit Athletes (Kaggle) | 1 | Dominadas (reps), sentadilla, peso muerto, cargada y envión, arrancada | CC BY 4.0 según quien lo subió; procede de crossfit.com, cuyos términos prohíben el scraping | Descartar |
| Mendeley, dominadas, Murcia (n = 84) | 1 | Dominadas: reps y 1RM con lastre | CC BY 4.0 | Solo para comparar o validar |
| Zenodo, Aalborg, 3-7RM (n = 28) | 1 | 15 ejercicios (curl, jalón, remo, elevaciones laterales…) | CC BY 4.0 | Solo para comparar |
| **FitnessVolt, API de estándares** | 5 (con datos) | Press militar, press inclinado, dominadas, fondos, remo Pendlay, sentadilla frontal… | Dice «CC BY 4.0», pero no se sabe de dónde salen los datos de gimnasio | Solo para comparar hasta que confirmen la procedencia |
| Strength Level | 5 | 287 ejercicios | Licencia de pago y por escrito, incluso para apps gratis | Solo para comparar (enlazar sí está permitido) |
| Symmetric Strength | 5 | 14 levantamientos | Términos: no copiar ni raspar | Solo para comparar |
| ExRx / Lon Kilgore | 5 | Press, banca, sentadilla, peso muerto, cargada y arrancada de fuerza | Términos: no copiar ni republicar sin permiso escrito | Solo para comparar |

Recomendación de mi parte:
1. **Dominadas y fondos con lastre:** OpenStreetlifting es el único dataset que he encontrado con marcas individuales, licencia limpia (CC0), sexo y peso corporal, y un tamaño útil: 4.586 atletas y unas 6.900 dominadas con lastre de hombres. Encaja igual que OpenPowerlifting: es una población de **competidores**, no de gimnasio.
2. **Press militar, press inclinado, remo y el resto:** no he encontrado ningún dataset abierto con marcas individuales. La única tabla con licencia abierta declarada es la API de FitnessVolt, y su procedencia es dudosa (ver la ficha). Antes de usarla hay que pedirles por escrito que confirmen que tienen derecho a licenciar los datos de «Symmetric Strength».
3. **Las webs de estándares** (Strength Level, Symmetric Strength, ExRx) no dejan copiar sus tablas. Strength Level dice de forma explícita que tampoco vale usarlas «por detrás» ni a cambio de citarles. Sirven como referencia para comparar a mano y para enlazarlas.

---

## Tipo 1: datasets abiertos con marcas individuales

### 1.1 OpenStreetlifting

- **Primaria:** https://github.com/openstreetlifting/openstreetlifting (datos en `backend/data/`). Web: https://openstreetlifting.org. API: https://api.openstreetlifting.org/api/v1. Leído en el commit `f64f0cd457` (2026-11-20).
- **Tipo:** 1.
- **Licencia (comprobada):**
  - Datos: CC0 1.0. Texto de `LICENSE-DATA`: «You may copy, modify and redistribute it, including commercially». Lo mismo vale para lo que devuelve la API (capítulo `LICENSING.md`).
  - Código: AGPLv3. No hay que tocarlo, igual que con OpenPowerlifting.
  - CC0 no cubre el material original enlazado como fuente (fotos, vídeos, posts de Instagram), solo los datos.
  - El proyecto tiene una página de datos personales (`PERSONAL_DATA.md`): los atletas pueden pedir que se borre su nombre. Para Forja no importa porque solo guardaríamos agregados, sin nombres.
- **Población (calculada por mí sobre los 296 `entries.csv`):**
  - 10.290 entradas, de las que 9.336 compitieron, de 4.586 atletas distintos.
  - 296 competiciones de 39 organizadores y federaciones entre 2015 y 2026, la mayoría de 2023 a 2026.
  - Países con más entradas: Italia (1.973), Francia (1.889), Alemania (1.245), Polonia (1.138), España (861) y EE. UU. (405).
  - Por sexo: 8.043 entradas de hombres y 2.247 de mujeres.
  - Son competidores de streetlifting: una población muy por encima de la media de un gimnasio.
- **Variables:**
  - Sexo, categoría de peso y peso corporal (`BodyweightKg`). En 2.687 entradas el peso está «recuperado» a partir de la puntuación RIS publicada, no medido en el pesaje.
  - País.
  - Edad (`Age`), solo en 545 entradas.
  - Tres intentos y el mejor de muscle-up, dominada, fondo y sentadilla, en **kg de lastre añadido**.
  - Total, RIS y estado (`competed`, `disqualified`, `no_show`).
- **Método de medida:** 1RM de competición con lastre, con jueces. Las reglas de técnica son las de cada federación y no las he comprobado una a una; por ejemplo, no sé si todas exigen agarre prono. Hay dos modalidades: «PD» o Classic (dominada y fondo) y «MPDS» (muscle-up, dominada, fondo y sentadilla).
- **Ejercicios útiles y n (calculado por mí; entradas que compitieron con lastre > 0, y entre paréntesis las que tienen peso corporal):**

  | Ejercicio | Hombres | Mujeres | Mediana de lastre, H / M |
  |---|---|---|---|
  | Dominada con lastre | 6.897 (5.469) | 1.938 (1.540) | 65 kg / 30 kg |
  | Fondo con lastre | 6.590 (5.421) | 1.864 (1.518) | 100 kg / 42,5 kg |
  | Muscle-up con lastre | 4.505 | 1.134 | 18,75 kg / 7,5 kg |
  | Sentadilla | 5.036 | 1.477 | 177,5 kg / 110 kg |

  Son entradas, no atletas: el mismo atleta aparece en varias competiciones. Habría que quedarse con la mejor marca de cada uno, como se hace con OpenPowerlifting.
- **Formato:** CSV por competición más un TOML con metadatos (fecha, país, federación y enlaces a las fuentes). Hay API JSON paginada. La descarga completa en un solo archivo está anunciada pero todavía no existe («coming soon»).
- **Veredicto: usar con aviso.**
  - Es la única fuente de tipo 1 con licencia limpia para dominadas y fondos con lastre.
  - Aviso: el rango es «frente a competidores de streetlifting», igual que el de OpenPowerlifting es frente a powerlifters.
  - La métrica es el **lastre añadido** (o el peso corporal más el lastre), no las repeticiones con el peso corporal. Quien no entrena con lastre se queda «sin estándar» en esta métrica.
  - Las franjas de mujeres con poco n deben quedarse sin rango.
  - El 26 % de los pesos corporales es «recuperado». Hay que decidir si se usan o se descartan.

### 1.2 OpenWeightlifting

- **Primaria:** https://github.com/euanwm/OpenWeightlifting (rama `development`, commit `a7047d4b22`, 2026-11-08). Web: https://www.openweightlifting.org.
- **Tipo:** 1.
- **Licencia (comprobada en el README):**
  - Datos: «all data (`*.csv`) in the `event_data/` folder is waived», es decir, dominio público.
  - Pide atribución, pero no la exige.
  - Código: BSD-3-Clause.
  - Duda abierta: no he leído los términos de las fuentes de origen (IWF, sport80 y las federaciones nacionales).
- **Población:** halterófilos de competición. Hay más de 1.000 archivos por país en AUS, UK y US, y 539 eventos de la IWF (la API de GitHub corta en 1.000 por carpeta, así que no tengo el total exacto).
- **Variables:**
  - Peso corporal.
  - Sexo y categoría, sacados del texto de la categoría (por ejemplo, «56 kg Men»).
  - Grupo de edad solo en algunos países (US: `age_category`).
  - Intentos y mejores marcas de arrancada y de dos tiempos, y el total.
- **Método de medida:** 1RM de competición.
- **Ejercicios:** solo arrancada y dos tiempos. No cubre ninguno de los ejercicios prioritarios del encargo.
- **Formato:** un CSV por evento.
- **Veredicto: descartar por ahora.** Los datos son buenos y la licencia es clara, pero no corresponden a ejercicios del catálogo. Si algún día entran la cargada de fuerza o la arrancada, hay que volver a mirarlo.

### 1.3 CrossFit Athletes (Kaggle, ulrikthygepedersen)

- **Primaria:** https://www.kaggle.com/datasets/ulrikthygepedersen/crossfit-athletes. Lo he consultado con la API pública de Kaggle y lo he descargado.
- **Tipo:** 1, pero de origen dudoso.
- **Licencia:**
  - Kaggle la muestra como «Attribution 4.0 International (CC BY 4.0)», pero la pone quien subió el dataset.
  - La descripción no dice de dónde salen los datos. Por las columnas (`athlete_id`, `region`, `affiliate`, los benchmarks del perfil…), son perfiles de atleta de games.crossfit.com.
  - Los términos de CrossFit (PDF del 07-08-2026, https://www.crossfit.com/wp-content/uploads/2026/08/07100027/CrossFit_Terms_of_Use_08072026.pdf) dicen: «You may not use any data mining, robots, scraping or similar data gathering». También prohíben incorporar su contenido «in any other database or compilation».
  - Conclusión: la etiqueta CC BY no sana el origen.
- **Población (calculada por mí):**
  - 423.006 filas.
  - Dominadas: 36.120 hombres y 11.087 mujeres con dato.
  - Sentadilla: 72.881 hombres y 34.122 mujeres.
  - Año sin fechar.
- **Variables:**
  - Sexo, edad, altura (pulgadas) y peso (libras).
  - `pullups` (repeticiones máximas), `backsq`, `deadlift`, `candj`, `snatch` (en libras), y pruebas de CrossFit (Fran, Helen…).
  - **Lleva nombres de personas.**
- **Método de medida:** autodeclarado en el perfil, sin verificar. Hay valores imposibles (máximo de dominadas 2.147.483.647; sentadilla de 8.388.607). En CrossFit se permite el kipping, así que no es una dominada estricta.
- **Formato:** un CSV de 71 MB.
- **Veredicto: descartar.** El origen choca con los términos de CrossFit, los datos son autodeclarados y ruidosos, la dominada no es estricta y lleva nombres.
- El otro dataset de Kaggle (https://www.kaggle.com/datasets/jeanmidev/crossfit-games, leaderboards del Open de 2019 y 2020) tiene la licencia «Data files © Original Authors». También se descarta.

### 1.4 Mendeley Data: «Relative Strength Ratio and 1RM in the prone-grip pull-up exercise»

- **Primaria:** https://data.mendeley.com/datasets/yf5xvycdn2/1 (DOI 10.17632/yf5xvycdn2.1). Autor: Adrián Bayonas, Universidad de Murcia. Publicado en 2025.
- **Tipo:** 1, pero pequeño.
- **Licencia:** «CC BY 4.0» (comprobada en la ficha del dataset).
- **Población:** n = 84. La ficha no dice sexo, edad ni nivel de entrenamiento. El artículo asociado («The Role of Relative Strength Ratio in Predicting Maximum Pull-Up Performance») no lo he localizado.
- **Variables:** peso, edad, altura, grasa corporal, masa muscular, repeticiones máximas de dominada prona con el peso corporal (MNR) y 1RM con lastre.
- **Método de medida:** según el título, medido en laboratorio. **No comprobado:** la sección de archivos salía vacía en la página y no he visto los datos.
- **Formato:** no consta.
- **Veredicto: solo para comparar o validar.** Con n = 84 no salen percentiles por sexo y peso. Puede servir para comprobar la conversión entre repeticiones con el peso corporal y 1RM con lastre, que sería el puente con OpenStreetlifting.

### 1.5 Zenodo: «3-7-repetition-maximum weight and repetitions from strength exercise tests»

- **Primaria:** https://zenodo.org/records/3253731. Universidad de Aalborg, 2019; es un trabajo de fin de estudios.
- **Tipo:** 1, pero muy pequeño.
- **Licencia:** CC BY 4.0 (comprobada con la API de Zenodo: `cc-by-4.0`).
- **Población:** 28 sujetos sanos, de ambos sexos, de Dinamarca. Hay columna de años de experiencia en fuerza.
- **Variables:** sexo, edad, peso, altura y % de grasa, más peso y repeticiones del test de 3 a 7RM.
- **Método de medida:** test de 3 a 7RM con protocolo descrito (`TestProtocol.txt`), no 1RM directo. Las máquinas son las de su laboratorio.
- **Ejercicios (comprobados en `Data.xlsx`):** press de banca, curl de bíceps, elevación de talones de pie, crunch, puente de glúteo, abductor, aductor, hiperextensión, jalón, elevación lateral, curl femoral, extensión de cuádriceps, remo, flexión lateral y extensión de tríceps.
- **Formato:** XLSX.
- **Veredicto: solo para comparar.** n = 28, con máquinas concretas.

### 1.6 Otros candidatos revisados (descartados)

- **Dryad, estudiantes de Tailandia (n = 841):**
  - https://datadryad.org/dataset/doi:10.5061/dryad.wdbrv165b
  - Flexiones en 1 minuto, abdominales, sentadillas y plancha. **Autodeclarado** según el resumen.
  - La página no nombra la licencia; solo pone «Content on this site is licensed for reuse». Dryad suele publicar en CC0, pero **no lo he comprobado** para este dataset.
  - Descartar: autodeclarado, prueba de 1 minuto y una población que no se parece.
- **rankings.officialstreetlifting.com:** resultados de streetlifting en web, sin descarga, sin licencia y sin página de términos (las rutas `/terms` y `/about` dan 404). Queda superado por OpenStreetlifting.
- **King of Weighted Calisthenics (Analysthenics):**
  - https://analysthenics.github.io/blog/2022/king-weighted-calisthenics
  - 148 atletas en línea; los datos solo se ven en el texto y no hay licencia de datos.
  - Descartar.
- **Strict curl de la federación 100% Raw:**
  - PDFs de resultados, por ejemplo https://rawpowerlifting.com/wp-content/uploads/2022/11/2022-Strict-Curl-Worlds.pdf
  - Licencia **no comprobada**, n pequeña y una sola federación.
  - Descartar.
- **Strongman Archives (strongmanarchives.com):** **no se pudo abrir**; la web no respondía. Además, el log press no equivale al press militar. Sin evaluar.
- **ACFT (pruebas del ejército de EE. UU.):** no hay datos individuales públicos. Solo tablas de puntuación (https://www.army.mil/e2/downloads/rv7/aft/AFT_Scoring_Scales_250601.pdf), que son tipo 3 y le tocan a otra parte del encargo.
- **OpenPowerlifting:** no trae otros levantamientos. Los eventos son combinaciones de S, B y D; ni curl ni press militar.
- **Búsqueda en Kaggle** de «strength training», «gym exercise», «calisthenics» y «pull ups»: solo salen catálogos de ejercicios, datos de sensores o registros de una sola persona. Ninguno con marcas de fuerza de una población.

---

## Tipo 5: webs de estándares de fuerza

### 5.1 Strength Level (strengthlevel.com)

- **Primaria:**
  - FAQ: https://strengthlevel.com/faq
  - Licencia de datos: https://strengthlevel.com/data-licensing
  - Términos: https://strengthlevel.com/terms-and-conditions (actualizados el 23-01-2025)
  - Empresa: Strength Level Limited (Cambridge, Reino Unido).
- **Tipo:** 5.
- **De dónde salen los datos (comprobado):**
  - Levantamientos que registran los propios usuarios, filtrados con «data-quality checks» (quitan bots, duplicados y valores imposibles).
  - Según la página de licencias, son 195.513.376 levantamientos de 27.893.268 usuarios entre 2015 y 2026, de 287 ejercicios.
  - Ejemplo: las dominadas se basan en 4.852.758 levantamientos (2016-2026), medidos en repeticiones y también en lastre.
  - Los niveles de principiante a élite son los percentiles 5, 20, 50, 80 y 95.
  - La FAQ no dice si el 1RM se estima a partir de las repeticiones.
  - La propia FAQ avisa de que los usuarios son más fuertes que la población general.
- **Ejercicios:** casi todos los del catálogo (press militar, dominadas, remo, press inclinado, prensa, hip thrust, curl, fondos, flexiones…).
- **Términos y licencia (comprobado):**
  - «We do not permit automated or programmatic access to our services».
  - «You will not replicate or create derivative versions of our calculators or standards».
  - «Commercial use requires a written licence».
  - «Free, open source and unreleased apps still need permission».
  - «Attribution does not replace a licence».
  - Lo que sí permite: «You're welcome to link users to our calculators and standards».
  - Coste orientativo según ellos: entre 1 y 3 meses de sueldo de un ingeniero sénior al año.
  - robots.txt permite rastrear, pero los términos lo prohíben.
- **Población y variables:** usuarios de la web y la app; sexo, peso corporal y edad (su calculadora pide edad).
- **Formato:** web.
- **Veredicto: solo para comparar.**
  - No se puede copiar ni usar por detrás.
  - Opción legal: enlazar su calculadora desde la ficha del ejercicio, o comprar una licencia.
  - Relacionado (tipo 2, **no comprobado**): Clark, Latella y van den Hoek, «Reference values for the squat, bench press and deadlift…», BMC Sports Sci Med Rehabil, 2026, en prensa, doi:10.1186/s13102-026-02195-6, con 74.892 usuarios de Strength Level. Solo cubre sentadilla, banca y peso muerto.

### 5.2 Symmetric Strength (symmetricstrength.com)

- **Primaria:**
  - Inicio: https://symmetricstrength.com/
  - Metodología: https://symmetricstrength.com/about
  - Términos: https://symmetricstrength.com/terms_of_use (actualizados el 18-10-2026)
- **Tipo:** 5.
- **De dónde salen los datos (comprobado):**
  - No usa datos de usuarios para los estándares: «a published formula, not from lifts that other users choose to log».
  - Pasa cada levantamiento al total de powerlifting que implica y lo puntúa con Wilks (la portada también habla de DOTS; es incoherente).
  - Para los percentiles compara con OpenPowerlifting.
  - Las proporciones entre levantamientos salen de los estándares de ExRx para la cargada y el press, y del resto dice: «world-record lifts, general consensus, and user feedback».
  - No publica los coeficientes.
  - Autor: «Symmetric Strength Editorial Team», sin nombre.
- **Ejercicios:** sentadilla, sentadilla frontal, peso muerto, peso muerto sumo, cargada de fuerza, banca, press inclinado, fondo con lastre, press militar, push press, press con agarre de arrancada, dominada supina y prona con lastre, y remo Pendlay.
- **Términos (comprobado):**
  - «You may not copy or reuse it except as these terms allow».
  - No permite «Scrape or bulk-collect data from the Service without permission».
- **Formato:** web.
- **Veredicto: solo para comparar.** Sus proporciones no tienen datos detrás (consenso y opiniones de usuarios), así que en la escala de la app serían como mucho nivel H, y además no se pueden copiar.

### 5.3 FitnessVolt: API pública de estándares (fitnessvolt.com)

- **Primaria:**
  - Documentación: https://fitnessvolt.com/strength-standards/strength-data/
  - API: https://fitnessvolt.com/wp-json/rpe-training/v1/public/lifts
- **Tipo:** 5, pero publica percentiles con su n.
- **De dónde salen los datos (comprobado en su página):** dos poblaciones que no se mezclan:
  - «Verified»: OpenPowerlifting, para sentadilla, banca y peso muerto.
  - «Gym»: «self-reported gym percentiles from the Symmetric Strength dataset», para 14 levantamientos.
  - No explican cómo consiguieron ese dataset, de qué años es ni con qué licencia.
- **Ejemplo (comprobado con una sola llamada a la API, como permiten):** press militar, hombres, categoría de 83 kg:
  - n = 115.964
  - p10 = 43,1 kg, p50 = 58,7 kg, p90 = 75,5 kg
- **Ejercicios con datos de gimnasio:** sentadilla frontal, peso muerto sumo, cargada de fuerza, press inclinado, press militar, push press, press con agarre de arrancada, fondo, dominada supina, dominada prona y remo Pendlay. Faltan prensa, hip thrust, curl y flexiones.
- **Licencia:**
  - «The data is free to use under CC BY 4.0», con enlace de atribución obligatorio.
  - **Duda seria:** Symmetric Strength dice que sus estándares no salen de usuarios y prohíbe recolectar sus datos en bloque. No he encontrado nada que relacione a las dos empresas: Symmetric Strength solo cita a HattanMedia como diseñador de la web.
  - Si FitnessVolt no tiene derecho a licenciar esos datos, la etiqueta CC BY no nos protege.
- **Variables:** sexo, categoría de peso y percentiles p10, p25, p50, p75, p90, p95 y p99, más el n y el múltiplo del peso corporal en la mediana. No hay edad.
- **Método de medida:** autodeclarado; no dice si es 1RM real o estimado.
- **Formato:** JSON.
- **Veredicto: solo para comparar** hasta que FitnessVolt confirme por escrito de dónde salen los datos de «Symmetric Strength» y que puede licenciarlos.
  - Si lo confirman, pasa a **usar con aviso** (autodeclarado, población de gimnasio) y sería la mejor fuente para el press militar, el press inclinado, el remo Pendlay y las dominadas.
  - Para sentadilla, banca y peso muerto no aporta nada, porque Forja ya usa OpenPowerlifting directamente.

### 5.4 ExRx.net: «Weightlifting Performance Standards» (Lon Kilgore)

- **Primaria:**
  - Estándares: https://exrx.net/Testing/WeightLifting/StrengthStandards
  - Términos: https://exrx.net/Notes/Legal (actualizados el 10-08-2026)
  - WebFetch devolvió un 403; leí cada página una sola vez con curl.
- **Tipo:** 5.
- **De dónde salen los datos (comprobado):**
  - «Provided by Dr. Lon Kilgore».
  - Son estándares de 1RM para adultos «based on competitive weightlifter and powerlifting classification systems», usados desde los años 50.
  - Dicen que no son normas de población: «should not to be confused with strength norms».
  - No hay n.
- **Ejercicios:** banca, peso muerto, press (militar), cargada de fuerza, arrancada de fuerza y sentadilla, en kg y en libras.
- **Términos (comprobado):**
  - «Automated scraping or manual copying of Our Content is prohibited».
  - «Do not modify, sell, or republish our content or tools» sin permiso escrito.
  - Hay un aviso de que pasan los casos a su abogado.
- **Formato:** web (tablas).
- **Veredicto: solo para comparar.**
  - No son percentiles de una población (son categorías de clasificación).
  - No se pueden copiar.
  - El press militar es el único ejercicio del catálogo que añaden a lo que ya tenemos.

### 5.5 Mark Rippetoe y Lon Kilgore, *Practical Programming for Strength Training* (libro)

- **Primaria:** el libro (The Aasgaard Company). Es de pago y **no lo he abierto**.
- **Tipo:** 5.
- **De dónde salen los datos:** según ExRx, las mismas tablas de clasificación de Kilgore (no comprobado contra el libro).
- **Licencia:** libro con todos los derechos reservados (suposición: no he leído la página de créditos).
- **Veredicto: solo para comparar.** Es la misma metodología que ExRx, sin datos de población.

### 5.6 StrengthLog (strengthlog.com/strength-standards)

- **Primaria:** https://www.strengthlog.com/strength-standards/
- **Tipo:** 5.
- **De dónde salen los datos:** «based on the users of our workout log StrengthLog». No da n, años ni método.
- **Ejercicios:** sentadilla, banca, peso muerto y **press militar**.
- **Licencia:** no hay ninguna declaración de reutilización, solo «© StrengthLog». Sin licencia, todos los derechos quedan reservados.
- **Veredicto: solo para comparar.**

### 5.7 strengthstandards.co

- La web **no responde** (código 000 el 2026-11-22). Solo quedan clones en GitHub sin procedencia.
- **Veredicto: descartar.**

---

## Qué ejercicio cubre cada fuente usable

| Ejercicio | Mejor fuente de tipo 1 o 5 | Métrica | Estado |
|---|---|---|---|
| Dominadas | OpenStreetlifting (CC0) | kg de lastre (o peso corporal + lastre), 1RM | Se puede, con aviso: población de competidores |
| Fondos en paralelas | OpenStreetlifting (CC0) | kg de lastre, 1RM | Se puede, con aviso |
| Press militar | FitnessVolt (procedencia dudosa); ExRx, Strength Level y StrengthLog solo para comparar | 1RM en kg | Pendiente de confirmar con FitnessVolt; si no, hay que buscar en los tipos 2 y 4 |
| Press inclinado | FitnessVolt (dudoso) | 1RM | Igual que el press militar |
| Remo con barra | FitnessVolt (remo Pendlay, dudoso) | 1RM | Igual; además, el remo Pendlay no es el remo inclinado de 45° |
| Sentadilla frontal | FitnessVolt (dudoso) | 1RM | Igual |
| Prensa, hip thrust, peso muerto rumano, curl, flexiones | Ninguna fuente abierta; solo Strength Level, que no se puede usar | — | Sin estándar desde los tipos 1 y 5 |

## Cómo encajaría OpenStreetlifting en Forja (apunte)

- El ETL sería igual que el de OpenPowerlifting:
  - Se lee `backend/data/competitions/**/entries.csv`, se filtra `Status = competed` y lastre > 0.
  - Se queda la mejor marca de cada atleta. La clave es nombre, apellido y `Disambiguation`, y se descarta después; no se guarda ningún nombre.
  - Se calculan percentiles del 1 al 99 por sexo y franja de peso, con un n mínimo por franja.
- En `estandar_fuerza` haría falta saber que la métrica es «lastre añadido en kg» y no un 1RM absoluto. Opciones:
  - Una columna de tipo de métrica.
  - Guardar el peso corporal más el lastre como carga total.
- Hay que fijar el commit o la fecha de la descarga para que se pueda reproducir, porque el repo cambia cada semana.
- Atribución opcional: «Data from OpenStreetlifting».

## Riesgos y dudas abiertas

1. **FitnessVolt y Symmetric Strength:** la licencia CC BY puede no valer nada si los datos no son suyos. Hay que preguntárselo por escrito.
2. **Población de OpenStreetlifting:** son competidores europeos de élite y semiélite. Quien entrena en un gimnasio saldrá en percentiles muy bajos, igual que pasa ya con OpenPowerlifting. Hay que decirlo en la app.
3. **Técnica de la dominada:** no he comprobado si todas las federaciones de streetlifting exigen la misma técnica (prono o supino, extensión completa). Forja distingue dominadas pronas y supinas; OpenStreetlifting no tiene esa columna.
4. **Pesos «recuperados»** en OpenStreetlifting: el 26 % de las entradas. Hay que decidir si entran en los percentiles por peso.
5. **Datos personales:** OpenStreetlifting y OpenWeightlifting traen nombres. Forja solo debe guardar agregados.
6. **No comprobado:** la licencia de Dryad, el contenido real del dataset de Mendeley, los términos de Strongman Archives y de la federación 100% Raw, y el libro de Rippetoe.
