#!/usr/bin/env python3

import os

from pymongo import MongoClient

MONGO_HOST = os.getenv("MONGO_HOST")
MONGO_BATCH_SIZE = 100
INSTITUTION_DB = "selcMsCore"
INSTITUTION_COLLECTION = "Institution"
PEC_NOTIFICATION_COLLECTION = "PecNotification"

class AnsiColors:
    WARNING = '\033[93m'
    ERROR = '\033[91m'
    ENDC = '\033[0m'

def getPecNotificationAndInstitution(pecNotificationCollection):
    pipeline = [
        {
            "$lookup": {
                "from": "Institution",
                "localField": "institutionId",
                "foreignField": "_id",
                "as": "institutionDocs"
            }
        }
    ]
    return pecNotificationCollection.aggregate(pipeline, batchSize=MONGO_BATCH_SIZE)

def checkDigitalAddress(pecNotificationDoc, institutionDoc):
    institutionId = pecNotificationDoc["institutionId"]
    productId = pecNotificationDoc["productId"]
    if "digitalAddress" not in pecNotificationDoc:
        print(AnsiColors.ERROR, "Anomaly in PecNotification with institutionId", institutionId, "and productId", productId, ": digitalAddress NOT FOUND in PecNotification", AnsiColors.ENDC)
    elif "digitalAddress" not in institutionDoc:
        print(AnsiColors.ERROR, "Anomaly in PecNotification with institutionId", institutionId, "and productId", productId, ": digitalAddress NOT FOUND in Institution", AnsiColors.ENDC)
    elif pecNotificationDoc["digitalAddress"] != institutionDoc["digitalAddress"]:
        print(AnsiColors.ERROR, "Anomaly in PecNotification with institutionId", institutionId, "and productId", productId, ": digitalAddress doesn't match the one in Institution", AnsiColors.ENDC)

def checkProductId(pecNotificationDoc, institutionDoc):
    institutionId = pecNotificationDoc["institutionId"]
    productId = pecNotificationDoc["productId"]
    onboarding = institutionDoc["onboarding"]
    foundOnboardingForProductId = False
    for o in onboarding:
        status = o["status"]
        product = o["productId"]
        if product == productId and status == "ACTIVE":
            if not foundOnboardingForProductId:
                foundOnboardingForProductId = True
            else:
                print(AnsiColors.ERROR, "Anomaly in PecNotification for institutionId", institutionId, "and productId", productId, ": multiple onboarding for same product in Institution", AnsiColors.ENDC)
    if not foundOnboardingForProductId:
        print(AnsiColors.ERROR, "Anomaly in PecNotification for institutionId", institutionId, "and productId", productId, ": onboarding NOT FOUND in Institution", AnsiColors.ENDC)

def main():
    client = MongoClient(MONGO_HOST)
    db = client[INSTITUTION_DB]
    pecNotificationCollection = db[PEC_NOTIFICATION_COLLECTION]
    institutionCollection = db[INSTITUTION_COLLECTION]

    totalPecNotificationCount = pecNotificationCollection.count_documents({})
    checkedPecNotificationCount = 0

    for pecNotificationDoc in getPecNotificationAndInstitution(pecNotificationCollection):
        institutionId = pecNotificationDoc["institutionId"]
        productId = pecNotificationDoc["productId"]
        institutionDocs = pecNotificationDoc["institutionDocs"]
        if institutionDocs:
            institutionDoc = institutionDocs[0]
            checkDigitalAddress(pecNotificationDoc, institutionDoc)
            checkProductId(pecNotificationDoc, institutionDoc)
        else:
            print(AnsiColors.ERROR, "Anomaly in PecNotification for institutionId", institutionId, "and productId", productId, ": institutionId NOT FOUND in Institution", AnsiColors.ENDC)
        checkedPecNotificationCount += 1
        print("Checked ", checkedPecNotificationCount, "/", totalPecNotificationCount, end="\r")

    print("\n")
    client.close()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        pass
