## Institution migration scripts

Python scripts to manipulate data about Selfcare Institution domain.

## Requirements

- Python 3.6 or higher
- MongoDB
- Required Python packages: `python-dotenv`, `pymongo`, `python-dateutil`

## Setup

1. **Clone the repository:**
   ```sh
   git clone <repository_url>
   cd <repository_directory>/selfcare-institution/scripts/institution-migration
   ```

## institutiton_to_pec_notification

This Python script creates `PecNotification` documents for institutions with active onboarding, using data stored in a MongoDB database. The process performs the following main operations:

1. Connects to the MongoDB database.
2. Calculates the module day of the epoch for notifications.
3. Retrieves institutions with active onboarding in batches.
4. Creates or updates `PecNotification` documents for each institution and onboarding.

Create a .env file in the root directory of the project and add the environment variables inside .env.example

