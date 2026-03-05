#!/bin/bash

echo "Import contractTemplates"
mongoimport --host localhost --db selcProduct --collection contractTemplates --file /docker-entrypoint-initdb.d/contractTemplates.json --jsonArray

echo "Inizializzazione completata!"
