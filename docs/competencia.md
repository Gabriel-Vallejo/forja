# Forja: competencia a fondo

Ampliación de la sección 1 de [fase-0.md](fase-0.md). Cada app dice cómo se analizó y en qué fecha.

## BodyLeveling

**Análisis desde la web, 2026-11-15.** No la he probado: solo existe para iPhone y no tenemos uno. Las fuentes son su web, sus guías, su FAQ y su política de privacidad. La ficha del App Store (`id6803004585`, el enlace que da su propia web) devuelve 404 en las tiendas de EE. UU., España y Francia. Tampoco he encontrado reseñas independientes.

### Qué hace
- Le das tres fotos (frente, espalda y perfil) y puntúa **14 grupos musculares** de 0 a 100 respecto a tu objetivo. Las bandas son: ≥ 70 fuerte, 45-69 en desarrollo y < 45 rezagado.
- Calcula una puntuación global ponderada por objetivo: definición, masa, rendimiento o equilibrio.
- El primer escaneo añade de 0 a 6 «rasgos estructurales» de tres tipos: inserción, estructura y proporción.
- Te da de **3 a 7 ejercicios** priorizados por los músculos rezagados, con series y un rango de **8-15 repeticiones**, colocados en la división de entreno que ya sigues.
- También da calorías y macros, alimentos a priorizar o limitar, 8 000 pasos al día y check-in semanal con tres fotos nuevas (según su FAQ).
- Se integra con Yazio, MyFitnessPal y Cronometer.
- **Precio:** el primer escaneo es gratis pero muestra solo una parte. Pro cuesta 7,99 €/mes o 24,99 €/año, a través del App Store.

### Cómo usa las proporciones (lo que dice y lo que no)
- **Lo que dice:** que las inserciones «cambian qué ejercicios funcionan» y que las proporciones de extremidades y torso fijan la palanca, y la palanca los rangos de repeticiones y los ángulos.
- **Lo que enseña** (guía de inserciones):
  - bíceps de inserción corta: trabajo en posición estirada;
  - gemelos de inserción alta: más volumen;
  - fémur largo respecto al torso: cambiar de postura, de posición de la barra o de variante en la sentadilla;
  - brazos largos: el recorrido del press de banca cambia.
- **Lo que no enseña:**
  - ningún ejercicio concreto por rasgo, ni grados, ni repeticiones distintas de 8-15;
  - ninguna cifra de precisión, que llama «estimación»;
  - ningún estudio ni validación;
  - ningún método: solo dice «un modelo de visión».
- **Cómo funciona por dentro:** su política de privacidad dice que manda las tres fotos y el perfil a **Google Gemini**. El escaneo es, por tanto, un LLM multimodal genérico que mira las fotos, no una medición de puntos del cuerpo.

### Privacidad
- Las fotos pasan por AWS S3 de forma temporal y por Gemini. Se borran al guardar el análisis; su FAQ añade que no se usan para entrenar modelos (la política de privacidad no lo dice). El perfil y los resultados se guardan hasta que borras la cuenta o pasan 24 meses de inactividad.
- No pide cuenta: usa un identificador aleatorio del dispositivo (RevenueCat).
- El responsable es un desarrollador individual en Francia. La política se apoya en la ejecución del contrato y **no menciona el art. 9 del RGPD** para fotos corporales ni datos de salud.

### Qué significa para Forja
BodyLeveling **dice** que elige por proporciones, pero no lo **demuestra**. Lo que se ve es un LLM que puntúa músculos a ojo y una rutina genérica de 8-15 repeticiones centrada en los puntos débiles. El hueco de Forja sigue abierto si hace lo que ellos no hacen:

| | BodyLeveling | Forja (plan) |
|---|---|---|
| Medida | Gemini «mira» las fotos | Puntos clave (MediaPipe) → ratios numéricos, evaluados con cinta métrica en `eval/` |
| Regla ratio → ejercicio | No publicada | Explícita y explicada en la app, marcada como heurística de entrenadores |
| Rutina | 3-7 ejercicios, 8-15 reps, dentro de tu división | Semana completa resuelta con CP-SAT bajo restricciones |
| Precisión | Sin cifra | Error publicado por versión |
| Fotos | Salen a Google y AWS | Se procesan en nuestro worker y se borran; solo quedan ratios |
| RGPD art. 9 | No lo cita | Consentimiento explícito |
| Plataforma | Solo iPhone | Android (APK) y PC |

