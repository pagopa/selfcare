package it.pagopa.selfcare.onboarding.entity.registry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import it.pagopa.selfcare.product.entity.Product;

@QuarkusTest
class RegistryManagerPDNDInfocamereTest {

    @InjectMock
    @RestClient
    InfocamerePdndApi infocamerePdndApi;

    @InjectMock
    @RestClient
    UserApi userRegistryApi;

    private Onboarding onboarding;
    private RegistryManagerPDNDInfocamere registryManager;
    private Product product;

    @BeforeEach
    void setUp() {
        onboarding = createDummyOnboarding();
        product = mock(Product.class);
    }

    @Test
    void customValidation_withIdPayMerchantProduct_nonPrivatePerson_returnsOnboarding() {
        // given
        when(product.getId()).thenReturn("prod-idpay-merchant");
        onboarding.getInstitution().setInstitutionType(InstitutionType.GSP);
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi
        );

        // when
        Uni<Onboarding> result = registryManager.customValidation(product);

        // then
        Onboarding resultOnboarding = result.await().indefinitely();
        assertNotNull(resultOnboarding);
        assertEquals(onboarding.getId(), resultOnboarding.getId());
    }

    @Test
    void customValidation_withPrivatePersonInstitution_userSearchSuccessful() {
        // given
        String taxCode = "RSSMRA80A01H501T";
        onboarding.getInstitution().setInstitutionType(InstitutionType.PRV_PF);
        onboarding.getInstitution().setTaxCode(taxCode);

        when(product.getId()).thenReturn("prod-idpay-merchant");

        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi
        );

        UserResource userResource = new UserResource();
        userResource.setId(UUID.fromString("12345678-1234-1234-1234-123456789012"));

        when(userRegistryApi.searchUsingPOST(
                eq("fiscalCode"),
                any()
        )).thenReturn(Uni.createFrom().item(userResource));

        // when
        Uni<Onboarding> result = registryManager.customValidation(product);

        // then
        Onboarding resultOnboarding = result.await().indefinitely();
        assertNotNull(resultOnboarding);
        assertEquals("12345678-1234-1234-1234-123456789012", resultOnboarding.getInstitution().getTaxCode());
        assertEquals("12345678-1234-1234-1234-123456789012", resultOnboarding.getInstitution().getOriginId());
    }

    @Test
    void customValidation_withPrivatePersonInstitution_userSearchThrowsException() {
        // given
        String taxCode = "RSSMRA80A01H501T";
        onboarding.getInstitution().setInstitutionType(InstitutionType.PRV_PF);
        onboarding.getInstitution().setTaxCode(taxCode);

        when(product.getId()).thenReturn("prod-idpay-merchant");

        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi
        );

        RuntimeException searchException = new RuntimeException("Search failed");

        when(userRegistryApi.searchUsingPOST(
                eq("fiscalCode"),
                any()
        )).thenReturn(Uni.createFrom().failure(searchException));

        // when
        Uni<Onboarding> result = registryManager.customValidation(product);

        // then
        assertThrows(RuntimeException.class, () -> result.await().indefinitely());
    }

    @Test
    void customValidation_withOtherInstitutionType_returnsOnboarding() {
        // given
        onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
        when(product.getId()).thenReturn("OTHER_PRODUCT");
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi
        );

        // when
        Uni<Onboarding> result = registryManager.customValidation(product);

        // then
        Onboarding resultOnboarding = result.await().indefinitely();
        assertNotNull(resultOnboarding);
        assertEquals(onboarding.getId(), resultOnboarding.getId());
    }

    @Test
    void customValidation_withNullProduct_returnsOnboarding() {
        // given
        onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi
        );

        // when
        Uni<Onboarding> result = registryManager.customValidation(null);

        // then
        Onboarding resultOnboarding = result.await().indefinitely();
        assertNotNull(resultOnboarding);
        assertEquals(onboarding.getId(), resultOnboarding.getId());
    }

    @Test
    void isValid_shouldReturnTrue() {
        // given
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi
        );

        // when
        Uni<Boolean> result = registryManager.isValid();

        // then
        Boolean isValid = result.await().indefinitely();
        assertTrue(isValid);
    }

    private Onboarding createDummyOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(UUID.randomUUID().toString());
        onboarding.setProductId("prod-idpay");

        Institution institution = new Institution();
        institution.setTaxCode("01234567890");
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setInstitution(institution);

        User user = new User();
        user.setId("user-id");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));

        return onboarding;
    }
}
