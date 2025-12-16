Feature: Contract Template API

  # UPLOAD

  Scenario: Successfully upload a contract template
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | createdBy | testuser  |
    And The following form data:
      | name        | accordo di adesione |
      | version     | 0.0.1               |
      | description | Upload test         |
    And Upload the file at path "blobStorage/contract-template.html" with form key "file" and content type "text/html"
    When I send a POST request to "/contract-template" with form data and multi-part file
    Then The status code is 201
    And The response body contains field "contractTemplateId"
    And The response body contains field "contractTemplatePath"
    And The response body contains:
      | productId               | prod-test           |
      | contractTemplateVersion | 0.0.1               |
      | name                    | accordo di adesione |
      | description             | Upload test         |
      | createdBy               | testuser            |
    And Finally remove the uploaded contract template with name "accordo di adesione" and version "0.0.1" for product "prod-test"

  Scenario: Bad request when uploading a contract template with invalid tags
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | createdBy | testuser  |
    And The following form data:
      | name        | accordo di adesione |
      | version     | 0.0.1               |
      | description | Upload test         |
    And Upload the file at path "request/contract-template-invalid.html" with form key "file" and content type "text/html"
    When I send a POST request to "/contract-template" with form data and multi-part file
    Then The status code is 400

  Scenario: Bad request when uploading a pdf contract template
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | createdBy | testuser  |
    And The following form data:
      | name        | accordo di adesione |
      | version     | 0.0.1               |
      | description | Upload test         |
    And Upload the file at path "blobStorage/contract-template.pdf" with form key "file" and content type "application/pdf"
    When I send a POST request to "/contract-template" with form data and multi-part file
    Then The status code is 400

  Scenario: Bad request when uploading with invalid name
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | createdBy | testuser  |
    And The following form data:
      | name        | accordo & adesione  |
      | version     | 0.0.1               |
      | description | Upload test         |
    And Upload the file at path "blobStorage/contract-template.html" with form key "file" and content type "text/html"
    When I send a POST request to "/contract-template" with form data and multi-part file
    Then The status code is 400

  Scenario: Bad request when uploading with invalid version
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | createdBy | testuser  |
    And The following form data:
      | name        | accordo di adesione |
      | version     | v0.0.1              |
      | description | Upload test         |
    And Upload the file at path "blobStorage/contract-template.html" with form key "file" and content type "text/html"
    When I send a POST request to "/contract-template" with form data and multi-part file
    Then The status code is 400

  Scenario: Bad request when uploading with missing params
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | createdBy | testuser  |
    And The following form data:
      | description | Upload test |
    And Upload the file at path "blobStorage/contract-template.html" with form key "file" and content type "text/html"
    When I send a POST request to "/contract-template" with form data and multi-part file
    Then The status code is 400

  Scenario: Conflict when uploading the same contract template version
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | createdBy | testuser  |
    And The following form data:
      | name        | accordo di adesione |
      | version     | 0.0.2               |
      | description | Upload test         |
    And Upload the file at path "blobStorage/contract-template.html" with form key "file" and content type "text/html"
    When I send a POST request to "/contract-template" with form data and multi-part file
    Then The status code is 409

  # DOWNLOAD

  Scenario: Successfully download html contract template
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
    And The following path params:
      | contractTemplateId | 5ca7f491-5770-40ba-baf0-6fe612bba14e |
    When I send a GET request to "/contract-template/{contractTemplateId}"
    Then The status code is 200
    And The response header contains:
      | Content-Type | text/html |
    And The response body contains the string "<h1>Esempio di Accordo di adesione</h1>"

  Scenario: Successfully download pdf contract template
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | fileType  | pdf       |
    And The following path params:
      | contractTemplateId | 76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6 |
    When I send a GET request to "/contract-template/{contractTemplateId}"
    Then The status code is 200
    And The response header contains:
      | Content-Type | application/pdf |
    And The response body contains the string "%PDF-"

  Scenario: Not found when downloading non-existing contract template type
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
    And The following path params:
      | contractTemplateId | 76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6 |
    When I send a GET request to "/contract-template/{contractTemplateId}"
    Then The status code is 404

  Scenario: Bad request when downloading with invalid file type
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | fileType  | txt       |
    And The following path params:
      | contractTemplateId | 76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6 |
    When I send a GET request to "/contract-template/{contractTemplateId}"
    Then The status code is 400

  # LIST

  Scenario: Successfully list all contract templates by productId
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
    When I send a GET request to "/contract-template"
    Then The status code is 200
    And The response body contains the list "items" of size 2
    And The response body contains at path "items" the following list of objects in any order:
      | contractTemplateId                   | productId | contractTemplateVersion | name                | description                        | createdBy | contractTemplatePath                                                   |
      | 5ca7f491-5770-40ba-baf0-6fe612bba14e | prod-test | 0.0.2                   | accordo di adesione |                                    | testuser  | contract-templates/prod-test/5ca7f491-5770-40ba-baf0-6fe612bba14e.html |
      | 76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6 | prod-test | 0.0.3                   | accordo di adesione | Accordo di adesione di esempio pdf |           | contract-templates/prod-test/76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6.pdf  |

  Scenario: Successfully list contract templates filtered by productId, name
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | name      | ACCORDO   |
    When I send a GET request to "/contract-template"
    Then The status code is 200
    And The response body contains the list "items" of size 2
    And The response body contains at path "items" the following list of objects in any order:
      | contractTemplateId                   | productId | contractTemplateVersion | name                | description                        | createdBy | contractTemplatePath                                                   |
      | 5ca7f491-5770-40ba-baf0-6fe612bba14e | prod-test | 0.0.2                   | accordo di adesione |                                    | testuser  | contract-templates/prod-test/5ca7f491-5770-40ba-baf0-6fe612bba14e.html |
      | 76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6 | prod-test | 0.0.3                   | accordo di adesione | Accordo di adesione di esempio pdf |           | contract-templates/prod-test/76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6.pdf  |

  Scenario: Successfully list contract templates filtered by productId, name: no matches
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | name      | X         |
    When I send a GET request to "/contract-template"
    Then The status code is 200
    And The response body contains the list "items" of size 0

  Scenario: Successfully list contract templates filtered by productId, version
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | version   | 0.0.2     |
    When I send a GET request to "/contract-template"
    Then The status code is 200
    And The response body contains the list "items" of size 1
    And The response body contains at path "items" the following list of objects in any order:
      | contractTemplateId                   | productId | contractTemplateVersion | name                | description | createdBy | contractTemplatePath                                                   |
      | 5ca7f491-5770-40ba-baf0-6fe612bba14e | prod-test | 0.0.2                   | accordo di adesione |             | testuser  | contract-templates/prod-test/5ca7f491-5770-40ba-baf0-6fe612bba14e.html |

  Scenario: Successfully list contract templates filtered by productId, version: no matches
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | version   | 0.0.5     |
    When I send a GET request to "/contract-template"
    Then The status code is 200
    And The response body contains the list "items" of size 0

  Scenario: Successfully list contract templates filtered by productId, name and version
    Given User login with username "j.doe" and password "test"
    And The following query params:
      | productId | prod-test |
      | name      | ACCORDO   |
      | version   | 0.0.3     |
    When I send a GET request to "/contract-template"
    Then The status code is 200
    And The response body contains the list "items" of size 1
    And The response body contains at path "items" the following list of objects in any order:
      | contractTemplateId                   | productId | contractTemplateVersion | name                | description                        | createdBy | contractTemplatePath                                                  |
      | 76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6 | prod-test | 0.0.3                   | accordo di adesione | Accordo di adesione di esempio pdf |           | contract-templates/prod-test/76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6.pdf |

  Scenario: Successfully list contract templates without filters
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/contract-template"
    Then The status code is 200
    And The response body contains the list "items" of size 2
    And The response body contains at path "items" the following list of objects in any order:
      | contractTemplateId                   | productId | contractTemplateVersion | name                | description                        | createdBy | contractTemplatePath                                                   |
      | 5ca7f491-5770-40ba-baf0-6fe612bba14e | prod-test | 0.0.2                   | accordo di adesione |                                    | testuser  | contract-templates/prod-test/5ca7f491-5770-40ba-baf0-6fe612bba14e.html |
      | 76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6 | prod-test | 0.0.3                   | accordo di adesione | Accordo di adesione di esempio pdf |           | contract-templates/prod-test/76f9cfd4-e9ae-4eeb-8ed3-1285cc5968d6.pdf  |
