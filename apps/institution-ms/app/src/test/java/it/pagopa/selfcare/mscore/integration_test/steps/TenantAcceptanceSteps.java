package it.pagopa.selfcare.mscore.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import it.pagopa.selfcare.cucumber.utils.TestDataProvider;
import it.pagopa.selfcare.cucumber.utils.TestJwtGenerator;
import it.pagopa.selfcare.cucumber.utils.model.JwtData;
import it.pagopa.selfcare.mscore.connector.dao.DelegationRepository;
import it.pagopa.selfcare.mscore.connector.dao.InstitutionRepository;
import it.pagopa.selfcare.mscore.connector.dao.MailNotificationConnectorImpl;
import it.pagopa.selfcare.mscore.connector.dao.MailNotificationRepository;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.MailNotificationEntity;
import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.constant.Origin;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;

public class TenantAcceptanceSteps {
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String INSTITUTION_ID = "tenant-acceptance-shared-institution";
    private static final String DELEGATION_ID = "tenant-acceptance-shared-delegation";
    private static final String NOTIFICATION_ID = "tenant-acceptance-shared-notification";

    private final SharedStepData sharedStepData;
    private final TestDataProvider testDataProvider;
    private final TestJwtGenerator testJwtGenerator;
    private final TenantContext tenantContext;
    private final InstitutionRepository institutionRepository;
    private final DelegationRepository delegationRepository;
    private final MailNotificationConnectorImpl mailNotificationConnector;
    private final MailNotificationRepository mailNotificationRepository;
    private final MongoTemplate mongoTemplate;
    private long delegationCountBeforeRejectedWrite;
    private int expectedRejectedWriteStatus;

