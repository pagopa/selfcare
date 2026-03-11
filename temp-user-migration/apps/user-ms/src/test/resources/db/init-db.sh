#!/bin/bash

echo "insert userInstitutions"
mongoimport --host localhost --db selcUser --collection userInstitutions --file /docker-entrypoint-initdb.d/userInstitutions.json --jsonArray

echo "insert userInfo"
mongoimport --host localhost --db selcUser --collection userInfo --file /docker-entrypoint-initdb.d/userInfo.json --jsonArray

echo "Inizializzazione completata!"
