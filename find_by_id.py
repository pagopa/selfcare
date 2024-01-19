from pymongo import MongoClient

HOST = "mongodb://localhost:27017/"

DB = 'selcUser'
COLLECTION = 'UserInstitution'
ID = ""

if __name__ == "__main__":
    client = MongoClient(HOST)

    # Find
    user = client[DB][COLLECTION].find({'_id': ID})
    print(COLLECTION + ": " + str(user))

    client.close()
