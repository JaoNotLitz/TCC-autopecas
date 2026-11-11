#!/bin/bash
# Lambdas de operação da fiscal-dlq (EPB-16): wc-dlq-monitor, agendada pelo EventBridge a cada
# minuto, e wc-dlq-redrive, invocada sob demanda (make redrive). Substituem o CloudWatch Alarm
# da seção 6.9, que o LocalStack gratuito não dispara para SNS de forma confiável.
source "$(dirname "$0")/00-common.sh"

JAR=/opt/wc/lambdas/wc-dlq-lambdas.jar
if [ ! -f "$JAR" ]; then
  log "jar das Lambdas da DLQ não encontrado em $JAR; pulando"; exit 0
fi
if ! awslocal lambda list-functions >/dev/null 2>&1; then
  log "Lambda desabilitado neste LocalStack; pulando"; exit 0
fi
REGION=$AWS_DEFAULT_REGION
ACCOUNT=000000000000
AMBIENTE="Variables={QUEUE_FISCAL_DLQ=$QUEUE_FISCAL_DLQ,TOPIC_SYSTEM_NOTIFICATIONS=$TOPIC_SYSTEM_NOTIFICATIONS,TABLE_COUNTERS=$TABLE_COUNTERS,DLQ_LIMITE=${DLQ_LIMITE:-5}}"

publica() { # publica <função> <classe do handler>
  awslocal lambda delete-function --function-name "$1" >/dev/null 2>&1 || true
  awslocal lambda create-function --function-name "$1" --runtime java21 --memory-size 512 --timeout 60 \
    --handler "$2::handleRequest" --role "arn:aws:iam::$ACCOUNT:role/wc-lambda-role" \
    --zip-file "fileb://$JAR" --environment "$AMBIENTE" >/dev/null
  awslocal lambda wait function-active-v2 --function-name "$1"
  log "lambda $1 ok"
}
publica wc-dlq-monitor br.com.wcautopecas.dlq.DlqMonitorHandler
publica wc-dlq-redrive br.com.wcautopecas.dlq.DlqRedriveHandler

REGRA=wc-dlq-monitor-cada-minuto
ARN_REGRA=$(awslocal events put-rule --name "$REGRA" --schedule-expression "rate(1 minute)" \
  --query RuleArn --output text)
awslocal lambda add-permission --function-name wc-dlq-monitor --statement-id eventbridge \
  --action lambda:InvokeFunction --principal events.amazonaws.com --source-arn "$ARN_REGRA" >/dev/null 2>&1 || true
awslocal events put-targets --rule "$REGRA" \
  --targets "Id=wc-dlq-monitor,Arn=arn:aws:lambda:$REGION:$ACCOUNT:function:wc-dlq-monitor" >/dev/null
log "regra $REGRA -> wc-dlq-monitor ok"
