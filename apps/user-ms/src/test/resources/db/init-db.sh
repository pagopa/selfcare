#!/bin/bash

echo "insert userInstitutions"
mongoimport --host localhost --db selcUser --collection userInstitutions --file /docker-entrypoint-initdb.d/userInstitutions.json --jsonArray

echo "Inizializzazione completata!"
