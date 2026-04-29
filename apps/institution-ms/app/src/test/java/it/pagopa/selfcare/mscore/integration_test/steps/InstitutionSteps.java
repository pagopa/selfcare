package it.pagopa.selfcare.mscore.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import it.pagopa.selfcare.mscore.connector.dao.InstitutionRepository;
import it.pagopa.selfcare.mscore.connector.dao.MailNotificationRepository;
import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.MailNotificationEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.inner.OnboardingEntity;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InstitutionSteps {

    private final SharedStepData sharedStepData;
    private final InstitutionRepository institutionRepository;
    private final MailNotificationRepository mailNotificationRepository;
    private final MongoTemplate mongoTemplate;

    private String mockInstitutionId;

    public InstitutionSteps(SharedStepData sharedStepData,
                            InstitutionRepository institutionRepository,
                            MailNotificationRepository mailNotificationRepository,
                            MongoTemplate mongoTemplate) {
        this.sharedStepData = sharedStepData;
        this.institutionRepository = institutionRepository;
        this.mailNotificationRepository = mailNotificationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @After("@RemoveInstitutionIdAfterScenario")
    public void removeInstitutionIdAfterScenario(Scenario scenario) {
        Optional.ofNullable(sharedStepData.getResponse().body().jsonPath().getString("id"))
                .ifPresent(institutionRepository::deleteById);
    }

    @After("@RemoveSubunitAndParentInstitutionAfterScenario")
    public void removeSubunitAndParentInstitutionAfterScenario(Scenario scenario) {
        Optional.ofNullable(sharedStepData.getResponse().body().jsonPath().getString("rootParent.id"))
                .ifPresent(institutionRepository::deleteById);
        Optional.ofNullable(sharedStepData.getResponse().body().jsonPath().getString("id"))
                .ifPresent(institutionRepository::deleteById);
    }

    @After("@RemoveMockInstitutionAfterScenario")
    public void removeMockInstitution() {
        Optional.ofNullable(mockInstitutionId).ifPresent(id -> {
            institutionRepository.deleteById(id);
            mongoTemplate.remove(new Query(Criteria.where("institutionId").is(id)), MailNotificationEntity.class);
        });
    }

    @And("A mock institution with id {string}")
    public void createMockInstitutionWithId(String id) {
        final InstitutionEntity entity = new InstitutionEntity();
        entity.setId(id);
        entity.setOrigin(Origin.MOCK);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setOnboarding(List.of(
                createMockOnboardingEntity("prod-io", RelationshipState.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now()),
                createMockOnboardingEntity("prod-pagopa", RelationshipState.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now()),
                createMockOnboardingEntity("prod-idpay", RelationshipState.DELETED, OffsetDateTime.now(), OffsetDateTime.now()),
                createMockOnboardingEntity("prod-pn", RelationshipState.SUSPENDED, OffsetDateTime.now(), OffsetDateTime.now())
        ));
        entity.setDigitalAddress("digital.address@test.com");
        final InstitutionEntity savedEntity = institutionRepository.save(entity);
        mockInstitutionId = savedEntity.getId();

        final MailNotificationEntity mailNotificationEntity = new MailNotificationEntity();
        mailNotificationEntity.setInstitutionId(mockInstitutionId);
        mailNotificationEntity.setProductIds(Set.of("prod-io", "prod-pagopa"));
        mailNotificationEntity.setModuleDayOfTheEpoch(10);
        mailNotificationEntity.setDigitalAddress("digital.address@test.com");
        mailNotificationEntity.setCreatedAt(Instant.now());
        mailNotificationEntity.setUpdatedAt(Instant.now());
        mailNotificationRepository.save(mailNotificationEntity);
    }

    @And("A mock institution with id {string} without active onboardings")
    public void createMockInstitutionWithoutActiveOnboardings(String id) {
        final InstitutionEntity entity = new InstitutionEntity();
        entity.setId(id);
        entity.setOrigin(Origin.MOCK);
        entity.setOriginId("x1");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setDigitalAddress("digital.address@test.com");
        entity.setOnboarding(List.of(
                createMockOnboardingEntity("prod-idpay", RelationshipState.DELETED, OffsetDateTime.now(), OffsetDateTime.now())
        ));
        final InstitutionEntity savedEntity = institutionRepository.save(entity);
        mockInstitutionId = savedEntity.getId();
    }

    private OnboardingEntity createMockOnboardingEntity(String productId, RelationshipState status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        final OnboardingEntity onboardingEntity = new OnboardingEntity();
        onboardingEntity.setProductId(productId);
        onboardingEntity.setStatus(status);
        onboardingEntity.setContract("MOCK_CONTRACT");
        onboardingEntity.setTokenId("MOCK_TOKEN");
        onboardingEntity.setCreatedAt(createdAt);
        onboardingEntity.setUpdatedAt(updatedAt);
        onboardingEntity.setInstitutionType(InstitutionType.PA);
        onboardingEntity.setOrigin(Origin.MOCK.getValue());
        onboardingEntity.setOriginId("123x");
        return onboardingEntity;
    }

    @And("Onboarding of institutionId {string} and productId {string} has createdAt {string}")
    public void checkOnboardingCreatedAt(String institutionId, String productId, String expectedCreatedAt) {
        final InstitutionEntity institution = institutionRepository.findById(institutionId).orElseThrow();
        final OnboardingEntity onboarding = institution.getOnboarding()
                .stream().filter(o -> o.getProductId().equals(productId)).findFirst().orElseThrow();
        Assertions.assertTrue(OffsetDateTime.parse(expectedCreatedAt).isEqual(onboarding.getCreatedAt()));
    }

    @And("Onboardings of institution with id {string} have following states:")
    public void checkOnboardingsStates(String institutionId, Map<String, String> expectedProductState) {
        final InstitutionEntity entity = institutionRepository.findById(institutionId).orElseThrow();
        entity.getOnboarding().forEach(e -> Assertions.assertEquals(expectedProductState.get(e.getProductId()), e.getStatus().name()));
    }

    @And("Count of MailNotification with institutionId {string} is {long}")
    public void checkMailNotificationCount(String institutionId, long expectedCount) {
        long count = mongoTemplate.count(new Query(Criteria.where("institutionId").is(institutionId)), MailNotificationEntity.class);
        Assertions.assertEquals(expectedCount, count);
    }

    @And("Onboarding for institutionId {string} and productId {string} was saved to db successfully with token {string} contract {string}, a module of {int}, institutionType {string}, origin {string} and originId {string}")
    public void checkOnboardingWasSaved(String institutionId, String productId, String expectedToken, String expectedContract, int expectedModule, String expectedInstitutionType, String expectedOrigin, String expectedOriginId) {
        final InstitutionEntity institution = institutionRepository.findById(institutionId).orElseThrow();
        final List<OnboardingEntity> onboardings = institution.getOnboarding().stream()
                .filter(o -> o.getProductId().equals(productId) && (o.getStatus().equals(RelationshipState.ACTIVE) || o.getStatus().equals(RelationshipState.SUSPENDED))).toList();
        Assertions.assertEquals(1, onboardings.size());

        final OnboardingEntity onboarding = onboardings.get(0);
        Assertions.assertEquals(expectedToken, onboarding.getTokenId());
        Assertions.assertEquals(expectedContract, onboarding.getContract());
        Assertions.assertEquals(expectedInstitutionType, onboarding.getInstitutionType().toString());
        Assertions.assertEquals(expectedOrigin, onboarding.getOrigin());
        Assertions.assertEquals(expectedOriginId, onboarding.getOriginId());
        final List<MailNotificationEntity> entities = mailNotificationRepository.find(Query.query(Criteria.where("institutionId").is(institutionId)), MailNotificationEntity.class);
        Assertions.assertEquals(1, entities.size());

        final MailNotificationEntity mailNotification = entities.get(0);
        Assertions.assertEquals(institutionId, mailNotification.getInstitutionId());
        Assertions.assertEquals(institution.getDigitalAddress(), mailNotification.getDigitalAddress());
        Assertions.assertEquals(expectedModule, mailNotification.getModuleDayOfTheEpoch());
        Assertions.assertTrue(mailNotification.getProductIds().contains(productId));
        Assertions.assertNotNull(mailNotification.getCreatedAt());
        Assertions.assertNotNull(mailNotification.getUpdatedAt());
    }

    @And("MailNotification for institutionId {string} and productId {string} was removed from db successfully")
    public void checkMailNotificationWasRemoved(String institutionId, String productId) {
        final List<MailNotificationEntity> entities = mailNotificationRepository.find(Query.query(Criteria.where("institutionId").is(institutionId)), MailNotificationEntity.class);
        Assertions.assertEquals(1, entities.size());

        final MailNotificationEntity mailNotification = entities.get(0);
        Assertions.assertFalse(mailNotification.getProductIds().contains(productId));
    }

    @And("MailNotification for institutionId {string} was deleted from db successfully")
    public void checkMailNotificationWasDeleted(String institutionId) {
        final List<MailNotificationEntity> entities = mailNotificationRepository.find(Query.query(Criteria.where("institutionId").is(institutionId)), MailNotificationEntity.class);
        Assertions.assertTrue(entities.isEmpty());
    }

}
