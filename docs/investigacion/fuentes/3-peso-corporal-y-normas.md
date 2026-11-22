> Informe de investigación del 2026-11-22 (agente de investigación, revisado por Claude). Lo que pone «comprobado» se leyó en la fuente; lo demás, como se indica. Resumen y recomendación: [../estandares-fuerza.md](../estandares-fuerza.md).

# Investigación 3: ejercicios con peso corporal y normas de organismos

Parte del encargo `docs/investigacion/estandares-fuerza-encargo.md`, tipo de fuente 3, más los estudios normativos de peso corporal que salieron por el camino. Fecha: 2026-11-22.

**Cómo leer esto.** «Comprobado» quiere decir que abrí la fuente primaria (PDF o texto completo) y la cifra está ahí. Si la cifra viene de una copia o de un resumen, lo digo. Los niveles A-D, H y P siguen la escala de `docs/design-system.md` §5.2. En el repo no hay una definición de cada letra, así que **los criterios son míos (añadido)**:

- **A:** distribución medida en una población grande y parecida, con método publicado.
- **B:** estudio normativo con revisión por pares y muestra razonable, o un baremo construido sobre datos poblacionales.
- **C:** muestra pequeña o población distinta de la de un gimnasio.
- **D:** baremo administrativo sin método publicado.

**Normas de aprobado frente a distribuciones.** Casi todo lo de organismos son **baremos de puntos o mínimos**, no percentiles. Un «60 puntos» del ejército no es un percentil 60. Donde el organismo dice cómo construyó el baremo, lo apunto.

---

## Resumen por ejercicio

| Ejercicio | Mejor fuente con licencia limpia | Qué da | Veredicto | Nivel |
|---|---|---|---|---|
| **Dominadas (prono)** | USMC MCO 6100.13A (dominio público) + Looney et al. 2024 (CC BY, media ± DE) | Baremo de puntos por edad y sexo; media y DE de 1 721 marines | Usar con aviso: es un baremo, no un percentil | D (baremo), C (media y DE) |
| Dominadas, alternativa española | Policía Nacional, convocatoria de 2026 (BOE, reutilizable) | Puntos de 0 a 10, solo hombres (17 = 10 puntos) | Solo para comparar | D |
| **Flexiones** | Orden DEF/15/2026 (BOE) y AFT del US Army (dominio público) | Baremos de puntos por sexo y edad (de 17 a 60+ años) | Usar con aviso | D (DEF), B-D (AFT: dice que se normó con ~1 millón de ACFT, pero no publica el método) |
| Flexiones, distribución real | CSEP-PATH (ACSM la reproduce), © CSEP | 5 categorías por edad y sexo | Solo para comparar: hay copyright y la tabla de mujeres es con rodillas | B |
| Flexiones, distribución abierta | Kjær et al. 2016 (CC BY) | Media, DE y Q1-Q3 por decenio | Solo para comparar: protocolo de 40 s distinto | C |
| **Plancha** | Strand et al. 2014 (CC BY) | **Percentiles P10-P90 por sexo**, n = 471 universitarios | **Usar** (la única distribución abierta en percentiles) | B |
| Plancha, adultos | Lawson et al. 2025 (CC BY-ND) | P10-P90 por sexo, n = 299 militares de ~37 años, con tope de 5 min | Usar con aviso | C |
| Plancha, baremos | AFT, USMC y DEF/15/2026 | Puntos por tiempo (igual para los dos sexos en los tres) | Solo para comparar | D |
| **Fondos en paralelas** | Ninguna | — | **Sin estándar** | — |
| **Elevación de piernas colgado** | Ninguna | — | **Sin estándar** | — |

**Recomendación:**

- **Plancha:** puede tener rango ya con Strand 2014, en percentiles de verdad, avisando de que la población son universitarios de EE. UU. de unos 20 años.
- **Dominadas y flexiones:** no hay una distribución de adultos con licencia abierta.
  - Opción honesta 1: mostrar «tu marca equivale a X puntos del baremo de los marines o del ejército español para tu edad y sexo», sin llamarlo percentil.
  - Opción honesta 2: esperar a **Knapik et al. 1993**. Son percentiles de flexiones del ejército de EE. UU. (n = 6 022), seguramente de dominio público, pero no pude abrirlo (ver ficha 15).
- **Fondos y elevación de piernas colgado:** se quedan «sin estándar».

---

## Fichas

