# Use Eclipse Temurin Java 17 as base image
FROM eclipse-temurin:17-jdk-jammy

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src/ src/
COPY data/ data/

# Create results directory with proper permissions
RUN mkdir -p results && chmod 755 results

# Build the project
RUN mvn clean compile

# Set JVM options for better container performance
ENV JAVA_OPTS="-Xmx2g -XX:+UseG1GC -XX:+UseContainerSupport"

# Default command to run the TSP solver
CMD ["sh", "-c", "mvn exec:java -Dexec.mainClass=tsp.Main -Dexec.args=\"${JAVA_OPTS}\""]