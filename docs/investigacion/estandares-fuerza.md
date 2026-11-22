# Estándares de fuerza para más ejercicios: resultado

Fecha: 2026-11-22 · Encargo: [estandares-fuerza-encargo.md](estandares-fuerza-encargo.md) · Fichas completas en [fuentes/](fuentes/)

Cuatro investigaciones en paralelo, una por cada tipo de fuente del encargo: (1) datasets abiertos y webs de estándares, (2) ciencia sobre el 1RM, (3) peso corporal y normas de organismos, y (4) relaciones entre ejercicios. Cada ficha dice si la cifra se leyó en la fuente primaria o solo en el resumen. Yo he vuelto a comprobar las dos fuentes en las que se apoya la recomendación principal (marcadas ✔).

## 1. Resumen

**Fuera de la sentadilla, la banca y el peso muerto no hay tablas de percentiles de gente de gimnasio con muestra grande y licencia abierta.** Lo que sí hay:

| Qué | Ejercicios | Fuente | Licencia | Cuándo |
|---|---|---|---|---|
| **Percentiles propios, como con OpenPowerlifting** | Dominada con lastre, fondo con lastre | [OpenStreetlifting](https://github.com/openstreetlifting/openstreetlifting) ✔ | Datos CC0 1.0 ✔ (el código es AGPLv3 y no se usa) | **Ya**: mismo cálculo que `database/estandares` |
| **Percentiles publicados** | Plancha (segundos) | Strand et al. 2014, *J Hum Kinet* ([PMC4096102](https://pmc.ncbi.nlm.nih.gov/articles/PMC4096102/)) ✔ | CC BY ✔ | **Con trabajo**: la app aún no registra series por tiempo |
| **Rango estimado a partir de otro levantamiento** | Sentadilla frontal (×0,84 de la sentadilla, r = 0,93, nivel B) | Ray et al. 2017 | CC BY | **Con aviso**: solo hombres; franja, no percentil |
| Lo mismo, con menos base (nivel C) | Press inclinado, press con mancuernas, press militar, curl | Estudios de 12 a 123 personas | Varias | **Esperar**: las cifras se contradicen o cambian mucho por sexo |
| **Baremos de puntos, no percentiles** | Dominadas y flexiones con el peso corporal | Marines de EE. UU. (MCO 6100.13A), ejército de EE. UU. (AFT 2025), Orden DEF/15/2026 | Dominio público / BOE reutilizable | **No como rango**: son umbrales de aprobado. Como mucho, «equivale a X puntos del baremo militar» |
| **Sin estándar** | Prensa, peso muerto rumano, hip thrust, remo con barra, jalón, extensiones, elevaciones, fondos y dominadas sin lastre, elevación de piernas colgado | — | — | No hay fuente que aguante. La prensa además depende de la máquina |

**Recomendación:** hacer ahora la **dominada y el fondo con lastre** con OpenStreetlifting, que es el mismo camino que ya funciona con OpenPowerlifting. Después, la **plancha** cuando la app registre tiempo, y la **sentadilla frontal** como estimación a partir de la sentadilla. El resto se queda «sin estándar» hasta que aparezca algo mejor.

**Una vía abierta (lo tiene que pedir Gabriel):** la API de FitnessVolt dice ser CC BY 4.0 y da percentiles con muestras enormes (por ejemplo, press militar de hombres de 83 kg con n = 115 964) de press militar, press inclinado, remo Pendlay, dominadas y sentadilla frontal. Pero atribuye los datos a Symmetric Strength, cuyos términos prohíben recolectarlos en bloque. Hasta que FitnessVolt confirme por escrito que puede licenciarlos, **solo para comparar**.

## 2. Por ejercicio

Niveles de evidencia como en [docs/referencias.md](../referencias.md): A (ensayo o meta-análisis), B (estudio observacional sólido), C (estudio pequeño o población distinta), D (norma u opinión), H (heurística de entrenadores) y P (propuesta propia).

| Ejercicio | Mejor fuente | Métrica | Integración | Nivel | Veredicto |
|---|---|---|---|---|---|
| Dominadas (prono y supino) con lastre | OpenStreetlifting: 6 897 entradas H / 1 938 M | kg de lastre, 1RM de competición | Percentiles 1-99 por sexo y franja de peso | B (población de competidores) | **Usar con aviso** |
| Fondos con lastre | OpenStreetlifting: 6 590 H / 1 864 M | kg de lastre | Igual | B | **Usar con aviso** |
| Plancha | Strand 2014: 471 universitarios, mediana 110 s H / 72 s M | Segundos | Percentiles publicados por sexo; hace falta registrar tiempo | B | **Usar con aviso** (población joven de EE. UU.) |
| Sentadilla frontal | Ray 2017: 0,84 ± 0,06 de la trasera, n = 21 hombres | Relación | Franja «estimada a partir de tu sentadilla» | B | **Usar con aviso** (sin dato de mujeres) |
| Press de banca con agarre cerrado | Lockie 2017: 0,94, r = 0,982, n = 20 | Relación | Igual | B | Usar con aviso; no está en el catálogo curado |
| Press inclinado | Rodríguez-Ridao 2020 (0,71 entrenados, 45°) frente a Marques 2025 (0,97 novatos) | Relación | Franja muy ancha | C | Esperar |
| Press con mancuernas | Saeterbakken 2011: 0,83, n = 12 | Relación | — | C | Esperar (falta confirmar si es la suma de las dos mancuernas) |
| Press militar de pie | Nada medido de pie con barra; sentado en Smith: 0,72 H / 0,82 M de la banca | Relación | — | C | Esperar |
| Curl con barra | Ribeiro 2014: 0,58 H / 0,74 M de la banca, n = 123 | Relación | — | C | Esperar |
| Hip thrust | Hammond 2019 y Plotkin 2023: 1,25-1,48 de la sentadilla | Relación | — | C | Solo para comparar |
| Remo con barra | Del inclinado no hay dato; el remo tumbado no es comparable | — | — | — | Sin estándar |
| Prensa de piernas | R² de 0,25 a 0,55 con la sentadilla; depende de la máquina | — | — | — | Sin estándar |
| Peso muerto rumano, jalón, extensiones, elevaciones, elevación de piernas | Nada verificable | — | — | — | Sin estándar |
| Dominadas y flexiones sin lastre | Baremos militares, no percentiles | Repeticiones | Solo «puntos del baremo» si se hace | D | No como rango |

## 3. Cómo se integraría

1. **OpenStreetlifting**, en `database/estandares`:
   - Leer `backend/data/competitions/**/entries.csv` desde un commit fijo, para que sea reproducible, y quedarse con `Status = competed` y lastre > 0.
   - De cada atleta, la mejor marca. La clave es nombre + apellido + `Disambiguation`, y se descarta después: no se guarda ningún nombre.
   - Percentiles 1-99 por sexo y franja de peso, con la misma muestra mínima de 200.
   - Decidir si entra el 26 % de pesos corporales «recuperados» de la puntuación. Mi propuesta: descartarlos y ver si las franjas siguen llegando a 200.
2. **Esquema:**
   - `estandar_fuerza.levantamiento` gana `dominada_lastre` y `fondo_lastre`.
   - Se añade una columna `metrica` (`kg_1rm` o `kg_lastre`), para que la app sepa que el dato es el lastre añadido y no la carga total.
3. **App:**
   - La «mejor serie» de dominadas y fondos tiene que calcular el e1RM sobre **peso corporal + lastre** y restar luego el peso. Aplicar Epley solo al lastre infravalora las series de muchas repeticiones con poco lastre.
   - Si no hay peso corporal, sin rango.
   - Las dominadas pronas y supinas del catálogo comparten tabla, con aviso: OpenStreetlifting no distingue el agarre.
4. **Aviso en la app**, como con OpenPowerlifting: «frente a competidores de streetlifting; quien compite levanta más que la media del gimnasio».
5. **Relaciones (sentadilla frontal):** una tabla `relacion_ejercicio` por sexo, con cociente, desviación, r, fuente y nivel. La app enseñaría una **franja** («estimado a partir de tu sentadilla: entre Plata II y Oro I»), nunca un percentil exacto.
6. **Plancha:** antes hay que registrar series por tiempo en el modelo de datos y en la pantalla de la sesión.

## 4. Riesgos y dudas

- **Sesgo de población:** OpenStreetlifting, igual que OpenPowerlifting, es de competidores (sobre todo de Italia, Francia, Alemania, Polonia y España). Quien entrena en un gimnasio saldrá bajo, y la app tiene que decirlo.
- **Técnica:** no se ha comprobado que todas las federaciones de streetlifting juzguen igual la dominada (agarre, extensión completa).
- **FitnessVolt / Symmetric Strength:** una licencia CC BY sobre datos que quizá no son suyos no vale. Solo para comparar hasta tener confirmación por escrito.
- **Strength Level, ExRx y Symmetric Strength:** prohíben copiar o hacer estándares derivados. Solo se pueden enlazar.
- **Relaciones de muestras pequeñas** (n = 12-30): varían por sexo y nivel y ninguna va por peso corporal. Por eso, franjas y no percentiles.
- **No comprobado:**
  - Los datos de Knapik 1993: normas de 6 022 soldados, posiblemente de dominio público, pero DTIC estaba en mantenimiento.
  - La orden de los marines vigente (CH-5).
  - Los textos completos de Kraemer 2026, Hoeger 1990 y Barbalho 2020.
  - El dataset de dominadas de Mendeley.
  - La licencia de Keefer 2020.
- **Errores en los originales**, encontrados al leerlos:
  - **Strand 2014, Tabla 2:** tiene cambiados los tamaños de muestra de deportistas y no deportistas.
  - **BOE de la Policía Nacional (2026):** llama «Flexiones» a la columna de dominadas.
  - **Reynolds 2006, Tabla 1:** las columnas por sexo dicen kg pero cuadran como libras (719 lb ≈ 326 kg). Es una deducción a partir de los totales; el artículo no lo dice.
