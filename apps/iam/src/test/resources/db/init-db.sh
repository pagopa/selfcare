#!/bin/bash

echo "insert userClaims"
mongoimport --host localhost --db selcIam --collection userClaims --file /docker-entrypoint-initdb.d/userClaims.json --jsonArray

echo "insert roles"
mongoimport --host localhost --db selcIam --collection roles --file /docker-entrypoint-initdb.d/roles.json --jsonArray

echo "insert institutions"
mongoimport --host localhost --db selcMsCore --collection Institution --file /docker-entrypoint-initdb.d/institution.json --jsonArray

echo "Inizializzazione completata!"
