from pymongo import MongoClient
import sys

# =========================
# CONFIGURATION
# =========================

MONGO_URI = "MONGO-CONNECTION-STRING"

DB_NAME = "selcUser"
COLLECTION = "userInstitutions"

PRODUCT_ID = "prod-io"
ROLE = "ADMIN_EA"
NEW_PRODUCT_ROLE = "admin_aggregator"

BATCH_SIZE = 1000
DRY_RUN = True

# =========================
# MONGODB CONNECTION
# =========================

client = MongoClient(MONGO_URI)
collection = client[DB_NAME][COLLECTION]

# =========================
# QUERY DEFINITION
# =========================

query = {
  "products": {
    "$elemMatch": {
      "productId": PRODUCT_ID,
      "role": ROLE
    }
  }
}

try:

  total = collection.count_documents(query)

  print(f"Documents found: {total}")

  if total == 0:
    print("No documents to process.")
    sys.exit(0)

  num_batches = (total + BATCH_SIZE - 1) // BATCH_SIZE

  print(
    "\n========== DRY RUN =========="
    if DRY_RUN
    else "\n========== EXECUTION =========="
  )

  print(f"Batch size: {BATCH_SIZE}")
  print(f"Number of batches: {num_batches}")

  # =========================
  # CURSOR INITIALIZATION
  # Stream matching document IDs without loading
  # the entire result set into memory
  # =========================

  cursor = collection.find(
    query,
    {"_id": 1}
  ).sort("_id", 1)

  batch_ids = []

  processed_docs = 0
  processed_batches = 0
  total_updated = 0

  for doc in cursor:

    batch_ids.append(doc["_id"])

    # Wait until the current batch reaches the configured size
    if len(batch_ids) < BATCH_SIZE:
      continue

    processed_batches += 1

    print(
      f"\nBatch {processed_batches}/{num_batches} "
      f"-> documents: {len(batch_ids)}"
    )

    if DRY_RUN:

      # Simulate the update without modifying data
      print(
        f"   (dry run) candidates: {len(batch_ids)}"
      )

      total_updated += len(batch_ids)

    else:

      # Update all matching products in the current batch
      result = collection.update_many(
        {
          "_id": {
            "$in": batch_ids
          }
        },
        {
          "$set": {
            "products.$[p].productRole": NEW_PRODUCT_ROLE
          }
        },
        array_filters=[
          {
            "p.productId": PRODUCT_ID,
            "p.role": ROLE
          }
        ]
      )

      total_updated += result.modified_count

      print(
        f"   updated: {result.modified_count}"
      )

    processed_docs += len(batch_ids)

    # Reset batch buffer
    batch_ids = []

  # =========================
  # PROCESS REMAINING DOCUMENTS
  # (last batch may contain fewer documents)
  # =========================

  if batch_ids:

    processed_batches += 1

    print(
      f"\nBatch {processed_batches}/{num_batches} "
      f"-> documents: {len(batch_ids)}"
    )

    if DRY_RUN:

      print(
        f"   (dry run) candidates: {len(batch_ids)}"
      )

      total_updated += len(batch_ids)

    else:

      result = collection.update_many(
        {
          "_id": {
            "$in": batch_ids
          }
        },
        {
          "$set": {
            "products.$[p].productRole": NEW_PRODUCT_ROLE
          }
        },
        array_filters=[
          {
            "p.productId": PRODUCT_ID,
            "p.role": ROLE,
            "p.productRole": {
              "$ne": NEW_PRODUCT_ROLE
            }
          }
        ]
      )

      total_updated += result.modified_count

      print(
        f"   updated: {result.modified_count}"
      )

    processed_docs += len(batch_ids)

  # =========================
  # EXECUTION SUMMARY
  # =========================

  print("\n===================================")
  print("SUMMARY")
  print("===================================")

  print(f"Documents found: {total}")
  print(f"Documents processed: {processed_docs}")
  print(f"Batches processed: {processed_batches}")

  if DRY_RUN:
    print(f"Update candidates: {total_updated}")
    print("\nDry run completed.")
  else:
    print(f"Documents updated: {total_updated}")

except KeyboardInterrupt:

  print("\n===================================")
  print("INTERRUPTED")
  print("===================================")

  print(f"Documents processed: {processed_docs}")
  print(f"Batches processed: {processed_batches}")

  if DRY_RUN:
    print(f"Candidates processed so far: {total_updated}")
  else:
    print(f"Documents updated so far: {total_updated}")

except Exception as e:

  print(f"\nERROR: {e}")

  import traceback
  traceback.print_exc()

finally:

  client.close()

  print("\nMongoDB connection closed.")
