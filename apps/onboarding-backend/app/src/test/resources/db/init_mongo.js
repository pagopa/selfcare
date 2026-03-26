db = db.getSiblingDB("selcOnboarding");

db.onboardings.insertMany([
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i103",
    createdAt: ISODate("2024-10-18T12:24:50.050Z"),
    institution: {
      address: "sede leg",
      city: "Milano",
      country: "IT",
      county: "MI",
      description: "Techpartner 23-02",
      digitalAddress: "pec@pectest.com",
      geographicTaxonomies: [],
      id: "c2808e95-59a7-44fb-a6df-64faf4ff3ed3",
      imported: false,
      institutionType: "PT",
      origin: "INFOCAMERE",
      taxCode: "11223345665",
      zipCode: "11223",
    },
    productId: "prod-io",
    status: "PENDING",
    users: [
      {
        id: "ec7ca4d5-c537-462a-8c06-e102e0c21c44",
        role: "MANAGER",
        userMailUuid: "ID_MAIL#49e16aa5-bc28-40c7-9844-6886e4647e22",
      },
    ],
    workflowType: "USERS",
  },
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i105",
    createdAt: ISODate("2024-10-18T12:24:50.050Z"),
    institution: {
      address: "sede leg",
      city: "Milano",
      country: "IT",
      county: "MI",
      description: "Techpartner 23-02",
      digitalAddress: "pec@pectest.com",
      geographicTaxonomies: [],
      id: "c2808e95-59a7-44fb-a6df-64faf4ff3ed3",
      imported: false,
      institutionType: "PT",
      origin: "INFOCAMERE",
      taxCode: "11223345665",
      zipCode: "11223",
    },
    productId: "prod-io",
    status: "PENDING",
    users: [
      {
        id: "ec7ca4d5-c537-462a-8c06-e102e0c21c44",
        role: "MANAGER",
        userMailUuid: "ID_MAIL#49e16aa5-bc28-40c7-9844-6886e4647e22",
      },
    ],
    workflowType: "USERS",
    referenceOnboardingId: "89ad7142-24bb-48ad-8504-9c9231137i110",
  },
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i110",
    createdAt: ISODate("2024-10-18T12:24:50.050Z"),
    institution: {
      address: "sede leg",
      city: "Milano",
      country: "IT",
      county: "MI",
      description: "Techpartner 23-02",
      digitalAddress: "pec@pectest.com",
      geographicTaxonomies: [],
      id: "c2808e95-59a7-44fb-a6df-64faf4ff3ed3",
      imported: false,
      institutionType: "PT",
      origin: "INFOCAMERE",
      originId: "11223345667",
      taxCode: "11223345667",
      zipCode: "11223",
    },
    productId: "prod-io",
    status: "COMPLETED",
    users: [
      {
        id: "ec7ca4d5-c537-462a-8c06-e102e0c21c44",
        role: "MANAGER",
        userMailUuid: "ID_MAIL#49e16aa5-bc28-40c7-9844-6886e4647e22",
      },
    ],
    workflowType: "CONTRACT_REGISTRATION",
  },
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i122",
    createdAt: ISODate("2024-10-18T12:24:50.050Z"),
    institution: {
      address: "sede leg",
      city: "Milano",
      country: "IT",
      county: "MI",
      description: "Techpartner 23-02",
      digitalAddress: "pec@pectest.com",
      geographicTaxonomies: [],
      id: "c2808e95-59a7-44fb-a6df-64faf4ff3ed3",
      imported: false,
      institutionType: "PT",
      origin: "INFOCAMERE",
      taxCode: "11223345661",
      zipCode: "11223",
    },
    productId: "prod-io",
    status: "COMPLETED",
    users: [
      {
        id: "ec7ca4d5-c537-462a-8c06-e102e0c21c44",
        role: "MANAGER",
        userMailUuid: "ID_MAIL#49e16aa5-bc28-40c7-9844-6886e4647e22",
      },
    ],
    workflowType: "CONTRACT_REGISTRATION",
  },
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i1022",
    createdAt: ISODate("2024-10-18T12:24:50.050Z"),
    institution: {
      address: "sede leg",
      city: "Milano",
      country: "IT",
      county: "MI",
      description: "Techpartner 23-02",
      digitalAddress: "pec@pectest.com",
      geographicTaxonomies: [],
      id: "c2808e95-59a7-44fb-a6df-64faf4ff3ed3",
      imported: false,
      institutionType: "PT",
      origin: "INFOCAMERE",
      taxCode: "11223345661",
      zipCode: "11223",
    },
    productId: "prod-io",
    status: "TOBEVALIDATED",
    users: [
      {
        id: "ec7ca4d5-c537-462a-8c06-e102e0c21c44",
        role: "MANAGER",
        userMailUuid: "ID_MAIL#49e16aa5-bc28-40c7-9844-6886e4647e22",
      },
    ],
    workflowType: "FOR_APPROVE",
  },
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i10222",
    createdAt: ISODate("2024-10-18T12:24:50.050Z"),
    institution: {
      address: "sede leg",
      city: "Milano",
      country: "IT",
      county: "MI",
      description: "Techpartner 23-02",
      digitalAddress: "pec@pectest.com",
      geographicTaxonomies: [],
      id: "c2808e95-59a7-44fb-a6df-64faf4ff3ed3",
      imported: false,
      institutionType: "PT",
      origin: "INFOCAMERE",
      taxCode: "11223345661",
      zipCode: "11223",
    },
    productId: "prod-pagopa",
    status: "TOBEVALIDATED",
    users: [
      {
        id: "ec7ca4d5-c537-462a-8c06-e102e0c21c44",
        role: "MANAGER",
        userMailUuid: "ID_MAIL#49e16aa5-bc28-40c7-9844-6886e4647e22",
      },
    ],
    workflowType: "FOR_APPROVE",
  },
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i123",
    createdAt: ISODate("2024-10-18T12:24:50.050Z"),
    institution: {
      address: "sede leg",
      city: "Milano",
      country: "IT",
      county: "MI",
      description: "Techpartner 23-02",
      digitalAddress: "pec@pectest.com",
      geographicTaxonomies: [],
      id: "c2808e95-59a7-44fb-a6df-64faf4ff3ed3",
      imported: false,
      institutionType: "PT",
      origin: "INFOCAMERE",
      taxCode: "11223345661",
      zipCode: "11223",
    },
    productId: "prod-io",
    status: "TOBEVALIDATED",
    users: [
      {
        id: "ec7ca4d5-c537-462a-8c06-e102e0c21c44",
        role: "MANAGER",
        userMailUuid: "ID_MAIL#49e16aa5-bc28-40c7-9844-6886e4647e22",
      },
    ],
    workflowType: "CONTRACT_REGISTRATION",
  },
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i1000",
    createdAt: ISODate("2024-10-18T12:24:50.050Z"),
    institution: {
      address: "sede leg",
      city: "Milano",
      country: "IT",
      county: "MI",
      description: "Techpartner 23-02",
      digitalAddress: "pec@pectest.com",
      geographicTaxonomies: [],
      id: "c2808e95-59a7-44fb-a6df-64faf4ff3ed3",
      imported: false,
      institutionType: "PT",
      origin: "INFOCAMERE",
      taxCode: "11223345661",
      zipCode: "11223",
    },
    productId: "prod-io",
    status: "COMPLETED",
    users: [
      {
        id: "ec7ca4d5-c537-462a-8c06-e102e0c21c44",
        role: "MANAGER",
        userMailUuid: "ID_MAIL#49e16aa5-bc28-40c7-9844-6886e4647e22",
      },
    ],
    workflowType: "CONTRACT_REGISTRATION",
  },
  {
    _id: "37f7609b-5a4b-4200-82e7-2117756d64aa",
    billing: {
      publicServices: false,
      vatNumber: "08875230016",
    },
    createdAt: ISODate("2024-02-23T11:04:09.749Z"),
    expiringDate: ISODate("2024-04-23T11:04:09.749Z"),
    institution: {
      address: "VIA DELLE MAGNOLIE 12, 00184 ROMA",
      city: "Roma",
      country: "IT",
      county: "RM",
      description: "BANCA FINTA SPA",
      digitalAddress: "BANCAFINTASPA@LEGALMAIL.IT",
      imported: false,
      institutionType: "AS",
      origin: "IVASS",
      taxCode: "08875230016",
      zipCode: "20154",
      id: "1ff1d0eb-5022-42d3-8713-e2b81421c503",
    },
    productId: "prod-interop",
    status: "COMPLETED",
    userRequestUid: "c5ae981f-c651-45f9-bb7f-589297c88416",
    users: [
      {
        _id: "ec7ca4d5-c537-462a-8c06-e102e0c21c44",
        productRole: "admin",
        role: "MANAGER",
        userMailUuid: "c5ba6158-da4f-4600-9136-b1315f7510a1",
      },
    ],
    workflowType: "CONTRACT_REGISTRATION",
    updateAt: ISODate("2024-02-23T11:04:16.280Z"),
    updatedAt: ISODate("2024-05-17T12:25:27.963Z"),
  },
  {
  "_id": "5369b559-d2ed-4c79-af12-4a4491fc70f1",
  "createdAt": {
    "$date": "2024-10-18T12:24:50.050Z"
  },
  "institution": {
    "address": "sede leg",
    "city": "Milano",
    "country": "IT",
    "county": "MI",
    "description": "Techpartner 23-02",
    "digitalAddress": "pec@pectest.com",
    "geographicTaxonomies": [],
    "id": "c2808e95-59a7-44fb-a6df-64faf4ff3ed3",
    "imported": false,
    "institutionType": "PT",
    "origin": "ADE",
    "taxCode": "11223345777",
    "zipCode": "11223"
  },
  "productId": "prod-io",
  "status": "COMPLETED",
  "users": [
    {
      "id": "ec7ca4d5-c537-462a-8c06-e102e0c21c44",
      "role": "MANAGER",
      "userMailUuid": "ID_MAIL#49e16aa5-bc28-40c7-9844-6886e4647e22"
    }
  ],
  "workflowType": "CONTRACT_REGISTRATION"
}
]);

