# Atalhos da stack local. Tudo roda em Docker; nenhuma conta AWS é usada.
COMPOSE = docker compose -f infra/local/docker-compose.yml --env-file infra/local/.env

.PHONY: up down reset logs status verify build test env redrive observabilidade e2e

env:
	@test -f infra/local/.env || cp infra/local/.env.example infra/local/.env

up: env ## sobe a stack (LocalStack + Redis + serviços)
	$(COMPOSE) up -d --build --wait

down: ## derruba os containers (LocalStack e Redis voltam zerados no próximo up)
	$(COMPOSE) down

reset: ## derruba e apaga volumes
	$(COMPOSE) down -v --remove-orphans

logs:
	$(COMPOSE) logs -f --tail=100

status: ## saúde dos serviços, filas, DLQ e Lambdas (sai com 1 se algo estiver fora)
	infra/local/status.sh

ARGS ?= {"dryRun":true}
redrive: ## lista (ou devolve, com dryRun false) as mensagens da fiscal-dlq: make redrive ARGS='{"origem":"sefaz-queue","max":10,"dryRun":false}'
	@docker exec wc-localstack awslocal lambda invoke --function-name wc-dlq-redrive --payload '$(ARGS)' /tmp/redrive.json >/dev/null
	@docker exec wc-localstack cat /tmp/redrive.json | python3 -m json.tool

observabilidade: ## sobe Prometheus (:9090) e Grafana (:3000) com o painel da WC Autopeças
	$(COMPOSE) --profile observabilidade up -d prometheus grafana

# Retentativas da SEFAZ em segundos (RF04 da HU-B17.4); o próximo make up volta ao padrão de 1 e 5 minutos.
e2e: env ## sobe a stack e roda os testes ponta a ponta pelo gateway; relatório em backend/e2e-tests/target/site/relatorio-e2e.html
	SEFAZ_ESPERAS_RETENTATIVA=2s,2s $(COMPOSE) up -d --build --wait
	cd backend && mvn -B -Pe2e -pl e2e-tests test; s=$$?; \
	  mvn -B -q -Pe2e -pl e2e-tests surefire-report:report-only; \
	  echo "relatório: backend/e2e-tests/target/site/relatorio-e2e.html"; exit $$s

verify: ## confere os recursos criados no LocalStack
	infra/local/verify.sh

build: ## compila e roda os testes de todos os módulos
	cd backend && mvn -B verify

test: build
