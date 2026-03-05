db = db.getSiblingDB("selcOnboarding");

db.onboardings.insertOne({
  createdAt: new Date(),
});

db.tokens.insertOne({
  createdAt: new Date(),
});

db = db.getSiblingDB("selcMsCore");

db.Institution.insertOne({
  createdAt: new Date(),
});

db = db.getSiblingDB("selcUser");

db.userInstitutions.insertOne({
  createdAt: new Date(),
});
