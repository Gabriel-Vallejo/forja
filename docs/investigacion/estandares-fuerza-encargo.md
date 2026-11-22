# Encargo: estándares de fuerza más allá de sentadilla, banca y peso muerto

Fecha: 2026-11-22 · Pedido por Gabriel · Resultado en [estandares-fuerza.md](estandares-fuerza.md)

## 1. Para qué

Hoy solo la sentadilla, la banca y el peso muerto con barra dan rango en Forja (percentiles de OpenPowerlifting por sexo y franja de peso, RF-30). El resto de ejercicios sale «sin estándar». Queremos saber **qué otros ejercicios pueden tener un rango honesto**: con una fuente que se pueda citar, una licencia que deje usarla en una app pública y una población parecida a quien entrena en un gimnasio.

Un rango falso es peor que ninguno. Si un ejercicio no tiene fuente buena, se queda «sin estándar».

## 2. Qué buscar

### Ejercicios, por prioridad

Del catálogo curado (63 ejercicios que usa el solver), los que más gente registra y que cubren los músculos que hoy salen sin rango:

| Prioridad | Ejercicio | Músculo del mapa | Métrica natural |
|---|---|---|---|
| 1 | Press militar de pie con barra | Hombros | 1RM (kg) |
| 1 | Dominadas (prono y supino) | Dorsal, bíceps | Repeticiones con el peso corporal, o 1RM con lastre |
| 1 | Remo con barra inclinado | Dorsal, espalda media | 1RM |
| 1 | Press inclinado con barra | Pecho | 1RM |
| 1 | Prensa de piernas | Cuádriceps | 1RM (depende mucho de la máquina) |
| 2 | Hip thrust con barra | Glúteos | 1RM |
| 2 | Peso muerto rumano | Isquios, glúteos | 1RM |
| 2 | Sentadilla frontal | Cuádriceps | 1RM |
| 2 | Curl con barra | Bíceps | 1RM |
| 2 | Fondos en paralelas | Pecho, tríceps | Repeticiones o lastre |
| 2 | Flexiones | Pecho, tríceps | Repeticiones |
| 3 | Press con mancuernas (banca, hombros), jalón, remo en polea, extensiones, elevaciones laterales | Varios | 1RM o repeticiones |
| 3 | Plancha, elevación de piernas colgado | Abdomen | Tiempo o repeticiones |

### Tipos de fuente, de mejor a peor

1. **Datos individuales abiertos** (como OpenPowerlifting): marcas de personas reales, con sexo, peso corporal y edad, en un archivo descargable y con licencia clara. Con eso se calculan percentiles propios.
2. **Datos normativos publicados en ciencia** (artículos con revisión por pares): tablas de percentiles o medias y desviaciones por sexo y edad o peso, de poblaciones medidas (estudiantes, adultos activos, militares, deportistas). Hay que fijarse en si la población se parece a la de un gimnasio.
3. **Normas de organismos** (ACSM, NSCA, Cooper Institute, ejércitos, pruebas de acceso de bomberos o policía): tablas de referencia para dominadas, flexiones, fuerza relativa…
4. **Relaciones entre ejercicios con estudio detrás**: por ejemplo, cuánto es el press militar respecto a la banca, o el press inclinado respecto al plano. Con una relación medida, el estándar de un ejercicio sale del de OpenPowerlifting. Si la relación es solo de entrenadores, se marca como heurística (nivel H).
5. **Webs de estándares de fuerza** (Strength Level, Symmetric Strength, ExRx/Kilgore, Lon Kilgore, Mark Rippetoe…): sirven para comparar y como referencia, pero casi seguro no se pueden copiar. Hay que leer sus términos y decir qué dejan hacer.

## 3. Cómo buscar

- Búsquedas en inglés y en español: *strength standards*, *normative data 1RM*, *percentile pull-ups adults*, *relative strength norms*, *overhead press bench press ratio*, *1RM leg press normative*, *dataset weightlifting individual records*, *military fitness test pull-ups percentile*, *hip thrust 1RM normative*.
- Bases académicas: PubMed y Europe PMC (que tienen API), Google Scholar y las revistas de la NSCA y del ACSM (*JSCR*, *MSSE*).
- Para cada fuente candidata, **ir a la primaria**: el artículo, la página del organismo o el repositorio del dataset, no un resumen de un blog. Si la primaria está tras un muro de pago, decirlo y no inventar las cifras.
- **Licencia:** leerla en la propia fuente (LICENSE, términos de uso, licencia del artículo: CC BY, CC BY-NC, todos los derechos reservados). En las tablas de un artículo, separar los **datos** (hechos, que en general no tienen derechos de autor) de la **tabla tal cual** (puede tenerlos). Si la duda es jurídica, decirlo como duda: no lo resuelve esta investigación.
- **Población y método:** tamaño de la muestra; quiénes (estudiantes, atletas, militares, gimnasio); cómo se midió (1RM real, estimado con repeticiones, máquina concreta); sexo, edad y peso; país y año.
- **No dar nada por bueno sin verlo.** Cada cifra que salga en el resultado tiene que tener su enlace y, si se ha comprobado, decirlo. Lo que no se pueda comprobar, va marcado.

## 4. Qué entregar

En `docs/investigacion/estandares-fuerza.md`:

1. **Resumen de una pantalla:** qué ejercicios pueden tener rango ya, cuáles con trabajo, cuáles no, y la recomendación.
2. **Una ficha por fuente** con: nombre, enlace a la primaria, tipo (1 a 5 de arriba), licencia y qué permite, población (quién, n, país, año), variables (sexo, edad, peso), método de medida, ejercicios que cubre, formato de los datos (CSV, tabla en PDF, web), y un veredicto: **usar**, **usar con aviso**, **solo para comparar** o **descartar**, con el motivo.
3. **Una tabla por ejercicio** con la mejor fuente disponible, cómo se integraría y su nivel de evidencia en la escala de la app (A, B, C, D, H o P de [docs/referencias.md](../referencias.md)).
4. **Cómo se integraría** en Forja, encajando con lo que ya existe: la tabla `estandar_fuerza` (percentiles 1-99 por levantamiento, sexo y franja de peso), la métrica (1RM en kg, repeticiones con el peso corporal o relación con un levantamiento de OpenPowerlifting) y lo que habría que cambiar en el esquema si la métrica no es un 1RM.
5. **Riesgos y dudas abiertas:** licencias dudosas, poblaciones que no encajan y sesgos.

## 5. Límites

- Fines de fitness, no sanitarios: nada de «normal» o «patológico». Es «dónde estás frente a esta población».
- Ni se copia código de ninguna fuente ni se raspan webs cuyos términos lo prohíban.
- Datos de personas: solo agregados o datos ya publicados como abiertos; nada que identifique a alguien.