db.tokens.insertMany([
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i103",
    onboardingId: "89ad7142-24bb-48ad-8504-9c9231137i103",
    type: "INSTITUTION",
    productId: "prod-io",
    contractTemplate:
      "contracts/template/io/2.4.2/io-accordo_di_adesione-v.2.4.2.html",
    createdAt: new Date("2021-10-20T12:53:34.000Z"),
    updatedAt: new Date("2023-07-25T10:24:08.119Z"),
    contractFilename: "signed_89ad7142-24bb-48ad-8504-9c9231137i103pdf",
  },
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i105",
    onboardingId: "89ad7142-24bb-48ad-8504-9c9231137i105",
    type: "INSTITUTION",
    productId: "prod-io",
    contractTemplate:
      "contracts/template/io/2.4.2/io-accordo_di_adesione-v.2.4.2.html",
    createdAt: new Date("2021-10-20T12:53:34.000Z"),
    updatedAt: new Date("2023-07-25T10:24:08.119Z"),
  },
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137i122",
    onboardingId: "89ad7142-24bb-48ad-8504-9c9231137i122",
    type: "INSTITUTION",
    productId: "prod-io",
    contractTemplate:
      "contracts/template/io/2.4.2/io-accordo_di_adesione-v.2.4.2.html",
    createdAt: new Date("2021-10-20T12:53:34.000Z"),
    updatedAt: new Date("2023-07-25T10:24:08.119Z"),
  },
]);

