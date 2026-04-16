#!/usr/bin/env python3

import os

from pymongo import MongoClient, UpdateOne
from pymongo.errors import BulkWriteError

from collections import Counter

MONGO_HOST = os.getenv("MONGO_HOST")
MONGO_BATCH_SIZE = 100
INSTITUTION_DB = "selcMsCore"
INSTITUTION_COLLECTION = "Institution"
DELEGATIONS_COLLECTION = "Delegations"

class AnsiColors:
    WARNING = '\033[93m'
    ERROR = '\033[91m'
    ENDC = '\033[0m'

def getDelegationsAndInstitutions(delegationsCollection):
    pipeline = [
        {
            "$lookup": {
                "from": INSTITUTION_COLLECTION,
                "localField": "from",
                "foreignField": "_id",
                "as": "fromInst"
            }
        },
        {
            "$lookup": {
                "from": INSTITUTION_COLLECTION,
                "localField": "to",
                "foreignField": "_id",
                "as": "toInst"
            }
        },
        {
            "$unwind": {
                "path": "$fromInst",
                "preserveNullAndEmptyArrays": True
            }
        },
        {
            "$unwind": {
                "path": "$toInst",
                "preserveNullAndEmptyArrays": True
            }
        }
    ]
    return delegationsCollection.aggregate(pipeline, batchSize=MONGO_BATCH_SIZE)

def getDelegationTypesUpdate(delegInstDoc):
    delegationId = delegInstDoc["_id"]
    if "productId" not in delegInstDoc:
        print(AnsiColors.WARNING, f"Delegation {delegationId} without productId", AnsiColors.ENDC)
        return None
    productId = delegInstDoc["productId"]

    if not "fromInst" in delegInstDoc and not "toInst" in delegInstDoc:
        print(AnsiColors.WARNING, f"Delegation {delegationId} without relative fromInstitution and toInstitution", AnsiColors.ENDC)
        return None
    elif not "fromInst" in delegInstDoc:
        print(AnsiColors.WARNING, f"Delegation {delegationId} without relative fromInstitution", AnsiColors.ENDC)
        return None
    elif not "toInst" in delegInstDoc:
        print(AnsiColors.WARNING, f"Delegation {delegationId} without relative toInstitution", AnsiColors.ENDC)
        return None
    fromInst = delegInstDoc["fromInst"]
    toInst = delegInstDoc["toInst"]

    if not "onboarding" in fromInst and not "onboarding" in toInst:
        print(AnsiColors.WARNING, f"Delegation {delegationId} without relative onboarding", AnsiColors.ENDC)
        return None
    elif not "onboarding" in fromInst:
        print(AnsiColors.WARNING, f"Delegation {delegationId} without relative onboarding in fromInstitution", AnsiColors.ENDC)
        return None
    elif not "onboarding" in toInst:
        print(AnsiColors.WARNING, f"Delegation {delegationId} without relative onboarding in toInstitution", AnsiColors.ENDC)
        return None
    onboardingFrom = next((o for o in fromInst["onboarding"] if o["productId"] == productId and o["status"] not in ["REJECTED", "DELETED"]), None)
    onboardingTo = next((o for o in toInst["onboarding"] if o["productId"] == productId and o["status"] not in ["REJECTED", "DELETED"]), None)

    if not onboardingFrom and not onboardingTo:
        print(AnsiColors.WARNING, f"Delegation {delegationId} with productId {productId} without relative onboarding", AnsiColors.ENDC)
        return None
    elif not onboardingFrom:
        print(AnsiColors.WARNING, f"Delegation {delegationId} with productId {productId} without relative onboarding in fromInstitution", AnsiColors.ENDC)
        return None
    elif not onboardingTo:
        print(AnsiColors.WARNING, f"Delegation {delegationId} with productId {productId} without relative onboarding in toInstitution", AnsiColors.ENDC)
        return None
    instFromType = onboardingFrom["institutionType"] if "institutionType" in onboardingFrom else None
    instToType = onboardingTo["institutionType"] if "institutionType" in onboardingTo else None

    if not instFromType and not instToType:
        print(AnsiColors.WARNING, f"Delegation {delegationId} with productId {productId} without relative onboarding institutionType", AnsiColors.ENDC)
        return None
    elif not instFromType:
        print(AnsiColors.WARNING, f"Delegation {delegationId} with productId {productId} without relative onboarding institutionType in fromInstitution", AnsiColors.ENDC)
        return None
    elif not instToType:
        print(AnsiColors.WARNING, f"Delegation {delegationId} with productId {productId} without relative onboarding institutionType in toInstitution", AnsiColors.ENDC)
        return None

    return UpdateOne({"_id": delegationId}, {"$set": {"fromType": instFromType, "toType": instToType}})

def bulkWrite(updateBatch, delegationsCollection):
    try:
        result = delegationsCollection.bulk_write(updateBatch, ordered=False)
        return { "countInserted": result.inserted_count, "countUpserted": result.upserted_count, "countMatched": result.matched_count, "countModified": result.modified_count, "countRemoved": result.deleted_count }
    except BulkWriteError as bwe:
        result = bwe.details
        print(AnsiColors.ERROR, f"BulkWriteError {result}", AnsiColors.ENDC)
        return { "countInserted": result["nInserted"], "countUpserted": result["nUpserted"], "countMatched": result["nMatched"], "countModified": result["nModified"], "countRemoved": result["nRemoved"] }

def main():
    client = MongoClient(MONGO_HOST)
    db = client[INSTITUTION_DB]
    delegationsCollection = db[DELEGATIONS_COLLECTION]

    totalDelegationsCount = delegationsCollection.count_documents({})
    checkedDelegationsCount = 0
    bulkCounters = { "countInserted": 0, "countUpserted": 0, "countMatched": 0, "countModified": 0, "countRemoved": 0 }

    updateBatch = []
    for d in getDelegationsAndInstitutions(delegationsCollection):
        update = getDelegationTypesUpdate(d)
        checkedDelegationsCount += 1
        if update:
            updateBatch.append(update)
        if len(updateBatch) >= MONGO_BATCH_SIZE:
            result = bulkWrite(updateBatch, delegationsCollection)
            updateBatch = []
            bulkCounters = dict(Counter(bulkCounters) + Counter(result))
            print(f"{checkedDelegationsCount} / {totalDelegationsCount} - {bulkCounters}", end="\r")

    if len(updateBatch) > 0:
        result = bulkWrite(updateBatch, delegationsCollection)
        bulkCounters = dict(Counter(bulkCounters) + Counter(result))

    print(f"{checkedDelegationsCount} / {totalDelegationsCount} - {bulkCounters}")
    client.close()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        pass

