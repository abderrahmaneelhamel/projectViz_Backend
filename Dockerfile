FROM openjdk:17-jdk-slim

# Install Graphviz
RUN apt-get update && \
    apt-get install -y --no-install-recommends graphviz && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the jar file into the container
COPY target/*.jar app.jar

EXPOSE 8080

# Specify the command to run on container start
ENTRYPOINT ["java", "-jar", "app.jar"]