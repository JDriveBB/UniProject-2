#!/bin/sh
# Importa la topología y crea los usuarios de las aplicaciones con permisos mínimos.
# Imagen: curlimages/curl:8.22.0. Variables: RABBITMQ_ADMIN_USER, RABBITMQ_ADMIN_PASSWORD,
# RABBITMQ_NUCLEO_PASSWORD, RABBITMQ_AGENTE_PASSWORD.
set -eu

API="http://rabbitmq:15672/api"

for nombre in RABBITMQ_ADMIN_USER RABBITMQ_ADMIN_PASSWORD RABBITMQ_NUCLEO_PASSWORD RABBITMQ_AGENTE_PASSWORD; do
  eval "valor=\${$nombre:-}"
  case "$valor" in
    ''|*[!A-Za-z0-9]*) echo "ERROR: $nombre está vacía o tiene caracteres que no son letras o números" >&2; exit 1 ;;
  esac
done

llamar() {
  # $1 = método HTTP, $2 = ruta dentro de la API, $3 = cuerpo (JSON o @archivo)
  curl --silent --show-error --fail-with-body \
       --retry 10 --retry-connrefused --retry-delay 3 \
       -u "$RABBITMQ_ADMIN_USER:$RABBITMQ_ADMIN_PASSWORD" \
       -H "content-type: application/json" \
       -X "$1" "$API$2" --data-binary "$3"
  echo
}

echo "1/5 Importando topologia (definitions.json)"
llamar POST /definitions @/definiciones/definitions.json

echo "2/5 Usuario nucleo"
llamar PUT /users/nucleo "{\"password\":\"$RABBITMQ_NUCLEO_PASSWORD\",\"tags\":\"\"}"

echo "3/5 Permisos nucleo: solo publicar en uniproject.eventos"
llamar PUT /permissions/%2F/nucleo '{"configure":"^$","write":"^uniproject\\.eventos$","read":"^$"}'

echo "4/5 Usuario agente"
llamar PUT /users/agente "{\"password\":\"$RABBITMQ_AGENTE_PASSWORD\",\"tags\":\"\"}"

echo "5/5 Permisos agente: solo consumir notificaciones.telegram"
llamar PUT /permissions/%2F/agente '{"configure":"^$","write":"^$","read":"^notificaciones\\.telegram$"}'

echo "Topologia y usuarios listos"
