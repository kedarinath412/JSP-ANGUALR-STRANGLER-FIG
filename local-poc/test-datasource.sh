#!/usr/bin/env bash
set -euo pipefail

# Human-friendly wrapper for WebSphere's DataSource connection test.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBSPHERE_CONTAINER="${WEBSPHERE_CONTAINER:-legacy-poc-websphere}"
CONTAINER_SCRIPT="/work/config/test-websphere-datasource.py"
SERVER_LOG="/opt/IBM/WebSphere/AppServer/profiles/AppSrv01/logs/server1/SystemOut.log"

echo "1/2 Copying the connection-test script into the container"
docker cp "${SCRIPT_DIR}/test-websphere-datasource.py" \
  "${WEBSPHERE_CONTAINER}:${CONTAINER_SCRIPT}"

START_LINE="$(docker exec "${WEBSPHERE_CONTAINER}" sh -c "wc -l < '${SERVER_LOG}'")"

echo "2/2 Asking WebSphere to test jdbc/LegacyPocDS"
docker exec "${WEBSPHERE_CONTAINER}" sh -c '
  WAS_ADMIN_PASSWORD=$(cat /tmp/PASSWORD)
  exec /opt/IBM/WebSphere/AppServer/bin/wsadmin.sh \
    -lang jython \
    -user wsadmin \
    -password "$WAS_ADMIN_PASSWORD" \
    -f /work/config/test-websphere-datasource.py
'

# With a user-defined provider and GenericDataStoreHelper, wsadmin can display
# WASX7388E even when the JDBC connection succeeded with a warning. The server
# log's DSRA8030I message is WebSphere's authoritative success confirmation.
NEW_LOG="$(docker exec "${WEBSPHERE_CONTAINER}" \
  sh -c "tail -n +$((START_LINE + 1)) '${SERVER_LOG}'")"

if grep -q "DSRA8030I: Successfully connected to DataSource" <<<"${NEW_LOG}"; then
  echo "SUCCESS: WebSphere connected to jdbc/LegacyPocDS."
else
  echo "FAILURE: WebSphere did not confirm a successful DataSource connection." >&2
  grep -E "DSRA|J2CA|PostgreSQL|[Ee]xception" <<<"${NEW_LOG}" >&2 || true
  exit 1
fi
