import math
import sys
import os
from pymongo import MongoClient
from dotenv import load_dotenv
from query import *

load_dotenv()
HOST =  os.getenv('HOST')

CORE_DB = 'selcMsCore'
INSTITUTION_COLLECTION = 'Institution'
DELEGATION_COLLECTION = 'Delegations'

BATCH_SIZE = 100
START_PAGE = 0
DELEGATION_ID = None if len(sys.argv) < 2 else sys.argv[1]


def copy_tax_code_to_delegations(client):
    if DELEGATION_ID is None:

        delegation_size_cursor = client[CORE_DB][DELEGATION_COLLECTION].aggregate(count_delegations())
        delegation_size = next(delegation_size_cursor)['count']
        print(DELEGATION_COLLECTION + " size: " + str(delegation_size))
        pages = math.ceil(delegation_size / BATCH_SIZE)

        for page in range(START_PAGE, pages):
            print("Start page " + str(page + 1) + "/" + str(pages))

        delegations_pages = client[CORE_DB][DELEGATION_COLLECTION].aggregate(
            get_delegations(page, BATCH_SIZE)
        )

        for delegation in delegations_pages:
            from_id = delegation.get('from')
            to_id = delegation.get('to')

            retrieve_institution_taxcode_and_update_delegation(client, delegation, from_id, to_id)

        print("End page " + str(page + 1) + "/" + str(pages))

    else:
        delegation = client[CORE_DB][DELEGATION_COLLECTION].find_one({"_id": DELEGATION_ID})
        if delegation is not None:
            from_id = delegation.get('from')
            to_id = delegation.get('to')

            retrieve_institution_taxcode_and_update_delegation(client, delegation, from_id, to_id)

            print("End copying taxCode to " + DELEGATION_COLLECTION)
        else:
            print("No delegation found with the provided ID.")


def retrieve_institution_taxcode_and_update_delegation(client, delegation, from_id, to_id):
    from_institution = client[CORE_DB][INSTITUTION_COLLECTION].find_one({"_id": from_id})
    to_institution = client[CORE_DB][INSTITUTION_COLLECTION].find_one({"_id": to_id})

    if from_institution is not None:
        from_tax_code = from_institution.get('taxCode')
        client[CORE_DB][DELEGATION_COLLECTION].update_one(
            {"_id": delegation['_id']},
            {"$set": {"fromTaxCode": from_tax_code}}
        )

    if to_institution is not None:
        to_tax_code = to_institution.get('taxCode')
        client[CORE_DB][DELEGATION_COLLECTION].update_one(
            {"_id": delegation['_id']},
            {"$set": {"toTaxCode": to_tax_code}}
        )


if __name__ == "__main__":
    client = MongoClient(HOST)

    print("Start copying taxCode to " + DELEGATION_COLLECTION)
    copy_tax_code_to_delegations(client)
    print("End copying taxCode to " + DELEGATION_COLLECTION)

    client.close()
