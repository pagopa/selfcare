#!/bin/bash

echo "insert userClaims"
mongoimport --host localhost --db testIam --collection userClaims --file /docker-entrypoint-initdb.d/userClaims.json --jsonArray

echo "insert roles"
mongoimport --host localhost --db testIam --collection roles --file /docker-entrypoint-initdb.d/roles.json --jsonArray

echo "Inizializzazione completata!"
