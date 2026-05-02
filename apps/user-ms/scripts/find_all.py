from pymongo import MongoClient

HOST = ""

DB = 'selcUser'
COLLECTION = ''
ID = ""

if __name__ == "__main__":
    client = MongoClient(HOST)

    # FindAll
    users = client[DB][COLLECTION].find({})
    print(COLLECTION + ": " + str(users))

    client.close()
