# Revisión del texto de consentimiento (preliminar)

> **No es asesoría legal.** Es una revisión hecha al desarrollar Forja (2026-11-21) contra el RGPD y la LOPDGDD, para llegar a la asesoría con el trabajo hecho. Una asesoría dio por buenos el consentimiento v2 y la política el 2026-11-21 ([ADR 0004](../adr/0004-privacidad-fotos-y-datos-de-salud.md), RF-45); la línea de encargados se rellenó después y está pendiente de pasársela.

## Qué se revisó
- Consentimiento de **limitaciones** (`frontend/src/pantallas/ConsentimientoLimitaciones.tsx`), base del art. 9.2.a RGPD para datos de salud.
- Consentimiento de **medidas de salud** (`frontend/src/pantallas/ConsentimientoMedidas.tsx`).
- Registro: edad mínima (`ReglasCuenta.EDAD_MINIMA`).

## Hallazgos y qué se hizo

| # | Hallazgo | Norma | Estado |
|---|---|---|---|
| 1 | El texto de limitaciones solo nombraba «la limitación funcional», pero ese consentimiento cubre también el **dolor de cada serie** y el **nivel de aviso** del cuestionario de dolor (se borran al retirarlo). Un consentimiento no es informado si no dice todo lo que se guarda. | RGPD arts. 4.11, 7, 9.2.a y 13 | Arreglado en el texto **v2** |
| 2 | No decía que retirar el consentimiento no afecta a lo tratado antes. | RGPD art. 7.3 | Arreglado en v2 |
| 3 | «No sale en registros» era ambiguo (¿el registro de series?). | Transparencia, art. 12 | Arreglado: «registros técnicos (logs)» |
| 4 | Faltaba cuánto tiempo se guarda. | art. 13.2.a | Arreglado: «hasta que lo retires o borres la cuenta» |
| 5 | Falta la información del art. 13 que va fuera de la pantalla: responsable y contacto, base jurídica de cada tratamiento, encargados (hosting, correo), transferencias fuera de la UE, derechos y cómo ejercerlos, reclamación ante la AEPD. | art. 13 | **Pendiente**: política de privacidad (borrador abajo) |
| 6 | Los consentimientos dados con la v1 siguen valiendo tras pasar a v2. Sin usuarios reales no importa; con ellos, un cambio sustancial obliga a pedirlo otra vez. | art. 7.1 (poder demostrarlo) | Arreglado (2026-11-22): al entrar, la app enseña el texto nuevo y no deja seguir sin aceptarlo o retirarlo (que borra). El backend respeta el antiguo mientras tanto, para que el plan no pierda las limitaciones sin avisar |
| 7 | Edad mínima 18 años: por encima de los 14 de la LOPDGDD. | LOPDGDD art. 7 | Correcto |
| 8 | Medidas de salud: dice qué, para qué, que es opcional y cómo retirarlo. Falta la conservación y el art. 7.3. | arts. 7.3 y 13 | Arreglado en medidas **v2**: cuánto y cómo se guarda (las medidas, cifradas; los tramos de movilidad no) y art. 7.3 |

## Borrador de política de privacidad (lo que falta del art. 13)
Rellenar lo marcado con `[…]` antes de publicar:
- **Responsable:** `[nombre y NIF]`, `[correo de contacto]`.
- **Qué y para qué:** cuenta (correo, año de nacimiento) para darte acceso; limitaciones, dolor y nivel de aviso, solo con tu consentimiento, para elegir ejercicios compatibles; movilidad y medidas, con su consentimiento, para elegir variantes y estimar el % de grasa; sesiones registradas para la progresión y tu racha.
- **Base jurídica:** contrato (art. 6.1.b) para la cuenta y el registro de sesiones; consentimiento explícito (art. 9.2.a) para los datos de salud.
- **Conservación:** mientras tengas cuenta; los datos de salud, hasta que retires su consentimiento. Al borrar la cuenta se borra todo (solo quedan, hasta que caducan, los hashes de los tokens de sesión, sin nada que te identifique).
- **Encargados:** Microsoft Ireland Operations Ltd. (Azure) para el hosting, la base de datos y el correo transaccional; los datos se guardan en la UE (región España Central y Azure Communication Services con ubicación «Europe»). Transferencias fuera de la UE: Microsoft puede dar soporte desde fuera; se apoya en el Marco de Privacidad de Datos UE-EE. UU. y en cláusulas contractuales tipo `[propuesta: confirmarlo con la asesoría]`.
- **Derechos:** acceso y portabilidad («Descargar mis datos»), rectificación (en la app), supresión (borrar cuenta), retirar cada consentimiento en su pantalla, oposición y limitación escribiendo a `[correo]`. Puedes reclamar ante la AEPD (aepd.es).