### 1. US Army: AFT Scoring Scales (2025)

- **Primaria:** https://www.army.mil/e2/downloads/rv7/aft/AFT_Scoring_Scales_250601.pdf (PDF de 9 páginas, «Approved: 15 May 2025 Effective: 1 June 2025»). La página del examen, con el estándar de ejecución, es https://www.army.mil/aft/.
- **Organismo:** US Army.
- **Licencia:** obra del Gobierno federal.
  - 17 U.S.C. §105: «Copyright protection under this title is not available for any work of the United States Government».
  - La web army.mil dice que su información «is considered public information and may be distributed or copied» y pide que se cite.
  - El PDF no lleva aviso propio.
- **Población:** soldados en activo y reservistas de 17 a 62+ años. La columna «M|C» es para hombres y para las especialidades de combate (sin distinción de sexo desde 2026) y la «F», para mujeres.
  - Según army.mil, el baremo se basa en datos de «nearly one million ACFTs» y en el análisis de RAND.
  - **No publica** la tabla que pasa de datos a puntos, así que no se sabe qué percentil es cada nota.
- **Método:**
  - **Flexiones con despegue de manos (hand-release push-up):** máximas repeticiones en 2 minutos. Abajo, el pecho, la cadera y los muslos tocan el suelo y los brazos se abren en T. Se sube con el cuerpo recto hasta extender los codos.
  - **Plancha:** sobre antebrazos, en línea recta y hasta perder la posición, con un aviso del juez.
  - **Peso muerto:** máximo de 3 repeticiones con barra hexagonal, en libras.
- **Qué da:** puntos de 0 a 100 por marca, en 10 franjas de edad y por sexo. El mínimo para aprobar son 60 puntos por prueba en las especialidades de combate.
- **Cifras comprobadas** en el PDF, con el texto extraído con pdftotext:

  | Prueba | Grupo | 100 puntos | 60 puntos |
  |---|---|---|---|
  | Flexiones | Hombres 17-21 | 58 | 15 |
  | Flexiones | Mujeres 17-21 | 53 | 11 |
  | Flexiones | Hombres 22-26 | 61 | 14 |
  | Plancha (igual para los dos sexos) | 17-21 | 3:40 | 1:30 |
  | Plancha (igual para los dos sexos) | 22-26 | 3:35 | 1:25 |
  | Peso muerto | Hombres 22-26 | 350 lb | 150 lb |
  | Peso muerto | Mujeres 22-26 | 230 lb | 120 lb |

- **Formato:** PDF con una tabla por prueba (sale de un .xlsx).
- **Veredicto:** **usar con aviso**, como «puntos AFT», no como percentil. Es la mejor norma oficial para flexiones porque cubre todas las edades y se normó con datos del propio ejército. Nivel **B-D**: hay datos detrás, pero el método no está publicado.
  - **Ojo con la población:** soldados, más en forma que la media, y con flexiones con despegue de manos, que dan menos repeticiones que las flexiones clásicas.
  - El peso muerto es de 3 repeticiones con barra hexagonal: no casa con el 1RM de OpenPowerlifting.

### 2. US Marine Corps: MCO 6100.13A (PFT)

- **Primaria:** https://www.fitness.marines.mil/Portals/211/Docs/PFT_CFT/MCO%206100.13A%20with%20CH-3.pdf
  - La leí entera en el navegador con pdf.js; curl y WebFetch dan 403.
  - La versión vigente es la CH-5, que no pude abrir. MARADMIN 613/25 añade una puntuación sin distinción de sexo para combate desde el 1-1-2026 (no comprobado en primaria).
- **Organismo:** US Marine Corps (HQMC, TECOM).
- **Licencia:** dominio público (17 U.S.C. §105). Portada: «DISTRIBUTION STATEMENT A: Approved for public release; distribution is unlimited.»
- **Población:** marines de 17 a 51+ años en 8 franjas, por sexo. El documento no dice cómo se construyó el baremo.
- **Método:**
  - **Dominadas:** colgado inmóvil con los brazos extendidos, agarre prono o supino, sin kipping. Cuenta cuando la barbilla pasa la barra y se vuelve a la extensión completa. No hay límite de tiempo.
  - **Flexiones:** 2 minutos, y como mucho dan 70 puntos.
  - **Plancha:** sobre antebrazos, igual para los dos sexos y todas las edades.
