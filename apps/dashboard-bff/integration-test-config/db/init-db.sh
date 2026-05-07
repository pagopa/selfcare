#!/bin/bash

#echo "insert Delegations"
#mongoimport --host localhost --db selcMsCore --collection Delegations --file /docker-entrypoint-initdb.d/Delegation.json --jsonArray

echo "insert Institutions"
mongoimport --host localhost --db selcMsCore --collection Institution --file /docker-entrypoint-initdb.d/institution.json --jsonArray

echo "insert Onboardings"
mongoimport --host localhost --db selcOnboarding --collection onboardings --file /docker-entrypoint-initdb.d/Onboardings.json --jsonArray

echo "insert Tokens"
mongoimport --host localhost --db selcOnboarding --collection tokens --file /docker-entrypoint-initdb.d/tokens.json --jsonArray

echo "insert UserGroups"
mongoimport --host localhost --db selcUserGroup --collection UserGroups --file /docker-entrypoint-initdb.d/UserGroups.json --jsonArray

echo "insert UserInfo"
mongoimport --host localhost --db selcUser --collection userInfo --file /docker-entrypoint-initdb.d/UserInfo.json --jsonArray

echo "insert UserInstitutions"
mongoimport --host localhost --db selcUser --collection userInstitutions --file /docker-entrypoint-initdb.d/UserInstitution.json --jsonArray

echo "insert userClaims"
mongoimport --host localhost --db selcIam --collection userClaims --file /docker-entrypoint-initdb.d/userClaims.json --jsonArray

echo "insert roles"
mongoimport --host localhost --db selcIam --collection roles --file /docker-entrypoint-initdb.d/roles.json --jsonArray

echo "insert documents"
mongoimport --host localhost --db selcDocument --collection documents --file /docker-entrypoint-initdb.d/documents.json --jsonArray

echo "Inizializzazione completata!"
