# Forja: documento de requisitos de producto

- **Fecha:** 2026-11-16 · **Estado:** borrador
- **Fuentes, por prioridad:** [ADR 0007](adr/0007-motor-de-seleccion-de-ejercicios.md) → resto de [ADR](adr/README.md) → [CONTEXT.md](../CONTEXT.md) → [CLAUDE.md](../CLAUDE.md) → [competencia](competencia.md) y [referencias](referencias.md). Si dos fuentes chocan, manda la de más arriba.
- Lo que no sale de esas fuentes va marcado **[propuesta]**. Donde falta una cifra de rendimiento: **[medir en la fase 1]**.

## 1. Qué es y para quién

### Problema
- Las apps de rutinas te ponen ejercicios sin decirte por qué. Fitbod resume el plan en tres frases genéricas, Gravl «explica» con un dato comercial (+19 % en 8 semanas) y Symmetry, Hevy y Gravl no publican cómo deciden ([competencia](competencia.md)).
- Las apps de escaneo dicen que eligen por tus proporciones, pero no publican método ni error, y BodyLeveling manda las fotos a Google Gemini sin citar el art. 9 del RGPD.
- La ciencia no respalda elegir ejercicios por proporciones: no hay umbrales validados y el error de una foto 2D es del tamaño de la diferencia entre personas ([ADR 0007](adr/0007-motor-de-seleccion-de-ejercicios.md)). Lo que sí decide cosas es lo que te limita y tu movilidad.
- En España nadie calcula la proteína por euro del súper, y NØDopApp solo mira medicamentos: los complementos se quedan fuera ([fase 0](fase-0.md)).

### Frase de venta
> Te pregunta lo que te limita, mide tu movilidad y arma la rutina con un solver que te explica cada ejercicio con su fuente. Además, la cesta de proteína más barata del súper y un escáner de riesgo de tus suplementos.

### Usuarios objetivo
| Perfil | Qué necesita | Qué usa de Forja |
|---|---|---|
| **Entrena fuerza e hipertrofia en un gimnasio comercial**, con Android | Una rutina que encaje con su material y sus días, y apuntar series donde no hay cobertura | Rutina con CP-SAT, registro sin conexión, progresión doble, estándares y mapa muscular |
| **Entrena con una lesión o una limitación** (dolor, cirugía, discapacidad, asimetría) | Ejercicios compatibles con lo que declara, sin que una app le prometa arreglar nada | Cribado, preguntas guiadas, «cuéntame», escalera de sustitución, dolor 0-10 por ejercicio |
| **Vigila lo que gasta en proteína** y come bajo en carbohidratos; toma suplementos | La cesta que llega a su proteína al menor coste y saber qué riesgo lleva su pre-entreno | Optimizador de la cesta, precios de Mercadona y de usuarios, escáner de etiquetas |

Edad mínima: 18 años para las funciones de cuerpo y salud ([ADR 0004](adr/0004-privacidad-fotos-y-datos-de-salud.md)).

### Qué no es
- **No es un producto sanitario.** Finalidad declarada, igual en la app, la tienda, el README y los posts: Forja «elige ejercicios compatibles con las limitaciones que tú indicas» y «no evalúa, diagnostica, trata ni previene lesiones» ([ADR 0007](adr/0007-motor-de-seleccion-de-ejercicios.md)).
- No es un certificador de suplementos: da riesgo por ingredientes declarados, nunca «libre de dopaje» ni «apto».
- No es un LLM que escribe rutinas: el LLM transcribe y explica; las reglas y el solver deciden.
- No es una red social ni pone medidas corporales tras un muro de pago.

## 2. Necesidades del usuario

