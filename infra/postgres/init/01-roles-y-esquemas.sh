#!/bin/bash
# Roles de las aplicaciones y esquema del agente (decisiones-tecnicas-java §5).
# Solo se ejecuta cuando el volumen de datos está vacío. Para repetirlo: dc down -v
set -euo pipefail
: "${NUCLEO_DB_PASSWORD:?falta NUCLEO_DB_PASSWORD}"
: "${AGENTE_DB_PASSWORD:?falta AGENTE_DB_PASSWORD}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v nucleo_pass="$NUCLEO_DB_PASSWORD" \
     -v agente_pass="$AGENTE_DB_PASSWORD" <<'EOSQL'
CREATE ROLE uniproject_nucleo LOGIN PASSWORD :'nucleo_pass';
CREATE ROLE uniproject_agente LOGIN PASSWORD :'agente_pass';

-- El núcleo es dueño de la base y, por tanto, de public (pg_database_owner).
ALTER DATABASE uniproject OWNER TO uniproject_nucleo;
REVOKE ALL ON DATABASE uniproject FROM PUBLIC;
GRANT CONNECT ON DATABASE uniproject TO uniproject_agente;

-- Nadie fuera del dueño usa public.
REVOKE ALL ON SCHEMA public FROM PUBLIC;

-- Esquema propio del agente.
CREATE SCHEMA agente AUTHORIZATION uniproject_agente;
ALTER ROLE uniproject_agente SET search_path = agente;
EOSQL
