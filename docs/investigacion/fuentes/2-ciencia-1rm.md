> Informe de investigación del 2026-11-22 (agente de investigación, revisado por Claude). Lo que pone «comprobado» se leyó en la fuente; lo demás, como se indica. Resumen y recomendación: [../estandares-fuerza.md](../estandares-fuerza.md).

# Investigación 2 · Datos normativos de 1RM en ciencia (ejercicios fuera de SBD)

Parte del encargo [estandares-fuerza-encargo.md](/home/imtrucha/Proyectos/forja/docs/investigacion/estandares-fuerza-encargo.md), tipo de fuente 2 (y las relaciones medidas entre ejercicios del tipo 4 que salieron por el camino). Fecha: 2026-11-22.

## Cómo se buscó

- **Europe PMC (API REST)**, búsquedas por título y por resumen: `normative/reference values/percentile + 1RM`, `leg press normative`, `strength standards`, `incline bench 1RM`, `lat pulldown pull-up 1RM`, `military/shoulder/overhead press 1RM`, `front squat back squat 1RM`, `hip thrust 1RM squat`, `Romanian deadlift 1RM`, `dumbbell barbell bench 1RM`, `bent-over row / prone bench pull bench 1RM`, `arm curl 1RM normative`, `leg curl leg extension 1RM normative`, `gym members 1RM percentiles`. Texto completo y tablas con `/{PMCID}/fullTextXML` y, cuando falló, con el BioC de NCBI.
- **WebSearch** (estándar) para clásicos de pago (Hoeger 1990, Cooper/ACSM) y relaciones entre ejercicios.
- **Incidencias:** la API de texto completo de Europe PMC dio errores 500 intermitentes; PMC (ncbi) pide CAPTCHA; PeerJ, Springer y la web de Europe PMC devolvieron 403 o redirección a login; la web de JSSM no sirvió el PDF. Lo que no se pudo abrir está marcado como **«solo resumen»** y de esos artículos no cito tablas.
- Convención: **«comprobado»** = he leído la cifra en la tabla o el texto de la fuente primaria. **«solo resumen»** = la cifra sale del resumen en Europe PMC/PubMed. **«derivado (P)»** = cuenta mía a partir de cifras comprobadas.

## Resumen (una pantalla)

**Hallazgo principal:** fuera de sentadilla, banca y peso muerto **no hay en la literatura revisada por pares ninguna tabla de percentiles de 1RM de población de gimnasio** con muestra grande y licencia abierta. Lo que existe es:

1. **Normas de organismo con máquinas antiguas** (Cooper Institute, recogidas por el ACSM): banca y prensa de piernas relativas al peso, por sexo y edad, en máquina Universal DVR. Libro con derechos reservados; no he podido ver la primaria (ver F12).
2. **Estudios pequeños (n = 12-70)** que miden el 1RM de un ejercicio en una población concreta: sirven como **punto de comparación**, no como percentiles.
3. **Relaciones medidas entre ejercicios** en las mismas personas (inclinado/plano, mancuernas/barra, jalón/dominada, hip thrust/sentadilla). Son la vía más prometedora para derivar un rango desde OpenPowerlifting, pero todas con n pequeño (nivel C).

