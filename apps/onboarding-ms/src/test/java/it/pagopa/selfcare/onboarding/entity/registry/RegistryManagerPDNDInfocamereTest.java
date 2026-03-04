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
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.api.PdndVisuraInfoCamereControllerApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;
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

    @InjectMock
    @RestClient
    PdndVisuraInfoCamereControllerApi pdndVisuraInfoCamereControllerApi;

    private Onboarding onboarding;
    private RegistryManagerPDNDInfocamere registryManager;
    private Product product;

    @BeforeEach
    void setUp() {
        onboarding = createDummyOnboarding();
        product = mock(Product.class);
    }

    @Test
    void customValidation_withIdPayMerchantProduct_pdndAtecosValid() {
        // given
        when(product.getId()).thenReturn("prod-idpay-merchant");
        onboarding.getInstitution().setAtecoCodes(List.of("01.11.00","01.12.00"));
        onboarding.getInstitution().setTaxCode("01234567890");
        String allowedAtecoCodes = "01.11.00,90.01,45.67";
        
        PDNDBusinessResource pdndResource = new PDNDBusinessResource();
        pdndResource.setAtecoCodes(List.of("01.11.00", "01.12.00"));
        
        when(pdndVisuraInfoCamereControllerApi.institutionVisuraPdndByTaxCodeUsingGET("01234567890"))
                .thenReturn(Uni.createFrom().item(pdndResource));
        
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi,
                Optional.of(allowedAtecoCodes),
                pdndVisuraInfoCamereControllerApi
        );

        // when
        Uni<Onboarding> result = registryManager.customValidation(product);

        // then
        Onboarding resultOnboarding = result.await().indefinitely();
        assertNotNull(resultOnboarding);
        assertEquals(onboarding.getId(), resultOnboarding.getId());
    }

    @Test
    void customValidation_withIdPayMerchantProduct_pdndAtecosNotInAllowedList() {
        // given
        when(product.getId()).thenReturn("prod-idpay-merchant");
        onboarding.getInstitution().setAtecoCodes(List.of("99.99"));
        onboarding.getInstitution().setTaxCode("01234567890");
        String allowedAtecoCodes = "12.34,90.01,45.67";
        
        PDNDBusinessResource pdndResource = new PDNDBusinessResource();
        pdndResource.setAtecoCodes(List.of("88.88", "77.77"));
        
        when(pdndVisuraInfoCamereControllerApi.institutionVisuraPdndByTaxCodeUsingGET("01234567890"))
                .thenReturn(Uni.createFrom().item(pdndResource));
        
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi,
                Optional.of(allowedAtecoCodes),
                pdndVisuraInfoCamereControllerApi
        );

        // when
        Uni<Onboarding> result = registryManager.customValidation(product);

        // then
        assertThrows(InvalidRequestException.class, () -> result.await().indefinitely());
    }

    @Test
    void customValidation_withIdPayMerchantProduct_pdndAtecosEmpty() {
        // given
        when(product.getId()).thenReturn("prod-idpay-merchant");
        onboarding.getInstitution().setAtecoCodes(List.of("12.34"));
        onboarding.getInstitution().setTaxCode("01234567890");
        String allowedAtecoCodes = "12.34,90.01,45.67";
        
        PDNDBusinessResource pdndResource = new PDNDBusinessResource();
        pdndResource.setAtecoCodes(List.of());
        
        when(pdndVisuraInfoCamereControllerApi.institutionVisuraPdndByTaxCodeUsingGET("01234567890"))
                .thenReturn(Uni.createFrom().item(pdndResource));
        
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi,
                Optional.of(allowedAtecoCodes),
                pdndVisuraInfoCamereControllerApi
        );

        // when
        Uni<Onboarding> result = registryManager.customValidation(product);

        // then
        assertThrows(InvalidRequestException.class, () -> result.await().indefinitely());
    }

    @Test
    void customValidation_withIdPayMerchantProduct_pdndAtecosNotMatchWithAtecoRequest() {
        // given
        when(product.getId()).thenReturn("prod-idpay-merchant");
        onboarding.getInstitution().setAtecoCodes(List.of("12.34"));
        onboarding.getInstitution().setTaxCode("01234567890");
        String allowedAtecoCodes = "12.34, 90.01 , 45.67";
        
        PDNDBusinessResource pdndResource = new PDNDBusinessResource();
        pdndResource.setAtecoCodes(List.of("12.34 ", "56.78"));
        
        when(pdndVisuraInfoCamereControllerApi.institutionVisuraPdndByTaxCodeUsingGET("01234567890"))
                .thenReturn(Uni.createFrom().item(pdndResource));
        
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi,
                Optional.of(allowedAtecoCodes),
                pdndVisuraInfoCamereControllerApi
        );

        // when
        Uni<Onboarding> result = registryManager.customValidation(product);

        // then
        assertThrows(InvalidRequestException.class, () -> result.await().indefinitely());
    }

    @Test
    void customValidation_withIdPayMerchantProduct_noAllowedAtecoCodes() {
        // given
        when(product.getId()).thenReturn("prod-idpay-merchant");
        onboarding.getInstitution().setAtecoCodes(List.of("12.34", "56.78"));
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi,
                Optional.empty(),
                pdndVisuraInfoCamereControllerApi
        );

        // when & then
        assertThrows(InvalidRequestException.class, () -> registryManager.customValidation(product));
    }

    @Test
    void customValidation_withPrivatePersonInstitution_userSearchSuccessful() {
        // given
        String taxCode = "RSSMRA80A01H501T";
        String allowedAtecoCodes = "01.11.00";
        onboarding.getInstitution().setInstitutionType(InstitutionType.PRV_PF);
        onboarding.getInstitution().setTaxCode(taxCode);
        onboarding.getInstitution().setAtecoCodes(List.of("01.11.00"));

        when(product.getId()).thenReturn("prod-idpay-merchant");

        PDNDBusinessResource pdndResource = new PDNDBusinessResource();
        pdndResource.setAtecoCodes(List.of("01.11.00"));

        when(pdndVisuraInfoCamereControllerApi.institutionVisuraPdndByTaxCodeUsingGET(taxCode))
                .thenReturn(Uni.createFrom().item(pdndResource));

        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi,
                Optional.of(allowedAtecoCodes),
                pdndVisuraInfoCamereControllerApi
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
        String allowedAtecoCodes = "01.11.00";
        onboarding.getInstitution().setInstitutionType(InstitutionType.PRV_PF);
        onboarding.getInstitution().setTaxCode(taxCode);
        onboarding.getInstitution().setAtecoCodes(List.of("01.11.00"));

        when(product.getId()).thenReturn("prod-idpay-merchant");

        PDNDBusinessResource pdndResource = new PDNDBusinessResource();
        pdndResource.setAtecoCodes(List.of("01.11.00"));

        when(pdndVisuraInfoCamereControllerApi.institutionVisuraPdndByTaxCodeUsingGET(taxCode))
                .thenReturn(Uni.createFrom().item(pdndResource));

        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi,
                Optional.of(allowedAtecoCodes),
                pdndVisuraInfoCamereControllerApi
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
                userRegistryApi,
                Optional.empty(),
                pdndVisuraInfoCamereControllerApi
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
                userRegistryApi,
                Optional.empty(),
                pdndVisuraInfoCamereControllerApi
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
                userRegistryApi,
                Optional.empty(),
                pdndVisuraInfoCamereControllerApi
        );

        // when
        Uni<Boolean> result = registryManager.isValid();

        // then
        Boolean isValid = result.await().indefinitely();
        assertTrue(isValid);
    }

    @Test
    void customValidation_withIdPayMerchantProduct_emptyAllowedAtecoCodesString() {
        // given
        when(product.getId()).thenReturn("prod-idpay-merchant");
        onboarding.getInstitution().setAtecoCodes(List.of("12.34", "56.78"));
        String allowedAtecoCodes = "";
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi,
                Optional.of(allowedAtecoCodes),
                pdndVisuraInfoCamereControllerApi
        );

        // when & then
        assertThrows(InvalidRequestException.class, () -> registryManager.customValidation(product));
    }

    @Test
    void customValidation_withIdPayMerchantProduct_blankAllowedAtecoCodesString() {
        // given
        when(product.getId()).thenReturn("prod-idpay-merchant");
        onboarding.getInstitution().setAtecoCodes(List.of("12.34", "56.78"));
        registryManager = new RegistryManagerPDNDInfocamere(
                onboarding,
                infocamerePdndApi,
                userRegistryApi,
                Optional.empty(),
                pdndVisuraInfoCamereControllerApi
        );

        // when & then
        assertThrows(InvalidRequestException.class, () -> registryManager.customValidation(product));
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
