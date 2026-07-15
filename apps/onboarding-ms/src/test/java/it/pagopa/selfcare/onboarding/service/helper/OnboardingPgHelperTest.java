package it.pagopa.selfcare.onboarding.service.helper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.OrchestrationService;
import it.pagopa.selfcare.onboarding.service.UserService;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessResource;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessesResource;
import org.openapi.quarkus.party_registry_proxy_json.model.LegalVerificationResult;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

@QuarkusTest
class OnboardingPgHelperTest {

    @Inject
    OnboardingPgHelper onboardingPgHelper;

    @InjectMock
    it.pagopa.selfcare.product.service.ProductService productAzureService;

    @InjectMock
    OnboardingPersistenceHelper persistenceHelper;

    @InjectMock
    OnboardingValidationHelper validationHelper;

    @InjectMock
    UserRegistryHelper userRegistryHelper;

    @InjectMock
    OrchestrationService orchestrationService;

    @InjectMock
    OnboardingMapper onboardingMapper;

    @InjectMock
    UserService userService;

    @InjectMock
    @RestClient
    InfocamereApi infocamereApi;

    @InjectMock
    @RestClient
    NationalRegistriesApi nationalRegistriesApi;

    // --- onboardingUserPg: checkOnboardingPgUserList ---

    @Test
    void onboardingUserPg_whenUserListEmpty_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> onboardingPgHelper.onboardingUserPg(buildOnboarding(), List.of()));
    }

    @Test
    void onboardingUserPg_whenUserListHasMoreThanOneUser_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> onboardingPgHelper.onboardingUserPg(buildOnboarding(),
                        List.of(managerUserRequest(), managerUserRequest())));
    }

    @Test
    void onboardingUserPg_whenSingleUserIsNotManager_throwsInvalidRequestException() {
        UserRequest delegate = new UserRequest();
        delegate.setRole(PartyRole.DELEGATE);
        assertThrows(InvalidRequestException.class,
                () -> onboardingPgHelper.onboardingUserPg(buildOnboarding(), List.of(delegate)));
    }

    // --- onboardingUserPg: retrievePreviousCompletedOnboarding ---

    @Test
    void onboardingUserPg_whenNoPreviousCompletedOnboarding_throwsResourceNotFoundException() {
        //given
        when(persistenceHelper.getOnboardingByFilters(
                anyString(), isNull(), anyString(), isNull(), anyString()))
                .thenReturn(Multi.createFrom().empty());

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(buildOnboarding(), List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitFailure().assertFailedWith(ResourceNotFoundException.class);
    }

    // --- onboardingUserPg: getProductByOnboarding ---

    @Test
    void onboardingUserPg_whenProductNotFound_throwsOnboardingNotAllowedException() {
        //given
        when(persistenceHelper.getOnboardingByFilters(
                anyString(), isNull(), anyString(), isNull(), anyString()))
                .thenReturn(Multi.createFrom().item(buildPreviousOnboarding()));

        when(productAzureService.getProductIsValid(anyString()))
                .thenThrow(new ProductNotFoundException("Product prod-pn-pg not found"));

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(buildOnboarding(), List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitFailure()
                .assertFailedWith(OnboardingNotAllowedException.class, "02492030446");
    }

    @Test
    void onboardingUserPg_whenStorageError_propagatesOriginalException() {
        //given
        when(persistenceHelper.getOnboardingByFilters(
                anyString(), isNull(), anyString(), isNull(), anyString()))
                .thenReturn(Multi.createFrom().item(buildPreviousOnboarding()));

        when(productAzureService.getProductIsValid(anyString()))
                .thenThrow(new RuntimeException("Azure Blob Storage unreachable"));

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(buildOnboarding(), List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitFailure()
                .assertFailedWith(RuntimeException.class, "Azure Blob Storage unreachable");
    }

    // --- onboardingUserPg: checkIfUserIsAlreadyManager ---

    @Test
    void onboardingUserPg_whenUserIsAlreadyManager_throwsInvalidRequestException() {
        //given
        mockPreviousOnboarding();
        mockValidProduct();
        when(validationHelper.validationRole(any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(managerUserRequest())));
        User managerUser = buildManagerUser();
        when(userRegistryHelper.retrieveUserResources(any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(managerUser)));
        when(userService.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(new UserInstitutionResponse())));

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(buildOnboarding(), List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitFailure().assertFailedWith(InvalidRequestException.class, "already manager");
    }

    // --- onboardingUserPg: checkManagerOnAde ---

    @Test
    void onboardingUserPg_whenAdeVerificationFails_throwsInvalidRequestException() {
        //given
        mockPreviousOnboarding();
        mockValidProduct();
        mockValidationAndUserRegistry();
        mockUserNotAlreadyManager();
        LegalVerificationResult result = new LegalVerificationResult();
        result.setVerificationResult(false);
        when(nationalRegistriesApi.verifyLegalUsingGET(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(result));

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(buildOnboarding(), List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitFailure().assertFailedWith(InvalidRequestException.class,
                "User is not manager of the institution on the registry");
    }

    @Test
    void onboardingUserPg_whenAdeReturnsWebApplicationException400_throwsInvalidRequestException() {
        //given
        mockPreviousOnboarding();
        mockValidProduct();
        mockValidationAndUserRegistry();
        mockUserNotAlreadyManager();
        WebApplicationException ex = webAppException(400);
        when(nationalRegistriesApi.verifyLegalUsingGET(anyString(), anyString()))
                .thenReturn(Uni.createFrom().failure(ex));

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(buildOnboarding(), List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitFailure().assertFailedWith(InvalidRequestException.class,
                "User is not manager of the institution on the registry");
    }

    @Test
    void onboardingUserPg_whenAdeReturnsWebApplicationExceptionNot400_propagatesException() {
        //given
        mockPreviousOnboarding();
        mockValidProduct();
        mockValidationAndUserRegistry();
        mockUserNotAlreadyManager();
        WebApplicationException ex = webAppException(500);
        when(nationalRegistriesApi.verifyLegalUsingGET(anyString(), anyString()))
                .thenReturn(Uni.createFrom().failure(ex));

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(buildOnboarding(), List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitFailure().assertFailedWith(WebApplicationException.class);
    }

    // --- onboardingUserPg: checkManagerOnInfocamere ---

    @Test
    void onboardingUserPg_whenInfocamereBusinessNotFound_throwsInvalidRequestException() {
        //given
        Onboarding onboarding = buildOnboarding(Origin.INFOCAMERE);
        mockPreviousOnboarding(Origin.INFOCAMERE);
        mockValidProduct();
        mockValidationAndUserRegistry();
        mockUserNotAlreadyManager();
        BusinessesResource businesses = new BusinessesResource();
        businesses.setBusinesses(List.of());
        when(infocamereApi.institutionsByLegalTaxIdUsingPOST(any()))
                .thenReturn(Uni.createFrom().item(businesses));

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(onboarding, List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitFailure().assertFailedWith(InvalidRequestException.class,
                "User is not manager of the institution on the registry");
    }

    @Test
    void onboardingUserPg_whenInfocamereBusinessesNull_throwsInvalidRequestException() {
        //given
        Onboarding onboarding = buildOnboarding(Origin.INFOCAMERE);
        mockPreviousOnboarding(Origin.INFOCAMERE);
        mockValidProduct();
        mockValidationAndUserRegistry();
        mockUserNotAlreadyManager();
        when(infocamereApi.institutionsByLegalTaxIdUsingPOST(any()))
                .thenReturn(Uni.createFrom().item(new BusinessesResource()));

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(onboarding, List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitFailure().assertFailedWith(InvalidRequestException.class,
                "User is not manager of the institution on the registry");
    }

    // --- onboardingUserPg: happy path ---

    @Test
    void onboardingUserPg_withAdeOrigin_returnsOnboardingResponse() {
        //given
        mockPreviousOnboarding();
        mockValidProduct();
        mockValidationAndUserRegistry();
        mockUserNotAlreadyManager();
        LegalVerificationResult result = new LegalVerificationResult();
        result.setVerificationResult(true);
        when(nationalRegistriesApi.verifyLegalUsingGET(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(result));
        when(orchestrationService.triggerOrchestrationIfEnabled(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));
        Onboarding saved = buildOnboarding();
        when(persistenceHelper.updateOnboarding(any()))
          .thenReturn(Uni.createFrom().item(saved));
        OnboardingResponse expected = new OnboardingResponse();
        when(onboardingMapper.toResponse(any())).thenReturn(expected);

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(buildOnboarding(), List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitItem().assertItem(expected);
    }

    @Test
    void onboardingUserPg_withInfocamereOrigin_returnsOnboardingResponse() {
        //given
        Onboarding onboarding = buildOnboarding(Origin.INFOCAMERE);
        mockPreviousOnboarding(Origin.INFOCAMERE);
        mockValidProduct();
        mockValidationAndUserRegistry();
        mockUserNotAlreadyManager();
        BusinessResource business = new BusinessResource();
        business.setBusinessTaxId(onboarding.getInstitution().getTaxCode());
        BusinessesResource businesses = new BusinessesResource();
        businesses.setBusinesses(List.of(business));
        when(infocamereApi.institutionsByLegalTaxIdUsingPOST(any()))
                .thenReturn(Uni.createFrom().item(businesses));
        when(orchestrationService.triggerOrchestrationIfEnabled(any(), any()))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));
        when(persistenceHelper.updateOnboarding(any()))
                .thenReturn(Uni.createFrom().item(onboarding));
        OnboardingResponse expected = new OnboardingResponse();
        when(onboardingMapper.toResponse(any())).thenReturn(expected);

        //when
        UniAssertSubscriber<OnboardingResponse> subscriber = onboardingPgHelper
                .onboardingUserPg(onboarding, List.of(managerUserRequest()))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitItem().assertItem(expected);
    }

    // --- isUserActiveManager ---

    @Test
    void isUserActiveManager_whenUserInstitutionsNonEmpty_returnsTrue() {
        //given
        when(userService.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(new UserInstitutionResponse())));

        //when
        UniAssertSubscriber<Boolean> subscriber = onboardingPgHelper
                .isUserActiveManager("inst-id", "prod-pn-pg", "user-id")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitItem().assertItem(true);
    }

    @Test
    void isUserActiveManager_whenUserInstitutionsEmpty_returnsFalse() {
        //given
        when(userService.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(List.of()));

        //when
        UniAssertSubscriber<Boolean> subscriber = onboardingPgHelper
                .isUserActiveManager("inst-id", "prod-pn-pg", "user-id")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitItem().assertItem(false);
    }

    @Test
    void isUserActiveManager_whenProductIdIsNull_passesNullProductList() {
        //given
        when(userService.retrieveUserInstitutions(any(), any(), isNull(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(List.of()));

        //when
        UniAssertSubscriber<Boolean> subscriber = onboardingPgHelper
                .isUserActiveManager("inst-id", null, "user-id")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitItem().assertItem(false);
    }

    @Test
    void isUserActiveManager_whenUserServiceFails_propagatesFailure() {
        //given
        when(userService.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("user-ms unreachable")));

        //when
        UniAssertSubscriber<Boolean> subscriber = onboardingPgHelper
                .isUserActiveManager("inst-id", "prod-pn-pg", "user-id")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitFailure().assertFailedWith(RuntimeException.class, "user-ms unreachable");
    }

    // --- Fixtures ---

    private static Onboarding buildOnboarding() {
        return buildOnboarding(Origin.ADE);
    }

    private static Onboarding buildOnboarding(Origin origin) {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboarding-id");
        onboarding.setProductId("prod-pn-pg");
        onboarding.setInstitution(buildInstitution(origin));
        return onboarding;
    }

    private static Onboarding buildPreviousOnboarding() {
        return buildPreviousOnboarding(Origin.ADE);
    }

    private static Onboarding buildPreviousOnboarding(Origin origin) {
        Onboarding previous = buildOnboarding(origin);
        previous.setId("prev-onboarding-id");
        previous.setReferenceOnboardingId(null);
        return previous;
    }

    private static Institution buildInstitution(Origin origin) {
        Institution institution = new Institution();
        institution.setId("inst-id");
        institution.setTaxCode("02492030446");
        institution.setOrigin(origin);
        institution.setInstitutionType(InstitutionType.PG);
        return institution;
    }

    private static UserRequest managerUserRequest() {
        UserRequest req = new UserRequest();
        req.setRole(PartyRole.MANAGER);
        req.setTaxCode("MGRXXX00A00X000X");
        return req;
    }

    private static User buildManagerUser() {
        User user = new User();
        user.setId("manager-user-id");
        user.setRole(PartyRole.MANAGER);
        return user;
    }

    private static WebApplicationException webAppException(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        WebApplicationException ex = mock(WebApplicationException.class);
        when(ex.getResponse()).thenReturn(response);
        return ex;
    }

    private void mockPreviousOnboarding() {
        mockPreviousOnboarding(Origin.ADE);
    }

    private void mockPreviousOnboarding(Origin origin) {
        when(persistenceHelper.getOnboardingByFilters(
                anyString(), isNull(), anyString(), isNull(), anyString()))
                .thenReturn(Multi.createFrom().item(buildPreviousOnboarding(origin)));
    }

    private void mockValidProduct() {
        Product product = new Product();
        Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
        roleMappings.put(PartyRole.MANAGER, new ProductRoleInfo());
        product.setRoleMappings(roleMappings);
        when(productAzureService.getProductIsValid(anyString())).thenReturn(product);
    }

    private void mockValidationAndUserRegistry() {
        when(validationHelper.validationRole(any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(managerUserRequest())));
        when(userRegistryHelper.retrieveUserResources(any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(buildManagerUser())));
    }

    private void mockUserNotAlreadyManager() {
        when(userService.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(List.of()));
    }
}

