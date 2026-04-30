#!/usr/bin/env python3

import os

from pymongo import MongoClient

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

def getInstitutionOnboardingTypes(institutionDoc):
    institutionOnboardingTypes = {}
    institutionId = institutionDoc["_id"]
    if "onboarding" not in institutionDoc:
        print(AnsiColors.WARNING, f"Institution {institutionId} without onboarding node", AnsiColors.ENDC)
        return {}
    for onboarding in institutionDoc["onboarding"]:
        if "tokenId" not in onboarding:
            print(AnsiColors.WARNING, f"Institution {institutionId} without tokenId in onboarding node", AnsiColors.ENDC)
            continue
        if "productId" not in onboarding:
            print(AnsiColors.WARNING, f"Institution {institutionId} without productId in onboarding node", AnsiColors.ENDC)
            continue
        tokenId = onboarding["tokenId"]
        productId = onboarding["productId"]
        institutionType = onboarding["institutionType"] if "institutionType" in onboarding else None
        origin = onboarding["origin"] if "origin" in onboarding else None
        originId = onboarding["originId"] if "originId" in onboarding else None
        institutionOnboardingTypes[(tokenId, productId)] = { "institutionType": institutionType, "origin": origin, "originId": originId }
    return institutionOnboardingTypes

def checkOnboardingTypes(onboardingsCollection, institutionOnboardingTypes):
    diffCounter = 0
    tokens = [k[0] for k in institutionOnboardingTypes.keys()]
    for onboarding in onboardingsCollection.find({"_id": {"$in": tokens}}):
        tokenId = onboarding["_id"]
        if "productId" not in onboarding:
            print(AnsiColors.WARNING, f"Onboarding {tokenId} without productId", AnsiColors.ENDC)
            continue
        productId = onboarding["productId"]
        testEnvProductIds = onboarding["testEnvProductIds"] if "testEnvProductIds" in onboarding else []
        if "institution" not in onboarding:
            print(AnsiColors.WARNING, f"Onboarding {tokenId} without institution node", AnsiColors.ENDC)
            continue
        institution = onboarding["institution"]
        if "id" not in institution:
            print(AnsiColors.WARNING, f"Onboarding {tokenId} without institutionId", AnsiColors.ENDC)
            continue
        institutionId = institution["id"]
        institutionType = institution["institutionType"] if "institutionType" in institution else None
        origin = institution["origin"] if "origin" in institution else None
        originId = institution["originId"] if "originId" in institution else None
        productsToCheck = [productId] + testEnvProductIds
        for p in productsToCheck:
            if (tokenId, p) not in institutionOnboardingTypes:
                print(AnsiColors.WARNING, f"Not found ({tokenId, p}): tokenId is associated to the right onboarding?", AnsiColors.ENDC)
                continue
            diff = []
            # institutionType
            if institutionType:
                if institutionType != institutionOnboardingTypes[(tokenId, p)]["institutionType"]:
                    diff.append("institutionType")
            else:
                print(AnsiColors.WARNING, f"Onboarding {tokenId} without institutionType", AnsiColors.ENDC)
            # origin
            if origin:
                if origin != institutionOnboardingTypes[(tokenId, p)]["origin"]:
                    diff.append("origin")
            else:
                print(AnsiColors.WARNING, f"Onboarding {tokenId} without origin", AnsiColors.ENDC)
            # originId
            if originId:
                if originId != institutionOnboardingTypes[(tokenId, p)]["originId"]:
                    diff.append("originId")
            else:
                print(AnsiColors.WARNING, f"Onboarding {tokenId} without originId", AnsiColors.ENDC)
            # diff
            if diff:
                print(AnsiColors.ERROR, f"Onboarding {tokenId} with institutionId {institutionId} and productId {p} with different {diff}", AnsiColors.ENDC)
                diffCounter += 1
    return diffCounter

def main():
    client = MongoClient(MONGO_HOST)
    institutionDB = client[INSTITUTION_DB]
    institutionCollection = institutionDB[INSTITUTION_COLLECTION]
    onboardingDB = client[ONBOARDING_DB]
    onboardingsCollection = onboardingDB[ONBOARDINGS_COLLECTION]

    totalInstitutionCount = institutionCollection.count_documents({})
    checkedInstitutionCount = 0
    totalOnboardingCount = 0
    invalidOnboardingCount = 0

    institutionOnboardingTypes = {}
    for institutionDoc in institutionCollection.find({}, batch_size=MONGO_BATCH_SIZE):
        institutionOnboardingTypes = institutionOnboardingTypes | getInstitutionOnboardingTypes(institutionDoc)
        totalOnboardingCount += len(institutionOnboardingTypes)
        if len(institutionOnboardingTypes) >= MONGO_BATCH_SIZE:
            invalidOnboardingCount += checkOnboardingTypes(onboardingsCollection, institutionOnboardingTypes)
            institutionOnboardingTypes = {}
        checkedInstitutionCount += 1
        print(f"Checked {checkedInstitutionCount} / {totalInstitutionCount}", end="\r")

    if len(institutionOnboardingTypes) > 0:
        invalidOnboardingCount += checkOnboardingTypes(onboardingsCollection, institutionOnboardingTypes)
        institutionOnboardingTypes = {}

    print("\n")
    print(f"Total Onboardings: {totalOnboardingCount}")
    print(f"Invalid Onboardings: {invalidOnboardingCount} / {totalOnboardingCount}")
    client.close()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        pass
