# syntax=docker/dockerfile:1
# Build từ thư mục gốc repo: docker build -f backend/Dockerfile .
# Một image chứa cả API và web/dist (ADR-0002, ADR-0005): web và API cùng tên miền.

FROM node:24-alpine AS web
WORKDIR /web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk AS backend
WORKDIR /build
COPY backend/ ./
COPY --from=web /web/dist/ src/main/resources/static/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 plandi
WORKDIR /app
COPY --from=backend /build/target/plandi-0.0.1-SNAPSHOT.jar app.jar
USER plandi
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
