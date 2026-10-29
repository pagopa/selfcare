package it.pagopa.selfcare.contractscrm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.contractscrm.client.EventHubRestClient;
import it.pagopa.selfcare.contractscrm.config.ContractsCrmConfig;
import it.pagopa.selfcare.contractscrm.exception.InvalidRequestException;
import it.pagopa.selfcare.contractscrm.exception.ResourceNotFoundException;
import it.pagopa.selfcare.contractscrm.mapper.ContractDelegationMapper;
import it.pagopa.selfcare.contractscrm.model.dto.request.DelegationNotification;
import it.pagopa.selfcare.contractscrm.model.dto.response.ContractDelegationResponse;
import it.pagopa.selfcare.contractscrm.model.entity.ContractDelegation;
import it.pagopa.selfcare.contractscrm.model.entity.ContractStatus;
import it.pagopa.selfcare.contractscrm.model.entity.InstitutionContract;
import it.pagopa.selfcare.contractscrm.repository.ContractDelegationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.onboarding_ms_json.api.OnboardingControllerApi;
import org.openapi.quarkus.onboarding_ms_json.model.InstitutionResponse;
import org.openapi.quarkus.onboarding_ms_json.model.OnboardingGet;
import org.openapi.quarkus.onboarding_ms_json.model.OnboardingGetResponse;
import org.openapi.quarkus.onboarding_ms_json.model.OnboardingStatus;
import org.openapi.quarkus.onboarding_ms_json.model.PaymentServiceProviderRequest;
import org.openapi.quarkus.webhook_ms_json.api.WebhookApi;
import org.openapi.quarkus.webhook_ms_json.model.NotificationRequest;

@Slf4j
@ApplicationScoped
public class ContractDelegationServiceImpl implements ContractDelegationService {

  private final OnboardingControllerApi onboardingApi;
  private final WebhookApi webhookApi;
  private final EventHubRestClient eventHubRestClient;
  private final ContractDelegationRepository repository;
  private final ContractDelegationMapper mapper;
  private final ContractsCrmConfig config;
  private final ObjectMapper objectMapper;

  public ContractDelegationServiceImpl(
      @RestClient OnboardingControllerApi onboardingApi,
      @RestClient WebhookApi webhookApi,
      @RestClient EventHubRestClient eventHubRestClient,
      ContractDelegationRepository repository,
      ContractDelegationMapper mapper,
      ContractsCrmConfig config,
      ObjectMapper objectMapper) {
    this.onboardingApi = onboardingApi;
    this.webhookApi = webhookApi;
    this.eventHubRestClient = eventHubRestClient;
    this.repository = repository;
    this.mapper = mapper;
    this.config = config;
    this.objectMapper = objectMapper;
  }

  @Override
  public Uni<ContractDelegationResponse> processDelegation(DelegationNotification delegation) {
    if (!isPspDelegation(delegation)) {
      log.debug(
          "Delegation {} of type {} ignored", delegation.getDelegationId(), delegation.getType());
      return Uni.createFrom().nullItem();
    }
    validate(delegation);
    String productId = delegation.getProductId();
    return Uni.combine()
        .all()
        .unis(
            findActiveContract(productId, delegation.getFrom()),
            findActiveContract(productId, delegation.getTo()))
        .asTuple()
        .onItem()
        .transformToUni(
            contracts ->
                upsertContractDelegation(
                    delegation, productId, contracts.getItem1(), contracts.getItem2()));
  }

  private Uni<ContractDelegationResponse> upsertContractDelegation(
      DelegationNotification delegation,
      String productId,
      InstitutionContract fromContract,
      InstitutionContract toContract) {
    return repository
        .findByProductIdAndContractId(productId, fromContract.getContractId())
        .onItem()
        .transformToUni(
            existing -> {
              ContractDelegation contractDelegation =
                  Optional.ofNullable(existing).orElseGet(() -> newContractDelegation(productId));
              if (!applyDelegation(contractDelegation, delegation, fromContract, toContract)) {
                log.info(
                    "Delegation {} is older than the stored {} of contract delegation {}: skipped",
                    delegation.getDelegationId(),
                    delegation.getType(),
                    contractDelegation.getId());
                return Uni.createFrom().item(mapper.toResponse(contractDelegation));
              }
              contractDelegation.setUpdatedAt(Instant.now());
              return repository
                  .persistOrUpdate(contractDelegation)
                  .map(mapper::toResponse)
                  .call(this::publishIfComplete);
            });
  }

  private ContractDelegation newContractDelegation(String productId) {
    ContractDelegation contractDelegation = new ContractDelegation();
    contractDelegation.setId(UUID.randomUUID().toString());
    contractDelegation.setProductId(productId);
    contractDelegation.setCreatedAt(Instant.now());
    return contractDelegation;
  }

