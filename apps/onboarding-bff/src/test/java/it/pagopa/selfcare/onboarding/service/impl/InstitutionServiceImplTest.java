package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
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
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.service.impl.PartyRegistryProxyService;
import it.pagopa.selfcare.onboarding.service.impl.PartyService;
import it.pagopa.selfcare.onboarding.service.ProductService;
import it.pagopa.selfcare.onboarding.service.impl.UserRegistryService;
import it.pagopa.selfcare.onboarding.util.PgManagerVerifier;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Response;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
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
    @Spy
    InstitutionMapper institutionMapper = new InstitutionMapperImpl();
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
        // given
        OnboardingData data = new OnboardingData();
        when(onboardingService.onboarding(data)).thenReturn(Uni.createFrom().voidItem());

        // when
        service.onboardingProductV2(data).await().indefinitely();

        // then
        verify(onboardingService).onboarding(data);
        verifyNoInteractions(partyService, productService);
    }

    @Test
    void onboardingPaAggregator_requiresAggregates() {
        // given
        OnboardingData withNull = new OnboardingData();
        OnboardingData withEmpty = new OnboardingData();
        withEmpty.setAggregates(List.of());

        // when
        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingPaAggregator(withNull).await().indefinitely());
        assertThrows(ValidationException.class, () -> service.onboardingPaAggregator(withEmpty).await().indefinitely());

        // then
        assertEquals("Aggregate institutions are required if given institution is an Aggregator", e.getMessage());
        verifyNoInteractions(onboardingService);
    }

    @Test
    void onboardingPaAggregator_delegatesWhenAggregatesArePresent() {
        // given
        OnboardingData data = new OnboardingData();
        data.setAggregates(List.of(new Institution()));
        when(onboardingService.onboardingPaAggregation(data)).thenReturn(Uni.createFrom().voidItem());

        // when
        service.onboardingPaAggregator(data).await().indefinitely();

        // then
        verify(onboardingService).onboardingPaAggregation(data);
    }

    @Test
    void onboardingCompanyV2_infocamereRequiresTheBusinessToBelongToTheUser() {
        // given
        OnboardingData data = new OnboardingData();
        data.setTaxCode(TAX_CODE);
        data.setOrigin("INFOCAMERE");
        InstitutionInfoIC businesses = new InstitutionInfoIC();
        BusinessInfoIC business = new BusinessInfoIC();
        business.setBusinessTaxId(TAX_CODE);
        businesses.setBusinesses(List.of(business));
        when(registryProxyService.getInstitutionsByUserFiscalCode("USER_CF")).thenReturn(businesses);

        // when
        service.onboardingCompanyV2(data, "USER_CF").await().indefinitely();

        // then
        verify(onboardingService).onboardingCompany(data);
    }

    @Test
    void onboardingCompanyV2_infocamereBusinessNotOwnedOrMissingIsNotAllowed() {
        // given
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

        // when
        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingCompanyV2(data, "USER_CF").await().indefinitely());
        // then
        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingCompanyV2(data, "EMPTY_CF").await().indefinitely());
        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingCompanyV2(data, "NULL_CF").await().indefinitely());
        verifyNoInteractions(onboardingService);
    }

    @Test
    void onboardingCompanyV2_adeRequiresAPositiveMatch() {
        // given
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

        // when
        service.onboardingCompanyV2(data, "OK_CF").await().indefinitely();
        // then
        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingCompanyV2(data, "KO_CF").await().indefinitely());
        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingCompanyV2(data, "NULL_CF").await().indefinitely());

        verify(onboardingService).onboardingCompany(data);
    }

    @Test
    void onboardingCompanyV2_unsupportedOriginIsABadRequest() {
        // given
        OnboardingData data = new OnboardingData();
        data.setOrigin("IPA");

        // when
        InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> service.onboardingCompanyV2(data, "USER_CF").await().indefinitely());

        // then
        assertEquals("Origin not supported", e.getMessage());
        verifyNoInteractions(onboardingService, registryProxyService);
    }

    @Test
    void onboardingProduct_requiresEveryMandatorySection() {
        // given
        // when
        assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(null).await().indefinitely());

        OnboardingData noBilling = baseData(InstitutionType.PA, "IPA");
        noBilling.setBilling(null);
        // then
        assertEquals("Institution's billing data are required",
                assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(noBilling).await().indefinitely()).getMessage());

        OnboardingData noType = baseData(InstitutionType.PA, "IPA");
        noType.setInstitutionType(null);
        assertEquals("An institution type is required",
                assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(noType).await().indefinitely()).getMessage());

        OnboardingData noUpdate = baseData(InstitutionType.PA, "IPA");
        noUpdate.setInstitutionUpdate(null);
        assertEquals("InsitutionUpdate is required",
                assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(noUpdate).await().indefinitely()).getMessage());
        verifyNoInteractions(productService, partyService);
    }

    @Test
    void onboardingProduct_pspRequiresPspData() {
        // given
        OnboardingData data = baseData(InstitutionType.PSP, "IPA");

        // when
        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingProduct(data).await().indefinitely());

        // then
        assertEquals("Field 'pspData' is required for PSP institution onboarding", e.getMessage());
        verifyNoInteractions(productService);
    }

    @Test
    void onboardingProduct_locationIsRequiredOnlyOutsideIpaAdeInfocamere() {
        // given
        OnboardingData selc = baseData(InstitutionType.GSP, "SELC");
        selc.setLocation(null);

        // when
        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingProduct(selc).await().indefinitely());

        // then
        assertEquals("Location infos are required", e.getMessage());
        verifyNoInteractions(productService);
    }

    @Test
    void onboardingProduct_productNotFound() {
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(Uni.createFrom().nullItem());

        // when
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.onboardingProduct(data).await().indefinitely());

        // then
        assertEquals("Product is required", failure.getMessage());
    }

    @Test
    void onboardingProduct_ptRequiresADelegableProduct() {
        // given
        OnboardingData data = baseData(InstitutionType.PT, "IPA");
        Product product = product(false);
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PT)).thenReturn(Uni.createFrom().item(product));

        // when
        OnboardingNotAllowedException e = assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingProduct(data).await().indefinitely());

        // then
        assertEquals("Institution with external id '" + TAX_CODE + "' is not allowed to onboard '" + PRODUCT_ID + "' product",
                e.getMessage());
        verifyNoInteractions(partyService);
    }

    @Test
    void onboardingProduct_phaseOutProductIsRejected() {
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        Product product = product(true);
        product.setStatus(ProductStatus.PHASE_OUT);
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(Uni.createFrom().item(product));

        // when
        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingProduct(data).await().indefinitely());

        // then
        assertEquals("Unable to complete the onboarding for institution with taxCode '" + TAX_CODE
                + "' to product '" + PRODUCT_ID + "', the product is dismissed.", e.getMessage());
    }

    @Test
    void onboardingProduct_existingInstitutionAndUsers_newUserIsSavedAndRoleMapped() {
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        Institution institution = institution("ext-id");
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution));
        when(userRegistryService.search(eq("USER_TAX"), any())).thenReturn(Optional.empty());
        UserId userId = new UserId();
        userId.setId(UUID.fromString(USER_UUID));
        when(userRegistryService.saveUser(any())).thenReturn(userId);

        // when
        service.onboardingProduct(data).await().indefinitely();

        // then
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
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution("ext-id")));
        RegistryUser found = new RegistryUser();
        found.setId(USER_UUID);
        found.setName(certified(Certification.NONE, "OtherName"));
        found.setFamilyName(certified(Certification.NONE, "Rossi"));
        when(userRegistryService.search(eq("USER_TAX"), any())).thenReturn(Optional.of(found));

        // when
        service.onboardingProduct(data).await().indefinitely();

        ArgumentCaptor<MutableUserFieldsDto> update = ArgumentCaptor.forClass(MutableUserFieldsDto.class);
        // then
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
        // given
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

        // when
        service.onboardingProduct(data).await().indefinitely();

        // then
        verify(userRegistryService, never()).updateUser(any(), any());
        verify(partyService).onboardingOrganization(data);
    }

    @Test
    void onboardingProduct_certifiedValueMismatchIsNotAllowed() {
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution("ext-id")));
        RegistryUser found = new RegistryUser();
        found.setId(USER_UUID);
        found.setName(certified(Certification.SPID, "Another"));
        when(userRegistryService.search(eq("USER_TAX"), any())).thenReturn(Optional.of(found));

        // when
        assertThrows(UpdateNotAllowedException.class, () -> service.onboardingProduct(data).await().indefinitely());

        // then
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
        // given
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
        when(products.getProduct(PRODUCT_ID, type)).thenReturn(Uni.createFrom().item(product));
        when(products.isProductEnabled(PRODUCT_ID)).thenReturn(Uni.createFrom().item(true));
        when(products.verifyAllowedByInstitutionTaxCode(PRODUCT_ID, TAX_CODE)).thenReturn(Uni.createFrom().item(false));
        when(party.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of());
        Institution created = institution("created-ext");
        when(users.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));
        switch (expectedCall) {
            case "createInstitutionFromANAC" -> when(party.createInstitutionFromANAC(data)).thenReturn(created);
            case "createInstitutionFromIVASS" -> when(party.createInstitutionFromIVASS(data)).thenReturn(created);
            case "createInstitutionFromInfocamere" -> when(party.createInstitutionFromInfocamere(data)).thenReturn(created);
            default -> throw new IllegalArgumentException(expectedCall);
        }

        // when
        local.onboardingProduct(data).await().indefinitely();

        // then
        assertEquals("created-ext", data.getInstitutionExternalId());
        verify(party).onboardingOrganization(data);
        verify(party, never()).createInstitution(any());
    }

    @Test
    void onboardingProduct_missingInstitutionPresentOnIpaIsCreatedFromIpa() {
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        data.setSubunitType("AOO");
        data.setSubunitCode("AOO1");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, "AOO1")).thenThrow(new ResourceNotFoundException());
        when(registryProxyService.getAooById("AOO1")).thenReturn(new AooResponse());
        when(partyService.createInstitutionFromIpa(TAX_CODE, "AOO1", "AOO")).thenReturn(institution("ipa-ext"));
        when(userRegistryService.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));

        // when
        service.onboardingProduct(data).await().indefinitely();

        // then
        assertEquals("ipa-ext", data.getInstitutionExternalId());
        verify(partyService, never()).createInstitution(any());
    }

    @Test
    void onboardingProduct_uoAndPlainIpaLookups() {
        // given
        OnboardingData uo = baseData(InstitutionType.PA, "IPA");
        uo.setSubunitType("UO");
        uo.setSubunitCode("UO1");
        stubProductAndGate(product(true), InstitutionType.PA, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, "UO1")).thenReturn(List.of());
        when(registryProxyService.getUoById("UO1")).thenReturn(new UoResponse());
        when(partyService.createInstitutionFromIpa(TAX_CODE, "UO1", "UO")).thenReturn(institution("uo-ext"));
        when(userRegistryService.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));

        // when
        service.onboardingProduct(uo).await().indefinitely();

        // then
        verify(registryProxyService).getUoById("UO1");
        assertEquals("uo-ext", uo.getInstitutionExternalId());
    }

    @Test
    void onboardingProduct_missingInstitutionNotOnIpaIsCreatedAsIs() {
        // given
        OnboardingData data = baseData(InstitutionType.GSP, "SELC");
        data.setLocation(new InstitutionLocation());
        stubProductAndGate(product(true), InstitutionType.GSP, true, false);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of());
        when(registryProxyService.getInstitutionProxyById(TAX_CODE)).thenThrow(new ResourceNotFoundException());
        when(partyService.createInstitution(data)).thenReturn(institution("plain-ext"));
        when(userRegistryService.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));

        // when
        service.onboardingProduct(data).await().indefinitely();

        // then
        assertEquals("plain-ext", data.getInstitutionExternalId());
        verify(partyService, never()).createInstitutionFromIpa(any(), any(), any());
    }

    @Test
    void onboardingProduct_roleMappingValidation() {
        // given
        Product noRoles = product(true);
        noRoles.setRoleMappingsByInstitutionType(Map.of("PA", Map.of()));
        OnboardingData missingRole = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(noRoles, InstitutionType.PA, true, false);

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.onboardingProduct(missingRole).await().indefinitely());

        // then
        assertEquals("At least one Product role related to MANAGER Party role is required", e.getMessage());
    }

    @Test
    void onboardingProduct_moreThanOneProductRoleIsAmbiguous() {
        // given
        Product ambiguous = product(true);
        ProductRoleInfo info = new ProductRoleInfo();
        info.setRoles(List.of(productRole("admin"), productRole("operator")));
        ambiguous.setRoleMappingsByInstitutionType(Map.of("PA", Map.of(PartyRole.MANAGER, info)));
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(ambiguous, InstitutionType.PA, true, false);

        // when
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> service.onboardingProduct(data).await().indefinitely());

        // then
        assertEquals("More than one Product role related to MANAGER Party role is available. Cannot automatically set the Product role",
                e.getMessage());
    }

    @Test
    void onboardingProduct_notEnabledProductAndNotAllowedInstitutionIsRejected() {
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        Product product = product(true);
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(Uni.createFrom().item(product));
        when(productService.isProductEnabled(PRODUCT_ID)).thenReturn(Uni.createFrom().item(false));
        when(productService.verifyAllowedByInstitutionTaxCode(PRODUCT_ID, TAX_CODE)).thenReturn(Uni.createFrom().item(false));

        // when
        assertThrows(OnboardingNotAllowedException.class, () -> service.onboardingProduct(data).await().indefinitely());
        // then
        verifyNoInteractions(partyService);
    }

    @Test
    void onboardingProduct_allowedInstitutionPassesEvenWhenProductIsNotEnabled() {
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        stubProductAndGate(product(true), InstitutionType.PA, false, true);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution("ext-id")));
        when(userRegistryService.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));

        // when
        service.onboardingProduct(data).await().indefinitely();

        // then
        verify(partyService).onboardingOrganization(data);
    }

    @Test
    void onboardingProduct_childProductRequiresTheParentOnboardingAndUsesParentRoles() {
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        Product child = product(true);
        child.setId("prod-child");
        child.setParentId("prod-base");
        child.setRoleMappingsByInstitutionType(null);
        child.setRoleMappings(null);
        Product base = product(true);
        base.setId("prod-base");
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(Uni.createFrom().item(child));
        when(productService.getProduct("prod-base", null)).thenReturn(Uni.createFrom().item(base));
        when(productService.isProductEnabled("prod-base")).thenReturn(Uni.createFrom().item(true));
        when(productService.verifyAllowedByInstitutionTaxCode("prod-base", TAX_CODE)).thenReturn(Uni.createFrom().item(false));
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution("ext-id")));
        when(userRegistryService.search(anyString(), any())).thenReturn(Optional.of(registeredUser()));

        // when
        service.onboardingProduct(data).await().indefinitely();

        // then
        verify(partyService).verifyOnboarding("prod-base", null, TAX_CODE, "IPA", null, null);
        assertEquals("admin", data.getUsers().get(0).getProductRole());
    }

    @Test
    void onboardingProduct_childProductWithoutParentOnboardingIsAValidationError() {
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        Product child = product(true);
        child.setId("prod-child");
        child.setParentId("prod-base");
        Product base = product(true);
        base.setId("prod-base");
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(Uni.createFrom().item(child));
        when(productService.getProduct("prod-base", null)).thenReturn(Uni.createFrom().item(base));
        when(productService.isProductEnabled("prod-base")).thenReturn(Uni.createFrom().item(true));
        when(productService.verifyAllowedByInstitutionTaxCode("prod-base", TAX_CODE)).thenReturn(Uni.createFrom().item(false));
        org.mockito.Mockito.doThrow(new ResourceNotFoundException()).when(partyService)
                .verifyOnboarding("prod-base", null, TAX_CODE, "IPA", null, null);

        // when
        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingProduct(data).await().indefinitely());

        // then
        assertEquals("Unable to complete the onboarding for institution with taxCode '" + TAX_CODE
                + "' to product 'prod-child'. Please onboard first the 'prod-base' product for the same institution", e.getMessage());
    }

    @Test
    void onboardingProduct_dismissedParentProductIsRejected() {
        // given
        OnboardingData data = baseData(InstitutionType.PA, "IPA");
        Product child = product(true);
        child.setParentId("prod-base");
        Product base = product(true);
        base.setId("prod-base");
        base.setStatus(ProductStatus.PHASE_OUT);
        when(productService.getProduct(PRODUCT_ID, InstitutionType.PA)).thenReturn(Uni.createFrom().item(child));
        when(productService.getProduct("prod-base", null)).thenReturn(Uni.createFrom().item(base));

        // when
        ValidationException e = assertThrows(ValidationException.class, () -> service.onboardingProduct(data).await().indefinitely());

        // then
        assertEquals("Unable to complete the onboarding for institution with taxCode '" + TAX_CODE
                + "' to product 'prod-base', the base product is dismissed.", e.getMessage());
    }

    @Test
    void getInstitutions_returnsTheUserInstitutions() {
        // given
        Product product = product(true);
        InstitutionInfo info = new InstitutionInfo();
        when(productService.getProduct(PRODUCT_ID, null)).thenReturn(Uni.createFrom().item(product));
        when(partyService.getInstitutionsByUser(product, "uid")).thenReturn(Uni.createFrom().item(List.of(info)));

        // when
        var actualAsync1 = service.getInstitutions(PRODUCT_ID, "uid").await().indefinitely();

        // then
        assertEquals(List.of(info), actualAsync1);
    }

    @Test
    void getInstitutions_unknownProductAndEmptyResultAreNotFound() {
        // given
        when(productService.getProduct("missing", null)).thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("raw downstream")));
        Product product = product(true);
        when(productService.getProduct(PRODUCT_ID, null)).thenReturn(Uni.createFrom().item(product));
        when(partyService.getInstitutionsByUser(product, "uid")).thenReturn(Uni.createFrom().item(List.of()));

        // when
        ResourceNotFoundException unknown = assertThrows(ResourceNotFoundException.class, () -> service.getInstitutions("missing", "uid").await().indefinitely());
        ResourceNotFoundException empty = assertThrows(ResourceNotFoundException.class, () -> service.getInstitutions(PRODUCT_ID, "uid").await().indefinitely());

        // then
        assertEquals("No product found with id missing", unknown.getMessage());
        assertEquals("No institutions found for product " + PRODUCT_ID, empty.getMessage());
    }

    @Test
    void ipaSearchAndLookup_delegateToThePartyRegistryProxy() {
        // given
        IpaInstitutionsSearchResult search = new IpaInstitutionsSearchResult();
        InstitutionProxyInfo found = new InstitutionProxyInfo();
        when(registryProxyService.searchIpaInstitutions("*", "L6", 0, 50)).thenReturn(search);
        when(registryProxyService.findIpaInstitutionByTaxCode(TAX_CODE, "L6")).thenReturn(found);

        // when
        var actualResult1 = service.searchIpaInstitutions("*", "L6", 0, 50).await().indefinitely();
        // then
        assertSame(search, actualResult1);
        var actualResult2 = service.findIpaInstitutionByTaxCode(TAX_CODE, "L6").await().indefinitely();
        assertSame(found, actualResult2);
    }

    @Test
    void getActiveOnboarding_keepsOnlyActiveOnboardingsOfTheProduct() {
        // given
        Institution institution = institution("ext-id");
        institution.setOnboarding(List.of(
                onboarding(PRODUCT_ID, "ACTIVE"), onboarding(PRODUCT_ID, "PENDING"), onboarding("other", "ACTIVE")));
        Institution withoutOnboarding = institution("other-ext");
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, "sub")).thenReturn(List.of(institution, withoutOnboarding));

        // when
        List<Institution> result = service.getActiveOnboarding(TAX_CODE, PRODUCT_ID, "sub").await().indefinitely();

        // then
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getOnboarding().size());
        assertEquals("ACTIVE", result.get(0).getOnboarding().get(0).getStatus());
    }

    @Test
    void getActiveOnboarding_notFoundCases() {
        // given
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode("none", null)).thenReturn(List.of());
        Institution inactive = institution("ext-id");
        inactive.setOnboarding(List.of(onboarding(PRODUCT_ID, "PENDING")));
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode("inactive", null)).thenReturn(List.of(inactive));

        // when
        assertEquals("Institution not found",
        // then
                assertThrows(ResourceNotFoundException.class, () -> service.getActiveOnboarding("none", PRODUCT_ID, null).await().indefinitely()).getMessage());
        assertEquals("Institution doesn't have active onboarding for the given product",
                assertThrows(ResourceNotFoundException.class, () -> service.getActiveOnboarding("inactive", PRODUCT_ID, null).await().indefinitely()).getMessage());
    }

    @Test
    void getInstitutionOnboardingDataById_validatesInputAndMergesBilling() {
        // given
        // when
        assertEquals("An Institution id is required",
        // then
                assertThrows(IllegalArgumentException.class, () -> service.getInstitutionOnboardingDataById(" ", PRODUCT_ID).await().indefinitely()).getMessage());
        assertEquals("A Product Id is required",
                assertThrows(IllegalArgumentException.class, () -> service.getInstitutionOnboardingDataById(INSTITUTION_ID, null).await().indefinitely()).getMessage());

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

        InstitutionOnboardingData result = service.getInstitutionOnboardingDataById(INSTITUTION_ID, PRODUCT_ID).await().indefinitely();

        assertSame(info, result.getInstitution());
        assertEquals("C3", info.getPricingPlan());
        assertSame(billing, info.getBilling());
        assertEquals(1, result.getGeographicTaxonomies().size());
    }

    @Test
    void getInstitutionOnboardingDataById_noOnboardingIsNotFound() {
        // given
        when(partyService.getOnboardings(INSTITUTION_ID, PRODUCT_ID)).thenReturn(List.of());

        // when
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> service.getInstitutionOnboardingDataById(INSTITUTION_ID, PRODUCT_ID).await().indefinitely());

        // then
        assertEquals("Onboarding for institutionId institution-id not found", e.getMessage());
        verify(partyService, never()).getInstitutionById(any(), any());
    }

    @Test
    void getInstitutionOnboardingData_requiresInputs() {
        // given
        // when
        assertEquals("An Institution id is required",
        // then
                assertThrows(IllegalArgumentException.class, () -> service.getInstitutionOnboardingData(null, PRODUCT_ID).await().indefinitely()).getMessage());
        assertEquals("A Product Id is required",
                assertThrows(IllegalArgumentException.class, () -> service.getInstitutionOnboardingData(INSTITUTION_ID, " ").await().indefinitely()).getMessage());
    }

    @Test
    void getInstitutionOnboardingData_missingBillingOrInstitutionIsNotFound() {
        // given
        when(partyService.getInstitutionBillingData("none", PRODUCT_ID)).thenReturn(null);
        when(partyService.getInstitutionBillingData("noinst", PRODUCT_ID)).thenReturn(new InstitutionInfo());
        when(partyService.getInstitutionByExternalId("noinst")).thenReturn(null);

        // when
        assertEquals("Institution none not found",
        // then
                assertThrows(ResourceNotFoundException.class, () -> service.getInstitutionOnboardingData("none", PRODUCT_ID).await().indefinitely()).getMessage());
        assertEquals("Institution noinst not found",
                assertThrows(ResourceNotFoundException.class, () -> service.getInstitutionOnboardingData("noinst", PRODUCT_ID).await().indefinitely()).getMessage());
    }

    @Test
    void getInstitutionOnboardingData_missingTaxonomiesIsAValidationError() {
        // given
        when(partyService.getInstitutionBillingData("inst1", PRODUCT_ID)).thenReturn(new InstitutionInfo());
        Institution institution = institution("inst1");
        institution.setGeographicTaxonomies(null);
        when(partyService.getInstitutionByExternalId("inst1")).thenReturn(institution);

        // when
        ValidationException e = assertThrows(ValidationException.class, () -> service.getInstitutionOnboardingData("inst1", PRODUCT_ID).await().indefinitely());

        // then
        assertEquals("The institution inst1 does not have geographic taxonomies.", e.getMessage());
    }

    @Test
    void getInstitutionOnboardingData_copiesInstitutionLocationAndKeepsExistingCity() {
        // given
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

        // when
        InstitutionOnboardingData result = service.getInstitutionOnboardingData("ext", PRODUCT_ID).await().indefinitely();

        // then
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

        InstitutionOnboardingData result = local.getInstitutionOnboardingData("ext", PRODUCT_ID).await().indefinitely();

        InstitutionLocation location = result.getInstitution().getInstitutionLocation();
        assertEquals("ROMA", location.getCity());
        assertEquals("RM", location.getCounty());
        assertEquals("IT", location.getCountry());
    }

    @Test
    void getInstitutionOnboardingData_ipaLookupNotFoundIsSwallowed() {
        // given
        InstitutionInfo info = new InstitutionInfo();
        info.setTaxCode(TAX_CODE);
        Institution institution = institution("ext");
        institution.setOrigin("IPA");
        institution.setGeographicTaxonomies(List.of());
        when(partyService.getInstitutionBillingData("ext", PRODUCT_ID)).thenReturn(info);
        when(partyService.getInstitutionByExternalId("ext")).thenReturn(institution);
        when(registryProxyService.getInstitutionProxyById(TAX_CODE)).thenThrow(new ResourceNotFoundException());

        // when
        InstitutionOnboardingData result = service.getInstitutionOnboardingData("ext", PRODUCT_ID).await().indefinitely();

        // then
        assertNull(result.getInstitution().getInstitutionLocation().getCity());
    }

    @Test
    void getInstitutionByExternalId_requiresAnId() {
        // given
        Institution institution = institution("ext");
        when(partyService.getInstitutionByExternalId("ext")).thenReturn(institution);

        // when
        var actualResult1 = service.getInstitutionByExternalId("ext").await().indefinitely();
        // then
        assertSame(institution, actualResult1);
        assertEquals("An Institution id is required",
                assertThrows(IllegalArgumentException.class, () -> service.getInstitutionByExternalId("").await().indefinitely()).getMessage());
    }

    @Test
    void getGeographicTaxonomyList_byExternalIdAndByTaxCode() {
        // given
        Institution institution = institution("ext");
        institution.setGeographicTaxonomies(List.of(new GeographicTaxonomy()));
        Institution withoutTaxonomies = institution("ext-2");
        when(partyService.getInstitutionByExternalId("ext")).thenReturn(institution);
        when(partyService.getInstitutionByExternalId("ext-2")).thenReturn(withoutTaxonomies);
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode(TAX_CODE, null)).thenReturn(List.of(institution));
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode("none", null)).thenReturn(List.of());
        when(partyService.getInstitutionsByTaxCodeAndSubunitCode("null", null)).thenReturn(null);

        // when
        var actualResult1 = service.getGeographicTaxonomyList("ext").await().indefinitely();
        // then
        assertEquals(1, actualResult1.size());
        var actualResult2 = service.getGeographicTaxonomyList("ext-2").await().indefinitely();
        assertTrue(actualResult2.isEmpty());
        var actualResult3 = service.getGeographicTaxonomyList(TAX_CODE, null).await().indefinitely();
        assertEquals(1, actualResult3.size());
        var actualResult4 = service.getGeographicTaxonomyList("none", null).await().indefinitely();
        assertTrue(actualResult4.isEmpty());
        var actualResult5 = service.getGeographicTaxonomyList("null", null).await().indefinitely();
        assertTrue(actualResult5.isEmpty());
        assertEquals("A taxCode id is required",
                assertThrows(IllegalArgumentException.class, () -> service.getGeographicTaxonomyList(" ", null).await().indefinitely()).getMessage());
    }

    @Test
    void verifyOnboarding_byExternalIdChecksAllowanceThenParty() {
        // given
        when(productService.isProductEnabled(PRODUCT_ID)).thenReturn(Uni.createFrom().item(true));
        when(productService.verifyAllowedByInstitutionTaxCode(PRODUCT_ID, "ext")).thenReturn(Uni.createFrom().item(false));

        // when
        service.verifyOnboarding("ext", PRODUCT_ID).await().indefinitely();

        // then
        verify(partyService).verifyOnboarding("ext", PRODUCT_ID);
    }

    @Test
    void verifyOnboarding_notAllowedDoesNotCallTheParty() {
        // given
        when(productService.isProductEnabled(PRODUCT_ID)).thenReturn(Uni.createFrom().item(false));
        when(productService.verifyAllowedByInstitutionTaxCode(PRODUCT_ID, "ext")).thenReturn(Uni.createFrom().item(false));

        // when
        OnboardingNotAllowedException e = assertThrows(OnboardingNotAllowedException.class,
                () -> service.verifyOnboarding("ext", PRODUCT_ID).await().indefinitely());

        // then
        assertEquals("Institution with external id 'ext' is not allowed to onboard '" + PRODUCT_ID + "' product", e.getMessage());
        verifyNoInteractions(partyService);
    }

    @Test
    void verifyOnboarding_bySubunitRequiresAnotherParameterAlongWithProductId() {
        // given
        // when
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> service.verifyOnboarding(PRODUCT_ID, null, "", null, "", null).await().indefinitely());

        // then
        assertEquals("At least one other parameter must be provided along with productId", e.getMessage());
        verifyNoInteractions(productService, onboardingService);
    }

    @Test
    void verifyOnboarding_bySubunitDelegatesToOnboardingMs() {
        // given
        when(productService.isProductEnabled(PRODUCT_ID)).thenReturn(Uni.createFrom().item(true));
        when(productService.verifyAllowedByInstitutionTaxCode(PRODUCT_ID, TAX_CODE)).thenReturn(Uni.createFrom().item(false));
        when(onboardingService.verifyOnboarding(PRODUCT_ID, TAX_CODE, "IPA", "origin-id", "sub", "PA"))
                .thenReturn(Uni.createFrom().voidItem());

        // when
        service.verifyOnboarding(PRODUCT_ID, TAX_CODE, "IPA", "origin-id", "sub", "PA").await().indefinitely();

        // then
        verify(onboardingService).verifyOnboarding(PRODUCT_ID, TAX_CODE, "IPA", "origin-id", "sub", "PA");
    }

    @Test
    void checkOrganization_callsTheOrganizationApi() {
        // given
        when(organizationApi.checkOrganization("CF", "VAT")).thenReturn(Uni.createFrom().item(Response.noContent().build()));

        // when
        service.checkOrganization(PRODUCT_ID, "CF", "VAT").await().indefinitely();

        // then
        verify(organizationApi).checkOrganization("CF", "VAT");
    }

    @Test
    void getInstitutionsByUser_removesBusinessesAlreadyOnboardedByTheUser() {
        // given
        InstitutionInfoIC result = new InstitutionInfoIC();
        BusinessInfoIC notOnboarded = business("1");
        BusinessInfoIC onboardedByUser = business("2");
        BusinessInfoIC onboardedByOther = business("3");
        result.setBusinesses(List.of(notOnboarded, onboardedByUser, onboardedByOther));
        when(registryProxyService.getInstitutionsByUserFiscalCode("USER_CF")).thenReturn(result);
        when(onboardingService.verifyOnboarding("prod-pn-pg", "1", null, null, null, null))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException()));
        when(onboardingService.verifyOnboarding("prod-pn-pg", "2", null, null, null, null))
                .thenReturn(Uni.createFrom().voidItem());
        when(onboardingService.verifyOnboarding("prod-pn-pg", "3", null, null, null, null))
                .thenReturn(Uni.createFrom().voidItem());
        UserId userId = new UserId();
        userId.setId(UUID.fromString(USER_UUID));
        when(userRegistryService.searchUser("USER_CF")).thenReturn(userId);
        CheckManagerRequest yes = new CheckManagerRequest();
        yes.setTaxCode("2");
        CheckManagerRequest no = new CheckManagerRequest();
        no.setTaxCode("3");
        when(onboardingMapper.toCheckManagerRequest(USER_UUID, "2", "prod-pn-pg")).thenReturn(yes);
        when(onboardingMapper.toCheckManagerRequest(USER_UUID, "3", "prod-pn-pg")).thenReturn(no);
        when(onboardingService.checkManager(yes)).thenReturn(Uni.createFrom().item(true));
        when(onboardingService.checkManager(no)).thenReturn(Uni.createFrom().item(false));

        // when
        InstitutionInfoIC filtered = service.getInstitutionsByUser("USER_CF").await().indefinitely();

        // then
        assertEquals(List.of(notOnboarded, onboardedByOther), filtered.getBusinesses());
    }

    @Test
    void getByFilters_mapsTheInstitutionsAndEmptyIsNotFound() {
        // given
        OnboardingResponse response = new OnboardingResponse();
        org.openapi.quarkus.onboarding_json.model.InstitutionResponse institutionResponse =
                new org.openapi.quarkus.onboarding_json.model.InstitutionResponse();
        response.setInstitution(institutionResponse);
        Institution mapped = institution("ext");
        when(onboardingService.getByFilters(PRODUCT_ID, TAX_CODE, null, null, null)).thenReturn(Uni.createFrom().item(List.of(response)));
        when(onboardingService.getByFilters("empty", null, null, null, null)).thenReturn(Uni.createFrom().item(List.of()));
        when(onboardingService.getByFilters("null", null, null, null, null)).thenReturn(Uni.createFrom().nullItem());
        when(institutionMapper.toInstitution(institutionResponse)).thenReturn(mapped);

        // when
        var actualResult1 = service.getByFilters(PRODUCT_ID, TAX_CODE, null, null, null).await().indefinitely();
        // then
        assertEquals(List.of(mapped), actualResult1);
        assertThrows(ResourceNotFoundException.class, () -> service.getByFilters("empty", null, null, null, null).await().indefinitely());
        assertThrows(ResourceNotFoundException.class, () -> service.getByFilters("null", null, null, null, null).await().indefinitely());
    }

    @Test
    void matchInstitutionAndUser_usesTheUserTaxCode() {
        // given
        User user = new User();
        user.setTaxCode("USER_CF");
        it.pagopa.selfcare.onboarding.client.model.MatchInfoResult expected = new it.pagopa.selfcare.onboarding.client.model.MatchInfoResult();
        when(registryProxyService.matchInstitutionAndUser("ext", "USER_CF")).thenReturn(expected);

        // when
        var actualResult1 = service.matchInstitutionAndUser("ext", user).await().indefinitely();
        // then
        assertSame(expected, actualResult1);
    }

    @Test
    void validateAggregatesCsv_keepsOnlyErrorsOrOnlyAggregates() {
        // given
        UploadedFile file = new UploadedFile("a.csv", "text/csv", new byte[] {1});
        VerifyAggregateResult withErrors = new VerifyAggregateResult();
        withErrors.setErrors(List.of(new RowError()));
        withErrors.setAggregates(List.of(new AggregateResult()));
        VerifyAggregateResult clean = new VerifyAggregateResult();
        clean.setAggregates(List.of(new AggregateResult()));
        when(onboardingService.aggregatesVerification(file, PRODUCT_ID)).thenReturn(Uni.createFrom().item(withErrors)).thenReturn(Uni.createFrom().item(clean));

        // when
        VerifyAggregateResult first = service.validateAggregatesCsv(file, PRODUCT_ID).await().indefinitely();
        VerifyAggregateResult second = service.validateAggregatesCsv(file, PRODUCT_ID).await().indefinitely();

        // then
        assertEquals(1, first.getErrors().size());
        assertTrue(first.getAggregates().isEmpty());
        assertTrue(second.getErrors().isEmpty());
        assertEquals(1, second.getAggregates().size());
    }

    @Test
    void checkRecipientCode_delegates() {
        // given
        when(onboardingService.checkRecipientCode("origin", "ABCDEF")).thenReturn(Uni.createFrom().item(RecipientCodeStatusResult.ACCEPTED));

        // when
        var actualResult1 = service.checkRecipientCode("origin", "ABCDEF").await().indefinitely();
        // then
        assertEquals(RecipientCodeStatusResult.ACCEPTED, actualResult1);
    }

    @Test
    void onboardingUsersPgFromIcAndAde_delegates() {
        // given
        OnboardingData data = new OnboardingData();

        // when
        service.onboardingUsersPgFromIcAndAde(data).await().indefinitely();

        // then
        verify(onboardingService).onboardingUsersPgFromIcAndAde(data);
    }

    @Test
    void verifyManager_returnsVerifiedAndFailsOtherwiseWithTheLegalRepresentativeMessage() {
        // given
        ManagerVerification verified = new ManagerVerification();
        verified.setVerified(true);
        ManagerVerification notVerified = new ManagerVerification();
        when(pgManagerVerifier.doVerify("OK_CF", "COMPANY")).thenReturn(verified);
        when(pgManagerVerifier.doVerify("KO_CF", "COMPANY")).thenReturn(notVerified);

        // when
        var actualResult1 = service.verifyManager("OK_CF", "COMPANY").await().indefinitely();
        // then
        assertSame(verified, actualResult1);
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, () -> service.verifyManager("KO_CF", "COMPANY").await().indefinitely());

        assertEquals("User with userTaxCode KO_CF is not the legal representative of the institution", e.getMessage());
    }

    @Test
    void getOnboardingWithFilter_delegatesTheEncodedFilters() {
        // given
        OnboardingResult result = new OnboardingResult();
        when(onboardingService.onboardingWithFilter("TAX", "COMPLETED")).thenReturn(Uni.createFrom().item(List.of(result)));

        // when
        var actualResult1 = service.getOnboardingWithFilter("TAX", "COMPLETED").await().indefinitely();
        // then
        assertEquals(List.of(result), actualResult1);
    }

    @Test
    void triggerOnboardingRequest_delegates() {
        // given
        // when
        service.triggerOnboardingRequest("42").await().indefinitely();

        // then
        verify(onboardingService).triggerOnboardingRequest("42");
        verifyNoMoreInteractions(onboardingService);
    }

    @Test
    void validateOnboardingByProductOrInstitutionTaxCode_passesWhenEitherCheckPasses() {
        // given
        when(productService.isProductEnabled("enabled")).thenReturn(Uni.createFrom().item(true));
        when(productService.isProductEnabled("restricted")).thenReturn(Uni.createFrom().item(false));
        when(productService.verifyAllowedByInstitutionTaxCode("enabled", TAX_CODE)).thenReturn(Uni.createFrom().item(false));
        when(productService.verifyAllowedByInstitutionTaxCode("restricted", TAX_CODE)).thenReturn(Uni.createFrom().item(true));
        when(productService.isProductEnabled("blocked")).thenReturn(Uni.createFrom().item(false));
        when(productService.verifyAllowedByInstitutionTaxCode("blocked", TAX_CODE)).thenReturn(Uni.createFrom().item(false));

        // when
        service.validateOnboardingByProductOrInstitutionTaxCode(TAX_CODE, "enabled").await().indefinitely();
        service.validateOnboardingByProductOrInstitutionTaxCode(TAX_CODE, "restricted").await().indefinitely();
        assertThrows(OnboardingNotAllowedException.class,
                () -> service.validateOnboardingByProductOrInstitutionTaxCode(TAX_CODE, "blocked").await().indefinitely());
        // then
        assertFalse(EnumSet.noneOf(RegistryUser.Fields.class).contains(RegistryUser.Fields.name));
    }

    private void stubProductAndGate(Product product, InstitutionType type, boolean enabled, boolean allowed) {
        when(productService.getProduct(PRODUCT_ID, type)).thenReturn(Uni.createFrom().item(product));
        when(productService.isProductEnabled(product.getParentId() == null ? product.getId() : product.getParentId())).thenReturn(Uni.createFrom().item(enabled));
        when(productService.verifyAllowedByInstitutionTaxCode(
                product.getParentId() == null ? product.getId() : product.getParentId(), TAX_CODE)).thenReturn(Uni.createFrom().item(allowed));
    }

    @Test
    @Timeout(value = 2, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void enabledProductStillWaitsForTheSequentialTaxCodeLookup() {
        // given
        AtomicReference<UniEmitter<? super Boolean>> enabled = new AtomicReference<>();
        AtomicReference<UniEmitter<? super Boolean>> allowed = new AtomicReference<>();
        when(productService.isProductEnabled(PRODUCT_ID))
                .thenReturn(Uni.createFrom().<Boolean>emitter(enabled::set));
        when(productService.verifyAllowedByInstitutionTaxCode(PRODUCT_ID, TAX_CODE))
                .thenReturn(Uni.createFrom().<Boolean>emitter(allowed::set));

        // when
        UniAssertSubscriber<Void> result = service.validateOnboardingByProductOrInstitutionTaxCode(TAX_CODE, PRODUCT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verify(productService, never()).verifyAllowedByInstitutionTaxCode(any(), any());
        enabled.get().complete(true);
        result.assertNotTerminated();
        verify(productService).verifyAllowedByInstitutionTaxCode(PRODUCT_ID, TAX_CODE);
        allowed.get().complete(false);
        result.assertCompleted().assertItem(null);
    }

    @Test
    void validationFailureDoesNotStartTheNextLookup() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing product");
        when(productService.isProductEnabled(PRODUCT_ID)).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Void> result = service.validateOnboardingByProductOrInstitutionTaxCode(TAX_CODE, PRODUCT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verify(productService, never()).verifyAllowedByInstitutionTaxCode(any(), any());
    }

    @Test
    void taxCodeFailureIsNotIgnoredForAnEnabledProduct() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing tax-code policy");
        when(productService.isProductEnabled(PRODUCT_ID)).thenReturn(Uni.createFrom().item(true));
        when(productService.verifyAllowedByInstitutionTaxCode(PRODUCT_ID, TAX_CODE))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Void> result = service.validateOnboardingByProductOrInstitutionTaxCode(TAX_CODE, PRODUCT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verifyNoInteractions(partyService, userRegistryService);
    }

    @Test
    void institutionLookupFailureKeepsItsMessageInsteadOfTheProductMessage() {
        // given
        Product product = product(true);
        ResourceNotFoundException failure = new ResourceNotFoundException("party lookup failed");
        when(productService.getProduct(PRODUCT_ID, null)).thenReturn(Uni.createFrom().item(product));
        when(partyService.getInstitutionsByUser(product, "uid")).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<List<InstitutionInfo>> result = service.getInstitutions(PRODUCT_ID, "uid")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }

    @Test
    void infocamereRegistryNotFoundStillKeepsTheBusiness() {
        // given
        InstitutionInfoIC businesses = new InstitutionInfoIC();
        BusinessInfoIC business = business("1");
        businesses.setBusinesses(List.of(business));
        when(registryProxyService.getInstitutionsByUserFiscalCode("USER_CF")).thenReturn(businesses);
        when(onboardingService.verifyOnboarding("prod-pn-pg", "1", null, null, null, null))
                .thenReturn(Uni.createFrom().voidItem());
        when(userRegistryService.searchUser("USER_CF")).thenThrow(new ResourceNotFoundException("missing user"));

        // when
        InstitutionInfoIC result = service.getInstitutionsByUser("USER_CF").await().atMost(Duration.ofSeconds(2));

        // then
        assertEquals(List.of(business), result.getBusinesses());
        verify(onboardingService, never()).checkManager(any());
    }

    @Test
    void infocamereManagerNotFoundStillKeepsTheBusiness() {
        // given
        InstitutionInfoIC businesses = new InstitutionInfoIC();
        BusinessInfoIC business = business("1");
        businesses.setBusinesses(List.of(business));
        when(registryProxyService.getInstitutionsByUserFiscalCode("USER_CF")).thenReturn(businesses);
        when(onboardingService.verifyOnboarding("prod-pn-pg", "1", null, null, null, null))
                .thenReturn(Uni.createFrom().voidItem());
        UserId user = new UserId();
        user.setId(UUID.fromString(USER_UUID));
        when(userRegistryService.searchUser("USER_CF")).thenReturn(user);
        CheckManagerRequest request = new CheckManagerRequest();
        when(onboardingMapper.toCheckManagerRequest(USER_UUID, "1", "prod-pn-pg")).thenReturn(request);
        when(onboardingService.checkManager(request))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("missing manager")));

        // when
        InstitutionInfoIC result = service.getInstitutionsByUser("USER_CF").await().atMost(Duration.ofSeconds(2));

        // then
        assertEquals(List.of(business), result.getBusinesses());
    }

    @Test
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void companyVerificationRunsOnWorkerBeforeStartingTheNativeOnboarding() throws Exception {
        // given
        OnboardingData data = new OnboardingData();
        data.setTaxCode(TAX_CODE);
        data.setOrigin("ADE");
        var match = new it.pagopa.selfcare.onboarding.client.model.MatchInfoResult();
        match.setVerificationResult(true);
        CountDownLatch lookupStarted = new CountDownLatch(1);
        CountDownLatch releaseLookup = new CountDownLatch(1);
        CountDownLatch onboardingStarted = new CountDownLatch(1);
        AtomicReference<Thread> lookupThread = new AtomicReference<>();
        AtomicReference<UniEmitter<? super Void>> pending = new AtomicReference<>();
        when(registryProxyService.matchInstitutionAndUser(TAX_CODE, "USER_CF")).thenAnswer(invocation -> {
            lookupThread.set(Thread.currentThread());
            lookupStarted.countDown();
            if (!releaseLookup.await(2, TimeUnit.SECONDS)) {
                throw new AssertionError("Registry lookup was not released");
            }
            return match;
        });
        when(onboardingService.onboardingCompany(data)).thenReturn(Uni.createFrom().<Void>emitter(emitter -> {
            pending.set(emitter);
            onboardingStarted.countDown();
        }));
        Uni<Void> operation = service.onboardingCompanyV2(data, "USER_CF");
        verifyNoInteractions(registryProxyService, onboardingService);
        Thread subscribingThread = Thread.currentThread();

        // when
        UniAssertSubscriber<Void> result = operation.subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        try {
            assertTrue(lookupStarted.await(2, TimeUnit.SECONDS));
            assertNotSame(subscribingThread, lookupThread.get());
            result.assertNotTerminated();
            verifyNoInteractions(onboardingService);
        } finally {
            releaseLookup.countDown();
        }
        assertTrue(onboardingStarted.await(2, TimeUnit.SECONDS));
        result.assertNotTerminated();
        pending.get().complete(null);
        result.awaitItem(Duration.ofSeconds(2)).assertCompleted().assertItem(null);
        verify(onboardingService).onboardingCompany(data);
    }

    @Test
    void companyRegistryFailureIsNotReplacedByTheBusinessOwnershipError() {
        // given
        OnboardingData data = new OnboardingData();
        data.setTaxCode(TAX_CODE);
        data.setOrigin("ADE");
        ResourceNotFoundException failure = new ResourceNotFoundException("registry unavailable");
        when(registryProxyService.matchInstitutionAndUser(TAX_CODE, "USER_CF")).thenThrow(failure);

        // when
        UniAssertSubscriber<Void> result = service.onboardingCompanyV2(data, "USER_CF")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.awaitFailure(Duration.ofSeconds(2));
        assertSame(failure, result.getFailure());
        verifyNoInteractions(onboardingService);
    }

    @Test
    void billingInstitutionAndLocationLookupsStaySequentialOnTheWorker() {
        // given
        InstitutionInfo info = new InstitutionInfo();
        Institution institution = institution("ext");
        institution.setTaxCode(TAX_CODE);
        institution.setOrigin("IPA");
        institution.setSubunitType("UO");
        institution.setSubunitCode("UO1");
        institution.setGeographicTaxonomies(List.of());
        UoResponse uo = new UoResponse();
        uo.setMunicipalIstatCode("istat");
        GeographicTaxonomiesResponse geography = new GeographicTaxonomiesResponse();
        geography.setDescription("ROMA - COMUNE");
        AtomicReference<Thread> billingThread = new AtomicReference<>();
        AtomicReference<Thread> institutionThread = new AtomicReference<>();
        AtomicReference<Thread> uoThread = new AtomicReference<>();
        AtomicReference<Thread> geographyThread = new AtomicReference<>();
        when(partyService.getInstitutionBillingData("ext", PRODUCT_ID)).thenAnswer(invocation -> {
            billingThread.set(Thread.currentThread());
            return info;
        });
        when(partyService.getInstitutionByExternalId("ext")).thenAnswer(invocation -> {
            institutionThread.set(Thread.currentThread());
            return institution;
        });
        when(registryProxyService.getUoById("UO1")).thenAnswer(invocation -> {
            uoThread.set(Thread.currentThread());
            return uo;
        });
        when(registryProxyService.getExtById("istat")).thenAnswer(invocation -> {
            geographyThread.set(Thread.currentThread());
            return geography;
        });
        Uni<InstitutionOnboardingData> operation = service.getInstitutionOnboardingData("ext", PRODUCT_ID);
        verifyNoInteractions(partyService, registryProxyService);
        Thread subscribingThread = Thread.currentThread();

        // when
        InstitutionOnboardingData result = operation.await().atMost(Duration.ofSeconds(2));

        // then
        assertNotSame(subscribingThread, billingThread.get());
        assertSame(billingThread.get(), institutionThread.get());
        assertSame(billingThread.get(), uoThread.get());
        assertSame(billingThread.get(), geographyThread.get());
        assertEquals("ROMA", result.getInstitution().getInstitutionLocation().getCity());
        var calls = org.mockito.Mockito.inOrder(partyService, registryProxyService);
        calls.verify(partyService).getInstitutionBillingData("ext", PRODUCT_ID);
        calls.verify(partyService).getInstitutionByExternalId("ext");
        calls.verify(registryProxyService).getUoById("UO1");
        calls.verify(registryProxyService).getExtById("istat");
        calls.verifyNoMoreInteractions();
    }

    @Test
    void locationLookupOnlyRecoversNotFoundAndPropagatesOtherFailures() {
        // given
        InstitutionInfo info = new InstitutionInfo();
        info.setTaxCode(TAX_CODE);
        Institution institution = institution("ext");
        institution.setOrigin("IPA");
        institution.setGeographicTaxonomies(List.of());
        InvalidRequestException failure = new InvalidRequestException("invalid registry response");
        when(partyService.getInstitutionBillingData("ext", PRODUCT_ID)).thenReturn(info);
        when(partyService.getInstitutionByExternalId("ext")).thenReturn(institution);
        when(registryProxyService.getInstitutionProxyById(TAX_CODE)).thenThrow(failure);

        // when
        UniAssertSubscriber<InstitutionOnboardingData> result = service.getInstitutionOnboardingData("ext", PRODUCT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.awaitFailure(Duration.ofSeconds(2));
        assertSame(failure, result.getFailure());
        verify(registryProxyService, never()).getExtById(any());
    }

    @Test
    void filteredInstitutionsAreNotMappedBeforeTheNativeLookupEmits() {
        // given
        AtomicReference<UniEmitter<? super List<OnboardingResponse>>> pending = new AtomicReference<>();
        when(onboardingService.getByFilters(PRODUCT_ID, TAX_CODE, null, null, null))
                .thenReturn(Uni.createFrom().<List<OnboardingResponse>>emitter(pending::set));
        OnboardingResponse response = new OnboardingResponse();
        var institutionResponse = new org.openapi.quarkus.onboarding_json.model.InstitutionResponse();
        response.setInstitution(institutionResponse);
        Institution institution = institution("ext");
        when(institutionMapper.toInstitution(institutionResponse)).thenReturn(institution);

        // when
        UniAssertSubscriber<List<Institution>> result = service.getByFilters(PRODUCT_ID, TAX_CODE, null, null, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verify(institutionMapper, never()).toInstitution(any());
        pending.get().complete(List.of(response));
        result.assertCompleted().assertItem(List.of(institution));
    }

    @Test
    void aggregatesAreNormalizedOnlyAfterTheNativeResultEmits() {
        // given
        UploadedFile file = new UploadedFile("aggregates.csv", "text/csv", new byte[]{1});
        VerifyAggregateResult response = new VerifyAggregateResult();
        response.setErrors(List.of(new RowError()));
        response.setAggregates(List.of(new AggregateResult()));
        AtomicReference<UniEmitter<? super VerifyAggregateResult>> pending = new AtomicReference<>();
        when(onboardingService.aggregatesVerification(file, PRODUCT_ID))
                .thenReturn(Uni.createFrom().<VerifyAggregateResult>emitter(pending::set));

        // when
        UniAssertSubscriber<VerifyAggregateResult> result = service.validateAggregatesCsv(file, PRODUCT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        assertEquals(1, response.getAggregates().size());
        pending.get().complete(response);
        result.assertCompleted().assertItem(response);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getAggregates().isEmpty());
    }

    @Test
    void managerLookupFailureKeepsTheDownstreamFailure() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("registry lookup failed");
        when(pgManagerVerifier.doVerify("USER_CF", TAX_CODE)).thenThrow(failure);

        // when
        UniAssertSubscriber<ManagerVerification> result = service.verifyManager("USER_CF", TAX_CODE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.awaitFailure(Duration.ofSeconds(2));
        assertSame(failure, result.getFailure());
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
