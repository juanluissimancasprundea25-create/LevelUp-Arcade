# ============================================================
# Dockerfile multi-stage para LevelUp Arcade
# Stage 1: compila el proyecto con Maven
# Stage 2: imagen ligera de runtime con solo el JAR
# ============================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copia el wrapper de Maven primero (cache de capas)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Descarga dependencias (esto se cachea si pom.xml no cambia)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copia el código fuente y compila
COPY src src
RUN ./mvnw clean package -DskipTests -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia solo el JAR final desde el stage anterior
COPY --from=build /app/target/*.jar app.jar

# Puerto que expone la aplicación
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]