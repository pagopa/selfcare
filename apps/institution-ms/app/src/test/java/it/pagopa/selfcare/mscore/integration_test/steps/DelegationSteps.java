package it.pagopa.selfcare.mscore.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import it.pagopa.selfcare.mscore.connector.dao.DelegationRepository;
import it.pagopa.selfcare.mscore.connector.dao.InstitutionRepository;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.inner.OnboardingEntity;
import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class DelegationSteps {

    private final InstitutionRepository institutionRepository;
    private final DelegationRepository delegationRepository;
    private final MongoTemplate mongoTemplate;

    public DelegationSteps(InstitutionRepository institutionRepository,
                           DelegationRepository delegationRepository,
                           MongoTemplate mongoTemplate) {
        this.institutionRepository = institutionRepository;
        this.delegationRepository = delegationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    private String mockInstitutionId1;
    private String mockInstitutionId2;
    private String mockDelegationId;

    @After("@RemovePairOfMockInstitutionAfterScenario")
    public void removePairOfMockInstitution() {
        Optional.ofNullable(mockInstitutionId1).ifPresent(institutionRepository::deleteById);
        Optional.ofNullable(mockInstitutionId2).ifPresent(institutionRepository::deleteById);
    }

    @After("@RemoveCreatedDelegationAfterScenario")
    public void removeCreatedDelegation() {
        Optional.ofNullable(mockInstitutionId1).ifPresent(id1 -> Optional.ofNullable(mockInstitutionId2).ifPresent(id2 -> {
            mongoTemplate.remove(new Query(Criteria.where("from").is(id1).and("to").is(id2)), DelegationEntity.class);
        }));
        Optional.ofNullable(mockDelegationId).ifPresent(delegationRepository::deleteById);
    }

    @And("A pair of mock institutions with id {string},{string} and taxcode {string},{string} with subunitCode {string},{string} with an onboarding on product {string} and isTest {string}, {string}")
    public void createPairOfMockInstitutionWithId(String id1, String id2, String taxCode1, String taxCode2, String subCode1, String subCode2, String productId, String isTest1, String isTest2) {
        final OnboardingEntity onboardingEntity = new OnboardingEntity();
        onboardingEntity.setProductId(productId);
        onboardingEntity.setInstitutionType(InstitutionType.PT);

        final InstitutionEntity entity1 = new InstitutionEntity();
        entity1.setId(id1);
        entity1.setOrigin(Origin.MOCK);
        entity1.setCreatedAt(OffsetDateTime.now());
        entity1.setUpdatedAt(OffsetDateTime.now());
        entity1.setTaxCode(taxCode1);
        entity1.setSubunitCode(subCode1);
        entity1.setOnboarding(List.of(onboardingEntity));
        entity1.setIsTest(Boolean.parseBoolean(isTest1));
        final InstitutionEntity savedEntity1 = institutionRepository.save(entity1);
        mockInstitutionId1 = savedEntity1.getId();

        final InstitutionEntity entity2 = new InstitutionEntity();
        entity2.setId(id2);
        entity2.setOrigin(Origin.MOCK);
        entity2.setCreatedAt(OffsetDateTime.now());
        entity2.setUpdatedAt(OffsetDateTime.now());
        entity2.setTaxCode(taxCode2);
        entity2.setSubunitCode(subCode2);
        entity2.setOnboarding(List.of(onboardingEntity));
        entity2.setIsTest(Boolean.parseBoolean(isTest2));
        final InstitutionEntity savedEntity2 = institutionRepository.save(entity2);
        mockInstitutionId2 = savedEntity2.getId();
    }

    @And("A mock delegation with id {string} of type {} with productId {string} for institution with id {string} and {string} with status {}")
    public void createMockDelegation(String delegationId, DelegationType delegationType, String productId, String id1, String id2, DelegationState status) {
        final InstitutionEntity fromInstitution = institutionRepository.findById(id1).orElseThrow();
        final InstitutionEntity toInstitution = institutionRepository.findById(id2).orElseThrow();
        final DelegationEntity delegation = new DelegationEntity();
        delegation.setId(delegationId);
        delegation.setFrom(fromInstitution.getId());
        delegation.setTo(toInstitution.getId());
        delegation.setInstitutionFromName("From Institution");
        delegation.setInstitutionFromRootName("From Root Institution");
        delegation.setInstitutionToName("To Institution");
        delegation.setFromTaxCode(fromInstitution.getTaxCode());
        delegation.setToTaxCode(toInstitution.getTaxCode());
        delegation.setFromType(fromInstitution.getOnboarding().stream().filter(onb -> productId.equals(onb.getProductId())).map(onb -> onb.getInstitutionType().name()).findFirst().orElse(null));
        delegation.setToType(toInstitution.getOnboarding().stream().filter(onb -> productId.equals(onb.getProductId())).map(onb -> onb.getInstitutionType().name()).findFirst().orElse(null));
        delegation.setProductId(productId);
        delegation.setStatus(status);
        if(status == DelegationState.DELETED){
            delegation.setClosedAt(OffsetDateTime.now());
        }
        delegation.setType(delegationType);
        final DelegationEntity savedDelegation = delegationRepository.save(delegation);
        mockDelegationId = savedDelegation.getId();
    }

    @And("A mock delegation with id {string} without real institutions")
    public void createMockDelegationWithoutRealInstitutions(String delegationId) {
        final DelegationEntity delegation = new DelegationEntity();
        delegation.setId(delegationId);
        delegation.setFrom("mockFrom");
        delegation.setTo("mockTo");
        delegation.setInstitutionFromName("From Institution");
        delegation.setInstitutionFromRootName("From Root Institution");
        delegation.setInstitutionToName("To Institution");
        delegation.setFromTaxCode("mockFromTaxCode");
        delegation.setToTaxCode("mockToTaxCode");
        delegation.setFromType(InstitutionType.PA.name());
        delegation.setToType(InstitutionType.GSP.name());
        delegation.setProductId("mockProductId");
        delegation.setStatus(DelegationState.ACTIVE);
        delegation.setType(DelegationType.PT);
        final DelegationEntity savedDelegation = delegationRepository.save(delegation);
        mockDelegationId = savedDelegation.getId();
    }

    @And("The delegation flag for institution {string} is {} on db")
    public void checkDelegationFlag(String institutionId, Boolean expectedValue) {
        final InstitutionEntity institution = institutionRepository.findById(institutionId).orElseThrow();
        Assertions.assertEquals(expectedValue, institution.isDelegation());
    }

    @And("The delegation with id {string} is in state {} and closedAt is not null on db")
    public void checkDelegationState(String delegationId, DelegationState delegationState) {
        final DelegationEntity delegation = delegationRepository.findById(delegationId).orElseThrow();
        Assertions.assertEquals(delegationState, delegation.getStatus());
        Assertions.assertNotNull(delegation.getClosedAt());
    }

    @And("The delegation from institution {string} to institution {string} for product {string} was saved to db successfully")
    public void checkCreatedDelegation(String institutionFromId, String institutionToId, String productId) {
        final InstitutionEntity fromInstitution = institutionRepository.findById(institutionFromId).orElseThrow();
        final InstitutionEntity toInstitution = institutionRepository.findById(institutionToId).orElseThrow();
        final DelegationEntity delegation = mongoTemplate.findOne(new Query(Criteria.where("from").is(institutionFromId).and("to").is(institutionToId)), DelegationEntity.class);
        OnboardingEntity onboardingTo = toInstitution.getOnboarding().stream().filter(onb -> onb.getProductId().equals(productId)).findFirst().orElseThrow();
        OnboardingEntity onboardingFrom = fromInstitution.getOnboarding().stream().filter(onb -> onb.getProductId().equals(productId)).findFirst().orElseThrow();
        Assertions.assertNotNull(delegation);
        Assertions.assertEquals(fromInstitution.getTaxCode(), delegation.getFromTaxCode());
        Assertions.assertEquals(toInstitution.getTaxCode(), delegation.getToTaxCode());
        Assertions.assertEquals(onboardingTo.getInstitutionType().name(), delegation.getFromType());
        Assertions.assertEquals(onboardingFrom.getInstitutionType().name(), delegation.getToType());
        Assertions.assertEquals(DelegationState.ACTIVE, delegation.getStatus());
        Assertions.assertNull(delegation.getClosedAt());
    }

}
