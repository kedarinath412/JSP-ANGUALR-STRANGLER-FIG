#!/usr/bin/env bash
set -euo pipefail

# Build-independent deployment wrapper. Run `mvn clean package` first.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
WEBSPHERE_CONTAINER="${WEBSPHERE_CONTAINER:-legacy-poc-websphere}"
EAR_FILE="${PROJECT_DIR}/legacy-poc-ear/target/legacy-poc-ear.ear"
CONTAINER_EAR="/work/app/legacy-poc-ear.ear"
CONTAINER_SCRIPT="/work/config/deploy-application.py"

if [[ ! -f "${EAR_FILE}" ]]; then
  echo "EAR not found: ${EAR_FILE}" >&2
  echo "Run 'mvn clean package' before deploying." >&2
  exit 1
fi

echo "1/3 Copying the EAR into the WebSphere container"
docker cp "${EAR_FILE}" "${WEBSPHERE_CONTAINER}:${CONTAINER_EAR}"

echo "2/3 Copying the WebSphere deployment script"
docker cp "${SCRIPT_DIR}/deploy-application.py" \
  "${WEBSPHERE_CONTAINER}:${CONTAINER_SCRIPT}"

echo "3/3 Installing or updating the application and starting it"
docker exec "${WEBSPHERE_CONTAINER}" sh -c '
  WAS_ADMIN_PASSWORD=$(cat /tmp/PASSWORD)
  exec /opt/IBM/WebSphere/AppServer/bin/wsadmin.sh \
    -lang jython \
    -user wsadmin \
    -password "$WAS_ADMIN_PASSWORD" \
    -f /work/config/deploy-application.py
'

echo "Deployment completed: http://localhost:9080/legacy-poc/"
