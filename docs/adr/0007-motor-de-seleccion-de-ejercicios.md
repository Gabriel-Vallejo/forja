# 0007. Qué decide los ejercicios: limitaciones, movilidad y reglas con evidencia; las palancas solo desempatan

- **Estado:** aceptada (2026-11-15)
- **Fecha:** 2026-11-15
- **Sustituye en parte a:** la premisa de [CONTEXT.md](../../CONTEXT.md) §1 («una rutina que elige los ejercicios según tus palancas»), el punto 2 del módulo A («Proporciones por foto») y el término «encaje biomecánico por palancas» del [0002](0002-solver-cp-sat.md).

## Contexto
La investigación de fase 1 (coach, fisio, médico y biomecánico; seis áreas, unas 150 fuentes) llega a estas conclusiones:

- **No hay umbrales validados de proporciones** (fémur/torso, brazo/torso, envergadura) para elegir ejercicios, ni ningún ensayo que asigne ejercicios por antropometría y mida el resultado.
- **En sentadilla, lo que predice la inclinación del tronco es la movilidad de tobillo, no los ratios.** Los dos estudios que miden ambas cosas no encuentran relación con los ratios (Fuglsang 2017; Berglund 2024). La dorsiflexión en el test de rodilla a la pared predice la profundidad (r ≈ 0,69, error mínimo detectable de 1,6-1,9 cm).
- **En peso muerto y banca, las correlaciones con las proporciones son modestas** (|r| ≈ 0,3-0,5). La masa corporal predice la banca mejor que la longitud del brazo.
- **Con una foto 2D, el error es del tamaño de la diferencia entre personas que se quiere medir:** no hay validación de longitudes de segmentos con MediaPipe frente a cinta, y los ángulos tienen unos 11° de error sin corrección.
- **Las inserciones musculares no cambian qué ejercicio es mejor y no se pueden medir con una foto.**
- **Lo que sí tiene buena evidencia vale para cualquier cuerpo:** cargar el músculo en posición estirada (curl femoral sentado frente a tumbado, tríceps por encima de la cabeza, gemelo de pie), y que unilateral y bilateral, o máquina y peso libre, dan la misma hipertrofia.
- **En lesiones, la evidencia favorece modificar carga, rango o variante antes que quitar el ejercicio.** Un dolor leve durante el ejercicio no empeora el resultado (Smith 2017).
- **Un LLM no puede decidir sobre salud:** pasa por alto entre el 26 y el 42 % de las banderas rojas cuando el texto lo escribe un paciente (Gökmen 2026).
- **La frontera legal la marca la finalidad.** Una app de fitness que elige ejercicios compatibles con lo que el usuario declara no es producto sanitario. Si «rehabilita», «alivia el dolor» o «detecta atrofias», se acerca a clase IIa según la MDCG 2019-11 rev.1.

El informe completo con fuentes y nivel de evidencia es interno. Las citas clave se pasarán a [docs/referencias.md](../referencias.md) en la fase 1.

## Decisión
**El motor de selección, de más a menos peso:**

1. **Restricciones de seguridad (duras).** Salen de la entrada guiada y de la IA de entrada (más abajo). Si el usuario dice «evitar» o la restricción viene de un profesional, el ejercicio queda a cero en CP-SAT. Si dice «limitar rango» o «limitar carga», se elige otra variante de la **escalera de sustitución**: misma familia → menos rango → otro implemento → otro patrón.
2. **Movilidad medida con tres tests caseros:**
   - rodilla a la pared (tobillo);
   - rotación de cadera sentado;
   - flexión de hombro tumbado, con foto lateral opcional.

   Cada test da un resultado en tres tramos que decide variantes concretas: talonera o sentadilla goblet frente a barra baja, apertura de pies, press por encima de la cabeza o en banco inclinado.
3. **Reglas universales con evidencia:** carga en posición estirada, volumen y frecuencia ([referencias](../referencias.md)) y variación con criterio.
4. **Respuesta registrada:** dolor de 0 a 10 por ejercicio, RPE y profundidad. Si un ejercicio duele de forma repetida, el solver lo baja por la escalera de sustitución; nunca lo prohíbe sin preguntar.
5. **Prioridades** que elige el usuario o que salen de su rendimiento: percentiles de OpenPowerlifting y estancamientos. **No son asimetrías ni atrofias detectadas.**
6. **Palancas (desempate, peso bajo):**
   - **Medida:** foto o cinta, en **tres tramos** (corto, medio y largo), nunca un ratio exacto. El peso en el objetivo escala con la confianza de la medida.
   - **Excepción:** la anchura de hombros (biacromial) sí da una sugerencia concreta, el agarre de banca (unas 1,5 veces la anchura biacromial).
   - **Fuera:** las inserciones.

**Trazabilidad:** cada regla guarda su fuente y su nivel (A/B/C/D, H = heurística de entrenadores, P = propuesta propia) y la app lo enseña. Una palanca se explica así: «suele ir bien a gente con tus proporciones; pruébalo». Nunca se dice que unas palancas son «malas».

**IA de entrada antes de la rutina** (el LLM transcribe y las reglas deciden):

1. **Cribado con botones** (PAR-Q+ o equivalente con licencia comprobada). Con cualquier «sí» relevante, la app recomienda hablar con un profesional antes de seguir.
2. **Preguntas guiadas:**
   - zona, lado, desde cuándo, si está diagnosticado y si un profesional puso restricciones;
   - dolor actual: dónde, con qué movimientos, de 0 a 10, y si aparece en reposo o de noche;
   - cirugías;
   - límites por patrón de movimiento.
