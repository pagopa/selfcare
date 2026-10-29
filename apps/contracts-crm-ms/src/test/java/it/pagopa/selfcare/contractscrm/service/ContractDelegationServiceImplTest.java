package it.pagopa.selfcare.contractscrm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.contractscrm.client.EventHubRestClient;
import it.pagopa.selfcare.contractscrm.config.ContractsCrmConfig;
import it.pagopa.selfcare.contractscrm.exception.InvalidRequestException;
import it.pagopa.selfcare.contractscrm.exception.ResourceNotFoundException;
import it.pagopa.selfcare.contractscrm.mapper.ContractDelegationMapperImpl;
import it.pagopa.selfcare.contractscrm.model.dto.request.DelegationNotification;
import it.pagopa.selfcare.contractscrm.model.dto.response.ContractDelegationResponse;
import it.pagopa.selfcare.contractscrm.model.entity.ContractDelegation;
import it.pagopa.selfcare.contractscrm.model.entity.ContractStatus;
import it.pagopa.selfcare.contractscrm.model.entity.InstitutionContract;
import it.pagopa.selfcare.contractscrm.repository.ContractDelegationRepository;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapi.quarkus.onboarding_ms_json.api.OnboardingControllerApi;
import org.openapi.quarkus.onboarding_ms_json.model.InstitutionResponse;
import org.openapi.quarkus.onboarding_ms_json.model.OnboardingGet;
import org.openapi.quarkus.onboarding_ms_json.model.OnboardingGetResponse;
import org.openapi.quarkus.onboarding_ms_json.model.OnboardingStatus;
import org.openapi.quarkus.onboarding_ms_json.model.PaymentServiceProviderRequest;
import org.openapi.quarkus.webhook_ms_json.api.WebhookApi;
import org.openapi.quarkus.webhook_ms_json.model.NotificationRequest;

class ContractDelegationServiceImplTest {

  private static final String PRODUCT_ID = "prod-pagopa";
  private static final String FROM = "from-institution";
  private static final String TO = "to-institution";
  private static final String FROM_CONTRACT = "from-contract";
  private static final String TO_CONTRACT = "to-contract";
  private static final String TOPIC = "SC-ContractsCRMDelegations";
  private static final OffsetDateTime ACTIVATED_AT =
      OffsetDateTime.of(2024, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC);

  private OnboardingControllerApi onboardingApi;
  private WebhookApi webhookApi;
  private EventHubRestClient eventHubRestClient;
  private ContractDelegationRepository repository;
  private ContractsCrmConfig config;
  private ContractDelegationServiceImpl service;

  @BeforeEach
  void setUp() {
    onboardingApi = mock(OnboardingControllerApi.class);
    webhookApi = mock(WebhookApi.class);
    eventHubRestClient = mock(EventHubRestClient.class);
    repository = mock(ContractDelegationRepository.class);
    config = mock(ContractsCrmConfig.class);
    when(config.tenantId()).thenReturn("AR");
    when(config.topic()).thenReturn(TOPIC);
    when(config.onboardingStatus()).thenReturn("COMPLETED");
    when(config.webhookEnabled()).thenReturn(true);
    when(repository.persistOrUpdate(any(ContractDelegation.class)))
        .thenAnswer(
            invocation ->
                Uni.createFrom().item(invocation.getArgument(0, ContractDelegation.class)));
    when(eventHubRestClient.sendMessage(any())).thenReturn(Uni.createFrom().voidItem());
    when(webhookApi.sendNotification(any()))
        .thenReturn(Uni.createFrom().item(Response.ok().build()));
    service =
        new ContractDelegationServiceImpl(
            onboardingApi,
            webhookApi,
            eventHubRestClient,
            repository,
            new ContractDelegationMapperImpl(),
            config,
            new ObjectMapper().registerModule(new JavaTimeModule()));
  }

