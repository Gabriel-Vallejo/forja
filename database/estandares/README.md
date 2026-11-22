# Estándares de fuerza

Percentiles de sentadilla, banca y peso muerto por sexo y franja de peso, sacados del CSV de [OpenPowerlifting](https://www.openpowerlifting.org) (dominio público; su código es AGPLv3 y no se usa). Generan la semilla `backend/src/main/resources/db/migration/R__estandares_fuerza.sql` (RF-30, ADR 0005).

```bash
uv run --no-project python descargar.py   # 171 MB a .cache/ (ignorado por git), con URL, fecha y sha256
uv run --no-project python calcular.py    # ~1 min: escribe la semilla e imprime n, p50 y p90 de cada grupo
uv run --extra dev pytest                 # tests con datos sintéticos
```

Criterios: solo *raw* (sin equipación), mayores de 18 o sin edad indicada, la mejor marca de cada persona y percentiles 1-99 de los grupos con 200 personas o más. Son levantadores de competición: los percentiles quedan por encima del gimnasio medio, y la app lo dice. Actualizar una vez al año.

*This page uses data from the OpenPowerlifting project, https://www.openpowerlifting.org. You may download a copy of the data at https://data.openpowerlifting.org.*
