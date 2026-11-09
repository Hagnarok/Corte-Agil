FROM node:24-bookworm-slim AS frontend
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY angular.json tsconfig*.json ./
COPY src ./src
COPY public ./public
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-17 AS backend
WORKDIR /build
COPY backend/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline
COPY backend/src ./src
RUN mvn -B package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 corte && useradd --uid 10001 --gid corte corte && mkdir data && chown corte:corte data
COPY --from=frontend /build/dist ./dist
COPY --from=backend /build/target/corte-agil.jar ./corte-agil.jar
USER corte
EXPOSE 3000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "corte-agil.jar"]