  @Test
  void processDelegation_notPspType_isIgnored() {
    DelegationNotification delegation = delegation("PT", "ACTIVE");

    assertNull(await(service.processDelegation(delegation)));
    verifyNoInteractions(onboardingApi, repository, eventHubRestClient, webhookApi);
  }

  @Test
  void processDelegation_missingTo_throwsInvalidRequest() {
    DelegationNotification delegation =
        delegation(DelegationNotification.TYPE_PSP_MEMBERSHIP, "ACTIVE");
    delegation.setTo(" ");

    assertThrows(InvalidRequestException.class, () -> service.processDelegation(delegation));
    verifyNoInteractions(onboardingApi, repository);
  }

  @Test
  void processDelegation_membershipOnNewContract_persistsWithoutPublishing() {
    stubOnboarding(FROM, onboarding(FROM_CONTRACT, ACTIVATED_AT));
    stubOnboarding(TO, onboarding(TO_CONTRACT, ACTIVATED_AT));
    when(repository.findByProductIdAndContractId(PRODUCT_ID, FROM_CONTRACT))
        .thenReturn(Uni.createFrom().nullItem());
    DelegationNotification delegation =
        delegation(DelegationNotification.TYPE_PSP_MEMBERSHIP, "ACTIVE");

    ContractDelegationResponse response = await(service.processDelegation(delegation));

    ContractDelegation saved = capturePersisted();
    assertNotNull(saved.getId());
    assertEquals(PRODUCT_ID, saved.getProductId());
    assertEquals(FROM, saved.getContract().getInstitutionId());
    assertEquals(FROM_CONTRACT, saved.getContract().getContractId());
    assertEquals(ACTIVATED_AT.toInstant(), saved.getContract().getDate());
    assertEquals(ContractStatus.ACTIVE, saved.getContract().getStatus());
    assertEquals(TO, saved.getMembership().getInstitutionId());
    assertEquals(TO_CONTRACT, saved.getMembership().getContractId());
    assertEquals(Instant.parse("2024-02-01T10:00:00Z"), saved.getMembership().getDate());
    assertNull(saved.getRecipient());
    assertEquals(TO_CONTRACT, response.getMembership().getId());
    verify(eventHubRestClient, never()).sendMessage(any());
    verify(webhookApi, never()).sendNotification(any());
    verify(onboardingApi)
        .getOnboardingWithFilter(
            any(), eq(FROM), any(), any(), eq(PRODUCT_ID), any(), any(), any(),
            eq(OnboardingStatus.COMPLETED), any(), any(), any(), any(), any());
  }

  @Test
  void processDelegation_recipientCompletingContract_publishesToEventHubAndWebhook() {
    stubOnboarding(FROM, onboarding(FROM_CONTRACT, ACTIVATED_AT));
    stubOnboarding(TO, onboarding(TO_CONTRACT, ACTIVATED_AT));
    ContractDelegation existing = existingWithMembership(Instant.parse("2024-01-15T00:00:00Z"));
    when(repository.findByProductIdAndContractId(PRODUCT_ID, FROM_CONTRACT))
        .thenReturn(Uni.createFrom().item(existing));
    DelegationNotification delegation =
        delegation(DelegationNotification.TYPE_PSP_RECIPIENT, "ACTIVE");

    ContractDelegationResponse response = await(service.processDelegation(delegation));

    assertSame(existing, capturePersisted());
    assertEquals("existing-id", response.getId());
    assertEquals(TO_CONTRACT, response.getRecipient().getId());
    assertNotNull(response.getMembership());
    verify(eventHubRestClient).sendMessage(response);
    ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(webhookApi).sendNotification(captor.capture());
    assertEquals(TOPIC, captor.getValue().getTopic());
    assertEquals("AR", captor.getValue().getTenantId());
    assertEquals(PRODUCT_ID, captor.getValue().getProductId());
    assertNotNull(captor.getValue().getPayload());
  }

