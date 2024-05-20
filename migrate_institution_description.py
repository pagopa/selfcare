import math
import os
from dotenv import load_dotenv
import sys

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
        onboardings_size_cursor = client[ONBOARDING_DB][ONBOARDINGS_COLLECTION].aggregate(count_institutions_from_onboardings())
        onboardings_size = next(onboardings_size_cursor)['count']
        print("Onboarding's institutions size: " + str(onboardings_size))
        pages = math.ceil(onboardings_size / BATCH_SIZE)

        for page in range(START_PAGE, pages):
            print("Start page " + str(page + 1) + "/" + str(pages))

            onboarding_pages = client[ONBOARDING_DB][ONBOARDINGS_COLLECTION].aggregate(
                get_institutions_from_onboardings(page, BATCH_SIZE)
            )

            for onboarding in onboarding_pages:
                institution_desc = onboarding['institution']['description']
                institution_id = onboarding['institution']['id']
                print("retrieved institution id " + institution_id + " with description: " + institution_desc)

                client[CORE_DB][INSTITUTION_COLLECTION].update_many(
                    {"_id": institution_id},
                    {"$set": {"description": institution_desc}},
                    False)

            print("End page " + str(page + 1) + "/" + str(pages))
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

            print("End update institution description for " + str(institution_id))
            return True
        else:
            print("Onboarding is not completed! End update institution description for " + str(INSTITUTION_ID))
            return False

def migrate_institution_description_from_institution_to_user(client):
    if INSTITUTION_ID is None:
        institutions_size = client[CORE_DB][INSTITUTION_COLLECTION].count_documents({})
        print(INSTITUTION_COLLECTION + " size: " + str(institutions_size))
        pages = math.ceil(institutions_size / BATCH_SIZE)

        for page in range(START_PAGE, pages):
            print("Start page " + str(page + 1) + "/" + str(pages))

            institution_pages = client[CORE_DB][INSTITUTION_COLLECTION].aggregate(
                get_institutions(page, BATCH_SIZE)
            )

            for institution in institution_pages:
                        institution_desc = institution.get('description')
                        institution_id = institution.get('_id')

                        client[USERS_DB][USER_INSTITUTION_COLLECTION].update_many(
                            {"institutionId": institution_id},
                            {"$set": {"institutionDescription": institution_desc}},
                            False)

            print("End page " + str(page + 1) + "/" + str(pages))

    else:
        institution = client[CORE_DB][INSTITUTION_COLLECTION].find_one(
                get_institution(INSTITUTION_ID)
            )

        institution_id = institution.get('_id')
        institution_desc = institution.get('description')

        client[USERS_DB][USER_INSTITUTION_COLLECTION].update_one(
               {"institutionId": institution_id},
               {"$set": {"institutionDescription": institution_desc}},
               False)

        print("End update institution description for " + str(institution_id))


if __name__ == "__main__":
    client = MongoClient(HOST)

    if migrate_institution_description_from_onboarding_to_core(client):
        migrate_institution_description_from_institution_to_user(client)

    client.close()