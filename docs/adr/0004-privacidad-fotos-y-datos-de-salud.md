# 0004. Privacidad de las fotos y los datos de salud

- **Estado:** aceptada (2026-11-15)
- **Fecha:** 2026-11-15

## Contexto
Forja trata fotos de cuerpo entero, medidas corporales (cintura, % de grasa estimado), lesiones, peso y suplementos. Lo tratamos todo como **categoría especial del art. 9 del RGPD** (datos de salud), aunque alguno sea dudoso, porque equivocarse por exceso cuesta poco.

La competencia directa (BodyLeveling, ver [competencia](../competencia.md)) manda las fotos a Google Gemini y no cita el art. 9. Hacerlo bien aquí es parte del producto, no solo cumplimiento.

> **Interpretación propia, no asesoría legal.** Antes de abrir la app a usuarios hay que revisarlo (ver «Pendiente» al final).

## Decisión
1. **Las fotos no se guardan.**
   - Las fotos de cuerpo se procesan **en memoria** en el `vision-service` (MediaPipe en CPU, síncrono; ver [0003](0003-cola-de-trabajos-gpu.md)) y se descartan al responder.
   - No se escriben en disco, ni en la base de datos, ni en la cola, ni en object storage, ni en los logs.
   - Solo quedan los ratios en `ratio_corporal`, con su origen (`foto` o `manual`).
   - Lo mismo con las fotos de etiquetas: solo queda el texto del OCR.
2. **Nada sale a terceros.** Todos los modelos son locales (MediaPipe, Florence-2 y Ollama). No hay APIs de LLM externas ni analítica de terceros. Las métricas de uso van a `evento_uso`, propia y anónima.
3. **Consentimiento explícito** (art. 9.2.a):
   - Va en una pantalla aparte, antes de la primera foto o medida, con texto claro de qué se guarda y qué no.
   - Se guarda en `consentimiento`: usuario, tipo (`fotos_cuerpo`, `medidas_salud`), versión del texto, fecha y fecha de revocación.
   - Revocar es tan fácil como dar el consentimiento (art. 7.3) y borra los datos que dependían de él.
   - Sin consentimiento la app sigue sirviendo: rutina sin ajuste por palancas y dieta sin % de grasa.
4. **Minimización** (art. 5.1.c y 25): se piden las medidas que usa un algoritmo y ninguna más. La edad se guarda como año de nacimiento, no como fecha completa.
5. **Cifrado en reposo** de las columnas sensibles:
   - medidas, ratios, lesiones y peso, con AES-GCM a nivel de aplicación (un `AttributeConverter` de JPA);
   - la clave va en el entorno (`.env`, nombre en `.env.example` sin valor) y nunca en el repo;
   - además, TLS en tránsito con Caddy y un disco cifrado en el VPS si el proveedor lo permite.
6. **Borrado real** (art. 17):
   - borrar la cuenta borra en cascada (`ON DELETE CASCADE` pensado tabla a tabla);
   - las copias de seguridad caducan a los 30 días **[a fijar en Producción]**, y eso se dice en la política;
   - hay un test de integración que crea un usuario, lo borra y comprueba que no queda ninguna fila suya.
7. **Logs:** nunca cuerpos de petición en las rutas de fotos, medidas ni suplementos. Los logs JSON llevan `traceId` y el ID de usuario con hash, no el ID en claro.
8. **Edad mínima de 18 años** para usar las funciones de cuerpo y salud.
9. **No es biometría identificativa:** los puntos del cuerpo no se usan para identificar a nadie (art. 4.14), así que no se tratan como plantilla biométrica. Si algún día se compararan entre personas, habría que revisarlo.

## Alternativas
| Opción | A favor | En contra |
|---|---|---|
| **Pose en el móvil** (MediaPipe en el dispositivo; la foto nunca sale) | Máxima privacidad: el servidor no ve nunca la imagen | Módulo nativo en Expo con dev build; dos implementaciones que evaluar (móvil y servidor); más trabajo en la fase 2. **Candidata para después de la v0.2**, cuando el pipeline del servidor esté evaluado. |
| Guardar las fotos cifradas para el «antes y después» | Función atractiva | Contradice la regla del proyecto; el antes y después se hace con las fotos que el usuario ya tiene en su móvil, como BodyLeveling |
| LLM multimodal externo (Gemini, GPT) para el escaneo | Rápido de montar | Datos de salud a un tercero, sin método medible; justo lo que criticamos de la competencia |
| Sin cifrado a nivel de aplicación (solo disco) | Más simple | Una copia de seguridad o un volcado filtrado expondría las medidas en claro |

## Consecuencias
- **Gana:** se puede decir en el README y en la app, con verdad, «tus fotos no se guardan ni salen de nuestro servidor». Es un argumento de venta frente a la competencia y un buen tema de entrevista (privacidad desde el diseño).
- **Cuesta:**
  - el cifrado por columna impide buscar o indexar esas columnas (no hace falta);
  - la gestión de la clave y su rotación hay que documentarla en la guía de despliegue;
  - el texto de consentimiento hay que versionarlo.
- **Pendiente antes de abrir la app a usuarios:**
  1. Política de privacidad propia.
  2. Una evaluación de impacto (EIPD, art. 35) ligera aunque la escala sea pequeña.
  3. Revisar si hace falta registro de actividades de tratamiento.
