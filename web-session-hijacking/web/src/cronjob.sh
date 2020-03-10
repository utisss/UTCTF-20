#!/bin/bash

readonly REQUIRED_ENV_VARS=(
	"POSTGRES_USER"
	"PROBLEM_DB_USER"
	"PROBLEM_DB_PASSWORD"
	"POSTGRES_DB")

main() {
	clean_databases
}

clean_databases() {
	psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
		DELETE FROM messages WHERE msg_sent < current_timestamp - interval '3 minutes';
		DELETE FROM chat_ids WHERE last_used < current_timestamp - interval '3 minutes';
	EOSQL
}

while :
do
	main "$@"
	node monitorChats.js
	sleep 1m
done
