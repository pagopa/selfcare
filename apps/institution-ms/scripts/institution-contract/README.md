## Contract update script

Python script to update contract field in Institution collection.

## Requirements

- Python 3.6 or higher
- MongoDB
- Required Python packages: `pymongo`

## Setup

1. **Clone the repository:**
   ```sh
   git clone <repository_url>
   cd <repository_directory>/selfcare-institution/scripts/institution-contract
   ```

## Steps

1. Connects to the MongoDB database. (local, dev, uat, prod)
2. Export the `MONGO_HOST` environment variable with the connection string of the target mongo database
3. Run `python update_contract.py` in selfcare-institution/scripts/institution-contract

