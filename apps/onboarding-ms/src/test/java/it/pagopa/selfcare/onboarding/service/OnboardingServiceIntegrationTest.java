package it.pagopa.selfcare.onboarding.service;

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
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.ReasonRequest;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.controller.request.UserRequesterDto;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.entity.UserRequester;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapperImpl;
import it.pagopa.selfcare.onboarding.service.impl.OnboardingServiceDefault;
import it.pagopa.selfcare.onboarding.steps.IntegrationProfile;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;
import org.openapi.quarkus.product_json.model.BackOfficeRole;
import org.openapi.quarkus.product_json.model.Features;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RoleMapping;
import org.openapi.quarkus.product_json.model.WorkflowType;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.*;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
@TestProfile(IntegrationProfile.class)
class OnboardingServiceIntegrationTest {

    @Inject
    OnboardingServiceDefault onboardingService;

    @InjectMock
    TenantContext tenantContext;

    @InjectMock
    @RestClient
    UserApi userRegistryApi;

    @InjectMock
    ProductService productService;

    @InjectMock
    @RestClient
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;

    @InjectMock
    @RestClient
    InfocamerePdndApi infocamerePdndApi;

    @InjectMock
    OrchestrationService orchestrationService;

    @InjectMock
    UserService userService;

    @InjectMock
    InstitutionService institutionService;


    @Spy
    OnboardingMapper onboardingMapper = new OnboardingMapperImpl();

    @BeforeEach
    void setupTenantContext() {
        when(tenantContext.requiredTenantId()).thenReturn("AR");
        when(tenantContext.getTenantId()).thenReturn("AR");
        when(tenantContext.isInitialized()).thenReturn(true);
    }

    static final UserRequest manager = UserRequest.builder()
            .name("name")
            .surname("surname")
            .taxCode("taxCode")
            .role(PartyRole.MANAGER)
            .build();

    static final UserRequest delegate1 = UserRequest.builder()
            .name("name_delegate_1")
            .surname("surname_delegate_2")
            .taxCode("taxCode_delegate_3")
            .role(PartyRole.DELEGATE)
            .build();

    static final UserResource managerResource;
    static final UserResource managerResourceWk;
    static final UserResource managerResourceWkSpid;
    static final String PRODUCT_ROLE_ADMIN_CODE = "admin";
    static final String PRODUCT_ROLE_ADMIN_PSP_CODE = "admin-psp";

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

