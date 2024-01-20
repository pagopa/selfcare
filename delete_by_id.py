from pymongo import MongoClient

HOST = ""

DB = 'selcUser'
COLLECTION = ''
ID = ""

if __name__ == "__main__":
    client = MongoClient(HOST)

    # Delete
    users = client[DB][COLLECTION].delete_many({'_id': ID})
    print("Delete in " + COLLECTION + " " + str(users.deleted_count) + " users")

    client.close()
