# =========================================================
# BUILD STAGE
# =========================================================

FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy the complete backend project
COPY . .

# Make Maven wrapper executable
RUN chmod +x mvnw

# Build Spring Boot application
RUN ./mvnw clean package -DskipTests


# =========================================================
# RUNTIME STAGE
# =========================================================

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the generated Spring Boot JAR
COPY --from=build /app/target/*.jar app.jar

# Copy only the runtime graph required by GurgaonRoadGraph
COPY --from=build /app/data/osm/gurgaon_graph.json /app/data/osm/gurgaon_graph.json

# Render provides PORT through an environment variable.
# Spring Boot uses ${PORT:8080} from application.properties.
EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]