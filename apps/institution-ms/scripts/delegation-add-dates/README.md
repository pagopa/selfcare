## Add createdAt in delegation script

- Python script to add createdAt field in Delegations when it doesn't exist.
- Python script to check if all dates are with a valid format.

## Requirements

- Python 3.6 or higher
- MongoDB
- Required Python packages: `pymongo`

## Steps

1. Connects to the MongoDB database. (local, dev, uat, prod)
2. Export the `MONGO_HOST` environment variable with the connection string of the target mongo database
3. Run `python delegation_add_createdAt.py` in selfcare-institution/scripts/delegation-add-createdAt
4. Run `python check_valid_dates.py` in selfcare-institution/scripts/delegation-add-createdAt


