#!/usr/bin/env python3

import os

from pymongo import MongoClient

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

def checkDelegationTypes(delegInstDoc):
    delegationId = delegInstDoc["_id"]
    if "productId" not in delegInstDoc:
        print(AnsiColors.WARNING, f"Delegation {delegationId} without productId", AnsiColors.ENDC)
        return None
    productId = delegInstDoc["productId"]
    delFromType = delegInstDoc["fromType"] if "fromType" in delegInstDoc else None
    delToType = delegInstDoc["toType"] if "toType" in delegInstDoc else None

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
    diff = []
    if delFromType != instFromType:
        diff.append("fromType")
    if delToType != instToType:
        diff.append("toType")
    if diff:
        print(AnsiColors.ERROR, f"Delegation {delegationId} with different {diff} fields", AnsiColors.ENDC)
        return False
    return True

def main():
    client = MongoClient(MONGO_HOST)
    db = client[INSTITUTION_DB]
    delegationsCollection = db[DELEGATIONS_COLLECTION]

    totalDelegationsCount = delegationsCollection.count_documents({})
    checkedDelegationsCount = 0
    skippedDelegationsCount = 0
    invalidDelegationsCount = 0
    validDelegationsCount = 0

    for d in getDelegationsAndInstitutions(delegationsCollection):
        check = checkDelegationTypes(d)
        if check is True:
            validDelegationsCount += 1
        elif check is False:
            invalidDelegationsCount += 1
        elif check is None:
            skippedDelegationsCount += 1
        checkedDelegationsCount += 1
        print(f"Checked {checkedDelegationsCount} / {totalDelegationsCount}", end="\r")

    print("")
    print(f"Valid Delegations: {validDelegationsCount} / {totalDelegationsCount}")
    print(f"Invalid Delegations: {invalidDelegationsCount} / {totalDelegationsCount}")
    print(f"Skipped Delegations (missing institution / onboarding / product): {skippedDelegationsCount} / {totalDelegationsCount}")
    client.close()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        pass
