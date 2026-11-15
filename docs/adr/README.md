# docs/adr

Decisiones de arquitectura numeradas (0001-…).

| ADR | Decisión | Estado |
|---|---|---|
| [0001](0001-stack.md) | Monolito modular en Java 21 + Spring Boot 4, MySQL 8.4 LTS, FastAPI para IA, Expo | aceptada |
| [0002](0002-solver-cp-sat.md) | Rutina con OR-Tools CP-SAT: filtro previo, volumen y frecuencia con holgura, traza | aceptada |
| [0003](0003-cola-de-trabajos-gpu.md) | Cola en MySQL con `SKIP LOCKED`; el worker de GPU del portátil tira por HTTPS; las imágenes no se encolan | aceptada |
| [0004](0004-privacidad-fotos-y-datos-de-salud.md) | Fotos en memoria, consentimiento del art. 9.2.a, cifrado por columna, borrado real | aceptada |
| [0005](0005-fuentes-de-datos.md) | free-exercise-db, OpenPowerlifting CSV, OFF, mercaapi autoalojado, lista del BOE y CELAD/AESAN | aceptada |
| [0006](0006-version-de-pc.md) | Versión de PC: export web de Expo dentro de Tauri 2 (Windows y Linux); en PC las fotos se suben como archivo | propuesta |
