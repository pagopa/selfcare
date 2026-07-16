package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfo;
import it.pagopa.selfcare.onboarding.client.model.ManagerVerification;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.service.PartyRegistryProxyService;
import it.pagopa.selfcare.onboarding.service.PartyService;
import it.pagopa.selfcare.onboarding.service.UserRegistryService;
import it.pagopa.selfcare.onboarding.util.PgManagerVerifier;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_functions_json.api.OrganizationApi;
import org.openapi.quarkus.onboarding_json.api.AggregatesControllerApi;
import org.openapi.quarkus.onboarding_json.api.BillingPortalApi;
import org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus;
import org.openapi.quarkus.onboarding_json.model.VerifyAggregateResponse;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceImplTest {

    @InjectMocks
    private InstitutionServiceImpl institutionService;

    @Mock
    private OnboardingService onboardingMsConnector;

    @Mock
    private PartyService partyConnector;

    @Mock
    private ProductService productService;

    @Mock
    private UserRegistryService userConnector;

    @Mock
    private OrganizationApi organizationApi;

    @Mock
    private PartyRegistryProxyService partyRegistryProxyConnector;

    @Mock
    private InstitutionMapper institutionMapper;

    @Mock
    private OnboardingMapper onboardingMapper;

    @Mock
    private PgManagerVerifier pgManagerVerifier;

    @Mock
    private AggregatesControllerApi aggregatesApi;

    @Mock
    private BillingPortalApi billingPortalApi;

    @Test
    void onboardingProduct_nullOnboardingData_throwsNullPointerException() {
        // given / when / then
        assertThrows(NullPointerException.class, () -> institutionService.onboardingProduct(null));
    }

    @Test
    void onboardingProduct_ptInstitutionNonDelegableProduct_throwsOnboardingNotAllowedException() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setTaxCode("taxCode");
        onboardingData.setProductId("prod-test");
        onboardingData.setInstitutionType(InstitutionType.PT);
        onboardingData.setOrigin(Origin.IPA.getValue());
        it.pagopa.selfcare.onboarding.client.model.Billing billing = new it.pagopa.selfcare.onboarding.client.model.Billing();
        onboardingData.setBilling(billing);
        it.pagopa.selfcare.onboarding.client.model.InstitutionUpdate institutionUpdate = new it.pagopa.selfcare.onboarding.client.model.InstitutionUpdate();
        onboardingData.setInstitutionUpdate(institutionUpdate);

        Product product = new Product();
        product.setId("prod-test");
        product.setDelegable(false);
        product.setStatus(ProductStatus.ACTIVE);
        when(productService.getProduct("prod-test")).thenReturn(product);

        // when / then
        assertThrows(OnboardingNotAllowedException.class,
                () -> institutionService.onboardingProduct(onboardingData));
    }

    @Test
    void onboardingProduct_dismissedProduct_throwsValidationException() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setTaxCode("taxCode");
        onboardingData.setProductId("prod-test");
        onboardingData.setInstitutionType(InstitutionType.PA);
        onboardingData.setOrigin(Origin.IPA.getValue());
        it.pagopa.selfcare.onboarding.client.model.Billing billing = new it.pagopa.selfcare.onboarding.client.model.Billing();
        onboardingData.setBilling(billing);
        it.pagopa.selfcare.onboarding.client.model.InstitutionUpdate institutionUpdate = new it.pagopa.selfcare.onboarding.client.model.InstitutionUpdate();
        onboardingData.setInstitutionUpdate(institutionUpdate);

        Product product = new Product();
        product.setId("prod-test");
        product.setDelegable(true);
        product.setStatus(ProductStatus.PHASE_OUT);
        when(productService.getProduct("prod-test")).thenReturn(product);

        // when / then
        assertThrows(ValidationException.class,
                () -> institutionService.onboardingProduct(onboardingData));
    }

    @Test
    void getInstitutions_happyPath_returnsList() {
        // given
        String productId = "prod-test";
        String userId = "user-uid";
        Product product = new Product();
        product.setId(productId);
        List<InstitutionInfo> expected = List.of(new InstitutionInfo());

        when(productService.getProduct(productId)).thenReturn(product);
        when(partyConnector.getInstitutionsByUser(product, userId)).thenReturn(expected);

        // when
        List<InstitutionInfo> result = institutionService.getInstitutions(productId, userId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getInstitutions_productNotFound_throwsResourceNotFoundException() {
        // given
        String productId = "prod-missing";
        String userId = "user-uid";
        when(productService.getProduct(productId)).thenThrow(new ProductNotFoundException("not found"));

        // when / then
        assertThrows(ResourceNotFoundException.class,
                () -> institutionService.getInstitutions(productId, userId));
    }

    @Test
    void checkOrganization_happyPath_callsOrganizationApi() {
        // given
        String productId = "prod-test";
        String fiscalCode = "FSCPRD00A00H501X";
        String vatNumber = "12345678901";
        when(organizationApi.checkOrganization(fiscalCode, vatNumber))
                .thenReturn(Uni.createFrom().item(Response.ok().build()));

        // when
        institutionService.checkOrganization(productId, fiscalCode, vatNumber);

        // then
        verify(organizationApi).checkOrganization(fiscalCode, vatNumber);
    }

    @Test
    void checkRecipientCode_happyPath_returnsStatus() {
        // given
        String originId = "origin-id";
        String recipientCode = "RC123";
        RecipientCodeStatus expected = RecipientCodeStatus.ACCEPTED;
        when(billingPortalApi.checkRecipientCode(originId, recipientCode))
                .thenReturn(Uni.createFrom().item(expected));

        // when
        RecipientCodeStatus result = institutionService.checkRecipientCode(originId, recipientCode);

        // then
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void validateAggregatesCsv_prodIo_returnsResult() {
        // given
        String productId = "prod-io";
        UploadedFile file = new UploadedFile("aggregates.csv", "text/csv", new byte[]{1, 2, 3});
        VerifyAggregateResponse expected = new VerifyAggregateResponse();
        when(aggregatesApi.verifyAppIoAggregatesCsv(any()))
                .thenReturn(Uni.createFrom().item(expected));

        // when
        VerifyAggregateResponse result = institutionService.validateAggregatesCsv(file, productId);

        // then
        assertNotNull(result);
    }

    @Test
    void validateAggregatesCsv_unsupportedProductId_throwsInvalidRequestException() {
        // given
        String productId = "prod-unsupported";
        UploadedFile file = new UploadedFile("aggregates.csv", "text/csv", new byte[]{1, 2, 3});

        // when / then
        assertThrows(it.pagopa.selfcare.onboarding.exception.InvalidRequestException.class,
                () -> institutionService.validateAggregatesCsv(file, productId));
    }

    @Test
    void verifyManager_verifiedUser_returnsManagerVerification() {
        // given
        String userTaxCode = "FSCPRD00A00H501X";
        String institutionTaxCode = "12345678901";
        ManagerVerification verification = new ManagerVerification();
        verification.setVerified(true);
        when(pgManagerVerifier.doVerify(userTaxCode, institutionTaxCode)).thenReturn(verification);

        // when
        ManagerVerification result = institutionService.verifyManager(userTaxCode, institutionTaxCode);

        // then
        assertNotNull(result);
    }

    @Test
    void verifyManager_notVerifiedUser_throwsResourceNotFoundException() {
        // given
        String userTaxCode = "FSCPRD00A00H501X";
        String institutionTaxCode = "12345678901";
        ManagerVerification verification = new ManagerVerification();
        verification.setVerified(false);
        when(pgManagerVerifier.doVerify(userTaxCode, institutionTaxCode)).thenReturn(verification);

        // when / then
        assertThrows(ResourceNotFoundException.class,
                () -> institutionService.verifyManager(userTaxCode, institutionTaxCode));
    }
}
