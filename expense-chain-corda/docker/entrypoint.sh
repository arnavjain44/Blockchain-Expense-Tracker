#!/bin/sh
set -e

CONFIG_FOLDER="${CONFIG_FOLDER:-/opt/corda}"
PERSISTENCE_FOLDER="${PERSISTENCE_FOLDER:-/opt/corda/persistence}"

echo "=========================================================="
echo "Checking database schema migration status..."
echo "=========================================================="
java -Djava.security.egd=file:/dev/./urandom \
     -Dcapsule.jvm.args="${JVM_ARGS}" \
     -jar /opt/corda/bin/corda.jar run-migration-scripts \
     --core-schemas \
     --app-schemas \
     --base-directory /opt/corda \
     --config-file "${CONFIG_FOLDER}/node.conf"

echo "=========================================================="
echo "Starting Corda node..."
echo "=========================================================="
count=$(grep -c dataSourceProperties "${CONFIG_FOLDER}/node.conf" 2>/dev/null || true)

if [ "$count" -eq 0 ]; then
  exec java -Djava.security.egd=file:/dev/./urandom \
            -Dcapsule.jvm.args="${JVM_ARGS}" \
            -Dcorda.dataSourceProperties.dataSource.url="jdbc:h2:file:${PERSISTENCE_FOLDER}/persistence;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0;LOCK_TIMEOUT=10000" \
            -jar /opt/corda/bin/corda.jar \
            --base-directory /opt/corda \
            --config-file "${CONFIG_FOLDER}/node.conf" ${CORDA_ARGS} "$@"
else
  exec java -Djava.security.egd=file:/dev/./urandom \
            -Dcapsule.jvm.args="${JVM_ARGS}" \
            -jar /opt/corda/bin/corda.jar \
            --base-directory /opt/corda \
            --config-file "${CONFIG_FOLDER}/node.conf" ${CORDA_ARGS} "$@"
fi