  /**
   * Sets contract and membership/recipient on the contract delegation.
   *
   * @return false when the stored membership/recipient is newer than the incoming delegation
   */
  private boolean applyDelegation(
      ContractDelegation contractDelegation,
      DelegationNotification delegation,
      InstitutionContract fromContract,
      InstitutionContract toContract) {
    boolean membership = DelegationNotification.TYPE_PSP_MEMBERSHIP.equals(delegation.getType());
    InstitutionContract current =
        membership ? contractDelegation.getMembership() : contractDelegation.getRecipient();
    toContract.setDate(delegationDate(delegation));
    toContract.setStatus(
        DelegationNotification.STATUS_DELETED.equalsIgnoreCase(delegation.getStatus())
            ? ContractStatus.DELETED
            : ContractStatus.ACTIVE);
    if (current != null
        && current.getDate() != null
        && current.getDate().isAfter(toContract.getDate())) {
      return false;
    }
    contractDelegation.setContract(fromContract);
    if (membership) {
      contractDelegation.setMembership(toContract);
    } else {
      contractDelegation.setRecipient(toContract);
    }
    return true;
  }

  private Uni<Void> publishIfComplete(ContractDelegationResponse contractDelegation) {
    if (contractDelegation.getMembership() == null || contractDelegation.getRecipient() == null) {
      return Uni.createFrom().voidItem();
    }
    return eventHubRestClient
        .sendMessage(contractDelegation)
        .invoke(
            () ->
                log.info(
                    "Contract delegation {} sent to topic {}",
                    contractDelegation.getId(),
                    config.topic()))
        .call(() -> forwardToWebhook(contractDelegation));
  }

  private Uni<Void> forwardToWebhook(ContractDelegationResponse contractDelegation) {
    if (!config.webhookEnabled()) {
      return Uni.createFrom().voidItem();
    }
    NotificationRequest request;
    try {
      request =
          NotificationRequest.builder()
              .productId(contractDelegation.getProductId())
              .tenantId(config.tenantId())
              .topic(config.topic())
              .payload(objectMapper.writeValueAsString(contractDelegation))
              .build();
    } catch (JsonProcessingException e) {
      log.error("Unable to serialize contract delegation {}", contractDelegation.getId(), e);
      return Uni.createFrom().voidItem();
    }
    return webhookApi
        .sendNotification(request)
        .replaceWithVoid()
        .onFailure()
        .recoverWithItem(
            failure -> {
              log.error(
                  "Unable to forward contract delegation {} to webhook: {}",
                  contractDelegation.getId(),
                  failure.getMessage());
              return null;
            });
  }

  private Uni<InstitutionContract> findActiveContract(String productId, String institutionId) {
    return onboardingApi
        .getOnboardingWithFilter(
            null,
            institutionId,
            null,
            null,
            productId,
            null,
            null,
            null,
            OnboardingStatus.fromString(config.onboardingStatus()),
            null,
            null,
            null,
            null,
            null)
        .onFailure(WebApplicationException.class)
        .transform(
            failure ->
                ((WebApplicationException) failure).getResponse().getStatus() == 404
                    ? notFound(productId, institutionId)
                    : failure)
        .map(
            response ->
                toInstitutionContract(response, institutionId)
                    .orElseThrow(() -> notFound(productId, institutionId)));
  }

  private static Optional<InstitutionContract> toInstitutionContract(
      OnboardingGetResponse response, String institutionId) {
    if (response == null || response.getItems() == null) {
      return Optional.empty();
    }
    return response.getItems().stream()
        .filter(onboarding -> isNotBlank(contractIdOf(onboarding)))
        .max(
            Comparator.comparing(
                OnboardingGet::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
        .map(
            onboarding ->
                InstitutionContract.builder()
                    .institutionId(institutionId)
                    .contractId(contractIdOf(onboarding))
                    .date(
                        Optional.ofNullable(onboarding.getActivatedAt())
                            .or(() -> Optional.ofNullable(onboarding.getCreatedAt()))
                            .map(OffsetDateTime::toInstant)
                            .orElse(null))
                    .status(ContractStatus.ACTIVE)
                    .build());
  }

  private static String contractIdOf(OnboardingGet onboarding) {
    return Optional.ofNullable(onboarding.getInstitution())
        .map(InstitutionResponse::getPaymentServiceProvider)
        .map(PaymentServiceProviderRequest::getContractId)
        .orElse(null);
  }

  private static ResourceNotFoundException notFound(String productId, String institutionId) {
    return new ResourceNotFoundException(
        String.format(
            "No active onboarding with PSP contract found for institution %s and product %s",
            institutionId, productId));
  }

  private static boolean isPspDelegation(DelegationNotification delegation) {
    return DelegationNotification.TYPE_PSP_MEMBERSHIP.equals(delegation.getType())
        || DelegationNotification.TYPE_PSP_RECIPIENT.equals(delegation.getType());
  }

  private static void validate(DelegationNotification delegation) {
    if (!isNotBlank(delegation.getFrom())
        || !isNotBlank(delegation.getTo())
        || !isNotBlank(delegation.getProductId())) {
      throw new InvalidRequestException("Delegation from, to and productId are required");
    }
  }

  private static boolean isNotBlank(String value) {
    return value != null && !value.isBlank();
  }

  private static Instant delegationDate(DelegationNotification delegation) {
    return Optional.ofNullable(delegation.getUpdatedAt())
        .or(() -> Optional.ofNullable(delegation.getCreatedAt()))
        .map(ContractDelegationServiceImpl::parseDate)
        .orElseGet(Instant::now);
  }

  private static Instant parseDate(String value) {
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException e) {
      try {
        return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
      } catch (DateTimeParseException ex) {
        log.warn("Unparsable delegation date {}", value);
        return null;
      }
    }
  }
}
