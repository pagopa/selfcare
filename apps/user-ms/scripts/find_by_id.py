from pymongo import MongoClient

HOST = ""

DB = 'selcUser'
COLLECTION = ''
ID = ""

if __name__ == "__main__":
    client = MongoClient(HOST)

    # Find
    user = client[DB][COLLECTION].find({'_id': ID})
    print(COLLECTION + ": " + str(user))

    client.close()