| # | Necesidad | Lo que hace mal la competencia | Respuesta de Forja |
|---|---|---|---|
| N-01 | Saber por qué tiene cada ejercicio | Ninguna de las cinco probadas lo explica por ejercicio | Traza del solver con regla, fuente y nivel de evidencia en cada tarjeta |
| N-02 | Entrenar con una limitación sin que se la ignoren ni se la «traten» | Gravl ofrece una «rutina de recuperación» y Fitbod dice usar lesiones, sin enseñar cómo | Restricciones confirmadas por el usuario, banderas rojas por reglas y escalera de sustitución |
| N-03 | Saber dónde está flojo y qué hacer | Symmetry dice dónde estás flojo pero no qué hacer; Symmetric Strength reparte el total igual para todo el mundo | Percentiles con fuente pública, mapa enlazado al ejercicio que lo trabaja |
| N-04 | Apuntar series sin cobertura | No se ha podido ver el registro en curso de ninguna ([competencia](competencia.md)) | Outbox en el móvil y regla de la siguiente sesión sin red ([ADR 0008](adr/0008-sincronizacion-sin-conexion.md)) |
| N-05 | Que sus fotos y medidas no salgan de la app | BodyLeveling pasa las fotos por AWS S3 y Gemini; Hevy y Symmetry cobran por las medidas | Fotos en memoria, cifrado por columna, consentimiento del art. 9, medidas gratis |
| N-06 | Empezar rápido | Gravl: unas 25 pantallas con marketing; Fitbod: unas 60 casillas de equipo | «Para qué lo usamos» bajo cada dato y nada de marketing entre preguntas |
| N-07 | Llegar a su proteína gastando poco | Proteína por euro probada en EE. UU. y vacía en España ([CONTEXT](../CONTEXT.md) §1.1) | Programación lineal con precios reales |
| N-08 | Saber si un complemento tiene riesgo de dopaje | NØDopApp excluye complementos; Global DRO no cubre España ni complementos | Cruce con la lista del BOE, sinónimos y alertas de CELAD y AESAN |
| N-09 | Usarla en Android y en PC, gratis | BodyLeveling, TrueForm y Thelo son de iPhone y de pago | APK y versión de PC en GitHub Releases ([ADR 0006](adr/0006-version-de-pc.md)) |

## 3. Funciones por versión

Horas de [CONTEXT](../CONTEXT.md) §1.8 **[estimación]**. Cada versión es publicable por sí sola.

### v0.1 · Entreno MVP (fase 1, 60 h)
| Función | Criterio de aceptación |
|---|---|
| Cuenta, consentimiento, cifrado y borrado | Un test crea un usuario con datos de salud, lo borra y no queda ninguna fila suya |
| Catálogo de ejercicios etiquetado | Cada ejercicio de free-exercise-db cargado tiene patrones, regiones cargadas, carga axial, impacto, agarre, uni o bilateral, por encima de la cabeza y familia de sustitución |
| Perfil manual | Guarda sexo, año de nacimiento, altura, peso, experiencia, días, material, objetivo y circunferencias; el % de grasa sale con la fórmula de la US Navy |
| Cribado con botones y preguntas guiadas | Cualquier «sí» relevante muestra la recomendación de hablar con un profesional antes de seguir |
| Banderas rojas por reglas | Con una bandera roja, la app no genera la rutina y recomienda urgencias hoy o un profesional en unos días |
| Tests de movilidad a mano | Los tres tests dan un tramo y el tramo cambia al menos una variante del plan (p. ej., talonera o goblet) |
| Rutina con CP-SAT | Tests de propiedades (generadores con semilla fija) en verde: nunca supera la duración ni el máximo de series por músculo |
| «Por qué este ejercicio» | Cada tarjeta muestra la regla, su fuente y su nivel (A-D, H o P); en v0.1 sale de la traza con plantillas, sin LLM **[propuesta]** |
| Ajustes del plan (más, menos, no recomendar, reemplazar, variedad, excluir 4 y enfocar 2) | Cada ajuste vuelve a resolver y el cambio se ve en la vista previa |
| Registro sin conexión | En modo avión se registran sesiones, se recupera la red y el servidor queda igual que el móvil |
| Progresión doble y deload | Hay un test por cada transición de la máquina de estados |