    @BeforeEach
    void setupDefaultMocks() {
        WorkflowTypeResponse defaultResponse =
                new WorkflowTypeResponse().workflowType(WorkflowType.CONTRACT_REGISTRATION);
        when(productService.getWorkflowType(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(defaultResponse));
        when(productService.getWorkflowType(any(), any(), any(), nullable(String.class)))
                .thenReturn(Uni.createFrom().item(defaultResponse));
        when(productService.getProductExpirationDays(anyString())).thenReturn(Uni.createFrom().item(30));
        when(productService.getProductExpirationDays(anyString(), nullable(String.class)))
                .thenReturn(Uni.createFrom().item(30));
    }

    @Test
    @RunOnVertxContext
    void onboarding_PRV(UniAsserter asserter) {
        // Given
        UserRequesterDto userRequesterDto = new UserRequesterDto();
        userRequesterDto.setName("name");
        userRequesterDto.setSurname("surname");
        userRequesterDto.setEmail("test@test.com");

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .build();

        Institution institutionBaseRequest = new Institution();
        institutionBaseRequest.setOrigin(Origin.PDND_INFOCAMERE);
        institutionBaseRequest.setDescription("name");
        institutionBaseRequest.setDigitalAddress("pec");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        institutionBaseRequest.setTaxCode("taxCode");

        Onboarding request = new Onboarding();
        request.setProductId(PROD_INTEROP.getValue());
        request.setInstitution(institutionBaseRequest);
        request.setUserRequester(userRequester);

        List<UserRequest> users = List.of(manager);

        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessName("name");
        pdndBusinessResource.setDigitalAddress("pec");

        mockPersistOnboarding(asserter);
        mockSimpleSearchPOSTAndPersist(asserter);
        mockSimpleProductValidAssert(request.getProductId(), asserter);
        mockVerifyOnboardingNotFound();

        asserter.execute(() -> {
            when(userRegistryApi.updateUsingPATCH(any(), any()))
                    .thenReturn(Uni.createFrom().item(Response.noContent().build()));
            when(userRegistryApi.findByIdUsingGET(any(), any()))
                    .thenReturn(Uni.createFrom().item(managerResourceWk));
            when(infocamerePdndApi.institutionPdndByTaxCodeUsingGET(any()))
                    .thenReturn(Uni.createFrom().item(pdndBusinessResource));
        });

        // When
        asserter.assertThat(
                () -> onboardingService.onboarding(request, users, null, userRequesterDto),
                Assertions::assertNotNull
        );

        // Then
        asserter.execute(() -> {
            PanacheMock.verify(Onboarding.class).persist(any(Onboarding.class), any());
            PanacheMock.verify(Onboarding.class).persistOrUpdate(any(List.class));
            PanacheMock.verify(Onboarding.class).find(any(Document.class));
            PanacheMock.verifyNoMoreInteractions(Onboarding.class);
        });
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

        asserter.execute(() -> when(orchestrationService.triggerOrchestrationIfEnabled(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));
    }

    private void mockSimpleProductValidAssert(String productId, UniAsserter asserter) {
        ProductResponse product = createDummyProduct(productId);
        asserter.execute(() -> {
            when(productService.getValidProduct(productId)).thenReturn(Uni.createFrom().item(product));
            when(productService.getValidProduct(eq(productId), nullable(String.class)))
                    .thenReturn(Uni.createFrom().item(product));
        });
    }

    private RoleMapping roleMapping(PartyRole role, String backOfficeRole,
                                    org.openapi.quarkus.product_json.model.InstitutionType institutionType) {
        return new RoleMapping().role(role.name()).institutionType(institutionType)
                .phasesAdditionAllowed(List.of("onboarding"))
                .backOfficeRoles(List.of(new BackOfficeRole().code(backOfficeRole)));
    }

    private ProductResponse createDummyProduct(String productId) {
        return new ProductResponse().productId(productId).tenantId("AR").title("title")
                .features(new Features().enabled(true).allowIndividualOnboarding(false).allowCompanyOnboarding(true))
                .roleMappings(List.of(
                        roleMapping(manager.getRole(), PRODUCT_ROLE_ADMIN_CODE, null),
                        roleMapping(delegate1.getRole(), PRODUCT_ROLE_ADMIN_CODE, null),
                        roleMapping(manager.getRole(), PRODUCT_ROLE_ADMIN_PSP_CODE,
                                org.openapi.quarkus.product_json.model.InstitutionType.PSP)));
    }


    void mockVerifyOnboardingNotFound() {
        PanacheMock.mock(Onboarding.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().empty());
        when(Onboarding.find(any())).thenReturn(query);
    }

    private Onboarding createDummyOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(UUID.randomUUID().toString());
        onboarding.setTenantId("AR");
        onboarding.setProductId("prod-id");

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
        ReasonRequest reasonRequest = new ReasonRequest();
        reasonRequest.setUserUid("uuid");
        reasonRequest.setReasonForReject("reason");
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        mockUpdateOnboarding(onboarding.getId(), 1L);
        UniAssertSubscriber<Long> subscriber = onboardingService
                .rejectOnboarding(onboarding.getId(), reasonRequest)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(1L);
    }

    private void mockUpdateOnboarding(String onboardingId, Long updatedItemCount) {
        ReactivePanacheUpdate query = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.update(any(Document.class))).thenReturn(query);
        when(query.where("tenantId = ?1 and _id = ?2", "AR", onboardingId))
                .thenReturn(Uni.createFrom().item(updatedItemCount));
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

}
