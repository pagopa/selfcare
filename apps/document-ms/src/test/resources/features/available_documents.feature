Feature: Available documents of the onboarding dossier

  Scenario: Institution onboarding returns signed contracts, update contracts, user contracts and attachments
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/avail-root-onb/available-documents"
    Then The status code is 200
    And The response body contains at path "documents" the following list of objects in any order:
      | name                                   | type        | onboardingId               | rootOnboardingId |
      | signed_contratto_adesione.pdf.p7m      | INSTITUTION | avail-root-onb             | avail-root-onb   |
      | Allegato tecnico                       | ATTACHMENT  | avail-root-onb             | avail-root-onb   |
      | signed_contratto_utente.pdf.p7m        | USER        | avail-user-onb             | avail-root-onb   |
      | signed_contratto_aggiornamento.pdf.p7m | INSTITUTION | avail-update-completed-onb | avail-root-onb   |
    And The response body doesn't contain field "attachments"
    And The response body doesn't contain field "contractFilename"

  Scenario: User onboarding returns only its own signed contract
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/avail-user-onb/available-documents"
    Then The status code is 200
    And The response body contains at path "documents" the following list of objects in any order:
      | name                            | type | onboardingId   | rootOnboardingId |
      | signed_contratto_utente.pdf.p7m | USER | avail-user-onb | avail-root-onb   |

  Scenario: Pending update onboarding without signed contract returns no documents
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/avail-update-pending-onb/available-documents"
    Then The status code is 200
    And The response body contains the list "documents" of size 0

  Scenario: Pending user onboarding without signed contract returns no documents
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/avail-user-pending-onb/available-documents"
    Then The status code is 200
    And The response body contains the list "documents" of size 0

  Scenario: Legacy institution without signed contract returns only the attachment
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/avail-legacy-onb/available-documents"
    Then The status code is 200
    And The response body contains at path "documents" the following list of objects in any order:
      | name            | type       | onboardingId     | rootOnboardingId |
      | Allegato legacy | ATTACHMENT | avail-legacy-onb | avail-legacy-onb |

  Scenario: Not existing onboarding returns an empty documents list
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/v1/documents/avail-not-found-onb/available-documents"
    Then The status code is 200
    And The response body contains the list "documents" of size 0