| Ejercicio | Mejor fuente científica encontrada | Qué da | Licencia | Veredicto | Nivel |
|---|---|---|---|---|---|
| Press militar / de hombros | Hernández-Belmonte 2021 (F4); Saeterbakken y Fimland 2013 (F5) | 1RM medio en hombres entrenados (sentado en Smith); relaciones entre variantes de pie/sentado, barra/mancuernas | CC BY-NC 3.0 / cerrado | Solo para comparar | C |
| Press inclinado con barra | Marques 2025 (F3) | 1RM inclinado ≈ 97 % del plano en las mismas personas (novatos) | CC BY 4.0 | Usar con aviso (relación) | C |
| Remo con barra | Sigvaldsen 2023 (F17), pero es **remo tumbado en Smith**, no remo inclinado | 1RM remo tumbado ≈ 92 % de la banca en Smith | CC BY 4.0 | Solo para comparar | C |
| Prensa de piernas | Reynolds 2006 (F9); Kraemer 2026 (F10); Cooper/ACSM (F12); Parrino 2021 (F11, mayores) | Medias por sexo; normas por edad en máquinas concretas | Cerrado / cerrado / libro / cerrado | Solo para comparar (depende de la máquina) | C |
| Hip thrust | Hammond 2019 (F14) | 1RM de hip thrust ≈ 1,4-1,5 × sentadilla (medianas, 14 hombres entrenados) | CC BY 4.0 | Solo para comparar | C |
| Peso muerto rumano | — | Nada con 1RM comparable | — | Sin estándar | — |
| Sentadilla frontal | — (Yavuz 2015, Gullett 2009, Spitz 2019: no dan la relación en lo que he podido leer) | — | Cerrado | Sin estándar | — |
| Curl con barra | Wong 2013 (F7) | Ecuación 6RM curl = 0,33 × 6RM banca + 6,2 kg | Acceso abierto JSSM (sin CC vista) | Solo para comparar | C |
| Press con mancuernas | Saeterbakken 2011 (F6) | 1RM mancuernas ≈ 17 % menos que con barra (hombres entrenados) | Cerrado | Usar con aviso (relación) | C |
| Jalón al pecho | Johnson 2009 (F8) | 1RM jalón / peso: hombres 0,93 ± 0,17; mujeres 0,55 ± 0,11 | Cerrado | Solo para comparar | C |
| Dominada lastrada (extra) | Talaber 2022 (F2); Johnson 2009 (F8) | 1RM (peso corporal + lastre) / peso: 1,37 ± 0,15 (policía de élite); 1,16 / 0,73 (universitarios H/M) | CC BY 4.0 / cerrado | Usar con aviso | C |
| Extensión de cuádriceps, curl femoral | Hoeger 1990 (F13), no accesible; Lemmer 2007 (n = 8-11 por grupo) | — | Cerrado | Sin estándar | — |

**Recomendación:** con fuentes científicas no se puede dar un **percentil** honesto a ninguno de estos ejercicios. Como mucho, para el press inclinado y el press con mancuernas se podría proponer un estándar **derivado** de la banca de OpenPowerlifting por una relación medida (nivel C, con aviso) o, mejor, dejarlos «sin estándar» y enseñar la referencia como dato («en un estudio, el inclinado fue el 97 % del plano»). Para la prensa de piernas, la dependencia de la máquina (plate-loaded, Keiser neumática, Universal DVR) hace que cualquier cifra engañe: **sin estándar**.

---

## Fichas

### F1 · Isenmann et al. 2026, Sci Rep — normalización por sexo, edad y antropometría (solo SBD)

