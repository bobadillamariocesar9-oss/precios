# ═══════════════════════════════════════════════════════════════
# api-precios-web — proxy + frontend Vue 3 (Spring Boot 3.4.1 / Java 21)
# Build multi-stage: compila con Maven y corre solo con el JRE.
# ═══════════════════════════════════════════════════════════════

# ── Etapa 1: build ────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ── Etapa 2: runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/api-precios-web-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
