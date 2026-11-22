# Revisión del fisioterapeuta

Forja es una app de fitness, no una herramienta sanitaria: elige ejercicios compatibles con las limitaciones que indica cada persona y, si detecta una señal de alarma, no genera la rutina y dice a quién acudir. Estas reglas las ha escrito el equipo a partir de guías clínicas. Antes de que lleguen a más usuarios, necesitamos que alguien con titulación las revise.

**Cómo devolverlo:** en cada fila, marca ✅ (de acuerdo), ✏️ (cámbialo así: …) o ❌ (quítalo) y añade un comentario si hace falta. Tiempo estimado: 15-20 minutos.

## 1. Cambio del 22-11-2026: debilidad y hormigueo «nuevos o que van a más»

Antes, la app preguntaba «¿Has notado que pierdes fuerza en un brazo o una pierna?» y «¿Notas hormigueo o la zona dormida?». Una persona con un déficit crónico ya valorado (por ejemplo, una hipotonía distal conocida o una neuropatía diagnosticada) contestaba «sí», y la app la mandaba al médico antes de entrenar, cada vez.

Ahora pregunta solo por lo nuevo o lo que empeora:

| Pregunta | Si contesta «sí» | ¿De acuerdo? |
|---|---|---|
| ¿Has notado una pérdida de fuerza **nueva, o que va a más**, en un brazo o una pierna? | Profesional sanitario en los próximos días; no se genera la rutina | |
| ¿Notas hormigueo o una zona dormida, **de forma nueva o que va a más**? | Profesional sanitario en los próximos días; no se genera la rutina | |
| Las dos a la vez | Urgencias hoy (se trata como déficit neurológico) | |

**En qué se apoya:**
- NICE NG59, recomendación 1.1.1: pensar en otros diagnósticos, sobre todo si aparecen síntomas nuevos o que cambian ([enlace](https://www.nice.org.uk/guidance/ng59/chapter/Recommendations)).
- NICE NG234, recomendación 1.3.1, en personas con cáncer: la debilidad de un miembro y el entumecimiento son signos de posible compresión medular, **sin** el matiz de «nuevo» ([enlace](https://www.nice.org.uk/guidance/ng234/chapter/Recommendations)).
- No hemos podido leer el texto completo del marco IFOMPT (Finucane y cols., JOSPT 2020, doi:10.2519/jospt.2020.9971), que es la fuente principal de estas banderas. No sabemos si distingue el déficit nuevo del crónico.

**Preguntas concretas:**
1. ¿Te parece bien que un déficit crónico estable y ya valorado no bloquee la rutina?
2. ¿Hace falta preguntar además «¿lo ha valorado un profesional?» o basta con «nuevo o que va a más»?
3. ¿Debilidad + hormigueo nuevos → urgencias hoy es correcto, o debería ser «profesional en días» salvo que sea en las dos piernas o vaya rápido?
4. La anestesia en silla de montar y la pérdida de control de esfínteres siguen siendo urgencias siempre, aunque sean antiguas. ¿De acuerdo?

## 2. El resto del cuestionario de dolor

Se hace al indicar dolor. Basta una señal para no generar la rutina.

| Pregunta | Si contesta «sí» | Fuente que usamos | ¿De acuerdo? |
|---|---|---|---|
| ¿Has perdido el control de la vejiga o el intestino, o notas dormida la zona genital o anal? | Urgencias hoy | IFOMPT 2020; Dionne y cols. 2019 (síndrome de cola de caballo) | |
| ¿Notas dolor u opresión en el pecho? | Urgencias hoy | ACSM, cribado previo (Riebe y cols. 2015) | |
| ¿Te has mareado o desmayado? | Urgencias hoy | ACSM 2015 | |
| ¿Tienes fiebre y, a la vez, dolor de espalda? | Profesional en días | IFOMPT 2020; NICE NG59 | |
| ¿Has tenido un golpe o una caída fuerte hace poco? (solo cuenta con dolor) | Profesional en días | IFOMPT 2020; NICE NG59 | |
| ¿Has perdido peso sin explicación? | Profesional en días | IFOMPT 2020 | |
| ¿Te despierta el dolor por la noche? | Profesional en días | NICE NG59 | |
| ¿Te duele aunque estés en reposo? (solo cuenta con dolor) | Profesional en días | NICE NG59 | |

**Duda abierta:** hoy cualquier mareo manda a urgencias. ¿Separamos «me he mareado» (profesional en días) de «me he desmayado» (urgencias)?

## 3. Cribado previo al ejercicio (7 preguntas)

Redactado por nosotros a partir de las categorías del algoritmo del ACSM (Riebe y cols. 2015). No usamos el texto del PAR-Q+ porque no hemos comprobado su licencia.

| Pregunta | Si contesta «sí» | ¿De acuerdo? |
|---|---|---|
| ¿Has notado dolor, opresión o molestia en el pecho, el cuello, la mandíbula o el brazo al hacer esfuerzo? | Profesional antes de entrenar; urgencias si ha sido hace poco | |
| ¿Te has desmayado, o te has mareado con fuerza, al hacer ejercicio? | Profesional antes de entrenar; urgencias si ha sido hace poco | |
| ¿Tienes diagnosticada una enfermedad del corazón, del metabolismo (como la diabetes) o del riñón? | Profesional antes de entrenar | |
| ¿Tomas alguna medicación que condicione cómo respondes al ejercicio? | Profesional antes de entrenar | |
| ¿Tienes ahora un problema de hueso, articulación o tejido blando que empeora cuando te mueves o entrenas? | Profesional antes de entrenar | |
| ¿Un profesional sanitario te ha indicado que solo hagas actividad física con supervisión? | Seguir esa indicación y consultar antes de usar Forja | |
| ¿Estás embarazada? | Profesional antes de entrenar | |

## 4. Dolor durante el entreno

| Regla | Valor | ¿De acuerdo? |
|---|---|---|
| Aviso en la serie si el dolor anotado es alto | Desde 7 sobre 10 | |
| «Este ejercicio te ha dolido de forma repetida»: la app lo dice y propone cambiarlo (nunca lo quita sola) | En 2 de sus 3 últimas sesiones, alguna serie por encima de 5 sobre 10 | |

El umbral de 5 sobre 10 sale del modelo de monitorización del dolor (Silbernagel y cols., Am J Sports Med 2007). Ese número lo citan resúmenes secundarios, pero no aparece en el abstract: no lo hemos comprobado en el texto completo.

## 5. Tests de movilidad

Los tramos son una propuesta nuestra; solo cambian qué variante de un ejercicio se propone.

| Test | Tramos | ¿De acuerdo? |
|---|---|---|
| Tobillo (rodilla a la pared, flexión dorsal) | Limitado < 8 cm · medio 8-12 cm · bueno > 12 cm | |
| Cadera (rotación, sentado al borde de una mesa con los pies colgando) | Limitado < 30° · medio 30-40° · bueno ≥ 40° | |
| Hombro (tumbado, brazos por encima de la cabeza sin despegar la zona lumbar) | Limitado: a más de un palmo del suelo o arqueando la espalda · medio: a menos de un palmo · bueno: tocan el suelo con los codos estirados | |

**Hueco conocido:** el test de tobillo solo mide la flexión dorsal. Una limitación hacia flexión plantar no la recoge. ¿Merece un test propio o basta con que la persona indique «Tobillo · limitar rango»?

## 6. Comentarios libres

¿Algo que la app no pregunte y debería, o que pregunte y sobre?
