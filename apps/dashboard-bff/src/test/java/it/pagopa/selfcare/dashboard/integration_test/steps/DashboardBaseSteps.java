package it.pagopa.selfcare.dashboard.integration_test.steps;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import it.pagopa.selfcare.dashboard.integration_test.model.Filter;
import it.pagopa.selfcare.dashboard.integration_test.model.JwtData;
import it.pagopa.selfcare.dashboard.integration_test.model.TestProperties;
import it.pagopa.selfcare.dashboard.integration_test.utils.KeyGenerator;
import it.pagopa.selfcare.dashboard.model.delegation.Order;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DashboardBaseSteps{

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private  DashboardStepsUtil dashboardStepsUtil;

    @Given("user login with username {string} and password {string}")
    public void login(String user, String pass) {
        dashboardStepsUtil.filter = new Filter();
        TestProperties testProperties = readDataPopulation();
        JwtData jwtData = testProperties.getJwtData().stream()
                .filter(data -> data.getUsername().equals(user) && data.getPassword().equals(pass))
                .findFirst()
                .orElse(null);
        dashboardStepsUtil.token = generateToken(jwtData);
    }

    @Given("A bad jwt token")
    public void badToken() {
        dashboardStepsUtil.filter = new Filter();
        dashboardStepsUtil.token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Imp3dF9hMjo3YTo0NjozYjoyYTo2MDo1Njo0MDo4ODphMDo1ZDphNDpmODowMToxZTozZSJ9.eyJmYW1pbHlfbmFtZSI6IlNhcnRvcmkiLCJmaXNjYWxfbnVtYmVyIjoiU1JUTkxNMDlUMDZHNjM1UyIsIm5hbWUiOiJBbnNlbG1vIiwic3BpZF9sZXZlbCI6Imh0dHBzOi8vd3d3LnNwaWQuZ292Lml0L1NwaWRMMiIsImZyb21fYWEiOmZhbHNlLCJ1aWQiOiI1MDk2ZTRjNi0yNWExLTQ1ZDUtOWJkZi0yZmI5NzRhN2MxYzgiLCJsZXZlbCI6IkwyIiwiaWF0IjoxNzM5MzYxMzUzLCJhdWQiOiJhcGkuZGV2LnNlbGZjYXJlLnBhZ29wYS5pdCIsImlzcyI6IlNQSUQiLCJqdGkiOiJfOWE2M2ZiNTQyYzk4NDJjZWMyNmQifQ.X9zoPHuLq6GafRM6zV6hnN09SQ1rL0rFWK5d-RfwJACHam1nPjqX6INx9Qd-_E69GFlr4O1JzzIzc3wfnbIhRlKMVTLjw5xjadc_sxoq-6sH-8Ek_aPeWqL44m_RKcngFCzh-7KrD32wrh4fyC_tdhFbS0SSWjTLgDy0mn3gGPLwFGmv2ASW7xZvw-DfQpsNZhEDJAOQgQ4qC5Lyxo_RriBHDIq1pZvtmW6RkIYsLJ8EGNoOGM4SzUOM3ZSSieh-48DLb8HsDwJgrle6gJJZoqZ0saeAN-7Gy-q55tl3E0hLhfif81RQ_nFH7nc3I9kLffaxWfpH7Oym5F3Nur-btg";
    }

    @And("The response body contains:")
    public void checkResponseBody(Map<String, String> expectedKeyValues) {
        expectedKeyValues.forEach((expectedKey, expectedValue) -> {
            final String currentValue = dashboardStepsUtil.getResponse().body().jsonPath().getString(expectedKey);
            Assertions.assertEquals(expectedValue, currentValue, String.format("The field %s does not contain the expected value", expectedKey));
        });
    }

    @And("The response body contains the list {string} of size {int}")
    public void checkResponseBodyListSize(String expectedJsonPath, int expectedSize) {
        final int currentSize = dashboardStepsUtil.getResponse().body().jsonPath().getList(expectedJsonPath).size();
        Assertions.assertEquals(expectedSize, currentSize);
    }


    @And("The response body contains at path {string} the following list of objects in any order:")
    public void checkResponseBodyObjectList(String expectedJsonPath, List<Map<String, Object>> expectedObjects) {
        List<Map<String, Object>> currentObjects = dashboardStepsUtil.getResponse().body().jsonPath().getList(expectedJsonPath);
        Assertions.assertEquals(expectedObjects.size(), currentObjects.size(), String.format("The lists have different sizes. Expected: %s, Current: %s", expectedObjects, currentObjects));
        ObjectMapper objMapper = new ObjectMapper();
        Set<String> expectedKeySet = ((Map)expectedObjects.get(0)).keySet();
        Set<String> expectedJsonSet = (Set)expectedObjects.stream().map(obj -> {
            return this.filterExpectedObject(obj, expectedKeySet);
        }).map(obj -> {
            return this.toJson(objMapper, obj);
        }).collect(Collectors.toSet());
        Set<String> currentJsonSet = (Set)currentObjects.stream().map(obj -> {
            return this.filterCurrentObject(obj, expectedKeySet);
        }).map(obj -> {
            return this.toJson(objMapper, obj);
        }).collect(Collectors.toSet());
        Assertions.assertEquals(expectedJsonSet, currentJsonSet, String.format("The lists contain different objects. Expected: %s, Current: %s", expectedJsonSet, currentJsonSet));
    }

    @And("The response body doesn't contain field {string}")
    public void checkResponseBodyMissingKey(String expectedJsonPath) {
        final String currentValue = dashboardStepsUtil.getResponse().body().jsonPath().getString(expectedJsonPath);
        Assertions.assertNull(currentValue);
    }

    public TestProperties readDataPopulation() {
        TestProperties testProperties = null;
        try {
            testProperties = objectMapper.readValue(new File("src/test/resources/dataPopulation/data.json"), new TypeReference<>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return testProperties;
    }

    public String generateToken(JwtData jwtData) {
        if (Objects.nonNull(jwtData)) {
            try {
                File file = new File("integration-test-config/key/private-key.pem");
                Algorithm alg = Algorithm.RSA256(KeyGenerator.getPrivateKey(new String(Files.readAllBytes(file.toPath()))));
                String jwt = JWT.create()
                        .withHeader(jwtData.getJwtHeader())
                        .withPayload(jwtData.getJwtPayload())
                        .withIssuedAt(Instant.now())
                        .withExpiresAt(Instant.now().plusSeconds(3600))
                        .sign(alg);
                log.info("generated token jwt");
                return jwt;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @And("the productId is {string}")
    public void theProductIdIs(String productId) {
        dashboardStepsUtil.filter.setProductId(productId);
    }

    @And("the environment is {string}")
    public void theEnvironmentIs(String env) {
        dashboardStepsUtil.filter.setEnvironment(env);
    }

    @And("the institutionType is {string}")
    public void theInstitutionTypeIs(String institutionType) {
        dashboardStepsUtil.filter.setInstitutionType(institutionType);
    }

    @Given("the userId is {string}")
    public void asUserId(String userId) {
        dashboardStepsUtil.filter.setUserId(userId);
    }

    @And("the productRoles are {string}")
    public void theProductRoleIs(String productRoles) {
        String[] fieldsArray = productRoles.split(",");
        dashboardStepsUtil.filter.setProductRoles(List.of(fieldsArray));
    }

    @And("the fiscalCode is {string}")
    public void theFiscalCodeIs(String taxCode) {
        dashboardStepsUtil.filter.setTaxCode(taxCode);
    }

    @And("the attachmentName is {string}")
    public void theAttachmentNameIs(String attachmentName) {
        dashboardStepsUtil.filter.setName(attachmentName);
    }

    @And("the fields are {string}")
    public void theFieldsAre(String fields) {
        String[] fieldsArray = fields.split(",");
        dashboardStepsUtil.filter.setFields(List.of(fieldsArray));
    }

    @And("the mobilePhone is {string}")
    public void theMobilePhoneIs(String mobilePhone) {
        dashboardStepsUtil.filter.setMobilePhone(mobilePhone);
    }


    @And("the email is {string}")
    public void theEmailIs(String email) {
        dashboardStepsUtil.filter.setEmail(email);
    }


    @And("the roles are {string}")
    public void theRolesAre(String roles) {
        String[] fieldsArray = roles.split(",");
        dashboardStepsUtil.filter.setRoles(List.of(fieldsArray));
    }

    @And("the states are {string}")
    public void theStatesAre(String roles) {
        String[] fieldsArray = roles.split(",");
        dashboardStepsUtil.filter.setStates(List.of(fieldsArray));
    }

    @And("the language is {string}")
    public void languageIs(String lang) {
        dashboardStepsUtil.filter.setLang(lang);
    }


    @And("the institutionId is {string}")
    public void theInstitutionIdIs(String institutionId) {
        dashboardStepsUtil.filter.setInstitutionId(institutionId);
    }

    @And("I have groupId {string}")
    public void iHaveGroupId(String groupId) {
        dashboardStepsUtil.filter.setGroupId(groupId);
    }

    @And("I have memberId {string}")
    public void iHaveMemberID(String memberId) {
        dashboardStepsUtil.filter.setUserId(memberId);
    }

    @And("require page {int} and size {int} and order by {string}")
    public void requirePageAndSizeAndOrderBy(int page, int size, String order) {
        dashboardStepsUtil.filter.setPage(page);
        dashboardStepsUtil.filter.setSize(size);
        dashboardStepsUtil.filter.setOrder(Order.valueOf(order));

    }

    @And("I have {string} as search filter")
    public void iHaveAsSearchFilter(String search) {
        dashboardStepsUtil.filter.setSearch(search);
    }

    @And("I have {string} as order")
    public void iHaveAsOrder(String order) {
        dashboardStepsUtil.filter.setOrder(Order.valueOf(order));
    }

    @And("I have {int} as size filter")
    public void iHaveAsSizeFilter(int size) {
        dashboardStepsUtil.filter.setSize(size);
    }


    @And("I set the page number to {int} and page size to {int}")
    public void iSetThePageNumberToAndPageSizeTo(int page, int size) {
        dashboardStepsUtil.filter.setPageable(Pageable.ofSize(size).withPage(page));
    }

    @Given("I have a filter with sorting by {string}")
    public void iHaveAFilterWithSortingBy(String sort) {
        dashboardStepsUtil.filter.setSort(sort);
    }


    @And("the response should contain an error message {string}")
    public void verifyErrorMessage(String expectedErrorMessage) {
        String[] errorMessageArray = expectedErrorMessage.split(",");
        Arrays.stream(errorMessageArray).forEach(s -> Assertions.assertTrue(dashboardStepsUtil.errorMessage.contains(s)));
    }

    @Then("the response status should be {int}")
    public void verifyResponseStatus(int expectedStatusCode) {
        Assertions.assertEquals(expectedStatusCode, dashboardStepsUtil.status);
    }

    @And("the products are {string}")
    public void theProductsAre(String roles) {
        String[] fieldsArray = roles.split(",");
        dashboardStepsUtil.filter.setProducts(List.of(fieldsArray));
    }

    @And("The response body is a list of size {int}")
    public void checkResponseBodySize(int expectedSize) {
        List<?> responseList = dashboardStepsUtil.getResponse().body().jsonPath().getList("$");
        Assertions.assertEquals(expectedSize, responseList.size());
    }

    @And("the response should contain a {string} query param for language")
    public void theResponseShouldContainAValidLanguage(String lang) {
        String uri = dashboardStepsUtil.responses.getBackOfficeUrl().toString();
        Assertions.assertTrue(uri.contains("lang="+lang));
    }

    private Map<String, Object> filterExpectedObject(Map<String, Object> obj, Set<String> expectedKeys) {
        Map<String, Object> filteredMap = new HashMap();
        Iterator var4 = expectedKeys.iterator();

        while(var4.hasNext()) {
            String key = (String)var4.next();
            if (obj.containsKey(key) && Objects.nonNull(obj.get(key))) {
                Object value = obj.get(key);
                if (value instanceof String) {
                    if (Boolean.TRUE.toString().equalsIgnoreCase((String)value)) {
                        value = true;
                    } else if (Boolean.FALSE.toString().equalsIgnoreCase((String)value)) {
                        value = false;
                    }
                }

                filteredMap.put(key, value);
            }
        }

        return filteredMap;
    }

    private Object getNestedValue(Map<String, Object> obj, String key) {
        String[] keys = key.split("\\.");
        Object value = obj;
        String[] var5 = keys;
        int var6 = keys.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            String k = var5[var7];
            if (!(value instanceof Map)) {
                return null;
            }

            value = ((Map)value).get(k);
            if (value == null) {
                return null;
            }
        }

        return value;
    }

    private Map<String, Object> filterCurrentObject(Map<String, Object> obj, Set<String> expectedKeys) {
        Map<String, Object> filteredMap = new HashMap();
        Iterator var4 = expectedKeys.iterator();

        while(var4.hasNext()) {
            String key = (String)var4.next();
            Object value = this.getNestedValue(obj, key);
            if (value != null) {
                filteredMap.put(key, value);
            }
        }

        return filteredMap;
    }

    private String toJson(ObjectMapper objectMapper, Map<String, Object> obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException var4) {
            throw new RuntimeException("Error converting object to JSON", var4);
        }
    }
}