### v0.2 · Cuerpo y estándares (fase 2, 40 h)
| Función | Criterio de aceptación |
|---|---|
| LLM de entrada («cuéntame») | En `eval/`, 100-200 frases sintéticas: 0 restricciones inventadas y el CI se bloquea si baja la sensibilidad a banderas rojas |
| Pantalla «esto es lo que he entendido» | Ninguna restricción entra al solver sin confirmar; una cita que no está en el texto se descarta |
| Fotos en tres tramos (MediaPipe) | El error frente a cinta en 15-20 personas está publicado en `eval/` y en el README |
| Palancas como desempate | Con la misma entrada y sin fotos, el plan solo cambia en desempates; el agarre de banca sugerido es unas 1,5 veces la anchura biacromial |
| Percentiles de OpenPowerlifting y mapa muscular | El mapa enseña débil, en la media, fuerte y sin datos, con el aviso del sesgo hacia arriba |
| Calculadora con una sola serie y método visible | Con una serie se obtiene percentil; la página de método tiene un ejemplo trabajado |
| LLM explicador | La explicación cita solo elementos de la traza; el LLM no cambia series ni ejercicios |
| Cola de trabajos y worker de GPU | Dos workers que compiten (Testcontainers) nunca procesan el mismo trabajo |

### v0.3 · Dieta (fase 3, 40 h)
| Función | Criterio de aceptación |
|---|---|
| Objetivos de energía y macros | Mifflin-St Jeor por factor de actividad, recalculado cuando la tendencia del peso semanal no cuadra |
| Catálogo de alimentos y precios | Cada precio guarda fuente y fecha; con mercaapi apagado, la cesta sigue saliendo con precios de usuarios |
| Optimizador de la cesta | La cesta cumple proteína ≥ objetivo, carbohidratos ≤ tope, kcal en rango y máximo de raciones por producto |
| Precios por foto del lineal | La foto se procesa en memoria y solo queda el precio |
| Avisos de bajada de precio | Un job detecta la bajada de un producto de tu cesta y avisa |
| Recetas | Solo usan productos de la cesta; las macros las recalcula el backend |

### v0.4 · Suplementos (fase 4, 35 h)
| Función | Criterio de aceptación |
|---|---|
| OCR y normalización | CER y precisión y recall de ingredientes publicados en `eval/` |
| Cruce con la lista del BOE y sinónimos | El conjunto de falsos negativos conocidos de `eval/` (DMAA y sus nombres) sale con riesgo alto |
| Alertas de CELAD y AESAN | Cada alerta guarda su PDF de origen |
| Ficha de riesgo | Dice «riesgo por ingredientes declarados» y si tiene sello Informed Sport o NSF; nunca «libre de dopaje» ni «apto» |
| Stack y push | Una alerta nueva sobre algo del stack manda un push con texto genérico, sin nombrar el producto |

### v1.0 · Producción (fase 5, 20 h + 8-10 h de la versión de PC)
| Función | Criterio de aceptación |
|---|---|
| Despliegue | Azure (Container Apps y Azure Database for MySQL) con IaC en Bicep o Terraform, y VPS con Caddy; ZAP baseline sin hallazgos altos **[propuesta: umbral]** |
| Observabilidad | Dashboards de latencia, errores, longitud de la cola y latencia del modelo |
| Releases | Una etiqueta `v*` publica APK, `.exe` (NSIS), `.AppImage` y `.deb` |
| Usuarios reales | 20 usuarios del gimnasio y la app en Google Play en prueba cerrada |
| Legal | Política de privacidad, EIPD ligera y registro de actividades revisados antes de abrir; mercaapi apagado por defecto |

## 4. Requisitos funcionales

### Entrada de limitaciones y cribado
| ID | Requisito | Versión | Respaldo |
|---|---|---|---|
| RF-01 | Cribado con botones tipo PAR-Q+ (o equivalente con licencia comprobada) | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-02 | Preguntas guiadas: zona, lado, desde cuándo, diagnóstico, restricciones de un profesional, dolor 0-10, reposo o noche, cirugías y límites por patrón | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-03 | Banderas rojas por palabras clave y respuestas del cribado, nunca por el LLM; el LLM puede añadir avisos, no quitarlos | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-04 | Perfiles que no se adaptan sin profesional (lesión medular con riesgo de disreflexia, enfermedad neuromuscular, embarazo con contraindicaciones, cardiopatía con síntomas): no se genera rutina y se explica por qué | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-05 | «Cuéntame» en texto libre: el LLM local extrae a un esquema cerrado (`region`, `lado`, `patron`, `tipo`, `parametro`, `estado`, `origen`, `texto_original`, con `desconocido`) | v0.2 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-06 | Cada restricción lleva cita literal y la confirma el usuario; si algo sigue ambiguo tras una pregunta cerrada, se aplica la opción conservadora | v0.2 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-07 | Se guarda la limitación funcional («sin carga axial»), no la patología | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |

