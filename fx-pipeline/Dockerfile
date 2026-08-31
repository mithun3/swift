# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
# Copy parent POM and all child modules
COPY pom.xml .
COPY client/pom.xml client/
COPY common/pom.xml common/
COPY serv-0/pom.xml serv-0/
COPY serv-a/pom.xml serv-a/
COPY serv-b/pom.xml serv-b/
COPY serv-c/pom.xml serv-c/
COPY test/pom.xml test/

# Copy source code
COPY client/src client/src
COPY common/src common/src
COPY serv-0/src serv-0/src
COPY serv-a/src serv-a/src
COPY serv-b/src serv-b/src
COPY serv-c/src serv-c/src
COPY test/src test/src

# Build the project and copy dependencies
RUN mvn clean package dependency:copy-dependencies -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy compiled JARs and dependencies from the builder stage
COPY --from=builder /app/common/target/common-1.0.0-SNAPSHOT.jar common/target/
COPY --from=builder /app/common/target/dependency/ common/target/dependency/

COPY --from=builder /app/serv-0/target/serv-0-1.0.0-SNAPSHOT.jar serv-0/target/
COPY --from=builder /app/serv-0/target/dependency/ serv-0/target/dependency/

COPY --from=builder /app/serv-a/target/serv-a-1.0.0-SNAPSHOT.jar serv-a/target/
COPY --from=builder /app/serv-a/target/dependency/ serv-a/target/dependency/

COPY --from=builder /app/serv-b/target/serv-b-1.0.0-SNAPSHOT.jar serv-b/target/
COPY --from=builder /app/serv-b/target/dependency/ serv-b/target/dependency/

COPY --from=builder /app/serv-c/target/serv-c-1.0.0-SNAPSHOT.jar serv-c/target/
COPY --from=builder /app/serv-c/target/dependency/ serv-c/target/dependency/

COPY --from=builder /app/test/target/test-1.0.0-SNAPSHOT.jar test/target/
COPY --from=builder /app/test/target/dependency/ test/target/dependency/

# Copy scripts for benchmarking
COPY scripts/ /app/scripts/
RUN chmod +x /app/scripts/*.sh

# Install Python 3 and required packages for generating HTML benchmark reports
RUN apt-get update && apt-get install -y python3 python3-pandas python3-matplotlib python3-numpy && rm -rf /var/lib/apt/lists/*

# Set up environment variables for ZGC and Chronicle Queue reflection.
# Flags are optimised for the Docker paired-cpuset deployment model
# (2 vCPUs per service via FX_SERV_X_CPUSET). The housekeeping-containment
# flags (ConcGCThreads, ParallelGCThreads, ActiveProcessorCount) prevent ZGC
# and JIT threads from monopolising both vCPUs and starving the event loop.
ENV JVM_OPTS="--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
              --add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
              --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED \
              --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
              --add-opens=java.base/java.lang=ALL-UNNAMED \
              --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
              --add-opens=java.base/java.io=ALL-UNNAMED \
              --add-opens=java.base/java.util=ALL-UNNAMED \
              -XX:+UseZGC -XX:+ZGenerational \
              -Xmx512m -Xms512m \
              -XX:SoftMaxHeapSize=384m \
              -XX:+AlwaysPreTouch \
              -XX:+DisableExplicitGC \
              -XX:-ZUncommit \
              -XX:ZUncommitDelay=600 \
              -XX:+UseContainerSupport \
              -XX:ActiveProcessorCount=2 \
              -XX:ConcGCThreads=1 \
              -XX:ParallelGCThreads=1 \
              -XX:ZAllocationSpikeTolerance=3.0 \
              -XX:CompileThreshold=500 \
              -XX:+TieredCompilation"

# Command is provided by docker-compose.yml
CMD ["echo", "Please specify a command in docker-compose.yml"]
