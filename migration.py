import math

from pymongo import MongoClient, WriteConcern

from query import *

HOST = ""

CORE_DB = 'selcMsCore'
USER_COLLECTION = 'User'

USERS_DB = 'selcUser'
USER_INSTITUTION_COLLECTION = 'UserInstitution'
USER_INFO_COLLECTION = 'UserInfo'

BATCH_SIZE = 10
START_PAGE = 0


def migrate_user_institution(client):
    user_size = client[CORE_DB][USER_COLLECTION].count_documents({})
    print(USER_COLLECTION + " size: " + str(user_size))
    pages = math.ceil(user_size / BATCH_SIZE)

    for page in range(START_PAGE, 1):
        print("Start page " + str(page) + "/" + str(pages - 1))

        result = client[CORE_DB][USER_COLLECTION].aggregate(
            user_institution_from_user_query(page, BATCH_SIZE)
        )

        for user in result:
            client[USERS_DB][USER_INSTITUTION_COLLECTION].update_one(
                {"userId": user.get('userId'), "institutionId": user.get('institutionId')},
                {"$set": {"products": user.get('products'),
                          "institutionDescription": user.get('institutionDescription')}}, True)

        print("End page " + str(page + 1) + "/" + str(pages))


def generate_user_info(client):
    user_institution_size = client[USERS_DB][USER_INSTITUTION_COLLECTION].count_documents({})
    print(USER_INSTITUTION_COLLECTION + " size: " + str(user_institution_size))

    result_info = client[USERS_DB][USER_INSTITUTION_COLLECTION].aggregate(
        user_info_from_user_institution_query(USERS_DB, USER_INFO_COLLECTION)
    )
    # client[USERS_DB][USER_INFO_COLLECTION].delete_many({})

    for user_info in result_info:
        client[USERS_DB][USER_INFO_COLLECTION].update_one(
            {"_id": user_info.get('_id')},
            {"$set": {"institutions": user_info.get('institutions')}}, True)

if __name__ == "__main__":
    client = MongoClient(HOST)
    with client.start_session() as session:
        session.start_transaction()
        migrate_user_institution(client)
        session.commit_transaction()

    generate_user_info(client)

    # check finale USER_INFO_COLLECTION deve avere lo stesso numero di elementi di USER_COLLECTION
    user_size = client[CORE_DB][USER_COLLECTION].count_documents({})
    user_info_size = client[USERS_DB][USER_INFO_COLLECTION].count_documents({})
    if user_info_size != user_size:
        print("ERROR")
        print("USER CORE " + str(user_size))
        print("USER INFO " + str(user_info_size))
    else:
        print("MIGRATION SUCCESSFUL")

    client.close()
