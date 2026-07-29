package it.pagopa.selfcare.onboarding.entity.registry;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BaseRegistryManagerTest {

    private Onboarding onboarding;
    private Institution institution;
    private RegistryManagerSELC registryManager;

    @BeforeEach
    void setUp() {
        onboarding = new Onboarding();
        institution = new Institution();
        onboarding.setInstitution(institution);
        registryManager = new RegistryManagerSELC(onboarding);
    }

    @Nested
    class ValidateInstitutionTypeByProduct {

        @Test
        void givenProductWithNoInstitutionTypesAllowed_whenValidate_thenReturnOnboarding() {
            // given
            institution.setInstitutionType(InstitutionType.PA);
            institution.setOrigin(Origin.IPA);
            Product product = new Product();
            product.setInstitutionTypesAllowed(null);

            // when
            Onboarding result = registryManager.validateInstitutionType(product).await().indefinitely();

            // then
            assertEquals(onboarding, result);
        }

        @Test
        void givenProductWithEmptyInstitutionTypesAllowed_whenValidate_thenReturnOnboarding() {
            // given
            institution.setInstitutionType(InstitutionType.PA);
            institution.setOrigin(Origin.IPA);
            Product product = new Product();
            product.setInstitutionTypesAllowed(List.of());

            // when
            Onboarding result = registryManager.validateInstitutionType(product).await().indefinitely();

            // then
            assertEquals(onboarding, result);
        }

        @Test
        void givenProductAllowsInstitutionType_whenValidate_thenReturnOnboarding() {
            // given
            institution.setInstitutionType(InstitutionType.PA);
            institution.setOrigin(Origin.IPA);
            Product product = new Product();
            product.setId("prod-test");
            product.setInstitutionTypesAllowed(List.of("PA", "GSP"));

            // when
            Onboarding result = registryManager.validateInstitutionType(product).await().indefinitely();

            // then
            assertEquals(onboarding, result);
        }

        @Test
        void givenProductDoesNotAllowInstitutionType_whenValidate_thenThrowInvalidRequestException() {
            // given
            institution.setInstitutionType(InstitutionType.SA);
            institution.setOrigin(Origin.ANAC);
            Product product = new Product();
            product.setId("prod-test");
            product.setInstitutionTypesAllowed(List.of("PA", "GSP"));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> registryManager.validateInstitutionType(product).await().indefinitely());
            assertTrue(exception.getMessage().contains("SA"));
            assertTrue(exception.getMessage().contains("prod-test"));
        }
    }

    @Nested
    class ValidateInstitutionTypeByOrigin {

        @Test
        void givenNullOrigin_whenValidate_thenReturnOnboarding() {
            // given
            institution.setInstitutionType(InstitutionType.PA);
            institution.setOrigin(null);
            Product product = new Product();

            // when
            Onboarding result = registryManager.validateInstitutionType(product).await().indefinitely();

            // then
            assertEquals(onboarding, result);
        }

        @Test
        void givenNullInstitutionType_whenValidate_thenReturnOnboarding() {
            // given
            institution.setInstitutionType(null);
            institution.setOrigin(Origin.IPA);
            Product product = new Product();

            // when
            Onboarding result = registryManager.validateInstitutionType(product).await().indefinitely();

            // then
            assertEquals(onboarding, result);
        }

        @Test
        void givenUnmappedOrigin_whenValidate_thenReturnOnboarding() {
            // given
            institution.setInstitutionType(InstitutionType.PA);
            institution.setOrigin(Origin.MOCK);
            Product product = new Product();

            // when
            Onboarding result = registryManager.validateInstitutionType(product).await().indefinitely();

            // then
            assertEquals(onboarding, result);
        }

        @ParameterizedTest
        @MethodSource("it.pagopa.selfcare.onboarding.entity.registry.BaseRegistryManagerTest#validOriginInstitutionTypeCombinations")
        void givenValidOriginInstitutionTypeCombination_whenValidate_thenReturnOnboarding(Origin origin, InstitutionType institutionType) {
            // given
            institution.setOrigin(origin);
            institution.setInstitutionType(institutionType);
            Product product = new Product();

            // when
            Onboarding result = registryManager.validateInstitutionType(product).await().indefinitely();

            // then
            assertEquals(onboarding, result);
        }

        @ParameterizedTest
        @MethodSource("it.pagopa.selfcare.onboarding.entity.registry.BaseRegistryManagerTest#invalidOriginInstitutionTypeCombinations")
        void givenInvalidOriginInstitutionTypeCombination_whenValidate_thenThrowInvalidRequestException(Origin origin, InstitutionType institutionType) {
            // given
            institution.setOrigin(origin);
            institution.setInstitutionType(institutionType);
            Product product = new Product();

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> registryManager.validateInstitutionType(product).await().indefinitely());
            assertTrue(exception.getMessage().contains(institutionType.name()));
            assertTrue(exception.getMessage().contains(origin.getValue()));
            assertTrue(exception.getMessage().contains("Allowed institution types"));
        }
    }

    static Stream<Arguments> validOriginInstitutionTypeCombinations() {
        return Stream.of(
                // IPA → PA, GSP, SCEC
                Arguments.of(Origin.IPA, InstitutionType.PA),
                Arguments.of(Origin.IPA, InstitutionType.GSP),
                Arguments.of(Origin.IPA, InstitutionType.SCEC),
                // ANAC → SA
                Arguments.of(Origin.ANAC, InstitutionType.SA),
                // IVASS → AS
                Arguments.of(Origin.IVASS, InstitutionType.AS),
                // PDND_INFOCAMERE → PRV, SCP, PRV_PF
                Arguments.of(Origin.PDND_INFOCAMERE, InstitutionType.PRV),
                Arguments.of(Origin.PDND_INFOCAMERE, InstitutionType.SCP),
                Arguments.of(Origin.PDND_INFOCAMERE, InstitutionType.PRV_PF),
                // SELC → PT, GSP, PSP, GPU, PRV, CON, REC, SCP
                Arguments.of(Origin.SELC, InstitutionType.PT),
                Arguments.of(Origin.SELC, InstitutionType.GSP),
                Arguments.of(Origin.SELC, InstitutionType.PSP),
                Arguments.of(Origin.SELC, InstitutionType.GPU),
                Arguments.of(Origin.SELC, InstitutionType.PRV),
                Arguments.of(Origin.SELC, InstitutionType.CON),
                Arguments.of(Origin.SELC, InstitutionType.REC),
                Arguments.of(Origin.SELC, InstitutionType.SCP),
                // INFOCAMERE → PG
                Arguments.of(Origin.INFOCAMERE, InstitutionType.PG),
                // ADE → PG
                Arguments.of(Origin.ADE, InstitutionType.PG)
        );
    }

    static Stream<Arguments> invalidOriginInstitutionTypeCombinations() {
        return Stream.of(
                // IPA non ammette SA, PG, PT, etc.
                Arguments.of(Origin.IPA, InstitutionType.SA),
                Arguments.of(Origin.IPA, InstitutionType.PG),
                Arguments.of(Origin.IPA, InstitutionType.PT),
                // ANAC non ammette PA, PG, etc.
                Arguments.of(Origin.ANAC, InstitutionType.PA),
                Arguments.of(Origin.ANAC, InstitutionType.PG),
                // IVASS non ammette PA, SA, etc.
                Arguments.of(Origin.IVASS, InstitutionType.PA),
                Arguments.of(Origin.IVASS, InstitutionType.SA),
                // PDND_INFOCAMERE non ammette PA, PG, etc.
                Arguments.of(Origin.PDND_INFOCAMERE, InstitutionType.PA),
                Arguments.of(Origin.PDND_INFOCAMERE, InstitutionType.PG),
                // SELC non ammette PA, SA, PG, etc.
                Arguments.of(Origin.SELC, InstitutionType.PA),
                Arguments.of(Origin.SELC, InstitutionType.SA),
                Arguments.of(Origin.SELC, InstitutionType.PG),
                // INFOCAMERE non ammette PA, SA, etc.
                Arguments.of(Origin.INFOCAMERE, InstitutionType.PA),
                Arguments.of(Origin.INFOCAMERE, InstitutionType.SA),
                // ADE non ammette PA, SA, etc.
                Arguments.of(Origin.ADE, InstitutionType.PA),
                Arguments.of(Origin.ADE, InstitutionType.SA)
        );
    }
}


