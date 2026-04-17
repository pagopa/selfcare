import os
from pymongo import MongoClient

HOST = os.getenv('MONGO_HOST')

# Set DB configurations
CORE_DB = 'selcMsCore'
ONBOARDING_DB = 'selcOnboarding'
INSTITUTION_COLLECTION = 'Institution'
TOKENS_COLLECTION = 'tokens'

BATCH_SIZE = 100
START_PAGE = 0

"""
This script synchronizes the 'contract' field related to the institution's onboarding in 'selcMsCore'
database, using information from the 'tokens' collection in the 'selcOnboarding' database.

For each institution, it iterates through the onboarding entities:
- If a valid tokenId is present and the corresponding token document contains a 'contractSigned' field,
  the 'contract' field in the onboarding entry is updated accordingly.
- If the token is missing or does not contain a 'contractSigned' field, the 'contract' field is removed.
- The institution document is updated in the database only if any onboarding entry was modified.

The script processes institutions in batches to handle large datasets efficiently.
"""

def sync_contracts(client):
    print("Start contract synchronization from token...")

    institutions_collection = client[CORE_DB][INSTITUTION_COLLECTION]
    tokens_collection = client[ONBOARDING_DB][TOKENS_COLLECTION]

    total = institutions_collection.count_documents({})
    pages = (total // BATCH_SIZE) + 1

    for page in range(START_PAGE, pages):
        print(f"Page {page + 1}/{pages}")
        institutions = institutions_collection.find().skip(page * BATCH_SIZE).limit(BATCH_SIZE)

        for institution in institutions:
            updated_onboarding = []
            modified = False

            for onboarding in institution.get("onboarding", []):
                token_id = onboarding.get("tokenId")
                if token_id:
                    token_doc = tokens_collection.find_one({"_id": token_id})
                    if token_doc and token_doc.get("contractSigned"):
                        if onboarding.get("contract") != token_doc["contractSigned"]:
                            onboarding["contract"] = token_doc["contractSigned"]
                            modified = True
                    else:
                        if "contract" in onboarding:
                            onboarding.pop("contract", None)
                            modified = True
                else:
                    if "contract" in onboarding:
                        onboarding.pop("contract", None)
                        modified = True

                updated_onboarding.append(onboarding)

            if modified:
                institutions_collection.update_one(
                    {"_id": institution["_id"]},
                    {"$set": {"onboarding": updated_onboarding}}
                )
                print(f"Institution updated: {institution['_id']}")

    print("Synchronization completed.")

if __name__ == "__main__":
    client = MongoClient(HOST)
    sync_contracts(client)
    client.close()
