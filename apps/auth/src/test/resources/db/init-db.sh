#!/bin/bash

echo "insert userInstitutions"
mongoimport --host localhost --db selcUser --collection userInstitutions --file /docker-entrypoint-initdb.d/userInstitutions.json --jsonArray

echo "insert Institutions"
mongoimport --host localhost --db selcMsCore --collection Institution --file /docker-entrypoint-initdb.d/institution.json --jsonArray

echo "insert otpFlows"
mongoimport --host localhost --db selcUser --collection otpFlows --file /docker-entrypoint-initdb.d/otpFlows.json --jsonArray

echo "Inizializzazione completata!"