- **Cifras comprobadas** (Tabla 2-2, página 2-10, versión de 2018):

  | Grupo | Dominadas para 100 puntos | Dominadas mínimas |
  |---|---|---|
  | Hombres 17-20 | 20 | 4 (40 puntos) |
  | Hombres 21-25 | 23 | 5 |
  | Mujeres 17-20 | 7 | 1 (60 puntos) |
  | Mujeres 26-30 | 12 | 4 |

  - **Flexiones (Tabla 2-2, cont.):** hombres 17-20, 82 repeticiones para el máximo (70 puntos) y 42 de mínimo; mujeres 17-20, 42 y 19.
  - **Plancha (Tabla 2-4, CH-2):** 4:20 = 100 y 1:03 = 40. **Ya no vale:** desde 2022 el máximo es 3:45 (225 s) y el mínimo 1:10, según Looney et al. 2024 (ficha 12) y la web de Marine Corps Force Fitness. No lo comprobé en la CH-5.
- **Formato:** PDF (orden ministerial con tablas).
- **Veredicto:** **usar con aviso** para dominadas, como «puntos del PFT de los marines», no como percentil. Es la única norma oficial de dominadas por edad y sexo, mujeres incluidas. Nivel **D**: no publica el método.

### 3. España: Orden DEF/15/2026 (pruebas físicas unificadas de las Fuerzas Armadas)

