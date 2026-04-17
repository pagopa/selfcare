import math
import os
from dotenv import load_dotenv
from pymongo import MongoClient

from query import *

load_dotenv(dotenv_path=".env", override=True)
HOST = os.getenv('MONGO_HOST')

CORE_DB = 'selcMsCore'
INSTITUTION_COLLECTION = 'Institution'
PEC_NOTIFICATION_COLLECTION = 'PecNotification'

BATCH_SIZE = 100
START_PAGE = 0

def delete_pec_notificatio_for_pt(client):
    print("Starting process to delete PecNotification for PT")

    institutions_size_cursor = client[CORE_DB][INSTITUTION_COLLECTION].aggregate(count_pt_institutions())
    institutions_size = next(institutions_size_cursor)['count']
    print("Institutions size: " + str(institutions_size))
    pages = math.ceil(institutions_size / BATCH_SIZE)
    for page in range(START_PAGE, pages):
        print("Start page " + str(page + 1) + "/" + str(pages))

        institutions_pages = client[CORE_DB][INSTITUTION_COLLECTION].aggregate(
                get_pt_institutions(page, BATCH_SIZE))

        for institution in institutions_pages:
            institution_id = institution['_id']
            pec_notifications = client[CORE_DB][PEC_NOTIFICATION_COLLECTION].find({"institutionId": institution_id})

            for pec_notification in pec_notifications:
                pec_notification_id = pec_notification['_id']
                print(f"founded PecNotification with _id: {pec_notification_id} for institutionId: {institution_id}")
                client[CORE_DB][PEC_NOTIFICATION_COLLECTION].delete_one({"_id": pec_notification_id})
                print(f"Deleted PecNotification with _id: {pec_notification_id}")

    print("completed")

if __name__ == "__main__":
    client = MongoClient(HOST)
    delete_pec_notificatio_for_pt(client)
    client.close()