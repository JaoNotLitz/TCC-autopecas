@echo off
rem Derruba a WC Autopecas (os dados do LocalStack e do Redis voltam zerados na proxima subida).
cd /d "%~dp0"
docker compose --profile front down
pause
