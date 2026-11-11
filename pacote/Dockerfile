# Imagens prontas da WC Autopeças (pasta pacote/). Quem roda o pacote só baixa as imagens;
# nada é compilado no PC de quem usa. O CI publica no GHCR (workflow "imagens").
#
#   docker build -f pacote/Dockerfile --target servicos   -t wc-servicos   .
#   docker build -f pacote/Dockerfile --target localstack -t wc-localstack .
#   docker build -f pacote/Dockerfile --target sefaz-mock -t wc-sefaz-mock .
#
# O Maven roda uma vez só, na arquitetura de quem builda ($BUILDPLATFORM): os jars servem para
# amd64 e arm64, então a imagem multiplataforma não recompila nada em emulação.

FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY backend/ .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests && \
    mkdir -p /jars /lambdas && \
    for m in auth-service customer-service product-service sales-service fiscal-service \
             entry-service document-service reports-service notification-service; do \
      cp "$m/target/$m.jar" /jars/; \
    done && \
    cp wc-jwt-authorizer/target/wc-jwt-authorizer.jar wc-dlq-lambdas/target/wc-dlq-lambdas.jar /lambdas/

# Uma imagem para os 9 serviços Java; a variável SERVICO escolhe qual jar roda.
FROM eclipse-temurin:21-jre-alpine AS servicos
RUN addgroup -S wc && adduser -S wc -G wc
WORKDIR /app
COPY infra/local/init/resources.env /app/resources.env
COPY pacote/servico.sh /app/servico.sh
COPY --from=build /jars/ /app/
USER wc
# Memória enxuta para caber em PCs de 8 GB: heap pequeno, GC serial e só o compilador C1.
ENV JAVA_OPTS="-Xms32m -Xmx160m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=160m -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=10s --timeout=3s --start-period=90s --retries=10 \
  CMD wget -qO- http://localhost:${SERVER_PORT:-8080}/actuator/health >/dev/null || exit 1
ENTRYPOINT ["sh", "/app/servico.sh"]

# LocalStack com os scripts de init e os jars das Lambdas dentro da imagem (sem pasta montada).
FROM localstack/localstack:3.8.1 AS localstack
COPY infra/local/init/ /etc/localstack/init/
COPY --from=build /lambdas/ /opt/wc/lambdas/
RUN chmod +x /etc/localstack/init/healthcheck.sh /etc/localstack/init/ready.d/*.sh

# Simulador da SEFAZ com os cenários dentro da imagem.
FROM wiremock/wiremock:3.9.2 AS sefaz-mock
COPY infra/local/sefaz-mock/mappings/ /home/wiremock/mappings/
