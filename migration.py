import math
import os
from dotenv import load_dotenv

from pymongo import MongoClient, WriteConcern

from query import *

load_dotenv()
HOST =  os.getenv('HOST')

CORE_DB = 'selcMsCore'
USER_COLLECTION = 'User'

USERS_DB = 'selcUser'
USER_INSTITUTION_COLLECTION = 'userInstitutions'
USER_INFO_COLLECTION = 'userInfo'

BATCH_SIZE = 100
START_PAGE = 0


def migrate_user_institution(client):
    user_size = client[CORE_DB][USER_COLLECTION].count_documents({})
    print(USER_COLLECTION + " size: " + str(user_size))
    pages = math.ceil(user_size / BATCH_SIZE)

    for page in range(START_PAGE, pages):
        print("Start page " + str(page) + "/" + str(pages))

        result = client[CORE_DB][USER_COLLECTION].aggregate(
            user_institution_from_user_query(page, BATCH_SIZE)
        )

        for user in result:
            institution_desc = user.get('institutionDescription')
            root_name = user.get('institutionRootName')
            set_dict = {"products": user.get('products')}
            if institution_desc is not None:
                set_dict["institutionDescription"] = institution_desc
            if root_name is not None:
                set_dict["institutionRootName"] = root_name

            client[USERS_DB][USER_INSTITUTION_COLLECTION].update_one(
                {"userId": user.get('userId'), "institutionId": user.get('institutionId')},
                {"$set": set_dict},
                True)

        print("End page " + str(page) + "/" + str(pages))


def generate_user_info(client):
    result_info = client[USERS_DB][USER_INSTITUTION_COLLECTION].aggregate(
        user_info_from_user_institution_query(USERS_DB, USER_INFO_COLLECTION)
    )
    # client[USERS_DB][USER_INFO_COLLECTION].delete_many({})

    for user_info in result_info:
        user_info['institutions'] = [institution for institution in user_info.get('institutions') if
                                     institution.get('status') is not None]
        if len(user_info.get('institutions')) != 0:
            client[USERS_DB][USER_INFO_COLLECTION].update_one(
                {"_id": user_info.get('_id')},
                {"$set": {"institutions": user_info.get('institutions')}}, True)
        else:
            client[USERS_DB][USER_INFO_COLLECTION].delete_one({"_id": user_info.get('_id')})


    user_institution_size = client[USERS_DB][USER_INSTITUTION_COLLECTION].count_documents({})
    print(USER_INSTITUTION_COLLECTION + " size: " + str(user_institution_size))
    user_info_size = client[USERS_DB][USER_INFO_COLLECTION].count_documents({})
    print(USER_INFO_COLLECTION + " size: " + str(user_info_size))

if __name__ == "__main__":
    client = MongoClient(HOST)
    with client.start_session() as session:
        session.start_transaction()
        migrate_user_institution(client)
        session.commit_transaction()

    print("Start generate the " + USER_INFO_COLLECTION)
    generate_user_info(client)
    print("End generate the " + USER_INFO_COLLECTION)


    client.close()
