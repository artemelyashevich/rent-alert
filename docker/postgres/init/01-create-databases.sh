#!/bin/bash

set -euo pipefail

create_service_db() {
    local db="$1" user="$2" password="$3"

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
        --set=db="$db" --set=user="$user" --set=password="$password" <<-'EOSQL'
		SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'user', :'password')
		WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'user')\gexec

		SELECT format('CREATE DATABASE %I OWNER %I', :'db', :'user')
		WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'db')\gexec

		-- only the owner (and the admin) may connect: services can't read each other's data
		SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'db')\gexec
	EOSQL
}

create_service_db identity "$IDENTITY_DB_USER" "$IDENTITY_DB_PASSWORD"
create_service_db notification "$NOTIFICATION_DB_USER" "$NOTIFICATION_DB_PASSWORD"
