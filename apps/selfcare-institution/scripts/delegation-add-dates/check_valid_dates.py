#!/usr/bin/env python3

import os
import re
from pymongo import MongoClient

MONGO_HOST = os.getenv("MONGO_HOST")
DB_NAME = "selcMsCore"
COLLECTION_NAME = "Delegations"
BATCH_SIZE = 100

# Regex ISO 8601 con microsecondi opzionali e supporto per Z o offset
ISO8601_REGEX = re.compile(
    r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,9})?(Z|[+-]\d{2}:\d{2})$"
)

def is_valid_iso8601(value):
    return isinstance(value, str) and ISO8601_REGEX.match(value)

def main():
    client = MongoClient(MONGO_HOST)
    db = client[DB_NAME]
    collection = db[COLLECTION_NAME]

    total = 0
    valid = 0
    missing = 0
    malformed = 0

    cursor = collection.find({}, {"_id": 1, "createdAt": 1}).batch_size(BATCH_SIZE)

    for doc in cursor:
        total += 1
        doc_id = doc["_id"]
        created_at = doc.get("createdAt")

        if created_at is None:
            missing += 1
            print(f"\nDocument without 'createdAt': {doc_id}")
        elif not is_valid_iso8601(created_at):
            malformed += 1
            print(f"\nDocument with invalid 'createdAt': {doc_id}")
        else:
            valid += 1

    print(f"\n--- Check completed ---")
    print(f"Total documents: {total}")
    print(f"Valid dates: {valid}")
    print(f"Missing dates: {missing}")
    print(f"Invalid dates: {malformed}")

    client.close()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\n Manual interruption detected. Exiting.")
