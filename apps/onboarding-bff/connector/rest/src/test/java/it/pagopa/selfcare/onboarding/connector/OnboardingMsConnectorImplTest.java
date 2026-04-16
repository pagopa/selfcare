package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.exceptions.InvalidRequestException;
import it.pagopa.selfcare.onboarding.connector.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.connector.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.institutions.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.*;
import it.pagopa.selfcare.onboarding.connector.rest.client.*;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.OnboardingMapperImpl;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {OnboardingMsConnectorImpl.class})
@ExtendWith(MockitoExtension.class)
class OnboardingMsConnectorImplTest {

    @Mock
    private MsOnboardingAggregatesApiClient msOnboardingAggregatesApiClient;
    @InjectMocks
    private OnboardingMsConnectorImpl onboardingMsConnector;

    @Mock
    private MsOnboardingApiClient msOnboardingApiClient;

    @Mock
    private MsOnboardingBillingApiClient msOnboardingBillingApiClient;

    @Mock
    private MsOnboardingSupportApiClient msOnboardingSupportApiClient;

    @Mock
    private MsOnboardingInternalApiClient msOnboardingInternalApiClient;


    @Spy
    private OnboardingMapper onboardingMapper = new OnboardingMapperImpl();

    @Test
    void onboarding_institutionDefault() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setTaxCode("taxCode");
        onboardingData.setInstitutionType(InstitutionType.GSP);
        Billing billing = mockInstance(new Billing());
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setTaxCode("taxCode");
        institutionUpdate.setDescription("description");
        onboardingData.setBilling(billing);
        onboardingData.setUsers(List.of(mockInstance(new User())));
        onboardingData.setOriginId("originId");
        onboardingData.setInstitutionUpdate(institutionUpdate);
        UserRequester userRequester = new UserRequester();
        userRequester.setEmail("email");
        userRequester.setName("name");
        userRequester.setSurname("surname");
        onboardingData.setUserRequester(userRequester);
        // when
        onboardingMsConnector.onboarding(onboardingData);
        // then

        ArgumentCaptor<OnboardingDefaultRequest> onboardingRequestCaptor = ArgumentCaptor.forClass(OnboardingDefaultRequest.class);
        verify(msOnboardingApiClient, times(1))
                ._onboarding(onboardingRequestCaptor.capture());
        OnboardingDefaultRequest actual = onboardingRequestCaptor.getValue();
        assertEquals(actual.getInstitution().getTaxCode(), institutionUpdate.getTaxCode());
        assertEquals(actual.getInstitution().getDescription(), institutionUpdate.getDescription());
        assertEquals(actual.getUserRequester().getEmail(), userRequester.getEmail());
        assertEquals(actual.getUserRequester().getName(), userRequester.getName());
        assertEquals(actual.getUserRequester().getSurname(), userRequester.getSurname());
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void onboarding_institutionPa() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setTaxCode("taxCode");
        onboardingData.setInstitutionType(InstitutionType.PA);
        Billing billing = mockInstance(new Billing());
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setTaxCode("taxCode");
        onboardingData.setBilling(billing);
        onboardingData.setUsers(List.of(mockInstance(new User())));
        onboardingData.setInstitutionUpdate(institutionUpdate);
        UserRequester userRequester = new UserRequester();
        userRequester.setEmail("email");
        userRequester.setName("name");
        userRequester.setSurname("surname");
        onboardingData.setUserRequester(userRequester);
        // when
        onboardingMsConnector.onboarding(onboardingData);
        // then

