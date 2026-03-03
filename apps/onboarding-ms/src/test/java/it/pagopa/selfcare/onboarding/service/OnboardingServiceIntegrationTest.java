package it.pagopa.selfcare.onboarding.service;

import static it.pagopa.selfcare.onboarding.common.InstitutionType.PSP;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_DASHBOARD_PSP;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.controller.request.UserRequesterDto;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.entity.UserRequester;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapperImpl;
import it.pagopa.selfcare.onboarding.service.impl.OnboardingServiceDefault;
import it.pagopa.selfcare.onboarding.service.strategy.OnboardingValidationStrategy;
import it.pagopa.selfcare.onboarding.steps.IntegrationProfile;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

@Slf4j
@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
@TestProfile(IntegrationProfile.class)
class OnboardingServiceIntegrationTest {

    @Inject
    OnboardingServiceDefault onboardingService;

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

    @InjectMock
    OnboardingValidationStrategy onboardingValidationStrategy;

    @Spy
    OnboardingMapper onboardingMapper = new OnboardingMapperImpl();

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
        mockVerifyAllowedProductList(request.getProductId(), asserter);

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

        asserter.execute(() -> when(orchestrationService.triggerOrchestration(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse())));
    }

    private void mockSimpleProductValidAssert(String productId, UniAsserter asserter) {
        Product productResource = createDummyProduct(productId);
        asserter.execute(() -> when(productService.getProductIsValid(productId))
                .thenReturn(productResource));
    }

    ProductRoleInfo dummyProductRoleInfo(String productRolCode) {
        ProductRole productRole = new ProductRole();
        productRole.setCode(productRolCode);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setRoles(List.of(productRole));
        productRoleInfo.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.ONBOARDING.value));
        return productRoleInfo;
    }

    Product createDummyProduct(String productId) {

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
        productResource.setAllowCompanyOnboarding(true);
        if (PROD_DASHBOARD_PSP.getValue().equals(productId)) {
            List<String> institutionTypeList = new ArrayList<>();
            institutionTypeList.add(PSP.name());
            productResource.setInstitutionTypesAllowed(institutionTypeList);
        }
        return productResource;
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
        when(Onboarding.findById(onboarding.getId()))
                .thenReturn(Uni.createFrom().item(onboarding));

        mockUpdateOnboarding(onboarding.getId(), 1L);
        UniAssertSubscriber<Long> subscriber = onboardingService
                .rejectOnboarding(onboarding.getId(), "string")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(1L);
    }

    private void mockUpdateOnboarding(String onboardingId, Long updatedItemCount) {
        ReactivePanacheUpdate query = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.update(any(Document.class))).thenReturn(query);
        when(query.where("_id", onboardingId)).thenReturn(Uni.createFrom().item(updatedItemCount));
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

    void mockVerifyAllowedProductList(String productId, UniAsserter asserter) {
        asserter.execute(() -> when(onboardingValidationStrategy.validate(productId)).thenReturn(true));
    }

}