  @Test
  void processDelegation_webhookFailure_isIgnored() {
    stubCompletingRecipient();
    when(webhookApi.sendNotification(any()))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("webhook down")));

    ContractDelegationResponse response =
        await(
            service.processDelegation(
                delegation(DelegationNotification.TYPE_PSP_RECIPIENT, "ACTIVE")));

    assertNotNull(response);
    verify(eventHubRestClient).sendMessage(any());
  }

  @Test
  void processDelegation_webhookDisabled_publishesOnlyToEventHub() {
    stubCompletingRecipient();
    when(config.webhookEnabled()).thenReturn(false);

    await(
        service.processDelegation(
            delegation(DelegationNotification.TYPE_PSP_RECIPIENT, "ACTIVE")));

    verify(eventHubRestClient).sendMessage(any());
    verify(webhookApi, never()).sendNotification(any());
  }

  @Test
  void processDelegation_eventHubFailure_fails() {
    stubCompletingRecipient();
    when(eventHubRestClient.sendMessage(any()))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("event hub down")));

    Uni<ContractDelegationResponse> result =
        service.processDelegation(
            delegation(DelegationNotification.TYPE_PSP_RECIPIENT, "ACTIVE"));

    assertThrows(RuntimeException.class, () -> await(result));
    verify(webhookApi, never()).sendNotification(any());
  }

  @Test
  void processDelegation_deletedDelegation_marksRecipientDeleted() {
    stubCompletingRecipient();

    ContractDelegationResponse response =
        await(
            service.processDelegation(
                delegation(DelegationNotification.TYPE_PSP_RECIPIENT, "DELETED")));

    assertEquals(ContractStatus.DELETED, response.getRecipient().getStatus());
  }

  @Test
  void processDelegation_olderThanStored_isSkipped() {
    stubOnboarding(FROM, onboarding(FROM_CONTRACT, ACTIVATED_AT));
    stubOnboarding(TO, onboarding(TO_CONTRACT, ACTIVATED_AT));
    ContractDelegation existing = existingWithMembership(Instant.parse("2024-03-01T00:00:00Z"));
    when(repository.findByProductIdAndContractId(PRODUCT_ID, FROM_CONTRACT))
        .thenReturn(Uni.createFrom().item(existing));

    ContractDelegationResponse response =
        await(
            service.processDelegation(
                delegation(DelegationNotification.TYPE_PSP_MEMBERSHIP, "DELETED")));

    assertEquals(ContractStatus.ACTIVE, response.getMembership().getStatus());
    verify(repository, never()).persistOrUpdate(any(ContractDelegation.class));
    verify(eventHubRestClient, never()).sendMessage(any());
  }

  @Test
  void processDelegation_withoutPspContract_throwsNotFound() {
    stubOnboarding(FROM, onboarding(null, ACTIVATED_AT));
    stubOnboarding(TO, onboarding(TO_CONTRACT, ACTIVATED_AT));

    Uni<ContractDelegationResponse> result =
        service.processDelegation(
            delegation(DelegationNotification.TYPE_PSP_MEMBERSHIP, "ACTIVE"));

    assertThrows(ResourceNotFoundException.class, () -> await(result));
    verifyNoInteractions(repository);
  }

  @Test
  void processDelegation_onboarding404_throwsNotFound() {
    stubOnboarding(FROM, onboarding(FROM_CONTRACT, ACTIVATED_AT));
    when(onboardingApi.getOnboardingWithFilter(
            any(), eq(TO), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any()))
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));

    Uni<ContractDelegationResponse> result =
        service.processDelegation(
            delegation(DelegationNotification.TYPE_PSP_MEMBERSHIP, "ACTIVE"));

    assertThrows(ResourceNotFoundException.class, () -> await(result));
  }

  @Test
  void processDelegation_usesMostRecentOnboardingWithContract() {
    OnboardingGet older = onboarding("old-contract", ACTIVATED_AT);
    older.setCreatedAt(ACTIVATED_AT.minusDays(10));
    OnboardingGet newer = onboarding(FROM_CONTRACT, null);
    newer.setCreatedAt(ACTIVATED_AT);
    OnboardingGet withoutContract = onboarding(null, ACTIVATED_AT);
    withoutContract.setCreatedAt(ACTIVATED_AT.plusDays(1));
    stubOnboarding(FROM, older, newer, withoutContract);
    stubOnboarding(TO, onboarding(TO_CONTRACT, ACTIVATED_AT));
    when(repository.findByProductIdAndContractId(PRODUCT_ID, FROM_CONTRACT))
        .thenReturn(Uni.createFrom().nullItem());

    ContractDelegationResponse response =
        await(
            service.processDelegation(
                delegation(DelegationNotification.TYPE_PSP_MEMBERSHIP, "ACTIVE")));

    assertEquals(FROM_CONTRACT, response.getContract().getId());
    assertEquals(ACTIVATED_AT.toInstant(), response.getContract().getDate());
  }

  private void stubCompletingRecipient() {
    stubOnboarding(FROM, onboarding(FROM_CONTRACT, ACTIVATED_AT));
    stubOnboarding(TO, onboarding(TO_CONTRACT, ACTIVATED_AT));
    when(repository.findByProductIdAndContractId(PRODUCT_ID, FROM_CONTRACT))
        .thenReturn(
            Uni.createFrom().item(existingWithMembership(Instant.parse("2024-01-15T00:00:00Z"))));
  }

  private void stubOnboarding(String institutionId, OnboardingGet... onboardings) {
    OnboardingGetResponse response = new OnboardingGetResponse();
    response.setItems(List.of(onboardings));
    response.setCount((long) onboardings.length);
    when(onboardingApi.getOnboardingWithFilter(
            any(), eq(institutionId), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any()))
        .thenReturn(Uni.createFrom().item(response));
  }

  private static OnboardingGet onboarding(String contractId, OffsetDateTime activatedAt) {
    PaymentServiceProviderRequest psp = new PaymentServiceProviderRequest();
    psp.setContractId(contractId);
    InstitutionResponse institution = new InstitutionResponse();
    institution.setPaymentServiceProvider(psp);
    OnboardingGet onboarding = new OnboardingGet();
    onboarding.setInstitution(institution);
    onboarding.setActivatedAt(activatedAt);
    onboarding.setCreatedAt(ACTIVATED_AT.minusDays(1));
    return onboarding;
  }

  private static ContractDelegation existingWithMembership(Instant membershipDate) {
    ContractDelegation existing = new ContractDelegation();
    existing.setId("existing-id");
    existing.setProductId(PRODUCT_ID);
    existing.setContract(
        InstitutionContract.builder()
            .institutionId(FROM)
            .contractId(FROM_CONTRACT)
            .status(ContractStatus.ACTIVE)
            .build());
    existing.setMembership(
        InstitutionContract.builder()
            .institutionId("member")
            .contractId("member-contract")
            .date(membershipDate)
            .status(ContractStatus.ACTIVE)
            .build());
    return existing;
  }

  private static DelegationNotification delegation(String type, String status) {
    return DelegationNotification.builder()
        .delegationId("delegation-id")
        .from(FROM)
        .to(TO)
        .productId(PRODUCT_ID)
        .type(type)
        .status(status)
        .createdAt("2024-01-20T10:00:00")
        .updatedAt("2024-02-01T10:00:00Z")
        .build();
  }

  private ContractDelegation capturePersisted() {
    ArgumentCaptor<ContractDelegation> captor = ArgumentCaptor.forClass(ContractDelegation.class);
    verify(repository).persistOrUpdate(captor.capture());
    return captor.getValue();
  }

  private static <T> T await(Uni<T> uni) {
    return uni.await().indefinitely();
  }
}