        ArgumentCaptor<OnboardingPaRequest> onboardingRequestCaptor = ArgumentCaptor.forClass(OnboardingPaRequest.class);
        verify(msOnboardingApiClient, times(1))
                ._onboardingPa(onboardingRequestCaptor.capture());
        OnboardingPaRequest actual = onboardingRequestCaptor.getValue();
        assertEquals(actual.getInstitution().getTaxCode(), institutionUpdate.getTaxCode());
        assertEquals(actual.getUserRequester().getEmail(), userRequester.getEmail());
        assertEquals(actual.getUserRequester().getName(), userRequester.getName());
        assertEquals(actual.getUserRequester().getSurname(), userRequester.getSurname());
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void onboarding_institutionPsp() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setTaxCode("taxCode");
        onboardingData.setInstitutionType(InstitutionType.PSP);
        Billing billing = mockInstance(new Billing());
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setTaxCode("taxCode");
        institutionUpdate.setPaymentServiceProvider(new PaymentServiceProvider());
        institutionUpdate.setDataProtectionOfficer(new DataProtectionOfficer());
        onboardingData.setBilling(billing);
        onboardingData.setUsers(List.of(mockInstance(new User())));
        onboardingData.setInstitutionUpdate(institutionUpdate);
        UserRequester userRequester = new UserRequester();
        userRequester.setEmail("email");
        userRequester.setName("name");
        userRequester.setSurname("surname");
        onboardingData.setUserRequester(userRequester);
        // when
        onboardingMsConnector.onboarding(onboardingData);
        // then
        ArgumentCaptor<OnboardingPspRequest> onboardingRequestCaptor = ArgumentCaptor.forClass(OnboardingPspRequest.class);
        verify(msOnboardingApiClient, times(1))
                ._onboardingPsp(onboardingRequestCaptor.capture());
        OnboardingPspRequest actual = onboardingRequestCaptor.getValue();
        assertEquals(actual.getInstitution().getTaxCode(), institutionUpdate.getTaxCode());
        assertNotNull(actual.getInstitution().getPaymentServiceProvider());
        assertNotNull(actual.getInstitution().getDataProtectionOfficer());
        assertEquals(actual.getUserRequester().getEmail(), userRequester.getEmail());
        assertEquals(actual.getUserRequester().getName(), userRequester.getName());
        assertEquals(actual.getUserRequester().getSurname(), userRequester.getSurname());
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void aggregatesVerification_withProdIO_shouldReturnValidResult() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "content".getBytes());
        VerifyAggregateResponse verifyAggregateAppIoResponse = new VerifyAggregateResponse();
        VerifyAggregateResult expectedResult = new VerifyAggregateResult();
        when(msOnboardingAggregatesApiClient._verifyAppIoAggregatesCsv(file))
                .thenReturn(ResponseEntity.ok(verifyAggregateAppIoResponse));
        when(onboardingMapper.toVerifyAggregateResult(eq(verifyAggregateAppIoResponse))).thenReturn(expectedResult);
        // when
        VerifyAggregateResult result = onboardingMsConnector.aggregatesVerification(file, "prod-io");

        // then
        assertEquals(expectedResult, result);
        verify(msOnboardingAggregatesApiClient, times(1))._verifyAppIoAggregatesCsv(file);
        verify(onboardingMapper, times(1)).toVerifyAggregateResult(verifyAggregateAppIoResponse);
    }

    @Test
    void aggregatesVerification_withProdPagoPa_shouldReturnValidResult() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "content".getBytes());
        VerifyAggregateResult expectedResult = new VerifyAggregateResult();
        VerifyAggregateResponse verifyAggregateResponse = new VerifyAggregateResponse();
        when(msOnboardingAggregatesApiClient._verifyPagoPaAggregatesCsv(file))
                .thenReturn(ResponseEntity.ok(new VerifyAggregateResponse()));
        when(onboardingMapper.toVerifyAggregateResult(eq(verifyAggregateResponse))).thenReturn(expectedResult);
        // when
        VerifyAggregateResult result = onboardingMsConnector.aggregatesVerification(file, "prod-pagopa");

        // then
        assertEquals(expectedResult, result);
        verify(msOnboardingAggregatesApiClient, times(1))._verifyPagoPaAggregatesCsv(file);
        verify(onboardingMapper, times(1)).toVerifyAggregateResult(verifyAggregateResponse);
    }

    @Test
    void aggregatesVerification_withProdPN_shouldReturnValidResult() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "content".getBytes());
        VerifyAggregateResult expectedResult = new VerifyAggregateResult();
        when(msOnboardingAggregatesApiClient._verifySendAggregatesCsv(file))
                .thenReturn(ResponseEntity.ok(new VerifyAggregateResponse()));
        when(onboardingMapper.toVerifyAggregateResult(any())).thenReturn(expectedResult);

        // when
        VerifyAggregateResult result = onboardingMsConnector.aggregatesVerification(file, "prod-pn");

        // then
        assertEquals(expectedResult, result);
        verify(msOnboardingAggregatesApiClient, times(1))._verifySendAggregatesCsv(file);
        verify(onboardingMapper, times(1)).toVerifyAggregateResult(any());
    }

    @Test
    void aggregatesVerification_withUnsupportedProductId_shouldThrowInvalidRequestException() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "content".getBytes());


        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                onboardingMsConnector.aggregatesVerification(file, "prod-fd"));
        assertEquals("400 BAD_REQUEST Unsupported productId: prod-fd", exception.getMessage());
        verify(msOnboardingAggregatesApiClient, never())._verifyAppIoAggregatesCsv(any());
        verify(msOnboardingAggregatesApiClient, never())._verifyPagoPaAggregatesCsv(any());
        verify(msOnboardingAggregatesApiClient, never())._verifySendAggregatesCsv(any());
    }

    @Test
    void onboardingCompany() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId("produictId");
        onboardingData.setTaxCode("taxCode");
        onboardingData.setInstitutionType(InstitutionType.PG);
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setTaxCode("taxCode");
        institutionUpdate.setDescription("description");
        onboardingData.setUsers(List.of(mockInstance(new User())));
        onboardingData.setInstitutionUpdate(institutionUpdate);
        // when
        onboardingMsConnector.onboardingCompany(onboardingData);
        // then
        ArgumentCaptor<OnboardingPgRequest> onboardingRequestCaptor = ArgumentCaptor.forClass(OnboardingPgRequest.class);
        verify(msOnboardingApiClient, times(1))
                ._onboardingPgCompletion(onboardingRequestCaptor.capture());
        OnboardingPgRequest actual = onboardingRequestCaptor.getValue();
        assertEquals(actual.getProductId(), onboardingData.getProductId());
        assertEquals(actual.getTaxCode(), institutionUpdate.getTaxCode());
        assertEquals(actual.getDigitalAddress(), institutionUpdate.getDigitalAddress());
        assertEquals(actual.getBusinessName(), institutionUpdate.getDescription());
        assertEquals(actual.getInstitutionType().getValue(), onboardingData.getInstitutionType().name());
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void onboardingComplete() throws IOException {
        // given
        final String tokenId = "tokenId";
        final MockMultipartFile mockMultipartFile =
                new MockMultipartFile("example", new ByteArrayInputStream("example".getBytes(StandardCharsets.UTF_8)));
        // when
        final Executable executable = () -> msOnboardingInternalApiClient._completeOnboardingUsingPUT(tokenId, mockMultipartFile);
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingInternalApiClient, times(1))
                ._completeOnboardingUsingPUT(tokenId, mockMultipartFile);
        verifyNoMoreInteractions(msOnboardingInternalApiClient);
    }

    @Test
    void onboardingUsersComplete() throws IOException {
        // given
        final String tokenId = "tokenId";
        final MockMultipartFile mockMultipartFile =
                new MockMultipartFile("example", new ByteArrayInputStream("example".getBytes(StandardCharsets.UTF_8)));
        // when
        final Executable executable = () -> msOnboardingApiClient._completeOnboardingUser(tokenId, mockMultipartFile);
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingApiClient, times(1))
                ._completeOnboardingUser(tokenId, mockMultipartFile);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void onboardingPending() {
        // given
        final String onboardingId = "onboardingId";
        // when
        final Executable executable = () -> onboardingMsConnector.onboardingPending(onboardingId);
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingApiClient, times(1))
                ._getOnboardingPending(onboardingId);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void getOnboardingWithUserInfo() {
        // given
        final String onboardingId = "onboardingId";
        when(msOnboardingApiClient._getByIdWithUserInfo(onboardingId))
                .thenReturn(ResponseEntity.of(Optional.of(new OnboardingGet())));
        // when
        final Executable executable = () -> onboardingMsConnector.getOnboardingWithUserInfo(onboardingId);
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingApiClient, times(1))
                ._getByIdWithUserInfo(onboardingId);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void approveOnboarding() {
        // given
        final String onboardingId = "onboardingId";
        // when
        final Executable executable = () -> onboardingMsConnector.approveOnboarding(onboardingId);
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingApiClient, times(1))
                ._approve(onboardingId);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void rejectOnboarding() {
        // given
        final String reason = "reason";
        final String onboardingId = "onboardingId";
        ReasonRequest reasonDto = new ReasonRequest();
        reasonDto.setReasonForReject(reason);
        // when
        final Executable executable = () -> onboardingMsConnector.rejectOnboarding(onboardingId, reason);
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingApiClient, times(1))
                ._delete(onboardingId, reasonDto);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void rejectOnboardingWithNoReason() {
        // given
        final String onboardingId = "onboardingId";
        ReasonRequest reasonDto = new ReasonRequest();
        // when
        final Executable executable = () -> onboardingMsConnector.rejectOnboarding(onboardingId, "");
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingApiClient, times(1))
                ._delete(onboardingId, reasonDto);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void onboardingUsers() {
        // given
        final String origin = "origin";
        final String originId = "originId";
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setOrigin(origin);
        onboardingData.setOriginId(originId);
        OnboardingUserRequest request = new OnboardingUserRequest();
        request.setOrigin(origin);
        request.setOriginId(originId);
        OnboardingResponse resource = Mockito.mock(OnboardingResponse.class);
        when(msOnboardingApiClient._onboardingUsers(request))
                .thenReturn(ResponseEntity.of(Optional.of(resource)));
        // when
        final Executable executable = () -> onboardingMsConnector.onboardingUsers(onboardingData);
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingApiClient, times(1))
                ._onboardingUsers(request);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }


    @Test
    void onboardingUsersAggregator() {
        // given
        final String origin = "origin";
        final String originId = "originId";
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setOrigin(origin);
        onboardingData.setOriginId(originId);
        OnboardingUserRequest request = new OnboardingUserRequest();
        request.setOrigin(origin);
        request.setOriginId(originId);
        OnboardingResponse resource = Mockito.mock(OnboardingResponse.class);
        when(msOnboardingApiClient._onboardingUsersAggregator(request))
                .thenReturn(ResponseEntity.of(Optional.of(resource)));
        // when
        final Executable executable = () -> onboardingMsConnector.onboardingUsersAggregator(onboardingData);
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingApiClient, times(1))
                ._onboardingUsersAggregator(request);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void onboardingPaAggregation() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setProductId("produictId");
        onboardingData.setTaxCode("taxCode");
        onboardingData.setInstitutionType(InstitutionType.PG);
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setTaxCode("taxCode");
        institutionUpdate.setDescription("description");
        onboardingData.setUsers(List.of(mockInstance(new User())));
        onboardingData.setInstitutionUpdate(institutionUpdate);
        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setDescription("description");
        onboardingData.setAggregates(List.of(institution));
        onboardingData.setIsAggregator(Boolean.TRUE);
        // when
        onboardingMsConnector.onboardingPaAggregation(onboardingData);
        // then
        ArgumentCaptor<OnboardingPaRequest> onboardingRequestCaptor = ArgumentCaptor.forClass(OnboardingPaRequest.class);
        verify(msOnboardingApiClient, times(1))
                ._onboardingPaAggregation(onboardingRequestCaptor.capture());
        OnboardingPaRequest actual = onboardingRequestCaptor.getValue();
        assertEquals(actual.getProductId(), onboardingData.getProductId());
        assertEquals(actual.getAggregates().size(), onboardingData.getAggregates().size());
        assertEquals(actual.getAggregates().get(0).getTaxCode(), onboardingData.getAggregates().get(0).getTaxCode());
        assertEquals(actual.getAggregates().get(0).getDescription(), onboardingData.getAggregates().get(0).getDescription());
        assertEquals(actual.getInstitution().getTaxCode(), onboardingData.getTaxCode());
        assertEquals(actual.getInstitution().getTaxCode(), onboardingData.getInstitutionUpdate().getTaxCode());
        assertEquals(actual.getInstitution().getDescription(), onboardingData.getInstitutionUpdate().getDescription());
        assertEquals(actual.getUsers().size(), onboardingData.getUsers().size());

        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void getOnboardingByFilters() {
        // given
        final String origin = "origin";
        final String originId = "originId";
        final String productId = "productId";
        OnboardingResponse resource = new OnboardingResponse();
        resource.setInstitution(new InstitutionResponse());
        resource.setProductId("productId");
        when(msOnboardingSupportApiClient._onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, null, null))
                .thenReturn(ResponseEntity.ok(List.of(resource)));
        // when
        final Executable executable = () -> onboardingMsConnector.getByFilters(productId, null, origin, originId, null);
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingSupportApiClient, times(1))
                ._onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, null, null);
        verifyNoMoreInteractions(msOnboardingSupportApiClient);
    }

    @Test
    void onboardingUsersPgFromIcAndAde() {
        final String origin = "ADE";
        // given
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setTaxCode("taxCode");
        onboardingData.setOrigin(origin);
        onboardingData.setInstitutionType(InstitutionType.PG);
        onboardingData.setUsers(List.of(mockInstance(new User())));

        OnboardingUserPgRequest request = new OnboardingUserPgRequest();
        request.setOrigin(Origin.fromValue(origin));
        request.setUsers(List.of(mockInstance(new UserRequest())));
        request.setTaxCode("taxCode");
        request.setInstitutionType(it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.InstitutionType.PG);
        request.setProductId("productId");

        when(onboardingMapper.toOnboardingUserPgRequest(onboardingData)).thenReturn(request);
        // when
        onboardingMsConnector.onboardingUsersPgFromIcAndAde(onboardingData);

        // then
        verify(msOnboardingApiClient, times(1))._onboardingUsersPg(request);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void onboardingUsersPgFromIcAndAde_shouldHandleNullResponse() {
        // given
        final String origin = "ADE";

        OnboardingUserPgRequest request = new OnboardingUserPgRequest();
        request.setOrigin(Origin.fromValue(origin));
        request.setUsers(List.of(mockInstance(new UserRequest())));
        request.setTaxCode("taxCode");
        request.setInstitutionType(it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.InstitutionType.PG);
        request.setProductId("productId");


        // when
        onboardingMsConnector.onboardingUsersPgFromIcAndAde(null);

        // then
        verify(msOnboardingApiClient, times(1))._onboardingUsersPg(null);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void getOnboardingByFiltersWithUO() {
        // given
        final String origin = "origin";
        final String originId = "originId";
        final String productId = "productId";
        OnboardingResponse resource = new OnboardingResponse();
        InstitutionResponse institution = new InstitutionResponse();
        institution.setSubunitType(InstitutionPaSubunitType.UO);
        resource.setInstitution(institution);

        resource.setProductId("productId");
        when(msOnboardingSupportApiClient._onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, null, null))
                .thenReturn(ResponseEntity.ok(List.of(resource)));
        // when
        List<OnboardingData> result = onboardingMsConnector.getByFilters(productId, null, origin, originId, null);
        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(msOnboardingSupportApiClient, times(1))
                ._onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, null, null);
        verifyNoMoreInteractions(msOnboardingSupportApiClient);
    }

    @Test
    void getOnboardingByFiltersWithAOO() {
        // given
        final String origin = "origin";
        final String originId = "originId";
        final String productId = "productId";
        OnboardingResponse resource = new OnboardingResponse();
        InstitutionResponse institution = new InstitutionResponse();
        institution.setSubunitType(InstitutionPaSubunitType.AOO);
        resource.setInstitution(institution);

        resource.setProductId("productId");
        when(msOnboardingSupportApiClient._onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, null, null))
                .thenReturn(ResponseEntity.ok(List.of(resource)));
        // when
        List<OnboardingData> result = onboardingMsConnector.getByFilters(productId, null, origin, originId, null);
        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(msOnboardingSupportApiClient, times(1))
                ._onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, null, null);
        verifyNoMoreInteractions(msOnboardingSupportApiClient);
    }

    @Test
    void getOnboardingByFiltersWithReferenceOnboardingId() {
        // given
        final String origin = "origin";
        final String originId = "originId";
        final String productId = "productId";
        OnboardingResponse resource = new OnboardingResponse();
        resource.setReferenceOnboardingId("referenceOnboardingId");
        InstitutionResponse institution = new InstitutionResponse();
        institution.setId(UUID.randomUUID().toString());
        resource.setInstitution(institution);

        resource.setProductId("productId");
        when(msOnboardingSupportApiClient._onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, null, null))
                .thenReturn(ResponseEntity.ok(List.of(resource)));
        // when
        List<OnboardingData> result = onboardingMsConnector.getByFilters(productId, null, origin, originId, null);
        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(msOnboardingSupportApiClient, times(1))
                ._onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, null, null);
        verifyNoMoreInteractions(msOnboardingSupportApiClient);
    }

    @Test
    void checkManager() {
        // given
        final String origin = "origin";
        final String originId = "originId";
        CheckManagerResponse response = new CheckManagerResponse();
        response.setResponse(true);
        CheckManagerData checkManagerData = new CheckManagerData();
        checkManagerData.setOrigin(origin);
        checkManagerData.setOriginId(originId);
        CheckManagerRequest request = new CheckManagerRequest();
        request.setOrigin(origin);
        request.setOriginId(originId);
        when(msOnboardingApiClient._checkManager(request))
                .thenReturn(ResponseEntity.ok(response));
        // when
        final Executable executable = () -> onboardingMsConnector.checkManager(checkManagerData);
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingApiClient, times(1))
                ._checkManager(request);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void checkRecipientCode() {
        // given
        final String recipientCode = "recipientCode";
        final String originId = "originId";
        RecipientCodeStatus expectedStatusResult = RecipientCodeStatus.ACCEPTED;
        ResponseEntity<RecipientCodeStatus> responseEntity = ResponseEntity.ok(expectedStatusResult);

        when(msOnboardingBillingApiClient._checkRecipientCode(recipientCode, originId))
                .thenReturn(responseEntity);
        when(onboardingMapper.toRecipientCodeStatusResult(responseEntity.getBody()))
                .thenReturn(RecipientCodeStatusResult.ACCEPTED);

        // when
        final Executable executable = () -> onboardingMsConnector.checkRecipientCode(recipientCode, originId);

        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingBillingApiClient, times(1))
                ._checkRecipientCode(recipientCode, originId);
        verify(onboardingMapper, times(1))
                .toRecipientCodeStatusResult(responseEntity.getBody());
        verifyNoMoreInteractions(msOnboardingApiClient);
        verifyNoMoreInteractions(onboardingMapper);
    }

    @Test
    void verifyOnboardingSubunitCode() {
        // given
        final String taxCode = "taxCode";
        final String subunitCode = "subunitCode";
        final String productId = "productId";
        final String origin = "origin";
        final String originId = "originId";
        // when
        final Executable executable = () -> onboardingMsConnector.verifyOnboarding(productId, taxCode, origin, originId, subunitCode, InstitutionType.PA.name());
        // then
        assertDoesNotThrow(executable);
        verify(msOnboardingApiClient, times(1))
                ._verifyOnboardingInfoByFilters(InstitutionType.PA.name(), origin, originId, productId, subunitCode, taxCode);
        verifyNoMoreInteractions(msOnboardingApiClient);
    }

    @Test
    void onboardingWithFilterTest() {
        // Given
        final String taxCode = "taxCode";
        final String status = "status";

        OnboardingGet onboardingGet = new OnboardingGet();
        onboardingGet.setProductId("prod-test");
        List<OnboardingGet> onboardingGets = List.of(onboardingGet);

        OnboardingGetResponse onboardingGetResponse = new OnboardingGetResponse();
        onboardingGetResponse.setItems(onboardingGets);
        ResponseEntity<OnboardingGetResponse> responseEntity = ResponseEntity.ok(onboardingGetResponse);

        when(msOnboardingApiClient._getOnboardingWithFilter( any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any())).thenReturn(responseEntity);

        OnboardingResult onboardingResult = new OnboardingResult();
        onboardingResult.setProductId("product-test");
        when(onboardingMapper.toOnboardingWithFilter(responseEntity.getBody()))
                .thenReturn(List.of(onboardingResult));

        // When
        onboardingMsConnector.onboardingWithFilter(taxCode, status);

        // Then
        verify(msOnboardingApiClient, times(1))
                ._getOnboardingWithFilter( any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any());
        verify(onboardingMapper, times(1))
                .toOnboardingWithFilter(responseEntity.getBody());
        verifyNoMoreInteractions(msOnboardingApiClient);
        verifyNoMoreInteractions(onboardingMapper);
    }

}
