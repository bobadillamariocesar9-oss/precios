# ═══════════════════════════════════════════════════════════════
# apiPrecios — backend (Spring Boot 3.4.1 / Java 21)
# Build multi-stage: compila con Maven y corre solo con el JRE.
# ═══════════════════════════════════════════════════════════════

# ── Etapa 1: build ────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cachea las dependencias en una capa separada (solo se re-descargan
# si cambia el pom.xml, no en cada cambio de código)
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests -Dmaven.test.skip=true

# ── Etapa 2: runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# NOTA sobre el scraper (CotoScraperService / Selenium):
# esta imagen NO incluye Chrome/Chromium, así que el endpoint
# POST /api/scraper/coto/run va a fallar dentro del contenedor.
# Si lo necesitás, agregá antes del ENTRYPOINT:
#   RUN apk add --no-cache chromium chromium-chromedriver
#   ENV CHROME_BIN=/usr/bin/chromium-browser

COPY --from=build /app/target/api-precios-0.0.1-SNAPSHOT.jar app.jar

# Snapshot de SEPA minorista del 24/11/2026 filtrado a CABA y EAN 7790742448309.
COPY deploy-data/sepa-caba-7790742448309-2026-11-24.zip /app/sepa.zip
ENV AGENT_SEPA_OFFICIAL_ZIP=/app/sepa.zip
ENV AGENT_LIVE_IMPORT_ENABLED=true

COPY deploy-data/flores-pilot/ /app/sepa-pilot/
ENV AGENT_SEPA_PILOT_ENABLED=true

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]

