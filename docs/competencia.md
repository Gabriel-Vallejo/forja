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

**Análisis desde la web, 2026-11-15** ([symmetry.club/es](https://symmetry.club/es) y [rangos](https://symmetry.club/es/rangos)). Pendiente de probarla a mano en Android.

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

### Qué significa para Forja
- **El mapa muscular coloreado y la rutina con IA ya existen en España, gratis y en Android, con millones de usuarios.** No son diferenciales de Forja. El plan los citaba «como Symmetric Strength», pero la referencia real en el mercado español es Symmetry.
- **Lo que sigue siendo de Forja:**
  - **las palancas**, con el error medido frente a cinta;
  - **reglas y estándares abiertos:** percentiles de OpenPowerlifting con fuente pública, series secundarias a 0,5 citado (Pelland 2026) y la traza del solver;
  - **la dieta por precio** en España;
  - **el escáner de suplementos.**
- **Riesgo:** quien ya usa Symmetry no cambia de app por un mapa muscular. El gancho tiene que ser lo que Symmetry no tiene.

## Symmetric Strength (web)
- Calculadora web gratuita, sin app. Puntuación global de fuerza a partir de hasta 14 series, con **Wilks** y ajuste por edad; puntuación de simetría de 0 a 100; percentil frente a OpenPowerlifting; desequilibrios **por patrón de movimiento** (no por músculo) con correctivos. [Web](https://symmetricstrength.com/) · [Metodología](https://symmetricstrength.com/about#methodology)
- Su web actual no muestra un mapa muscular. Interesa por su método publicado (Wilks + OpenPowerlifting), que es comparable con el de Forja.

## Gravl y Fitbod

Pendiente: prueba a mano en Android (onboarding completo y cambio de datos físicos para ver si cambian los ejercicios).
