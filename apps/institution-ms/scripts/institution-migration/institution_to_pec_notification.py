import math
import os
import time
from datetime import datetime
from dateutil.parser import parse
from dotenv import load_dotenv
from pymongo import MongoClient

from query import *

load_dotenv(dotenv_path=".env", override=True)
HOST = os.getenv('MONGO_HOST')

CORE_DB = 'selcMsCore'
INSTITUTION_COLLECTION = 'Institution'
USERS_DB = 'selcMsCore'
PEC_NOTIFICATION_COLLECTION = 'PecNotification'

epochDatePecNotification = os.getenv('EPOCH_DATE_PEC_NOTIFICATION')
sendingFrequencyPecNotification = int(os.getenv('SENDING_FREQUENCY_PEC_NOTIFICATION'))
#ex "productId1,productId2,productId3"
productIdsString = os.getenv('MIGRATE_PEC_PRODUCT_ID')
productIds = productIdsString.split(",")

BATCH_SIZE = 100
START_PAGE = 0
def institution_to_pec_notification(client):
    print("Starting process to create PecNotification")
    print("Products=" + productIdsString)

    module_day_of_the_epoch = calculate_module_day_of_the_epoch(
        epochDatePecNotification, datetime.now().isoformat(), sendingFrequencyPecNotification)
    print("Module day of the epoch: " + str(module_day_of_the_epoch))

    institutions_size_cursor = client[CORE_DB][INSTITUTION_COLLECTION].aggregate(count_institutions_with_active_onboarding(productIds))
    institutions_size = next(institutions_size_cursor)['count']
    print("Institutions size: " + str(institutions_size))
    pages = math.ceil(institutions_size / BATCH_SIZE)

    for page in range(START_PAGE, pages):
        print("Start page " + str(page + 1) + "/" + str(pages))

        institutions_pages = client[CORE_DB][INSTITUTION_COLLECTION].aggregate(
            get_institutions_with_active_onboarding(productIds, page, BATCH_SIZE)
        )

        for institution in institutions_pages:
            for onboarding in institution.get('onboarding', []):
                if onboarding.get('status') == 'ACTIVE' and onboarding["productId"] in productIds and institution.get("digitalAddress"):
                    pec_notification_document = {
                        "institutionId": institution["_id"],
                        "productId": onboarding["productId"],
                        "moduleDayOfTheEpoch": calculate_module_day_of_the_epoch(epochDatePecNotification, onboarding.get('createdAt'), sendingFrequencyPecNotification),
                        "digitalAddress": institution.get("digitalAddress")
                    }
                    client[USERS_DB][PEC_NOTIFICATION_COLLECTION].update_one(
                        {"institutionId": institution["_id"], "productId": onboarding["productId"]},
                        {"$set": pec_notification_document, "$setOnInsert": {"createdAt": datetime.now()}},
                        upsert=True
                    )
                    print("Create PecNotification for institution " + institution["_id"] + " and product " + onboarding["productId"])
        print("End page " + str(page + 1) + "/" + str(pages))
        time.sleep(15)

    print("Completed")

def calculate_module_day_of_the_epoch(epoch_start_str, current_date_str, sending_frequency):
    epoch_start = parse(epoch_start_str).date()
    current_date = parse(current_date_str).date()
    return (current_date - epoch_start).days % sending_frequency

if __name__ == "__main__":
    client = MongoClient(HOST)
    institution_to_pec_notification(client)
    client.close()