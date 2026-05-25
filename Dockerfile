# ============================================================
# Dockerfile multi-stage para LevelUp Arcade
# Stage 1: compila el JAR con Maven y JDK 21
# Stage 2: imagen final ligera con solo JRE 21
# ============================================================

# --- Stage 1: build ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiamos primero solo los ficheros de Maven para aprovechar la cache
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Damos permisos de ejecucion al wrapper
RUN chmod +x mvnw

# Descargamos dependencias (capa cacheable)
RUN ./mvnw dependency:go-offline -B

# Copiamos el codigo fuente y compilamos
COPY src src
RUN ./mvnw clean package -DskipTests -B

# --- Stage 2: runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S levelup && adduser -S levelup -G levelup

# Copiamos el JAR compilado desde el stage de build
COPY --from=build /app/target/*.jar app.jar

# Cambiamos a usuario no-root
USER levelup

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]