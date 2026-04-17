#!/usr/bin/env python3

import os
from collections import Counter
from pymongo import MongoClient, UpdateOne
from pymongo.errors import BulkWriteError

MONGO_HOST = os.getenv("MONGO_HOST")
MONGO_BATCH_SIZE = 100
INSTITUTION_DB = "selcMsCore"
INSTITUTION_COLLECTION = "Institution"

class AnsiColors:
    ERROR = '\033[91m'
    ENDC = '\033[0m'

def bulkWrite(updateBatch, institutionCollection):
    if not updateBatch:
        return { "countInserted": 0, "countUpserted": 0, "countMatched": 0, "countModified": 0, "countRemoved": 0 }
    try:
        result = institutionCollection.bulk_write(updateBatch, ordered=False)
        return {
            "countInserted": result.inserted_count,
            "countUpserted": result.upserted_count,
            "countMatched": result.matched_count,
            "countModified": result.modified_count,
            "countRemoved": result.deleted_count
        }
    except BulkWriteError as bwe:
        result = bwe.details
        print(AnsiColors.ERROR, f"BulkWriteError {result}", AnsiColors.ENDC)
        return {
            "countInserted": result.get("nInserted", 0),
            "countUpserted": result.get("nUpserted", 0),
            "countMatched": result.get("nMatched", 0),
            "countModified": result.get("nModified", 0),
            "countRemoved": result.get("nRemoved", 0),
        }

def main():
    client = MongoClient(MONGO_HOST)
    institutionDB = client[INSTITUTION_DB]
    institutionCollection = institutionDB[INSTITUTION_COLLECTION]

    totalInstitutionCount = institutionCollection.count_documents({"institutionType": {"$exists": True}})
    print(f"Documents to modify: {totalInstitutionCount}")

    modifiedCount = 0
    bulkCounters = { "countInserted": 0, "countUpserted": 0, "countMatched": 0, "countModified": 0, "countRemoved": 0 }

    bufferBatch = []
    cursor = institutionCollection.find({"institutionType": {"$exists": True}},{"_id": 1}, batch_size=MONGO_BATCH_SIZE)
    for institution in cursor:
        bufferBatch.append(
            UpdateOne({"_id": institution["_id"]}, {"$unset": {"institutionType": ""}})
        )

        if len(bufferBatch) >= MONGO_BATCH_SIZE:
            result = bulkWrite(bufferBatch, institutionCollection)
            bufferBatch = []
            bulkCounters = dict(Counter(bulkCounters) + Counter(result))
            modifiedCount += result["countModified"]
            print(f"{modifiedCount} / {totalInstitutionCount} - {bulkCounters}")

    # Process any remaining documents
    if bufferBatch:
        result = bulkWrite(bufferBatch, institutionCollection)
        bulkCounters = dict(Counter(bulkCounters) + Counter(result))
        modifiedCount += result["countModified"]
        print(f"{modifiedCount} / {totalInstitutionCount} - {bulkCounters}")

    client.close()

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nInterrupted by user")
