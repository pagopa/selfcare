# Institution mail migration scripts

Python scripts to migrate from the old PecNotification collection to the new MailNotification collection:

1. check_pec_notification_institutions.py: verify that the PecNotification collection is consistent with the Institution collection:
    - Check that the digitalAddress is the one present in Institution
    - Check that every PecNotification has an ACTIVE or SUSPENDED onboarding in Institution

2. pec_to_mail_notification.py: migrate documents from PecNotification to MailNotification:
    - Groups PecNotification by institutionId and upsert to a new MailNotification document
    - The new document take the PecNotification with the oldest createdAt as reference to set the moduleDayOfTheEpoch
    - It's possible to use `dryRun` as first argument to avoid to upsert and only check that the scripts can run without errors

3. check_mail_notification_institutions.py: verify that the MailNotification collection is consistent with the Institution collection after the migration process:
    - Check that the digitalAddress is the one present in Institution
    - Check that every productId in state ACTIVE or SUSPENDED for each Institution is present in the productIds field of the respective MailNotification document

4. check_institutions_mail_notification.py: verify if there are Institutions without a MailNotification document associated for the onboarded products

Before launching any script remember to export the `MONGO_HOST` environment variable with the connection string of the target mongo database