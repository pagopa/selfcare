package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.AooResponse;
import it.pagopa.selfcare.onboarding.client.model.AggregateResult;
import it.pagopa.selfcare.onboarding.client.model.Billing;
import it.pagopa.selfcare.onboarding.client.model.BusinessInfoIC;
import it.pagopa.selfcare.onboarding.client.model.Certification;
import it.pagopa.selfcare.onboarding.client.model.CertifiedField;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.client.model.Institution;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfo;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.client.model.InstitutionLocation;
import it.pagopa.selfcare.onboarding.client.model.InstitutionOnboarding;
import it.pagopa.selfcare.onboarding.client.model.InstitutionOnboardingData;
import it.pagopa.selfcare.onboarding.client.model.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.client.model.InstitutionUpdate;
import it.pagopa.selfcare.onboarding.client.model.IpaInstitutionsSearchResult;
import it.pagopa.selfcare.onboarding.client.model.ManagerVerification;
import it.pagopa.selfcare.onboarding.client.model.MutableUserFieldsDto;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResource;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.client.model.PaymentServiceProvider;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.client.model.ProductRole;
import it.pagopa.selfcare.onboarding.client.model.ProductRoleInfo;
import it.pagopa.selfcare.onboarding.client.model.ProductStatus;
import it.pagopa.selfcare.onboarding.client.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.client.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.client.model.RegistryUser;
import it.pagopa.selfcare.onboarding.client.model.RowError;
import it.pagopa.selfcare.onboarding.client.model.UoResponse;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.client.model.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.client.model.WorkContact;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.service.PartyRegistryProxyService;
import it.pagopa.selfcare.onboarding.service.PartyService;
import it.pagopa.selfcare.onboarding.service.ProductService;
import it.pagopa.selfcare.onboarding.service.UserRegistryService;
import it.pagopa.selfcare.onboarding.util.PgManagerVerifier;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Response;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_functions_json.api.OrganizationApi;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingResponse;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceImplTest {

    private static final String INSTITUTION_ID = "institution-id";
    private static final String PRODUCT_ID = "prod-io";
    private static final String TAX_CODE = "00000000000";
    private static final String USER_UUID = "11111111-1111-4111-8111-111111111111";

    @Mock
    OnboardingService onboardingService;
    @Mock
    PartyService partyService;
    @Mock
    ProductService productService;
    @Mock
    UserRegistryService userRegistryService;
    @Mock
    OrganizationApi organizationApi;
    @Mock
    PartyRegistryProxyService registryProxyService;
    @Mock
    InstitutionMapper institutionMapper;
    @Mock
    OnboardingMapper onboardingMapper;
    @Mock
    PgManagerVerifier pgManagerVerifier;

    InstitutionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InstitutionServiceImpl(onboardingService, partyService, productService, userRegistryService,
                organizationApi, registryProxyService, institutionMapper, onboardingMapper, pgManagerVerifier);
    }

    @Test
    void onboardingProductV2_delegatesToOnboardingMs() {
        OnboardingData data = new OnboardingData();

        service.onboardingProductV2(data);

        verify(onboardingService).onboarding(data);
        verifyNoInteractions(partyService, productService);
    }

    @Test
    void onboardingPaAggregator_requiresAggregates() {
        OnboardingData withNull = new OnboardingData();
        OnboardingData withEmpty = new OnboardingData();
        withEmpty.setAggregates(List.of());

        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingPaAggregator(withNull));
        assertThrows(ValidationException.class, () -> service.onboardingPaAggregator(withEmpty));

        assertEquals("Aggregate institutions are required if given institution is an Aggregator", e.getMessage());
        verifyNoInteractions(onboardingService);
    }

    @Test
    void onboardingPaAggregator_delegatesWhenAggregatesArePresent() {
        OnboardingData data = new OnboardingData();
        data.setAggregates(List.of(new Institution()));

        service.onboardingPaAggregator(data);

        verify(onboardingService).onboardingPaAggregation(data);
    }

    @Test
    void onboardingCompanyV2_infocamereRequiresTheBusinessToBelongToTheUser() {
        OnboardingData data = new OnboardingData();
        data.setTaxCode(TAX_CODE);
        data.setOrigin("INFOCAMERE");
        InstitutionInfoIC businesses = new InstitutionInfoIC();
        BusinessInfoIC business = new BusinessInfoIC();
        business.setBusinessTaxId(TAX_CODE);
        businesses.setBusinesses(List.of(business));
        when(registryProxyService.getInstitutionsByUserFiscalCode("USER_CF")).thenReturn(businesses);

        service.onboardingCompanyV2(data, "USER_CF");

        verify(onboardingService).onboardingCompany(data);
    }

    @Test
    void onboardingCompanyV2_infocamereBusinessNotOwnedOrMissingIsNotAllowed() {
        OnboardingData data = new OnboardingData();
        data.setTaxCode(TAX_CODE);
        data.setOrigin("INFOCAMERE");
        InstitutionInfoIC other = new InstitutionInfoIC();
        BusinessInfoIC business = new BusinessInfoIC();
        business.setBusinessTaxId("99999999999");
        other.setBusinesses(List.of(business));
        when(registryProxyService.getInstitutionsByUserFiscalCode("USER_CF")).thenReturn(other);
        when(registryProxyService.getInstitutionsByUserFiscalCode("EMPTY_CF")).thenReturn(new InstitutionInfoIC());
        when(registryProxyService.getInstitutionsByUserFiscalCode("NULL_CF")).thenReturn(null);

        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingCompanyV2(data, "USER_CF"));
        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingCompanyV2(data, "EMPTY_CF"));
        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingCompanyV2(data, "NULL_CF"));
        verifyNoInteractions(onboardingService);
    }

    @Test
    void onboardingCompanyV2_adeRequiresAPositiveMatch() {
        OnboardingData data = new OnboardingData();
        data.setTaxCode(TAX_CODE);
        data.setOrigin("ADE");
        it.pagopa.selfcare.onboarding.client.model.MatchInfoResult match = new it.pagopa.selfcare.onboarding.client.model.MatchInfoResult();
        match.setVerificationResult(true);
        it.pagopa.selfcare.onboarding.client.model.MatchInfoResult noMatch = new it.pagopa.selfcare.onboarding.client.model.MatchInfoResult();
        noMatch.setVerificationResult(false);
        when(registryProxyService.matchInstitutionAndUser(TAX_CODE, "OK_CF")).thenReturn(match);
        when(registryProxyService.matchInstitutionAndUser(TAX_CODE, "KO_CF")).thenReturn(noMatch);
        when(registryProxyService.matchInstitutionAndUser(TAX_CODE, "NULL_CF")).thenReturn(null);

        service.onboardingCompanyV2(data, "OK_CF");
        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingCompanyV2(data, "KO_CF"));
        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingCompanyV2(data, "NULL_CF"));

        verify(onboardingService).onboardingCompany(data);
    }

    @Test
    void onboardingCompanyV2_unsupportedOriginIsABadRequest() {
        OnboardingData data = new OnboardingData();
        data.setOrigin("IPA");

        InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> service.onboardingCompanyV2(data, "USER_CF"));

        assertEquals("Origin not supported", e.getMessage());
        verifyNoInteractions(onboardingService, registryProxyService);
    }

    @Test
    void onboardingProduct_requiresEveryMandatorySection() {
        assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(null));

        OnboardingData noBilling = baseData(InstitutionType.PA, "IPA");
        noBilling.setBilling(null);
        assertEquals("Institution's billing data are required",
                assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(noBilling)).getMessage());

        OnboardingData noType = baseData(InstitutionType.PA, "IPA");
        noType.setInstitutionType(null);
        assertEquals("An institution type is required",
                assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(noType)).getMessage());

        OnboardingData noUpdate = baseData(InstitutionType.PA, "IPA");
        noUpdate.setInstitutionUpdate(null);
        assertEquals("InsitutionUpdate is required",
                assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(noUpdate)).getMessage());
        verifyNoInteractions(productService, partyService);
    }

    @Test
    void onboardingProduct_pspRequiresPspData() {
        OnboardingData data = baseData(InstitutionType.PSP, "IPA");

        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingProduct(data));

        assertEquals("Field 'pspData' is required for PSP institution onboarding", e.getMessage());
        verifyNoInteractions(productService);
    }

    @Test
    void onboardingProduct_locationIsRequiredOnlyOutsideIpaAdeInfocamere() {
        OnboardingData selc = baseData(InstitutionType.GSP, "SELC");
        selc.setLocation(null);

        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingProduct(selc));

        assertEquals("Location infos are required", e.getMessage());
        verifyNoInteractions(productService);
    }

    @Test
    void onboardingProduct_productNotFound() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(null);

        assertEquals("Product is required",
                assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(data)).getMessage());
    }

    @Test
    void onboardingProduct_ptRequiresADelegableProduct() {
        OnboardingData data = baseData(InstitutionType.PT, "IPA");
        Product product = product(false);
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PT)).thenReturn(product);

        OnboardingNotAllowedException e = assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingProduct(data));

        assertEquals("Institution with external id '" + TAX_CODE + "' is not allowed to onboard '" + PRODUCT_ID + "' product",
                e.getMessage());
        verifyNoInteractions(partyService);
    }

    @Test
    void onboardingProduct_phaseOutProductIsRejected() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        Product product = product(true);
        product.setStatus(ProductStatus.PHASE_OUT);
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(product);

        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingProduct(data));

        assertEquals("Unable to complete the onboarding for institution with taxCode '" + TAX_CODE
                + "' to product '" + PRODUCT_ID + "', the product is dismissed.", e.getMessage());
    }

    @Test
    void onboardingProduct_existingInstitutionAndUsers_newUserIsSavedAndRoleMapped() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        Institution institution = institution("ext-id");
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution));
        when(userRegistryService.search(eq("USER_TAX"), any())).thenReturn(Optional.empty());
        UserId userId = new UserId();
        userId.setId(UUID.fromString(USER_UUID));
        when(userRegistryService.saveUser(any())).thenReturn(userId);

        service.onboardingProduct(data);

        assertEquals("Prod IO", data.getProductName());
        assertEquals("path/pa.pdf", data.getContractPath());
        assertEquals("v1", data.getContractVersion());
        assertEquals("admin", data.getUsers().get(0).getProductRole());
        assertEquals(USER_UUID, data.getUsers().get(0).getId());
        assertEquals("ext-id", data.getInstitutionExternalId());
        verify(partyService).onboardingOrganization(data);
        verify(partyService, never()).createInstitution(any());
    }

    @Test
    void onboardingProduct_existingUserIsUpdatedWhenFieldsDiffer() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution("ext-id")));
        RegistryUser found = new RegistryUser();
        found.setId(USER_UUID);
        found.setName(certified(Certification.NONE, "OtherName"));
        found.setFamilyName(certified(Certification.NONE, "Rossi"));
        when(userRegistryService.search(eq("USER_TAX"), any())).thenReturn(Optional.of(found));

        service.onboardingProduct(data);

        ArgumentCaptor<MutableUserFieldsDto> update = ArgumentCaptor.forClass(MutableUserFieldsDto.class);
        verify(userRegistryService).updateUser(eq(UUID.fromString(USER_UUID)), update.capture());
        assertEquals("Mario", update.getValue().getName().getValue());
        assertNull(update.getValue().getFamilyName());
        assertEquals("mario@example.com",
                update.getValue().getWorkContacts().get("institution-id").getEmail().getValue());
        assertEquals(USER_UUID, data.getUsers().get(0).getId());
        verify(userRegistryService, never()).saveUser(any());
    }

    @Test
    void onboardingProduct_unchangedUserIsNotUpdated() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution("ext-id")));
        RegistryUser found = new RegistryUser();
        found.setId(USER_UUID);
        found.setName(certified(Certification.NONE, "Mario"));
        found.setFamilyName(certified(Certification.SPID, "ROSSI"));
        WorkContact contact = new WorkContact();
        contact.setEmail(certified(Certification.NONE, "mario@example.com"));
        found.setWorkContacts(Map.of("institution-id", contact));
        when(userRegistryService.search(eq("USER_TAX"), any())).thenReturn(Optional.of(found));

        service.onboardingProduct(data);

        verify(userRegistryService, never()).updateUser(any(), any());
        verify(partyService).onboardingOrganization(data);
    }

    @Test
    void onboardingProduct_certifiedValueMismatchIsNotAllowed() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution("ext-id")));
        RegistryUser found = new RegistryUser();
        found.setId(USER_UUID);
        found.setName(certified(Certification.SPID, "Another"));
        when(userRegistryService.search(eq("USER_TAX"), any())).thenReturn(Optional.of(found));

        assertThrows(UpdateNotAllowedException.class, () -> service.onboardingProduct(data));

        verify(partyService, never()).onboardingOrganization(any());
    }

    @Test
    void onboardingProduct_missingInstitutionIsCreatedFromTheRightSource() {
        assertInstitutionCreation(InstitutionType.SA, "ANAC", null, "createInstitutionFromANAC");
        assertInstitutionCreation(InstitutionType.AS, "IVASS", null, "createInstitutionFromIVASS");
        assertInstitutionCreation(InstitutionType.PG, "INFOCAMERE", null, "createInstitutionFromInfocamere");
        assertInstitutionCreation(InstitutionType.PG, "ADE", null, "createInstitutionFromInfocamere");
    }

    private void assertInstitutionCreation(InstitutionType type, String origin, String subunitType, String expectedCall) {
        PartyService party = mock(PartyService.class);
        ProductService products = mock(ProductService.class);
        UserRegistryService users = mock(UserRegistryService.class);
        InstitutionServiceImpl local = new InstitutionServiceImpl(onboardingService, party, products, users,
                organizationApi, registryProxyService, institutionMapper, onboardingMapper, pgManagerVerifier);
        OnboardingData data = baseData(type, origin);
        data.setSubunitType(subunitType);
        Product product = product(true);
        product.setInstitutionContractMappings(Map.of("DEFAULT", template("path/default.pdf", "v1")));
        product.setRoleMappingsByInstitutionType(null);
        product.setRoleMappings(roleMappings());
        when(products.getProduct(PRODUCT_ID, type)).thenReturn(product);
        when(products.isProductEnabled(PRODUCT_ID)).thenReturn(true);
        when(party.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of());
        Institution created = institution("created-ext");
        when(users.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));
        switch (expectedCall) {
            case "createInstitutionFromANAC" -> when(party.createInstitutionFromANAC(data)).thenReturn(created);
            case "createInstitutionFromIVASS" -> when(party.createInstitutionFromIVASS(data)).thenReturn(created);
            case "createInstitutionFromInfocamere" -> when(party.createInstitutionFromInfocamere(data)).thenReturn(created);
            default -> throw new IllegalArgumentException(expectedCall);
        }

        local.onboardingProduct(data);

        assertEquals("created-ext", data.getInstitutionExternalId());
        verify(party).onboardingOrganization(data);
        verify(party, never()).createInstitution(any());
    }

    @Test
    void onboardingProduct_missingInstitutionPresentOnIpaIsCreatedFromIpa() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        data.setSubunitType("AOO");
        data.setSubunitCode("AOO1");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, "AOO1")).thenThrow(new ResourceNotFoundException());
        when(registryProxyService.getAooById("AOO1")).thenReturn(new AooResponse());
        when(partyService.createInstitutionFromIpa(TAX_CODE, "AOO1", "AOO")).thenReturn(institution("ipa-ext"));
        when(userRegistryService.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));

        service.onboardingProduct(data);

        assertEquals("ipa-ext", data.getInstitutionExternalId());
        verify(partyService, never()).createInstitution(any());
    }

    @Test
    void onboardingProduct_uoAndPlainIpaLookups() {
        OnboardingData uo = baseData(InstitutionType.PA, "IPA");
        uo.setSubunitType("UO");
        uo.setSubunitCode("UO1");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, "UO1")).thenReturn(List.of());
        when(registryProxyService.getUoById("UO1")).thenReturn(new UoResponse());
        when(partyService.createInstitutionFromIpa(TAX_CODE, "UO1", "UO")).thenReturn(institution("uo-ext"));
        when(userRegistryService.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));

        service.onboardingProduct(uo);

        verify(registryProxyService).getUoById("UO1");
        assertEquals("uo-ext", uo.getInstitutionExternalId());
    }

    @Test
    void onboardingProduct_missingInstitutionNotOnIpaIsCreatedAsIs() {
        OnboardingData data = baseData(InstitutionType.GSP, "SELC");
        data.setLocation(new InstitutionLocation());
        stubProductAndGate(product(true), InstitutionType.GSP, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of());
        when(registryProxyService.getInstitutionProxyById(TAX_CODE)).thenThrow(new ResourceNotFoundException());
        when(partyService.createInstitution(data)).thenReturn(institution("plain-ext"));
        when(userRegistryService.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));

        service.onboardingProduct(data);

        assertEquals("plain-ext", data.getInstitutionExternalId());
        verify(partyService, never()).createInstitutionFromIpa(any(), any(), any());
    }

    @Test
    void onboardingProduct_roleMappingValidation() {
        Product noRoles = product(true);
        noRoles.setRoleMappingsByInstitutionType(Map.of("PA", Map.of()));
        OnboardingData missingRole = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(noRoles, InstitutionType.PA, true, false);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(missingRole));

        assertEquals("At least one Product role related to MANAGER Party role is required", e.getMessage());
    }

    @Test
    void onboardingProduct_moreThanOneProductRoleIsAmbiguous() {
        Product ambiguous = product(true);
        ProductRoleInfo info = new ProductRoleInfo();
        info.setRoles(List.of(productRole("admin"), productRole("operator")));
        ambiguous.setRoleMappingsByInstitutionType(Map.of("PA", Map.of(PartyRole.MANAGER, info)));
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(ambiguous, InstitutionType.PA, true, false);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> service.onboardingProduct(data));

        assertEquals("More than one Product role related to MANAGER Party role is available. Cannot automatically set the Product role",
                e.getMessage());
    }

    @Test
    void onboardingProduct_notEnabledProductAndNotAllowedInstitutionIsRejected() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        Product product = product(true);
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(product);
        when(productService.isProductEnabled(PRODUCT_ID)).thenReturn(false);
        when(productService.verifyAllowedByInstitutionTaxCode(PRODUCT_ID, TAX_CODE)).thenReturn(false);

        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingProduct(data));
        verifyNoInteractions(partyService);
    }

    @Test
    void onboardingProduct_allowedInstitutionPassesEvenWhenProductIsNotEnabled() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(product(true), InstitutionType.PA, false, true);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution("ext-id")));
        when(userRegistryService.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));

        service.onboardingProduct(data);

        verify(partyService).onboardingOrganization(data);
    }

    @Test
    void onboardingProduct_childProductRequiresTheParentOnboardingAndUsesParentRoles() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        Product child = product(true);
        child.setId("prod-child");
        child.setParentId("prod-base");
        child.setRoleMappingsByInstitutionType(null);
        child.setRoleMappings(null);
        Product base = product(true);
        base.setId("prod-base");
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(child);
        when(productService.getProduct("prod-base", null)).thenReturn(base);
        when(productService.isProductEnabled("prod-base")).thenReturn(true);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution("ext-id")));
        when(userRegistryService.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));

        service.onboardingProduct(data);

        verify(partyService).verifyOnboarding("prod-base", null, TAX_CODE, "IPA", null, null);
        assertEquals("admin", data.getUsers().get(0).getProductRole());
    }

    @Test
    void onboardingProduct_childProductWithoutParentOnboardingIsAValidationError() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        Product child = product(true);
        child.setId("prod-child");
        child.setParentId("prod-base");
        Product base = product(true);
        base.setId("prod-base");
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(child);
        when(productService.getProduct("prod-base", null)).thenReturn(base);
        when(productService.isProductEnabled("prod-base")).thenReturn(true);
        org.mockito.Mockito.doThrow(new ResourceNotFoundException()).when(partyService)
                .verifyOnboarding("prod-base", null, TAX_CODE, "IPA", null, null);

        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingProduct(data));

        assertEquals("Unable to complete the onboarding for institution with taxCode '" + TAX_CODE
                + "' to product 'prod-child'. Please onboard first the 'prod-base' product for the same institution", e.getMessage());
    }

    @Test
    void onboardingProduct_dismissedParentProductIsRejected() {
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        Product child = product(true);
        child.setParentId("prod-base");
        Product base = product(true);
        base.setId("prod-base");
        base.setStatus(ProductStatus.PHASE_OUT);
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(child);
        when(productService.getProduct("prod-base", null)).thenReturn(base);

        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingProduct(data));

        assertEquals("Unable to complete the onboarding for institution with taxCode '" + TAX_CODE
                + "' to product 'prod-base', the base product is dismissed.", e.getMessage());
    }

    @Test
    void getInstitutions_returnsTheUserInstitutions() {
        Product product = product(true);
        InstitutionInfo info = new InstitutionInfo();
        when(productService.getProduct(PRODUCT_ID, null)).thenReturn(product);
        when(partyService.getInstitutionsByUser(product, "uid")).thenReturn(List.of(info));

        assertEquals(List.of(info), service.getInstitutions(PRODUCT_ID, "uid"));
    }

    @Test
    void getInstitutions_unknownProductAndEmptyResultAreNotFound() {
        when(productService.getProduct("missing", null)).thenThrow(new ResourceNotFoundException("raw downstream"));
        Product product = product(true);
        when(productService.getProduct(PRODUCT_ID, null)).thenReturn(product);
        when(partyService.getInstitutionsByUser(product, "uid")).thenReturn(List.of());

        ResourceNotFoundException unknown = assertThrows(ResourceNotFoundException.class, () -> service.getInstitutions("missing", "uid"));
        ResourceNotFoundException empty = assertThrows(ResourceNotFoundException.class, () -> service.getInstitutions(PRODUCT_ID, "uid"));

        assertEquals("No product found with id missing", unknown.getMessage());
        assertEquals("No institutions found for product " + PRODUCT_ID, empty.getMessage());
    }

    @Test
    void ipaSearchAndLookup_delegateToThePartyRegistryProxy() {
        IpaInstitutionsSearchResult search = new IpaInstitutionsSearchResult();
        InstitutionProxyInfo found = new InstitutionProxyInfo();
        when(registryProxyService.searchIpaInstitutions("*", "L6", 0, 50)).thenReturn(search);
        when(registryProxyService.findIpaInstitutionByTaxCode(TAX_CODE, "L6")).thenReturn(found);

        assertSame(search, service.searchIpaInstitutions("*", "L6", 0, 50));
        assertSame(found, service.findIpaInstitutionByTaxCode(TAX_CODE, "L6"));
    }

    @Test
    void getActiveOnboarding_keepsOnlyActiveOnboardingsOfTheProduct() {
        Institution institution = institution("ext-id");
        institution.setOnboarding(List.of(
                onboarding(PRODUCT_ID, "ACTIVE"), onboarding(PRODUCT_ID, "PENDING"), onboarding("other", "ACTIVE")));
        Institution withoutOnboarding = institution("other-ext");
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, "sub")).thenReturn(List.of(institution, withoutOnboarding));

        List<Institution> result = service.getActiveOnboarding(TAX_CODE, PRODUCT_ID, "sub");

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getOnboarding().size());
        assertEquals("ACTIVE", result.get(0).getOnboarding().get(0).getStatus());
    }

    @Test
    void getActiveOnboarding_notFoundCases() {
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode("none", null)).thenReturn(List.of());
        Institution inactive = institution("ext-id");
        inactive.setOnboarding(List.of(onboarding(PRODUCT_ID, "PENDING")));
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode("inactive", null)).thenReturn(List.of(inactive));

        assertEquals("Institution not found",
                assertThrows(ResourceNotFoundException.class, () -> service.getActiveOnboarding("none", PRODUCT_ID, null)).getMessage());
        assertEquals("Institution doesn't have active onboarding for the given product",
                assertThrows(ResourceNotFoundException.class, () -> service.getActiveOnboarding("inactive", PRODUCT_ID, null)).getMessage());
    }

    @Test
    void getInstitutionOnboardingDataById_validatesInputAndMergesBilling() {
        assertEquals("An Institution id is required",
                assertThrows(IllegalArgumentException.class, () -> service.getInstitutionOnboardingDataById(" ", PRODUCT_ID)).getMessage());
        assertEquals("A Product Id is required",
                assertThrows(IllegalArgumentException.class, () -> service.getInstitutionOnboardingDataById(INSTITUTION_ID, null)).getMessage());

        OnboardingResource onboarding = new OnboardingResource();
        onboarding.setPricingPlan("C3");
        Billing billing = new Billing();
        billing.setVatNumber("vat");
        onboarding.setBilling(billing);
        Institution institution = institution("ext-id");
        institution.setGeographicTaxonomies(List.of(new GeographicTaxonomy()));
        InstitutionInfo info = new InstitutionInfo();
        when(partyService.getOnboardings(INSTITUTION_ID, PRODUCT_ID)).thenReturn(List.of(onboarding));
        when(partyService.getInstitutionById(INSTITUTION_ID, PRODUCT_ID)).thenReturn(institution);
        when(institutionMapper.toInstitutionInfo(institution)).thenReturn(info);

        InstitutionOnboardingData result = service.getInstitutionOnboardingDataById(INSTITUTION_ID, PRODUCT_ID);

        assertSame(info, result.getInstitution());
        assertEquals("C3", info.getPricingPlan());
        assertSame(billing, info.getBilling());
        assertEquals(1, result.getGeographicTaxonomies().size());
    }

    @Test
    void getInstitutionOnboardingDataById_noOnboardingIsNotFound() {
        when(partyService.getOnboardings(INSTITUTION_ID, PRODUCT_ID)).thenReturn(List.of());

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> service.getInstitutionOnboardingDataById(INSTITUTION_ID, PRODUCT_ID));

        assertEquals("Onboarding for institutionId institution-id not found", e.getMessage());
        verify(partyService, never()).getInstitutionById(any(), any());
    }

    @Test
    void getInstitutionOnboardingData_requiresInputs() {
        assertEquals("An Institution id is required",
                assertThrows(IllegalArgumentException.class, () -> service.getInstitutionOnboardingData(null, PRODUCT_ID)).getMessage());
        assertEquals("A Product Id is required",
                assertThrows(IllegalArgumentException.class, () -> service.getInstitutionOnboardingData(INSTITUTION_ID, " ")).getMessage());
    }

    @Test
    void getInstitutionOnboardingData_missingBillingOrInstitutionIsNotFound() {
        when(partyService.getInstitutionBillingData("none", PRODUCT_ID)).thenReturn(null);
        when(partyService.getInstitutionBillingData("noinst", PRODUCT_ID)).thenReturn(new InstitutionInfo());
        when(partyService.getInstitutionByExternalId("noinst")).thenReturn(null);

        assertEquals("Institution none not found",
                assertThrows(ResourceNotFoundException.class, () -> service.getInstitutionOnboardingData("none", PRODUCT_ID)).getMessage());
        assertEquals("Institution noinst not found",
                assertThrows(ResourceNotFoundException.class, () -> service.getInstitutionOnboardingData("noinst", PRODUCT_ID)).getMessage());
    }

    @Test
    void getInstitutionOnboardingData_missingTaxonomiesIsAValidationError() {
        when(partyService.getInstitutionBillingData("inst1", PRODUCT_ID)).thenReturn(new InstitutionInfo());
        Institution institution = institution("inst1");
        institution.setGeographicTaxonomies(null);
        when(partyService.getInstitutionByExternalId("inst1")).thenReturn(institution);

        ValidationException e = assertThrows(ValidationException.class, () -> service.getInstitutionOnboardingData("inst1", PRODUCT_ID));

        assertEquals("The institution inst1 does not have geographic taxonomies.", e.getMessage());
    }

    @Test
    void getInstitutionOnboardingData_copiesInstitutionLocationAndKeepsExistingCity() {
        InstitutionInfo info = new InstitutionInfo();
        info.setTaxCode(TAX_CODE);
        Institution institution = institution("ext");
        institution.setCity("Roma");
        institution.setCounty("RM");
        institution.setCountry("IT");
        institution.setOrigin("IPA");
        institution.setSubunitCode("AOO1");
        institution.setSubunitType("AOO");
        institution.setGeographicTaxonomies(List.of());
        when(partyService.getInstitutionBillingData("ext", PRODUCT_ID)).thenReturn(info);
        when(partyService.getInstitutionByExternalId("ext")).thenReturn(institution);

        InstitutionOnboardingData result = service.getInstitutionOnboardingData("ext", PRODUCT_ID);

        assertEquals("Roma", result.getInstitution().getInstitutionLocation().getCity());
        assertEquals("AOO", result.getInstitution().getSubunitType());
        assertEquals("IPA", result.getInstitution().getOrigin());
        verifyNoInteractions(registryProxyService);
    }

    @Test
    void getInstitutionOnboardingData_ipaLocationLookupBySubunitType() {
        assertLocationLookup("UO", "UO1");
        assertLocationLookup("AOO", "AOO1");
        assertLocationLookup(null, null);
        assertLocationLookup("OTHER", null);
    }

    private void assertLocationLookup(String subunitType, String subunitCode) {
        PartyService party = mock(PartyService.class);
        PartyRegistryProxyService proxy = mock(PartyRegistryProxyService.class);
        InstitutionServiceImpl local = new InstitutionServiceImpl(onboardingService, party, productService, userRegistryService,
                organizationApi, proxy, institutionMapper, onboardingMapper, pgManagerVerifier);
        InstitutionInfo info = new InstitutionInfo();
        info.setTaxCode(TAX_CODE);
        Institution institution = institution("ext");
        institution.setOrigin("IPA");
        institution.setSubunitType(subunitType);
        institution.setSubunitCode(subunitCode);
        institution.setGeographicTaxonomies(List.of());
        when(party.getInstitutionBillingData("ext", PRODUCT_ID)).thenReturn(info);
        when(party.getInstitutionByExternalId("ext")).thenReturn(institution);
        GeographicTaxonomiesResponse taxonomies = new GeographicTaxonomiesResponse();
        taxonomies.setProvinceAbbreviation("RM");
        taxonomies.setCountryAbbreviation("IT");
        taxonomies.setDescription("ROMA - COMUNE");
        UoResponse uo = new UoResponse();
        uo.setMunicipalIstatCode("istat-uo");
        AooResponse aoo = new AooResponse();
        aoo.setMunicipalIstatCode("istat-aoo");
        ProxyInstitutionResponse proxyInstitution = new ProxyInstitutionResponse();
        proxyInstitution.setIstatCode("istat-ipa");
        String expectedCode;
        if ("UO".equals(subunitType)) {
            when(proxy.getUoById(subunitCode)).thenReturn(uo);
            expectedCode = "istat-uo";
        } else if ("AOO".equals(subunitType)) {
            when(proxy.getAooById(subunitCode)).thenReturn(aoo);
            expectedCode = "istat-aoo";
        } else {
            when(proxy.getInstitutionProxyById(TAX_CODE)).thenReturn(proxyInstitution);
            expectedCode = "istat-ipa";
        }
        when(proxy.getExtById(expectedCode)).thenReturn(taxonomies);

        InstitutionOnboardingData result = local.getInstitutionOnboardingData("ext", PRODUCT_ID);

        InstitutionLocation location = result.getInstitution().getInstitutionLocation();
        assertEquals("ROMA", location.getCity());
        assertEquals("RM", location.getCounty());
        assertEquals("IT", location.getCountry());
    }

    @Test
    void getInstitutionOnboardingData_ipaLookupNotFoundIsSwallowed() {
        InstitutionInfo info = new InstitutionInfo();
        info.setTaxCode(TAX_CODE);
        Institution institution = institution("ext");
        institution.setOrigin("IPA");
        institution.setGeographicTaxonomies(List.of());
        when(partyService.getInstitutionBillingData("ext", PRODUCT_ID)).thenReturn(info);
        when(partyService.getInstitutionByExternalId("ext")).thenReturn(institution);
        when(registryProxyService.getInstitutionProxyById(TAX_CODE)).thenThrow(new ResourceNotFoundException());

        InstitutionOnboardingData result = service.getInstitutionOnboardingData("ext", PRODUCT_ID);

        assertNull(result.getInstitution().getInstitutionLocation().getCity());
    }

    @Test
    void getInstitutionByExternalId_requiresAnId() {
        Institution institution = institution("ext");
        when(partyService.getInstitutionByExternalId("ext")).thenReturn(institution);

        assertSame(institution, service.getInstitutionByExternalId("ext"));
        assertEquals("An Institution id is required",
                assertThrows(IllegalArgumentException.class, () -> service.getInstitutionByExternalId("")).getMessage());
    }

    @Test
    void getGeographicTaxonomyList_byExternalIdAndByTaxCode() {
        Institution institution = institution("ext");
        institution.setGeographicTaxonomies(List.of(new GeographicTaxonomy()));
        Institution withoutTaxonomies = institution("ext-2");
        when(partyService.getInstitutionByExternalId("ext")).thenReturn(institution);
        when(partyService.getInstitutionByExternalId("ext-2")).thenReturn(withoutTaxonomies);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution));
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode("none", null)).thenReturn(List.of());
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode("null", null)).thenReturn(null);

        assertEquals(1, service.getGeographicTaxonomyList("ext").size());
        assertTrue(service.getGeographicTaxonomyList("ext-2").isEmpty());
        assertEquals(1, service.getGeographicTaxonomyList(TAX_CODE, null).size());
        assertTrue(service.getGeographicTaxonomyList("none", null).isEmpty());
        assertTrue(service.getGeographicTaxonomyList("null", null).isEmpty());
        assertEquals("A taxCode id is required",
                assertThrows(IllegalArgumentException.class, () -> service.getGeographicTaxonomyList(" ", null)).getMessage());
    }

    @Test
    void verifyOnboarding_byExternalIdChecksAllowanceThenParty() {
        when(productService.isProductEnabled(PRODUCT_ID)).thenReturn(true);

        service.verifyOnboarding("ext", PRODUCT_ID);

        verify(partyService).verifyOnboarding("ext", PRODUCT_ID);
    }

    @Test
    void verifyOnboarding_notAllowedDoesNotCallTheParty() {
        when(productService.isProductEnabled(PRODUCT_ID)).thenReturn(false);
        when(productService.verifyAllowedByInstitutionTaxCode(PRODUCT_ID, "ext")).thenReturn(false);

        OnboardingNotAllowedException e = assertThrows(OnboardingNotAllowedException.class,
                () -> service.verifyOnboarding("ext", PRODUCT_ID));

        assertEquals("Institution with external id 'ext' is not allowed to onboard '" + PRODUCT_ID + "' product", e.getMessage());
        verifyNoInteractions(partyService);
    }

    @Test
    void verifyOnboarding_bySubunitRequiresAnotherParameterAlongWithProductId() {
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> service.verifyOnboarding(PRODUCT_ID, null, "", null, "", null));

        assertEquals("At least one other parameter must be provided along with productId", e.getMessage());
        verifyNoInteractions(productService, onboardingService);
    }

    @Test
    void verifyOnboarding_bySubunitDelegatesToOnboardingMs() {
        when(productService.isProductEnabled(PRODUCT_ID)).thenReturn(true);

        service.verifyOnboarding(PRODUCT_ID, TAX_CODE, "IPA", "origin-id", "sub", "PA");

        verify(onboardingService).verifyOnboarding(PRODUCT_ID, TAX_CODE, "IPA", "origin-id", "sub", "PA");
    }

    @Test
    void checkOrganization_callsTheOrganizationApi() {
        when(organizationApi.checkOrganization("CF", "VAT")).thenReturn(Uni.createFrom().item(Response.noContent().build()));

        service.checkOrganization(PRODUCT_ID, "CF", "VAT");

        verify(organizationApi).checkOrganization("CF", "VAT");
    }

    @Test
    void getInstitutionsByUser_removesBusinessesAlreadyOnboardedByTheUser() {
        InstitutionInfoIC result = new InstitutionInfoIC();
        BusinessInfoIC notOnboarded = business("1");
        BusinessInfoIC onboardedByUser = business("2");
        BusinessInfoIC onboardedByOther = business("3");
        result.setBusinesses(List.of(notOnboarded, onboardedByUser, onboardedByOther));
        when(registryProxyService.getInstitutionsByUserFiscalCode("USER_CF")).thenReturn(result);
        org.mockito.Mockito.doThrow(new ResourceNotFoundException()).when(onboardingService)
                .verifyOnboarding("prod-pn-pg", "1", null, null, null, null);
        UserId userId = new UserId();
        userId.setId(UUID.fromString(USER_UUID));
        when(userRegistryService.searchUser("USER_CF")).thenReturn(userId);
        CheckManagerRequest yes = new CheckManagerRequest();
        yes.setTaxCode("2");
        CheckManagerRequest no = new CheckManagerRequest();
        no.setTaxCode("3");
        when(onboardingMapper.toCheckManagerRequest(USER_UUID, "2", "prod-pn-pg")).thenReturn(yes);
        when(onboardingMapper.toCheckManagerRequest(USER_UUID, "3", "prod-pn-pg")).thenReturn(no);
        when(onboardingService.checkManager(yes)).thenReturn(true);
        when(onboardingService.checkManager(no)).thenReturn(false);

        InstitutionInfoIC filtered = service.getInstitutionsByUser("USER_CF");

        assertEquals(List.of(notOnboarded, onboardedByOther), filtered.getBusinesses());
    }

    @Test
    void getByFilters_mapsTheInstitutionsAndEmptyIsNotFound() {
        OnboardingResponse response = new OnboardingResponse();
        org.openapi.quarkus.onboarding_json.model.InstitutionResponse institutionResponse =
                new org.openapi.quarkus.onboarding_json.model.InstitutionResponse();
        response.setInstitution(institutionResponse);
        Institution mapped = institution("ext");
        when(onboardingService.getByFilters(PRODUCT_ID, TAX_CODE, null, null, null)).thenReturn(List.of(response));
        when(onboardingService.getByFilters("empty", null, null, null, null)).thenReturn(List.of());
        when(onboardingService.getByFilters("null", null, null, null, null)).thenReturn(null);
        when(institutionMapper.toInstitution(institutionResponse)).thenReturn(mapped);

        assertEquals(List.of(mapped), service.getByFilters(PRODUCT_ID, TAX_CODE, null, null, null));
        assertThrows(ResourceNotFoundException.class, () -> service.getByFilters("empty", null, null, null, null));
        assertThrows(ResourceNotFoundException.class, () -> service.getByFilters("null", null, null, null, null));
    }

    @Test
    void matchInstitutionAndUser_usesTheUserTaxCode() {
        User user = new User();
        user.setTaxCode("USER_CF");
        it.pagopa.selfcare.onboarding.client.model.MatchInfoResult expected = new it.pagopa.selfcare.onboarding.client.model.MatchInfoResult();
        when(registryProxyService.matchInstitutionAndUser("ext", "USER_CF")).thenReturn(expected);

        assertSame(expected, service.matchInstitutionAndUser("ext", user));
    }

    @Test
    void validateAggregatesCsv_keepsOnlyErrorsOrOnlyAggregates() {
        UploadedFile file = new UploadedFile("a.csv", "text/csv", new byte[] {1});
        VerifyAggregateResult withErrors = new VerifyAggregateResult();
        withErrors.setErrors(List.of(new RowError()));
        withErrors.setAggregates(List.of(new AggregateResult()));
        VerifyAggregateResult clean = new VerifyAggregateResult();
        clean.setAggregates(List.of(new AggregateResult()));
        when(onboardingService.aggregatesVerification(file, PRODUCT_ID)).thenReturn(withErrors).thenReturn(clean);

        VerifyAggregateResult first = service.validateAggregatesCsv(file, PRODUCT_ID);
        VerifyAggregateResult second = service.validateAggregatesCsv(file, PRODUCT_ID);

        assertEquals(1, first.getErrors().size());
        assertTrue(first.getAggregates().isEmpty());
        assertTrue(second.getErrors().isEmpty());
        assertEquals(1, second.getAggregates().size());
    }

    @Test
    void checkRecipientCode_delegates() {
        when(onboardingService.checkRecipientCode("origin", "ABCDEF")).thenReturn(RecipientCodeStatusResult.ACCEPTED);

        assertEquals(RecipientCodeStatusResult.ACCEPTED, service.checkRecipientCode("origin", "ABCDEF"));
    }

    @Test
    void onboardingUsersPgFromIcAndAde_delegates() {
        OnboardingData data = new OnboardingData();

        service.onboardingUsersPgFromIcAndAde(data);

        verify(onboardingService).onboardingUsersPgFromIcAndAde(data);
    }

    @Test
    void verifyManager_returnsVerifiedAndFailsOtherwiseWithTheLegalRepresentativeMessage() {
        ManagerVerification verified = new ManagerVerification();
        verified.setVerified(true);
        ManagerVerification notVerified = new ManagerVerification();
        when(pgManagerVerifier.doVerify("OK_CF", "COMPANY")).thenReturn(verified);
        when(pgManagerVerifier.doVerify("KO_CF", "COMPANY")).thenReturn(notVerified);

        assertSame(verified, service.verifyManager("OK_CF", "COMPANY"));
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, () -> service.verifyManager("KO_CF", "COMPANY"));

        assertEquals("User with userTaxCode KO_CF is not the legal representative of the institution", e.getMessage());
    }

    @Test
    void getOnboardingWithFilter_delegatesTheEncodedFilters() {
        OnboardingResult result = new OnboardingResult();
        when(onboardingService.onboardingWithFilter("TAX", "COMPLETED")).thenReturn(List.of(result));

        assertEquals(List.of(result), service.getOnboardingWithFilter("TAX", "COMPLETED"));
    }

    @Test
    void triggerOnboardingRequest_delegates() {
        service.triggerOnboardingRequest("42");

        verify(onboardingService).triggerOnboardingRequest("42");
        verifyNoMoreInteractions(onboardingService);
    }

    @Test
    void validateOnboardingByProductOrInstitutionTaxCode_passesWhenEitherCheckPasses() {
        when(productService.isProductEnabled("enabled")).thenReturn(true);
        when(productService.isProductEnabled("restricted")).thenReturn(false);
        when(productService.verifyAllowedByInstitutionTaxCode("enabled", TAX_CODE)).thenReturn(false);
        when(productService.verifyAllowedByInstitutionTaxCode("restricted", TAX_CODE)).thenReturn(true);
        when(productService.isProductEnabled("blocked")).thenReturn(false);
        when(productService.verifyAllowedByInstitutionTaxCode("blocked", TAX_CODE)).thenReturn(false);

        service.validateOnboardingByProductOrInstitutionTaxCode(TAX_CODE, "enabled");
        service.validateOnboardingByProductOrInstitutionTaxCode(TAX_CODE, "restricted");
        assertThrows(OnboardingNotAllowedException.class,
                () -> service.validateOnboardingByProductOrInstitutionTaxCode(TAX_CODE, "blocked"));
        assertFalse(EnumSet.noneOf(RegistryUser.Fields.class).contains(RegistryUser.Fields.name));
    }

    private void stubProductAndGate(Product product, InstitutionType type, boolean enabled, boolean allowed) {
        when(productService.getProduct(PRODUCT_ID, type)).thenReturn(product);
        when(productService.isProductEnabled(product.getParentId() == null ? product.getId() : product.getParentId())).thenReturn(enabled);
        when(productService.verifyAllowedByInstitutionTaxCode(
                product.getParentId() == null ? product.getId() : product.getParentId(), TAX_CODE)).thenReturn(allowed);
    }

    private static OnboardingData baseData(InstitutionType type, String origin) {
        OnboardingData data = new OnboardingData();
        data.setProductId(PRODUCT_ID);
        data.setTaxCode(TAX_CODE);
        data.setInstitutionType(type);
        data.setOrigin(origin);
        data.setBilling(new Billing());
        data.setLocation(new InstitutionLocation());
        InstitutionUpdate update = new InstitutionUpdate();
        if (type == InstitutionType.PSP) {
            update.setPaymentServiceProvider(null);
        } else {
            update.setPaymentServiceProvider(new PaymentServiceProvider());
        }
        data.setInstitutionUpdate(update);
        User manager = new User();
        manager.setTaxCode("USER_TAX");
        manager.setName("Mario");
        manager.setSurname("Rossi");
        manager.setEmail("mario@example.com");
        manager.setRole(PartyRole.MANAGER);
        data.setUsers(List.of(manager));
        return data;
    }

    private static Product product(boolean delegable) {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setTitle("Prod IO");
        product.setStatus(ProductStatus.ACTIVE);
        product.setDelegable(delegable);
        product.setInstitutionContractMappings(Map.of("PA", template("path/pa.pdf", "v1"), "DEFAULT", template("path/default.pdf", "v1")));
        product.setRoleMappingsByInstitutionType(Map.of("PA", roleMappings(), "GSP", roleMappings(), "PT", roleMappings()));
        product.setRoleMappings(roleMappings());
        return product;
    }

    private static it.pagopa.selfcare.onboarding.client.model.ContractTemplate template(String path, String version) {
        it.pagopa.selfcare.onboarding.client.model.ContractTemplate template = new it.pagopa.selfcare.onboarding.client.model.ContractTemplate();
        template.setContractTemplatePath(path);
        template.setContractTemplateVersion(version);
        return template;
    }

    private static Map<PartyRole, ProductRoleInfo> roleMappings() {
        ProductRoleInfo info = new ProductRoleInfo();
        info.setRoles(List.of(productRole("admin")));
        return Map.of(PartyRole.MANAGER, info);
    }

    private static ProductRole productRole(String code) {
        ProductRole role = new ProductRole();
        role.setCode(code);
        return role;
    }

    private static Institution institution(String externalId) {
        Institution institution = new Institution();
        institution.setId(INSTITUTION_ID);
        institution.setExternalId(externalId);
        return institution;
    }

    private static InstitutionOnboarding onboarding(String productId, String status) {
        InstitutionOnboarding onboarding = new InstitutionOnboarding();
        onboarding.setProductId(productId);
        onboarding.setStatus(status);
        return onboarding;
    }

    private static BusinessInfoIC business(String taxId) {
        BusinessInfoIC business = new BusinessInfoIC();
        business.setBusinessTaxId(taxId);
        return business;
    }

    private static RegistryUser registeredUser() {
        RegistryUser found = new RegistryUser();
        found.setId(USER_UUID);
        found.setName(certified(Certification.NONE, "Mario"));
        found.setFamilyName(certified(Certification.NONE, "Rossi"));
        WorkContact contact = new WorkContact();
        contact.setEmail(certified(Certification.NONE, "mario@example.com"));
        found.setWorkContacts(Map.of(INSTITUTION_ID, contact));
        return found;
    }

    private static CertifiedField<String> certified(Certification certification, String value) {
        CertifiedField<String> field = new CertifiedField<>();
        field.setCertification(certification);
        field.setValue(value);
        return field;
    }
}