- **Cita:** Isenmann E, Geisler S, Havers T, Gavanda S, et al. *Normalization of muscle strength measures in barbell exercises by sex, age, and anthropometry: notes on allometry.* Sci Rep. 2026. DOI [10.1038/s41598-026-60646-z](https://doi.org/10.1038/s41598-026-60646-z) · [PMC13338387](https://europepmc.org/article/PMC/PMC13338387)
- **Tipo:** 2. **Licencia:** CC BY 4.0 (comprobado en el XML).
- **Población:** 393 personas (197 mujeres, 196 hombres), 16-64 años, de principiantes a powerlifters de élite; Alemania (material y afiliaciones alemanas; país de reclutamiento no explícito en lo que leí).
- **Método:** 1RM real en banca, sentadilla y peso muerto con material de competición.
- **Qué da:** coeficientes de regresión (edad, altura, log de masa) para un z-score, y tablas de categorías (Untrained… Elite) con rango de percentil, 1RM absoluto y relativo.
- **Ejemplos comprobados:** Tabla 4 (hombres, banca), «Intermediate»: percentil 27-75, 101,7 ± 18,9 kg, 1,20 ± 0,15 × peso. Tabla 3 (mujeres, banca), «Intermediate»: 47,8 ± 9,0 kg, 0,73 ± 0,13.
- **Ojo:** los propios autores concluyen que las clasificaciones por intervalos tienen mucha heterogeneidad dentro de cada categoría y **no las recomiendan**.
- **Veredicto:** **solo para comparar** (no cubre ejercicios nuevos). Útil como contraste del rango que ya da Forja con OpenPowerlifting en una población que incluye principiantes. **Nivel B.**

### F2 · Talaber et al. 2022, BMC Sports Sci Med Rehabil — unidad de policía de operaciones especiales

- **Cita:** Talaber KA, Orr RM, Maupin D, Schram B, Hasanki K, Roberts A, Robinson J. *Profiling the absolute and relative strength of a special operations police unit.* BMC Sports Sci Med Rehabil. 2022;14:111. DOI [10.1186/s13102-022-00502-5](https://doi.org/10.1186/s13102-022-00502-5) · [PMC9208152](https://europepmc.org/article/PMC/PMC9208152)
- **Tipo:** 2. **Licencia:** CC BY 4.0 (comprobado).
- **Población:** 47 policías varones australianos de élite, peso inicial 88,84 ± 8,25 kg; sin edad publicada; datos retrospectivos de 5 tomas en 18 meses.
- **Método:** 1RM real de banca, sentadilla, peso muerto y **dominada prona lastrada** (cinturón con lastre); «1RM documentado = peso corporal + lastre» (comprobado en el texto).
- **Ejemplos comprobados (Tabla 2, toma 1):** dominada 121,43 ± 14,91 kg absoluta, 1,37 ± 0,15 × peso; banca 109,67 ± 19,80 kg, 1,24 ± 0,20; sentadilla 125,79 ± 24,53 kg, 1,42 ± 0,25.
- **Derivado (P):** lastre medio ≈ 121,43 − 88,84 ≈ 32,6 kg.
- **Veredicto:** **usar con aviso** solo como referencia de dominada lastrada en hombres muy entrenados (población táctica, no de gimnasio general; solo hombres; sin percentiles). **Nivel C.**

### F3 · Marques et al. 2025, Sci Rep — press plano vs inclinado con barra libre

- **Cita:** Marques DL, Abohasel W, Alkahtani S, Alsaeed M, Algarzae NK, Neiva HP, Marinho DA, Marques MC. *Load-velocity relationship in the free-weight horizontal and incline bench press.* Sci Rep. 2025;15:26203. DOI [10.1038/s41598-025-12166-5](https://doi.org/10.1038/s41598-025-12166-5) · [PMC12274453](https://europepmc.org/article/PMC/PMC12274453)
- **Tipo:** 2/4 (relación medida). **Licencia:** CC BY 4.0 (comprobado).
- **Población:** 30 hombres, 26 ± 3 años, activos (fútbol, natación, tenis de mesa) **sin experiencia en entrenamiento de fuerza**; Arabia Saudí.
- **Método:** 1RM real en press plano e inclinado con barra libre, en sesiones distintas.
- **Ejemplo comprobado (Tabla 3):** 1RM plano 53,9 ± 10,5 kg; 1RM inclinado 52,5 ± 10,2 kg.
- **Derivado (P):** inclinado/plano ≈ 0,97. El texto cita un estudio previo en Smith con hombres entrenados donde el 1RM plano era mayor que el inclinado (no lo he leído).
- **Veredicto:** **usar con aviso** como relación (inclinado ≈ 97 % del plano en novatos); no extrapolar a entrenados sin otra fuente. No da percentiles. **Nivel C.**

### F4 · Hernández-Belmonte et al. 2021, Biol Sport — press de hombros sentado

- **Cita:** Hernández-Belmonte A, Martínez-Cava A, Morán-Navarro R, Courel-Ibáñez J, Pallarés JG. *A comprehensive analysis of the velocity-based method in the shoulder press exercise: stability of the load-velocity relationship and sticking region parameters.* Biol Sport. 2021;38(2):235-243. DOI [10.5114/biolsport.2020.98453](https://doi.org/10.5114/biolsport.2020.98453) · [PMC8139344](https://europepmc.org/article/PMC/PMC8139344)
- **Tipo:** 2. **Licencia:** **CC BY-NC 3.0** (comprobado): no comercial.
- **Población:** 48 hombres, 22,1 ± 3,5 años, 76,3 ± 8,8 kg, ≥ 4 años entrenando fuerza; criterio de inclusión: 1RM/peso > 0,60; Universidad de Murcia (España).
- **Método:** 1RM real de **press frontal sentado en máquina Smith** (respaldo inclinado 10°), no press militar de pie con barra libre.
- **Ejemplo comprobado (Tabla 5):** 1RM 69,1 ± 10,6 kg; 1RM/peso 0,91 ± 0,13. Tabla 3: terciles de fuerza relativa 0,77 ± 0,05 / 0,90 ± 0,05 / 1,06 ± 0,08.
- **Veredicto:** **solo para comparar** (variante distinta, muestra seleccionada por fuerza, licencia NC). **Nivel C.**

### F5 · Saeterbakken y Fimland 2013, JSCR — variantes de press de hombros

- **Cita:** Saeterbakken AH, Fimland MS. *Effects of body position and loading modality on muscle activity and strength in shoulder presses.* J Strength Cond Res. 2013;27(7):1824-1831. DOI [10.1519/JSC.0b013e318276b873](https://doi.org/10.1519/JSC.0b013e318276b873)
- **Tipo:** 4. **Licencia:** cerrado (NSCA/Wolters Kluwer). **Solo resumen.**
- **Población:** 15 hombres sanos (país y nivel de entrenamiento no constan en el resumen).
- **Qué da (solo resumen):** 1RM con mancuernas de pie ~7 % menor que con barra de pie, y ~10 % menor que con mancuernas sentado.
- **Veredicto:** **solo para comparar** (relaciones entre variantes; no relaciona el press de hombros con la banca). **Nivel C.**

### F6 · Saeterbakken, van den Tillaar y Fimland 2011, J Sports Sci — banca con barra, Smith y mancuernas

- **Cita:** *A comparison of muscle activity and 1-RM strength of three chest-press exercises with different stability requirements.* J Sports Sci. 2011;29(5):533-538. DOI [10.1080/02640414.2010.543916](https://doi.org/10.1080/02640414.2010.543916)
- **Tipo:** 4. **Licencia:** cerrado (Taylor & Francis). **Solo resumen.**
- **Población:** 12 hombres entrenados, 22,7 ± 1,7 años, 78,6 ± 7,6 kg.
- **Qué da (solo resumen):** carga con mancuernas 17 % menor que con barra y 14 % menor que en Smith; barra ~3 % más que Smith.
- **Duda:** el resumen no aclara si la «carga con mancuernas» es la suma de las dos. Hay que verlo en el texto completo antes de usarlo.
- **Veredicto:** **usar con aviso** como relación press con mancuernas ↔ banca, solo si se confirma el punto anterior. **Nivel C.**

### F7 · Wong et al. 2013, J Sports Sci Med — predecir cargas de ejercicios accesorios desde la banca

- **Cita:** Wong DP, Ngo KL, Tse MA, Smith AW. *Using bench press load to predict upper body exercise loads in physically active individuals.* J Sports Sci Med. 2013;12(1):38-43. PMID [24149723](https://pubmed.ncbi.nlm.nih.gov/24149723/) · [PMC3761768](https://europepmc.org/article/PMC/PMC3761768)
- **Tipo:** 4. **Licencia:** JSSM se declara de acceso abierto (definición BOAI: leer, descargar, copiar «para cualquier uso legal»), **no vi una licencia CC en el artículo**. **Solo resumen** (no pude abrir el texto completo).
- **Población:** 29 universitarios activos (país y reparto por sexo no constan en el resumen), 2,9 ± 2,1 años de experiencia, 1RM estimado de banca 54,31 ± 14,60 kg (0,80 ± 0,22 × peso).
- **Qué da (solo resumen):** ecuaciones con **6RM**, no 1RM: curl con barra = 0,33 × banca + 6,20 kg; press de hombros con mancuernas = 0,42 × banca + 5,84 kg; curl martillo = 0,28 × banca + 6,30 kg.
- **Veredicto:** **solo para comparar** (6RM, muestra pequeña, sin sexo claro). **Nivel C.**

### F8 · Johnson et al. 2009, JSCR — jalón y dominada máximos

- **Cita:** Johnson D, Lynch J, Nash K, Cygan J, Mayhew JL. *Relationship of lat-pull repetitions and pull-ups to maximal lat-pull and pull-up strength in men and women.* J Strength Cond Res. 2009;23(3):1022-1028. DOI [10.1519/JSC.0b013e3181a2d7f5](https://doi.org/10.1519/JSC.0b013e3181a2d7f5)
- **Tipo:** 2/4. **Licencia:** cerrado. **Solo resumen.**
- **Población:** universitarios de EE. UU., 35 hombres y 23 mujeres.
- **Método:** 1RM de jalón y 1RM de dominada (añadiendo lastre o contrapeso al peso corporal).
- **Qué da (solo resumen):** 1RM/peso en dominada 1,16 ± 0,15 (H) y 0,73 ± 0,09 (M); en jalón 0,93 ± 0,17 (H) y 0,55 ± 0,11 (M).
- **Veredicto:** **solo para comparar** (cerrado, n pequeño, la máquina de jalón no consta). Es la única referencia de jalón por sexo que he encontrado. **Nivel C.**

### F9 · Reynolds, Gordon y Robergs 2006, JSCR — prensa de piernas y banca, 18-69 años

- **Cita:** Reynolds JM, Gordon TJ, Robergs RA. *Prediction of one repetition maximum strength from multiple repetition maximum testing and anthropometry.* J Strength Cond Res. 2006;20(3):584-592. DOI [10.1519/R-15304.1](https://doi.org/10.1519/R-15304.1) (comprobado en Europe PMC). PDF en la web del autor: [unm.edu/~rrobergs](https://www.unm.edu/~rrobergs/478RMStrengthPrediction.pdf)
- **Tipo:** 2. **Licencia:** © 2006 NSCA (comprobado en el PDF); cerrado.
- **Población:** 70 adultos (34 H, 36 M), 18-69 años (30,6 ± 11,2), de la comunidad universitaria de Nuevo México (EE. UU.); 16 sin entrenar, 37 en circuito y 17 con rutina dividida.
- **Método:** 1RM real; **prensa Cybex plate-loaded no ajustable** («squat press») y banca con barra.
- **Ejemplos comprobados (Tabla 1):** total: 1RM prensa 264,1 ± 101,4 kg, 1RM banca 67,0 ± 34,5 kg. Hombres: «719 ± 224» prensa y «209 ± 64» banca; mujeres: «450 ± 112» y «90 ± 22».
- **Error en el original:** las columnas de hombres y mujeres están rotuladas en kg pero **son libras**: 719 lb ≈ 326 kg, 450 lb ≈ 204 kg, 209 lb ≈ 95 kg y 90 lb ≈ 41 kg. Con eso la media ponderada cuadra con el total (≈ 263 kg y ≈ 67 kg) y con el cociente 1RM/masa magra de la misma tabla (4,45 en hombres ≈ 326/72,8). Es deducción mía, no lo dice el artículo.
- **Derivado (P):** prensa/peso ≈ 3,8 (H, 326/85,0) y ≈ 3,2 (M, 204/63,4).
- **Veredicto:** **solo para comparar** (máquina concreta, n = 70, cerrado, error de unidades). **Nivel C.**

### F10 · Kraemer et al. 2026, MSSE — prensa de piernas de 18 a 85 años

- **Cita:** Kraemer WJ, Amadon G, Graham JH, Kackley ML, Sapper TN, Volek JS, Hayes JP, Hayes SM. *Lower-Body Strength, Lean Mass, and Bone Mineral Density Across the Adult Lifespan: Age- and Sex-Related Associations.* Med Sci Sports Exerc. 2026. DOI [10.1249/MSS.0000000000004074](https://doi.org/10.1249/MSS.0000000000004074) · [PMC13718888](https://europepmc.org/article/PMC/PMC13718888) (manuscrito de autor)
- **Tipo:** 2. **Licencia:** cerrado (revista); manuscrito gratuito en PMC sin licencia CC indicada. **Solo resumen**: la web y la API de Europe PMC no me sirvieron el texto completo (403 / 500).
- **Población:** 251 adultos (149 M, 102 H), 18-85 años; EE. UU.
- **Método:** 1RM de prensa de piernas (máquina no consta en el resumen).
- **Qué da:** el resumen solo dice que la fuerza baja con la edad; no he visto tablas por edad y sexo.
- **Veredicto:** **pendiente de leer**; candidata a «solo para comparar» si trae medias por franja de edad. **Nivel C** hasta verlo.

### F11 · Parrino et al. 2021, Exp Gerontol — prensa y press de pecho en mayores

- **Cita:** Parrino RL, Strand KL, Hockman AC, Signorile JF. *Leg press and chest press strength normative values by half-decades in older persons.* Exp Gerontol. 2021;150:111401. DOI [10.1016/j.exger.2021.111401](https://doi.org/10.1016/j.exger.2021.111401)
- **Tipo:** 2. **Licencia:** cerrado (Elsevier). **Solo resumen.**
- **Población:** 445 adultos de 60-85 años (EE. UU.).
- **Método:** 1RM en máquinas neumáticas **Keiser A420**; cuartiles por quinquenio y sexo.
- **Qué da (solo resumen):** hombres 60-64: prensa 237 ± 39 kg; 2,80 ± 0,53 × peso; 80-85: 172 ± 40 kg.
- **Veredicto:** **descartar** para Forja (solo > 60 años y máquina neumática, cuyos kg no equivalen a los de una prensa de discos). **Nivel C.**

### F12 · Cooper Institute (recogido en las Guidelines del ACSM) — banca y prensa relativas al peso

- **Fuente:** The Cooper Institute. *Physical Fitness Assessments and Norms for Adults and Law Enforcement* (monografía, Dallas, 2013) y tablas reproducidas en *ACSM's Guidelines for Exercise Testing and Prescription* (Wolters Kluwer).
- **Tipo:** 3 (organismo), con datos de población medida. **Licencia:** libro con todos los derechos reservados; el ACE reproduce la tabla de banca de mujeres «con permiso del Cooper Institute» ([PDF del ACE](https://www.acefitness.org/images/webcontent/assets/certification/ace-answers/forms/pt/38_Bench-Press_Assessment_Protocol.pdf)), lo que confirma que la tabla tiene dueño.
- **Población y método (según reproducciones, no verificado en la primaria):** adultos atendidos en la Cooper Clinic (EE. UU.), 1RM en **máquina Universal DVR** expresado como 1RM/peso, percentiles por sexo y década.
- **Indicio independiente:** Covington et al. 2025 (MSSE, [10.1249/MSS.0000000000003702](https://doi.org/10.1249/MSS.0000000000003702)) comparan 659 pacientes con cáncer con «normative values from the apparently healthy population as reported by the ACSM» para press de pecho y prensa: confirma que el ACSM publica esas normas.
- **Cifras:** no cito ninguna. Una web de terceros (fitnessnorms.pages.dev) reproduce la banca Cooper de hombres de 20-29 años (p50 = 1,06 × peso), pero **no es la primaria** y no lo doy por comprobado.
- **Veredicto:** **solo para comparar**, y solo si alguien consulta el libro. La máquina Universal DVR ya casi no existe en gimnasios. **Nivel B** para la población original; **C** aplicado a Forja. Detalle en la investigación del tipo 3.

### F13 · Hoeger et al. 1990, J Appl Sport Sci Res — 7 ejercicios en máquina

- **Cita:** Hoeger WWK, Hopkins DR, Barette SL, Hale DF. *Relationship between repetitions and selected percentages of one repetition maximum: a comparison between untrained and trained males and females.* J Appl Sport Sci Res. 1990;4(2):47-54. Sin DOI.
- **Tipo:** 2. **Licencia:** cerrado (NSCA). **No he podido ver ni el resumen con cifras.**
- **Qué cubre (según revisiones que lo citan):** prensa de piernas, jalón, banca, extensión de cuádriceps, abdominales, curl femoral y curl de brazo, en hombres y mujeres entrenados y sin entrenar.
- **Veredicto:** **descartar mientras no se vea el texto**. Sería la única fuente con extensión de cuádriceps y curl femoral, pero con n pequeño y máquinas de 1990. **Nivel C** como mucho.

### F14 · Hammond et al. 2019, PeerJ — hip thrust y sentadilla en hombres entrenados

- **Cita:** Hammond A, Perrin C, Steele J, Giessing J, Gentil P, Fisher JP. *The effects of a 4-week mesocycle of barbell back squat or barbell hip thrust strength training upon isolated lumbar extension strength.* PeerJ. 2019;7:e7337. DOI [10.7717/peerj.7337](https://doi.org/10.7717/peerj.7337) · [PMC6662562](https://europepmc.org/article/PMC/PMC6662562)
- **Tipo:** 4. **Licencia:** CC BY 4.0 (comprobado).
- **Población:** 14 hombres entrenados, 22,07 ± 0,62 años, 79,77 ± 13,81 kg (país no comprobado).
- **Método:** 1RM real de sentadilla y de hip thrust con barra en las mismas personas.
- **Ejemplos comprobados (Tabla 2, antes de entrenar, medianas ± RIC):** grupo sentadilla: sentadilla 115,0 ± 35,0 kg, hip thrust 160,0 ± 42,5 kg; grupo hip thrust: sentadilla 110,0 ± 7,5 kg, hip thrust 162,5 ± 25,0 kg.
- **Derivado (P):** hip thrust / sentadilla ≈ 1,39-1,48 (cociente de medianas, no mediana de cocientes).
- **Veredicto:** **solo para comparar** (n = 14, solo hombres, el hip thrust depende mucho de la técnica y del recorrido). **Nivel C.**

### F15 · Plotkin et al. 2023, Front Physiol — hip thrust vs sentadilla en novatos

- **Cita:** Plotkin DL, et al. *Hip thrust and back squat training elicit similar gluteus muscle hypertrophy and transfer similarly to the deadlift.* Front Physiol. 2023;14:1279170. DOI [10.3389/fphys.2023.1279170](https://doi.org/10.3389/fphys.2023.1279170) · [PMC10593473](https://europepmc.org/article/PMC/PMC10593473)
- **Licencia:** CC BY 4.0 (comprobado). **Población:** 34 universitarios sin entrenar (5 H y 13 M en el grupo hip thrust), EE. UU.
- **Método:** **3RM**, no 1RM; los resultados de fuerza van en figuras.
- **Veredicto:** **descartar** como norma (3RM, novatos, sin tablas de fuerza). **Nivel C.**

### F16 · Barbalho et al. 2020, Int J Sports Med — hip thrust en mujeres entrenadas

- **Cita:** Barbalho M, Coswig V, Souza D, Serrão JC, Hebling Campos M, Gentil P. *Back Squat vs. Hip Thrust Resistance-training Programs in Well-trained Women.* Int J Sports Med. 2020;41(5):306-310. DOI [10.1055/a-1082-1126](https://doi.org/10.1055/a-1082-1126)
- **Licencia:** cerrado. **Solo resumen**, que no da los 1RM iniciales.
- **Población:** 22 mujeres entrenadas, Brasil.
- **Veredicto:** **descartar** mientras no se vea el texto; sería la única referencia de hip thrust en mujeres entrenadas. **Nivel C.**

### F17 · Sigvaldsen et al. 2023, PLOS ONE — banca y remo tumbado en Smith

- **Cita:** Sigvaldsen E, Loturco I, Larsen F, Bruusgaard J, Kalhovde JM, Haugen T. *Validity and reliability of upper body push and pull tests to determine one-repetition maximum.* PLoS One. 2023;18(7):e0288649. DOI [10.1371/journal.pone.0288649](https://doi.org/10.1371/journal.pone.0288649) · [PMC10343053](https://europepmc.org/article/PMC/PMC10343053)
- **Tipo:** 4. **Licencia:** CC BY 4.0 (comprobado).
- **Población:** 28 adultos activos (22 H, 6 M), 25 ± 2 años, Noruega, 2021-2022.
- **Método:** 1RM real en **máquina Smith**: banca y **prone bench pull** (remo tumbado en banco), no remo inclinado con barra libre.
- **Ejemplo comprobado (Tabla 1, sesión 1):** banca 78,3 ± 27,8 kg; remo tumbado 72,4 ± 16,7 kg.
- **Derivado (P):** remo tumbado / banca ≈ 0,92 (cociente de medias, sexos mezclados).
- **Veredicto:** **solo para comparar** (variante y máquina distintas, sexos mezclados). **Nivel C.**

### F18 · Estudios pequeños que también miden 1RM accesorios (agrupados)

| Estudio | Qué mide | n | Licencia | Veredicto |
|---|---|---|---|---|
| Lemmer et al. 2007, JSCR, [10.1519/R-20816.1](https://doi.org/10.1519/R-20816.1) | Press de pecho, jalón, press de hombros, tríceps, curl, prensa y extensión, por edad y sexo | 8-11 por grupo | Cerrado (solo resumen) | Descartar: n mínimo |
| Grosicki et al. 2014, Clin Interv Aging, [10.2147/CIA.S55719](https://doi.org/10.2147/CIA.S55719) | Prensa, extensión y curl de bíceps | 16 jóvenes, 32 mayores | CC BY-NC (según Europe PMC) | Descartar: n mínimo |
| Shimano et al. 2006, JSCR, [10.1519/R-18195.1](https://doi.org/10.1519/R-18195.1) | Sentadilla, banca y curl con barra | 8 + 8 hombres | Cerrado (solo resumen, sin cifras de 1RM) | Descartar |

### F19 · Sentadilla frontal y peso muerto rumano: sin relación verificable

- Sentadilla frontal: Russell y Phillips 1989 ([10.1080/02701367.1989.10607441](https://doi.org/10.1080/02701367.1989.10607441), n = 8), Gullett 2009 ([10.1519/JSC.0b013e31818546bb](https://doi.org/10.1519/JSC.0b013e31818546bb), n = 15), Yavuz 2015 ([10.1080/02640414.2014.984240](https://doi.org/10.1080/02640414.2014.984240), n = 12) y Spitz 2019 ([10.1519/JSC.0000000000002962](https://doi.org/10.1519/JSC.0000000000002962), n = 11). Todos cerrados; **ningún resumen da el cociente frontal/trasera** y no he podido leer los textos. No cito cifras.
- Peso muerto rumano: lo que aparece (p. ej. Lee et al. 2018, J Exerc Sci Fit, [PMC6323186](https://europepmc.org/article/PMC/PMC6323186)) usa cargas submáximas para EMG, no 1RM de rumano.
- **Veredicto:** **sin estándar** para ambos con fuentes científicas. Cualquier cociente que circule (p. ej. «la frontal es el 80-85 % de la trasera») es **heurística (H)** mientras no aparezca un estudio.

### F20 · Montenegro et al. 2026, Sports Med Open — percentiles de powerlifters (fuera de alcance)

- DOI [10.1186/s40798-026-01111-z](https://doi.org/10.1186/s40798-026-01111-z) · [PMC13582683](https://europepmc.org/article/PMC/PMC13582683), CC BY. 101.898 powerlifters «classic» con control antidopaje, 21-49 años, percentiles 50/75/90/99 por categoría IPF. Solo SBD y sobre OpenPowerlifting: es el terreno del tipo 1. Lo dejo anotado para contrastar el cálculo propio de Forja. **Solo para comparar, nivel B.**

---

## Cómo encajaría en Forja (lo que sale de esta parte)

- **Tabla `estandar_fuerza` (percentiles 1-99 por levantamiento, sexo y franja de peso):** ninguna fuente de esta investigación permite rellenarla para un ejercicio nuevo. Todas dan media ± DE de muestras pequeñas; convertir eso en percentiles asumiendo normalidad sería inventarse la cola.
- **Métrica «relación con un levantamiento de OpenPowerlifting»:** es la única vía con algo de base. Habría que guardar, por ejercicio derivado: el levantamiento base, el coeficiente, la fuente (DOI), la población y el nivel. Candidatos:
  - Press inclinado = 0,97 × banca (F3, novatos, nivel C).
  - Press con mancuernas = 0,83 × banca (F6, si se confirma que es la suma de las dos mancuernas; nivel C).
  - El resto (hip thrust 1,4-1,5 × sentadilla, remo tumbado 0,92 × banca, curl por ecuación de 6RM) tiene demasiada incertidumbre: mejor no.
- **Propuesta (P):** si se hace, enseñarlo como «estimado a partir de tu banca» y nunca como percentil propio del ejercicio, con el nivel C visible.

## Riesgos y dudas abiertas

- **Poblaciones:** novatos saudíes (F3), policías australianos de élite (F2), universitarios (F7, F8), mayores (F11). Ninguna es «gente de gimnasio» en general, y casi ninguna incluye mujeres en número suficiente.
- **Máquinas:** prensa (Cybex plate-loaded, Keiser neumática, Universal DVR) y Smith frente a barra libre cambian los kg. Un estándar de prensa sin saber la máquina del usuario es engañoso.
- **Licencias:** varias son cerradas (JSCR, J Sports Sci, Exp Gerontol, MSSE). Los **datos** sueltos (una media, un cociente) son hechos y en general se pueden citar con referencia; la **tabla tal cual** no se debe copiar. F4 es CC BY-NC: el uso en una app pública con fines comerciales sería dudoso. Es una duda jurídica que esta investigación no resuelve.
- **Errores en originales:** Reynolds 2006 (F9) rotula en kg columnas que son libras.
- **Sin leer:** Kraemer 2026 (F10), Hoeger 1990 (F13), Barbalho 2020 (F16), los de sentadilla frontal (F19) y la monografía Cooper (F12). Si alguien tiene acceso institucional, son los primeros a mirar.
