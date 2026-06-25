#!/usr/bin/env python3

import os

from pymongo import MongoClient, UpdateOne
from pymongo.errors import BulkWriteError

from collections import Counter

MONGO_HOST = os.getenv("MONGO_HOST")
MONGO_BATCH_SIZE = 100
INSTITUTION_DB = "selcMsCore"
INSTITUTION_COLLECTION = "Institution"
ONBOARDING_DB = "selcOnboarding"
ONBOARDINGS_COLLECTION = "onboardings"

class AnsiColors:
    WARNING = '\033[93m'
    ERROR = '\033[91m'
    ENDC = '\033[0m'

def getTokens(institutionDoc):
    tokens = {}
    institutionId = institutionDoc["_id"]
    if "onboarding" not in institutionDoc:
        print(AnsiColors.WARNING, f"Institution {institutionId} without onboarding node", AnsiColors.ENDC)
        return tokens
    for onboarding in institutionDoc["onboarding"]:
        if "tokenId" not in onboarding:
            print(AnsiColors.WARNING, f"Institution {institutionId} without tokenId in onboarding node", AnsiColors.ENDC)
            continue
        tokenId = onboarding["tokenId"]
        tokens[tokenId] = tokenId
    return tokens

def getInstitutionTypesUpdateBatch(tokens, onboardingsCollection):
    updateBatch = []
    for onboarding in onboardingsCollection.find({"_id": {"$in": list(tokens)}}):
        tokenId = onboarding["_id"]
        if "productId" not in onboarding:
            print(AnsiColors.WARNING, f"Onboarding {tokenId} without productId", AnsiColors.ENDC)
            continue
        if "institution" not in onboarding:
            print(AnsiColors.WARNING, f"Onboarding {tokenId} without institution node", AnsiColors.ENDC)
            continue
        productId = onboarding["productId"]
        institution = onboarding["institution"]
        if "id" not in institution:
            print(AnsiColors.WARNING, f"Onboarding {tokenId} without institutionId", AnsiColors.ENDC)
            continue
        institutionId = institution["id"]
        institutionType = institution["institutionType"] if "institutionType" in institution else None
        origin = institution["origin"] if "origin" in institution else None
        originId = institution["originId"] if "originId" in institution else None
        fieldsToSet = {}
        # institutionType
        if institutionType:
            fieldsToSet["onboarding.$[elem].institutionType"] = institutionType
        else:
            print(AnsiColors.WARNING, f"Onboarding {tokenId} without institutionType", AnsiColors.ENDC)
        # origin
        if origin:
            fieldsToSet["onboarding.$[elem].origin"] = origin
        else:
            print(AnsiColors.WARNING, f"Onboarding {tokenId} without origin", AnsiColors.ENDC)
        # originId
        if originId:
            fieldsToSet["onboarding.$[elem].originId"] = originId
        else:
            print(AnsiColors.WARNING, f"Onboarding {tokenId} without originId", AnsiColors.ENDC)
        # add update operation
        updateBatch.append(UpdateOne({"_id": institutionId}, {"$set": fieldsToSet}, array_filters=[{"elem.productId": productId, "elem.tokenId": tokenId}]))
        # add update operations for every testEnvProductIds
        testEnvProductIds = onboarding["testEnvProductIds"] if "testEnvProductIds" in onboarding else []
        for testProductId in testEnvProductIds:
            updateBatch.append(UpdateOne({"_id": institutionId}, {"$set": fieldsToSet}, array_filters=[{"elem.productId": testProductId, "elem.tokenId": tokenId}]))
    return updateBatch

def bulkWrite(updateBatch, institutionCollection):
    if not updateBatch:
        return { "countInserted": 0, "countUpserted": 0, "countMatched": 0, "countModified": 0, "countRemoved": 0 }
    try:
        result = institutionCollection.bulk_write(updateBatch, ordered=False)
        return { "countInserted": result.inserted_count, "countUpserted": result.upserted_count, "countMatched": result.matched_count, "countModified": result.modified_count, "countRemoved": result.deleted_count }
    except BulkWriteError as bwe:
        result = bwe.details
        print(AnsiColors.ERROR, f"BulkWriteError {result}", AnsiColors.ENDC)
        return { "countInserted": result["nInserted"], "countUpserted": result["nUpserted"], "countMatched": result["nMatched"], "countModified": result["nModified"], "countRemoved": result["nRemoved"] }

def main():
    client = MongoClient(MONGO_HOST)
    institutionDB = client[INSTITUTION_DB]
    institutionCollection = institutionDB[INSTITUTION_COLLECTION]
    onboardingDB = client[ONBOARDING_DB]
    onboardingsCollection = onboardingDB[ONBOARDINGS_COLLECTION]

    totalInstitutionCount = institutionCollection.count_documents({})
    checkedInstitutionCount = 0
    bulkCounters = { "countInserted": 0, "countUpserted": 0, "countMatched": 0, "countModified": 0, "countRemoved": 0 }

    tokens = {}
    for institutionDoc in institutionCollection.find({}, batch_size=MONGO_BATCH_SIZE):
        tokens = tokens | getTokens(institutionDoc)
        checkedInstitutionCount += 1
        if len(tokens) >= MONGO_BATCH_SIZE:
            updateBatch = getInstitutionTypesUpdateBatch(tokens, onboardingsCollection)
            result = bulkWrite(updateBatch, institutionCollection)
            tokens = {}
            bulkCounters = dict(Counter(bulkCounters) + Counter(result))
            print(f"{checkedInstitutionCount} / {totalInstitutionCount} - {bulkCounters}", end="\r")

    if len(tokens) > 0:
        updateBatch = getInstitutionTypesUpdateBatch(tokens, onboardingsCollection)
        result = bulkWrite(updateBatch, institutionCollection)
        tokens = {}
        bulkCounters = dict(Counter(bulkCounters) + Counter(result))

    print(f"{checkedInstitutionCount} / {totalInstitutionCount} - {bulkCounters}")
    client.close()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        pass
