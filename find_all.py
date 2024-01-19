from pymongo import MongoClient

HOST = "mongodb://localhost:27017/"

DB = 'selcUser'
COLLECTION = 'UserInstitution'
ID = ""

if __name__ == "__main__":
    client = MongoClient(HOST)

    # FindAll
    users = client[DB][COLLECTION].find({})
    print(COLLECTION + ": " + str(users))

    client.close()
