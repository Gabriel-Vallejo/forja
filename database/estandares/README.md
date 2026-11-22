# Estándares de fuerza

Percentiles por sexo y franja de peso: sentadilla, banca y peso muerto del CSV de [OpenPowerlifting](https://www.openpowerlifting.org) (dominio público), y dominada y fondo con lastre de [OpenStreetlifting](https://openstreetlifting.org) (datos CC0). El código de los dos es AGPLv3 y no se usa. Generan la semilla `backend/src/main/resources/db/migration/R__estandares_fuerza.sql` (RF-30, ADR 0005).

```bash
uv run --no-project python descargar.py   # 171 MB a .cache/ (ignorado por git), con URL, fecha y sha256
git clone --depth 1 https://github.com/openstreetlifting/openstreetlifting .cache/openstreetlifting  # el commit queda en la semilla
uv run --no-project python calcular.py    # ~1 min: escribe la semilla e imprime n, p50 y p90 de cada grupo
uv run --extra dev pytest                 # tests con datos sintéticos
```

Criterios de OpenStreetlifting: quien compitió con lastre, su mejor marca; entran los pesos «recovered» (salen exactos de la puntuación RIS). La métrica es el lastre añadido (`kg_lastre`).

Criterios de OpenPowerlifting: solo *raw* (sin equipación), mayores de 18 o sin edad indicada, la mejor marca de cada persona y percentiles 1-99 de los grupos con 200 personas o más. Son levantadores de competición: los percentiles quedan por encima del gimnasio medio, y la app lo dice. Actualizar una vez al año.

*This page uses data from the OpenPowerlifting project, https://www.openpowerlifting.org. You may download a copy of the data at https://data.openpowerlifting.org.*