    public TenantAcceptanceSteps(
            SharedStepData sharedStepData,
            TestDataProvider testDataProvider,
            TestJwtGenerator testJwtGenerator,
            TenantContext tenantContext,
            InstitutionRepository institutionRepository,
            DelegationRepository delegationRepository,
            MailNotificationConnectorImpl mailNotificationConnector,
            MailNotificationRepository mailNotificationRepository,
            MongoTemplate mongoTemplate) {
        this.sharedStepData = sharedStepData;
        this.testDataProvider = testDataProvider;
        this.testJwtGenerator = testJwtGenerator;
        this.tenantContext = tenantContext;
        this.institutionRepository = institutionRepository;
        this.delegationRepository = delegationRepository;
        this.mailNotificationConnector = mailNotificationConnector;
        this.mailNotificationRepository = mailNotificationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Given("tenant isolation acceptance data is prepared")
    public void prepareAcceptanceData() {
        for (String tenantId : List.of("AR", "PNPG")) {
            tenantContext.withTenant(tenantId, () -> {
                removeAcceptanceData();
                InstitutionEntity institution = new InstitutionEntity();
                institution.setId(INSTITUTION_ID);
                institution.setTenantId(tenantId);
                institution.setDescription(tenantId + " institution");
                institution.setOrigin(Origin.MOCK);
                institution.setDigitalAddress(tenantId.toLowerCase() + "@example.test");
                institution.setOnboarding(List.of());
                institutionRepository.save(institution);

                DelegationEntity delegation = new DelegationEntity();
                delegation.setId(DELEGATION_ID);
                delegation.setTenantId(tenantId);
                delegation.setFrom(INSTITUTION_ID);
                delegation.setTo("tenant-acceptance-broker");
                delegation.setInstitutionFromName(tenantId + " delegator");
                delegation.setInstitutionToName("Tenant acceptance broker");
                delegation.setInstitutionFromRootName(tenantId + " root");
                delegation.setProductId(tenantId.toLowerCase() + "-product");
                delegation.setType(DelegationType.AOO);
                delegation.setStatus(DelegationState.ACTIVE);
                delegation.setIsTest(false);
                delegationRepository.save(delegation);

                Assertions.assertTrue(mailNotificationConnector.addMailNotification(
                        NOTIFICATION_ID,
                        tenantId.toLowerCase() + "-product",
                        tenantId.toLowerCase() + "@example.test",
                        5));
                return null;
            });
        }
    }

    @Given("I authenticate as {string} for tenant {string}")
    public void authenticateForTenant(String username, String tenantId) {
        JwtData source = testDataProvider.getTestData().getJwtData().stream()
                .filter(data -> username.equals(data.getUsername()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown test user"));
        JwtData tenantJwt = new JwtData();
        tenantJwt.setJwtHeader(source.getJwtHeader());
        Map<String, Object> claims = new HashMap<>(source.getJwtPayload());
        claims.put("tenant_id", tenantId);
        tenantJwt.setJwtPayload(claims);
        sharedStepData.setToken(testJwtGenerator.generateToken(tenantJwt));
        sharedStepData.setTenantId(tenantId);
    }

    @When("I retrieve institution {string} using tenant header {string}")
    public void retrieveInstitutionWithTenantHeader(String institutionId, String tenantId) {
        sharedStepData.setResponse(given()
                .header("Authorization", "Bearer " + sharedStepData.getToken())
                .header(TENANT_HEADER, tenantId)
                .accept(ContentType.JSON)
                .get("/institutions/{id}", institutionId)
                .then()
                .extract());
    }

    @When("I retrieve delegations for the tenant acceptance institution")
    public void retrieveTenantDelegations() {
        sharedStepData.setResponse(given()
                .header("Authorization", "Bearer " + sharedStepData.getToken())
                .header(TENANT_HEADER, sharedStepData.getTenantId())
                .accept(ContentType.JSON)
                .queryParam("institutionId", INSTITUTION_ID)
                .get("/delegations")
                .then()
                .extract());
    }

    @Then("the tenant acceptance institution description is {string}")
    public void assertInstitutionDescription(String description) {
        Assertions.assertEquals(description, sharedStepData.getResponse().body().jsonPath().getString("description"));
    }

    @Then("the tenant acceptance delegation product is {string}")
    public void assertDelegationProduct(String productId) {
        Assertions.assertEquals(
                productId,
                sharedStepData.getResponse().body().jsonPath().getString("[0].productId"));
    }

    @And("tenant acceptance mail notifications are isolated")
    public void assertMailNotificationsAreIsolated() {
        for (String tenantId : List.of("AR", "PNPG")) {
            List<MailNotificationEntity> notifications = tenantContext.withTenant(tenantId,
                    () -> mailNotificationRepository.find(
                            Query.query(Criteria.where("institutionId").is(NOTIFICATION_ID)),
                            MailNotificationEntity.class));
            Assertions.assertEquals(1, notifications.size());
            Assertions.assertEquals(
                    Set.of(tenantId.toLowerCase() + "-product"),
                    notifications.get(0).getProductIds());
            Assertions.assertEquals(tenantId, notifications.get(0).getTenantId());
        }
    }

    @When("I attempt a delegation write with tenant selection {string}")
    public void attemptDelegationWrite(String selection) {
        delegationCountBeforeRejectedWrite = tenantContext.withTenant(
                "AR", () -> mongoTemplate.count(new Query(), DelegationEntity.class));
        var request = given()
                .header("Authorization", "Bearer " + sharedStepData.getToken())
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "from": "%s",
                          "to": "tenant-acceptance-broker",
                          "institutionFromName": "AR delegator",
                          "institutionToName": "Tenant acceptance broker",
                          "productId": "ar-product",
                          "type": "AOO",
                          "institutionFromRootName": "AR root"
                        }
                        """.formatted(INSTITUTION_ID));
        switch (selection) {
            case "missing" -> expectedRejectedWriteStatus = 401;
            case "unsupported" -> {
                expectedRejectedWriteStatus = 401;
                request.header(TENANT_HEADER, "UNSUPPORTED");
            }
            case "duplicate" -> {
                expectedRejectedWriteStatus = 400;
                request.header(TENANT_HEADER, "AR").header(TENANT_HEADER, "PNPG");
            }
            case "mismatch" -> {
                expectedRejectedWriteStatus = 400;
                request.header(TENANT_HEADER, "PNPG");
            }
            default -> throw new IllegalArgumentException("Unknown tenant selection case");
        }
        sharedStepData.setResponse(request.post("/delegations").then().extract());
    }

    @Then("tenant selection is rejected before persistence")
    public void assertRejectedBeforePersistence() {
        Assertions.assertEquals(expectedRejectedWriteStatus, sharedStepData.getResponse().statusCode());
        if (expectedRejectedWriteStatus == 400) {
            Assertions.assertTrue(sharedStepData.getResponse().contentType().startsWith("application/problem+json"));
        }
        long currentCount = tenantContext.withTenant(
                "AR", () -> mongoTemplate.count(new Query(), DelegationEntity.class));
        Assertions.assertEquals(delegationCountBeforeRejectedWrite, currentCount);
    }

    @After("@TenantIsolationAcceptance")
    public void cleanupAcceptanceData() {
        for (String tenantId : List.of("AR", "PNPG")) {
            tenantContext.withTenant(tenantId, () -> {
                removeAcceptanceData();
                return null;
            });
        }
    }

    private void removeAcceptanceData() {
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(INSTITUTION_ID)), InstitutionEntity.class);
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(DELEGATION_ID)), DelegationEntity.class);
        mongoTemplate.remove(Query.query(Criteria.where("institutionId").is(NOTIFICATION_ID)), MailNotificationEntity.class);
    }
}
