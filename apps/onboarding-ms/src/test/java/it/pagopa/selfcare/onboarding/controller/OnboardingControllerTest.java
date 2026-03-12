package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.CheckManagerResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingAggregationImportRequest;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.model.RecipientCodeStatus;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(OnboardingController.class)
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class OnboardingControllerTest {

    static final OnboardingPspRequest onboardingPspValid;
    static final UserRequest userDTO;
    static final OnboardingPgRequest onboardingPgValid;
    static final OnboardingDefaultRequest onboardingBaseValid;
    static final InstitutionBaseRequest institution;
    static final InstitutionPspRequest institutionPsp;
    static final OnboardingUserPgRequest onboardingUserPgValid;

    @InjectMock
    OnboardingService onboardingService;

    static {
        onboardingBaseValid = new OnboardingDefaultRequest();
        onboardingBaseValid.setProductId("productId");

        userDTO = new UserRequest();
        userDTO.setTaxCode("taxCode");

        BillingRequest billingRequest = new BillingRequest();
        billingRequest.setVatNumber("vatNumber");

        onboardingBaseValid.setUsers(List.of(userDTO));
        onboardingBaseValid.setBilling(billingRequest);

        institution = new InstitutionBaseRequest();
        institution.setInstitutionType(InstitutionType.PT);
        institution.setTaxCode("taxCode");
        institution.setDigitalAddress("example@example.it");
        institution.setOriginId("originId");
        institution.setOrigin(Origin.IPA);
        onboardingBaseValid.setInstitution(institution);

        /* PSP */
        onboardingPspValid = new OnboardingPspRequest();
        onboardingPspValid.setProductId("productId");
        onboardingPspValid.setUsers(List.of(userDTO));

        institutionPsp = new InstitutionPspRequest();
        institutionPsp.setOrigin(Origin.SELC);
        institutionPsp.setOriginId("originId");
        institutionPsp.setInstitutionType(InstitutionType.PT);
        institutionPsp.setTaxCode("taxCode");
        institutionPsp.setDigitalAddress("example@example.it");
        institutionPsp.setPaymentServiceProvider(new PaymentServiceProviderRequest());
        onboardingPspValid.setInstitution(institutionPsp);

        /* PG */

        onboardingPgValid = new OnboardingPgRequest();
        onboardingPgValid.setTaxCode("code");
        onboardingPgValid.setProductId("productId");
        onboardingPgValid.setDigitalAddress("email@pagopa.it");
        onboardingPgValid.setUsers(List.of(userDTO));

        onboardingUserPgValid = new OnboardingUserPgRequest();
        onboardingUserPgValid.setTaxCode("code");
        onboardingUserPgValid.setProductId("productId");
        onboardingUserPgValid.setUsers(List.of(userDTO));
        onboardingUserPgValid.setOrigin(Origin.ADE);

    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboarding_shouldNotValidBody() {

        given()
                .when()
                .body(new OnboardingDefaultRequest())
                .contentType(ContentType.JSON)
                .post()
                .then()
                .statusCode(400);
    }

    @ParameterizedTest
    @TestSecurity(user = "userJwt")
    @ValueSource(strings = {"/psp", "/pa"})
    void onboarding_shouldNotValidPspBody(String path) {

        given()
                .when()
                .body(new OnboardingDefaultRequest())
                .contentType(ContentType.JSON)
                .post(path)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboarding() {

        Mockito.when(onboardingService.onboarding(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingBaseValid)
                .contentType(ContentType.JSON)
                .post()
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPa() {
        OnboardingPaRequest onboardingPaValid = dummyOnboardingPa();

        Mockito.when(onboardingService.onboarding(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPaValid)
                .contentType(ContentType.JSON)
                .post("/pa")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .onboarding(captor.capture(), any(), any(), any());
        assertEquals(captor.getValue().getBilling().getRecipientCode(), onboardingPaValid.getBilling().getRecipientCode().toUpperCase());

    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPaAggregator_withoutVatNumberAndParentDescription() {
        OnboardingPaRequest onboardingPaValid = dummyOnboardingPa();
        onboardingPaValid.setIsAggregator(Boolean.TRUE);
        List<AggregateInstitutionRequest> aggregateInstitutions = new ArrayList<>();
        AggregateInstitutionRequest aggregateInstitutionRequest = new AggregateInstitutionRequest();
        aggregateInstitutions.add(aggregateInstitutionRequest);
        onboardingPaValid.setAggregates(aggregateInstitutions);

        Mockito.when(onboardingService.onboarding(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPaValid)
                .contentType(ContentType.JSON)
                .post("/pa/aggregation")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .onboarding(captor.capture(), any(), any(), any());
        assertEquals(captor.getValue().getBilling().getRecipientCode(), onboardingPaValid.getBilling().getRecipientCode().toUpperCase());
        assertTrue(captor.getValue().getIsAggregator());
        assertNull(captor.getValue().getAggregates().get(0).getVatNumber());
        assertNull(captor.getValue().getAggregates().get(0).getParentDescription());
        assertFalse(captor.getValue().getAggregates().isEmpty());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPaAggregator_WithVatNumberAndParentDescription() {
        OnboardingPaRequest onboardingPaValid = dummyOnboardingPa();
        onboardingPaValid.setIsAggregator(Boolean.TRUE);
        List<AggregateInstitutionRequest> aggregateInstitutions = new ArrayList<>();
        AggregateInstitutionRequest aggregateInstitutionRequest = new AggregateInstitutionRequest();
        aggregateInstitutionRequest.setVatNumber("vatNumber");
        aggregateInstitutionRequest.setParentDescription("parentDescription");
        aggregateInstitutions.add(aggregateInstitutionRequest);
        onboardingPaValid.setAggregates(aggregateInstitutions);

        Mockito.when(onboardingService.onboarding(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPaValid)
                .contentType(ContentType.JSON)
                .post("/pa/aggregation")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .onboarding(captor.capture(), any(), any(), any());
        assertEquals(captor.getValue().getBilling().getRecipientCode(), onboardingPaValid.getBilling().getRecipientCode().toUpperCase());
        assertTrue(captor.getValue().getIsAggregator());
        assertNotNull(captor.getValue().getAggregates().get(0).getVatNumber());
        assertNotNull(captor.getValue().getAggregates().get(0).getParentDescription());
        assertFalse(captor.getValue().getAggregates().isEmpty());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingAggregationIncrement() {
        OnboardingPaRequest onboardingPaValid = dummyOnboardingPa();
        onboardingPaValid.setIsAggregator(Boolean.TRUE);
        List<AggregateInstitutionRequest> aggregateInstitutions = new ArrayList<>();
        AggregateInstitutionRequest aggregateInstitutionRequest = new AggregateInstitutionRequest();
        aggregateInstitutions.add(aggregateInstitutionRequest);
        onboardingPaValid.setAggregates(aggregateInstitutions);

        Mockito.when(onboardingService.onboardingIncrement(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPaValid)
                .contentType(ContentType.JSON)
                .post("/aggregation/increment")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .onboardingIncrement(captor.capture(), any(), any(), any());
        assertEquals(captor.getValue().getBilling().getRecipientCode(), onboardingPaValid.getBilling().getRecipientCode().toUpperCase());
        assertTrue(captor.getValue().getIsAggregator());
        assertFalse(captor.getValue().getAggregates().isEmpty());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingUsers() {
        OnboardingUserRequest onboardingUserRequest = dummyOnboardingUser();

        Mockito.when(onboardingService.onboardingUsers(any(), anyString(), eq(WorkflowType.USERS)))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingUserRequest)
                .contentType(ContentType.JSON)
                .post("/users")
                .then()
                .statusCode(204);
    }


    @Test
    @TestSecurity(user = "userJwt")
    void onboardingUsersAggregators() {
        OnboardingUserRequest onboardingUserRequest = dummyOnboardingUser();

        Mockito.when(onboardingService.onboardingUsers(any(), anyString(), eq(WorkflowType.USERS_EA)))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingUserRequest)
                .contentType(ContentType.JSON)
                .post("/users/aggregator")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingUsers_shouldNotValidBody() {

        given()
                .when()
                .body(new OnboardingUserRequest())
                .contentType(ContentType.JSON)
                .post("/users")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPsp() {

        Mockito.when(onboardingService.onboarding(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPspValid)
                .contentType(ContentType.JSON)
                .post("/psp")
                .then()
                .statusCode(200);
    }

    @Test
    void complete_unauthorized() {

        given()
                .when()
                .pathParam("tokenId", "actual-token-id")
                .contentType(ContentType.MULTIPART)
                .put("/{tokenId}/complete")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void complete() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.complete(any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{onboardingId}/complete")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .complete(expectedId.capture(), any());
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void completeWithManyFiles() {
        File testFile1 = new File("src/test/resources/application.properties");
        File testFile2 = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.complete(any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile1)
                .multiPart("contract", testFile2)
                .put("/{onboardingId}/complete")
                .then()
                .statusCode(400);
    }

    @Test
    void completeOnboardingUsers_unauthorized() {

        given()
                .when()
                .pathParam("tokenId", "actual-token-id")
                .contentType(ContentType.MULTIPART)
                .put("/{tokenId}/completeOnboardingUsers")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void completeOnboardingUsers() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.completeOnboardingUsers(any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{onboardingId}/completeOnboardingUsers")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .completeOnboardingUsers(expectedId.capture(), any());
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void consume() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.completeWithoutSignatureVerification(any(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{onboardingId}/consume")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .completeWithoutSignatureVerification(expectedId.capture(), any());
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteOK() {
        String onboardingId = "actual-onboarding-id";
        ReasonRequest reasonRequest = new ReasonRequest();
        reasonRequest.setReasonForReject("string");

        when(onboardingService.rejectOnboarding(onboardingId, "string"))
                .thenReturn(Uni.createFrom().item(1L));

        given()
                .when()
                .body(reasonRequest)
                .contentType(ContentType.JSON)
                .pathParam("onboardingId", onboardingId)
                .put("/{onboardingId}/reject")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .rejectOnboarding(expectedId.capture(), eq("string"));
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteInvalidOnboardingIdOrOnboardingNotFound() {
        String onboardingId = "actual-onboarding-id";
        ReasonRequest reasonRequest = new ReasonRequest();
        reasonRequest.setReasonForReject("string");

        when(onboardingService.rejectOnboarding(onboardingId, "string"))
                .thenThrow(InvalidRequestException.class);

        given()
                .when()
                .body(reasonRequest)
                .contentType(ContentType.JSON)
                .pathParam("onboardingId", onboardingId)
                .put("/{onboardingId}/reject")
                .then()
                .statusCode(400);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .rejectOnboarding(expectedId.capture(), eq("string"));
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboarding() {
        OnboardingGetResponse response = getOnboardingGetResponse();
        OnboardingGetFilters filters = OnboardingGetFilters.builder()
                .productId("prod-io")
                .taxCode("taxCode")
                .subunitCode("subunitCode")
                .from("2023-12-01")
                .to("2023-12-31")
                .status("ACTIVE")
                .build();
        when(onboardingService.onboardingGet(filters))
                .thenReturn(Uni.createFrom().item(response));

        Map<String, String> queryParameterMap = getStringStringMap();

        given()
                .when()
                .queryParams(queryParameterMap)
                .get()
                .then()
                .statusCode(204);

        verify(onboardingService, times(1))
                .onboardingGet((OnboardingGetFilters) any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboardingById() {
        OnboardingGet onboardingGet = dummyOnboardingGet();
        when(onboardingService.onboardingGet(onboardingGet.getId()))
                .thenReturn(Uni.createFrom().item(onboardingGet));

        given()
                .when()
                .get("/" + onboardingGet.getId())
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .onboardingGet(onboardingGet.getId());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboardingByIdWithUserInfo() {
        OnboardingGet onboardingGet = dummyOnboardingGet();
        when(onboardingService.onboardingGetWithUserInfo(onboardingGet.getId()))
                .thenReturn(Uni.createFrom().item(onboardingGet));

        given()
                .when()
                .get("/" + onboardingGet.getId() + "/withUserInfo")
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .onboardingGetWithUserInfo(onboardingGet.getId());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboardingPending() {
        OnboardingGet onboardingGet = dummyOnboardingGet();
        when(onboardingService.onboardingPending(onboardingGet.getId()))
                .thenReturn(Uni.createFrom().item(onboardingGet));

        given()
                .when()
                .get("/{onboardingId}/pending", onboardingGet.getId())
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .onboardingPending(onboardingGet.getId());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void approve() {
        OnboardingGet onboardingGet = dummyOnboardingGet();
        when(onboardingService.approve(onboardingGet.getId()))
                .thenReturn(Uni.createFrom().item(onboardingGet));

        given()
                .when()
                .put("/{onboardingId}/approve", onboardingGet.getId())
                .then()
                .statusCode(200);

        verify(onboardingService, times(1))
                .approve(onboardingGet.getId());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingCompletion() {

        OnboardingDefaultRequest onboardingDefaultRequest = dummyOnboardingDefaultRequest();

        Mockito.when(onboardingService.onboardingCompletion(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingDefaultRequest)
                .contentType(ContentType.JSON)
                .post("/completion")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1)).onboardingCompletion(captor.capture(), any(), any());
        assertEquals(InstitutionType.PRV, captor.getValue().getInstitution().getInstitutionType());
    }

    private static OnboardingDefaultRequest dummyOnboardingDefaultRequest() {
        OnboardingDefaultRequest onboardingDefaultRequest = new OnboardingDefaultRequest();
        InstitutionBaseRequest institution = new InstitutionBaseRequest();
        onboardingDefaultRequest.setProductId("productId");
        onboardingDefaultRequest.setUsers(List.of(userDTO));
        institution.setTaxCode("taxCode");
        institution.setDigitalAddress("digital@address.it");
        institution.setOrigin(Origin.SELC);
        institution.setOriginId("originId");
        institution.setInstitutionType(InstitutionType.PRV);
        onboardingDefaultRequest.setInstitution(institution);
        return onboardingDefaultRequest;
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPaCompletion() {

        OnboardingPaRequest onboardingPaRequest = dummyOnboardingPa();

        Mockito.when(onboardingService.onboardingCompletion(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPaRequest)
                .contentType(ContentType.JSON)
                .post("/pa/completion")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1)).onboardingCompletion(captor.capture(), any(), any());
        assertEquals(InstitutionType.PA, captor.getValue().getInstitution().getInstitutionType());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPspCompletion() {

        OnboardingPspRequest onboardingPspRequest = getOnboardingPspRequest();

        Mockito.when(onboardingService.onboardingCompletion(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPspRequest)
                .contentType(ContentType.JSON)
                .post("/psp/completion")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1)).onboardingCompletion(captor.capture(), any(), any());
        assertEquals(InstitutionType.PSP, captor.getValue().getInstitution().getInstitutionType());
    }

    private static OnboardingPspRequest getOnboardingPspRequest() {
        OnboardingPspRequest onboardingPspRequest = new OnboardingPspRequest();
        InstitutionPspRequest institution = new InstitutionPspRequest();
        onboardingPspRequest.setProductId("productId");
        onboardingPspRequest.setUsers(List.of(userDTO));
        institution.setTaxCode("taxCode");
        institution.setDigitalAddress("digital@address.it");
        institution.setOrigin(Origin.SELC);
        institution.setOriginId("originId");
        institution.setInstitutionType(InstitutionType.PSP);
        institution.setPaymentServiceProvider(new PaymentServiceProviderRequest());
        institution.setDataProtectionOfficer(new DataProtectionOfficerRequest());
        onboardingPspRequest.setInstitution(institution);
        return onboardingPspRequest;
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingCompletePg() {

        OnboardingPgRequest onboardingPgRequest = new OnboardingPgRequest();
        onboardingPgRequest.setProductId("productId");
        onboardingPgRequest.setUsers(List.of(userDTO));
        onboardingPgRequest.setTaxCode("taxCode");
        onboardingPgRequest.setDigitalAddress("digital@address.it");
        onboardingPgRequest.setOrigin(Origin.INFOCAMERE);

        Mockito.when(onboardingService.onboardingPgCompletion(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPgRequest)
                .contentType(ContentType.JSON)
                .post("/pg/completion")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1)).onboardingPgCompletion(captor.capture(), any());
        assertEquals(InstitutionType.PG, captor.getValue().getInstitution().getInstitutionType());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPgUser() {
        Mockito.when(onboardingService.onboardingUserPg(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingUserPgValid)
                .contentType(ContentType.JSON)
                .post("/users/pg")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .onboardingUserPg(captor.capture(), any());
        assertEquals(InstitutionType.PG, captor.getValue().getInstitution().getInstitutionType());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPgUserFailsWithWrongBody() {

        given()
                .when()
                .body(new OnboardingUserPgRequest())
                .contentType(ContentType.JSON)
                .post("/users/pg")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingImportPA() {

        OnboardingImportRequest onboardingImportRequest = dummyOnboardingImport();

        Mockito.when(onboardingService.onboardingImport(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingImportRequest)
                .contentType(ContentType.JSON)
                .post("/pa/import")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .onboardingImport(any(), any(), any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingImport() {

        OnboardingDefaultRequest onboardingImportRequest = dummyOnboardingDefaultRequest();
        onboardingImportRequest.getInstitution().setInstitutionType(InstitutionType.PRV);

        Mockito.when(onboardingService.onboardingImport(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingImportRequest)
                .contentType(ContentType.JSON)
                .post("/import")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .onboardingImport(any(), any(), any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboarding_shouldNotValidImportBody() {

        given()
                .when()
                .body(new OnboardingDefaultRequest())
                .contentType(ContentType.JSON)
                .post("/pa/import")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingImportPSP() {

        OnboardingImportPspRequest onboardingImportRequest = dummyOnboardingPspRequest();

        Mockito.when(onboardingService.onboardingImport(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingImportRequest)
                .contentType(ContentType.JSON)
                .post("/psp/import")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .onboardingImport(any(), any(), any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingImportPSP_with_activatedAt() {

        OnboardingImportPspRequest onboardingImportRequest = dummyOnboardingPspRequest();
        onboardingImportRequest.getContractImported().setActivatedAt(LocalDateTime.now());

        Mockito.when(onboardingService.onboardingImport(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingImportRequest)
                .contentType(ContentType.JSON)
                .post("/psp/import")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .onboardingImport(any(), any(), any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingImportPSP_bad_request() {

        OnboardingImportPspRequest onboardingImportRequest = new OnboardingImportPspRequest();

        given()
                .when()
                .body(onboardingImportRequest)
                .contentType(ContentType.JSON)
                .post("/psp/import")
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(onboardingService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getInstitutionOnboardings() {
        // given
        OnboardingResponse onboardingResponse = dummyOnboardingResponse();
        List<OnboardingResponse> onboardingResponses = new ArrayList<>();
        onboardingResponses.add(onboardingResponse);
        when(onboardingService.institutionOnboardings("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.PENDING))
                .thenReturn(Uni.createFrom().item(onboardingResponses));

        Map<String, String> queryParameterMap = getStringStringMapOnboardings();

        // when
        given()
                .when()
                .queryParams(queryParameterMap)
                .get("/institutionOnboardings")
                .then()
                .statusCode(200);

        // then
        verify(onboardingService, times(1))
                .institutionOnboardings("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.PENDING);
        assertNotNull(onboardingResponses);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void verifyOnboardingNoContentType() {
        OnboardingResponse onboardingResponse = dummyOnboardingResponse();
        List<OnboardingResponse> onboardingResponses = new ArrayList<>();
        onboardingResponses.add(onboardingResponse);
        when(onboardingService.verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop", null))
                .thenReturn(Uni.createFrom().item(onboardingResponses));

        Map<String, String> queryParameterMap = getStringStringMapOnboardings();

        given()
                .when()
                .queryParams(queryParameterMap)
                .head("/verify")
                .then()
                .statusCode(204);

        verify(onboardingService, times(1))
                .verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop", null);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void verifyOnboardingResourceNotFound() {
        List<OnboardingResponse> onboardingResponses = new ArrayList<>();
        when(onboardingService.verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop", null))
                .thenReturn(Uni.createFrom().item(onboardingResponses));

        Map<String, String> queryParameterMap = getStringStringMapOnboardings();

        given()
                .when()
                .queryParams(queryParameterMap)
                .head("/verify")
                .then()
                .statusCode(404);

        verify(onboardingService, times(1))
                .verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop", null);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void verifyOnboardingWithValidInstitutionType() {
        OnboardingResponse onboardingResponse = dummyOnboardingResponse();
        List<OnboardingResponse> onboardingResponses = new ArrayList<>();
        onboardingResponses.add(onboardingResponse);
        when(onboardingService.verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop", InstitutionType.PA))
                .thenReturn(Uni.createFrom().item(onboardingResponses));

        Map<String, String> queryParameterMap = getStringStringMapOnboardings();
        queryParameterMap.put("institutionType", "PA");

        given()
                .when()
                .queryParams(queryParameterMap)
                .head("/verify")
                .then()
                .statusCode(204);

        verify(onboardingService, times(1))
                .verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop", InstitutionType.PA);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void verifyOnboardingWithEmptyInstitutionType() {
        OnboardingResponse onboardingResponse = dummyOnboardingResponse();
        List<OnboardingResponse> onboardingResponses = new ArrayList<>();
        onboardingResponses.add(onboardingResponse);
        when(onboardingService.verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop", null))
                .thenReturn(Uni.createFrom().item(onboardingResponses));

        Map<String, String> queryParameterMap = getStringStringMapOnboardings();
        queryParameterMap.put("institutionType", "");

        given()
                .when()
                .queryParams(queryParameterMap)
                .head("/verify")
                .then()
                .statusCode(204);

        verify(onboardingService, times(1))
                .verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop", null);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void verifyOnboardingWithInvalidInstitutionType() {
        Map<String, String> queryParameterMap = getStringStringMapOnboardings();
        queryParameterMap.put("institutionType", "INVALID_TYPE");

        given()
                .when()
                .queryParams(queryParameterMap)
                .head("/verify")
                .then()
                .statusCode(500);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void verifyOnboardingWithDifferentInstitutionTypes() {
        OnboardingResponse onboardingResponse = dummyOnboardingResponse();
        List<OnboardingResponse> onboardingResponses = new ArrayList<>();
        onboardingResponses.add(onboardingResponse);

        // Test with PSP
        when(onboardingService.verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop", InstitutionType.PSP))
                .thenReturn(Uni.createFrom().item(onboardingResponses));

        Map<String, String> queryParameterMap = getStringStringMapOnboardings();
        queryParameterMap.put("institutionType", "PSP");

        given()
                .when()
                .queryParams(queryParameterMap)
                .head("/verify")
                .then()
                .statusCode(204);

        verify(onboardingService, times(1))
                .verifyOnboarding("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.COMPLETED, "prod-interop", InstitutionType.PSP);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void checkRecipientCodeValid() {
        String recipientCode = "validRecipientCode";
        String originId = "validOriginId";

        when(onboardingService.checkRecipientCode(recipientCode, originId))
                .thenReturn(Uni.createFrom().nullItem());

        Response response = given()
                .when()
                .queryParam("recipientCode", recipientCode)
                .queryParam("originId", originId)
                .get("/checkRecipientCode")
                .then()
                .statusCode(200)
                .extract()
                .response();

        RecipientCodeStatus responseBody = response.as(RecipientCodeStatus.class);
        assertEquals(RecipientCodeStatus.ACCEPTED, responseBody);

        verify(onboardingService, times(1)).checkRecipientCode(recipientCode, originId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void checkRecipientCodeDeniedNoBilling() {
        String recipientCode = "recipientCodeNoBilling";
        String originId = "originIdNoBilling";

        when(onboardingService.checkRecipientCode(recipientCode, originId))
                .thenReturn(Uni.createFrom().item(CustomError.DENIED_NO_BILLING));

        Response response = given()
                .when()
                .queryParam("recipientCode", recipientCode)
                .queryParam("originId", originId)
                .get("/checkRecipientCode")
                .then()
                .statusCode(200)
                .extract()
                .response();

        RecipientCodeStatus responseBody = response.as(RecipientCodeStatus.class);
        assertEquals(RecipientCodeStatus.DENIED_NO_BILLING, responseBody);

        verify(onboardingService, times(1)).checkRecipientCode(recipientCode, originId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void checkRecipientCodeDeniedNoAssociation() {
        String recipientCode = "recipientCodeNoAssociation";
        String originId = "originIdNoAssociation";

        when(onboardingService.checkRecipientCode(recipientCode, originId))
                .thenReturn(Uni.createFrom().item(CustomError.DENIED_NO_ASSOCIATION));

        Response response = given()
                .when()
                .queryParam("recipientCode", recipientCode)
                .queryParam("originId", originId)
                .get("/checkRecipientCode")
                .then()
                .statusCode(200)
                .extract()
                .response();

        RecipientCodeStatus responseBody = response.as(RecipientCodeStatus.class);
        assertEquals(RecipientCodeStatus.DENIED_NO_ASSOCIATION, responseBody);

        verify(onboardingService, times(1)).checkRecipientCode(recipientCode, originId);
    }


    private static Map<String, String> getStringStringMap() {
        Map<String, String> queryParameterMap = new HashMap<>();
        queryParameterMap.put("productId", "prod-io");
        queryParameterMap.put("taxCode", "taxCode");
        queryParameterMap.put("from", "2023-12-01");
        queryParameterMap.put("to", "2023-12-31");
        queryParameterMap.put("status", "ACTIVE");
        return queryParameterMap;
    }

    private static Map<String, String> getStringStringMapOnboardings() {
        Map<String, String> queryParameterMap = new HashMap<>();
        queryParameterMap.put("taxCode", "taxCode");
        queryParameterMap.put("subunitCode", "subunitCode");
        queryParameterMap.put("origin", "origin");
        queryParameterMap.put("originId", "originId");
        queryParameterMap.put("status", "PENDING");
        queryParameterMap.put("productId", "prod-interop");
        return queryParameterMap;
    }

    private static OnboardingGetResponse getOnboardingGetResponse() {
        OnboardingGet onboarding = dummyOnboardingGet();
        OnboardingGetResponse response = new OnboardingGetResponse();
        response.setCount(1L);
        response.setItems(List.of(onboarding));
        return response;
    }

    private static OnboardingGet dummyOnboardingGet() {
        OnboardingGet onboarding = new OnboardingGet();
        onboarding.setId("id");
        onboarding.setStatus("ACTIVE");
        onboarding.setProductId("prod-io");
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setTaxCode("taxCode");
        institutionResponse.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setInstitution(institutionResponse);
        return onboarding;
    }

    private static OnboardingResponse dummyOnboardingResponse() {
        String str = "2025-01-09 11:36";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);

        OnboardingResponse onboarding = new OnboardingResponse();
        onboarding.setId("id");
        onboarding.setStatus("PENDING");
        onboarding.setProductId("prod-io");
        onboarding.setUpdatedAt(dateTime);
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setTaxCode("taxCode");
        institutionResponse.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setInstitution(institutionResponse);
        return onboarding;
    }

    private OnboardingUserRequest dummyOnboardingUser() {
        OnboardingUserRequest onboardingUserRequest = new OnboardingUserRequest();
        onboardingUserRequest.setProductId("productId");
        onboardingUserRequest.setTaxCode("taxCode");
        onboardingUserRequest.setOriginId("originId");
        onboardingUserRequest.setOrigin("origin");
        onboardingUserRequest.setUsers(List.of(userDTO));
        return onboardingUserRequest;
    }

    private OnboardingPaRequest dummyOnboardingPa() {
        OnboardingPaRequest onboardingPaValid = new OnboardingPaRequest();
        onboardingPaValid.setProductId("productId");

        BillingPaRequest billingPaRequest = new BillingPaRequest();
        billingPaRequest.setRecipientCode("code");
        billingPaRequest.setVatNumber("vat");

        institution.setInstitutionType(InstitutionType.PA);

        onboardingPaValid.setUsers(List.of(userDTO));
        onboardingPaValid.setInstitution(institution);
        onboardingPaValid.setBilling(billingPaRequest);

        return onboardingPaValid;
    }

    private OnboardingImportRequest dummyOnboardingImport() {
        InstitutionImportRequest importInstitution = new InstitutionImportRequest();
        importInstitution.setTaxCode("taxCode");
        OnboardingImportRequest onboardingImportValid = new OnboardingImportRequest();
        onboardingImportValid.setProductId("productId");
        onboardingImportValid.setContractImported(new OnboardingImportContract());
        onboardingImportValid.setUsers(List.of(userDTO));
        onboardingImportValid.setInstitution(importInstitution);

        return onboardingImportValid;
    }

    private OnboardingImportPspRequest dummyOnboardingPspRequest() {
        OnboardingImportPspRequest onboardingPspRequest = new OnboardingImportPspRequest();
        onboardingPspRequest.setBilling(new BillingRequest());
        onboardingPspRequest.setContractImported(new OnboardingImportContract());
        InstitutionPspRequest institutionPspRequest = new InstitutionPspRequest();
        institutionPspRequest.setInstitutionType(InstitutionType.PSP);
        institutionPspRequest.setOrigin(Origin.SELC);
        institutionPspRequest.setOriginId("originId");
        institutionPspRequest.setDigitalAddress("address@gmail.com");
        PaymentServiceProviderRequest pspData = new PaymentServiceProviderRequest();
        pspData.setAbiCode("abiCode");
        pspData.setProviderNames(List.of("test"));
        institutionPspRequest.setPaymentServiceProvider(new PaymentServiceProviderRequest());
        onboardingPspRequest.setInstitution(institutionPspRequest);
        onboardingPspRequest.setProductId("prod-io");
        return onboardingPspRequest;
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateOnboardingOK() {
        String onboardingId = "actual-onboarding-id";
        Onboarding onboardingUpdate = new Onboarding();
        onboardingUpdate.setId(onboardingId);
        Billing billing = new Billing();
        billing.setRecipientCode("X123");
        onboardingUpdate.setBilling(billing);

        when(onboardingService.updateOnboarding(onboardingId, onboardingUpdate))
                .thenReturn(Uni.createFrom().item(1L));

        Map<String, String> queryParameterMap = getStringStringMapOnboardingStatusUpdate();

        given()
                .when()
                .queryParams(queryParameterMap)
                .body(onboardingUpdate)
                .contentType(ContentType.JSON)
                .pathParam("onboardingId", onboardingId)
                .put("/{onboardingId}/update")
                .then()
                .statusCode(204);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .updateOnboarding(any(), captor.capture());
        assertEquals(captor.getValue().getBilling().getRecipientCode(), billing.getRecipientCode());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateOnboardingNotFound() {
        String onboardingId = "actual-onboarding-id";
        Onboarding onboardingUpdate = new Onboarding();

        when(onboardingService.updateOnboarding(onboardingId, onboardingUpdate))
                .thenThrow(InvalidRequestException.class);

        Map<String, String> queryParameterMap = getStringStringMapOnboardingStatusUpdate();

        given()
                .when()
                .queryParams(queryParameterMap)
                .body(onboardingUpdate)
                .contentType(ContentType.JSON)
                .pathParam("onboardingId", onboardingId)
                .put("/{onboardingId}/update")
                .then()
                .statusCode(204);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1))
                .updateOnboarding(any(), captor.capture());
        assertNotEquals(captor.getValue().getId(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void checkManager() {
        CheckManagerRequest request = new CheckManagerRequest();

        CheckManagerResponse response = new CheckManagerResponse();
        response.setResponse(true);
        when(onboardingService.checkManager(any()))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .when()
                .body(request)
                .contentType(ContentType.JSON)
                .post("/check-manager")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .checkManager(any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingAggregationCompletion() {

        Mockito.when(onboardingService.onboardingAggregationCompletion(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingBaseValid)
                .contentType(ContentType.JSON)
                .post("/aggregation/completion")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .onboardingAggregationCompletion(any(), any(), any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingAggregationPspCompletion() {

        Mockito.when(onboardingService.onboardingAggregationCompletion(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPspValid)
                .contentType(ContentType.JSON)
                .post("/aggregation/psp/completion")
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1))
                .onboardingAggregationCompletion(any(), any(), any(), any());
    }


    private Map<String, String> getStringStringMapOnboardingStatusUpdate() {
        Map<String, String> queryParameterMap = new HashMap<>();
        queryParameterMap.put("status", "COMPLETED");
        return queryParameterMap;
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateRecipientCodeByOnboardingIdTest() {
        // given
        String fakeOnboardingId = "ASDF22234545";
        String fakeRecipientCode = "TEST_CODE2234";

        Onboarding onboarding = new Onboarding();
        Billing billing = new Billing();
        billing.setRecipientCode(fakeRecipientCode);
        onboarding.setBilling(billing);

        when(onboardingService.updateOnboarding(fakeOnboardingId, onboarding))
                .thenReturn(Uni.createFrom().item(1L));

        // when
        given()
                .when()
                .queryParam("recipientCode", fakeRecipientCode)
                .pathParam("onboardingId", fakeOnboardingId)
                .contentType(ContentType.JSON)
                .put("/{onboardingId}/recipient-code")
                .then()
                .statusCode(204);

        // then
        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1)).updateOnboarding(anyString(), captor.capture());
        assertEquals(captor.getValue().getBilling().getRecipientCode(), fakeRecipientCode);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingAggregationImportTest_KO() {
        // given
        OnboardingAggregationImportRequest onboardingImport = new OnboardingAggregationImportRequest();

        //when
        given()
            .when()
            .body(onboardingImport)
            .contentType(ContentType.JSON)
            .post("/aggregation/import")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingAggregationImportTest_OK() {
        // given
        OnboardingAggregationImportRequest onboardingImport = dummyOnboardingAggregationImportRequest();
        OnboardingResponse response = dummyOnboardingResponse();

        Mockito.when(onboardingService.onboardingAggregationImport(any(), any(), any(), any(), any()))
            .thenReturn(Uni.createFrom().item(response));

        // when
        given()
            .when()
            .body(onboardingImport)
            .contentType(ContentType.JSON)
            .post("/aggregation/import")
            .then()
            .statusCode(200);

        // then
        Mockito.verify(onboardingService, times(1))
            .onboardingAggregationImport(any(), any(), any(), any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteOnboardingOK() {

        final String onboardingId = "actual-onboarding-id";
        when(onboardingService.deleteOnboarding(onboardingId))
                .thenReturn(Uni.createFrom().item(1L));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("onboardingId", onboardingId)
                .delete("/{onboardingId}")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .deleteOnboarding(expectedId.capture());
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteOnboardingNotFound() {

        final String onboardingId = "actual-onboarding-id";
        when(onboardingService.deleteOnboarding(onboardingId))
                .thenThrow(InvalidRequestException.class);

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("onboardingId", onboardingId)
                .delete("/{onboardingId}")
                .then()
                .statusCode(400);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(onboardingService, times(1))
                .deleteOnboarding(expectedId.capture());
        assertEquals(expectedId.getValue(), onboardingId);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingWithValidItalianIban() {
        // given
        OnboardingDefaultRequest req = dummyOnboardingDefaultRequest();
        req.getInstitution().setOrigin(Origin.PDND_INFOCAMERE);
        req.getInstitution().setInstitutionType(InstitutionType.PRV);
        PaymentRequestDto payment =  new PaymentRequestDto();
        payment.setIban("IT60X0542811101000000123456");
        payment.setHolder("Mario Rossi");
        req.setPayment(payment);

        Mockito.when(onboardingService.onboarding(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        // when/then
        given()
                .when()
                .body(req)
                .contentType(ContentType.JSON)
                .post()
                .then()
                .statusCode(200);

        Mockito.verify(onboardingService, times(1)).onboarding(any(), any(), any(), any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingWithInvalidItalianIban() {
        // given
        OnboardingDefaultRequest req = dummyOnboardingDefaultRequest();
        req.getInstitution().setOrigin(Origin.PDND_INFOCAMERE);
        req.getInstitution().setInstitutionType(InstitutionType.PRV);
        PaymentRequestDto payment =  new PaymentRequestDto();
        payment.setIban("FR1420041010050500013M02606");
        payment.setHolder("Mario Rossi");
        req.setPayment(payment);

        Mockito.when(onboardingService.onboarding(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        // when / then
        given()
                .when()
                .body(req)
                .contentType(ContentType.JSON)
                .post()
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(onboardingService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingWithInvalidIbanLength() {

        // given
        OnboardingDefaultRequest req = dummyOnboardingDefaultRequest();
        req.getInstitution().setOrigin(Origin.PDND_INFOCAMERE);
        req.getInstitution().setInstitutionType(InstitutionType.PRV);
        PaymentRequestDto payment =  new PaymentRequestDto();
        payment.setIban("IT60X054281110100000012345");
        payment.setHolder("Mario Rossi");
        req.setPayment(payment);

        Mockito.when(onboardingService.onboarding(any(), any(), any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        // when / then
        given()
                .when()
                .body(req)
                .contentType(ContentType.JSON)
                .post()
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(onboardingService);
    }

    private OnboardingAggregationImportRequest dummyOnboardingAggregationImportRequest() {
        OnboardingAggregationImportRequest onboardingRequest = new OnboardingAggregationImportRequest();
        onboardingRequest.setBilling(new BillingRequest());
        InstitutionBaseRequest institutionBaseRequest = new InstitutionBaseRequest();
        onboardingRequest.setProductId("productId");
        onboardingRequest.setUsers(List.of(userDTO));
        institutionBaseRequest.setTaxCode("taxCode");
        institutionBaseRequest.setDigitalAddress("digital@address.it");
        institutionBaseRequest.setOrigin(Origin.SELC);
        institutionBaseRequest.setOriginId("originId");
        institutionBaseRequest.setInstitutionType(InstitutionType.PRV);
        onboardingRequest.setInstitution(institutionBaseRequest);
        onboardingRequest.setProductId("prod-io");
        OnboardingImportContract importContract = new OnboardingImportContract();
        importContract.setFilePath("/test/path");
        String str = "2025-01-15 11:30";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
        importContract.setCreatedAt(dateTime);
        onboardingRequest.setOnboardingImportContract(importContract);
        return onboardingRequest;
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getOnboardingProduct_whenFound_shouldReturnOk() {

        OnboardingGet onboarding = dummyOnboardingGet();
        String productId = "productId";
        String institutionId = "institutionId";
        onboarding.setId("onboarding-123");
        onboarding.setProductId(productId);

        when(onboardingService.retrieveOnboardingByInstitutionId(institutionId, productId))
                // ...deve restituire un Uni contenente il nostro oggetto di test.
                .thenReturn(Uni.createFrom().item(onboarding));

        given()
                .pathParam("institutionId", institutionId)
                .queryParam("productId", productId)
                .when()
                .get("/institutions/{institutionId}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    /**
     * Test per il caso di fallimento (404 Not Found)
     * Verifica che quando il service non trova l'onboarding, l'API risponda con 404.
     */
    @Test
    @TestSecurity(user = "userJwt")
    void getOnboardingProduct_whenNotFound_shouldReturnNotFound() {

        String productId = "productId";
        String institutionId = "institutionId";
        String errorMessage = String.format("Onboarding not found for institution %s and product %s",
                institutionId, productId);


        when(onboardingService.retrieveOnboardingByInstitutionId(institutionId, productId))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException(errorMessage)));

        given()
                .pathParam("institutionId", institutionId)
                .queryParam("productId", productId)
                .when()
                .get("/institutions/{institutionId}")
                .then()
                .statusCode(404);
    }

    @Test
    void uploadContractSigned_unauthorized() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{onboardingId}/upload-contract-signed")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void uploadContractSigned_whenSuccess_shouldReturnNoContent() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.uploadContractSigned(anyString(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{onboardingId}/upload-contract-signed")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void uploadContractSigned_withoutFile_shouldReturnBadRequest() {
        String onboardingId = "actual-onboarding-id";

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .put("/{onboardingId}/upload-contract-signed")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void uploadContractSigned_whenServiceThrowsException_shouldReturnError() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";
        String errorMessage = "Upload failed";

        when(onboardingService.uploadContractSigned(anyString(), any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException(errorMessage)));

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{onboardingId}/upload-contract-signed")
                .then()
                .statusCode(500);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void uploadContractSigned_shouldPassCorrectParametersToService() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        when(onboardingService.uploadContractSigned(anyString(), any()))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .contentType(ContentType.MULTIPART)
                .multiPart("contract", testFile)
                .put("/{onboardingId}/upload-contract-signed")
                .then()
                .statusCode(204);

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<FormItem> formItemCaptor = ArgumentCaptor.forClass(FormItem.class);
        verify(onboardingService, times(1))
                .uploadContractSigned(idCaptor.capture(), formItemCaptor.capture());
        assertEquals(idCaptor.getValue(), onboardingId);
        assertNotNull(formItemCaptor.getValue());
    }

}
