#!/usr/bin/env python3

import os
from pymongo import MongoClient

MONGO_HOST = os.getenv("MONGO_HOST")
MONGO_BATCH_SIZE = 100
INSTITUTION_DB = "selcMsCore"
INSTITUTION_COLLECTION = "Institution"

def main():
    client = MongoClient(MONGO_HOST)
    institutionDB = client[INSTITUTION_DB]
    institutionCollection = institutionDB[INSTITUTION_COLLECTION]

    totalInstitutionCount = institutionCollection.count_documents({})
    checkedInstitutionCount = 0
    violations = 0

    print(f"Scanning {totalInstitutionCount} documents...\n")

    for institution in institutionCollection.find({}, batch_size=MONGO_BATCH_SIZE):
        checkedInstitutionCount += 1

        if "institutionType" in institution:
            print(f"[FOUND] Institution {institution['_id']} contains field institutionType")
            violations += 1

        if checkedInstitutionCount % 1000 == 0:
            print(f"Checked {checkedInstitutionCount}/{totalInstitutionCount}...")

    print("\n=== Scan completed ===")
    print(f"Checked documents: {checkedInstitutionCount}")
    print(f"Documents with unwanted field 'institutionType': {violations}")

    client.close()

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nInterrupted by user")