3. **Texto libre al final («cuéntame»):** discapacidades, atrofias, hipertrofias o asimetrías, y cualquier otra cosa.
   - Un LLM local (Ollama, con `format` = JSON Schema y validación Pydantic) lo **extrae** a un esquema cerrado de restricciones: `region`, `lado`, `patron`, `tipo`, `parametro` (por categoría y RPE, nunca en grados ni kilos), `estado`, `origen` y `texto_original`, con `desconocido` permitido en cada campo.
   - Toda restricción lleva su **cita literal**; si la cita no está en lo que escribió el usuario, se descarta.
   - El usuario confirma cada restricción en la pantalla «esto es lo que he entendido; ¿falta algo?». Si algo es ambiguo, se le hace una pregunta cerrada; si sigue sin resolverse, se aplica la opción conservadora.
4. **Banderas rojas por reglas** (palabras clave y respuestas del cribado), nunca por el LLM. El LLM puede añadir avisos, pero no quitarlos. Con una sola bandera roja, la app para y recomienda ir a urgencias hoy o a un profesional en unos días.
5. **Se guarda la limitación funcional** («sin carga axial»), no la patología («hernia L5-S1»). Sigue siendo un dato de salud ([0004](0004-privacidad-fotos-y-datos-de-salud.md)).
6. **Perfiles que la app no adapta sin profesional:** la app no genera la rutina y explica por qué. Son, por ejemplo:
   - lesión medular con riesgo de disreflexia;
   - enfermedad neuromuscular;
   - embarazo con contraindicaciones;
   - cardiopatía con síntomas.

   La lista exacta sale del informe y la revisa un fisio antes de la v0.2.

**Finalidad declarada:** en la app, la ficha de la tienda, el README y los posts, Forja «elige ejercicios compatibles con las limitaciones que tú indicas» y «no evalúa, diagnostica, trata ni previene lesiones». Las palabras «rehabilita», «alivia», «corrige», «previene lesiones», «detecta desequilibrios o atrofias» y «seguro para tu lesión» quedan prohibidas.

## Alternativas
| Opción | A favor | En contra |
|---|---|---|
| Mantener las palancas por foto como motor | Era el gancho del plan | Sin umbrales validados, sin ensayos y con un error de foto del tamaño de la señal: prometería algo que no se puede cumplir |
| Quitar del todo las fotos y las palancas | Más simple, menos datos de salud | Pierde un desempate razonable (agarre de banca, variante preferida) y la parte de visión que justifica el `vision-service` |
| Que un LLM genere la rutina a partir del texto libre | Rápido y flexible | No es auditable y pasa por alto banderas rojas; contradice el [0002](0002-solver-cp-sat.md) |
| Detectar asimetrías y atrofias por foto | Llamativo | No es fiable (ICC < 0,40 en los índices de asimetría) y empuja la finalidad hacia producto sanitario |

## Consecuencias
- **Frase de venta:** de «elige los ejercicios según tus palancas» a «te pregunta lo que te limita, mide tu movilidad y te explica cada ejercicio con su fuente».
- **Diferencial:** frente a Symmetry, Fitbod y Gravl, ninguna explica por qué pone cada ejercicio ni de dónde sale la regla.
- **Plan por fases:**
  - **Fase 1 (v0.1):** cribado con botones, preguntas guiadas, tabla de restricciones, escalera de sustitución, tests de movilidad introducidos a mano y reglas universales.
  - **Fase 2 (v0.2):** la extracción con LLM del texto libre, con su evaluación en `eval/`: 100-200 frases sintéticas en español; restricciones inventadas = 0; la sensibilidad a banderas rojas bloquea el CI si empeora. También la foto en tres tramos y las palancas como desempate.
- **Modelo de datos:**
  - nuevas tablas `restriccion` (los campos de arriba) y `test_movilidad` (tipo, valor, tramo y fecha);
  - `ratio_corporal` pasa a guardar el tramo y la confianza;
  - los ejercicios ganan los atributos espejo: patrones, regiones cargadas, carga axial, impacto, agarre, uni o bilateral, por encima de la cabeza y familia de sustitución.
- **Consentimiento:** se suma el tipo `limitaciones` ([0004](0004-privacidad-fotos-y-datos-de-salud.md)).
- **Riesgos:** la lista de banderas rojas y la de perfiles que se derivan son propuestas propias sin revisión clínica, y deben pasar por un fisio antes de la v0.2. Además, el informe deja 17 huecos sin verificar.

## Enmienda (2026-11-17): el backend recuerda el bloqueo

El PRD pide que, con una bandera roja, la app no genere la rutina. Para que no dependa del cliente, `POST /banderas` y `POST /cribado` guardan **solo el nivel del resultado** (`urgencias_hoy`, `profesional_en_dias` o `parar_urgencias`) y la fecha, cifrados como `restriccion.datos` (`bloqueo_salud`, una fila por usuario y origen); las respuestas siguen sin guardarse. Cada cuestionario levanta solo su bloqueo, y `POST /rutinas/generar` responde 409 `bandera_roja_activa` mientras haya alguno. Revocar el consentimiento `limitaciones` o borrar la cuenta lo borra (RF-46).
