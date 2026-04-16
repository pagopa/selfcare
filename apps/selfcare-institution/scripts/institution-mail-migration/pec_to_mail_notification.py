#!/usr/bin/env python3

import os
import sys

from datetime import datetime, UTC
from pymongo import MongoClient

MONGO_HOST = os.getenv("MONGO_HOST")
MONGO_BATCH_SIZE = 100
INSTITUTION_DB = "selcMsCore"
PEC_NOTIFICATION_COLLECTION = "PecNotification"
MAIL_NOTIFICATION_COLLECTION = "MailNotification"

class AnsiColors:
    WARNING = '\033[93m'
    ERROR = '\033[91m'
    ENDC = '\033[0m'

def getPecNotificationMissingFields(document):
    requiredFields = {"productId", "digitalAddress", "institutionId", "moduleDayOfTheEpoch", "createdAt"}
    return requiredFields - document.keys()

def getPecNotificationGroupsByInstitutionId(pecNotificationCollection):
    pecNotificationGroupsPipeline = [
        {
            '$group': {
                '_id': '$institutionId', 
                'documents': {
                    '$push': '$$ROOT'
                }
            }
        },
        {
            '$project': {
                '_id': 1, 
                'documents': {
                    '$sortArray': {
                        'input': '$documents', 
                        'sortBy': {
                            'createdAt': 1
                        }
                    }
                }
            }
        }
    ]
    return pecNotificationCollection.aggregate(pecNotificationGroupsPipeline, batchSize=MONGO_BATCH_SIZE)

def upsertMailNotification(institutionId, productIds, digitalAddress, moduleDayOfTheEpoch, createdAt, mailNotificationCollection):
    query = {"institutionId": institutionId}
    update = {
        "$addToSet": {"productIds": {"$each": list(productIds)}},
        "$set": {
            "moduleDayOfTheEpoch": moduleDayOfTheEpoch,
            "digitalAddress": digitalAddress,
            "updatedAt": datetime.now(UTC)
        },
        "$setOnInsert": {
            "createdAt": createdAt,
            "institutionId": institutionId,
        }
    }
    mailNotificationCollection.update_one(query, update, upsert=True)

def main(dryRun = False):
    client = MongoClient(MONGO_HOST)
    db = client[INSTITUTION_DB]
    pecNotificationCollection = db[PEC_NOTIFICATION_COLLECTION]
    mailNotificationCollection = db[MAIL_NOTIFICATION_COLLECTION]

    totalPecNotificationCount = pecNotificationCollection.count_documents({})
    processedPecNotificationCount = 0
    migratedPecNotificationCount = 0
    skippedPecNotificationCount = 0

    processedGroupCount = 0
    upsertedGroupCount = 0
    skippedGroupCount = 0

    for group in getPecNotificationGroupsByInstitutionId(pecNotificationCollection):
        institutionId = group["_id"]
        productIds = set()
        digitalAddress = ""
        anomalies = False

        for document in group["documents"]:
            pecNotificationId = document["_id"]
            missingFields = getPecNotificationMissingFields(document)
            if missingFields:
                print(AnsiColors.WARNING, "Anomaly on PecNotification with id", pecNotificationId, ": missing fields", missingFields, AnsiColors.ENDC)
                anomalies = True
            else:
                if document["productId"] not in productIds:
                    productIds.add(document["productId"])
                else:
                    print(AnsiColors.WARNING, "Anomaly on PecNotification with id", pecNotificationId, ": duplicated productId", document["productId"], AnsiColors.ENDC)
                    anomalies = True
                if digitalAddress == "":
                    digitalAddress = document["digitalAddress"]
                elif digitalAddress != document["digitalAddress"]:
                    print(AnsiColors.WARNING, "Anomaly on PecNotification with id", pecNotificationId, ": multiple digital addresses", document["digitalAddress"], AnsiColors.ENDC)
                    anomalies = True
            processedPecNotificationCount += 1

        if not anomalies:
            # The documents are ordered by createdAt (oldest first) --> We take the module from the first document
            moduleDayOfTheEpoch = group["documents"][0]["moduleDayOfTheEpoch"]
            createdAt = group["documents"][0]["createdAt"]
            if not dryRun:
                try:
                    upsertMailNotification(institutionId, productIds, digitalAddress, moduleDayOfTheEpoch, createdAt, mailNotificationCollection)
                except Exception as ex:
                    print(AnsiColors.ERROR, "Error upserting MailNotification with institutionId", institutionId, ":", ex, AnsiColors.ENDC)
            upsertedGroupCount += 1
            migratedPecNotificationCount += len(group["documents"])
        else:
            print(AnsiColors.ERROR, "Skipping migration of PecNotification group with institutionId", institutionId, ": anomalies detected", AnsiColors.ENDC)
            skippedGroupCount += 1
            skippedPecNotificationCount += len(group["documents"])

        processedGroupCount += 1
        print("PecNotification processed:", processedPecNotificationCount, "/", totalPecNotificationCount, end="\r")

    print("")
    print("PecNotification migrated:", migratedPecNotificationCount)
    print("PecNotification skipped:", skippedPecNotificationCount)
    print("MailNotification groups processed:", processedGroupCount)
    print("MailNotification groups upserted:", upsertedGroupCount)
    print("MailNotification groups skipped:", skippedGroupCount)
    client.close()

if __name__ == '__main__':
    try:
        main(len(sys.argv) > 1 and sys.argv[1] == "dryRun")
    except KeyboardInterrupt:
        pass
