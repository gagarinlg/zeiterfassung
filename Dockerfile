# Stage 1: Build backend
FROM gradle:8.6-jdk21 AS backend-build
WORKDIR /app/backend
COPY backend/settings.gradle.kts backend/build.gradle.kts ./
COPY backend/gradle ./gradle
RUN gradle dependencies --no-daemon || true
COPY backend/src ./src
RUN gradle bootJar --no-daemon -x test

# Stage 2: Build frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN addgroup --system zeiterfassung && adduser --system --ingroup zeiterfassung zeiterfassung

COPY --from=backend-build /app/backend/build/libs/*.jar app.jar
COPY --from=frontend-build /app/frontend/dist ./static

RUN chown -R zeiterfassung:zeiterfassung /app
USER zeiterfassung

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
