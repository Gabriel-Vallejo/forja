#!/usr/bin/env bash
# Lo mismo que el CI, en local (para cuando no hay minutos de GitHub Actions o antes de etiquetar una versión).
#
#   scripts/verificar.sh          # backend, app, catálogo, vision-service, compose y workflows
#   scripts/verificar.sh --e2e    # además, los flujos de Maestro (necesita emulador, backend local y
#                                 # CORREO/CONTRASENA de una cuenta con plan; ver frontend/README.md)
set -euo pipefail
cd "$(dirname "$0")/.."
paso() { printf '\n→ %s\n' "$1"; }

paso "compose y workflows"
docker compose --profile dev --profile gpu --profile obs config -q
docker run --rm -v "$PWD":/repo -w /repo rhysd/actionlint:1.7.12 -no-color

paso "backend (tests, cobertura y OpenAPI)"
(cd backend && ./mvnw -q -B verify)
git diff --exit-code -- docs/api/openapi.json

paso "app (lint, tipos, tests y tipos del OpenAPI)"
(cd frontend && npm run -s lint && npx tsc --noEmit && npx jest --silent && npm run -s api)
git diff --exit-code -- frontend/src/api/esquema.d.ts

for dir in database/catalogo database/estandares vision-service; do
	[ -f "$dir/pyproject.toml" ] || continue # el CI también se salta las carpetas sin código todavía
	paso "$dir"
	# Un entorno virtual por carpeta, fuera del repo; uv lo crea la primera vez.
	(cd "$dir" && uv run -q --extra dev ruff check . && uv run -q --extra dev pytest -q)
done
(cd database/catalogo && uv run -q --extra dev mypy --strict . && uv run -q python generar.py)
git diff --exit-code -- backend/src/main/resources/db/migration/R__catalogo_semilla.sql

if [ "${1:-}" = "--e2e" ]; then
	paso "Maestro"
	~/.maestro/bin/maestro test -e METRO=no -e CORREO="${CORREO:?}" -e CONTRASENA="${CONTRASENA:?}" frontend/.maestro/
fi
printf '\nTodo en verde.\n'
