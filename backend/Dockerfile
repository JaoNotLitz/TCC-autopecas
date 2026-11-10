# Dockerfile único para todos os serviços: docker build --build-arg MODULE=auth-service .
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am package -DskipTests && \
    cp ${MODULE}/target/${MODULE}.jar /app.jar

# Alvo "lambda": só entrega o jar (docker compose copia para o volume lido pelo LocalStack).
FROM alpine:3.20 AS lambda
COPY --from=build /app.jar /artifacts/app.jar
ARG MODULE
ENV MODULE=${MODULE}
CMD ["sh", "-c", "cp /artifacts/app.jar /out/${MODULE}.jar && echo \"${MODULE}.jar pronto\""]

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl && addgroup -S wc && adduser -S wc -G wc
USER wc
WORKDIR /app
COPY --from=build /app.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -sf http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