### Movilidad y medidas
| ID | Requisito | Versión | Respaldo |
|---|---|---|---|
| RF-08 | Tres tests caseros (rodilla a la pared, rotación de cadera sentado, flexión de hombro tumbado), cada uno en tres tramos | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-09 | El tramo decide variantes concretas: talonera o goblet frente a barra baja, apertura de pies, press por encima de la cabeza o en banco inclinado | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-10 | Proporciones por dos fotos o por cinta, en tres tramos con confianza; nunca un ratio exacto ni «palancas malas» | v0.2 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md), [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md) |
| RF-11 | En PC, las fotos se suben como archivo; en el móvil, cámara guiada | v0.2 | [0006](adr/0006-version-de-pc.md) |

### Rutina con CP-SAT y su «por qué»
| ID | Requisito | Versión | Respaldo |
|---|---|---|---|
| RF-12 | Filtro previo por material, restricciones «evitar» o de un profesional y exclusiones del usuario, con el motivo de cada descarte | v0.1 | [0002](adr/0002-solver-cp-sat.md), [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-13 | «Limitar rango» o «limitar carga» bajan por la escalera: misma familia → menos rango → otro implemento → otro patrón | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-14 | Duras: duración por sesión, máximo de ejercicios y de series (6) por patrón y día, sin básico pesado en días seguidos, máximo de series por músculo | v0.1 | [0002](adr/0002-solver-cp-sat.md) |
| RF-15 | Mínimo de volumen y frecuencia ≥ 2 como metas con holgura; si se usa holgura, la app lo dice («con 3 días y 45 min no llegas a 10 series de espalda; te quedan 8»); el usuario puede bajar la frecuencia a 1 | v0.1 | [0002](adr/0002-solver-cp-sat.md) |
| RF-16 | Series de los músculos secundarios a 0,5 (Pelland 2026) | v0.1 | [0002](adr/0002-solver-cp-sat.md), [referencias](referencias.md) |
| RF-17 | Objetivo ponderado (pesos en configuración): prioridades, movilidad, palancas con peso bajo, preferencias, fatiga y holgura | v0.1 | [0002](adr/0002-solver-cp-sat.md) |
| RF-18 | Traza por ejercicio, con `OPTIMAL` o `FEASIBLE` («el mejor plan» o «un buen plan») y la diferencia frente a las 2-3 alternativas más cercanas | v0.1 | [0002](adr/0002-solver-cp-sat.md) |
| RF-19 | Cada regla enseña fuente y nivel (A, B, C, D, H o P); la frecuencia ≥ 2 y las palancas, como heurística | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md), [referencias](referencias.md) |
| RF-20 | Ajustes en lenguaje natural («me duele el hombro en el press») pasan por el mismo flujo de cita y confirmación que RF-06 antes de volver a resolver | v0.2 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |

