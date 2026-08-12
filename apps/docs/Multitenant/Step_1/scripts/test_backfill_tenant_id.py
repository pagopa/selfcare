import importlib.util
import pathlib
import unittest
from unittest.mock import MagicMock, Mock

from pymongo.errors import OperationFailure


SCRIPT_PATH = pathlib.Path(__file__).with_name("backfill_tenant_id.py")
SPEC = importlib.util.spec_from_file_location("backfill_tenant_id", SCRIPT_PATH)
BACKFILL = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BACKFILL)


class FakeClient:
    def __init__(self, databases):
        self.databases = databases

    def __getitem__(self, name):
        return self.databases[name]


class BackfillTenantIdTest(unittest.TestCase):

    def test_validates_exact_account_and_tenant(self):
        mongo_host = (
            "mongodb://user:secret@selc-d-cosmosdb-mongodb-account.mongo.cosmos.azure.com:10255/"
            "?tls=true"
        )

        BACKFILL.validateAccount(
            mongo_host, "selc-d-cosmosdb-mongodb-account", "AR")

        with self.assertRaisesRegex(ValueError, "not expected account"):
            BACKFILL.validateAccount(
                mongo_host, "selc-d-weu-pnpg-cosmosdb-mongodb-account", "PNPG")

    def test_rejects_account_registered_for_another_tenant(self):
        mongo_host = (
            "mongodb://user:secret@"
            "selc-d-weu-pnpg-cosmosdb-mongodb-account.mongo.cosmos.azure.com:10255/"
        )

        with self.assertRaisesRegex(ValueError, "belongs to tenant PNPG"):
            BACKFILL.validateAccount(
                mongo_host, "selc-d-weu-pnpg-cosmosdb-mongodb-account", "AR")

    def test_collection_listing_error_is_fatal(self):
        database = MagicMock()
        database.list_collection_names.side_effect = OperationFailure("denied")
        client = FakeClient({"selcAuth": database})

        with self.assertRaises(OperationFailure):
            BACKFILL.resolveCollections(client, ["selcAuth"], set())

    def test_missing_collection_requires_explicit_allowance(self):
        database = MagicMock()
        database.list_collection_names.return_value = []
        client = FakeClient({"selcAuth": database})

        with self.assertRaisesRegex(RuntimeError, "--allow-missing"):
            BACKFILL.resolveCollections(client, ["selcAuth"], set())

        with self.assertRaisesRegex(RuntimeError, "No expected collection was inspected"):
            BACKFILL.resolveCollections(
                client, ["selcAuth"], {"selcAuth.otpFlows"})

    def test_mismatched_tenant_blocks_every_write(self):
        collection = Mock()
        collection.count_documents.side_effect = [4, 1]
        database = MagicMock()
        database.list_collection_names.return_value = ["otpFlows"]
        database.__getitem__.return_value = collection
        client = FakeClient({"selcAuth": database})

        untagged, tagged, mismatched = BACKFILL.run(
            client, ["selcAuth"], "AR", apply=True, allowedMissing=set())

        self.assertEqual((4, 0, 1), (untagged, tagged, mismatched))
        collection.update_many.assert_not_called()

    def test_apply_tags_missing_and_explicit_null_tenant_ids(self):
        collection = Mock()
        collection.count_documents.side_effect = [4, 0]
        collection.update_many.return_value.modified_count = 4
        database = MagicMock()
        database.list_collection_names.return_value = ["otpFlows"]
        database.__getitem__.return_value = collection
        client = FakeClient({"selcAuth": database})

        untagged, tagged, mismatched = BACKFILL.run(
            client, ["selcAuth"], "AR", apply=True, allowedMissing=set())

        self.assertEqual((4, 4, 0), (untagged, tagged, mismatched))
        collection.update_many.assert_called_once_with(
            BACKFILL.UNTAGGED, {"$set": {"tenantId": "AR"}})

    def test_queries_require_exact_scalar_tenant_values(self):
        self.assertEqual(
            {
                "$or": [
                    {"tenantId": {"$exists": False}},
                    {"$expr": {"$eq": [{"$type": "$tenantId"}, "null"]}},
                ]
            },
            BACKFILL.UNTAGGED)
        self.assertEqual(
            {
                "tenantId": {"$exists": True},
                "$expr": {
                    "$and": [
                        {"$ne": [{"$type": "$tenantId"}, "null"]},
                        {"$ne": ["$tenantId", "AR"]},
                    ]
                },
            },
            BACKFILL.mismatchedTenant("AR"))


if __name__ == "__main__":
    unittest.main()
