# Atalhos da stack local. Tudo roda em Docker; nenhuma conta AWS é usada.
COMPOSE = docker compose -f infra/local/docker-compose.yml --env-file infra/local/.env

.PHONY: up down reset logs status verify build test env

env:
	@test -f infra/local/.env || cp infra/local/.env.example infra/local/.env

up: env ## sobe a stack (LocalStack + Redis + serviços)
	$(COMPOSE) up -d --build --wait

down: ## derruba os containers (mantém o volume do Redis)
	$(COMPOSE) down

reset: ## derruba e apaga volumes
	$(COMPOSE) down -v --remove-orphans

logs:
	$(COMPOSE) logs -f --tail=100

status:
	$(COMPOSE) ps

verify: ## confere os recursos criados no LocalStack
	infra/local/verify.sh

build: ## compila e roda os testes de todos os módulos
	cd backend && mvn -B verify

test: build