### Registro sin conexión y progresión
| ID | Requisito | Versión | Respaldo |
|---|---|---|---|
| RF-21 | Series con peso, repes, RIR, temporizador de descanso, dolor 0-10 y RPE; el calentamiento no cuenta para el volumen. La profundidad se sacó de la v0.1 el 2026-11-21: nada la usaba y estorbaba en una fila que se usa con una mano | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md), [competencia](competencia.md) |
| RF-22 | UUID v7 en el cliente y `PUT /api/v1/sesiones/{uuid}` idempotente | v0.1 | [0008](adr/0008-sincronizacion-sin-conexion.md) |
| RF-23 | Las series solo se añaden: corrección con `corrige_a`, anulación con `anulada` | v0.1 | [0008](adr/0008-sincronizacion-sin-conexion.md) |
| RF-24 | Outbox en `expo-sqlite`; una sesión `rechazada` se enseña y nunca se borra sola | v0.1 | [0008](adr/0008-sincronizacion-sin-conexion.md) |
| RF-25 | El plan lleva la regla de la siguiente sesión y la app la aplica sin red; al sincronizar manda el servidor | v0.1 | [0008](adr/0008-sincronizacion-sin-conexion.md) |
| RF-26 | Progresión doble y deload cuando el e1RM se estanca 2-3 semanas | v0.1 | [CONTEXT](../CONTEXT.md) §1.5 |
| RF-27 | Un ejercicio que duele de forma repetida baja por la escalera; nunca se prohíbe sin preguntar | v0.1 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| RF-28 | Plan versionado: cada sesión guarda su `plan_version` | v0.1 | [0008](adr/0008-sincronizacion-sin-conexion.md) |
| RF-51 | Racha semanal: una semana cuenta si entrenas tus días (los de tus ajustes); descansar no la rompe y la semana en curso no la corta hasta que acaba. Con mejor racha, días entrenados, % de días cumplidos y mapa de 12 semanas, en el día de la zona horaria del móvil | v0.1 | `GET /api/v1/progreso/resumen` |

### Estándares y mapa
| ID | Requisito | Versión | Respaldo |
|---|---|---|---|
| RF-29 | Marcas a mano o e1RM del registro (Epley o Brzycki) | v0.2 | [CONTEXT](../CONTEXT.md) §1.2.A |
| RF-30 | Percentiles de sentadilla, banca y peso muerto por sexo y peso, desde el CSV de OpenPowerlifting, con el sesgo dicho | v0.2 | [0005](adr/0005-fuentes-de-datos.md) |
| RF-31 | Accesorios con ratios frente a los básicos, marcados como heurística propia | v0.2 | [CONTEXT](../CONTEXT.md) §1.2.A |
| RF-32 | Las prioridades salen del usuario o de su rendimiento; la app no las presenta como asimetrías ni atrofias | v0.2 | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |

