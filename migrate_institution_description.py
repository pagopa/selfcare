import math
import os
from dotenv import load_dotenv
import sys
import time
from pymongo import MongoClient, WriteConcern

from query import *

load_dotenv()
HOST =  os.getenv('HOST')

CORE_DB = 'selcMsCore'
INSTITUTION_COLLECTION = 'Institution'

USERS_DB = 'selcUser'
USER_INSTITUTION_COLLECTION = 'userInstitutions'

ONBOARDING_DB = 'selcOnboarding'
ONBOARDINGS_COLLECTION = 'onboardings'

BATCH_SIZE = 100
START_PAGE = 0
INSTITUTION_ID = None if len(sys.argv) < 2 else sys.argv[1]

def migrate_institution_description_from_onboarding_to_core(client):
    if INSTITUTION_ID is None:
        institutions_size_cursor = client[CORE_DB][INSTITUTION_COLLECTION].aggregate(count_institutions_whithout_description())
        institutions_size = next(institutions_size_cursor)['count']
        print("Institutions size: " + str(institutions_size))
        pages = math.ceil(institutions_size / BATCH_SIZE)

        for page in range(START_PAGE, pages):
            print("Start page " + str(page + 1) + "/" + str(pages))

            institutions_pages = client[CORE_DB][INSTITUTION_COLLECTION].aggregate(
                get_institutions_whithout_description(page, BATCH_SIZE)
            )


            print("Institutions size: " + str(institutions_pages))

            for institution in institutions_pages:
                institution_onboarding = client[ONBOARDING_DB][ONBOARDINGS_COLLECTION].find_one(
                    {"_id": institution["onboarding"][0]["tokenId"]}
                )

                print(institution_onboarding)
                if institution_onboarding is not None and institution_onboarding['institution']['description'] is not None and institution_onboarding['institution']['description'] != '':
                
                    institution_desc = institution_onboarding['institution']['description']
                    institution_id = institution_onboarding['institution']['id']
                    print("retrieved institution id " + institution_id + " with description: " + institution_desc)

                    client[CORE_DB][INSTITUTION_COLLECTION].update_one(
                        {"_id": institution_id},
                        {"$set": {"description": institution_desc}},
                        False)
                    
                    client[USERS_DB][USER_INSTITUTION_COLLECTION].update_many(
                        {"institutionId": institution_id},
                        {"$set": {"institutionDescription": institution_desc}},
                        False)

            print("End page " + str(page + 1) + "/" + str(pages))
            time.sleep(15)

        return True
    else:
        onboarding = client[ONBOARDING_DB][ONBOARDINGS_COLLECTION].find_one(
            get_onboarding(INSTITUTION_ID)
        )

        if onboarding is not None:
            institution_id = onboarding['institution']['id']
            institution_desc = onboarding['institution']['description']

            client[CORE_DB][INSTITUTION_COLLECTION].update_one(
                {"_id": institution_id},
                {"$set": {"description": institution_desc}},
                False)
                
            client[USERS_DB][USER_INSTITUTION_COLLECTION].update_many(
                {"institutionId": institution_id},
                {"$set": {"institutionDescription": institution_desc}},
                False)

            print("End update institution description for " + str(institution_id))
            return True
        else:
            print("Onboarding is not completed! End update institution description for " + str(INSTITUTION_ID))
            return False



if __name__ == "__main__":
    client = MongoClient(HOST)

    migrate_institution_description_from_onboarding_to_core(client)

    client.close()