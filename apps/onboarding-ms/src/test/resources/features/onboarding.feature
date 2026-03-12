@Onboarding
Feature: Onboarding collection

  Scenario: Successfully update recipient code by onboarding id
    Given I have an onboarding record with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e85" the current recipient code is "RC001"
    When I send a PUT request to "/{onboardingId}/recipient-code" with "89ad7142-24bb-48ad-8504-9c9231137e85" and "RC001"
    Then the response status code should be 204

  Scenario: Can't update recipient code with invalid onboardingId
    Given I have an onboarding record with onboardingId "89ad7142-24bb-48ad-8504-9c9231137e90" the current recipient code is "RC001"
    When I send a PUT request to "/{onboardingId}/recipient-code" with "89ad7142-24bb-48ad-8504-9c9231137e90" and "RC001"
    Then the response status code should be 400

  Scenario: Can't perform onboarding request for an empty object
    Given I have an empty request object
    When I send a POST request to "/pa" with empty body
    Then the response status code should be 400

  Scenario: Can't perform onboarding request for an invalid object
    Given I have a request object named "invalid_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "institutionData is required"

  Scenario: Can't perform onboarding request with missing origin and originId
    Given I have a request object named "empty_origin_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "origin is required"
    And the response should contain the text "originId is required"

  Scenario: Can't perform onboarding request with missing digitalAddress attribute
    Given I have a request object named "empty_pec_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "digitalAddress is required"

  Scenario: Successfully store onboarding in status REQUEST
    Given I have a request object named "success_pa_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"

  Scenario: Can't perform onboarding with parent not completed
    Given I have a request object named "invalid_child_product_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Parent product prod-io not onboarded for institution having externalId 83001010616"

  Scenario: Can't perform onboarding for the same product, taxCode and workflowType
    Given I have a request object named "duplicated_onboarding_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 409

  Scenario: Can't perform onboarding for prod-io-premium with not allowed pricingPlan
    Given I have a request object named "invalid_io_premium_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "onboarding pricing plan for io-premium is not allowed"

  Scenario: Can't perform onboarding for GSP without additional information
    Given I have a request object named "invalid_gsp_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Additional Information is required when institutionType is GSP and productId is pagopa"

  Scenario: Can't perform onboarding for GSP without note field
    Given I have a request object named "invalid_gsp_note_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Other Note is required when other boolean are false"

  Scenario: Can't perform onboarding request for invalid digitalAddress (different from IPA registry one)
    Given I have a request object named "invalid_proxy_pec_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Field digitalAddress or description are not valid"

  Scenario: Can't perform onboarding request for invalid recipient code (different from IPA registry one)
    Given I have a request object named "invalid_proxy_recipient_code_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 404
    And the response should contain the text "UO UFO5PP not found"

  Scenario: Successfully perform onboarding request for AOO
    Given I have a request object named "success_aoo_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"

  Scenario: Successfully perform onboarding request for UO
    Given I have a request object named "success_uo_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"

  Scenario: Can't perform onboarding request for UO with invalid recipientCode
    Given I have a request object named "invalid_recipient_code_uo_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Field digitalAddress or description are not valid for institution with taxCode=83001010616 and subunitCode=UF9UPF"

  Scenario: Can't perform onboarding request for UO with denied billing
    Given I have a request object named "invalid_billing_uo_request"
    When I send a POST request to "/pa" with this request
    Then the response status code should be 400
    And the response should contain the text "Field digitalAddress or description are not valid for institution with taxCode=83001010616 and subunitCode=RSRFHL"

  Scenario: Successfully store onboarding in status REQUEST
    Given I have a request object named "success_gsp_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "FOR_APPROVE"

  Scenario: Can't perform onboarding request for GSP not IPA with Invalid workflow type
    Given I have a request object named "invalid_gsp_selc_request"
    When I send a POST request to "" with this request
    Then the response status code should be 400
    And the response body should not be empty
    And the response should contain the text "Invalid workflow type for origin SELC"

  Scenario: Successfully store onboarding in status REQUEST
    Given I have a request object named "success_con_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "FOR_APPROVE"

  Scenario: Successfully store onboarding in status REQUEST
    Given I have a request object named "success_rec_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "FOR_APPROVE"

  Scenario: Successfully store onboarding in status REQUEST
    Given I have a request object named "success_prv_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "FOR_APPROVE"

  #Scenario: Successfully store onboarding in status PENDING
   # Given I have a request object named "success_pg_request"
   # When I send a POST request for PNPG to "/pg/completion" with this request
   # Then the response status code should be 200
   # And the response body should not be empty
   # And the response should have field "status" with value "PENDING"

  Scenario: Can't perform onboarding request for PG cause institution is not into registry
    Given I have a request object named "institution_not_into_registry_request"
    When I send a POST request for PNPG to "/pg/completion" with this request
    Then the response status code should be 400
    And the response body should not be empty
    And the response should contain the text "Institution with taxCode 27937810870 is not into registry"

  Scenario: Can't perform onboarding request for PG with invalid institution request
    Given I have a request object named "invalid_tax_code_pg_request"
    When I send a POST request for PNPG to "/pg/completion" with this request
    Then the response status code should be 400
    And the response body should not be empty
    And the response should contain the text "taxCode is required"

  Scenario: Can't perform onboarding request for PG with invalid institution request
    Given I have a request object named "invalid_user_pg_request"
    When I send a POST request for PNPG to "/pg/completion" with this request
    Then the response status code should be 400
    And the response body should not be empty
    And the response should contain the text "at least one user is required"

  Scenario: Can't perform onboarding request for PG with invalid institution request
    Given I have a request object named "invalid_product_pg_request"
    When I send a POST request for PNPG to "/pg/completion" with this request
    Then the response status code should be 400
    And the response body should not be empty
    And the response should contain the text "productId is required"

  Scenario: Can't perform onboarding request for PG with invalid institution request
    Given I have a request object named "invalid_origin_pg_request"
    When I send a POST request for PNPG to "/pg/completion" with this request
    Then the response status code should be 400
    And the response body should not be empty
    #And the response should contain the text "non deve essere null"

  Scenario: Can't perform onboarding request for PG with invalid institution request
    Given I have a request object named "invalid_digital_address_pg_request"
    When I send a POST request for PNPG to "/pg/completion" with this request
    Then the response status code should be 400
    And the response body should not be empty
    #And the response should contain the text "non deve essere null"

  Scenario: Successfully store onboarding for SA in status REQUEST
    Given I have a request object named "success_sa_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "CONTRACT_REGISTRATION"

  Scenario: Can't perform onboarding request for AS with invalid originId
    Given I have a request object named "invalid_origin_as_request"
    When I send a POST request to "" with this request
    Then the response status code should be 404
    And the response should contain the text "Insurance A113P not found"

  Scenario: Can't perform onboarding request for SA with invalid digitalAddress
    Given I have a request object named "invalid_sa_request"
    When I send a POST request to "" with this request
    Then the response status code should be 400
    And the response should contain the text "Field digitalAddress or description are not valid"

  Scenario: Successfully store onboarding for foreing SA in status REQUEST
    Given I have a request object named "success_foreign_as_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "CONTRACT_REGISTRATION"

  Scenario: Successfully store onboarding for foreing AS in status REQUEST
    Given I have a request object named "success_as_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "CONTRACT_REGISTRATION"

  Scenario: Can't perform onboarding request for SA with invalid originId
    Given I have a request object named "invalid_origin_sa_request"
    When I send a POST request to "" with this request
    Then the response status code should be 404
    And the response should contain the text "Station 02130120745 not found"

  Scenario: Can't perform onboarding request for AS with invalid digitalAddress
    Given I have a request object named "invalid_as_request"
    When I send a POST request to "" with this request
    Then the response status code should be 400
    And the response should contain the text "Field digitalAddress or description are not valid"

  Scenario: Successfully store onboarding for GPU in status REQUEST
    Given I have a request object named "success_gpu_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "FOR_APPROVE_GPU"

  #Scenario: Can't perform onboarding request for UO Aggregate
   # Given I have a request object named "invalid_aggregate_pa_request"
   # When I send a POST request to "/pa/aggregation" with this request
   # Then the response status code should be 400
   # And the response body should not be empty
   # And the response should contain the text "Field digitalAddress or description are not valid for institution with taxCode=83001010616 and subunitCode=RSRFHL"

  #Scenario: Successfully store onboarding in status REQUEST
   # Given I have a request object named "success_aggregation_pa_request"
   # When I send a POST request to "/pa/aggregation" with this request
   # Then the response status code should be 200
   # And the response body should not be empty
   # And the response should have field "status" with value "REQUEST"
   # And the response should have field "workflowType" with value "CONTRACT_REGISTRATION_AGGREGATOR"

  Scenario: Successfully store onboarding in status REQUEST
    Given I have a request object named "success_aggregation_gpu_request"
    When I send a POST request to "/aggregation/completion" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "CONFIRMATION_AGGREGATOR"

  Scenario: Successfully store onboarding for PSP in status REQUEST
    Given I have a request object named "success_psp_request"
    When I send a POST request for PSP to "/psp" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "FOR_APPROVE"

  Scenario: Can't perform onboarding request for PSP with missing pspData
    Given I have a request object named "invalid_psp_request"
    When I send a POST request for PSP to "/psp" with this request
    Then the response status code should be 400
    And the response should contain the text "Field 'pspData' is required for PSP institution onboarding"

  Scenario: Can't perform onboarding prod-dashboard-psp with parent not completed
    Given I have a request object named "invalid_child_dashboard_psp_request"
    When I send a POST request for PSP to "/psp" with this request
    Then the response status code should be 400
    And the response should contain the text "Parent product prod-pagopa not onboarded for institution having externalId 12312312341"

  Scenario: Can't perform onboarding import PSP with missing contract data
    Given I have a request object named "invalid_import_psp_request"
    When I send a POST request for import PSP to "/psp/import" with this request
    Then the response status code should be 400

  Scenario: Successfully store onboarding for import PSP in status REQUEST
    Given I have a request object named "success_import_psp_request"
    When I send a POST request for import PSP to "/psp/import" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "PENDING"
    And the response should have field "workflowType" with value "IMPORT"

  Scenario: Successfully store onboarding for PT in status REQUEST
    Given I have a request object named "success_pt_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "FOR_APPROVE_PT"

  Scenario: Can't perform onboarding request for PT with product not delegable
    Given I have a request object named "invalid_pt_request"
    When I send a POST request to "" with this request
    Then the response status code should be 400
    And the response should contain the text "Institution with external id '83001010616' is not allowed to onboard 'prod-io' product because it is not delegable"

  Scenario: Can't perform onboarding users request with missing product
    Given I have a request object named "invalid_empty_product_request"
    When I send a POST request for user to "/users" with this request
    Then the response status code should be 400
    And the response should contain the text "productId is required"

  Scenario: Can't perform onboarding users request with missing origin
    Given I have a request object named "invalid_empty_origin_request"
    When I send a POST request for user to "/users" with this request
    Then the response status code should be 400
    And the response should contain the text "origin is required"

  Scenario: Can't perform user request for not existing institution
    Given I have a request object named "invalid_user_request"
    When I send a POST request for user to "/users" with this request
    Then the response status code should be 404
    And the response should contain the text "Institution with taxCode 83001010616 origin IPA originId c_h423 subunitCode null not found"

  Scenario: Can't perform user request for not existing onboarding
    Given I have a request object named "invalid_missing_onboarding_request"
    When I send a POST request for user to "/users" with this request
    Then the response status code should be 404
    And the response should contain the text "Onboarding for taxCode 00095990644, origin IPA, originId c_a489, productId prod-io, subunitCode null not found"

  Scenario: Successfully store onboarding user in status REQUEST
    Given I have a request object named "success_user_request"
    When I send a POST request for user to "/users" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "USERS"
    And there is a document for onboardings with origin "IPA" originId "c_l186" and workflowType "USERS"

  Scenario: Successfully store onboarding for SCP in status REQUEST
    Given I have a request object named "success_scp_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "FOR_APPROVE"

  #Scenario: Can't perform onboarding request for SCP when data into PDND are not equals what we expected
   # Given I have a request object named "invalid_case1_scp_request"
   # When I send a POST request to "" with this request
   # Then the response status code should be 400
   # And the response should contain the text "Field digitalAddress or description are not valid"

  Scenario: Can't perform onboarding request for SCP when taxcode are not into PDND
    Given I have a request object named "invalid_case2_scp_request"
    When I send a POST request to "" with this request
    Then the response status code should be 404
    And the response should contain the text "Institution 153763712211 not found in the registry"

  Scenario: Successfully store onboarding for import PRV in status REQUEST
    Given I have a request object named "success_import_prv_request"
    When I send a POST request for import PRV to "/import" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "PENDING"
    And the response should have field "workflowType" with value "IMPORT"

  Scenario: Successfully store onboarding PRV with valid iban in status REQUEST
    Given I have a request object named "success_iban_prv_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "CONTRACT_REGISTRATION"
    And there is a document for onboardings with origin "PDND_INFOCAMERE" originId "15376371009" and workflowType "CONTRACT_REGISTRATION"

  Scenario: Can't perform onboarding PRV request with invalid iban
    Given I have a request object named "invalid_iban_prv_request"
    When I send a POST request to "" with this request
    Then the response status code should be 400
    And the response should contain the text "IBAN is not in an Italian format or is not 27 characters long"

  Scenario: Successfully store onboarding in status REQUEST with institutionType SCEC
    Given I have a request object named "success_scec_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "CONTRACT_REGISTRATION"

  Scenario: Can't perform onboarding PRV merchant request with null ATECO codes
    Given I have a request object named "invalid_ateco_null_prv_request"
    When I send a POST request to "" with this request
    Then the response status code should be 400
    And the response should contain the text "Institution must have at least one ATECO code"

  Scenario: Can't perform onboarding PRV merchant request with invalid ATECO codes
    Given I have a request object named "invalid_ateco_prv_request"
    When I send a POST request to "" with this request
    Then the response status code should be 400
    And the response should contain the text "Institution ATECO codes from PDND Visura are not allowed for this product"

  Scenario: Can't perform onboarding PRV merchant request with invalid ATECO codes
    Given I have a request object named "invalid_ateco_not_match_request"
    When I send a POST request to "" with this request
    Then the response status code should be 400
    And the response should contain the text "Institution ATECO codes from request doesn't match with ATECO codes from PDND Visura"

  Scenario: Successfully store onboarding PRV merchant in status REQUEST with valid ATECO codes
    Given I have a request object named "success_ateco_valid_prv_request"
    When I send a POST request to "" with this request
    Then the response status code should be 200
    And the response body should not be empty
    And the response should have field "status" with value "REQUEST"
    And the response should have field "workflowType" with value "CONTRACT_REGISTRATION"