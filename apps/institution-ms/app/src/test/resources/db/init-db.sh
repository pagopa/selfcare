#!/bin/bash

echo "insert Institutions"
mongoimport --host localhost --db selcMsCore --collection Institution --file /docker-entrypoint-initdb.d/institution.json --jsonArray

echo "insert Delegations"
mongoimport --host localhost --db selcMsCore --collection Delegations --file /docker-entrypoint-initdb.d/delegations.json --jsonArray

echo "Inizializzazione completata!"
