@Onboarding
Feature: Onboarding collection

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for workflow FOR_APPROVE_GPU request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status        |
      | TOBEVALIDATED |
      | PENDING       |
      | COMPLETED     |

#  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect workflow FOR_APPROVE_GPU request
#    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e86"
#    When I send a GET request with given onboardingId
#    Then the response should have status code 202
#    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
#    And there is a document for onboarding with status "FAILED"

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct workflow FOR_APPROVE_PT request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e87"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status        |
      | TOBEVALIDATED |
      | COMPLETED     |

#  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect workflow FOR_APPROVE_PT request
#    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e88"
#    When I send a GET request with given onboardingId
#    Then the response should have status code 202
#    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
#    And there is a document for onboarding with status "FAILED"

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct workflow CONFIRMATION request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e89"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status    |
      | PENDING   |
      | COMPLETED |

#  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect workflow CONFIRMATION request
#    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e90"
#    When I send a GET request with given onboardingId
#    Then the response should have status code 202
#    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
#    And there is a document for onboarding with status "FAILED"

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct workflow CONTRACT_REGISTRATION request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9232137e85"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status    |
      | PENDING   |
      | COMPLETED |

#  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect workflow CONTRACT_REGISTRATION request
#    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9232137e97"
#    When I send a GET request with given onboardingId
#    Then the response should have status code 202
#    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
#    And there is a document for onboarding with status "FAILED"


  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct workflow FOR_APPROVE request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8502-9c9232137e95"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status        |
      | TOBEVALIDATED |
      | PENDING       |
      | COMPLETED     |

#  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect workflow FOR_APPROVE request
#    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e99"
#    When I send a GET request with given onboardingId
#    Then the response should have status code 202
#    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
#    And there is a document for onboarding with status "FAILED"

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct workflow USERS request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e91"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status    |
      | PENDING   |
      | COMPLETED |

#  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect workflow USERS request
#    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e92"
#    When I send a GET request with given onboardingId
#    Then the response should have status code 202
#    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
#    And there is a document for onboarding with status "FAILED"

  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for workflow USERS_EA request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e94"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status    |
      | PENDING   |
      | COMPLETED |

#  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect workflow USERS_EA request
#    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e95"
#    When I send a GET request with given onboardingId
#    Then the response should have status code 202
#    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
#    And there is a document for onboarding with status "FAILED"

  Scenario: Verify correct invocation of the StartOnboardingOrchestration for workflow IMPORT request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137i99"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "COMPLETED"

#  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect workflow IMPORT request
#    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137i100"
#    When I send a GET request with given onboardingId
#    Then the response should have status code 202
#    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
#    And there is a document for onboarding with status "FAILED"

  Scenario: Verify correct invocation of the StartOnboardingOrchestration for workflow IMPORT_AGGREGATION request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "3d8e6b10-2a4f-4c7e-9b51-8f0a1c2d3e01"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "COMPLETED"
    And the aggregate onboarding with taxCode "90000000029" has status "COMPLETED"
    And there is an active delegation from the aggregate to the aggregator

  Scenario: Increment an aggregator with a new and an already delegated institution
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "a14d68d1-1b61-4ea6-9ea1-716ea97f6866"
    And the aggregate with taxCode "90000000052" already has an active delegation
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "COMPLETED"
    And the onboarding has an activation date
    And the aggregator user is active for aggregates
    And the aggregate onboarding with taxCode "90000000045" has status "COMPLETED"
    And there is an active delegation from the aggregate to the aggregator
    And the aggregate with taxCode "90000000052" has no new onboarding or delegation

  Scenario: Complete an aggregate onboarding with an assigned manager
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "9f4b1c65-90ad-4b50-9dbb-686500000001"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "COMPLETED"
    And the onboarding has an activation date
    And the institution "7c3f1a52-6d2e-4b8a-9f10-3e5d7a9b2c46" has an active onboarding for the product
    And there is an active delegation from the aggregate to the aggregator
    And the aggregate user is active for the product

  Scenario: Complete an aggregate onboarding without assigned users
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "9f4b1c65-90ad-4b50-9dbb-686500000002"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "COMPLETED"
    And the onboarding has an activation date
    And the institution "7c3f1a52-6d2e-4b8a-9f10-3e5d7a9b2c47" has an active onboarding for the product
    And there is an active delegation from the aggregate to the aggregator
    And the aggregate has no users

  Scenario: Verify correct invocation of the StartOnboardingOrchestration for correct workflow USERS_PG request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e102"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "COMPLETED"

