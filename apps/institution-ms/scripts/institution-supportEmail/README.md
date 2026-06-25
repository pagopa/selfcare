## SupportEmail script
Python script to update the supportEmail field in the Institution collection using the corresponding onboarding if support email is present.

## Requirements

- Python 3.6 or higher
- MongoDB
- Required Python packages: `pymongo`

## Setup

1. **Clone the repository:**
   ```sh
   git clone <repository_url>
   cd <repository_directory>/selfcare-institution/scripts/institution-supportEmail
   ```

## Steps

1. Connects to the MongoDB database. (local, dev, uat, prod)
2. Export the `MONGO_HOST` environment variable with the connection string of the target mongo database
3. Run `python update_supportEmail.py --dry-run` in selfcare-institution/scripts/institution-supportEmail to test
4. Run `python update_supportEmail.py` in selfcare-institution/scripts/institution-supportEmail to update supportEmail field

