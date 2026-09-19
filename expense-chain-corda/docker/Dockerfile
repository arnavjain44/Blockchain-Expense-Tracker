# Corda 4.11 Community Node Docker Image
FROM corda/community:4.11-zulu-openjdk8

LABEL maintainer="ExpenseChain Team"
LABEL description="Corda 4.11 containerized node for private permissioned network"

# Environment defaults
ENV ACCEPT_LICENSE=Y
ENV JVM_ARGS="-Xmx512m -Xms256m -XX:+UseG1GC"

# Set working directory to Corda standard location
WORKDIR /opt/corda

# Ensure directories exist with proper corda user ownership
USER corda
RUN mkdir -p /opt/corda/cordapps \
             /opt/corda/certificates \
             /opt/corda/persistence \
             /opt/corda/logs \
             /opt/corda/artemis \
             /opt/corda/additional-node-infos

# The base image provides run-corda entrypoint by default
