@echo off
rem Sobe a WC Autopecas com as imagens prontas (precisa do Docker Desktop aberto).
cd /d "%~dp0"
echo Baixando e subindo a WC Autopecas. Na primeira vez leva alguns minutos...
docker compose up -d --wait
if errorlevel 1 (
  echo.
  echo Algo nao subiu. Veja os logs com: docker compose logs --tail=100
  pause
  exit /b 1
)
echo.
echo Tudo no ar.
echo   API (gateway): http://localhost:4566/restapis/wcapi/local/_user_request_/api
echo   Login: admin@wcautopecas.com.br / admin123
pause