db = db.getSiblingDB("selcMsCore");

db.Institution.insertOne({
  createdAt: new Date(),
});

db.Institution.insertMany([
  {
    _id: "05e970c2-6393-469f-b178-2999df0b57ba",
    origin: "SELC",
    originId: "00145190922",
    description: "test",
    institutionType: "GPU",
    digitalAddress: "1@1.com",
    address: "via Roma 3",
    zipCode: "23123",
    taxCode: "00145190922",
    city: "Milano",
    county: "MI",
    country: "IT",
    geographicTaxonomies: [
      {
        code: "ITA",
        desc: "ITALIA",
      },
    ],
    rea: "REA-13",
    imported: false,
    createdAt: "2023-01-01T10:00:00.000Z",
    updatedAt: "2024-07-01T12:00:00.000Z",
  },
  {
    _id: "05e970c2-6393-469f-b178-2999df0b57bu",
    origin: "SELC",
    originId: "11223345661",
    description: "test",
    institutionType: "GPU",
    digitalAddress: "1@1.com",
    address: "via Roma 3",
    zipCode: "23123",
    taxCode: "11223345661",
    city: "Milano",
    county: "MI",
    country: "IT",
    geographicTaxonomies: [
      {
        code: "ITA",
        desc: "ITALIA",
      },
    ],
    onboarding: [
      {
        productId: "prod-interop",
        status: "ACTIVE",
        tokenId: "89ad7142-24bb-48ad-8504-9c9231137i1000",
        contract:
          "parties/docs/b0882722-eb57-4ab3-ac1a-d95dac45d741/Accordo Adesione.pdf2115572574547542175.pdf",
        billing: {
          vatNumber: "11223345661",
          recipientCode: "c_d548",
          publicServices: false,
        },
        createdAt: "2022-09-29T19:03:11.523Z",
        updatedAt: "2024-07-05T12:12:07.2534613Z",
        institutionType: "PA",
        origin: "SELC",
        originId: "c_d548",
      },
    ],
    rea: "REA-13",
    imported: false,
    createdAt: "2023-01-01T10:00:00.000Z",
    updatedAt: "2024-07-01T12:00:00.000Z",
  },
]);

