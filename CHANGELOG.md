# Changelog

## [Sin publicar]

## [0.1.3] - 2026-11-22
- Consentimiento de medidas **v2**: cuánto y cómo se guarda y que retirarlo no anula lo hecho antes (art. 7.3 RGPD).
- Si un consentimiento se dio con un texto anterior, al entrar la app enseña el nuevo y no deja seguir sin aceptarlo o retirarlo (y borrar). Sin conexión no bloquea nada.

## [0.1.2] - 2026-11-22
- La pregunta de hormigueo del cuestionario de dolor también pide lo nuevo o lo que va a más, como la de fuerza: una neuropatía crónica ya valorada no bloquea el plan. Las dos banderas citan además NICE NG59 (1.1.1).
- [docs/revision-fisio.md](docs/revision-fisio.md): las banderas rojas, el cribado, los umbrales de dolor y los tests de movilidad, listos para que los revise un fisioterapeuta.

## [0.1.1] - 2026-11-22
- La pregunta de pérdida de fuerza del cuestionario de dolor pregunta por la debilidad nueva o que va a más: quien tiene una crónica ya valorada por un profesional puede contestar que no sin saltarse nada.
- CI con menos minutos (E2E al etiquetar o a mano) y `scripts/verificar.sh` para pasar lo mismo en local.

## [0.1.0] - 2026-11-21
Primera versión: el entreno. APK para Android en [Releases](https://github.com/Gabriel-Vallejo/forja/releases/tag/v0.1.0), backend en Azure.

### Cuenta y privacidad
- Registro con correo y contraseña (Argon2id y lista local de 46 000 contraseñas filtradas), verificación y recuperación por correo (Azure Communication Services, datos en Europa).
- Dispositivos con sesión propia, «Descargar mis datos» y borrado total de la cuenta.
- Limitaciones con consentimiento aparte (texto v2) y cifradas por columna.

### Entreno
- Cribado de 7 preguntas con banderas rojas por reglas que bloquean el plan cuando toca.
- Tres tests de movilidad que cambian variantes concretas del plan.
- Rutina con CP-SAT: tiempo por sesión, topes por patrón y día, 10-20 series semanales por músculo, los 6 patrones básicos cada semana y «por qué este ejercicio» con su fuente y su nivel de evidencia.
- Ajustes con vista previa: una serie más o menos, reemplazar, no recomendar, variedad, enfocar y material del gimnasio.

### Registro
- Series sin conexión con RPE y dolor, deshacer y sincronización al volver la cobertura.
- Progresión doble con deload automático y aviso si un ejercicio duele en 2 de sus 3 últimas sesiones.
- Racha semanal y cifras del perfil con las sesiones reales.

### Infraestructura
- Imagen Docker del backend y despliegue en Azure (Container Apps, MySQL Flexible y Communication Services) con `infra/azure/desplegar.sh`.
- 2 652 tests del backend, 538 de la app y 4 flujos de Maestro en emulador.
