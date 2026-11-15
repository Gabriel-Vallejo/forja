# 0002. Generar la rutina con CP-SAT

- **Estado:** aceptada (2026-11-15)
- **Fecha:** 2026-11-15

## Contexto
La rutina semanal tiene que cumplir a la vez:
- volumen por músculo dentro de un rango,
- duración máxima por sesión,
- material disponible y lesiones,
- variedad,
- encaje con las palancas del usuario.

Además, cada elección tiene que poder explicarse («por qué hack y no barra baja»). Es un problema combinatorio de asignación con restricciones y un objetivo ponderado. Las reglas tienen que ser auditables y testeables; un LLM no puede escribir las series (ver [CONTEXT.md](../../CONTEXT.md) §1.2.A.6).

## Decisión
**OR-Tools CP-SAT** (Apache 2.0) desde Java, con este diseño:

1. **Las restricciones absolutas se aplican como filtro antes del modelo:** material, contraindicación por lesión y ejercicios que el usuario excluye. Cada ejercicio descartado guarda su motivo («sin barra», «lesión de hombro → sin press por encima de la cabeza»). Así el modelo es más pequeño y la traza de los descartes sale gratis.
2. **Las restricciones duras del modelo son pocas y siempre satisfacibles:**
   - duración máxima por sesión,
   - máximo de ejercicios por patrón y día,
   - no repetir un básico pesado en días consecutivos.
3. **El volumen y la frecuencia son metas con holgura,** no restricciones duras. Cada una tiene una variable de holgura con una penalización alta en el objetivo, así que el solver siempre devuelve un plan. Si usa holgura, la app lo dice: «con 3 días y 45 min no llegas a 10 series de espalda; te quedan 8».
   - Las series de los músculos secundarios cuentan como 0,5 (Pelland 2026).
   - Frecuencia ≥ 2 por músculo: **heurística práctica** con penalización alta por defecto, que el usuario puede bajar a 1. Con el volumen igualado, la evidencia no la exige (Schoenfeld 2019, ver [referencias](../referencias.md#frecuencia-de-entreno)).
4. **Objetivo (maximizar)**, como suma de términos con pesos en configuración y no en código:
   - cobertura de los músculos débiles y prioritarios,
   - encaje biomecánico por palancas,
   - preferencias,
   - menos fatiga,
   - menos holgura.
5. **Traza:** después de resolver, se calcula la contribución de cada término del objetivo a cada ejercicio elegido, más los motivos de descarte del paso 1. El LLM solo convierte esa traza en texto.
6. **Reproducible:** parámetros fijos, semilla fija y límite de tiempo (5 s de partida **[estimación, medir en la fase 1]**). Los tests no comparan con un plan exacto: comprueban **propiedades** con jqwik, por ejemplo «nunca supera la duración» o «con lesión de hombro no hay press por encima de la cabeza».
7. **Diagnóstico de inviabilidad:** las asunciones de CP-SAT (`addAssumptions` y la lista de asunciones que bastan para la inviabilidad) solo se usan como **herramienta de depuración**, no en el camino normal. Su resultado no es mínimo y hay errores abiertos en el tracker de OR-Tools. La holgura del punto 3 cubre el caso del usuario.

La dieta usa el mismo paquete (OR-Tools): GLOP para la programación lineal, o CP-SAT si hay raciones enteras. No necesita ADR aparte.

## Alternativas
| Opción | A favor | En contra |
|---|---|---|
| Plantillas + reglas voraces (if/else) | Simple, rápido de escribir | No equilibra objetivos a la vez; cada caso nuevo es un parche; difícil de probar con propiedades |
| Timefold Solver (Java, Apache 2.0) | Muy Java, búsqueda local buena en problemas grandes | Nuestro problema es pequeño (~7 días × ~100 ejercicios candidatos); CP-SAT da el óptimo o una cota y es más conocido |
| MILP (GLOP/SCIP o CBC) | Estándar | Las restricciones lógicas (si va este, no va aquel) se modelan peor que en CP |
| Que un LLM genere la rutina | Rápido de prototipar | No auditable ni reproducible, alucina volúmenes; contradice la regla del proyecto |

## Consecuencias
- **Gana:** un plan siempre válido para las restricciones absolutas, explicaciones trazables y tests por propiedades. También es el contenido estrella del post y de la entrevista («mi app arma la rutina con un solver»).
- **Cuesta:** hay que modelar con cuidado y ajustar los pesos (se evalúan con planes revisados a mano). Además, la librería nativa de OR-Tools va en la imagen Docker del backend: comprobar en la fase 1 que funciona en la imagen base elegida (glibc).
- **Riesgo:** el tiempo de resolución crece con el catálogo. Mitigación: el filtro previo del punto 1, el límite de tiempo y devolver la mejor solución encontrada.

## Fuentes
- [CP-SAT Solver (OR-Tools)](https://developers.google.com/optimization/cp/cp_solver)
- [CpModel.addAssumptions (Javadoc)](https://javadoc.io/static/com.google.ortools/ortools-java/9.10.4067/com/google/ortools/sat/CpModel.html) · [el núcleo de inviabilidad no es mínimo](https://groups.google.com/g/or-tools-discuss/c/qlVv2uSq1uo)
- [docs/referencias.md](../referencias.md): volumen y frecuencia

## Enmienda (2026-11-15, revisión)
- **El máximo de volumen es duro.** La holgura vale solo para el **mínimo** de volumen y para la frecuencia. Si el máximo fuera blando, unos pesos mal ajustados podrían superarlo, y el máximo es la protección contra lesiones del plan. La propiedad de test pasa a ser «nunca supera el máximo de series por músculo», en lugar de «todo músculo prioritario ≥ 2 veces», que es falsa si el usuario baja la frecuencia a 1.
- **Reproducible de verdad:** el límite es `max_deterministic_time`, no el tiempo de reloj, con un número fijo de workers. Una semilla fija con 5 s de reloj no da el mismo plan cuando se agota el tiempo.
- **La traza dice si el plan es óptimo.** Guarda `OPTIMAL` o `FEASIBLE`. Si es `FEASIBLE`, la app no dice «el mejor plan», sino «un buen plan».
- **«Por qué este y no aquel»:** para las 2-3 alternativas más cercanas a cada ejercicio elegido, se fuerza el cambio, se vuelve a resolver y se guarda la diferencia del objetivo, o la restricción que lo impide. Es lo que permite al LLM explicar la elección y no solo listar contribuciones.
