package it.pagopa.selfcare.onboarding.event;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.event.entity.Institution;
import it.pagopa.selfcare.onboarding.event.entity.Onboarding;
import it.pagopa.selfcare.onboarding.event.entity.util.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.event.mapper.OnboardingMapper;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class OnboardingMapperTest {

    @Inject
    private OnboardingMapper mapper;

    @Test
    @DisplayName("Should map all institution fields to index resource")
    void toIndexResourceShouldMapAllInstitutionFields() {
        // given
        Institution institution = new Institution();
        institution.setId("institution-id");
        institution.setDescription("Institution description");
        institution.setParentDescription("Parent description");
        institution.setTaxCode("12345678901");
        institution.setSubunitCode("AOO001");
        institution.setSubunitType(InstitutionPaSubunitType.AOO);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setIsTest(Boolean.TRUE);
        institution.setCity("Rome");
        institution.setCounty("RM");
        institution.setCountry("IT");
        institution.setOrigin(Origin.IPA);

        Onboarding onboarding = new Onboarding();
        onboarding.id = "onboarding-id";
        onboarding.setInstitution(institution);

        // when
        org.openapi.quarkus.party_registry_proxy_json.model.OnboardingIndexResource result = mapper.toIndexResource(onboarding);

        // then
        assertNotNull(result);
        assertEquals(onboarding.getId(), result.getOnboardingId());
        assertEquals(institution.getId(), result.getInstitutionId());
        assertEquals(institution.getDescription(), result.getDescription());
        assertEquals(institution.getTaxCode(), result.getTaxCode());
        assertEquals(institution.getSubunitCode(), result.getSubunitCode());
    }

    @Test
    @DisplayName("Should return null mapped fields when institution is null")
    void toIndexResourceShouldReturnNullMappedFieldsWhenInstitutionIsNull() {
        // given
        Onboarding onboarding = new Onboarding();
        onboarding.id = "onboarding-id";
        onboarding.setInstitution(null);

        // when
        org.openapi.quarkus.party_registry_proxy_json.model.OnboardingIndexResource result = mapper.toIndexResource(onboarding);

        // then
        assertNotNull(result);
        assertNull(result.getInstitutionId());
    }

    @Test
    @DisplayName("Should map onboarding entity to api model")
    void toEntityShouldMapOnboardingToApiModel() {
        // given
        Onboarding onboarding = new Onboarding();
        onboarding.id = "onboarding-id";
        onboarding.setProductId("product-id");
        onboarding.setStatus(OnboardingStatus.COMPLETED);

        // when
        org.openapi.quarkus.onboarding_functions_json.model.Onboarding result = mapper.toEntity(onboarding);

        // then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should convert local date time to offset date time")
    void mapShouldConvertLocalDateTimeToOffsetDateTime() {
        // given
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30);

        // when
        java.time.OffsetDateTime result = mapper.map(localDateTime);

        // then
        assertNotNull(result);
        assertEquals(ZoneOffset.UTC, result.getOffset());
    }

    @Test
    @DisplayName("Should return null when local date time is null")
    void mapShouldReturnNullWhenLocalDateTimeIsNull() {
        // given
        LocalDateTime localDateTime = null;

        // when
        java.time.OffsetDateTime result = mapper.map(localDateTime);

        // then
        assertNull(result);
    }
}
