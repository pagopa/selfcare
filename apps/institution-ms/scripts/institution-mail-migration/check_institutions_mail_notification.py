#!/usr/bin/env python3

import os

from pymongo import MongoClient

MONGO_HOST = os.getenv("MONGO_HOST")
MONGO_BATCH_SIZE = 100
INSTITUTION_DB = "selcMsCore"
INSTITUTION_COLLECTION = "Institution"
PRODUCTS_WHITELIST = ["prod-interop", "prod-pagopa", "prod-io", "prod-pn", "prod-io-premium", "prod-io-sign"]

class AnsiColors:
    WARNING = '\033[93m'
    ERROR = '\033[91m'
    ENDC = '\033[0m'

def getInstitutionAndMailNotification(institutionCollection):
    pipeline = [
        {
            "$lookup": {
                "from": "MailNotification",
                "localField": "_id",
                "foreignField": "institutionId",
                "as": "mailNotificationDocs"
            }
        }
    ]
    return institutionCollection.aggregate(pipeline, batchSize=MONGO_BATCH_SIZE)

def checkMailNotification(institutionDoc):
    institutionId = institutionDoc["_id"]
    institutionType = institutionDoc["institutionType"] if "institutionType" in institutionDoc else ""
    mailNotificationDocs = institutionDoc["mailNotificationDocs"]
    if mailNotificationDocs and institutionType == "PT":
        print(AnsiColors.ERROR, "Anomaly for institutionId", institutionId, ": MailNotification present for an institution of type PT", AnsiColors.ENDC)
        return

    onboarding = institutionDoc["onboarding"] if "onboarding" in institutionDoc else []
    foundProducts = []
    for o in onboarding:
        status = o["status"]
        product = o["productId"]
        if product in PRODUCTS_WHITELIST and status == "ACTIVE":
            foundProducts.append(product)
    if mailNotificationDocs:
        mailNotification = mailNotificationDocs[0]
        productIds = mailNotification["productIds"]
        for p in foundProducts:
            if p not in productIds:
                print(AnsiColors.ERROR, "Anomaly for institutionId", institutionId, ": missing productId", p, "in MailNotification", AnsiColors.ENDC)
        if mailNotification["digitalAddress"] != institutionDoc["digitalAddress"]:
            print(AnsiColors.ERROR, "Anomaly for institutionId", institutionId, ": digitalAddress in MailNotification is different from the one in Institution", AnsiColors.ENDC)
    elif len(foundProducts) > 0 and institutionType != "PT":
        print(AnsiColors.ERROR, "Anomaly for institutionId", institutionId, ": MailNotification NOT FOUND for products", foundProducts, AnsiColors.ENDC)

def main():
    client = MongoClient(MONGO_HOST)
    db = client[INSTITUTION_DB]
    institutionCollection = db[INSTITUTION_COLLECTION]

    totalInstitutionCount = institutionCollection.count_documents({})
    checkedInstitutionCount = 0

    for institutionDoc in getInstitutionAndMailNotification(institutionCollection):
        checkMailNotification(institutionDoc)
        checkedInstitutionCount += 1
        print("Checked ", checkedInstitutionCount, "/", totalInstitutionCount, end="\r")

    print("\n")
    client.close()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        pass
