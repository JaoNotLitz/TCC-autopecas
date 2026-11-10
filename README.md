# TCC Autopeças — backend

Back-end do sistema fiscal da WC Autopeças: microsserviços Spring Boot que rodam 100% local com LocalStack e Docker.
O front React fica em [dinastiadoge/tcc](https://github.com/dinastiadoge/tcc).

- Arquitetura: [`ARQUITETURA-BACKEND-DETALHADA.md`](ARQUITETURA-BACKEND-DETALHADA.md)
- Contrato com o front: [`MAPEAMENTO-CLASSES.md`](MAPEAMENTO-CLASSES.md)
- Backlog (épicos e histórias): [`backlog/`](backlog/README.md)
- Código: [`backend/`](backend/README.md) · Stack local: [`infra/local/`](infra/local/README.md)

```bash
make up      # sobe LocalStack, Redis e os serviços
make verify  # confere os recursos AWS emulados
```
