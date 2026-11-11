#!/bin/sh
# Sobe a WC Autopeças com as imagens prontas. Derrubar: docker compose --profile front down
cd "$(dirname "$0")" || exit 1
echo "Baixando e subindo a WC Autopeças. Na primeira vez leva alguns minutos..."
docker compose up -d --wait || { echo "Algo não subiu. Veja: docker compose logs --tail=100"; exit 1; }
echo
echo "Tudo no ar."
echo "  API (gateway): http://localhost:4566/restapis/wcapi/local/_user_request_/api"
echo "  Login: admin@wcautopecas.com.br / admin123"
