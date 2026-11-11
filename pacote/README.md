# Pacote pronto: WC Autopeças em qualquer PC

Sobe o backend inteiro (LocalStack, Redis, simulador da SEFAZ e os 9 serviços) **sem compilar nada**:
o Docker só baixa imagens que o CI já buildou e publicou no GHCR. Não precisa de Java, Maven, `make`
nem do resto do repositório.

## Como rodar

1. Instale o [Docker Desktop](https://www.docker.com/products/docker-desktop/) (no Windows, com WSL2) e deixe-o aberto.
2. Baixe esta pasta `pacote/` (ou só o [`docker-compose.yml`](docker-compose.yml) e o `iniciar.bat`).
3. Rode:
   - **Windows:** dois cliques em `iniciar.bat` (para parar, `parar.bat`).
   - **Linux/macOS/WSL:** `./iniciar.sh`, ou direto `docker compose up -d --wait`.

A primeira vez baixa uns 1,5 GB de imagens; depois sobe em 1 a 2 minutos.

| O quê | Endereço |
|---|---|
| API pelo gateway (o que o front usa) | http://localhost:4566/restapis/wcapi/local/_user_request_/api |
| Login de exemplo | `admin@wcautopecas.com.br` / `admin123` (vendedor: `vendedor@wcautopecas.com.br` / `vendedor123`) |
| E-mails enviados (SES local) | http://localhost:4566/_aws/ses |
| Simulador da SEFAZ | http://localhost:8089 |
| Front (quando a imagem do front estiver publicada) | http://localhost:5173 |

Os dados ficam só na memória, como no `make up`: derrubar e subir de novo volta ao catálogo de exemplo.

## Memória

Feito para caber num PC de 8 GB com o WSL limitado a 4 GB:

- Cada serviço Java roda com heap de 160 MB, GC serial e só o compilador C1, com teto de 400 MB por container
  (na prática ficam em ~220 MB).
- LocalStack até 1 GB, simulador da SEFAZ 256 MB, Redis 64 MB.
- Tudo junto fica em ~2,7 GB, medido com a stack no ar.

Para limitar o WSL, crie `C:\Users\<você>\.wslconfig` com:

```ini
[wsl2]
memory=4GB
```

e rode `wsl --shutdown` antes de abrir o Docker Desktop de novo.

## Front

O compose já tem o serviço `front` (porta 5173), no perfil `front`, apontando para a imagem
`ghcr.io/dinastiadoge/tcc-front`. Enquanto essa imagem não existir, ele fica de fora. Quando existir:

```bash
docker compose --profile front up -d --wait
```

## Versões e imagens

| Imagem | Conteúdo |
|---|---|
| `ghcr.io/jaonotlitz/wc-servicos` | Os 9 serviços Java; a variável `SERVICO` escolhe qual roda |
| `ghcr.io/jaonotlitz/wc-localstack` | LocalStack 3.8.1 com os scripts de `infra/local/init` e os jars das Lambdas dentro |
| `ghcr.io/jaonotlitz/wc-sefaz-mock` | WireMock com os cenários de `infra/local/sefaz-mock` |

O workflow [`imagens`](../.github/workflows/imagens.yml) builda as três a cada push no `main` (amd64 e arm64,
então roda em Mac com chip Apple também), sobe este pacote, roda o smoke do gateway e os testes ponta a ponta
nele, e só então publica com as tags `latest` e o sha curto do commit. Para fixar uma versão, crie um `.env`
ao lado do compose com `WC_VERSAO=<sha>` (veja [`.env.exemplo`](.env.exemplo)).

Para buildar as imagens localmente (sem o GHCR):

```bash
for alvo in servicos localstack sefaz-mock; do
  docker build -f pacote/Dockerfile --target $alvo -t ghcr.io/jaonotlitz/wc-$alvo:latest .
done
```

## Problemas comuns

- **`docker pull` pede login (denied):** os pacotes no GHCR precisam estar públicos. Em
  github.com > seu perfil > Packages > `wc-servicos` (e os outros dois) > Package settings > Change visibility > Public.
- **LocalStack fica `unhealthy`:** algum script de init falhou; `docker logs wc-localstack` mostra qual.
  Na primeira subida ele baixa a imagem `public.ecr.aws/lambda/java:21` para rodar as Lambdas, então precisa de internet.
- **Porta ocupada:** o pacote usa 4566, 6379 e 8080 a 8089. Derrube o `make up` do repositório antes
  (`make down`), porque os dois usam os mesmos nomes de container.
