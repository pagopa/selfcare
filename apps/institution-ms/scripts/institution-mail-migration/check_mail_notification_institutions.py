#!/usr/bin/env python3

import os

from pymongo import MongoClient

MONGO_HOST = os.getenv("MONGO_HOST")
MONGO_BATCH_SIZE = 100
INSTITUTION_DB = "selcMsCore"
INSTITUTION_COLLECTION = "Institution"
MAIL_NOTIFICATION_COLLECTION = "MailNotification"
PRODUCTS_WHITELIST = ["prod-interop", "prod-pagopa", "prod-io", "prod-pn", "prod-io-premium", "prod-io-sign"]

class AnsiColors:
    WARNING = '\033[93m'
    ERROR = '\033[91m'
    ENDC = '\033[0m'

def getMailNotificationAndInstitution(mailNotificationCollection):
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
    return mailNotificationCollection.aggregate(pipeline, batchSize=MONGO_BATCH_SIZE)

def checkDigitalAddress(mailNotificationDoc, institutionDoc):
    institutionId = mailNotificationDoc["institutionId"]
    if "digitalAddress" not in mailNotificationDoc:
        print(AnsiColors.ERROR, "Anomaly in MailNotification for institutionId", institutionId, ": digitalAddress NOT FOUND in MailNotification", AnsiColors.ENDC)
    elif "digitalAddress" not in institutionDoc:
        print(AnsiColors.ERROR, "Anomaly in MailNotification for institutionId", institutionId, ": digitalAddress NOT FOUND in Institution", AnsiColors.ENDC)
    elif mailNotificationDoc["digitalAddress"] != institutionDoc["digitalAddress"]:
        print(AnsiColors.ERROR, "Anomaly in MailNotification for institutionId", institutionId, ": digitalAddress doesn't match the one in Institution", AnsiColors.ENDC)

def checkProductIds(mailNotificationDoc, institutionDoc):
    institutionId = mailNotificationDoc["institutionId"]
    productIds = mailNotificationDoc["productIds"]
    onboarding = institutionDoc["onboarding"]
    foundProducts = []
    for o in onboarding:
        status = o["status"]
        product = o["productId"]
        if status == "ACTIVE":
            foundProducts.append(product)
            if product in PRODUCTS_WHITELIST and product not in productIds:
                print(AnsiColors.ERROR, "Anomaly in MailNotification for institutionId", institutionId, ": productId", product, "not found in MailNotification", AnsiColors.ENDC)
    for p in productIds:
        if p not in foundProducts:
            print(AnsiColors.ERROR, "Anomaly in MailNotification for institutionId", institutionId, ": productId", p, "not found in institution", foundProducts, AnsiColors.ENDC)

def main():
    client = MongoClient(MONGO_HOST)
    db = client[INSTITUTION_DB]
    mailNotificationCollection = db[MAIL_NOTIFICATION_COLLECTION]
    institutionCollection = db[INSTITUTION_COLLECTION]

    totalMailNotificationCount = mailNotificationCollection.count_documents({})
    checkedMailNotificationCount = 0

    for mailNotificationDoc in getMailNotificationAndInstitution(mailNotificationCollection):
        institutionId = mailNotificationDoc["institutionId"]
        institutionDocs = mailNotificationDoc["institutionDocs"]
        if institutionDocs:
            institutionDoc = institutionDocs[0]
            checkDigitalAddress(mailNotificationDoc, institutionDoc)
            checkProductIds(mailNotificationDoc, institutionDoc)
        else:
            print(AnsiColors.ERROR, "Anomaly in MailNotification for institutionId", institutionId, ": institutionId NOT FOUND in Institution", AnsiColors.ENDC)
        checkedMailNotificationCount += 1
        print("Checked ", checkedMailNotificationCount, "/", totalMailNotificationCount, end="\r")

    print("\n")
    client.close()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        pass
