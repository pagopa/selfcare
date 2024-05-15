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

BATCH_SIZE = 100
START_PAGE = 0
INSTITUTION_ID = None if len(sys.argv) < 2 else sys.argv[1]

def migrate_institution_description(client):
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

    migrate_institution_description(client)

    client.close()