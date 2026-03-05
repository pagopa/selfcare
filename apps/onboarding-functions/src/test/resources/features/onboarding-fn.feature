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