#  Scenario: Verify correct invocation of the StartOnboardingOrchestration for incorrect workflow USERS_PG request
#    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137i103"
#    When I send a GET request with given onboardingId
#    Then the response should have status code 202
#    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
#    And there is a document for onboarding with status "FAILED"


  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct workflow CONTRACT_REGISTRATION request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "190e3ca4-8de4-4f90-ad2c-51b112531bb6"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status    |
      | PENDING   |
      | COMPLETED |


  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct workflow CONTRACT_REGISTRATION request with institutionType SCEC
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "e49167f4-d9f3-475f-8814-9c0d81e316d2"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status    |
      | PENDING   |
      | COMPLETED |


  Scenario Outline: Verify correct invocation of the StartOnboardingOrchestration for correct workflow CONTRACT_WITH_COUNTERSIGNATURE request
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "190e3ca4-8de4-4f90-ad2c-51b1125317b6"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
    And there is a document for onboarding with status "<status>"

    Examples:
      | status    |
      | PENDING   |
      | PENDING_IN_REVIEW |
#      | COMPLETED |

  Scenario: Verify correct invocation of the StartOnboardingOrchestration for workflow CONTRACT_UPDATE request
    Given The onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9a01" is present
    And Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9a02"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And there is a document for onboarding with status "PENDING"
    And the contract document of the onboarding has rootOnboardingId "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9a01"
    And the signed contract of the onboarding is uploaded
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And there is a document for onboarding with status "COMPLETED"
    And the referenced onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9a01" is aligned with the completed contract update

  Scenario: Completing a CONTRACT_UPDATE overrides only the other pending contract updates of the same adhesion
    Given The onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9b01" is present
    And The onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9b04" is present
    And Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9b02"
    When I send a GET request with given onboardingId
    Then there is a document for onboarding with status "PENDING"
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9b03"
    When I send a GET request with given onboardingId
    Then there is a document for onboarding with status "PENDING"
    And the signed contract of the onboarding is uploaded
    When I send a GET request with given onboardingId
    Then there is a document for onboarding with status "COMPLETED"
    And the onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9b02" has status "OVERRIDDEN"
    And the onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9b04" has status "PENDING"
    And the referenced onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9b01" is aligned with the completed contract update

  Scenario: Revoking an adhesion deletes the contracts of its contract updates and rejects the pending ones
    Given The onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9c01" is present
    And Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9c02"
    When I send a GET request with given onboardingId
    Then there is a document for onboarding with status "PENDING"
    And the signed contract of the onboarding is uploaded
    When I send a GET request with given onboardingId
    Then there is a document for onboarding with status "COMPLETED"
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9c03"
    When I send a GET request with given onboardingId
    Then there is a document for onboarding with status "PENDING"
    And the signed contract of the onboarding is uploaded
    Given Preparing the invocation of "TriggerDeleteInstitutionAndUser" HTTP call with onboardingId "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9c01"
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9c03" has status "REJECTED"
    And the onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9c02" has status "COMPLETED"
    And the contract of the onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9c02" has been moved to the deleted storage
    And the contract of the onboarding "5f1c7e2a-9b3d-4c8e-a1f0-2d6b8c4e9c03" has been moved to the deleted storage

  Scenario: Reject StartOnboardingOrchestration without Authorization
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85"
    And the request has no Authorization header
    When I send a GET request with given onboardingId
    Then the response should have status code 401
    And the response header "WWW-Authenticate" should be "Bearer"
    And the onboarding status should not change

  Scenario: Reject StartOnboardingOrchestration with a malformed bearer token
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85"
    And the request has a malformed bearer token
    When I send a GET request with given onboardingId
    Then the response should have status code 401
    And the response header "WWW-Authenticate" should be "Bearer"
    And the onboarding status should not change

  Scenario: Reject StartOnboardingOrchestration when token and header tenants differ
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85"
    And the request token belongs to tenant "PNPG"
    When I send a GET request with given onboardingId
    Then the response should have status code 401
    And the response header "WWW-Authenticate" should be "Bearer"
    And the onboarding status should not change

  Scenario: Reject StartOnboardingOrchestration without a tenant header
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85"
    And the request has no tenant header
    When I send a GET request with given onboardingId
    Then the response should have status code 400
    And the onboarding status should not change

  Scenario: Reject StartOnboardingOrchestration with an unsupported tenant header
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85"
    And the request tenant header is "FAKE-TENANT"
    When I send a GET request with given onboardingId
    Then the response should have status code 400
    And the onboarding status should not change

  Scenario: Accept StartOnboardingOrchestration with a token without tenant_id claim attributed to the default tenant
    Given Preparing the invocation of "StartOnboardingOrchestration" HTTP call with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85"
    And the request token has no tenant_id claim
    When I send a GET request with given onboardingId
    Then the response should have status code 202
    And the answer should contain "id,purgeHistoryDeleteUri,restartPostUri"