- **Primaria:** BOE-A-2026-1379, BOE núm. 19, de 21-1-2026, https://www.boe.es/boe/dias/2026/01/21/pdfs/BOE-A-2026-1379.pdf
- **Organismo:** Ministerio de Defensa.
- **Licencia:** reutilizable para fines comerciales y no comerciales.
  - Aviso legal del BOE (https://www.boe.es/informacion/aviso_legal/index.php): licencia tipo aprobada por Resolución de 27-6-2024 y Ley 37/2007 de reutilización. Cita literal: «permiten la reutilización de los documentos sometidos a ellas para fines comerciales y no comerciales».
  - Condiciones: citar la fuente con enlace, no desnaturalizar la información y no dar a entender que el uso es oficial.
  - **Añadido mío:** además, las disposiciones normativas no tienen derechos de autor por el art. 13 de la Ley de Propiedad Intelectual.
- **Población:** militares profesionales de 17 a 60+ años (evaluación periódica), por sexo. El art. 12 dice que hacen falta 20 puntos para el apto. **No explica cómo se construyeron las tablas.**
- **Método:**
  - **Flexo-extensiones de brazos:** 2 minutos, con la barbilla tocando una almohadilla de 10 cm, el cuerpo alineado y una parada permitida.
  - **Plancha isométrica:** sobre antebrazos y puntas de los pies, con los codos bajo los hombros y el tiempo hasta perder la alineación.
- **Qué da:** puntos de 0 a 100 por repetición o tiempo en 9 franjas de edad (17-25 … 60+). Las flexiones van por sexo y la plancha es **igual para los dos sexos**. El Anexo III fija además las marcas de ingreso.
- **Cifras comprobadas** (Anexo II, páginas 9763-9767):

  | Prueba | Grupo | 20 puntos | 50 puntos | 100 puntos |
  |---|---|---|---|---|
  | Flexiones | Hombres 17-25 | 14 | 34 | 72 |
  | Flexiones | Mujeres 17-25 | 8 | 28 | 66 |
  | Plancha | 17-25 | 0:40 | 2:05 | 5:15 |

  - **Ingreso en tropa y marinería (Anexo III):** 9 flexiones los hombres y 5 las mujeres, y 40 s de plancha para los dos.
- **Formato:** PDF del BOE (tablas).
- **Veredicto:** **usar con aviso**, como «puntos del baremo de las FAS», no como percentil. Es española, reciente, cubre de 17 a 60+ y se puede reutilizar sin problema. Nivel **D**: no tiene método publicado.

### 4. España: Policía Nacional, Escala Básica (convocatoria de 2026)

- **Primaria:** BOE-A-2026-15055 (Resolución de 7-7-2026, BOE núm. 167, de 10-7-2026), Anexo II: https://www.boe.es/boe/dias/2026/07/10/pdfs/BOE-A-2026-15055.pdf. La de 2025 (BOE-A-2025-16610) trae el mismo baremo; lo comprobé.
- **Licencia:** como en la ficha 3 (reutilización del BOE).
- **Población:** opositores de 18 años en adelante. Es un baremo de oposición sin método publicado.
- **Método:**
  - **Hombres, dominadas:** agarre prono algo más ancho que los hombros, desde la extensión completa, sin balanceo, con la barbilla por encima de la barra y una pausa entre repeticiones. No se permite estar más de 5 s colgado entre repeticiones.
  - **Mujeres:** **suspensión con los brazos flexionados** (agarre supino, la barbilla por encima de la barra, tiempo máximo). No son dominadas.
- **Cifras comprobadas:**
  - **Dominadas (hombres):** de 0 a 4 = 0 puntos (eliminatorio), 5 = 1, 10-11 = 5 y 17 = 10.
  - **Suspensión (mujeres):** 35 s o menos = 0, de 57 a 62 s = 5 y 95 s o más = 10.
  - Hace falta una media de 5 o más entre las tres pruebas.
- **Error del original:** la tabla del BOE llama «Flexiones-puntos» a la columna de los hombres, aunque el ejercicio descrito son dominadas.
- **Veredicto:** **solo para comparar.** Es un baremo de selección con la nota máxima en 17. No sirve para mujeres porque es otra prueba. Nivel **D**.

### 5. España: Guardia Civil, Escala de Cabos y Guardias (2026)

- **Primaria:** BOE-A-2026-9982 (Resolución 160/38243/2026, BOE núm. 112, de 8-5-2026), Apéndice II: https://www.boe.es/boe/dias/2026/05/08/pdfs/BOE-A-2026-9982.pdf
- **Licencia:** reutilización del BOE.
- **Qué da:** solo **mínimos de apto**.
- **Método:** flexo-extensiones con la barbilla al suelo (almohadillado de 6 cm como máximo), sin límite de tiempo y con un descanso en posición.
- **Cifras comprobadas:**

  | Edad | Hombres | Mujeres |
  |---|---|---|
  | Menos de 35 | 16 | 11 |
  | De 35 a 39 | 16 | 11 |
  | 40 o más | 14 | 9 |

- **Veredicto:** **solo para comparar** («el mínimo de la Guardia Civil para tu edad»). Nivel **D**.

### 6. CSEP-PATH (Canadian Society for Exercise Physiology), reproducido por el ACSM

- **Primaria:** CSEP Physical Activity Training for Health (CSEP-PATH) Resource Manual, 2.ª ed., 2019. Es un libro de pago y **no lo abrí**.
  - Comprobé las cifras en la reproducción autorizada de ACE: https://contentcdn.eacefitness.com/assets/certification/ace-answers/forms/pt/36_Push-up_Assessment_Protocol.pdf («Reprinted with permission from Canadian Society for Exercise Physiology (2019)»).
  - Las *Guidelines for Exercise Testing and Prescription* del ACSM usan esta misma tabla según fuentes secundarias. **No lo comprobé**: el libro es de pago.
- **Licencia:** © CSEP, todos los derechos reservados. La propia ACE la reproduce con permiso. No se puede copiar la tabla; los umbrales son hechos, pero eso es una duda jurídica que no resuelve esta investigación.
- **Población:** según la literatura secundaria, sale de la Canada Fitness Survey de 1981 por la vía de la CPAFLA. **No lo comprobé en primaria.**
- **Método:** flexiones sin límite de tiempo hasta el fallo técnico, con el pecho tocando un puño o un bloque. **Hombres, flexiones normales; mujeres, con rodillas apoyadas.**
- **Qué da:** 5 categorías (de «Excellent» a «Needs improvement») por sexo y edad (15-19 … 60-69). No son percentiles.
- **Cifras** (comprobadas en la copia de ACE): de 20 a 29 años, en los hombres «Excellent» es 36 o más y «Good» de 22 a 28; en las mujeres (con rodillas) «Excellent» es 30 o más.
- **Veredicto:** **solo para comparar.** Tiene copyright, los datos son de 1981 y la prueba de las mujeres no es comparable. Nivel B como estudio poblacional, pero no utilizable.

### 7. Cooper Institute: *Physical Fitness Assessments and Norms for Adults and Law Enforcement* (2013)

- **Primaria:** monografía de pago. **No la abrí.**
- **Licencia:** © Cooper Institute.
- **Qué da:** según fuentes secundarias, percentiles de flexiones en 1 minuto (las mujeres pueden hacerlas con rodillas) y de abdominales por edad y sexo. Sin n publicado.
- **Veredicto:** **descartar** para la app (copyright y método opaco). Como mucho, para comparar.

### 8. FitnessGram (Cooper Institute): Healthy Fitness Zone

- **Primaria:** tabla HFZ (© 2013 The Cooper Institute), copia del Illinois State Board of Education: https://www.isbe.net/Documents/HFZ_Standards.pdf
- **Licencia:** © Cooper Institute.
- **Población:** de 5 a 17+ años. Son umbrales **por criterio de salud**, no percentiles.
- **Cifras comprobadas** (fila 17+):

  | Prueba | Chicos | Chicas |
  |---|---|---|
  | Flexiones a 90° | 18 o más | 7 o más |
  | Dominadas modificadas | 14 o más | 4 o más |
  | Suspensión con brazos flexionados | 15 s o más | 8 s o más |

- **Veredicto:** **solo para comparar.** Es para jóvenes, la app es para mayores de 18 y tiene copyright.

### 9. Strand SL, Hjelm J, Shoepe TC, Fajardo MA (2014). Norms for an isometric muscle endurance test

- **Referencia:** *J Hum Kinet* 40:93-102. doi:10.2478/hukin-2014-0011 · PMID 25031677 · PMC4096102.
- **Licencia:** **CC BY** (metadatos de PMC; lo comprobé en el texto completo).
- **Población:** 471 universitarios de EE. UU. (North Park University y Loyola Marymount): 194 hombres (20,4 años, 81 kg) y 277 mujeres (20,2 años, 64 kg). 109 eran deportistas NCAA.
- **Método:** plancha sobre antebrazos con los codos bajo los hombros y el cuerpo rígido, hasta la fatiga o hasta dos correcciones seguidas sin conseguir arreglar la postura.
- **Qué da:** **percentiles P10-P90 por sexo** y por condición de deportista.
- **Cifras comprobadas** (Tabla 2):

  | Percentil | Hombres | Mujeres |
  |---|---|---|
  | P10 | 62 s | 35 s |
  | P50 | 110 s | 72 s |
  | P90 | 201 s | 142 s |

- **Error del original:** en la Tabla 2 las cabeceras «Non-Varsity (n = 109)» y «Varsity (n = 361)» están cambiadas: el texto dice que 109 eran deportistas.
- **Veredicto:** **usar.** Es la única distribución en percentiles con licencia abierta para un ejercicio de peso corporal. Hay que avisar de que la población son universitarios de unos 20 años y que no hay franjas de edad. Nivel **B**.

### 10. Lawson DJ et al. (2025). Comprehensive Fitness Assessment in a Professional Military Education Cohort

- **Referencia:** *Int J Exerc Sci* 18. doi:10.70252/WUPH8495 · PMC12408081.
- **Licencia:** **CC BY-ND** (metadatos de PMC). Las cifras son hechos, pero la tabla no se puede adaptar.
- **Población:** 307 alumnos y profesores del Air Command and Staff College (Maxwell AFB): 219 hombres y 86 mujeres, de 37 ± 5 años.
- **Método:** plancha sobre antebrazos con un **tope de 5 minutos**.
- **Cifras comprobadas:**

  | Percentil | Hombres | Mujeres |
  |---|---|---|
  | P10 | 81 s | 58 s |
  | P50 | 134 s | 120 s |
  | P90 | 240,5 s | 196,8 s |

- **Veredicto:** **usar con aviso** como segunda referencia para adultos de 30 a 40 años. La muestra es pequeña, militar y con techo. Nivel **C**.

### 11. Kjær IGH et al. (2016). Normative values for musculoskeletal- and neuromotor fitness in apparently healthy Norwegian adults

- **Referencia:** *BMC Sports Sci Med Rehabil* 8. doi:10.1186/s13102-016-0059-4 · PMID 27891234 · PMC5116214.
- **Licencia:** **CC BY.**
- **Población:** muestra aleatoria nacional de Noruega, de 20 a 65 años (2009). En las flexiones, 342 hombres y 150 mujeres, pero **solo 23-39 personas por celda** en los de 20-29.
- **Método:** «modified push-ups» en 40 s, un protocolo propio que no es la flexión clásica; quien no puede, las hace de rodillas.
- **Cifras comprobadas:** de 20 a 29 años, los hombres hacen 14,2 ± 3,2 (Q1-Q3 de 12 a 16) y las mujeres 10,3 ± 4,2 (de 7 a 13).
- **Veredicto:** **solo para comparar.** El protocolo no casa con lo que registra un usuario y las celdas son pequeñas. Nivel **C**.

### 12. Looney DP, Potter AW, Schafer EA, Chapman CL, Friedl KE (2024). The 300 Marines

- **Referencia:** *Front Physiol* 15:1406749. doi:10.3389/fphys.2024.1406749 · PMID 38957215 · PMC11217602.
- **Licencia:** **CC BY.**
- **Población:** 497 mujeres (29 ± 7 años) y 1 224 hombres (30 ± 8) marines que aprobaron el PFT y el CFT y eligieron dominadas. Las marcas las declaran ellos mismos.
- **Qué da:** media ± DE, además de la tabla vigente de máximos por edad y sexo (Tabla 1), que confirma la ficha 2 y la plancha de 225 s.
- **Cifras comprobadas:** dominadas del resto de marines (los que no sacan 300), 9 ± 3 las mujeres y 20 ± 5 los hombres.
- **Veredicto:** **usar con aviso**, solo si se aceptan percentiles aproximados con una normal (marcado como **P**, cálculo propio). Hay sesgo: solo están los que aprobaron, y se sabe que la distribución de dominadas tiene asimetría positiva (estudio de DTIC citado en la búsqueda, no abierto). Nivel **C**.

### 13. Keefer M, DeBeliso M (2020). A Comparison of USMC PFT and CFT Results

- **Referencia:** *Int J Exerc Sci* 13(2). PMID 33414879 · PMC7745917.
- **Licencia:** **no comprobada** (el texto completo de PMC no se pudo descargar). Lo más probable es que sea una licencia CC de la revista, pero sin confirmar.
- **Población:** 19 678 marines varones en activo, de 22,5 ± 1,3 años.
- **Cifra comprobada en el resumen:** dominadas, 18,0 ± 5,0.
- **Veredicto:** **solo para comparar.** Es una media de hombres jóvenes y muy entrenados. Nivel **C**.

### 14. China: estándar nacional de condición física de estudiantes (国家学生体质健康标准, revisión de 2014)

- **Primaria:** el documento del Ministerio de Educación no abrió (la web de moe.gov.cn da error). Las cifras están comprobadas en la copia de la Universidad de Hainan: https://spe.hainanu.edu.cn/__local/D/30/C5/FA5DC557BDE70225475D20B7798_EE33C660_12532.pdf. El número de documento oficial (教体艺〔2014〕5号) **no está comprobado**.
- **Licencia:** **añadido mío, sin comprobar:** el art. 5 de la ley china de derechos de autor excluye los documentos oficiales de los órganos del Estado.
- **Población:** universitarios chinos. Las **dominadas, solo hombres**; las mujeres hacen abdominales en 1 minuto.
- **Cifras comprobadas en la copia:** en 1.º y 2.º, 19 dominadas = 100 puntos, 15 = 80 y 10 = 60 (aprobado). En 3.º y 4.º, 20 = 100. Hay hasta 10 puntos extra por repeticiones por encima del máximo.
- **Veredicto:** **solo para comparar.** La población es muy distinta y el método no está publicado. Nivel **D**.

### 15. Knapik J et al. (1993). Army Physical Fitness Test (APFT): Normative Data on 6022 Soldiers

- **Primaria:** DTIC ADA274547, https://apps.dtic.mil/sti/html/tr/ADA274547/index.html. **No pude abrirlo:** WebFetch da 403 y DTIC estaba en mantenimiento. Los datos de la ficha salen del registro de NTIS en la búsqueda.
- **Licencia:** probablemente de dominio público, por ser un informe del US Army (17 U.S.C. §105). **Falta comprobar la «distribution statement».**
- **Población:** 5 346 hombres y 676 mujeres de 14 bases (encuesta de condición física del ejército en activo de 1988).
- **Qué daría:** **percentiles por edad y sexo** de flexiones en 2 minutos y de abdominales.
- **Veredicto:** **pendiente; es la candidata más prometedora para las flexiones.** Si se confirma que es de dominio público y trae las tablas, nivel **B**: población militar de 1988 y flexiones del APFT, que no son con despegue de manos.

### 16. Lockie RG, Dawes JJ, Orr RM, Dulla JM (2020). Recruit Fitness Standards From a Large Law Enforcement Agency

- **Referencia:** *J Strength Cond Res* 34(4):934-941. PMID 31972819.
- **Acceso:** de pago; solo leí el resumen.
- **Población y pruebas:** 908 reclutas de policía (761 hombres y 147 mujeres). Percentiles de dominadas y de flexiones en 120 s.
- **Cifras:** no comprobadas.
- **Veredicto:** **solo para comparar** si alguien consigue el texto. Las cifras de la tabla serían datos, pero la tabla tiene copyright.

### 17. Otras fuentes revisadas que no sirven

- **RAND RRA1825-1 (2022), *Independent Review of the ACFT*:**
  - Lo abrí (texto extraído). Trae tasas de aprobado y percentiles de la nota total por sexo (Figura 3.1), **no distribuciones de repeticiones por prueba**.
  - © RAND: «Permission is required from RAND to reproduce». **Descartar.**
- **Statistics Canada, Hoffmann et al. 2019** (normas de la encuesta canadiense de salud, CHMS, ciclo 5): resumen comprobado en Europe PMC (PMID 31617933). Cubre fuerza de agarre, salto, flexibilidad y capacidad aeróbica, **pero no flexiones.** Descartar para este fin.
- **YMCA (*Fitness Testing and Assessment Manual*), NSCA (*Essentials of Strength Training and Conditioning*), ACSM (*GETP*) y Eurofit para adultos:** son libros de pago o de Human Kinetics y no los abrí. Por lo que dicen las fuentes secundarias, reproducen tablas de otros (CSEP, YMCA). Del YMCA solo hay la prueba de press de banca con carga fija, que no es de peso corporal. **No comprobado. Solo para comparar.**
- **Bomberos (España):** los baremos con dominadas salen en las bases municipales o autonómicas (BOCM, DOGC…), no en el BOE. Son heterogéneos y **no los investigué a fondo.**
- **Fondos en paralelas y elevación de piernas colgado:** ninguna búsqueda dio normas en adultos. Ni Europe PMC (por ejemplo, «dips OR hanging leg raise» con «normative, percentile, norms» saca 8 resultados irrelevantes) ni ningún organismo los usa como prueba.

---

## Cómo encajaría en Forja (resumen para quien integre)

- **La tabla `estandar_fuerza`:** hoy son percentiles por levantamiento, sexo y franja de **peso**. En peso corporal la variable que manda es la **edad**, no el peso. Haría falta:
  - `metrica` (1RM_KG | REPETICIONES | SEGUNDOS);
  - `tipo_norma` (PERCENTIL | PUNTOS_BAREMO | MINIMO);
  - una franja de **edad** opcional.
- **Con percentiles de verdad** (plancha de Strand 2014; flexiones de Knapik 1993 si se confirma): se usa el mismo flujo que con OpenPowerlifting, con una franja única de peso y las franjas de edad del estudio.
- **Con baremos de puntos** (AFT, USMC, DEF/15/2026): en la app no deberían salir como «percentil». La frase honesta sería «X puntos en el baremo de las FAS (17-25, hombre)».
- **Estándar de ejecución:** cada fuente cuenta la repetición de forma distinta, y conviene mostrar cuál se usa:
  - dominadas sin kipping;
  - flexiones con despegue de manos frente a barbilla a la almohadilla;
  - plancha sobre antebrazos con o sin tope.

## Riesgos y dudas

- **Baremo no es lo mismo que percentil.** Ningún organismo publica qué percentil es cada puntuación. El AFT dice que se normó con datos de casi un millón de pruebas, pero no da la tabla.
- **Población:** casi todo son militares y opositores, más en forma que quien entra a un gimnasio. Solo Strand y Kjær son civiles.
- **Protocolos incompatibles:**
  - Flexiones: 2 minutos, sin límite, con despegue de manos, con rodillas las mujeres de CSEP, o 40 s modificadas en Kjær.
  - Plancha: con tope de 5 min en Lawson o de 3:40 a 5:15 en los baremos.
- **Licencias:** las de Estados Unidos y el BOE son limpias; CSEP, Cooper y FitnessGram, no. CC BY-ND (Lawson) permite citar las cifras pero no adaptar la tabla: es una duda jurídica que no resuelvo.
- **Por verificar:**
  - la CH-5 de la MCO 6100.13A;
  - el informe de Knapik de 1993;
  - la licencia de Keefer 2020;
  - el documento primario chino del Ministerio de Educación.