### Dieta
| ID | Requisito | Versión | Respaldo |
|---|---|---|---|
| RF-33 | Proteína por defecto 1,6 g/kg, ajustable hasta 2,2, con la fuente (Morton 2018) | v0.3 | [referencias](referencias.md#proteína) |
| RF-34 | Programación lineal con OR-Tools (GLOP, o CP-SAT si hay raciones enteras), productos excluidos incluidos | v0.3 | [0002](adr/0002-solver-cp-sat.md) |
| RF-35 | Precios y alimentos detrás de `FuentePrecios` y `FuenteNutricion`; mercaapi solo para productos candidatos y con flag | v0.3 | [0005](adr/0005-fuentes-de-datos.md) |
| RF-36 | Datos de Open Food Facts y de OCR propio en filas separadas, cada uno con su `fuente` | v0.3 | [0005](adr/0005-fuentes-de-datos.md) |

### Suplementos
| ID | Requisito | Versión | Respaldo |
|---|---|---|---|
| RF-37 | OCR síncrono en memoria; solo el texto va a la cola para normalizarlo (`202` + trabajo) | v0.4 | [0003](adr/0003-cola-de-trabajos-gpu.md) |
| RF-38 | Lista del BOE versionada por año, con fuente de sinónimos de licencia comprobada | v0.4 | [0005](adr/0005-fuentes-de-datos.md) |
| RF-39 | Reglas puntuadas: sustancia exacta, sinónimo o alerta = alto; sin certificado = medio; coincidencia dudosa a revisión | v0.4 | [CONTEXT](../CONTEXT.md) §1.5 |
| RF-40 | La ficha avisa de que las clases abiertas («y otras sustancias de estructura similar») no se detectan por nombre | v0.4 | [0005](adr/0005-fuentes-de-datos.md) |

### Cuenta y dispositivos
| ID | Requisito | Versión | Respaldo |
|---|---|---|---|
| RF-41 | Alta con correo y contraseña (Argon2id, 12 caracteres mínimo, lista local de filtradas) y verificación con enlace de 24 h | v0.1 | [0009](adr/0009-autenticacion.md) |
| RF-42 | JWT Ed25519 de 15 min en cabecera; refresco opaco de 30 días, rotatorio y uno por dispositivo; reutilizar uno rotado revoca la familia | v0.1 | [0009](adr/0009-autenticacion.md) |
| RF-43 | Lista de dispositivos en ajustes y cierre de sesión remoto | v0.1 | [0009](adr/0009-autenticacion.md) |
| RF-44 | Recuperación con enlace de 30 min, misma respuesta exista o no la cuenta; cambiar la contraseña revoca todos los dispositivos | v0.1 | [0009](adr/0009-autenticacion.md) |

### Privacidad
| ID | Requisito | Versión | Respaldo |
|---|---|---|---|
| RF-45 | Consentimiento por tipo (`fotos_cuerpo`, `medidas_salud`, `lesiones`, `limitaciones`, `suplementos`), en pantalla aparte, versionado y revocable con la misma facilidad | v0.1 | [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md) |
| RF-46 | Revocar un consentimiento borra los datos que dependían de él; sin consentimiento la app sigue funcionando | v0.1 | [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md) |
| RF-47 | `DELETE /api/v1/cuenta` con la contraseña: revoca todo y borra en cascada; restaurar una copia no resucita cuentas | v0.1 | [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md), [0009](adr/0009-autenticacion.md) |
| RF-48 | Pantalla «Datos y licencias» con © Open Food Facts contributors, la frase de OpenPowerlifting, autores de wger y el identificador del BOE | v0.2 **[propuesta: versión]** | [0005](adr/0005-fuentes-de-datos.md) |
| RF-49 | El formulario de feedback avisa de que va a GitHub | v1.0 | [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md) |
| RF-50 | «Descargar mis datos» en Privacidad: un JSON con todo lo que Forja guarda del usuario, datos de salud incluidos (acceso y portabilidad, arts. 15 y 20 del RGPD) | v0.1 | [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md) |

## 5. Requisitos no funcionales

| ID | Requisito | Respaldo |
|---|---|---|
| RNF-01 | **Fotos en memoria:** cuerpo `application/octet-stream` en los dos saltos, tamaño máximo validado antes de leer, `/tmp` en `tmpfs`, y un test falla si aparece un fichero temporal. Nunca en disco, base, cola ni logs | [0003](adr/0003-cola-de-trabajos-gpu.md), [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md) |
| RNF-02 | **Cifrado:** AES-GCM por columna (medidas, ratios, lesiones, peso, `stack_usuario`, `trabajo.carga` y `trabajo.resultado`), con versión, id de clave, nonce de 12 bytes y datos asociados; rotación sin parada; TLS con Caddy | [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md) |
| RNF-03 | **Art. 9 del RGPD:** todo dato de cuerpo, lesión o suplemento se trata como de salud; minimización (año de nacimiento, no fecha); encargados en la UE listados en la política | [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md) |
| RNF-04 | **Seguridad:** Bean Validation en cada DTO, `ProblemDetail` (RFC 9457), Bucket4j (espera creciente tras 10 fallos), CORS cerrado, CSP de Tauri, propiedad comprobada en `GET /trabajos/{id}`, token de worker solo para `/worker/**`; CodeQL, Dependabot, gitleaks, Trivy y ZAP en el CI; secretos solo en `.env` | [0009](adr/0009-autenticacion.md), [0003](adr/0003-cola-de-trabajos-gpu.md), [CONTEXT](../CONTEXT.md) §0.9 |
| RNF-05 | **Rendimiento:** CP-SAT con `max_deterministic_time` (5 s de partida) **[medir en la fase 1]**; p95 de los endpoints **[medir en la fase 1]**; p95 del OCR en CPU y RAM del VPS **[medir en la fase 1]**; tamaño del instalador de PC **[medir con el primer build]** | [0002](adr/0002-solver-cp-sat.md), [0003](adr/0003-cola-de-trabajos-gpu.md), [0006](adr/0006-version-de-pc.md) |
| RNF-06 | **Sin conexión y degradación:** el móvil entrena y registra sin red; un 401 a mitad de envío no pierde nada; si cae el `vision-service`, entrada manual (Resilience4j); sin latido del worker de GPU en 60 s, el trabajo va al de CPU | [0008](adr/0008-sincronizacion-sin-conexion.md), [0003](adr/0003-cola-de-trabajos-gpu.md) |
| RNF-07 | **Accesibilidad:** etiquetas, contraste AA y tamaños dinámicos; los tokens de Crimson Ink en `theme.ts` pasan AA **[propuesta: comprobarlo en el CI]** | [CONTEXT](../CONTEXT.md) §0.4, [CLAUDE.md](../CLAUDE.md) §5 |
| RNF-08 | **Plataformas:** Android (APK por EAS) y PC en Windows 11 y Linux desde el mismo `frontend/` (Expo web + Tauri 2); el CI ejecuta `npx expo export -p web`; se prueba en Windows y Linux antes de cada release | [0006](adr/0006-version-de-pc.md) |
| RNF-09 | **Idioma y voz:** todo en español, de tú, sin mezclar idiomas; ninguna pantalla, ficha de tienda, README ni post usa «rehabilita», «alivia», «corrige», «previene lesiones», «detecta desequilibrios o atrofias» ni «seguro para tu lesión» **[propuesta: comprobarlo con un grep en el CI]** | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md), [CLAUDE.md](../CLAUDE.md) §4-5 |
| RNF-10 | **Evaluación de IA:** cada modelo tiene dataset con `DATASET.md`, script y métricas versionadas en `eval/`; `eval.yml` falla si la métrica cae; `models.yaml` con `revision` y licencia, y el CI falla si falta la licencia; `modelo_usado` guardado en cada trabajo | [CONTEXT](../CONTEXT.md) §0.5-0.6, [0003](adr/0003-cola-de-trabajos-gpu.md) |
| RNF-11 | **Tests:** ≥ 80 % de cobertura (JaCoCo en el dominio), propiedades con generadores de semilla fija, Testcontainers con MySQL real, contrato contra el OpenAPI, Jest + RNTL, Maestro en 2-3 flujos y pytest | [CLAUDE.md](../CLAUDE.md) §4, [CONTEXT](../CONTEXT.md) §0.2-0.5 |
| RNF-12 | **Procedencia:** cada fila de un catálogo guarda `fuente`, `id_externo`, versión o fecha y, si hace falta, licencia y autor; instantáneas con URL, hash y fecha | [0005](adr/0005-fuentes-de-datos.md) |
| RNF-13 | **Observabilidad:** Micrometer, Prometheus y Grafana; logs JSON con `traceId` y el ID de usuario con hash; `evento_uso` con HMAC, seudonimizada | [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md), [CONTEXT](../CONTEXT.md) §0.8 |

