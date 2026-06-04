from pymongo import MongoClient
import sys

# =========================
# CONFIG
# =========================

MONGO_URI = "MONGO-CONNECTION-STRING"

ONBOARDING_DB = "selcOnboarding"
USER_INSTITUTIONS_DB = "selcUser"

UPDATE_MISSING_TOKEN = False

VALID_WORKFLOW_TYPES = {
    "USERS",
    "USERS_PG",
    "USERS_EA"
}

# =========================
# CONNECTION
# =========================

print("Connessione a MongoDB...")

client = MongoClient(MONGO_URI)

onboarding_collection = client[ONBOARDING_DB]["onboardings"]
user_institutions_collection = client[USER_INSTITUTIONS_DB]["userInstitutions"]

print("Connessione effettuata.")

# =========================
# COUNTERS
# =========================

total_missing_token = 0
total_missing_userInstitution = 0
total_missing_product = 0
total_updated_token = 0

processed_onboardings = 0
processed_users = 0

try:

    print("Recupero onboarding validi...")

    onboardings = list(
        onboarding_collection.find({
            "workflowType": {"$in": list(VALID_WORKFLOW_TYPES)},
            "status": "COMPLETED"
        })
    )

    onboardings.sort(
        key=lambda o: o.get("createdAt")
    )

    print(f"Onboarding caricati: {len(onboardings)}")

    print("Inizio elaborazione...\n")

    for onboarding in onboardings:

        processed_onboardings += 1

        if processed_onboardings % 1000 == 0:
            print(
                f"Processati {processed_onboardings}/{len(onboardings)} onboarding"
            )

        onboarding_id = onboarding.get("_id")
        institution_id = onboarding.get("institution", {}).get("id")
        product_id = onboarding.get("productId")
        users = onboarding.get("users", [])

        for user in users:
            user_id = user.get("_id")
            user_role = user.get("role")

            if user_role == "MANAGER":

                has_delegate_same_user = any(
                    other_user.get("_id") == user_id
                    and other_user.get("role") == "DELEGATE"
                    for other_user in users
                )

                if not has_delegate_same_user:
                    continue

            processed_users += 1

            user_institution = user_institutions_collection.find_one({
                "institutionId": institution_id,
                "userId": user_id
            })

            if not user_institution:
                total_missing_userInstitution += 1
                continue

            products = user_institution.get("products", [])

            matching_products = [
                (idx, product)
                for idx, product in enumerate(products)
                if (product.get("productId") == product_id and product.get("role") == user_role)
            ]

            if not matching_products:
                total_missing_product += 1
                continue

            latest_index, latest_product = max(
                matching_products,
                key=lambda item: item[1].get("createdAt", "")
            )

            token_id = latest_product.get("tokenId")

            if not token_id:

                total_missing_token += 1

                status = latest_product.get("status")
                created_at = latest_product.get("createdAt")

                print(
                    f"[MISSING TOKEN] "
                    f"userInstitutionId={user_institution.get('_id')} "
                    f"userId={user_id} "
                    f"institutionId={institution_id} "
                    f"productId={product_id} "
                    f"status={status} "
                    f"createdAt={created_at}"
                )

                if UPDATE_MISSING_TOKEN:

                    result = user_institutions_collection.update_one(
                        {"_id": user_institution["_id"]},
                        {
                            "$set": {
                                f"products.{latest_index}.tokenId": onboarding_id
                            }
                        }
                    )

                    if result.modified_count > 0:
                        total_updated_token += 1

                        print(
                            f"  -> UPDATED "
                            f"tokenId={onboarding_id} "
                            f"productIndex={latest_index}"
                        )

    # =========================
    # OUTPUT
    # =========================

    print("\n===================================")
    print("=========== RIEPILOGO ============")
    print("===================================\n")

    print(f"Onboarding processati: {processed_onboardings}")
    print(f"Users processati: {processed_users}")

    print(f"\nMissing userInstitution: {total_missing_userInstitution}")
    print(f"Missing product: {total_missing_product}")
    print(f"Missing tokenId: {total_missing_token}")

    if UPDATE_MISSING_TOKEN:
        print(f"Token aggiornati: {total_updated_token}")

except KeyboardInterrupt:
    print("\nEsecuzione interrotta manualmente.")

except Exception as e:
    print(f"\nERRORE: {e}")
    import traceback
    traceback.print_exc()

finally:
    client.close()
    print("\nConnessione Mongo chiusa.")
    sys.exit(0)