**Riesgo:** su marketing ya ocupa la frase «tu palanca decide tus ejercicios». Forja tiene que enseñar la regla y el número, no repetir el eslogan.

### Fuentes
- [Web](https://bodyleveling.com/) · [Escaneo con IA](https://bodyleveling.com/ai-body-scan/) · [Inserciones musculares](https://bodyleveling.com/muscle-insertions/) · [Soporte](https://bodyleveling.com/support) · [Privacidad](https://bodyleveling.com/privacy)

## Symmetry (España): la competencia más cercana en entreno

**Análisis desde la web y probada a mano en Android, 2026-11-15** ([symmetry.club/es](https://symmetry.club/es) y [rangos](https://symmetry.club/es/rangos)).

### Qué hace
- App española, gratis en **Android** e iOS. Según su web, más de 3 M de descargas, más de 1 M de usuarios activos y 4,8 de valoración.
- **Rutina con IA:** la genera a partir de la experiencia, el objetivo, los días y el material. Sugiere pesos y repeticiones serie a serie, con sobrecarga progresiva.
- **Registro de series:** historial, volumen por músculo, rachas y récords.
- **Rangos por músculo** (nueve niveles, de Hierro a Simétrico) y **mapa muscular coloreado por rango**. Cada serie puntúa contra **su propia comunidad**, separada por sexo y ajustada al peso corporal con escalado alométrico. Los rangos de los ejercicios de un músculo se combinan «dando más peso a los ejercicios con más series».
- Feed social y un chat con IA («Symmetry Intelligence»).
- **Precio:** gratis; Pro (sugerencia de peso y repeticiones) a 12,99 €/mes o 39,99 €/año en la App Store de España.

### Lo que no hace o no explica
- **No usa proporciones corporales ni palancas** para elegir ejercicios. No pide fotos de cuerpo.
- **No publica el método:** dice que la fórmula exacta no la publica. No da precisión, no cita estudios y no explica cómo reparte una serie entre músculos primarios y secundarios.
- Ni dieta ni suplementos.

### Probada a mano (20 capturas)
- **Navegación:** pestañas Amigos, Rangos, Entrenar, Biblioteca y Perfil. «Tu Plan» es un carrusel de días (p. ej., «EMPUJE · 96 min · 8 ejercicios») con tres alternativas: biblioteca de programas, «generar con IA (sitio + tiempo)» y entreno libre.
- **Rangos:** tarjeta con el rango global (gema + nivel I-III + «top N %»), mapa corporal frontal y trasero coloreado por rango y lista por grupo muscular. Los músculos sin datos salen «SIN RANGO».
- **Calculadora de 20 s:** eliges uno de 6 ejercicios básicos, das tu mejor serie (reglas deslizantes de peso y repeticiones) y devuelve el rango con una campana de la población y tu punto, más el aviso «es una estimación; tu rango oficial se gana entrenando».
- **Perfil:** «escaneo de cuerpo» de tipo antes/después (no mide proporciones), medidas con candado PRO salvo el peso, y racha con calendario.
- **Lo que falla:** los rangos dicen dónde estás flojo pero no qué hacer; los programas son por categoría, no por persona; ninguna pantalla explica por qué hay cada ejercicio.
- **No capturado:** onboarding, registro de series, progresión y muro de pago.

### Qué significa para Forja
- **El mapa muscular coloreado y la rutina con IA ya existen en España, gratis y en Android, con millones de usuarios.** No son diferenciales de Forja. El plan los citaba «como Symmetric Strength», pero la referencia real en el mercado español es Symmetry.
- **Lo que sigue siendo de Forja:**
  - **las palancas**, con el error medido frente a cinta;
  - **reglas y estándares abiertos:** percentiles de OpenPowerlifting con fuente pública, series secundarias a 0,5 citado (Pelland 2026) y la traza del solver;
  - **la dieta por precio** en España;
  - **el escáner de suplementos.**
- **Riesgo:** quien ya usa Symmetry no cambia de app por un mapa muscular. El gancho tiene que ser lo que Symmetry no tiene.

## Symmetric Strength (web)
**Web y probada a mano, 2026-11-15** (22 capturas). [Web](https://symmetricstrength.com/) · [Metodología](https://symmetricstrength.com/about#methodology)
- **Entrada mínima:** sexo, peso corporal, edad opcional y tu mejor serie reciente (1-10 repeticiones, RPE opcional) de hasta 14 levantamientos agrupados por patrón. Con una sola serie ya puntúa.
- **Escala:** «Strength Score» con 8 niveles de color, de Untrained (30) a World class (125). **Ellos mismos aclaran que los niveles son bandas de su modelo, no percentiles de población.**
- **Método publicado con un ejemplo trabajado en 5 pasos:** nivel → puntos Wilks (ajustados por edad) → total de powerlifting equivalente → cuota fija de cada levantamiento en el total → peso objetivo según tus repeticiones. Lleva fecha de revisión y descargos honestos.
- **Datos de su comunidad:** mediana de 1RM por levantamiento, publicada solo si hay 200 o más levantadores.
- **Cobertura muscular** (7 o 30 días): series sin calentamiento, **músculo principal = 1 y secundario = 0,5**, la misma regla que Forja toma de Pelland 2026. Se activa a partir de 3 sesiones.
- **Lo que falla:** solo 14 levantamientos de barra o lastre; el mismo reparto del total para todo el mundo, aunque las palancas cambien; solo en inglés y con mucha navegación.
- **No capturado:** la pantalla de resultados (puntuación, simetría y puntos débiles ya calculados).

## Fitbod
**Probada a mano en Android, 2026-11-15** (45 capturas y una nota de uso).
- **Onboarding conversacional:** un coach en burbujas de chat y un botón por respuesta. Pregunta objetivo, constancia, antigüedad, tipo de gimnasio, gimnasio concreto por geolocalización, el equipo al detalle (unas 60 casillas, incluidos los pesos de las mancuernas), fuerza actual opcional, días por semana y división, con una «Recomendada».
- **Plan antes de entrenar:** resumen con la semana, silueta muscular por día y «Por qué funciona este plan». Son tres frases genéricas, no por ejercicio.
- **Ejercicio de enfoque:** un básico que se repite durante un mes con intensidad creciente.
- **Ficha de ejercicio:** peso y repeticiones, vídeo, historial, calculadora de discos y **Reemplazar**. Con **Más** puedes poner notas, añadir calentamiento, recomendarlo con más o menos frecuencia, «no recomendar de nuevo» y eliminarlo.
- **Recuperación muscular** en una silueta, que se activa con el primer entreno. «Objetivos semanales de series» por empuje, tirón y piernas, sin decir de dónde salen.
- **Aviso honesto:** «los pesos iniciales son estimaciones; tus ajustes calibran los siguientes».
- **Lo que falla:** no explica cada ejercicio, la lista de equipo abruma y mete preguntas de marketing en el onboarding.
- **No capturado:** registro de series en curso, temporizador, regla de progresión y muro de pago.

## Gravl
**Probada a mano en Android, 2026-11-15** (53 capturas).
- **Onboarding de unas 25 pantallas en 11 pasos:** razón, objetivo, experiencia, perfil de salud (sexo, edad, altura y peso, cada uno con «para qué lo usamos»), máximos opcionales con proyección a 8 semanas, frecuencia, rutina (que elija la IA, una fija o «rutina de recuperación»), **nivel de variedad** (consistente, equilibrado o variado), **hasta 4 músculos excluidos y hasta 2 enfocados**, equipo por gimnasio y notificaciones. Casi todo se puede omitir.
- **No mide proporciones:** solo altura y peso. Las fotos son de progreso. El «análisis de técnica por vídeo» solo sale como reclamo.
- **No explica la elección de ejercicios.** Su única «explicación» es comercial: «usuarios con un 1RM similar mejoran un 19 % en 8 semanas».
- **Strength Score** por ejercicio, con la mejor serie de 3 meses y el 1RM estimado. Separa series de calentamiento y efectivas, y marca un «objetivo» por serie.
- **Series por músculo** en aros numéricos, sin mapa coloreado. Rachas con «congelaciones».
- **Lo que falla:**
  - un onboarding largo, con marketing entre pregunta y pregunta;
  - mezcla de español, inglés y voseo;
  - un plan de ejemplo de 85 min y 28 series, que se sale de sus propias franjas.
- **No capturado:** registro de series en curso, regla de progresión y muro de pago (solo se ve un contador «Prueba: 3/3»).

## Hevy
**Probada a mano en Android, 2026-11-15** (19 capturas).
- **Navegación:** Inicio (feed social), Entrenamiento (rutinas como tarjetas con «Empezar rutina», entreno vacío y explorar) y Perfil (gráfico semanal de duración, volumen y repeticiones).
- **Estadísticas gratis:** mapa corporal de los últimos 7 días, radar de distribución muscular y biblioteca de ejercicios con el músculo resaltado en cada miniatura. Exporta e importa datos.
- **Trainer (PRO):** programa por chips (split, objetivo, músculo objetivo, nivel y variedad) con sobrecarga automática, sin decir cómo decide.
- **De pago:** casi todas las medidas corporales y el historial de más de 3 meses.
- **No capturado:** el registro de series en curso, que es justo el motivo por el que estaba en la lista.

## Qué se lleva Forja
Ninguna de las cinco **explica por qué hay cada ejercicio** ni **usa proporciones del cuerpo**. El primer hueco es el de Forja. El segundo no se cubre midiendo palancas: la investigación de la fase 1 mostró que no sostienen la elección de ejercicios ([ADR 0007](adr/0007-motor-de-seleccion-de-ejercicios.md)). Las ideas de UX, ordenadas por fase:

| Idea | De dónde sale | Cómo encaja en Forja | Fase |
|---|---|---|---|
| «Por qué este ejercicio» en cada tarjeta (palanca, punto débil, percentil) | Hueco de todas | Texto de la traza del solver ([ADR 0002](adr/0002-solver-cp-sat.md)) | 1 |
| Recomendar más o menos, no recomendar, reemplazar | Fitbod (Más) | Penalización o prohibición en el objetivo de CP-SAT, y re-solve | 1 |
| Nivel de variedad (consistente, equilibrado o variado) | Gravl | Peso de la penalización por repetir ejercicios | 1 |
| Excluir hasta 4 músculos y enfocar hasta 2 | Gravl | Prioridades del objetivo; el enfoque por defecto sale de los puntos débiles medidos | 1 |
| Equipo por gimnasio con los pesos disponibles | Fitbod y Gravl | Restricción dura y redondeo de cargas | 1 |
| «Para qué lo usamos» bajo cada dato pedido | Gravl | Imprescindible para las fotos y medidas (art. 9, [ADR 0004](adr/0004-privacidad-fotos-y-datos-de-salud.md)) | 1-2 |
| Aviso de calibración de los pesos iniciales | Fitbod | Primer mesociclo de la progresión doble | 1 |
| Calentamiento separado de las series efectivas, con objetivo por serie | Gravl y Fitbod | No cuenta para el volumen; el objetivo sale de la progresión doble | 1 |
| Vista previa del plan antes de confirmar (duración, ejercicios y series) | Gravl y Fitbod | Pantalla de resultado del solver | 1 |
| Calculadora rápida con una sola serie | Symmetry y Symmetric Strength | Onboarding de estándares sin historial | 2 |
| Dos escalas etiquetadas: nivel del modelo y percentil de OpenPowerlifting, con «es una estimación» | Symmetric Strength y Symmetry | Mapa y estándares, con el sesgo de los competidores dicho | 2 |
| Mapa muscular con estados (débil, en la media, fuerte, sin datos) y «registra N sesiones» si faltan datos | Symmetry, Fitbod y Symmetric Strength | Mapa de puntos débiles, enlazado al ejercicio que lo trabaja | 2 |
| Método visible con un ejemplo trabajado en pocos pasos | Symmetric Strength | Página de método en la app y en el README | 2 |

**Lo que no copiar:**
- medidas corporales tras un muro de pago (Hevy y Symmetry);
- onboarding inflado de marketing (Gravl);
- feed social como pantalla principal (Hevy);
- programas por categoría en lugar de por persona (Symmetry).

**Falta por ver:** el registro de una serie en curso en Symmetry, Hevy y Fitbod (es el uso diario de Forja) y la pantalla de resultados de Symmetric Strength.