## 6. Métricas de éxito

**Producto** ([CONTEXT](../CONTEXT.md) §0.10 y §1.8):
- 10-30 usuarios reales (USAL, gimnasio, equipo de CTF); en la v1.0, 20 del gimnasio y Google Play en prueba cerrada.
- Retención a 7 días y acciones por semana, medidas en `evento_uso`, sin analítica de terceros. Umbral de éxito **[propuesta: fijarlo con los primeros 10 usuarios]**.

**Modelos** (cada número va al README):
| Modelo | Métrica | Dataset |
|---|---|---|
| LLM de entrada | Restricciones inventadas = 0; sensibilidad a banderas rojas que no baje | 100-200 frases sintéticas en español |
| MediaPipe Pose | Error frente a cinta, expresado como acierto de tramo **[propuesta: el plan lo medía en ratio]** | 15-20 personas del gimnasio, fotos fuera del repo |
| Florence-2 | CER del OCR | 100 etiquetas propias |
| Normalización a JSON | Precisión y recall de ingredientes | Las mismas etiquetas, etiquetadas a mano |
| Sinónimos de sustancias | Falsos negativos = 0 en el conjunto conocido **[propuesta: umbral]** | Falsos negativos conocidos (DMAA y otros) |
| LLM de intención | Exactitud de la intención | 50 frases de prueba |
| Todos | Latencia p50 y p95 en GPU frente a CPU, y antes y después de cuantizar | Las de arriba |

