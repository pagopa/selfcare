#!/usr/bin/env python3

import os
import sys
from pymongo import MongoClient, UpdateOne

MONGO_HOST = os.getenv("MONGO_HOST")

CORE_DB = "selcMsCore"
ONBOARDING_DB = "selcOnboarding"

INSTITUTION_COLLECTION = "Institution"
ONBOARDING_COLLECTION = "onboardings"

BATCH_SIZE = 100
PROGRESS_UPDATE = 50

class Colors:
    WARNING = '\033[93m'
    INFO = '\033[96m'
    END = '\033[0m'

def get_active_prod_io_sign_token(institution):
    return next(
        (
            onboarding.get("tokenId")
            for onboarding in institution.get("onboarding", [])
            if onboarding.get("productId") == "prod-io-sign"
               and onboarding.get("status") == "ACTIVE"
        ),
        None
    )

def get_onboarding(db, onboarding_id):
    return db.find_one({"_id": onboarding_id})

def main(dry_run=False):
    client = MongoClient(MONGO_HOST)

    core_collection = client[CORE_DB][INSTITUTION_COLLECTION]
    onboarding_collection = client[ONBOARDING_DB][ONBOARDING_COLLECTION]

    total_institutions = core_collection.count_documents(
        {"supportEmail": {"$exists": False}, "onboarding.productId": "prod-io-sign"}
    )

    print(Colors.INFO + f"Institutions to process: {total_institutions}" + Colors.END)

# Cursor solo per institution che non hanno supportEmail
    cursor = core_collection.find(
        {"supportEmail": {"$exists": False}, "onboarding.productId": "prod-io-sign"},
        batch_size=BATCH_SIZE
    )

    processed = 0
    updated_count = 0
    skipped_count = 0
    bulk_ops = []

    for inst in cursor:
        institution_id = inst.get("_id")
        token_id = get_active_prod_io_sign_token(inst)
        if token_id:
            onboarding = get_onboarding(onboarding_collection, token_id)
            if onboarding:
                if onboarding.get("status") == "COMPLETED":
                    support_email = onboarding.get("institution", {}).get("supportEmail")
                    if support_email:
                        bulk_ops.append(
                            UpdateOne(
                                {"_id": institution_id},
                                {
                                    "$set": {
                                        "supportEmail": support_email
                                    }
                                }
                            ))
                        updated_count += 1
                        print(Colors.INFO + f"supportEmail updated for institution {institution_id} with token {token_id}" + Colors.END)
                    else:
                        skipped_count += 1
                        print(Colors.WARNING + f"No supportEmail for institution {institution_id} with token {token_id}" + Colors.END)
                else:
                    skipped_count += 1
                    print(Colors.WARNING + f"Onboarding for institution {institution_id} with token {token_id} with status NOT COMPLETED" + Colors.END)
            else:
                skipped_count += 1
                print(Colors.WARNING + f"No onboarding for institution {institution_id} with token {token_id}" + Colors.END)
        else:
            skipped_count += 1
            print(Colors.WARNING + f"No tokenId or onboarding not active for {institution_id}" + Colors.END)

        processed += 1

        # progress logger
        if processed % PROGRESS_UPDATE == 0 or processed == total_institutions:
            percent = (processed / total_institutions) * 100
            print(f"Progress: {processed}/{total_institutions} ({percent:.1f}%)")

        # bulk write
        if len(bulk_ops) >= BATCH_SIZE:
            if not dry_run:
                core_collection.bulk_write(bulk_ops, ordered=False)
            print(f"Bulk updated {len(bulk_ops)} institutions...")
            bulk_ops = []

    # Scrivi eventuali update rimasti
    if bulk_ops:
        if not dry_run:
            core_collection.bulk_write(bulk_ops, ordered=False)
        print(f"Bulk updated {len(bulk_ops)} institutions...")

    print("\nSynchronization completed.")
    print(f"Total updated: {updated_count}")
    print(f"Total skipped: {skipped_count}")

    if dry_run:
        print(Colors.INFO + "DRY-RUN mode: no changes were written to DB." + Colors.END)

    client.close()


if __name__ == "__main__":
    try:
        dry_run = "--dry-run" in sys.argv
        main(dry_run)
    except KeyboardInterrupt:
        print("\nInterrupted by user")