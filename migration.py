import sys
# from dotenv import load_dotenv
from pymongo import MongoClient

from query import *

load_dotenv()
HOST = os.getenv('HOST')

CORE_DB = 'selcMsCore'
USER_COLLECTION = 'User'

USERS_DB = 'selcUser'
USER_INSTITUTION_COLLECTION = 'userInstitutions'
USER_INFO_COLLECTION = 'userInfo'

BATCH_SIZE = 100
START_PAGE = 0


def generate_user_info(client, userId):
    result_info = client[USERS_DB][USER_INSTITUTION_COLLECTION].aggregate(
        user_info_from_user_institution_query(USERS_DB, USER_INFO_COLLECTION, userId)
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
    userId = None
    if len(sys.argv) >1:
     userId = sys.argv[1]

    print("Start generate the " + USER_INFO_COLLECTION)
    generate_user_info(client, userId)
    print("End generate the " + USER_INFO_COLLECTION)


    client.close()