**Calidad:** cobertura ≥ 80 % y CI en verde (tests, eval y seguridad).

## 7. Fuera de alcance y riesgos abiertos

### Fuera de alcance
- Que un LLM genere la rutina o escriba series ([0002](adr/0002-solver-cp-sat.md), [0007](adr/0007-motor-de-seleccion-de-ejercicios.md)).
- Asimetrías o atrofias por foto (ICC < 0,40) e inserciones musculares ([0007](adr/0007-motor-de-seleccion-de-ejercicios.md)).
- Guardar fotos, también para el «antes y después»; LLM externos y analítica de terceros ([0004](adr/0004-privacidad-fotos-y-datos-de-salud.md)).
- Pose en el móvil: candidata después de la v0.2 ([0004](adr/0004-privacidad-fotos-y-datos-de-salud.md)).
- macOS y firma de código al principio; registro sin conexión y cámara en vivo en PC ([0006](adr/0006-version-de-pc.md)).
- iOS: ninguna fuente lo planifica; se publica APK y PC ([CLAUDE.md](../CLAUDE.md) §8).
- Scraping de supermercados, lista de la AMA desde su web y búsqueda vectorial ([0005](adr/0005-fuentes-de-datos.md), [0001](adr/0001-stack.md)).
- «Entrar con Google», enlace mágico y CAPTCHA de terceros ([0009](adr/0009-autenticacion.md)).
- Microservicios por módulo ([0001](adr/0001-stack.md)).

### Riesgos abiertos
| Riesgo | Estado | Dónde |
|---|---|---|
| Las banderas rojas y los perfiles que no se adaptan son propuestas propias **sin revisión clínica** | Un fisio los revisa antes de la v0.2; la investigación deja además 17 huecos sin verificar | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| Que la app se considere producto sanitario (MDR, clase IIa según la MDCG 2019-11 rev.1) | Finalidad declarada y lista de palabras prohibidas (RNF-09) | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md), [CONTEXT](../CONTEXT.md) §1.9 |
| Licencia del cribado tipo PAR-Q+ | Sin comprobar | [0007](adr/0007-motor-de-seleccion-de-ejercicios.md) |
| El alcance: tres apps para una persona | Releases por módulo | [CONTEXT](../CONTEXT.md) §1.9 |
| Symmetry ya da el mapa y la rutina con IA gratis en Android; BodyLeveling ocupa el eslogan de las palancas | El gancho es la explicación con fuente, no el mapa | [competencia](competencia.md) |
| mercaapi tira de la API interna de Mercadona (art. 133 LPI) | Alcance mínimo y flag apagado en la v1.0 pública | [0005](adr/0005-fuentes-de-datos.md) |
| Licencia de la fuente de sinónimos (PubChem) y aviso legal de CELAD y AESAN | Verificar en la fase 4 | [0005](adr/0005-fuentes-de-datos.md) |
| Sesgo hacia arriba de los percentiles de OpenPowerlifting | Se dice junto al mapa | [0005](adr/0005-fuentes-de-datos.md) |
| Pesos del objetivo de CP-SAT y tiempo de resolución con el catálogo | Planes revisados a mano; filtro previo y límite de tiempo | [0002](adr/0002-solver-cp-sat.md) |
| OR-Tools nativo en la imagen Docker (glibc) | Comprobar en la fase 1 | [0002](adr/0002-solver-cp-sat.md) |
| WebKitGTK en Linux con NVIDIA y llavero sin Secret Service | DMABUF desactivado si hace falta; Stronghold como plan B | [0006](adr/0006-version-de-pc.md) |
| Proveedor de correo con servidores en la UE | Elegir en la fase 1 | [0009](adr/0009-autenticacion.md) |
| Política de privacidad, EIPD, registro de actividades y caducidad de copias (30 días) | Antes de abrir la app a usuarios | [0004](adr/0004-privacidad-fotos-y-datos-de-salud.md) |