db = db.getSiblingDB("selcUser");

db.userInstitutions.insertMany([
  {
    createdAt: new Date(),
  },
  {
   _id: new ObjectId("6894755453f442f61d6b140d"),
   institutionId: "f80ad9b9-90b8-421d-ae8a-11fdab8188f4",
   userId: "97a511a7-2acc-47b9-afed-2f3c65753b4a",
   products: [
      {
        productId: "prod-io",
        relationshipId: "39ba0512-dfe8-4d4f-b799-6172157df5bd",
        tokenId: "f24998e9-15b2-4d50-8aed-d87be21cbe39",
        status: "ACTIVE",
        productRole: "admin",
        role: "MANAGER",
        env: "ROOT",
        createdAt:  new Date(),
        updatedAt:  new Date()
      }
    ],
    institutionDescription: "Comune di Villarbasse",
    userMailUuid: "ID_MAIL#d373e4d6-9a86-4316-9ce1-426ed95d139b"
  }
]);

db = db.getSiblingDB("selcProduct");

db.products.insertMany([
  {
    _id: "89ad7142-24bb-48ad-8504-9c9231137232",
    productId: "prod-test",
    alias: "prod-test",
    title: "Prod TEST",
    description: "Product description",
    status: "TESTING",
    version: 1,
    consumers: [
      "Standard"
    ],
    visualConfiguration: {
      logoUrl: "http://localhost:8080",
      depictImageUrl: "http://localhost:8080",
      logoBgColor: "#0B3EE3"
    },
    features: {
      allowCompanyOnboarding: true,
      allowIndividualOnboarding: false,
      allowedInstitutionTaxCode: [],
      delegable: false,
      invoiceable: true,
      expirationDays: 30,
      enabled: true
    },
    roleMappings: [
      {
        role: "OPERATOR",
        multiroleAllowed: false,
        phasesAdditionAllowed: [],
        skipUserCreation: false,
        backOfficeRoles: [
          {
            code: "referente operativo",
            label: "Operatore",
            description: "Operatore"
          }
        ]
      }
    ],
    contracts: [
      {
        type: "institution",
        institutionType: "DEFAULT",
        path: "contracts/template/io/2.4.6/io-accordo_di_adesione-v.2.4.6.html",
        version: "2.4.6",
        order: 10,
        generated: true,
        mandatory: true,
        name: "Accordo di adesione IO",
        workflowState: "REQUEST",
        workflowType: []
      }
    ],
    institutionOrigins: [
      {
        institutionType: "PA",
        origin: "IPA",
        labelKey: "pa"
      },
      {
        institutionType: "GSP",
        origin: "IPA",
        labelKey: "gsp"
      },
      {
        institutionType: "SCP",
        origin: "SELC",
        labelKey: "scp"
      }
    ],
    emailTemplates: [
      {
        type: "IMPORT",
        institutionType: "DEFAULT",
        path: "contracts/template/mail/import-massivo-io/1.0.0.json",
        version: "1.0.0",
        status: "PENDING"
      }
    ],
    backOfficeEnvironmentConfigurations: [
      {
        env: "Locale",
        urlPublic: "http://localhost:8080",
        urlBO: "http://localhost:8080",
        identityTokenAudience: "locale"
      }
    ],
    metadata: {
      createdBy: "user-apim-name"
    }
  }
]);