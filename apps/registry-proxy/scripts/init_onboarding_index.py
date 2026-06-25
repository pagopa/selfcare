#!/usr/bin/env python3
"""
Init Onboarding Search Index

Fetches onboarding data from the MongoDB onboarding collection and updates the search index accordingly.

Before running this script, ensure that the following environment variables are set:
- MONGO_HOST: The connection string for the MongoDB instance.
- ONBOARDING_INDEX_URL: The base URL for the onboarding search index API.
- ONBOARDING_INDEX_API_KEY: The API key for authenticating with the onboarding search index API.

"""

import requests
import json
import sys
import os

from datetime import datetime, timezone
from pymongo import MongoClient

MONGO_HOST = os.environ["MONGO_HOST"]
MONGO_BATCH_SIZE = 100
ONBOARDING_DB = "selcOnboarding"
ONBOARDING_COLLECTION = "onboardings"
ONBOARDING_INDEX_NAME = "onboarding-index-ar"
ONBOARDING_INDEX_URL = os.environ["ONBOARDING_INDEX_URL"]
ONBOARDING_INDEX_API_KEY = os.environ["ONBOARDING_INDEX_API_KEY"]

# Use it to delete documents from the index if needed, pass a list of onboardingIds to delete
def delete_onboarding_index(onboarding_ids: list[str]):
    response = requests.post(f"{ONBOARDING_INDEX_URL}/indexes/{ONBOARDING_INDEX_NAME}/docs/index?api-version=2023-11-01", headers={"api-key": ONBOARDING_INDEX_API_KEY}, json={
        "value": [
            {
                "@search.action": "delete",
                "onboardingId": _id,
            }
            for _id in onboarding_ids
        ]
    })
    if response.status_code != 200:
        print(f"Error deleting from onboarding index: {response.status_code} - {response.text}", file=sys.stderr)
        return False
    return True

# Convert datetime to string forcing utc timezone and formatting it in isoformat with only 3 digits for milliseconds
def get_date_string(dt: datetime) -> str:
    dt = dt.replace(tzinfo=timezone.utc)
    dt = dt.strftime("%Y-%m-%dT%H:%M:%S.") + f"{dt.microsecond // 1000:03d}+00:00"
    return dt

# Update the onboarding index with the given onboardings, it will merge or upload the documents based on the onboardingId
def update_onboarding_index(onboardings: list[dict]):
    response = requests.post(f"{ONBOARDING_INDEX_URL}/indexes/{ONBOARDING_INDEX_NAME}/docs/index?api-version=2023-11-01", headers={"api-key": ONBOARDING_INDEX_API_KEY}, json={
        "value": [
            {
                "@search.action": "mergeOrUpload",
                "onboardingId": o["_id"],
                **({"institutionId": institutionId} if (institutionId := o.get("institution", {}).get("id")) else {}),
                **({"description": description} if (description := o.get("institution", {}).get("description")) else {}),
                **({"parentDescription": parentDescription} if (parentDescription := o.get("institution", {}).get("parentDescription")) else {}),
                **({"taxCode": taxCode} if (taxCode := o.get("institution", {}).get("taxCode")) else {}),
                **({"subunitCode": subunitCode} if (subunitCode := o.get("institution", {}).get("subunitCode")) else {}),
                **({"subunitType": subunitType} if (subunitType := o.get("institution", {}).get("subunitType")) else {}),
                **({"productId": productId} if (productId := o.get("productId")) else {}),
                **({"institutionType": institutionType} if (institutionType := o.get("institution", {}).get("institutionType")) else {}),
                **({"status": status} if (status := o.get("status")) else {}),
                **({"createdAt": get_date_string(createdAt)} if (createdAt := o.get("createdAt")) else {}),
                **({"updatedAt": get_date_string(updatedAt)} if (updatedAt := o.get("updatedAt")) else {}),
                **({"activatedAt": get_date_string(activatedAt)} if (activatedAt := o.get("activatedAt")) else {}),
                **({"expiringDate": get_date_string(expiringDate)} if (expiringDate := o.get("expiringDate")) else {}),
                **({"isTest": isTest} if (isTest := o.get("institution", {}).get("isTest")) else {}),
                **({"city": city} if (city := o.get("institution", {}).get("city")) else {}),
                **({"county": county} if (county := o.get("institution", {}).get("county")) else {}),
                **({"country": country} if (country := o.get("institution", {}).get("country")) else {}),
            }
            for o in onboardings
        ]
    })
    if response.status_code != 200:
        print(f"Error updating onboarding index: {response.status_code} - {response.text}", file=sys.stderr)
        return False
    return True

def get_onboarding_filter():
    return {
        "workflowType": {"$ne": "USERS"},
        "institution": {"$exists": True, "$ne": None},
        "institution.description": {"$exists": True, "$ne": None},
        "institution.institutionType": {"$exists": True, "$ne": None},
        "productId": {"$exists": True, "$ne": None, "$nin": ["prod-interop-atst", "prod-interop-coll", "prod-fd", "prod-fd-garantito", "prod-pagopa-ec"]},
        "status": {"$exists": True, "$ne": None, "$nin": ["REQUEST"]}
    }

def main():
    client = MongoClient(MONGO_HOST)
    db = client[ONBOARDING_DB]
    collection = db[ONBOARDING_COLLECTION]
    print("Fetching onboardings with filter:", json.dumps(get_onboarding_filter()))
    total_count = collection.count_documents(get_onboarding_filter())
    count = 0
    count_errors = 0
    onboardings = []
    for o in collection.find(get_onboarding_filter(), batch_size=MONGO_BATCH_SIZE):
        onboardings.append(o)
        count += 1
        if len(onboardings) >= MONGO_BATCH_SIZE:
            print(f"Updating index: {count}/{total_count} onboardings", end="\r")
            if not update_onboarding_index(onboardings):
                count_errors += len(onboardings)
            onboardings = []
    if onboardings:
        print(f"Updating index: {count}/{total_count} onboardings")
        if not update_onboarding_index(onboardings):
            count_errors += len(onboardings)
        onboardings = []
    print(f"Errors updating onboardings: {count_errors}")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        pass
