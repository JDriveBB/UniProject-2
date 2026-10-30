#!/bin/sh
set -eu

API="http://rabbitmq:15672/api"

for nombre in RABBITMQ_ADMIN_USER RABBITMQ_ADMIN_PASSWORD RABBITMQ_NUCLEO_PASSWORD RABBITMQ_AGENTE_PASSWORD; do
  eval "valor=\${$nombre:-}"
  case "$valor" in
    ''|*[!A-Za-z0-9]*) echo "ERROR: $nombre está vacía o tiene caracteres que no son letras o números" >&2; exit 1 ;;
  esac
done

llamar() {
  # $1 = método HTTP, $2 = ruta dentro de la API, $3 = cuerpo opcional
  if [ $# -ge 3 ]; then
    curl --silent --show-error --fail-with-body \
         -u "$RABBITMQ_ADMIN_USER:$RABBITMQ_ADMIN_PASSWORD" \
         -H "content-type: application/json" \
         -X "$1" "$API$2" --data-binary "$3"
  else
    curl --silent --show-error --fail-with-body \
         -u "$RABBITMQ_ADMIN_USER:$RABBITMQ_ADMIN_PASSWORD" \
         -H "content-type: application/json" \
         -X "$1" "$API$2"
  fi
}

# Paso 1: DELETE contenidos de colas
RESP1=$(llamar DELETE "/queues/%2F/notificaciones.telegram/contents") || {
  echo "FALLO paso 1: $RESP1"
  exit 1
}
RESP1_B=$(llamar DELETE "/queues/%2F/notificaciones.telegram.fallidos/contents") || {
  echo "FALLO paso 1: $RESP1_B"
  exit 1
}

# Paso 2: GET exchange uniproject.eventos
RESP2=$(llamar GET "/exchanges/%2F/uniproject.eventos") || {
  echo "FALLO paso 2: $RESP2"
  exit 1
}
if ! echo "$RESP2" | grep -q '"policy":"uniproject-eventos-alterno"'; then
  echo "FALLO paso 2: $RESP2"
  exit 1
fi

# Paso 3: GET queue notificaciones.telegram
RESP3=$(llamar GET "/queues/%2F/notificaciones.telegram") || {
  echo "FALLO paso 3: $RESP3"
  exit 1
}
if ! (echo "$RESP3" | grep -q '"type":"quorum"' && echo "$RESP3" | grep -q '"policy":"notificaciones-telegram-reintentos"'); then
  echo "FALLO paso 3: $RESP3"
  exit 1
fi

# Paso 4: POST publish evento
CUERPO_P4='{"properties":{"delivery_mode":2},"routing_key":"tarea.reasignada","payload":"prueba-fallidos","payload_encoding":"string"}'
RESP4=$(llamar POST "/exchanges/%2F/uniproject.eventos/publish" "$CUERPO_P4") || {
  echo "FALLO paso 4: $RESP4"
  exit 1
}
if ! echo "$RESP4" | grep -q '"routed":true'; then
  echo "FALLO paso 4: $RESP4"
  exit 1
fi

# Paso 5: POST get en notificaciones.telegram con reject_requeue_false
CUERPO_P5='{"count":1,"ackmode":"reject_requeue_false","encoding":"auto"}'
RESP5=$(llamar POST "/queues/%2F/notificaciones.telegram/get" "$CUERPO_P5") || {
  echo "FALLO paso 5: $RESP5"
  exit 1
}
if ! echo "$RESP5" | grep -q 'prueba-fallidos'; then
  echo "FALLO paso 5: $RESP5"
  exit 1
fi

# Paso 6: hasta 5 intentos en notificaciones.telegram.fallidos
CUERPO_P6='{"count":1,"ackmode":"ack_requeue_false","encoding":"auto"}'
HALLADO=0
RESP6=""
for i in 1 2 3 4 5; do
  RESP6=$(llamar POST "/queues/%2F/notificaciones.telegram.fallidos/get" "$CUERPO_P6") || true
  if echo "$RESP6" | grep -q 'prueba-fallidos'; then
    HALLADO=1
    break
  fi
  sleep 1
done

if [ "$HALLADO" -ne 1 ]; then
  echo "FALLO paso 6: $RESP6"
  exit 1
fi

# Paso 7: VERIFICACION OK
echo "VERIFICACION OK"

# Paso 8 (informativo, no hace exit 1)
CUERPO_P8_PUB='{"properties":{"delivery_mode":2},"routing_key":"tarea.reasignada","payload":"prueba-reintento","payload_encoding":"string"}'
llamar POST "/exchanges/%2F/uniproject.eventos/publish" "$CUERPO_P8_PUB" >/dev/null 2>&1 || true

CUERPO_P8_REJECT='{"count":1,"ackmode":"reject_requeue_true","encoding":"auto"}'
llamar POST "/queues/%2F/notificaciones.telegram/get" "$CUERPO_P8_REJECT" >/dev/null 2>&1 || true

CUERPO_P8_GET='{"count":1,"ackmode":"ack_requeue_false","encoding":"auto"}'
RESP8_INM=$(llamar POST "/queues/%2F/notificaciones.telegram/get" "$CUERPO_P8_GET") || true

sleep 12
RESP8_TAR=$(llamar POST "/queues/%2F/notificaciones.telegram/get" "$CUERPO_P8_GET") || true

if [ "$RESP8_INM" = "[]" ] && echo "$RESP8_TAR" | grep -q 'prueba-reintento'; then
  echo "REINTENTO DIFERIDO OK"
else
  echo "REINTENTO DIFERIDO NO OBSERVADO: inmediata=$RESP8_INM diferida=$RESP8_TAR"
fi

llamar DELETE "/queues/%2F/notificaciones.telegram/contents" >/dev/null 2>&1 || true

exit 0
