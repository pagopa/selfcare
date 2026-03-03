package it.pagopa.selfcare.onboarding.service;

import static it.pagopa.selfcare.onboarding.common.InstitutionType.PA;
import static it.pagopa.selfcare.onboarding.common.InstitutionType.PSP;
import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static it.pagopa.selfcare.onboarding.common.WorkflowType.*;
import static it.pagopa.selfcare.onboarding.service.impl.OnboardingServiceDefault.USERS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.service.impl.OnboardingServiceDefault.USERS_FIELD_TAXCODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.openapi.quarkus.core_json.model.InstitutionProduct.StateEnum.PENDING;

import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.*;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapperImpl;
import it.pagopa.selfcare.onboarding.mapper.TokenMapper;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.service.impl.OnboardingServiceDefault;
import it.pagopa.selfcare.onboarding.service.profile.OnboardingTestProfile;
import it.pagopa.selfcare.onboarding.service.strategy.OnboardingValidationStrategy;
import it.pagopa.selfcare.onboarding.service.util.OnboardingUtils;
import it.pagopa.selfcare.product.entity.*;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.http.HttpStatus;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.openapi.quarkus.core_json.api.OnboardingApi;
import org.openapi.quarkus.core_json.model.InstitutionsResponse;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.*;
import org.openapi.quarkus.party_registry_proxy_json.model.*;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class OnboardingServiceDefaultTest {

    @Inject
    OnboardingServiceDefault onboardingService;

    @InjectMock
    @RestClient
    UserApi userRegistryApi;

    @InjectMock
    @RestClient
    InsuranceCompaniesApi insuranceCompaniesApi;

    @InjectMock
    ProductService productService;

    @InjectMock
    @RestClient
    AooApi aooApi;

    @InjectMock
    @RestClient
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;

    @InjectMock
    @RestClient
    UoApi uoApi;

    @InjectMock
    @RestClient
    InfocamerePdndApi infocamerePdndApi;

    @InjectMock
    @RestClient
    PdndVisuraInfoCamereControllerApi pdndVisuraInfoCamereControllerApi;

    @RestClient
    @InjectMock
    InfocamereApi infocamereApi;

    @RestClient
    @InjectMock
    NationalRegistriesApi nationalRegistriesApi;

    @InjectMock
    AzureBlobClient azureBlobClient;

    @InjectMock
    SignatureService signatureService;

    @InjectMock
    TokenService tokenService;

    @InjectMock
    @RestClient
    OnboardingApi onboardingApi;

    @InjectMock
    OrchestrationService orchestrationService;

    @InjectMock
    UserService userInstitutionApi;

    @InjectMock
    InstitutionService institutionService;

    @InjectMock
    @RestClient
    GeographicTaxonomiesApi geographicTaxonomiesApi;

    @InjectMock
    OnboardingValidationStrategy onboardingValidationStrategy;

    @Spy
    OnboardingMapper onboardingMapper = new OnboardingMapperImpl();

    @InjectMock
    TokenMapper tokenMapper;

    static final UserRequest manager = UserRequest.builder()
            .name("name")
            .surname("surname")
            .taxCode("taxCode")
            .role(PartyRole.MANAGER)
            .email("email_manager")
            .build();

    static final UserRequest delegate1 = UserRequest.builder()
            .name("name_delegate_1")
            .surname("surname_delegate_2")
            .taxCode("taxCode_delegate_3")
            .role(PartyRole.DELEGATE)
            .build();

    static final UserRequest delegate2 = UserRequest.builder()
            .name("name_delegate_2")
            .surname("surname_delegate_2")
            .taxCode("taxCode_delegate_2")
            .role(PartyRole.DELEGATE)
            .build();

    static final UserRequest delegate3 = UserRequest.builder()
            .name("name_delegate_3")
            .surname("surname_delegate_3")
            .taxCode("taxCode_delegate_3")
            .role(PartyRole.DELEGATE)
            .build();


    static final UserResource managerResource;
    static final UserResource managerResourceWk;
    static final UserResource managerResourceWkSpid;

    static final String PRODUCT_ROLE_ADMIN_CODE = "admin";
    static final String PRODUCT_ROLE_ADMIN_PSP_CODE = "admin-psp";
    static final String DIGITAL_ADDRESS_FIELD = "digitalAddress";
    static final String DESCRIPTION_FIELD = "description";
    static final File testFile = new File("src/test/resources/application.properties");

    static final FormItem TEST_FORM_ITEM = FormItem.builder()
            .fileName("testFile")
            .file(testFile)
            .build();

    static {
        managerResource = new UserResource();
        managerResource.setId(UUID.randomUUID());
        managerResource.setName(new CertifiableFieldResourceOfstring()
                .value(manager.getName())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        managerResource.setFamilyName(new CertifiableFieldResourceOfstring()
                .value(manager.getSurname())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));

        managerResourceWk = new UserResource();
        managerResourceWk.setId(UUID.randomUUID());
        managerResourceWk.setName(new CertifiableFieldResourceOfstring()
                .value(manager.getName())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        managerResourceWk.setFamilyName(new CertifiableFieldResourceOfstring()
                .value(manager.getSurname())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));

        Map<String, WorkContactResource> map = new HashMap<>();
        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.setEmail(new CertifiableFieldResourceOfstring()
                .value("mail@live.it")
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        map.put(UUID.randomUUID().toString(), workContactResource);
        managerResourceWk.setWorkContacts(map);

        managerResourceWkSpid = new UserResource();
        managerResourceWkSpid.setId(UUID.randomUUID());
        managerResourceWkSpid.setName(new CertifiableFieldResourceOfstring()
                .value(manager.getName())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.SPID));
        managerResourceWkSpid.setFamilyName(new CertifiableFieldResourceOfstring()
                .value(manager.getSurname())
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.SPID));
        managerResourceWkSpid.setWorkContacts(map);
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_throwExceptionIfProductThrowException(UniAsserter asserter) {
        Onboarding onboardingRequest = createDummyOnboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");

        Product productResource = new Product();
        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource) // Prima chiamata: ritorna un valore valido
                .thenThrow(new IllegalArgumentException()) // Seconda chiamata: lancia un'eccezione
        );

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null, null), OnboardingNotAllowedException.class);
    }


    @Test
    @RunOnVertxContext
    void onboarding_throwExceptionIfProductIsNotValid(UniAsserter asserter) {
        Onboarding onboardingRequest = createDummyOnboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");

        Product productResource = new Product();
        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource) // Prima chiamata: ritorna un valore valido
                .thenThrow(new ProductNotFoundException()) // Seconda chiamata: lancia un'eccezione
        );

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null, null), OnboardingNotAllowedException.class);
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_throwExceptionIfProductAlreadyOnboarded(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        onboardingRequest.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter, false, true);
        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);
        mockVerifyOnboardingNotEmpty(asserter);

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null, null), ResourceConflictException.class);
    }

    @Test
    @RunOnVertxContext
    void onboardingIncrement_Ok(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding onboardingRequest = new Onboarding();
        onboardingRequest.setIsAggregator(true);
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setOriginId("originId");
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboardingRequest.setInstitution(institutionBaseRequest);
        Billing billing = new Billing();
        billing.setRecipientCode("recCode");
        onboardingRequest.setBilling(billing);
        onboardingRequest.setUserRequester(userRequester);

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setDescription("test");
        onboardingRequest.setAggregates(List.of(aggregateInstitution));

        AggregateInstitutionRequest aggregateInstitutionRequest = new AggregateInstitutionRequest();
        aggregateInstitutionRequest.setDescription("test");
        aggregateInstitutionRequest.setTaxCode("taxCode");

        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter, false, true);
        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);
        mockVerifyOnboardingNotEmpty(asserter);

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));


        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleSfe("codSfe");
        uoResource.setCodiceIpa("originId");
        when(uoApi.findByUnicodeUsingGET1("recCode", null)).thenReturn(Uni.createFrom().item(uoResource));

        managerResource.setId(UUID.fromString("9456d91f-ef53-4f89-8330-7f9a195d5d1e"));

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
            when(userRegistryApi.searchUsingPOST(eq(USERS_FIELD_LIST), any()))
                    .thenReturn(Uni.createFrom().item(managerResource));
        });
        OnboardingResponse onboardingResponse = getOnboardingResponse();

        Uni<OnboardingResponse> response = onboardingService.onboardingIncrement(onboardingRequest, users, List.of(aggregateInstitutionRequest), userRequesterDto);

        // Confronta con AssertJ ignorando `createdAt`
        asserter.execute(() -> response.subscribe().with(actualResponse -> assertThat(actualResponse)
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(onboardingResponse)));
    }

    private static OnboardingResponse getOnboardingResponse() {
        OnboardingResponse onboardingResponse = new OnboardingResponse();
        onboardingResponse.setWorkflowType(INCREMENT_REGISTRATION_AGGREGATOR.toString());
        onboardingResponse.setProductId("productId");
        it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse institution = new it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse();
        institution.setInstitutionType("PA");
        institution.setOriginId("originId");
        institution.setTaxCode("taxCode");
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setOrigin(Origin.IPA);
        onboardingResponse.setInstitution(institution);

        UserOnboardingResponse userOnboardingResponse = new UserOnboardingResponse();
        userOnboardingResponse.setId("9456d91f-ef53-4f89-8330-7f9a195d5d1e");
        userOnboardingResponse.setRole(PartyRole.MANAGER);
        userOnboardingResponse.setProductRole("admin");
        onboardingResponse.setUsers(List.of(userOnboardingResponse));

        BillingResponse billingResponse = new BillingResponse();
        billingResponse.setRecipientCode("recCode");
        onboardingResponse.setBilling(billingResponse);

        onboardingResponse.setStatus(PENDING.toString());
        onboardingResponse.setIsAggregator(true);
        return onboardingResponse;
    }


    @Test
    @RunOnVertxContext
    void onboardingIncrement_throwExceptionIfProductNotOnboarded(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        onboardingRequest.setInstitution(institutionBaseRequest);
        onboardingRequest.setUserRequester(userRequester);

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setDescription("test");
        onboardingRequest.setAggregates(List.of(aggregateInstitution));

        AggregateInstitutionRequest aggregateInstitutionRequest = new AggregateInstitutionRequest();
        aggregateInstitutionRequest.setDescription("test");
        aggregateInstitutionRequest.setTaxCode("taxCode");

        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter, false, true);
        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);
        mockVerifyOnboardingNotFound();

        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleSfe("codSfe");
        uoResource.setCodiceIpa("originId");
        when(uoApi.findByUnicodeUsingGET1(any(), any())).thenReturn(Uni.createFrom().item(uoResource));

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);

        Onboarding onboarding1 = createDummyOnboarding();

        Mockito.doAnswer(invocation -> Multi.createFrom().items(onboarding1))
                .when(query)
                .stream();

        when(Onboarding.find(any())).thenReturn(query);
        when(Onboarding.find(any(Document.class), any(Document.class))).thenReturn(query);

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));
        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        asserter.assertFailedWith(() -> onboardingService.onboardingIncrement(onboardingRequest,
                users, List.of(aggregateInstitutionRequest), userRequesterDto), InvalidRequestException.class);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class), any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_throwExceptionIfProductIsNotDelegable(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PT);
        onboardingRequest.setInstitution(institutionBaseRequest);

        Product productResource = new Product();
        productResource.setDelegable(Boolean.FALSE);
        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource));

        mockVerifyOnboardingNotFound();

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null, null), OnboardingNotAllowedException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_throwExceptionIfProductRoleIsNotValid(UniAsserter asserter) {
        Onboarding onboardingRequest = createDummyOnboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("prod-pn");
        onboardingRequest.getInstitution().setInstitutionType(InstitutionType.PG);

        Product productResource = new Product();
        productResource.setRoleMappings(new HashMap<>());
        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource));

        mockVerifyOnboardingNotFound();
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null, null), OnboardingNotAllowedException.class);
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_throwExceptionIfProductParentRoleIsNotValid(UniAsserter asserter) {
        Onboarding onboardingRequest = createDummyOnboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");

        Product productResource = new Product();
        Product productParent = new Product();
        productParent.setRoleMappings(new HashMap<>());
        productResource.setParent(productParent);

        asserter.execute(() -> when(productService.getProductIsValid(onboardingRequest.getProductId()))
                .thenReturn(productResource));

        mockVerifyOnboardingNotFound();
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null, null), OnboardingNotAllowedException.class);
    }


    @Test
    @RunOnVertxContext
    void onboarding_shouldThrowExceptionIfRoleNotValid(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding onboardingDefaultRequest = new Onboarding();
        Institution institution = dummyInstitution();
        institution.setOrigin(Origin.IVASS);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboardingDefaultRequest.setInstitution(institution);
        onboardingDefaultRequest.setProductId(PROD_INTEROP.getValue());
        onboardingDefaultRequest.setUserRequester(userRequester);

        List<UserRequest> users = List.of(UserRequest.builder()
                .taxCode("taxCode")
                .role(PartyRole.OPERATOR)
                .build());

        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockPersistOnboarding(asserter);
        mockVerifyAllowedProductList(onboardingDefaultRequest.getProductId(), asserter, true);

        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));
        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingDefaultRequest, users, null, userRequesterDto),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_Aoo(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.AOO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setDescription("TEST");
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);

        mockPersistOnboarding(asserter);
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();

        AOOResource aooResource = new AOOResource();
        aooResource.setDenominazioneEnte("TEST");
        aooResource.setDenominazioneAoo("TEST");
        aooResource.setMail1(DIGITAL_ADDRESS_FIELD);
        asserter.execute(() -> when(aooApi.findByUnicodeUsingGET(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(aooResource)));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_AooNotFound(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.AOO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        AOOResource aooResource = new AOOResource();
        aooResource.setDenominazioneEnte("TEST");
        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(exception.getResponse()).thenReturn(response);
        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(uoResource)));
        asserter.execute(() -> when(aooApi.findByUnicodeUsingGET(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(exception)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null, null), ResourceNotFoundException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_AooException(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.AOO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        AOOResource aooResource = new AOOResource();
        aooResource.setDenominazioneEnte("TEST");
        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(exception.getResponse()).thenReturn(response);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(uoResource)));

        asserter.execute(() -> when(aooApi.findByUnicodeUsingGET(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(exception)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null, null), WebApplicationException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_Uo(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.UO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setDescription("TEST");
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        uoResource.setMail1(DIGITAL_ADDRESS_FIELD);
        uoResource.setDescrizioneUo("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().item(uoResource)));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_UoNotFound(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.UO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        institutionBaseRequest.setDigitalAddress("mail@pec.it");
        request.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException("Resource not found");

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        uoResource.setCodiceFiscaleEnte("taxCode");
        uoResource.setMail1("mail@pec.it");
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(new InstitutionResource()));
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(resourceNotFoundException)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null, null), ResourceNotFoundException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_addParentDescriptionForAooOrUo_UoException(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setSubunitType(InstitutionPaSubunitType.UO);
        institutionBaseRequest.setSubunitCode("SubunitCode");
        request.setInstitution(institutionBaseRequest);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(exception.getResponse()).thenReturn(response);

        UOResource uoResource = new UOResource();
        uoResource.setDenominazioneEnte("TEST");
        asserter.execute(() -> when(uoApi.findByUnicodeUsingGET1(institutionBaseRequest.getSubunitCode(), null))
                .thenReturn(Uni.createFrom().failure(exception)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null, null), WebApplicationException.class);
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_allowedPricingPlan(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_IO_PREMIUM.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setDescription("TEST");
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setOrigin(Origin.IPA);
        request.setInstitution(institutionBaseRequest);
        request.setPricingPlan("C0");
        request.setUserRequester(userRequester);
        Billing billing = new Billing();
        billing.setRecipientCode("recCode");
        request.setBilling(billing);

        mockPersistOnboarding(asserter);
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();

        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleSfe("codSfe");
        uoResource.setCodiceIpa("originId");
        when(uoApi.findByUnicodeUsingGET1("recCode", null)).thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDescription("TEST");
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingPa_notAllowedPricingPlan(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_IO_PREMIUM.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setDescription("TEST");
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setOrigin(Origin.IPA);
        request.setInstitution(institutionBaseRequest);
        request.setPricingPlan("C1");
        request.setUserRequester(userRequester);
        Billing billing = new Billing();
        billing.setRecipientCode("recCode");
        request.setBilling(billing);

        mockPersistOnboarding(asserter);
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();

        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleSfe("codSfe");
        uoResource.setCodiceIpa("originId");
        when(uoApi.findByUnicodeUsingGET1("recCode", null)).thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDescription("TEST");
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null, userRequesterDto), InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_notAllowedInstitutionType(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_DASHBOARD_PSP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setDescription("TEST");
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setOrigin(Origin.IPA);
        request.setInstitution(institutionBaseRequest);
        request.setPricingPlan("C1");
        Billing billing = new Billing();
        billing.setRecipientCode("recCode");
        request.setBilling(billing);

        mockPersistOnboarding(asserter);
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();

        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleSfe("codSfe");
        uoResource.setCodiceIpa("originId");
        when(uoApi.findByUnicodeUsingGET1("recCode", null)).thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDescription("TEST");
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null, null), InvalidRequestException.class);
    }

    void mockSimpleSearchPOSTAndPersist(UniAsserter asserter) {

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(managerResource)));

        asserter.execute(() -> when(Onboarding.persistOrUpdate(any(List.class)))
                .thenAnswer(arg -> {
                    List<Onboarding> onboardings = (List<Onboarding>) arg.getArguments()[0];
                    onboardings.get(0).setId(UUID.randomUUID().toString());
                    return Uni.createFrom().nullItem();
                }));

        asserter.execute(() -> when(orchestrationService.triggerOrchestration(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));
    }

    @Test
    @RunOnVertxContext
    void onboarding_PRV_PagoPA(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_PAGOPA.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setAtecoCodes(List.of("01.11.00"));
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");
        pdndBusinessResource.setAtecoCodes(List.of("01.11.00"));

        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));
        when(pdndVisuraInfoCamereControllerApi.institutionVisuraPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_PRV(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setAtecoCodes(List.of("01.11.00"));
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");
        pdndBusinessResource.setAtecoCodes(List.of("01.11.00"));

        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));
        when(pdndVisuraInfoCamereControllerApi.institutionVisuraPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        mockAllowedProductByInstitutionTaxCodeList(asserter, false);

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_PRV_soleTrader(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_IDPAY_MERCHANT.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setAtecoCodes(List.of("01.11.00"));
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");
        pdndBusinessResource.setAtecoCodes(List.of("01.11.00"));

        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));
        when(pdndVisuraInfoCamereControllerApi.institutionVisuraPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_SELC_WorkflowType_CONFIRMATION(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_PAGOPA.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.GSP);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });


        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        // onboardingCompletion will set the workflowType to CONFIRMATION, which is allowed for GSP
        asserter.assertThat(() -> onboardingService.onboardingCompletion(request, users, userRequesterDto), Assertions::assertNotNull);
    }

    /*@Test
    @RunOnVertxContext
    void onboarding_PRV_Bad_Request(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDescription("nome");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");

        asserter.execute(() -> when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(pdndBusinessResource)));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null), InvalidRequestException.class);

    }*/

    /*@Test
    @RunOnVertxContext
    void onboarding_PRV_Bad_Request_Pec(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDigitalAddress("wrong-pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");

        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null), InvalidRequestException.class);

    }*/

    @Test
    @RunOnVertxContext
    void onboarding_PRV_Institution_Not_Found(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("wrong-pec");
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        List<AggregateInstitution> aggregates = new ArrayList<>();
        AggregateInstitution institution = new AggregateInstitution();
        aggregates.add(institution);
        request.setAggregates(aggregates);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        ClientWebApplicationException exception = new ClientWebApplicationException(HttpStatus.SC_NOT_FOUND);
        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().failure(exception));

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null, null), ResourceNotFoundException.class);

    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_Aggregator(UniAsserter asserter) {
        UserRequest managerUser = UserRequest.builder()
                .name("name")
                .taxCode("taxCode")
                .role(PartyRole.MANAGER)
                .build();

        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        request.setIsAggregator(Boolean.TRUE);
        List<UserRequest> users = List.of(managerUser);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        request.setBilling(billing);
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);
        List<AggregateInstitution> aggregates = new ArrayList<>();
        AggregateInstitution institution = new AggregateInstitution();
        aggregates.add(institution);
        request.setAggregates(aggregates);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_Aggregator_WithUsers(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        UserRequest managerUser = UserRequest.builder()
                .name("name")
                .taxCode("taxCode")
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = new Onboarding();
        request.setIsAggregator(Boolean.TRUE);
        request.setProductId(PROD_INTEROP.getValue());

        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(InstitutionType.PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        request.setBilling(billing);
        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setTaxCode("taxCode");

        AggregateInstitutionRequest aggregateInstitutionRequest = new AggregateInstitutionRequest();
        aggregateInstitutionRequest.setTaxCode("taxCode");
        aggregateInstitutionRequest.setUsers(List.of(managerUser));

        List<AggregateInstitution> aggregates = new ArrayList<>();
        aggregates.add(aggregateInstitution);
        request.setAggregates(aggregates);

        List<UserRequest> users = List.of(managerUser);

        mockPersistOnboarding(asserter);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, List.of(aggregateInstitutionRequest), userRequesterDto), Assertions::assertNotNull);

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingSa_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IVASS);
        institutionBaseRequest.setInstitutionType(InstitutionType.SA);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        onboardingRequest.setInstitution(institutionBaseRequest);
        onboardingRequest.setUserRequester(userRequester);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);

        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(managerResourceWk));

        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    Product mockSimpleProductValidAssert(String productId, boolean hasParent, UniAsserter asserter, boolean allowIndividualOnboarding, boolean allowCompanyOnboarding) {
        Product productResource = createDummyProduct(productId, hasParent, allowIndividualOnboarding, allowCompanyOnboarding);
        asserter.execute(() -> when(productService.getProductIsValid(productId))
                .thenReturn(productResource));
        return productResource;
    }

    ProductRoleInfo dummyProductRoleInfo(String productRolCode) {
        ProductRole productRole = new ProductRole();
        productRole.setCode(productRolCode);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setRoles(List.of(productRole));
        productRoleInfo.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.ONBOARDING.value));
        return productRoleInfo;
    }

    Product createDummyProduct(String productId, boolean hasParent,
                               boolean allowIndividualOnboarding,
                               boolean allowCompanyOnboarding) {
        Map<PartyRole, ProductRoleInfo> roleMappingByInstitutionType = new HashMap<>();
        roleMappingByInstitutionType.put(manager.getRole(), dummyProductRoleInfo(PRODUCT_ROLE_ADMIN_PSP_CODE));

        Product productResource = new Product();
        productResource.setId(productId);
        Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
        roleMappings.put(manager.getRole(), dummyProductRoleInfo(PRODUCT_ROLE_ADMIN_CODE));
        roleMappings.put(delegate1.getRole(), dummyProductRoleInfo(PRODUCT_ROLE_ADMIN_CODE));
        productResource.setRoleMappings(roleMappings);
        productResource.setRoleMappingsByInstitutionType(Map.of(PSP.name(), roleMappingByInstitutionType));
        productResource.setTitle("title");
        productResource.setAllowIndividualOnboarding(allowIndividualOnboarding);
        productResource.setAllowCompanyOnboarding(allowCompanyOnboarding);

        if (PROD_DASHBOARD_PSP.getValue().equals(productId)) {
            List<String> institutionTypeList = new ArrayList<>();
            institutionTypeList.add(PSP.name());
            productResource.setInstitutionTypesAllowed(institutionTypeList);
        }

        if (hasParent) {
            Product parent = new Product();
            parent.setId("productParentId");
            parent.setRoleMappings(Map.of(manager.getRole(), dummyProductRoleInfo(PRODUCT_ROLE_ADMIN_CODE)));
            productResource.setParentId(parent.getId());
            productResource.setParent(parent);
        }

        return productResource;
    }


    @Test
    @RunOnVertxContext
    void onboardingPsp_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding onboardingRequest = createDummyOnboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        onboardingRequest.getInstitution().setOrigin(Origin.SELC);
        onboardingRequest.getInstitution().setInstitutionType(PSP);
        onboardingRequest.setUserRequester(userRequester);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);

        asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(any(), any()))
                        .thenReturn(Uni.createFrom().item(managerResourceWk)));

        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingPsp_whenUserFoundedAndWillNotUpdateAndProductHasParent(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setOrigin(Origin.SELC);
        institutionPspRequest.setInstitutionType(PSP);
        institutionPspRequest.setTaxCode("taxCode");
        onboardingRequest.setInstitution(institutionPspRequest);
        onboardingRequest.setUserRequester(userRequester);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), true, asserter, false, true);

        // mock verify allowed Map
        asserter.execute(() -> when(onboardingValidationStrategy.validate(any()))
                .thenReturn(true));

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);

        Onboarding onboarding1 = new Onboarding();
        Onboarding onboarding2 = new Onboarding();
        onboarding2.setInstitution(institutionPspRequest);

        Mockito.doAnswer(invocation -> Multi.createFrom().empty())
                .doAnswer(invocation -> Multi.createFrom().items(onboarding1))
                .doAnswer(invocation -> Multi.createFrom().items(onboarding2))
                .when(query)
                .stream();

        when(Onboarding.find(any())).thenReturn(query);
        when(Onboarding.find(any(Document.class), any(Document.class))).thenReturn(query);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        asserter.assertThat(
                () -> onboardingService.onboarding(onboardingRequest, users, null, userRequesterDto),
                Assertions::assertNotNull);

        asserter.execute(
                () -> {
                    PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
                    PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
                    PanacheMock.verify(Onboarding.class, times(2)).find(any(Document.class));
                    PanacheMock.verify(Onboarding.class, times(1)).find(any(Document.class), any(Document.class));
                    PanacheMock.verifyNoMoreInteractions(Onboarding.class);
                });
    }

    @Test
    @RunOnVertxContext
    void onboardingPsp_ProductHasParentNotOnboarded(UniAsserter asserter) {
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setOrigin(Origin.SELC);
        institutionPspRequest.setInstitutionType(PSP);
        institutionPspRequest.setTaxCode("taxCode");
        onboardingRequest.setInstitution(institutionPspRequest);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingRequest.getProductId(), true, asserter, false, true);

        // mock verify allowed Map
        asserter.execute(() -> when(onboardingValidationStrategy.validate(any()))
                .thenReturn(true));

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);

        Mockito.doAnswer(invocation -> Multi.createFrom().empty())
                .doAnswer(invocation -> Multi.createFrom().empty())
                .when(query).stream();

        when(Onboarding.find(any())).thenReturn(query);

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null, null), InvalidRequestException.class);

    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundedAndWillNotUpdate(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding onboardingDefaultRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingDefaultRequest.setProductId("productId");
        Institution institution = dummyInstitution();
        institution.setOrigin(Origin.IVASS);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboardingDefaultRequest.setInstitution(institution);
        onboardingDefaultRequest.setUserRequester(userRequester);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboardingDefaultRequest.getProductId(), asserter, true);
        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));
        asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(managerResourceWk)));

        asserter.assertThat(() -> onboardingService.onboarding(onboardingDefaultRequest, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundedAndWillUpdate(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        request.setBilling(billing);
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionPspRequest = new Institution();
        institutionPspRequest.setDescription(DESCRIPTION_FIELD);
        institutionPspRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionPspRequest.setOrigin(Origin.IPA);
        institutionPspRequest.setInstitutionType(InstitutionType.GSP);
        request.setInstitution(institutionPspRequest);
        AdditionalInformations adds = new AdditionalInformations();
        adds.setIpa(false);
        adds.setOtherNote("other");
        request.setAdditionalInformations(adds);
        request.setUserRequester(userRequester);

        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> {
            when(userRegistryApi.searchUsingPOST(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        mockPersistOnboarding(asserter);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        uoResource.setMail1(DIGITAL_ADDRESS_FIELD);
        uoResource.setDescrizioneUo(DESCRIPTION_FIELD);
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));

        asserter.execute(() -> when(orchestrationService.triggerOrchestration(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), response -> {
            Assertions.assertEquals(request.getProductId(), response.getProductId());
            Assertions.assertNotNull(response.getUsers().get(0).getUserMailUuid());
        });

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundAndWillNotUpdate(UniAsserter asserter) {
        UserRequest wrongManager = UserRequest.builder()
                .name("wrong_name")
                .taxCode("managerTaxCode")
                .role(PartyRole.MANAGER)
                .build();

        Onboarding request = createDummyOnboarding();
        List<UserRequest> users = List.of(wrongManager);
        request.setProductId(PROD_PAGOPA.getValue());
        request.getInstitution().setInstitutionType(InstitutionType.GSP);
        request.getInstitution().setOrigin(Origin.SELC);

        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> {
            when(userRegistryApi.searchUsingPOST(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWkSpid));
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        mockPersistOnboarding(asserter);

        asserter.assertFailedWith(() -> onboardingService.onboarding(request, users, null, userRequesterDto), InvalidRequestException.class);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboarding_whenUserFoundedAndWillUpdateMailUuid(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        UserRequest newManager = UserRequest.builder()
                .name("name")
                .taxCode("taxCode")
                .role(PartyRole.MANAGER)
                .email("example@live.it")
                .build();

        Onboarding request = createDummyOnboarding();
        List<UserRequest> users = List.of(newManager);
        request.setProductId(PROD_IO.getValue());
        request.getInstitution().setInstitutionType(InstitutionType.GSP);
        request.setUserRequester(userRequester);


        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> {
            when(userRegistryApi.searchUsingPOST(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        mockPersistOnboarding(asserter);

        asserter.execute(() -> when(orchestrationService.triggerOrchestration(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), response -> {
            Assertions.assertEquals(request.getProductId(), response.getProductId());
            Assertions.assertNotNull(response.getUsers().get(0).getUserMailUuid());
        });

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenUserNotFoundedAndWillSave(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId("productId");
        request.setInstitution(dummyInstitution());
        request.getInstitution().setOrigin(Origin.SELC);
        request.setUserRequester(userRequester);
        final UUID createUserId = UUID.randomUUID();

        mockPersistOnboarding(asserter);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));

        asserter.execute(() -> {
            when(userRegistryApi.searchUsingPOST(any(), any()))
                    .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
            when(userRegistryApi.saveUsingPATCH(any()))
                    .thenReturn(Uni.createFrom().item(UserId.builder().id(createUserId).build()));
        });

        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenReturn(Uni.createFrom().item(institutionResource));


        asserter.execute(() -> when(Onboarding.persistOrUpdate(any(List.class)))
                .thenAnswer(arg -> {
                    List<Onboarding> onboardings = (List<Onboarding>) arg.getArguments()[0];
                    onboardings.get(0).setId(UUID.randomUUID().toString());
                    return Uni.createFrom().nullItem();
                }));

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }


    @Test
    @RunOnVertxContext
    void onboarding_shouldThrowExceptionIfUserRegistryFails(UniAsserter asserter) {
        Onboarding onboardingDefaultRequest = createDummyOnboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        institution.setOrigin(Origin.SELC);
        onboardingDefaultRequest.setInstitution(institution);
        onboardingDefaultRequest.setProductId(PROD_PAGOPA.getValue());
        List<UserRequest> users = List.of(manager);
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        mockPersistOnboarding(asserter);

        mockSimpleProductValidAssert(onboardingDefaultRequest.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboardingDefaultRequest.getProductId(), asserter, true);

        asserter.execute(() -> {
            when(userRegistryApi.searchUsingPOST(any(), any()))
                    .thenReturn(Uni.createFrom().failure(new WebApplicationException()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingDefaultRequest, users, null, userRequesterDto), WebApplicationException.class);
    }

    void mockVerifyOnboardingNotFound() {
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().empty());
        when(Onboarding.find(any())).thenReturn(query);
    }

    void mockVerifyOnboardingNotEmpty(UniAsserter asserter) {

        asserter.execute(() -> {
            PanacheMock.mock(Onboarding.class);
            ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
            when(query.stream()).thenReturn(Multi.createFrom().items(createDummyOnboarding()));
            when(Onboarding.find(any())).thenReturn(query);
        });

    }

    @Test
    @RunOnVertxContext
    void completeWithoutSignatureVerification_shouldThrowExceptionWhenExpired(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setExpiringDate(LocalDateTime.now().minusDays(1));
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        asserter.assertFailedWith(() -> onboardingService.completeWithoutSignatureVerification(onboarding.getId(), null),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void completeWithoutSignatureVerification_shouldNotThrowExceptionWhenExpiredAndStatusIsToBeValidated(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setExpiringDate(LocalDateTime.now().minusDays(1));
        onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter);

        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        final String filepath = "upload-file-path";
        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
        mockUpdateToken(asserter, filepath);

        asserter.assertThat(() -> onboardingService.completeWithoutSignatureVerification(onboarding.getId(), TEST_FORM_ITEM),
                Assertions::assertNotNull);
    }

    @Test
    @RunOnVertxContext
    void complete_shouldNotThrowExceptionWhenExpiredAndStatusIsToBeValidated(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setExpiringDate(LocalDateTime.now().minusDays(1));
        onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter);

        //Mock find managerUserfiscal code
        String actualUseUid = onboarding.getUsers().get(0).getId();
        UserResource actualUserResource = new UserResource();
        actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
        asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                .thenReturn(Uni.createFrom().item(actualUserResource)));

        //Mock contract signature
        asserter.execute(() -> doNothing()
                .when(signatureService)
                .verifySignature(any(), any(), any()));

        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        final String filepath = "upload-file-path";
        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
        mockUpdateToken(asserter, filepath);

        asserter.assertThat(() -> onboardingService.complete(onboarding.getId(), TEST_FORM_ITEM),
                Assertions::assertNotNull);
    }

    @Test
    @RunOnVertxContext
    void complete_shouldThrowExceptionWhenExpiredAndStatusIsNotToBeValidated(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setExpiringDate(LocalDateTime.now().minusDays(1));
        onboarding.setStatus(OnboardingStatus.PENDING);
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        asserter.assertFailedWith(() -> onboardingService.complete(onboarding.getId(), null),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void completeWithoutSignatureVerification(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter);

        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        final String filepath = "upload-file-path";
        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
        mockUpdateToken(asserter, filepath);

        asserter.assertThat(() -> onboardingService.completeWithoutSignatureVerification(onboarding.getId(), TEST_FORM_ITEM),
                Assertions::assertNotNull);

    }

    @Test
    @RunOnVertxContext
    void completeWithoutSignatureValidation(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter);

        //Mock find managerUserfiscal code
        String actualUseUid = onboarding.getUsers().get(0).getId();
        UserResource actualUserResource = new UserResource();
        actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
        asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                .thenReturn(Uni.createFrom().item(actualUserResource)));

        //Mock contract signature fail
        asserter.execute(() -> doNothing()
                .when(signatureService)
                .verifySignature(any(), any(), any()));

        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        final String filepath = "upload-file-path";
        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
        mockUpdateToken(asserter, filepath);

        asserter.assertThat(() -> onboardingService.complete(onboarding.getId(), TEST_FORM_ITEM),
                Assertions::assertNotNull);
    }

    @Test
    @RunOnVertxContext
    void completeOnboardingUsersWithoutSignatureValidation(UniAsserter asserter) {
        Onboarding onboarding = createDummyUsersOnboarding();
        onboarding.setProductId("productParentId");
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter);

        //Mock find managerUserfiscal code
        String actualUseUid = onboarding.getUsers().get(0).getId();
        UserResource actualUserResource = new UserResource();
        actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
        asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                .thenReturn(Uni.createFrom().item(actualUserResource)));

        //Mock contract signature fail
        asserter.execute(() -> doNothing()
                .when(signatureService)
                .verifySignature(any(), any(), any()));

        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);
        mockSimpleProductValidAssert(onboarding.getProductId(), true, asserter, false, true);
        mockVerifyOnboardingNotFound();

        final String filepath = "upload-file-path";
        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
        mockUpdateToken(asserter, filepath);

        asserter.assertThat(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                Assertions::assertNotNull);
    }

    @Test
    @RunOnVertxContext
    void completeOnboardingUsers_throwProductNotOnboardedInReferenceOnboarding(UniAsserter asserter) {
        Onboarding onboarding = createDummyUsersOnboarding();
        onboarding.setStatus(OnboardingStatus.PENDING);
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter);
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);
        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        asserter.assertFailedWith(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void completeOnboardingUsers_throwOnboardingNotAllowedException(UniAsserter asserter) {
        Onboarding onboarding = createDummyUsersOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter);
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);

        asserter.execute(() -> when(onboardingValidationStrategy.validate(onboarding.getProductId()))
                .thenReturn(false));

        asserter.assertFailedWith(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                OnboardingNotAllowedException.class);
    }

    @Test
    @RunOnVertxContext
    void completeOnboardingUsers_throwInvalidRequestException(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter);
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);
        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        asserter.assertFailedWith(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                InvalidRequestException.class);
    }

    @Test
    void testOnboardingGet() {
        int page = 0, size = 3;
        Onboarding onboarding = createDummyOnboarding();
        mockFindOnboarding(onboarding);
        OnboardingGetResponse getResponse = getOnboardingGetResponse(onboarding);
        OnboardingGetFilters filters = OnboardingGetFilters.builder()
                .productId("prod-io")
                .taxCode("taxCode")
                .from("2023-12-01")
                .to("2023-12-31")
                .status("ACTIVE")
                .page(page)
                .size(size)
                .build();
        UniAssertSubscriber<OnboardingGetResponse> subscriber = onboardingService
                .onboardingGet(filters)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(getResponse);
    }

    @Test
    void testOnboardingGet2() {
        int page = 0, size = 3;
        Onboarding onboarding = createDummyOnboarding();
        mockFindOnboarding(onboarding);
        OnboardingGetResponse getResponse = getOnboardingGetResponse(onboarding);
        OnboardingGetFilters filters = OnboardingGetFilters.builder()
                .productId("prod-io")
                .taxCode("taxCode")
                .subunitCode("subunitCode")
                .from("2023-12-01")
                .to("2023-12-31")
                .status("ACTIVE")
                .page(page)
                .size(size)
                .build();
        UniAssertSubscriber<OnboardingGetResponse> subscriber = onboardingService
                .onboardingGet(filters)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(getResponse);
    }

    @Test
    void testOnboardingGetWithNoPagination() {
        Onboarding onboarding = createDummyOnboarding();
        ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.find(any(Document.class), any(Document.class))).thenReturn(query);
        when(Onboarding.find(any(Document.class), eq(null))).thenReturn(query);
        when(query.list()).thenReturn(Uni.createFrom().item(List.of(onboarding)));
        when(query.count()).thenReturn(Uni.createFrom().item(1L));

        OnboardingGetResponse getResponse = getOnboardingGetResponse(onboarding);
        OnboardingGetFilters filters = OnboardingGetFilters.builder()
                .taxCode("taxCode")
                .subunitCode("subunitCode")
                .from("2023-12-01")
                .to("2023-12-31")
                .productIds(List.of("prod-io"))
                .status("ACTIVE")
                .skipPagination(true)
                .build();
        UniAssertSubscriber<OnboardingGetResponse> subscriber = onboardingService
                .onboardingGet(filters)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(getResponse);
    }

    @Test
    void testOnboardingGetWithPaymentNode() {
        Onboarding onboarding = createDummyOnboarding();
        Payment payment = new Payment();
        payment.encryptedHolder("holder");
        payment.encryptedIban("iban");
        onboarding.setPayment(payment);
        ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.find(any(Document.class), any(Document.class))).thenReturn(query);
        when(Onboarding.find(any(Document.class), eq(null))).thenReturn(query);
        when(query.list()).thenReturn(Uni.createFrom().item(List.of(onboarding)));
        when(query.count()).thenReturn(Uni.createFrom().item(1L));

        OnboardingGetFilters filters = OnboardingGetFilters.builder()
                .taxCode("taxCode")
                .subunitCode("subunitCode")
                .from("2023-12-01")
                .to("2023-12-31")
                .productIds(List.of("prod-io"))
                .status("ACTIVE")
                .skipPagination(true)
                .build();
        UniAssertSubscriber<OnboardingGetResponse> subscriber = onboardingService
                .onboardingGet(filters)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        var response = subscriber.assertCompleted().getItem();
        assertNotNull(response);
        assertNotNull(response.getItems());
        assertNotNull(response.getItems().get(0));
        assertEquals(response.getItems().get(0).getPayment().getIban(), "iban");
    }


    private OnboardingGetResponse getOnboardingGetResponse(Onboarding onboarding) {
        OnboardingGet onboardingGet = onboardingMapper.toGetResponse(onboarding);
        OnboardingGetResponse response = new OnboardingGetResponse();
        response.setCount(1L);
        response.setItems(List.of(onboardingGet));
        return response;
    }

    private void mockFindOnboarding(Onboarding onboarding) {
        ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);
        ReactivePanacheQuery<Onboarding> queryPage = mock(ReactivePanacheQuery.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.find(any(Document.class), any(Document.class))).thenReturn(query);
        when(Onboarding.find(any(Document.class), eq(null))).thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(queryPage);
        when(queryPage.list()).thenReturn(Uni.createFrom().item(List.of(onboarding)));
        when(query.count()).thenReturn(Uni.createFrom().item(1L));
    }

    private void mockFindToken(UniAsserter asserter) {
        Token token = new Token();
        token.setChecksum("actual-checksum");
        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(() -> when(tokenService.retrieveToken(any(Onboarding.class), any(FormItem.class), any()))
                .thenReturn(Uni.createFrom().item(token)));
        asserter.execute(() -> when(tokenService.retrieveToken(anyString())).thenReturn(Uni.createFrom().item(token)));
    }

    private void mockUpdateToken(UniAsserter asserter, String filepath) {

        //Mock token update
        asserter.execute(() -> PanacheMock.mock(Token.class));
        ReactivePanacheUpdate panacheUpdate = mock(ReactivePanacheUpdate.class);
        asserter.execute(() -> when(panacheUpdate.where("contractSigned", filepath))
                .thenReturn(Uni.createFrom().item(1L)));
        asserter.execute(() -> when(Token.update(anyString(), any(Object[].class)))
                .thenReturn(panacheUpdate));
    }

    private Onboarding createDummyOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(UUID.randomUUID().toString());
        onboarding.setProductId("prod-id");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setSubunitCode("subunitCode");
        institution.setOrigin(Origin.IPA);
        onboarding.setInstitution(institution);
        onboarding.setUserRequester(userRequester);

        User user = new User();
        user.setId("actual-user-id");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));
        return onboarding;
    }

    private Onboarding createDummyUsersOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(UUID.randomUUID().toString());
        onboarding.setProductId("prod-id");
        onboarding.setReferenceOnboardingId("referenceOnboardinId");
        onboarding.setStatus(OnboardingStatus.COMPLETED);

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setSubunitCode("subunitCode");
        onboarding.setInstitution(institution);

        User user = new User();
        user.setId("actual-user-id");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));
        return onboarding;
    }

    @Test
    void testOnboardingUpdateStatusOK() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        mockUpdateOnboarding(onboarding.getId(), 1L);
        UniAssertSubscriber<Long> subscriber = onboardingService
                .rejectOnboarding(onboarding.getId(), "string")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void rejectOnboarding_statusIsCOMPLETED() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        mockUpdateOnboarding(onboarding.getId(), 1L);
        UniAssertSubscriber<Long> subscriber = onboardingService
                .rejectOnboarding(onboarding.getId(), "string")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void testOnboardingDeleteOnboardingNotFoundOrAlreadyDeleted() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));
        mockUpdateOnboarding(onboarding.getId(), 0L);

        UniAssertSubscriber<Long> subscriber = onboardingService
                .rejectOnboarding(onboarding.getId(), "string")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    private void mockUpdateOnboarding(String onboardingId, Long updatedItemCount) {
        ReactivePanacheUpdate query = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.update(any(Document.class))).thenReturn(query);
        when(query.where("_id", onboardingId)).thenReturn(Uni.createFrom().item(updatedItemCount));
    }

    @Test
    void onboardingGet() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .onboardingGet(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        OnboardingGet actual = subscriber.assertCompleted().awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(onboarding.getId(), actual.getId());
        Assertions.assertEquals(onboarding.getProductId(), actual.getProductId());
    }

    @Test
    void onboardingGet_shouldResourceNotFound() {
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        onboardingService
                .onboardingGet(UUID.randomUUID().toString())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ResourceNotFoundException.class);
    }

    @Test
    void onboardingPending() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.PENDING);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .onboardingPending(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        OnboardingGet actual = subscriber.assertCompleted().awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(onboarding.getId(), actual.getId());
        Assertions.assertEquals(onboarding.getProductId(), actual.getProductId());
    }

    @Test
    void onboardingPending_shouldResourceNotFound() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        onboardingService
                .onboardingPending(UUID.randomUUID().toString())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ResourceNotFoundException.class);
    }

    @Test
    void onboardingGetWithUserInfo() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        when(userRegistryApi.findByIdUsingGET(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(managerResource));

        when(tokenService.getAttachments(any()))
                .thenReturn(Uni.createFrom().item(List.of("filename")));

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .onboardingGetWithUserInfo(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        OnboardingGet actual = subscriber.assertCompleted().awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(onboarding.getId(), actual.getId());
        Assertions.assertEquals(onboarding.getProductId(), actual.getProductId());
        Assertions.assertEquals(onboarding.getUsers().size(), actual.getUsers().size());
        UserResponse actualUser = actual.getUsers().get(0);
        Assertions.assertEquals(actualUser.getName(), managerResource.getName().getValue());
        Assertions.assertEquals(actualUser.getSurname(), managerResource.getFamilyName().getValue());
    }

    @Test
    void approve() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(createDummyProduct(onboarding.getProductId(), false, false, true));

        mockVerifyOnboardingNotFound();

        when(orchestrationService.triggerOrchestration(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));

        when(onboardingValidationStrategy.validate(onboarding.getProductId()))
                .thenReturn(true);

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .approve(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        OnboardingGet actual = subscriber.awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(onboarding.getId(), actual.getId());
    }

    @Test
    void approve_throwExceptionIfOnboardingIsNotToBeValidated() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(createDummyProduct(onboarding.getProductId(), false, false, true));

        onboardingService
                .approve(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void approveCompletion() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));

        when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(createDummyProduct(onboarding.getProductId(), false, false, true));

        mockVerifyOnboardingNotFound();

        when(onboardingValidationStrategy.validate(onboarding.getProductId()))
                .thenReturn(true);

        UniAssertSubscriber<OnboardingGet> subscriber = onboardingService
                .approve(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        OnboardingGet actual = subscriber.awaitItem().getItem();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(onboarding.getId(), actual.getId());

        verify(orchestrationService, times(1))
                .triggerOrchestration(any(), any());
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_importPA(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        request.setBilling(billing);
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setImported(true);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        request.setInstitution(institutionBaseRequest);
        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setIstatCode("istatCode");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCountryAbbreviation("IT");
        geographicTaxonomyResource.setProvinceAbbreviation("RM");
        geographicTaxonomyResource.setDesc("desc");
        asserter.execute(() -> when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(geographicTaxonomyResource)));

        asserter.assertThat(() -> onboardingService.onboardingImport(request, users, contractImported, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_Onboarding_importPSP(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        request.setProductId(PROD_PAGOPA.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setInstitutionType(PSP);
        PaymentServiceProvider paymentServiceProvider = new PaymentServiceProvider();
        paymentServiceProvider.setAbiCode("abiCode");
        institutionBaseRequest.setPaymentServiceProvider(paymentServiceProvider);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setActivatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.assertThat(() -> onboardingService.onboardingImport(request, List.of(), contractImported, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_aggregationCompletion(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setInstitutionType(InstitutionType.SA);
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        request.setInstitution(institutionBaseRequest);
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        request.setBilling(billing);

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        asserter.assertThat(() -> onboardingService.onboardingAggregationCompletion(request, users, null, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingCompletion(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        Product product = createDummyProduct(PROD_INTEROP.getValue(), false, false, true);
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setInstitutionType(InstitutionType.SA);
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> when(productService.getProduct(any())).thenReturn(product));

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setDescription(DESCRIPTION_FIELD);
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        asserter.assertThat(() -> onboardingService.onboardingCompletion(request, users, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingPgCompletion(UniAsserter asserter) {
        Onboarding request = new Onboarding();
        Product product = createDummyProduct(PROD_PN.getValue(), false, false, true);
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_PN.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setInstitutionType(InstitutionType.PG);
        institutionBaseRequest.setOrigin(Origin.INFOCAMERE);
        request.setInstitution(institutionBaseRequest);

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> when(productService.getProduct(any())).thenReturn(product));

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        BusinessResource businessResource = new BusinessResource();
        businessResource.setBusinessTaxId("taxCode");
        BusinessesResource resource = new BusinessesResource();
        resource.setBusinesses(List.of(businessResource));
        when(infocamereApi.institutionsByLegalTaxIdUsingPOST(any())).thenReturn(Uni.createFrom().item(resource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        asserter.assertThat(() -> onboardingService.onboardingPgCompletion(request, users), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingUsers(UniAsserter asserter) {
        OnboardingUserRequest request = new OnboardingUserRequest();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        request.setUsers(users);
        request.setInstitutionType(InstitutionType.PA);
        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);

        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));
        when(Onboarding.find((Document) any(), any())).thenReturn(query);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        org.openapi.quarkus.core_json.model.InstitutionResponse institutionResponse = new org.openapi.quarkus.core_json.model.InstitutionResponse();
        institutionResponse.setOrigin(Origin.IPA.name());
        institutionResponse.setOriginId("originId");
        InstitutionsResponse response = new InstitutionsResponse();
        response.setInstitutions(List.of(institutionResponse));
        asserter.execute(() -> when(institutionService.getInstitutionsUsingGET(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(response)));

        asserter.assertThat(() -> onboardingService.onboardingUsers(request, "userId", WorkflowType.USERS), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingUsersWithNullOnboardingReeferenceId(UniAsserter asserter) {
        OnboardingUserRequest request = new OnboardingUserRequest();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        request.setUsers(users);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());
        when(Onboarding.find((Document) any(), any())).thenReturn(query);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        org.openapi.quarkus.core_json.model.InstitutionResponse institutionResponse = new org.openapi.quarkus.core_json.model.InstitutionResponse();
        institutionResponse.setOrigin(Origin.IPA.name());
        institutionResponse.setOriginId("originId");
        InstitutionsResponse response = new InstitutionsResponse();
        response.setInstitutions(List.of(institutionResponse));
        when(institutionService.getInstitutionsUsingGET(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(response));

        asserter.assertFailedWith(() -> onboardingService.onboardingUsers(request, "userId", WorkflowType.USERS_EA), ResourceNotFoundException.class);

    }

    @Test
    void onboardingUsersWithInstitutionNotFound() {
        OnboardingUserRequest request = new OnboardingUserRequest();
        List<UserRequest> users = List.of(manager);
        request.setTaxCode("taxCode");
        request.setSubunitCode("subunitCode");
        request.setProductId(PROD_INTEROP.getValue());
        request.setUsers(users);
        request.setInstitutionType(PA);

        org.openapi.quarkus.core_json.model.InstitutionResponse institutionResponse = new org.openapi.quarkus.core_json.model.InstitutionResponse();
        institutionResponse.setOrigin(Origin.IPA.name());
        institutionResponse.setOriginId("originId");
        institutionResponse.setInstitutionType("PSP");
        InstitutionsResponse response = new InstitutionsResponse();
        response.setInstitutions(List.of(institutionResponse, institutionResponse));
        when(institutionService.getInstitutionsUsingGET("taxCode", "subunitCode", null, null, null, null))
                .thenReturn(Uni.createFrom().item(response));

        onboardingService
                .onboardingUsers(request, "userId", WorkflowType.USERS)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ResourceNotFoundException.class);

    }

    @Test
    @RunOnVertxContext
    void onboardingUsersWithInstitutions(UniAsserter asserter) {

        OnboardingUserRequest request = new OnboardingUserRequest();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        request.setUsers(users);
        request.setInstitutionType(InstitutionType.PSP);
        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);

        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));
        when(Onboarding.find((Document) any(), any())).thenReturn(query);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        org.openapi.quarkus.core_json.model.InstitutionResponse institutionResponse = new org.openapi.quarkus.core_json.model.InstitutionResponse();
        institutionResponse.setOrigin(Origin.IPA.name());
        institutionResponse.setOriginId("originId");
        institutionResponse.setInstitutionType("PSP");
        InstitutionsResponse response = new InstitutionsResponse();
        response.setInstitutions(List.of(institutionResponse, institutionResponse));
        asserter.execute(() -> when(institutionService.getInstitutionsUsingGET(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(response)));

        asserter.assertThat(() -> onboardingService.onboardingUsers(request, "userId", WorkflowType.USERS), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
        });

    }

    @Test
    void testInstitutionOnboardings() {
        Onboarding onboarding = mock(Onboarding.class);
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));
        when(Onboarding.find(any())).thenReturn(query);
        UniAssertSubscriber<List<OnboardingResponse>> subscriber = onboardingService
                .institutionOnboardings("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.PENDING)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        List<OnboardingResponse> response = subscriber.assertCompleted().awaitItem().getItem();
        assertFalse(response.isEmpty());
        assertEquals(1, response.size());
    }

    @Test
    void checkRecipientCodeWithValidResponse() {
        String recipientCode = "recipientCode";
        String originId = "originId";
        CustomError customError = CustomError.DENIED_NO_ASSOCIATION;
        UOResource uoResource = Mockito.mock(UOResource.class);
        OnboardingUtils onboardingUtils = Mockito.mock(OnboardingUtils.class);
        // Mock the response from uoApi.findByUnicodeUsingGET1
        when(uoApi.findByUnicodeUsingGET1(eq(recipientCode), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        // Mock the response from onboardingUtils.validationRecipientCode
        when(onboardingUtils.getValidationRecipientCodeError(originId, uoResource))
                .thenReturn(Uni.createFrom().item(customError));

        // Call the method under test
        onboardingService
                .checkRecipientCode(recipientCode, originId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(customError);
    }

    @Test
    void checkRecipientCodeWithGenericError() {
        final String recipientCode = "recipientCode";
        final String originId = "originId";
        RuntimeException exception = new RuntimeException("Generic error");
        when(uoApi.findByUnicodeUsingGET1(eq(recipientCode), any()))
                .thenReturn(Uni.createFrom().failure(exception));
        UniAssertSubscriber<CustomError> subscriber = onboardingService
                .checkRecipientCode(recipientCode, originId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(RuntimeException.class);
    }

    @Test
    void checkRecipientCodeWithResourceNotFound() {
        final String recipientCode = "recipientCode";
        final String originId = "originId";
        ClientWebApplicationException exception = new ClientWebApplicationException(HttpStatus.SC_NOT_FOUND);
        when(uoApi.findByUnicodeUsingGET1(eq(recipientCode), any()))
                .thenReturn(Uni.createFrom().failure(exception));
        UniAssertSubscriber<CustomError> subscriber = onboardingService
                .checkRecipientCode(recipientCode, originId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class);
    }

    @Nested
    @TestProfile(OnboardingTestProfile.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class OnboardingServiceDefaultWithSignatureValidationTest {
        // can't be tested
        //@Test
        @RunOnVertxContext
        void complete_shouldThrowExceptionWhenSignatureFail(UniAsserter asserter) {
            Onboarding onboarding = createDummyOnboarding();
            asserter.execute(() -> PanacheMock.mock(Onboarding.class));
            asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                    .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

            mockFindToken(asserter);

            //Mock find managerUserfiscal code
            String actualUseUid = onboarding.getUsers().get(0).getId();
            UserResource actualUserResource = new UserResource();
            actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
            asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                    .thenReturn(Uni.createFrom().item(actualUserResource)));

            //Mock contract signature fail
            asserter.execute(() -> doThrow(InvalidRequestException.class)
                    .when(signatureService)
                    .verifySignature(any(), any(), any()));

            asserter.assertFailedWith(() -> onboardingService.complete(onboarding.getId(), TEST_FORM_ITEM),
                    InvalidRequestException.class);
        }

        // can't be tested
        //@Test
        @RunOnVertxContext
        void completeOnboardingUsers_shouldThrowExceptionWhenSignatureFail(UniAsserter asserter) {
            Onboarding onboarding = createDummyUsersOnboarding();
            asserter.execute(() -> PanacheMock.mock(Onboarding.class));
            asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                    .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

            mockFindToken(asserter);

            //Mock find managerUserfiscal code
            String actualUseUid = onboarding.getUsers().get(0).getId();
            UserResource actualUserResource = new UserResource();
            actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
            asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                    .thenReturn(Uni.createFrom().item(actualUserResource)));

            //Mock contract signature fail
            asserter.execute(() -> doThrow(InvalidRequestException.class)
                    .when(signatureService)
                    .verifySignature(any(), any(), any()));

            asserter.assertFailedWith(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                    InvalidRequestException.class);
        }

        @Test
        @RunOnVertxContext
        void completeOnboardingUsers(UniAsserter asserter) {
            Onboarding onboarding = createDummyUsersOnboarding();
            onboarding.setProductId("productParentId");
            asserter.execute(() -> PanacheMock.mock(Onboarding.class));
            asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                    .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

            mockFindToken(asserter);

            //Mock find managerUserfiscal code
            String actualUseUid = onboarding.getUsers().get(0).getId();
            UserResource actualUserResource = new UserResource();
            actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
            asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                    .thenReturn(Uni.createFrom().item(actualUserResource)));

            //Mock contract signature fail
            asserter.execute(() -> doNothing()
                    .when(signatureService)
                    .verifySignature(any(), any(), any()));

            mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);
            mockSimpleProductValidAssert(onboarding.getProductId(), true, asserter, false, true);
            mockVerifyOnboardingNotFound();

            final String filepath = "upload-file-path";
            when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
            mockUpdateToken(asserter, filepath);

            asserter.assertThat(() -> onboardingService.completeOnboardingUsers(onboarding.getId(), TEST_FORM_ITEM),
                    Assertions::assertNotNull);
        }

        @Test
        @RunOnVertxContext
        void complete(UniAsserter asserter) {
            Onboarding onboarding = createDummyOnboarding();
            asserter.execute(() -> PanacheMock.mock(Onboarding.class));
            asserter.execute(() -> when(Onboarding.findByIdOptional(any()))
                    .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

            mockFindToken(asserter);

            //Mock find managerUserfiscal code
            String actualUseUid = onboarding.getUsers().get(0).getId();
            UserResource actualUserResource = new UserResource();
            actualUserResource.setFiscalCode("ACTUAL-FISCAL-CODE");
            asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, actualUseUid))
                    .thenReturn(Uni.createFrom().item(actualUserResource)));

            //Mock contract signature fail
            asserter.execute(() -> doNothing()
                    .when(signatureService)
                    .verifySignature(any(), any(), any()));

            mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);
            mockVerifyOnboardingNotFound();
            mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

            final String filepath = "upload-file-path";
            when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
            mockUpdateToken(asserter, filepath);

            asserter.assertThat(() -> onboardingService.complete(onboarding.getId(), TEST_FORM_ITEM),
                    Assertions::assertNotNull);
        }
    }

    void mockPersistOnboarding(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.persist(any(Onboarding.class), any()))
                .thenAnswer(arg -> {
                    Onboarding onboarding = (Onboarding) arg.getArguments()[0];
                    onboarding.setId(UUID.randomUUID().toString());
                    onboarding.setInstitution(((Onboarding) arg.getArguments()[0]).getInstitution());
                    return Uni.createFrom().nullItem();
                }));
    }

    void mockPersistToken(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(() -> when(Token.persist(any(Token.class), any()))
                .thenAnswer(arg -> {
                    Token token = (Token) arg.getArguments()[0];
                    token.setId(UUID.randomUUID().toString());
                    return Uni.createFrom().nullItem();
                }));
    }

    void mockVerifyAllowedProductList(String productId, UniAsserter asserter, boolean aspectedResult) {
        asserter.execute(() -> when(onboardingValidationStrategy.validate(productId)).thenReturn(aspectedResult));
    }

    void mockAllowedProductByInstitutionTaxCodeList(UniAsserter asserter, boolean aspectedResult) {
        asserter.execute(() -> when(productService.verifyAllowedByInstitutionTaxCode(anyString(), anyString())).thenReturn(aspectedResult));
    }

    private void mockUpdateOnboardingInfo(String onboardingId, Long updatedItemCount) {
        ReactivePanacheUpdate query = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.update(any(Document.class))).thenReturn(query);
        when(query.where("_id", onboardingId)).thenReturn(Uni.createFrom().item(updatedItemCount));
    }

    @Test
    void testUpdateOnboardingStatusOK() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.TOBEVALIDATED);

        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        mockUpdateOnboardingInfo(onboarding.getId(), 1L);
        UniAssertSubscriber<Long> subscriber = onboardingService
                .updateOnboarding(onboarding.getId(), onboarding)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void testOnboardingUpdateOnboardingNotFound() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));
        mockUpdateOnboardingInfo(onboarding.getId(), 0L);

        UniAssertSubscriber<Long> subscriber = onboardingService
                .updateOnboarding(onboarding.getId(), onboarding)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void testCheckManager() {
        CheckManagerRequest request = createDummyCheckManagerRequest();
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().items(onboarding));
        when(Onboarding.find((Document) any(), any())).thenReturn(query);
        UserRequest userRequest = new UserRequest();
        userRequest.setRole(PartyRole.MANAGER);

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        Uni<CheckManagerResponse> result = onboardingService.checkManager(request);
        CheckManagerResponse checkResponse = result.await().indefinitely();

        assertNotNull(checkResponse);
    }

    @Test
    void testCheckManagerWithTrueCheck() {
        final UUID uuid = UUID.randomUUID();
        CheckManagerRequest request = createDummyCheckManagerRequest();
        Onboarding onboarding = createDummyOnboarding();
        onboarding.getUsers().get(0).setId(uuid.toString());
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().items(onboarding));
        when(Onboarding.find((Document) any(), any())).thenReturn(query);
        UserResource userResource = new UserResource();
        userResource.setId(uuid);
        when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<CheckManagerResponse> subscriber = onboardingService
                .checkManager(request)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        CheckManagerResponse response = subscriber.getItem();
        assertNotNull(response);
    }

    @Test
    void testCheckManagerWithEmptyOnboardings() {
        CheckManagerRequest request = createDummyCheckManagerRequest();
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().empty());
        when(Onboarding.find((Document) any(), any())).thenReturn(query);
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<CheckManagerResponse> subscriber = onboardingService
                .checkManager(request)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
        CheckManagerResponse checkResponse = subscriber.getItem();
        assertNotNull(checkResponse);
        assertFalse(checkResponse.isResponse());
    }

    private static CheckManagerRequest createDummyCheckManagerRequest() {
        CheckManagerRequest request = new CheckManagerRequest();
        UUID userId = UUID.randomUUID();
        request.setUserId(userId);
        return request;
    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPgFailsWhenUserListIsWrong(UniAsserter asserter) {
        assertThrows(InvalidRequestException.class, () -> onboardingService.onboardingUserPg(new Onboarding(), new ArrayList<>()));
    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPgFailsWhenInstitutionWasNotPreviouslyOnboarded(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        List<UserRequest> userRequests = List.of(manager);

        asserter.execute(() -> {
            PanacheMock.mock(Onboarding.class);
            ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
            when(query.stream()).thenReturn(Multi.createFrom().empty());
            when(Onboarding.find((Document) any(), any())).thenReturn(query);
        });

        asserter.assertFailedWith(() -> onboardingService.onboardingUserPg(onboarding, userRequests), ResourceNotFoundException.class);
    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPgFailsWhenUserWasAlreadyManager(UniAsserter asserter) {
        Onboarding previousOnboarding = createDummyOnboarding();
        previousOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);

        Onboarding newOnboarding = createDummyOnboarding();
        newOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);

        List<UserRequest> userRequests = List.of(manager);

        mockFindOnboarding(asserter, previousOnboarding);
        mockSimpleProductValidAssert(newOnboarding.getProductId(), false, asserter, false, true);
        mockSimpleSearchPOSTAndPersist(asserter);

        mockRetrieveUserInstitutions(newOnboarding.getInstitution().getId(), true, asserter);

        asserter.assertFailedWith(() -> onboardingService.onboardingUserPg(newOnboarding, userRequests), InvalidRequestException.class);

    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPgFailsWhenUserIsNotManagerOnInfocamereRegistry(UniAsserter asserter) {
        Onboarding previousOnboarding = createDummyOnboarding();
        previousOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        previousOnboarding.getInstitution().setOrigin(Origin.INFOCAMERE);

        Onboarding newOnboarding = createDummyOnboarding();
        newOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        newOnboarding.getInstitution().setOrigin(Origin.INFOCAMERE);

        List<UserRequest> userRequests = List.of(manager);

        mockFindOnboarding(asserter, previousOnboarding);
        mockSimpleProductValidAssert(newOnboarding.getProductId(), false, asserter, false, true);
        mockSimpleSearchPOSTAndPersist(asserter);

        mockRetrieveUserInstitutions(newOnboarding.getInstitution().getId(), false, asserter);

        asserter.execute(() -> when(infocamereApi.institutionsByLegalTaxIdUsingPOST(any()))
                .thenReturn(Uni.createFrom().item(new BusinessesResource())));

        asserter.assertFailedWith(() -> onboardingService.onboardingUserPg(newOnboarding, userRequests), InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPgFailsWhenUserIsNotManagerOnAdeRegistry(UniAsserter asserter) {
        Onboarding previousOnboarding = createDummyOnboarding();
        previousOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        previousOnboarding.getInstitution().setOrigin(Origin.ADE);

        Onboarding newOnboarding = createDummyOnboarding();
        newOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        newOnboarding.getInstitution().setOrigin(Origin.ADE);

        List<UserRequest> userRequests = List.of(manager);

        mockFindOnboarding(asserter, previousOnboarding);
        mockSimpleProductValidAssert(newOnboarding.getProductId(), false, asserter, false, true);
        mockSimpleSearchPOSTAndPersist(asserter);

        mockRetrieveUserInstitutions(newOnboarding.getInstitution().getId(), false, asserter);

        LegalVerificationResult legalVerificationResult = new LegalVerificationResult();
        legalVerificationResult.setVerificationResult(false);

        asserter.execute(() -> when(nationalRegistriesApi.verifyLegalUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(legalVerificationResult)));

        asserter.assertFailedWith(() -> onboardingService.onboardingUserPg(newOnboarding, userRequests), InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void testOnboardingUserPg(UniAsserter asserter) {
        Onboarding previousOnboarding = createDummyOnboarding();
        previousOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        previousOnboarding.getInstitution().setOrigin(Origin.ADE);

        Onboarding newOnboarding = createDummyOnboarding();
        newOnboarding.getInstitution().setInstitutionType(InstitutionType.PG);
        newOnboarding.getInstitution().setOrigin(Origin.ADE);

        List<UserRequest> userRequests = List.of(manager);

        mockFindOnboarding(asserter, previousOnboarding);
        mockSimpleProductValidAssert(newOnboarding.getProductId(), false, asserter, false, true);
        mockSimpleSearchPOSTAndPersist(asserter);

        mockRetrieveUserInstitutions(newOnboarding.getInstitution().getId(), false, asserter);

        LegalVerificationResult legalVerificationResult = new LegalVerificationResult();
        legalVerificationResult.setVerificationResult(true);

        asserter.execute(() -> when(nationalRegistriesApi.verifyLegalUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(legalVerificationResult)));

        asserter.execute(() -> when(Onboarding.persistOrUpdate(any(List.class)))
                .thenAnswer(arg -> {
                    List<Onboarding> onboardings = (List<Onboarding>) arg.getArguments()[0];
                    onboardings.get(0).setId(UUID.randomUUID().toString());
                    return Uni.createFrom().nullItem();
                }));

        asserter.execute(() -> when(orchestrationService.triggerOrchestration(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));

        asserter.assertNotNull(() -> onboardingService.onboardingUserPg(newOnboarding, userRequests));
    }

    private void mockRetrieveUserInstitutions(String institutionId, boolean shouldRetrieveUser, UniAsserter asserter) {
        UserInstitutionResponse userInstitutionResponse = new UserInstitutionResponse();
        userInstitutionResponse.setId("test");
        userInstitutionResponse.setInstitutionId(institutionId);

        asserter.execute(() -> when(userInstitutionApi.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(shouldRetrieveUser ? List.of(userInstitutionResponse) : Collections.emptyList())));
    }

    @Test
    @RunOnVertxContext
    void onboardingAggregationImportTest(UniAsserter asserter) {
        // given
        Onboarding request = new Onboarding();
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        request.setBilling(billing);
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_PN.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setInstitutionType(PA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setImported(true);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        request.setInstitution(institutionBaseRequest);
        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setIstatCode("istatCode");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCountryAbbreviation("IT");
        geographicTaxonomyResource.setProvinceAbbreviation("RM");
        geographicTaxonomyResource.setDesc("desc");
        asserter.execute(() -> when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(geographicTaxonomyResource)));

        List<AggregateInstitutionRequest> aggregates = new ArrayList<>();
        AggregateInstitutionRequest aggregateInstitutionRequest = new AggregateInstitutionRequest();
        aggregateInstitutionRequest.setTaxCode("taxCode");
        aggregateInstitutionRequest.setUsers(List.of(delegate1));

        AggregateInstitutionRequest aggregateInstitutionRequest2 = new AggregateInstitutionRequest();
        aggregateInstitutionRequest2.setTaxCode("taxCode");
        aggregateInstitutionRequest2.setSubunitCode("subunitCode");
        aggregateInstitutionRequest2.setUsers(List.of(delegate2));

        AggregateInstitutionRequest aggregateInstitutionRequest3 = new AggregateInstitutionRequest();
        aggregateInstitutionRequest3.setTaxCode("taxCode2");
        aggregateInstitutionRequest3.setUsers(List.of(delegate3));

        aggregates.add(aggregateInstitutionRequest);
        aggregates.add(aggregateInstitutionRequest2);
        aggregates.add(aggregateInstitutionRequest3);

        User user1 = new User("test1", PartyRole.DELEGATE, null, null);
        User user2 = new User("test2", PartyRole.DELEGATE, null, null);
        User user3 = new User("test3", PartyRole.DELEGATE, null, null);


        AggregateInstitution aggregateInstitution1 = new AggregateInstitution();
        aggregateInstitution1.setTaxCode("taxCode");
        aggregateInstitution1.setUsers(List.of(user1));

        AggregateInstitution aggregateInstitution2 = new AggregateInstitution();
        aggregateInstitution2.setTaxCode("taxCode");
        aggregateInstitution2.setSubunitCode("subunitCode");
        aggregateInstitution2.setUsers(List.of(user2));

        AggregateInstitution aggregateInstitution3 = new AggregateInstitution();
        aggregateInstitution3.setTaxCode("taxCode2");
        aggregateInstitution3.setUsers(List.of(user3));

        request.setAggregates(List.of(aggregateInstitution1, aggregateInstitution2, aggregateInstitution3));

        // when
        asserter.assertThat(() -> onboardingService.onboardingAggregationImport(request, contractImported, users, aggregates, null),
                Assertions::assertNotNull);

        // then
        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    void deleteOnboarding_statusIsPENDING() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.PENDING);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        UniAssertSubscriber<Long> subscriber = onboardingService
                .deleteOnboarding(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void deleteOnboarding_workflowTypeUSERS() {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setWorkflowType(WorkflowType.USERS);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        UniAssertSubscriber<Long> subscriber = onboardingService
                .deleteOnboarding(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void testDeleteOnboardingStatusOK() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        mockUpdateOnboarding(onboarding.getId(), 1L);
        UniAssertSubscriber<Long> subscriber = onboardingService
                .deleteOnboarding(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void testDeleteOnboardingNotFoundOrAlreadyDeleted() {
        Onboarding onboarding = createDummyOnboarding();
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));
        mockUpdateOnboarding(onboarding.getId(), 0L);

        UniAssertSubscriber<Long> subscriber = onboardingService
                .deleteOnboarding(onboarding.getId())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void retrieveOnboardingByInstitutionId_shouldReturnMappedObject_whenOnboardingExists() {
        // Arrange
        String institutionId = "inst-001";
        String productId = "prod-abc";

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setId(institutionId);
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setProductId(productId);
        onboarding.setInstitution(institution); // usa un costruttore/dummy appropriato

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);
        when(Onboarding.find(anyString(), (Object) any())).thenReturn(query);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(onboarding));

        // Act
        OnboardingGet result = onboardingService
                .retrieveOnboardingByInstitutionId(institutionId, productId)
                .await().indefinitely();

        // Assert
        assertNotNull(result);
        assertEquals(productId, result.getProductId()); // verifica sui campi, non equals()
        assertEquals(institutionId, result.getInstitution().getId());
    }

    @Test
    void retrieveOnboardingByInstitutionId_shouldThrowNotFound_whenNoResult() {

        String institutionId = "inst-404";
        String productId = "prod-404";

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);

        when(Onboarding.find(anyString(), (Object) any())).thenReturn(query);
        when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                onboardingService
                        .retrieveOnboardingByInstitutionId(institutionId, productId)
                        .await().indefinitely()
        );

        assertTrue(exception.getMessage().contains("institutionId=" + institutionId));
        assertTrue(exception.getMessage().contains("productId=" + productId));
    }


    private void mockFindOnboarding(UniAsserter asserter, Onboarding onboarding) {
        asserter.execute(() -> {
            PanacheMock.mock(Onboarding.class);
            ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
            when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));
            when(Onboarding.find((Document) any(), any())).thenReturn(query);
        });
    }

    Institution dummyInstitution() {
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        return institution;
    }

    @Test
    @RunOnVertxContext
    void onboarding_prv_whenProductIsNotAllowedAndInstitutionTaxCodeIsAllowed(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode-OK");
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");

        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));

        mockSimpleSearchPOSTAndPersist(asserter);

        Product product = mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        product.setAllowedInstitutionTaxCode(List.of("taxCode-OK"));

        mockVerifyOnboardingNotFound();

        mockAllowedProductByInstitutionTaxCodeList(asserter, true);
        mockVerifyAllowedProductList(request.getProductId(), asserter, false);

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_verifyExpirationDateWhenIsSet(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");

        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));

        mockSimpleSearchPOSTAndPersist(asserter);

        Product product = mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        product.setExpirationDate(Integer.valueOf("30"));

        asserter.execute(() -> when(productService.getProductExpirationDate(request.getProductId()))
                .thenReturn(Integer.valueOf("30")));

        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(request.getProductId(), asserter, true);

        mockAllowedProductByInstitutionTaxCodeList(asserter, false);

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void verifyOnboarding_prvPfSuccessfulUserSearchAndOnboarding(UniAsserter asserter) {
        String taxCode = "RSSMRA80A01H501U";
        String subunitCode = "subunitCode";
        String origin = "origin";
        String originId = "originId";
        OnboardingStatus status = OnboardingStatus.COMPLETED;
        String productId = "productId";
        InstitutionType institutionType = InstitutionType.PRV_PF;

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        OnboardingResponse onboardingResponse = new OnboardingResponse();
        onboardingResponse.setId("onboardingId");

        asserter.execute(() -> {
            when(userRegistryApi.searchUsingPOST(eq(USERS_FIELD_LIST), any(UserSearchDto.class)))
                    .thenReturn(Uni.createFrom().item(userResource));

            PanacheMock.mock(Onboarding.class);
            ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
            when(query.stream()).thenReturn(Multi.createFrom().items(createDummyOnboarding()));
            when(Onboarding.find(any(Document.class))).thenReturn(query);
        });

        asserter.assertThat(() ->
                        onboardingService.verifyOnboarding(taxCode, subunitCode, origin, originId, status, productId, institutionType),
                result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(1);
                });

        asserter.execute(() -> {
            verify(userRegistryApi).searchUsingPOST(eq(USERS_FIELD_LIST), argThat(dto ->
                    dto.getFiscalCode().equals(taxCode)));
        });
    }

    @Test
    @RunOnVertxContext
    void verifyOnboarding_prvPfUserNotFoundReturnsEmpty(UniAsserter asserter) {
        String taxCode = "RSSMRA80A01H501U";
        String subunitCode = "subunitCode";
        String origin = "origin";
        String originId = "originId";
        OnboardingStatus status = OnboardingStatus.COMPLETED;
        String productId = "productId";
        InstitutionType institutionType = InstitutionType.PRV_PF;

        WebApplicationException notFoundException = new WebApplicationException(Response.status(404).build());

        asserter.execute(() -> {
            when(userRegistryApi.searchUsingPOST(eq(USERS_FIELD_LIST), any(UserSearchDto.class)))
                    .thenReturn(Uni.createFrom().failure(notFoundException));
        });

        asserter.assertThat(() ->
                        onboardingService.verifyOnboarding(taxCode, subunitCode, origin, originId, status, productId, institutionType),
                result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).isEmpty();
                });

        asserter.execute(() -> {
            verify(userRegistryApi).searchUsingPOST(eq(USERS_FIELD_LIST), argThat(dto ->
                    dto.getFiscalCode().equals(taxCode)));
        });
    }

    @Test
    @RunOnVertxContext
    void verifyOnboarding_prvPfSuccessfulUserSearchAndReferenceOnboardingId(UniAsserter asserter) {
        final String taxCode = "RSSMRA80A01H501U";
        final String subunitCode = "subunitCode";
        final String origin = "origin";
        final String originId = "originId";
        final String productId = "productId";
        OnboardingStatus status = OnboardingStatus.COMPLETED;
        InstitutionType institutionType = InstitutionType.PRV_PF;

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        Onboarding onboarding = createDummyOnboarding();
        onboarding.setReferenceOnboardingId("referenceOnboardingId");

        asserter.execute(() -> {
            when(userRegistryApi.searchUsingPOST(eq(USERS_FIELD_LIST), any(UserSearchDto.class)))
                    .thenReturn(Uni.createFrom().item(userResource));

            PanacheMock.mock(Onboarding.class);
            ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
            when(query.stream()).thenReturn(Multi.createFrom().items(onboarding));
            when(Onboarding.find(any(Document.class))).thenReturn(query);
        });

        asserter.assertThat(() ->
                        onboardingService.verifyOnboarding(taxCode, subunitCode, origin, originId, status, productId, institutionType),
                result -> {
                    assertThat(result).isNotNull();
                    assertThat(result).hasSize(0);
                });

        asserter.execute(() -> {
            verify(userRegistryApi).searchUsingPOST(eq(USERS_FIELD_LIST), argThat(dto ->
                    dto.getFiscalCode().equals(taxCode)));
        });
    }

    @Test
    @RunOnVertxContext
    void verifyOnboarding_prvPfUserSearchThrowsWebApplicationException(UniAsserter asserter) {
        String taxCode = "RSSMRA80A01H501U";
        String subunitCode = "subunitCode";
        String origin = "origin";
        String originId = "originId";
        OnboardingStatus status = OnboardingStatus.COMPLETED;
        String productId = "productId";
        InstitutionType institutionType = InstitutionType.PRV_PF;

        WebApplicationException serverErrorException = new WebApplicationException(Response.status(500).build());

        asserter.execute(() -> {
            when(userRegistryApi.searchUsingPOST(eq(USERS_FIELD_LIST), any(UserSearchDto.class)))
                    .thenReturn(Uni.createFrom().failure(serverErrorException));
        });

        asserter.assertFailedWith(() ->
                        onboardingService.verifyOnboarding(taxCode, subunitCode, origin, originId, status, productId, institutionType),
                WebApplicationException.class);

        asserter.execute(() -> {
            verify(userRegistryApi).searchUsingPOST(eq(USERS_FIELD_LIST), argThat(dto ->
                    dto.getFiscalCode().equals(taxCode)));
        });
    }

    @Test
    @RunOnVertxContext
    void verifyOnboarding_prvPfUserSearchThrowsOtherException(UniAsserter asserter) {
        String taxCode = "RSSMRA80A01H501U";
        String subunitCode = "subunitCode";
        String origin = "origin";
        String originId = "originId";
        OnboardingStatus status = OnboardingStatus.COMPLETED;
        String productId = "productId";
        InstitutionType institutionType = InstitutionType.PRV_PF;

        RuntimeException runtimeException = new RuntimeException("Generic error");

        asserter.execute(() -> {
            when(userRegistryApi.searchUsingPOST(eq(USERS_FIELD_LIST), any(UserSearchDto.class)))
                    .thenReturn(Uni.createFrom().failure(runtimeException));
        });

        asserter.assertFailedWith(() ->
                        onboardingService.verifyOnboarding(taxCode, subunitCode, origin, originId, status, productId, institutionType),
                RuntimeException.class);

        asserter.execute(() -> {
            verify(userRegistryApi).searchUsingPOST(eq(USERS_FIELD_LIST), argThat(dto ->
                    dto.getFiscalCode().equals(taxCode)));
        });
    }

    @Test
    @RunOnVertxContext
    void onboardingScecTest(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding request = new Onboarding();
        List<UserRequest> users = List.of(manager);
        request.setProductId(PROD_INTEROP.getValue());
        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setOriginId("taxCode-OK");
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setImported(true);
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.SCEC);
        institutionBaseRequest.setTaxCode("taxCode-OK");
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);
        mockPersistOnboarding(asserter);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");

        when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(pdndBusinessResource));

        mockSimpleSearchPOSTAndPersist(asserter);

        Product product = mockSimpleProductValidAssert(request.getProductId(), false, asserter, false, true);
        product.setAllowedInstitutionTaxCode(List.of("taxCode-OK"));

        mockVerifyOnboardingNotFound();

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("S01G");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setIstatCode("istatCode");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCountryAbbreviation("IT");
        geographicTaxonomyResource.setProvinceAbbreviation("RM");
        geographicTaxonomyResource.setDesc("desc");
        asserter.execute(() -> when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(geographicTaxonomyResource)));

        mockAllowedProductByInstitutionTaxCodeList(asserter, true);
        mockVerifyAllowedProductList(request.getProductId(), asserter, false);

        asserter.assertThat(() -> onboardingService.onboarding(request, users, null, userRequesterDto), onboardingResponse -> {
            assertNotNull(onboardingResponse);
            assertEquals(WorkflowType.CONTRACT_REGISTRATION.name(), onboardingResponse.getWorkflowType());
            assertEquals("REQUEST", onboardingResponse.getStatus());
        });

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });

    }

    @Test
    @RunOnVertxContext
    void onboarding_whenIsNotAggregatorAndNotIo_test(UniAsserter asserter) {
        Onboarding onboarding = new Onboarding();

        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        List<UserRequest> users = List.of(manager);
        onboarding.setProductId(PROD_INTEROP.getValue());

        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setImported(true);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboarding.setInstitution(institutionBaseRequest);

        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(Onboarding.find(any())).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().item(createDummyOnboarding()));

        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setIstatCode("istatCode");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCountryAbbreviation("IT");
        geographicTaxonomyResource.setProvinceAbbreviation("RM");
        geographicTaxonomyResource.setDesc("desc");
        asserter.execute(() -> when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(geographicTaxonomyResource)));

        asserter.assertFailedWith(() -> onboardingService.onboardingImport(onboarding, users, contractImported, null), ResourceConflictException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenIsAggregatorAndNotIo_test(UniAsserter asserter) {
        Onboarding onboarding = new Onboarding();

        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        List<UserRequest> users = List.of(manager);
        onboarding.setProductId(PROD_INTEROP.getValue());
        onboarding.setIsAggregator(Boolean.TRUE);

        AggregateInstitutionRequest aggregateInstitution = new AggregateInstitutionRequest();
        aggregateInstitution.setDescription("test");
        List<AggregateInstitutionRequest> aggregateInstitutions = List.of(aggregateInstitution);


        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setImported(true);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboarding.setInstitution(institutionBaseRequest);

        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(Onboarding.find(any())).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding));

        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setIstatCode("istatCode");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCountryAbbreviation("IT");
        geographicTaxonomyResource.setProvinceAbbreviation("RM");
        geographicTaxonomyResource.setDesc("desc");
        asserter.execute(() -> when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(geographicTaxonomyResource)));

        asserter.assertFailedWith(() -> onboardingService.onboardingAggregationImport(onboarding, contractImported, users, aggregateInstitutions, null), ResourceConflictException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenIsAggregatorAndProductIo_test(UniAsserter asserter) {
        Onboarding onboarding = new Onboarding();

        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        AggregateInstitutionRequest aggregateInstitution = new AggregateInstitutionRequest();
        aggregateInstitution.setDescription("test");
        List<AggregateInstitutionRequest> aggregateInstitutions = List.of(aggregateInstitution);


        List<UserRequest> users = List.of(manager);
        onboarding.setProductId(PROD_IO.getValue());
        onboarding.setIsAggregator(Boolean.TRUE);
        onboarding.setWorkflowType(CONTRACT_REGISTRATION);

        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setImported(true);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboarding.setInstitution(institutionBaseRequest);

        Onboarding onboarding2 = new Onboarding();
        onboarding2.setWorkflowType(CONFIRMATION_AGGREGATE);

        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(Onboarding.find(any())).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding2));

        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setIstatCode("istatCode");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCountryAbbreviation("IT");
        geographicTaxonomyResource.setProvinceAbbreviation("RM");
        geographicTaxonomyResource.setDesc("desc");
        asserter.execute(() -> when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(geographicTaxonomyResource)));

        asserter.assertThat(() -> onboardingService.onboardingAggregationImport(onboarding, contractImported, users, aggregateInstitutions, null), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class, times(2)).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenIsAggregatorTest_throwsIncrementException(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");
        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding onboarding = new Onboarding();

        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        AggregateInstitutionRequest aggregateInstitution = new AggregateInstitutionRequest();
        aggregateInstitution.setDescription("test");
        List<AggregateInstitutionRequest> aggregateInstitutions = List.of(aggregateInstitution);

        List<UserRequest> users = List.of(manager);
        onboarding.setProductId(PROD_IO.getValue());
        onboarding.setIsAggregator(Boolean.TRUE);
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);

        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setImported(true);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboarding.setInstitution(institutionBaseRequest);
        onboarding.setUserRequester(userRequester);

        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);

        Onboarding onboarding2 = new Onboarding();
        onboarding2.setWorkflowType(IMPORT_AGGREGATION);

        User manager = new User();
        manager.setId("22323233");
        manager.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(manager));

        List<Onboarding> onboardingList = List.of(onboarding, onboarding2);

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(Onboarding.find(any())).thenReturn(query);
        when(Onboarding.find(any(Document.class), any(Document.class))).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().iterable(onboardingList));

        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setIstatCode("istatCode");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCountryAbbreviation("IT");
        geographicTaxonomyResource.setProvinceAbbreviation("RM");
        geographicTaxonomyResource.setDesc("desc");
        asserter.execute(() -> when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(geographicTaxonomyResource)));

        asserter.assertThat(() -> onboardingService.onboardingAggregationImport(
                onboarding, contractImported, users, aggregateInstitutions, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class, times(3)).find(any(Document.class));
            PanacheMock.verify(Onboarding.class, times(1)).find(any(Document.class), any(Document.class));
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });

    }


    @Test
    @RunOnVertxContext
    void onboarding_whenIsAggregatorAndProductIo(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding onboarding = new Onboarding();

        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        AggregateInstitutionRequest aggregateInstitution = new AggregateInstitutionRequest();
        aggregateInstitution.setDescription("test");
        List<AggregateInstitutionRequest> aggregateInstitutions = List.of(aggregateInstitution);


        List<UserRequest> users = List.of(manager);
        onboarding.setProductId(PROD_IO.getValue());
        onboarding.setIsAggregator(Boolean.TRUE);
        onboarding.setWorkflowType(CONTRACT_REGISTRATION);

        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setImported(true);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboarding.setInstitution(institutionBaseRequest);
        onboarding.setUserRequester(userRequester);

        Onboarding onboarding2 = new Onboarding();
        onboarding2.setProductId(PROD_IO.name());
        onboarding2.setWorkflowType(CONFIRMATION_AGGREGATE);
        onboarding2.setReferenceOnboardingId("referenceOnboardingId");

        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(Onboarding.find(any())).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding2));

        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setIstatCode("istatCode");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCountryAbbreviation("IT");
        geographicTaxonomyResource.setProvinceAbbreviation("RM");
        geographicTaxonomyResource.setDesc("desc");
        asserter.execute(() -> when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(geographicTaxonomyResource)));

        asserter.assertThat(() -> onboardingService.onboarding(onboarding, users, aggregateInstitutions, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class, times(1)).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenIsAggregatorAndProductPagoPa_test(UniAsserter asserter) {
        Onboarding onboarding = new Onboarding();

        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        AggregateInstitutionRequest aggregateInstitution = new AggregateInstitutionRequest();
        aggregateInstitution.setDescription("test");
        List<AggregateInstitutionRequest> aggregateInstitutions = List.of(aggregateInstitution);


        List<UserRequest> users = List.of(manager);
        onboarding.setProductId(PROD_PAGOPA.getValue());
        onboarding.setIsAggregator(Boolean.TRUE);
        onboarding.setWorkflowType(CONTRACT_REGISTRATION);

        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.IPA);
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setImported(true);
        institutionBaseRequest.setDescription(DESCRIPTION_FIELD);
        institutionBaseRequest.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        onboarding.setInstitution(institutionBaseRequest);

        Onboarding onboarding2 = new Onboarding();
        onboarding2.setWorkflowType(CONFIRMATION_AGGREGATE);

        OnboardingImportContract contractImported = new OnboardingImportContract();
        contractImported.setFileName("filename");
        contractImported.setFilePath("filepath");
        contractImported.setCreatedAt(LocalDateTime.now());
        contractImported.setContractType("type");

        mockPersistOnboarding(asserter);
        mockPersistToken(asserter);

        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);

        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(Onboarding.find(any())).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().item(onboarding2));

        mockVerifyAllowedProductList(onboarding.getProductId(), asserter, true);

        asserter.execute(() -> when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.noContent().build())));

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIPA");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setCategory("L37");
        institutionResource.setDescription(DESCRIPTION_FIELD);
        institutionResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institutionResource.setIstatCode("istatCode");
        asserter.execute(() -> when(institutionRegistryProxyApi.findInstitutionUsingGET(institutionBaseRequest.getTaxCode(), null, null))
                .thenReturn(Uni.createFrom().item(institutionResource)));

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCountryAbbreviation("IT");
        geographicTaxonomyResource.setProvinceAbbreviation("RM");
        geographicTaxonomyResource.setDesc("desc");
        asserter.execute(() -> when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
                .thenReturn(Uni.createFrom().item(geographicTaxonomyResource)));

        asserter.assertFailedWith(() -> onboardingService.onboardingAggregationImport(onboarding, contractImported, users, aggregateInstitutions, null), ResourceConflictException.class);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void uploadContractSigned_shouldSuccessfullyUploadContractAndUpdateOnboarding(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        String onboardingId = onboarding.getId();
        final String filepath = "upload-file-path";

        // Mock di Onboarding - DEVE RIMANERE ATTIVO PER TUTTA LA DURATA
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));
        when(Onboarding.findById(any()))
                .thenReturn(Uni.createFrom().item(onboarding));

        mockFindToken(asserter);
        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);
        mockUpdateToken(asserter, onboardingId);
        mockUpdateOnboarding(onboardingId, 1L);

        asserter.assertThat(() -> onboardingService.uploadContractSigned(onboardingId, TEST_FORM_ITEM),
                result -> {
                    Assertions.assertNotNull(result);
                    Assertions.assertEquals(onboarding.getId(), result.getId());
                    Assertions.assertNotNull(result.getUpdatedAt());
                });
    }

    @Test
    @RunOnVertxContext
    void uploadContractSigned(UniAsserter asserter) {

        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.getInstitution().setInstitutionType(PA);
        String onboardingId = onboarding.getId();
        final String filepath = "upload-file-path";

        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding)));
        when(Onboarding.findById(any()))
                .thenReturn(Uni.createFrom().item(onboarding));

        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);

        PanacheMock.mock(Token.class);
        when(Token.list(eq("onboardingId"), eq(onboardingId)))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        mockSimpleProductValidAssert(onboarding.getProductId(), false, asserter, false, true);

        Token mockToken = mock(Token.class);

        when(tokenService.retrieveToken(eq(onboarding), any(FormItem.class), any(Product.class)))
                .thenReturn(Uni.createFrom().item(mockToken));

        when(mockToken.persist()).thenReturn(Uni.createFrom().item(mockToken));

        doNothing().when(mockToken).setContractSigned(anyString());
        doNothing().when(mockToken).setContractFilename(anyString());
        doNothing().when(mockToken).setChecksum(anyString());

        when(tokenService.getAndVerifyDigest(any(), any(ContractTemplate.class), anyBoolean()))
                .thenReturn("digest_mock");
        when(tokenService.getContractPathByOnboarding(any(), any()))
                .thenReturn("path/to/contract");

        mockUpdateToken(asserter, onboardingId);
        mockUpdateOnboarding(onboardingId, 1L);

        asserter.assertThat(() -> onboardingService.uploadContractSigned(onboardingId, TEST_FORM_ITEM),
                result -> {
                    Assertions.assertNotNull(result);
                    Assertions.assertEquals(onboarding.getId(), result.getId());

                    // Usa la stessa sintassi sicura anche nel verify
                    verify(tokenService).retrieveToken(eq(onboarding), any(FormItem.class), any(Product.class));
                });
    }


    @Test
    @RunOnVertxContext
    void uploadContractSigned_whenOnboardingNotFound_shouldThrowInvalidRequestException(UniAsserter asserter) {
        String onboardingId = "non-existent-id";

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(onboardingId))
                .thenReturn(Uni.createFrom().item(Optional.empty())));

        asserter.assertFailedWith(() -> onboardingService.uploadContractSigned(onboardingId, TEST_FORM_ITEM),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void uploadContractSigned_whenOnboardingNotCompleted_shouldThrowInvalidRequestException(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.PENDING);
        String onboardingId = onboarding.getId();

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(onboardingId))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        asserter.assertFailedWith(() -> onboardingService.uploadContractSigned(onboardingId, TEST_FORM_ITEM),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void uploadContractSigned_whenUploadFileToAzureFails_shouldPropagateException(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        String onboardingId = onboarding.getId();

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(onboardingId))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter);

        when(azureBlobClient.uploadFile(any(), any(), any()))
                .thenThrow(new RuntimeException("Azure upload failed"));

        asserter.assertFailedWith(() -> onboardingService.uploadContractSigned(onboardingId, TEST_FORM_ITEM),
                RuntimeException.class);
    }

    @Test
    @RunOnVertxContext
    void uploadContractSigned_whenUpdateOnboardingFails_shouldPropagateException(UniAsserter asserter) {
        Onboarding onboarding = createDummyOnboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        String onboardingId = onboarding.getId();
        final String filepath = "upload-file-path";

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() -> when(Onboarding.findByIdOptional(onboardingId))
                .thenReturn(Uni.createFrom().item(Optional.of(onboarding))));

        mockFindToken(asserter);

        when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(filepath);

        mockUpdateToken(asserter, filepath);

        // Mock update onboarding to fail
        asserter.execute(() -> {
            ReactivePanacheUpdate query = mock(ReactivePanacheUpdate.class);
            when(Onboarding.update(any(Document.class))).thenReturn(query);
            when(query.where("_id", onboardingId))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("DB update failed")));
        });

        asserter.assertFailedWith(() -> onboardingService.uploadContractSigned(onboardingId, TEST_FORM_ITEM),
                RuntimeException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenValidIndividualCFWithAllowIndividualOnboarding_shouldSucceed(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();
        String individualCF = "RSSMRA80A01H501T";
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institution = dummyInstitution();
        institution.setOrigin(Origin.IVASS);
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setTaxCode(individualCF);
        onboardingRequest.setInstitution(institution);
        onboardingRequest.setUserRequester(userRequester);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);

        Product product = createDummyProduct("productId", false, true, true);
        asserter.execute(() -> {
            when(productService.getProductIsValid("productId"))
                    .thenReturn(product);
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });
        
        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);
        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));

        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenValidIndividualCFWithDisallowIndividualOnboarding_shouldThrowInvalidRequestException(UniAsserter asserter) {
        String individualCF = "RSSMRA80A01H501T";
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institution = dummyInstitution();
        institution.setOrigin(Origin.IVASS);
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setTaxCode(individualCF);
        institution.setOriginId("originId");
        onboardingRequest.setInstitution(institution);

        mockVerifyOnboardingNotFound();
        mockSimpleSearchPOSTAndPersist(asserter);

        Product product = createDummyProduct("productId", false, false, true);
        asserter.execute(() -> when(productService.getProductIsValid("productId"))
                .thenReturn(product));

        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);
        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null, null),
                InvalidRequestException.class);

    }

    @Test
    @RunOnVertxContext
    void onboarding_whenCompanyTaxCodeWithAllowCompanyOnboarding_shouldSucceed(UniAsserter asserter) {
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        String companyTaxCode = "12345678901";
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institution = dummyInstitution();
        institution.setOrigin(Origin.IVASS);
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setTaxCode(companyTaxCode);
        onboardingRequest.setInstitution(institution);
        onboardingRequest.setUserRequester(userRequester);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);

        // Create product that allows company onboarding
        Product product = createDummyProduct("productId", false, true, true);
        asserter.execute(() -> when(productService.getProductIsValid("productId"))
                .thenReturn(product));

        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);
        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));

        asserter.execute(() -> when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(managerResourceWk)));

        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users, null, userRequesterDto), Assertions::assertNotNull);

        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenCompanyTaxCodeWithDisallowCompanyOnboarding_shouldThrowInvalidRequestException(UniAsserter asserter) {
        String companyTaxCode = "12345678901";
        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager);
        onboardingRequest.setProductId("productId");
        Institution institution = dummyInstitution();
        institution.setOrigin(Origin.IVASS);
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setTaxCode(companyTaxCode);
        onboardingRequest.setInstitution(institution);

        mockVerifyOnboardingNotFound();
        mockSimpleSearchPOSTAndPersist(asserter);

        // Create product that disallows company onboarding
        Product product = createDummyProduct("productId", false, true, false);
        asserter.execute(() -> when(productService.getProductIsValid("productId"))
                .thenReturn(product));

        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);
        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null, null),
                InvalidRequestException.class);
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenAllowManagerAsDelegateWithSameTaxCodeAndSameEmail_shouldSucceed(UniAsserter asserter) {
        String companyTaxCode = "12345678901";

        final UserRequest delegate = UserRequest.builder()
                .name("name_delegate_1")
                .surname("surname_delegate_2")
                .taxCode("taxCode")
                .role(PartyRole.DELEGATE)
                .email("email_manager")
                .build();

        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager, delegate);
        onboardingRequest.setProductId("productId");
        Institution institution = dummyInstitution();
        institution.setOrigin(Origin.SELC);
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setTaxCode(companyTaxCode);
        onboardingRequest.setInstitution(institution);
        onboardingRequest.setUserRequester(userRequester);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);

        // Create a product that allows company onboarding
        Product product = createDummyProduct("productId", false, true, true);
        asserter.execute(() -> when(productService.getProductIsValid("productId"))
                .thenReturn(product));

        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);
        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
        });

        asserter.assertThat(() -> onboardingService.onboarding(onboardingRequest, users, null, userRequesterDto), Assertions::assertNotNull);
    }

    @Test
    @RunOnVertxContext
    void onboarding_whenAllowManagerAsDelegateWithSameTaxCodeAndDifferentEmail_shouldThrowInvalidRequestException(UniAsserter asserter) {
        String companyTaxCode = "12345678901";

        final UserRequest delegate = UserRequest.builder()
                .name("name_delegate_1")
                .surname("surname_delegate_2")
                .taxCode("taxCode")
                .role(PartyRole.DELEGATE)
                .email("email_delegate")
                .build();

        Onboarding onboardingRequest = new Onboarding();
        List<UserRequest> users = List.of(manager, delegate);
        onboardingRequest.setProductId("productId");
        Institution institution = dummyInstitution();
        institution.setOrigin(Origin.SELC);
        institution.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        institution.setDescription(DESCRIPTION_FIELD);
        institution.setTaxCode(companyTaxCode);
        onboardingRequest.setInstitution(institution);

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);

        // Create a product that allows company onboarding
        Product product = createDummyProduct("productId", false, true, true);
        asserter.execute(() -> when(productService.getProductIsValid("productId"))
                .thenReturn(product));

        mockVerifyOnboardingNotFound();
        mockVerifyAllowedProductList(onboardingRequest.getProductId(), asserter, true);
        InsuranceCompanyResource insuranceCompanyResource = new InsuranceCompanyResource();
        insuranceCompanyResource.setDescription(DESCRIPTION_FIELD);
        insuranceCompanyResource.setDigitalAddress(DIGITAL_ADDRESS_FIELD);
        when(insuranceCompaniesApi.searchByTaxCodeUsingGET(any())).thenReturn(Uni.createFrom().item(insuranceCompanyResource));

        asserter.assertFailedWith(() -> onboardingService.onboarding(onboardingRequest, users, null, null), InvalidRequestException.class);
    }

}
