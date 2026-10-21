#!/bin/bash

echo "insert userInstitutions"
mongoimport --host localhost --db selcUser --collection userInstitutions --file /docker-entrypoint-initdb.d/userInstitutions.json --jsonArray

echo "insert UserGroups"
mongoimport --host localhost --db selcUserGroup --collection UserGroups --file /docker-entrypoint-initdb.d/userGroups.json --jsonArray

echo "Inizializzazione completata!"
