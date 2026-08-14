#!/usr/bin/env bash
set -euo pipefail

# Human-friendly wrapper around the WebSphere Jython configuration script.
# Export DB_PASSWORD before running this command; its value is never printed.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBSPHERE_CONTAINER="${WEBSPHERE_CONTAINER:-legacy-poc-websphere}"
CONTAINER_SCRIPT="/work/config/configure-websphere-postgresql.py"

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo "DB_PASSWORD is required." >&2
  echo "Example: DB_PASSWORD='your-local-password' ./local-poc/configure-websphere.sh" >&2
  exit 1
fi

echo "1/2 Copying the WebSphere configuration script into the container"
docker cp "${SCRIPT_DIR}/configure-websphere-postgresql.py" \
  "${WEBSPHERE_CONTAINER}:${CONTAINER_SCRIPT}"

echo "2/2 Configuring the authentication alias, JDBC provider, and DataSource"
docker exec -e DB_PASSWORD "${WEBSPHERE_CONTAINER}" sh -c '
  WAS_ADMIN_PASSWORD=$(cat /tmp/PASSWORD)
  exec /opt/IBM/WebSphere/AppServer/bin/wsadmin.sh \
    -lang jython \
    -user wsadmin \
    -password "$WAS_ADMIN_PASSWORD" \
    -f /work/config/configure-websphere-postgresql.py
'

echo "WebSphere PostgreSQL configuration completed."
