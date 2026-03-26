package it.pagopa.selfcare.onboarding.service.impl;

import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.COMPLETED;
import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.PENDING;
import static it.pagopa.selfcare.onboarding.common.Origin.IPA;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;
import static it.pagopa.selfcare.onboarding.common.WorkflowType.USERS;
import static it.pagopa.selfcare.onboarding.constants.CustomError.*;
import static it.pagopa.selfcare.onboarding.util.ErrorMessage.*;
import static it.pagopa.selfcare.product.utils.ProductUtils.validRoles;

import io.quarkus.mongodb.panache.common.reactive.Panache;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple2;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.request.UserRequesterDto;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.controller.response.UserResponse;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.entity.registry.RegistryManager;
import it.pagopa.selfcare.onboarding.entity.registry.RegistryResourceFactory;
import it.pagopa.selfcare.onboarding.exception.*;
import it.pagopa.selfcare.onboarding.factory.OnboardingResponseFactory;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingDocumentMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.service.*;
import it.pagopa.selfcare.onboarding.service.util.OnboardingUtils;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.onboarding.util.SortEnum;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.OnboardingApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.core_json.model.InstitutionsResponse;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessesResource;
import org.openapi.quarkus.party_registry_proxy_json.model.GetInstitutionsByLegalDto;
import org.openapi.quarkus.party_registry_proxy_json.model.GetInstitutionsByLegalFilterDto;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;

@Slf4j
@ApplicationScoped
public class OnboardingServiceDefault implements OnboardingService {

    public static final String GSP_CATEGORY_INSTITUTION_TYPE = "L37";
    public static final String SCEC_CATEGORY_INSTITUTION_TYPE = "S01G";
    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    public static final String USERS_FIELD_TAXCODE = "fiscalCode";
    private static final String ID_MAIL_PREFIX = "ID_MAIL#";
    private static final String INTEGRATION_PROFILE = "integrationProfile";
    private static final String TIMEOUT_ORCHESTRATION_RESPONSE = "70";
    private static final Pattern INDIVIDUAL_CF_PATTERN =
            Pattern.compile("^[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]$");

    @RestClient
    @Inject
    UserApi userRegistryApi;

    @RestClient
    @Inject
    OnboardingApi onboardingApi;

    @RestClient
    @Inject
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;

    @RestClient
    @Inject
    AooApi aooApi;

    @RestClient
    @Inject
    UoApi uoApi;

    @RestClient
    @Inject
    InfocamereApi infocamereApi;

    @RestClient
    @Inject
    NationalRegistriesApi nationalRegistriesApi;

    @RestClient
    @Inject
    DocumentControllerApi documentControllerApi;

    @RestClient
    @Inject
    DocumentContentControllerApi documentContentControllerApi;

    @Inject
    OnboardingMapper onboardingMapper;

    @Inject
    OnboardingResponseFactory onboardingResponseFactory;

    @Inject
    InstitutionMapper institutionMapper;

    @Inject
    ProductService productService;

    @Inject
    UserMapper userMapper;

    @Inject
    OnboardingDocumentMapper onboardingDocumentMapper;

    @Inject
    RegistryResourceFactory registryResourceFactory;

    @Inject
    OnboardingUtils onboardingUtils;

    @Inject
    OrchestrationService orchestrationService;

    @Inject
    UserService userService;

    @Inject
    InstitutionService institutionService;

    @ConfigProperty(name = "onboarding.orchestration.enabled")
    Boolean onboardingOrchestrationEnabled;

    @ConfigProperty(name = "quarkus.profile")
    String activeProfile;

    @ConfigProperty(name = "onboarding-ms.add-user-requester.enabled")
    boolean addUserRequesterEnabled;

    @Override
    public Uni<OnboardingResponse> onboarding(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates,
            UserRequesterDto userRequester) {
        Integer onboardingExpirationDays = productService.getProductExpirationDate(onboarding.getProductId());
        onboarding.setExpiringDate(OffsetDateTime.now().plusDays(onboardingExpirationDays).toLocalDateTime());
        WorkflowType workflowType = getWorkflowType(onboarding);
        onboarding.setWorkflowType(workflowType);
        onboarding.setStatus(OnboardingStatus.REQUEST);
        log.info("Starting onboarding for institution with description: {}, origin: {}, institutionType: {}, workflowType: {}",
                onboarding.getInstitution().getDescription(),
                onboarding.getInstitution().getOrigin(),
                onboarding.getInstitution().getInstitutionType(),
                workflowType);
        return fillUsersAndOnboarding(onboarding, userRequests, aggregates, false, userRequester);
    }

    @Override
    public Uni<OnboardingResponse> onboardingIncrement(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates,
            UserRequesterDto userRequester) {
        Integer onboardingExpirationDays = productService.getProductExpirationDate(onboarding.getProductId());
        onboarding.setExpiringDate(
                OffsetDateTime.now().plusDays(onboardingExpirationDays).toLocalDateTime());
        onboarding.setWorkflowType(WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR);
        onboarding.setStatus(PENDING);
        log.info("Starting onboardingIncrement for institution with description: {}, origin: {}, institutionType: {}, workflowType: {}",
                onboarding.getInstitution().getDescription(),
                onboarding.getInstitution().getOrigin(),
                onboarding.getInstitution().getInstitutionType(),
                WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR);
        return addReferencedOnboardingId(onboarding)
                .flatMap(onboardingObj -> fillUsersAndOnboarding(onboardingObj, userRequests, aggregates, true, userRequester));
    }

    /**
     * As onboarding but it is specific for USERS workflow
     */
    @Override
    public Uni<OnboardingResponse> onboardingUsers(
            OnboardingUserRequest request, String userId, WorkflowType workflowType) {
        log.info("Starting onboardingUsers for institution with origin: {}, institutionType: {}, workflowType: {}",
                request.getOrigin(),
                request.getInstitutionType(),
                workflowType);
        Integer onboardingExpirationDays = productService.getProductExpirationDate(request.getProductId());
        return getInstitutionFromUserRequest(request)
                .onItem()
                .transform(response -> institutionMapper.toEntity(response))
                .onItem()
                .transform(
                        institution -> {
                            Onboarding onboarding = onboardingMapper.toEntity(request, userId, workflowType);
                            institution.setInstitutionType(request.getInstitutionType());
                            onboarding.setInstitution(institution);
                            onboarding.setExpiringDate(
                                    OffsetDateTime.now().plusDays(onboardingExpirationDays).toLocalDateTime());
                            return onboarding;
                        })
                .onItem()
                .transformToUni(onboarding -> verifyExistingOnboarding(onboarding, request.getUsers()));
    }

    /**
     * As above but it is specific for CONFIRMATION workflow where onboarding goes directly to persist
     * phase It is created with PENDING state and wait for completion of the orchestration of
     * persisting onboarding 'apiStartAndWaitOnboardingOrchestrationGet'
     */
    @Override
    public Uni<OnboardingResponse> onboardingCompletion(
            Onboarding onboarding, List<UserRequest> userRequests, UserRequesterDto userRequester) {
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
        onboarding.setStatus(OnboardingStatus.REQUEST);

        return fillUsersAndOnboarding(
                onboarding, userRequests, null, false, userRequester);
    }

    @Override
    public Uni<OnboardingResponse> onboardingPgCompletion(
            Onboarding onboarding, List<UserRequest> userRequests) {
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
        onboarding.setStatus(PENDING);

        return fillUsersAndOnboarding(
                onboarding, userRequests, null, false, null);
    }

    @Override
    public Uni<OnboardingResponse> onboardingAggregationCompletion(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates, UserRequesterDto userRequester) {
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATOR);
        onboarding.setStatus(OnboardingStatus.REQUEST);

        return fillUsersAndOnboarding(onboarding, userRequests, aggregates, false, null);
    }

    @Override
    public Uni<OnboardingResponse> onboardingAggregationImport(
            Onboarding onboarding,
            OnboardingImportContract contractImported,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates, UserRequesterDto userRequester) {
        onboarding.setWorkflowType(WorkflowType.IMPORT_AGGREGATION);
        onboarding.setStatus(PENDING);

        return fillUsersAndOnboardingForImport(
                onboarding, userRequests, aggregates, contractImported, userRequester);
    }


    /**
     * As onboarding but it is specific for IMPORT workflow
     */
    @Override
    public Uni<OnboardingResponse> onboardingImport(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            OnboardingImportContract contractImported,
            UserRequesterDto userRequester) {
        onboarding.setWorkflowType(WorkflowType.IMPORT);
        onboarding.setStatus(PENDING);
        return fillUsersAndOnboardingForImport(
                onboarding, userRequests, null, contractImported, userRequester);
    }

    private Uni<OnboardingResponse> fillUsersAndOnboarding(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates,
            boolean isAggregatesIncrement,
            UserRequesterDto userRequester) {

        onboarding.setCreatedAt(LocalDateTime.now());

        return verifyExistingOnboarding(onboarding, isAggregatesIncrement)
                .onItem()
                .transformToUni(product -> handleOnboarding(onboarding, userRequests, aggregates, product, userRequester));
    }


    private Uni<Product> verifyExistingOnboarding(Onboarding onboarding, boolean isAggregatesIncrement) {
        return getProductByOnboarding(onboarding)
                .onItem()
                .transformToUni(
                        product -> verifyAlreadyOnboarding(
                                onboarding.getInstitution(),
                                product.getId(),
                                product.getParentId(),
                                isAggregatesIncrement)
                                .replaceWith(product));
    }

    private Uni<OnboardingResponse> handleOnboarding(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates,
            Product product,
            UserRequesterDto userRequester) {

        return Uni.createFrom()
                .item(registryResourceFactory.create(onboarding, getManagerTaxCode(userRequests)))
                .onItem()
                .invoke(registryManager -> registryManager.setResource(registryManager.retrieveInstitution()))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem()
                .transformToUni(registryManager -> validateAndPersistOnboarding(registryManager, onboarding, userRequests, aggregates, product, userRequester));
    }

    private String getManagerTaxCode(List<UserRequest> userRequests) {
        if (Objects.nonNull(userRequests)) {
            return userRequests.stream()
                    .filter(userRequest -> userRequest.getRole().equals(PartyRole.MANAGER))
                    .map(UserRequest::getTaxCode)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private Uni<OnboardingResponse> validateAndPersistOnboarding(
            RegistryManager<?> registryManager,
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates,
            Product product,
            UserRequesterDto userRequester) {
        log.info(
            "Starting validateAndPersistOnboarding for institution with description: {}",
            onboarding.getInstitution().getDescription());
        return registryManager.isValid()
                .onItem()
                .transformToUni(ignored -> registryManager.validateInstitutionType(product))
                .onItem()
                .invoke(() -> validateTaxCode(onboarding.getInstitution().getTaxCode(), product))
                .onItem()
                .transformToUni(ignored -> verifyAllowManagerAsDelegate(userRequests))
                .onItem()
                .transformToUni(ignored -> addUserRequester(userRequester, onboarding.getUserRequester()))
                .onItem()
                .transformToUni(ignored -> registryManager.customValidation(product))
                .onItem()
                .invoke(() -> onboarding.setTestEnvProductIds(product.getTestEnvProductIds()))
                .onItem()
                .transformToUni(ignored -> validateAggregates(aggregates, userRequests))
                .onItem()
                .transformToUni(current -> persistOnboarding(onboarding, userRequests, product, aggregates))
                .onItem()
                .transformToUni(currentOnboarding -> persistAndStartOrchestrationOnboarding(currentOnboarding,
                        orchestrationService.triggerOrchestration(currentOnboarding.getId(), null)))
                .onItem()
                .transform(onboardingMapper::toResponse);
    }

    /**
     * Adds the email UUID of the user requester by calling the user registry API and, if necessary,
     * updating the remote user.
     *
     * @param userRequesterRequest DTO containing the requester data (used for the email)
     * @param userRequester object {@code UserRequester} that can be modified to set {@code userMailUuid}
     */
    private Uni<Void> addUserRequester(UserRequesterDto userRequesterRequest,
                                       UserRequester userRequester) {
        log.info("Starting addUserRequester");

        if (!addUserRequesterEnabled || Objects.isNull(userRequesterRequest) || StringUtils.isBlank(userRequesterRequest.getEmail())) {
            log.info("addUserRequester skipped (feature flag disabled) or userRequester is null");
            return Uni.createFrom().voidItem();
        }

        return userRegistryApi
                .findByIdUsingGET(USERS_FIELD_LIST, userRequester.getUserRequestUid())
                .onItem()
                .transformToUni(userResource -> {
                    Optional<String> optUserMailRandomUuid = Optional.of(retrieveUserMailUuid(userResource, userRequesterRequest.getEmail()));

                    Optional<MutableUserFieldsDto> optUserFieldsDto =
                            toUpdateUserRequest(userRequesterRequest, userResource, optUserMailRandomUuid);

                    return optUserFieldsDto
                            .map(userUpdateRequest ->
                                    userRegistryApi
                                            .updateUsingPATCH(userResource.getId().toString(), userUpdateRequest)
                                            .replaceWith(userResource.getId()))
                            .orElse(Uni.createFrom().item(userResource.getId()))
                            .onItem()
                            .invoke(() -> optUserMailRandomUuid.ifPresent(userRequester::setUserMailUuid));
                })
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex -> {
                    if (((WebApplicationException) ex).getResponse().getStatus() != 404) {
                        return Uni.createFrom().failure(ex);
                    }
                    return Uni.createFrom().nullItem();
                })
                .replaceWithVoid();
    }

    /**
     * Validates that among MANAGER and DELEGATE user requests, the same tax code is never associated
     * with more than one distinct email address.
     * <p>
     * Emails are normalized (trim + lowercase) before comparison.
     * If a violation is found, the returned {@link Uni} fails with {@link InvalidRequestException};
     * otherwise it completes successfully.
     */
    private Uni<Void> verifyAllowManagerAsDelegate(List<UserRequest> userRequests) {

        log.info("Starting verifyAllowManagerAsDelegate");

        boolean ok = userRequests.stream()
                .filter(userRequest -> userRequest.getRole() == PartyRole.MANAGER || userRequest.getRole() == PartyRole.DELEGATE)
                .filter(userRequest -> userRequest.getTaxCode() != null && !userRequest.getTaxCode().isBlank())
                .filter(userRequest -> userRequest.getEmail() != null && !userRequest.getEmail().isBlank())
                .collect(Collectors.groupingBy(
                        userRequest -> userRequest.getTaxCode().trim().toLowerCase(),
                        Collectors.mapping(userRequest -> userRequest.getEmail().trim().toLowerCase(), Collectors.toSet())
                ))
                .values().stream()
                .allMatch(emails -> emails.size() <= 1);

        if (!ok) {
            return Uni.createFrom().failure(
                    new InvalidRequestException(
                            VALIDATION_USER_BY_TAXCODE.getMessage(),
                            VALIDATION_USER_BY_TAXCODE.getCode()
                    )
            );
        }

        return Uni.createFrom().voidItem();
    }

    /**
     * This method validates aggregates data, checking for each of them if information
     * from proxy registry match incoming data of the request.
     */
    private Uni<Void> validateAggregates(List<AggregateInstitutionRequest> aggregates, List<UserRequest> userRequests) {
        if (aggregates == null) {
            return Uni.createFrom().voidItem();
        }
        List<Uni<Void>> validationUnis = aggregates.stream()
                .map(aggregate -> Uni.createFrom()
                        .item(registryResourceFactory.create(buildOnboardingFromAggregate(aggregate), getManagerTaxCode(userRequests)))
                        .onItem()
                        .invoke(aggregateRegistryManager -> aggregateRegistryManager.setResource(aggregateRegistryManager.retrieveInstitution()))
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .onItem()
                        .transformToUni(aggregateRegistryManager ->
                                aggregateRegistryManager.isValid()
                                        .onItem().invoke(() ->
                                                log.debug("Aggregate valid: taxCode: {}, originId: {}",
                                                        aggregate.getTaxCode(),
                                                        aggregate.getOriginId()))
                                        .onFailure().invoke(failure ->
                                                log.warn("Aggregate validation failed: taxCode: {}, originId: {}, reason: {}",
                                                        aggregate.getTaxCode(),
                                                        aggregate.getOriginId(),
                                                        failure.getMessage()))
                                        .onFailure().recoverWithUni(failure -> Uni.createFrom().failure(failure))
                        )
                        .replaceWithVoid())
                .collect(Collectors.toList());

        return Uni.combine().all().unis(validationUnis).discardItems();
    }

    /**
     * This method checks whether the product and any parent have already been onboarded for the
     * provided institution. In the case where we are in the aggregate increment flow, the product on
     * the aggregator entity must already be onboarded, so in the case of a ResourceConflictException,
     * the exception should not be propagated. In the case of standard onboarding, the exception
     * should be propagated, and the flow should be blocked.
     */
    private Uni<Void> verifyAlreadyOnboarding(
            Institution institution, String productId, String parentId, boolean isAggregatesIncrement) {
        if (isAggregatesIncrement) {
            return verifyAlreadyOnboardingForProductAndProductParent(institution, productId, parentId)
                    .onFailure(ResourceConflictException.class)
                    .recoverWithNull()
                    .replaceWithVoid();
        }
        return verifyAlreadyOnboardingForProductAndProductParent(institution, productId, parentId)
                .replaceWithVoid();
    }

    private Uni<OnboardingResponse> verifyExistingOnboarding(
            Onboarding onboarding, List<UserRequest> userRequests) {
        onboarding.setCreatedAt(LocalDateTime.now());

        return getProductByOnboarding(onboarding)
                .onItem()
                .transformToUni(
                        product ->
                                this.addReferencedOnboardingId(onboarding)
                                        /* if product has some test environments, request must also onboard them (for ex. prod-interop-coll) */
                                        .onItem()
                                        .invoke(
                                                current -> onboarding.setTestEnvProductIds(product.getTestEnvProductIds()))
                                        .onItem()
                                        .invoke(() -> verifyAllowManagerAsDelegate(userRequests))
                                        .onItem()
                                        .transformToUni(
                                                current -> persistOnboarding(onboarding, userRequests, product, null))
                                        /* Update onboarding data with users and start orchestration */
                                        .onItem()
                                        .transformToUni(
                                                currentOnboarding ->
                                                        persistAndStartOrchestrationOnboarding(
                                                                currentOnboarding,
                                                                orchestrationService.triggerOrchestration(
                                                                        currentOnboarding.getId(), null)))
                                        .onItem()
                                        .transform(onboardingMapper::toResponse));
    }

    private Uni<OnboardingResponse> fillUsersAndOnboardingForImport(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregateRequests,
            OnboardingImportContract contractImported,
            UserRequesterDto userRequester) {

        onboarding.setCreatedAt(LocalDateTime.now());

        return verifyExistingOnboardingForImport(onboarding)
                .onItem()
                .transformToUni(product -> handleOnboardingForImport(
                        onboarding, userRequests, aggregateRequests, product, contractImported))
                .onFailure(IncrementRequiredException.class)
                .recoverWithUni(throwable -> {
                    // Call onboardingIncrement only if onboarding is aggregator and product is PROD_IO
                    // If IncrementRequiredException is thrown, it means there are existing onboardings
                    // with workflowType different from CONFIRMATION_AGGREGATE
                    // In this case, we need to call onboardingIncrement
                    // For other cases, rethrow the exception
                    log.info("Existing onboarding found for institution {} and product {}, calling onboardingIncrement",
                            onboarding.getInstitution().getTaxCode(), onboarding.getProductId());
                    return onboardingIncrement(onboarding, userRequests, aggregateRequests, userRequester);
                });
    }

    private Uni<Product> verifyExistingOnboardingForImport(Onboarding onboarding) {
        return getProductByOnboarding(onboarding)
                .onItem()
                .transformToUni(product ->
                        verifyAlreadyOnboardingForProductAndProductParent(
                                onboarding.getInstitution(), product.getId(), product.getParentId())
                                .replaceWith(product)
                                .onFailure(ResourceConflictException.class)
                                .recoverWithUni(throwable ->
                                        handleConflictForImport(onboarding, product)
                                )
                );
    }

    private Uni<Product> handleConflictForImport(Onboarding onboarding, Product product) {
        log.info("Handling conflict for import: institutionTaxCode: {}, productId: {}, isAggregator: {}",
                onboarding.getInstitution().getTaxCode(),
                product.getId(),
                onboarding.getIsAggregator());
        // Apply special logic only if onboarding is aggregator and product is PROD_IO
        if (!isAggregatorProdIo(onboarding)) {
            log.info("Not an aggregator PROD_IO case, rethrowing conflict exception for institutionTaxCode: {}, productId: {}",
                    onboarding.getInstitution().getTaxCode(),
                    product.getId());
            // For other cases (not aggregator or not PROD_IO), rethrow the original exception
            return Uni.createFrom().failure(createConflictException(product, onboarding.getInstitution()));
        }

        // Special logic for aggregator + PROD_IO: check existing onboardings workflowType
        Institution institution = onboarding.getInstitution();
        String origin = institution.getOrigin() != null ? institution.getOrigin().getValue() : null;

        return verifyOnboarding(
                institution.getTaxCode(),
                institution.getSubunitCode(),
                origin,
                institution.getOriginId(),
                COMPLETED,
                product.getId(),
                institution.getInstitutionType())
                .onItem()
                .transformToUni(onboardingResponses -> {
                    // Check if any onboarding has workflowType different from CONFIRMATION_AGGREGATE
                    boolean hasNonConfirmationAggregate = onboardingResponses.stream()
                            .anyMatch(response -> !Objects.equals(response.getWorkflowType(),
                                    WorkflowType.CONFIRMATION_AGGREGATE.name()));

                    if (hasNonConfirmationAggregate) {
                        log.info("Found existing onboarding with workflowType different from {}, for institutionTaxCode: {}",
                                WorkflowType.CONFIRMATION_AGGREGATE.name(),
                                institution.getTaxCode());
                    } else {
                        log.info("All existing onboardings have {} workflowType, ignoring conflict for institutionTaxCode: {}",
                                WorkflowType.CONFIRMATION_AGGREGATE.name(),
                                institution.getTaxCode());
                    }

                    // If at least one has different workflowType, throw custom exception to trigger onboardingIncrement
                    // Otherwise, all are CONFIRMATION_AGGREGATE, ignore the 409 and continue
                    return hasNonConfirmationAggregate
                            ? Uni.createFrom().failure(createIncrementRequiredException(product, institution))
                                : Uni.createFrom().item(product);
                });
    }

    private boolean isAggregatorProdIo(Onboarding onboarding) {
        return Boolean.TRUE.equals(onboarding.getIsAggregator()) && PROD_IO.getValue().equals(onboarding.getProductId());
    }

    private ResourceConflictException createConflictException(Product product, Institution institution) {
        return new ResourceConflictException(
                String.format(PRODUCT_ALREADY_ONBOARDED.getMessage(), product.getId(), institution.getTaxCode()),
                PRODUCT_ALREADY_ONBOARDED.getCode());
    }

    private IncrementRequiredException createIncrementRequiredException(Product product, Institution institution) {
        return new IncrementRequiredException(
                String.format(PRODUCT_ALREADY_ONBOARDED.getMessage(), product.getId(), institution.getTaxCode()),
                PRODUCT_ALREADY_ONBOARDED.getCode());
    }

    private Uni<OnboardingResponse> handleOnboardingForImport(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregateRequests,
            Product product,
            OnboardingImportContract contractImported) {

        return Uni.createFrom()
                .item(registryResourceFactory.create(onboarding, getManagerTaxCode(userRequests)))
                .onItem()
                .invoke(registryManager -> registryManager.setResource(registryManager.retrieveInstitution()))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem()
                .transformToUni(registryManager -> validateAndPersistOnboardingForImport(
                        registryManager, onboarding, userRequests, aggregateRequests, product, contractImported));
    }

    private Uni<OnboardingResponse> validateAndPersistOnboardingForImport(
            RegistryManager<?> registryManager,
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregateRequests,
            Product product,
            OnboardingImportContract contractImported) {

        return registryManager.isValid()
                .onItem()
                .transformToUni(ignored -> registryManager.customValidation(product))
                .onItem()
                .invoke(() -> onboarding.setTestEnvProductIds(product.getTestEnvProductIds()))
                .onItem()
                .transformToUni(ignored -> persistOnboarding(onboarding, userRequests, product, aggregateRequests))
                .onItem()
                .call(onboardingPersisted -> documentControllerApi.persistDocumentForImport(
                        onboardingDocumentMapper.toRequest(onboardingPersisted, product, contractImported)))
                .onItem()
                .transformToUni(currentOnboarding -> persistAndStartOrchestrationOnboarding(
                        currentOnboarding,
                        orchestrationService.triggerOrchestration(currentOnboarding.getId(), TIMEOUT_ORCHESTRATION_RESPONSE)))
                .onItem()
                .transform(onboardingMapper::toResponse);
    }

    private Uni<Onboarding> persistOnboarding(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            Product product,
            List<AggregateInstitutionRequest> aggregates) {

        log.info(
                "persist onboarding for: product {}, product parent {}",
                product.getId(), product.getParentId());

        Map<PartyRole, ProductRoleInfo> roleMappings =
                Objects.nonNull(product.getParent())
                        ? product
                        .getParent()
                        .getRoleMappings(onboarding.getInstitution().getInstitutionType().name())
                        : product.getRoleMappings(onboarding.getInstitution().getInstitutionType().name());

        if (Objects.nonNull(product.getParentId())) {
            setInstitutionId(onboarding, product.getParentId());
        }

        // This condition has been added in order to avoid transaction for cucumber tests(not clustered mongo does not support transactions)
        if (INTEGRATION_PROFILE.equals(activeProfile)) {
            return storeAndValidateOnboarding(onboarding, userRequests, product, aggregates, roleMappings);
        }
        return Panache.withTransaction(() ->
                storeAndValidateOnboarding(onboarding, userRequests, product, aggregates, roleMappings));
    }

    private Uni<Onboarding> storeAndValidateOnboarding(Onboarding onboarding, List<UserRequest> userRequests, Product product,
                                                       List<AggregateInstitutionRequest> aggregates, Map<PartyRole, ProductRoleInfo> roleMappings) {
        return Onboarding.persist(onboarding)
                .replaceWith(onboarding)
                .onItem()
                .transformToUni(
                        onboardingPersisted ->
                                validationRole(
                                        userRequests,
                                        validRoles(
                                                product,
                                                PHASE_ADDITION_ALLOWED.ONBOARDING,
                                                onboarding.getInstitution().getInstitutionType()))
                                        .onItem()
                                        .transformToUni(
                                                ignore ->
                                                        validateUserAggregatesRoles(
                                                                aggregates,
                                                                validRoles(
                                                                        product,
                                                                        PHASE_ADDITION_ALLOWED.ONBOARDING,
                                                                        onboarding.getInstitution().getInstitutionType())))
                                        .onItem()
                                        .transformToUni(
                                                ignore ->
                                                        retrieveAndSetUserAggregatesResources(
                                                                onboardingPersisted, product, aggregates))
                                        .onItem()
                                        .transformToUni(
                                                ignore -> retrieveUserResources(userRequests, roleMappings))
                                        .onItem()
                                        .invoke(onboardingPersisted::setUsers)
                                        .replaceWith(onboardingPersisted));
    }

    private Uni<Onboarding> setInstitutionId(Onboarding onboarding, String parentId) {
        final String taxCode = onboarding.getInstitution().getTaxCode();
        final String origin = onboarding.getInstitution().getOrigin().name();
        final String originId = onboarding.getInstitution().getOriginId();
        final String subunitCode = onboarding.getInstitution().getSubunitCode();
        final String institutionType = onboarding.getInstitution().getInstitutionType().name();

        return getOnboardingByFilters(taxCode, subunitCode, origin, originId, parentId)
                .filter(item -> institutionType.equalsIgnoreCase(item.getInstitution().getInstitutionType().name()))
                .collect().asList()
                .onItem().transformToUni(onboardings -> {
                    if (!onboardings.isEmpty()) {
                        onboarding.getInstitution().setId(onboardings.get(0).getInstitution().getId());
                    } else {
                        throw new ResourceNotFoundException(
                                String.format(
                                        "Onboarding for taxCode %s, origin %s, originId %s, parentId %s, subunitCode %s not found and institutionType %s",
                                        taxCode, origin, originId, parentId, subunitCode, institutionType
                                )
                        );
                    }
                    return Uni.createFrom().item(onboarding);
                });
    }

    private Uni<Onboarding> addReferencedOnboardingId(Onboarding onboarding) {
        final String taxCode = onboarding.getInstitution().getTaxCode();
        final String origin = onboarding.getInstitution().getOrigin().name();
        final String originId = onboarding.getInstitution().getOriginId();
        final String productId = onboarding.getProductId();
        final String subunitCode = onboarding.getInstitution().getSubunitCode();

        Multi<Onboarding> onboardings =
                getOnboardingByFilters(taxCode, subunitCode, origin, originId, productId);
        Uni<Onboarding> current =
                onboardings
                        .filter(item -> Objects.isNull(item.getReferenceOnboardingId()))
                        .toUni()
                        .onItem()
                        .ifNull()
                        .failWith(
                                () ->
                                        new ResourceNotFoundException(
                                                String.format(
                                                        "Onboarding for taxCode %s, origin %s, originId %s, productId %s, subunitCode %s not found",
                                                        taxCode, origin, originId, productId, subunitCode)))
                        .invoke(
                                previousOnboarding -> {
                                    onboarding.setReferenceOnboardingId(previousOnboarding.getId());
                                    onboarding.setBilling(previousOnboarding.getBilling());
                                }
                        );
        return current
                .onItem()
                .transformToUni(ignored -> onboardings.collect().first())
                .onItem()
                .invoke(
                        lastOnboarding -> {
                            String previousManagerId =
                                    lastOnboarding.getUsers().stream()
                                            .filter(user -> user.getRole().equals(PartyRole.MANAGER))
                                            .map(User::getId)
                                            .findFirst()
                                            .orElse(null);
                            onboarding.setPreviousManagerId(previousManagerId);
                        })
                .replaceWith(onboarding);
    }

    private Multi<Onboarding> getOnboardingByFilters(
            String taxCode, String subunitCode, String origin, String originId, String productId) {
        final Map<String, Object> queryParameter =
                QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                        taxCode, subunitCode, origin, originId, COMPLETED, productId);
        Document sort = QueryUtils.buildSortDocument(Onboarding.Fields.createdAt.name(), SortEnum.DESC);
        Document query = QueryUtils.buildQuery(queryParameter);
        return Onboarding.find(query, sort).stream();
    }

    public Uni<Onboarding> persistAndStartOrchestrationOnboarding(
            Onboarding onboarding, Uni<OrchestrationResponse> orchestration) {
        final List<Onboarding> onboardings = new ArrayList<>();
        onboardings.add(onboarding);

        log.info(
                "Persist onboarding and start orchestration {} for: taxCode {}, subunitCode {}, type {}",
                onboardingOrchestrationEnabled,
                onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(),
                onboarding.getInstitution().getInstitutionType());

        if (Boolean.TRUE.equals(onboardingOrchestrationEnabled)) {
            return Onboarding.persistOrUpdate(onboardings)
                    .onItem()
                    .transformToUni(saved -> orchestration)
                    .replaceWith(onboarding);
        } else {
            return Onboarding.persistOrUpdate(onboardings).replaceWith(onboarding);
        }
    }

    /**
     * Identify which workflow must be trigger during onboarding process. Each workflow consist of
     * different activities such as creating contract or sending appropriate mail. For more
     * information look at <a
     * href="https://pagopa.atlassian.net/wiki/spaces/SCP/pages/776339638/DR+-+Domain+Onboarding">...</a>
     *
     * @param onboarding actual onboarding request
     * @return WorkflowType
     */
    private WorkflowType getWorkflowType(Onboarding onboarding) {
        InstitutionType institutionType = onboarding.getInstitution().getInstitutionType();
        Product product = productService.getProductIsValid(onboarding.getProductId());

        if (InstitutionType.PT.equals(institutionType)) {
            return WorkflowType.FOR_APPROVE_PT;
        }

        if (Objects.nonNull(onboarding.getIsAggregator())
                && onboarding.getIsAggregator().equals(Boolean.TRUE)) {
            return WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR;
        }

        if (InstitutionType.PA.equals(institutionType)
                || verifyInstitutionOnGSP(institutionType, onboarding.getInstitution().getOrigin().getValue())
                || InstitutionType.SA.equals(institutionType)
                || InstitutionType.AS.equals(institutionType)
                || Objects.nonNull(product.getParentId())
                || (InstitutionType.PRV_PF.equals(institutionType)
                || (InstitutionType.PRV.equals(institutionType)
                && !PROD_PAGOPA.getValue().equals(onboarding.getProductId())))) {
            return WorkflowType.CONTRACT_REGISTRATION;
        }

        if (InstitutionType.PG.equals(institutionType)) {
            return WorkflowType.CONFIRMATION;
        }

        if (InstitutionType.GPU.equals(institutionType)) {
            return WorkflowType.FOR_APPROVE_GPU;
        }

        return WorkflowType.FOR_APPROVE;
    }

    private boolean verifyInstitutionOnGSP(InstitutionType institutionType, String origin) {
        Set<InstitutionType> allowedInstitutionType = Set.of(InstitutionType.GSP, InstitutionType.SCEC);
        return Objects.nonNull(institutionType) && allowedInstitutionType.contains(institutionType)
                && IPA.getValue().equals(origin);
    }

    private Uni<Product> product(String productId) {
        return Uni.createFrom()
                .item(() -> productService.getProductIsValid(productId))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Product> getProductByOnboarding(Onboarding onboarding) {

        /* retrieve product, if is not valid will throw OnboardingNotAllowedException */
        return product(onboarding.getProductId())
                .onFailure()
                .transform(
                        ex ->
                                new OnboardingNotAllowedException(
                                        String.format(
                                                UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED.getMessage(),
                                                onboarding.getInstitution().getTaxCode(),
                                                onboarding.getProductId()),
                                        DEFAULT_ERROR.getCode()));
    }

    private Uni<Boolean> verifyAlreadyOnboardingForProductAndProductParent(
            Institution institution, String productId, String productParentId) {
        if (Objects.nonNull(productParentId)) {
            log.info("Verifying already onboarding for institution: {}, productId: {}, parentId: {}",
                    institution.getDescription(), productId, productParentId);
            return checkIfAlreadyOnboardingAndValidateAllowedProductList(institution, productId)
                    .onItem().transformToUni(ignored ->
                            checkIfAlreadyOnboardingAndValidateAllowedProductList(institution, productParentId)
                                    .onItem().transformToUni(result -> Uni.createFrom().failure(
                                            new InvalidRequestException(
                                                    String.format(PARENT_PRODUCT_NOT_ONBOARDED.getMessage(),
                                                            productParentId,
                                                            institution.getTaxCode()),
                                                    PARENT_PRODUCT_NOT_ONBOARDED.getCode())))
                                    .onFailure(ResourceConflictException.class)
                                    .recoverWithNull().replaceWith(Uni.createFrom().item(true))
                    );
        } else {
            log.info("Verifying already onboarding for institution: {}, productId: {}",
                    institution.getDescription(), productId);
            return checkIfAlreadyOnboardingAndValidateAllowedProductList(institution, productId);
        }
    }

    private Uni<Boolean> verifyOnboardingNotExistForProductAndProductParent(
            Onboarding onboarding, String productId, String productParentId) {
        return (Objects.nonNull(productParentId)
                // If product has parent, I must verify if onboarding is present for parent and child
                ? checkIfOnboardingNotExistAndValidateAllowedProductList(onboarding, productParentId)
                .onFailure(ResourceConflictException.class)
                .recoverWithUni(
                        ignore -> checkIfOnboardingNotExistAndValidateAllowedProductList(onboarding, productId))
                // If product is a root, I must only verify if onboarding for root
                : checkIfOnboardingNotExistAndValidateAllowedProductList(onboarding, productId));
    }

    private Uni<Boolean> validateAllowedProductList(String taxCode, String subunitCode, String productId) {
        log.info(
                "Validating allowed map for: taxCode {}, subunitCode {}, product {}",
                taxCode, subunitCode, productId);
        if (!validateByProductOrInstitutionTaxCode(productId, taxCode)) {
            return Uni.createFrom()
                    .failure(
                            new OnboardingNotAllowedException(
                                    String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE.getMessage(), taxCode, productId),
                                    DEFAULT_ERROR.getCode()));

        }
        return Uni.createFrom().item(Boolean.TRUE);
    }

    private boolean validateByProductOrInstitutionTaxCode(String productId, String taxCode) {
        log.info("Validate start");
        log.debug("Provided productId = {} and taxCode = {}", productId, taxCode);
        boolean result = productService.isProductEnabled(productId) || productService.verifyAllowedByInstitutionTaxCode(productId, taxCode);
        log.debug("Validate result = {}", result);
        log.info("Validate end");
        return result;
    }

    private Uni<Boolean> checkIfAlreadyOnboardingAndValidateAllowedProductList(
            Institution institution, String productId) {
        return validateAllowedProductList(institution.getTaxCode(), institution.getSubunitCode(), productId)
                .flatMap(ignored -> {
                    String origin = institution.getOrigin() != null ? institution.getOrigin().getValue() : null;
                    return verifyOnboarding(
                            institution.getTaxCode(),
                            institution.getSubunitCode(),
                            origin,
                            institution.getOriginId(),
                            COMPLETED,
                            productId,
                            institution.getInstitutionType())
                            .flatMap(onboardingResponses ->
                                    handleOnboardingResponses(onboardingResponses, productId, institution.getTaxCode()));
                });
    }

    private Uni<Boolean> handleOnboardingResponses(
            List<OnboardingResponse> onboardingResponses,
            String productId,
            String taxCode) {

        if (onboardingResponses.isEmpty()) {
            return Uni.createFrom().item(Boolean.TRUE);
        }

        if (isProductIoWithoutReferenceOnboarding(productId, onboardingResponses)) {
            return Uni.createFrom().item(Boolean.TRUE);
        }

        return Uni.createFrom().failure(
                new ResourceConflictException(
                        String.format(
                                PRODUCT_ALREADY_ONBOARDED.getMessage(),
                                productId,
                                taxCode),
                        PRODUCT_ALREADY_ONBOARDED.getCode()));
    }

    private boolean isProductIoWithoutReferenceOnboarding(
            String productId,
            List<OnboardingResponse> onboardingResponses) {

        return ProductId.PROD_IO.getValue().equals(productId)
                && onboardingResponses.stream()
                .allMatch(response -> Objects.nonNull(response.getReferenceOnboardingId()));
    }

    private Uni<Boolean> checkIfOnboardingNotExistAndValidateAllowedProductList(
            Onboarding onboarding, String productId) {
        return validateAllowedProductList(
                onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(),
                productId)
            .flatMap(
                ignored -> {
                  if (Objects.isNull(onboarding.getReferenceOnboardingId())) {
                    return Uni.createFrom()
                        .failure(
                            new InvalidRequestException(
                                INVALID_REFERENCE_ONBORADING.getMessage(),
                                INVALID_REFERENCE_ONBORADING.getCode()));
                  }
                  return Onboarding.findByIdOptional(onboarding.getReferenceOnboardingId())
                      .onItem()
                      .transformToUni(
                          opt ->
                              opt.map(Onboarding.class::cast)
                                  .filter(
                                      referenceOnboarding ->
                                          referenceOnboarding.getStatus().equals(COMPLETED))
                                  .map(referenceOnboarding -> Uni.createFrom().item(Boolean.TRUE))
                                  .orElse(
                                      Uni.createFrom()
                                          .failure(
                                              new InvalidRequestException(
                                                  String.format(
                                                      PRODUCT_NOT_ONBOARDED.getMessage(),
                                                      onboarding.getProductId(),
                                                      onboarding.getInstitution().getTaxCode(),
                                                      PRODUCT_NOT_ONBOARDED.getCode())))));
                });
    }

    private String retrieveProductRole(
            UserRequest userInfo, Map<PartyRole, ProductRoleInfo> roleMappings) {
        try {
            if (Objects.isNull(roleMappings) || roleMappings.isEmpty())
                throw new IllegalArgumentException("Role mappings is required");

            if (Objects.isNull(roleMappings.get(userInfo.getRole())))
                throw new IllegalArgumentException(
                        String.format(AT_LEAST_ONE_PRODUCT_ROLE_REQUIRED.getMessage(), userInfo.getRole()));
            if (Objects.isNull((roleMappings.get(userInfo.getRole()).getRoles())))
                throw new IllegalArgumentException(
                        String.format(AT_LEAST_ONE_PRODUCT_ROLE_REQUIRED.getMessage(), userInfo.getRole()));
            if (roleMappings.get(userInfo.getRole()).getRoles().size() != 1)
                throw new IllegalArgumentException(
                        String.format(MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE.getMessage(), userInfo.getRole()));
            return roleMappings.get(userInfo.getRole()).getRoles().get(0).getCode();

        } catch (IllegalArgumentException e) {
            throw new OnboardingNotAllowedException(e.getMessage(), DEFAULT_ERROR.getCode());
        }
    }

    private Uni<List<UserRequest>> validationRole(
            List<UserRequest> users, List<PartyRole> validRoles) {

        List<UserRequest> usersNotValidRole =
                users.stream().filter(user -> !validRoles.contains(user.getRole())).toList();
        if (!usersNotValidRole.isEmpty()) {
            String usersNotValidRoleString =
                    usersNotValidRole.stream()
                            .map(user -> user.getRole().toString())
                            .collect(Collectors.joining(","));
            return Uni.createFrom()
                    .failure(
                            new InvalidRequestException(
                                    String.format(
                                            ROLES_NOT_ADMITTED_ERROR.getMessage(), usersNotValidRoleString),
                                    ROLES_NOT_ADMITTED_ERROR.getCode()));
        }

        return Uni.createFrom().item(users);
    }

    private Uni<Void> validateUserAggregatesRoles(
            List<AggregateInstitutionRequest> aggregates, List<PartyRole> validRoles) {
        log.debug("starting validateUserAggregatesRoles");
        if (!CollectionUtils.isEmpty(aggregates)) {
            return Multi.createFrom()
                    .iterable(aggregates)
                    .filter(aggregate -> !CollectionUtils.isEmpty(aggregate.getUsers()))
                    .onItem()
                    .invoke(
                            aggregate ->
                                    log.debug("Validating role for users of aggregate: {}", aggregate.getTaxCode()))
                    .onItem()
                    .transformToUniAndMerge(
                            aggregate ->
                                    validationRole(aggregate.getUsers(), validRoles)
                                            .onFailure()
                                            .invoke(
                                                    throwable ->
                                                            log.error(
                                                                    "Error during validation role for aggregate: {}",
                                                                    aggregate.getTaxCode(), throwable)))
                    .collect()
                    .asList()
                    .replaceWithVoid();
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> retrieveAndSetUserAggregatesResources(
            Onboarding onboarding, Product product, List<AggregateInstitutionRequest> aggregates) {

        log.info(
                "Retrieving user resources for aggregates: product {}, product parent {}",
                product.getId(), product.getParentId());

        Map<PartyRole, ProductRoleInfo> roleMappings =
                Objects.nonNull(product.getParent())
                        ? product
                        .getParent()
                        .getRoleMappings(onboarding.getInstitution().getInstitutionType().name())
                        : product.getRoleMappings(onboarding.getInstitution().getInstitutionType().name());

        if (!CollectionUtils.isEmpty(aggregates)) {
            return Multi.createFrom()
                    .iterable(aggregates)
                    .filter(aggregate -> !CollectionUtils.isEmpty(aggregate.getUsers()))
                    .onItem()
                    .invoke(
                            aggregate ->
                                    log.debug("Retrieving user resources for aggregate: {}", aggregate.getTaxCode()))
                    .onItem()
                    .transformToUni(
                            aggregate ->
                                    retrieveUserResources(aggregate.getUsers(), roleMappings)
                                            .onFailure()
                                            .invoke(
                                                    throwable ->
                                                            log.error(
                                                                    "Error during retrieving user resources for aggregate: {}",
                                                                    aggregate.getTaxCode(), throwable))
                                            .onItem()
                                            .invoke(users -> setUsersInAggregateToPersist(onboarding, aggregate, users)))
                    .concatenate()
                    .onItem()
                    .ignoreAsUni();
        }

        return Uni.createFrom().voidItem();
    }

    private static void setUsersInAggregateToPersist(
            Onboarding onboarding, AggregateInstitutionRequest aggregate, List<User> users) {
        onboarding.getAggregates().stream()
                .filter(
                        aggregateInstitution ->
                                Optional.ofNullable(aggregateInstitution.getSubunitCode()).equals(Optional.ofNullable(aggregate.getSubunitCode())) &&
                                        aggregateInstitution.getTaxCode().equals(aggregate.getTaxCode()))
                .findAny()
                .ifPresent(aggregateInstitutionRequest -> aggregateInstitutionRequest.setUsers(users));
    }

    private Uni<List<User>> retrieveUserResources(
            List<UserRequest> users, Map<PartyRole, ProductRoleInfo> roleMappings) {

        return Multi.createFrom()
            .iterable(users)
            .onItem()
            .transformToUni(
                user -> {
                    log.debug("Processing user with taxCode: {}", user.getTaxCode());
                    return userRegistryApi
                        /* search user by tax code */
                        .searchUsingPOST(
                            USERS_FIELD_LIST, new UserSearchDto().fiscalCode(user.getTaxCode()))

                        /* retrieve userId, if found will eventually update some fields */
                        .onItem()
                        .transformToUni(
                            userResource -> {
                              log.debug("User found with id: {}", userResource.getId());
                              Optional<String> optUserMailRandomUuid =
                                  Optional.ofNullable(user.getEmail())
                                      .map(mail -> retrieveUserMailUuid(userResource, mail));
                              Optional<MutableUserFieldsDto> optUserFieldsDto =
                                  toUpdateUserRequest(user, userResource, optUserMailRandomUuid);
                              return optUserFieldsDto
                                  .map(
                                      userUpdateRequest -> {
                                          log.debug("Updating user: {}", userResource.getId());
                                          return userRegistryApi
                                              .updateUsingPATCH(
                                                  userResource.getId().toString(), userUpdateRequest)
                                              .replaceWith(userResource.getId());
                                      })
                                  .orElse(Uni.createFrom().item(userResource.getId()))
                                  .map(
                                      userResourceId ->
                                          User.builder()
                                              .id(userResourceId.toString())
                                              .role(user.getRole())
                                              .userMailUuid(optUserMailRandomUuid.orElse(null))
                                              .productRole(retrieveProductRole(user, roleMappings))
                                              .build());
                            })
                        /* if not found 404, will create new user */
                        .onFailure(WebApplicationException.class)
                        .recoverWithUni(
                            ex -> {
                              if (((WebApplicationException) ex).getResponse().getStatus() != 404) {
                                log.debug("Error retrieving user with taxCode: {}", user.getTaxCode(), ex);
                                return Uni.createFrom().failure(ex);
                              }

                              log.debug("User not found, creating new user with taxCode: {}", user.getTaxCode());
                              String userMailRandomUuid =
                                  ID_MAIL_PREFIX.concat(UUID.randomUUID().toString());
                              return userRegistryApi
                                  .saveUsingPATCH(createSaveUserDto(user, userMailRandomUuid))
                                  .onItem()
                                  .transform(
                                      userId -> {
                                          log.debug("User created with id: {}", userId.getId());
                                          return User.builder()
                                              .id(userId.getId().toString())
                                              .role(user.getRole())
                                              .userMailUuid(userMailRandomUuid)
                                              .productRole(retrieveProductRole(user, roleMappings))
                                              .build();
                                      });
                            });
                })
            .concatenate()
            .collect()
            .asList();
    }

    private SaveUserDto createSaveUserDto(UserRequest model, String userMailRandomUuid) {
        SaveUserDto resource = new SaveUserDto();
        resource.setFiscalCode(model.getTaxCode());
        resource.setName(
                new CertifiableFieldResourceOfstring()
                        .value(model.getName())
                        .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        resource.setFamilyName(
                new CertifiableFieldResourceOfstring()
                        .value(model.getSurname())
                        .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));

        if (Objects.nonNull(userMailRandomUuid)) {
            WorkContactResource contact = new WorkContactResource();
            contact.setEmail(
                    new CertifiableFieldResourceOfstring()
                            .value(model.getEmail())
                            .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
            resource.setWorkContacts(Map.of(userMailRandomUuid, contact));
        }
        return resource;
    }

    private String retrieveUserMailUuid(UserResource foundUser, String userMail) {
        if (Objects.isNull(foundUser.getWorkContacts())) {
            return ID_MAIL_PREFIX.concat(UUID.randomUUID().toString());
        }

        return foundUser.getWorkContacts().entrySet().stream()
                .filter(
                        entry ->
                                Objects.nonNull(entry.getValue()) && Objects.nonNull(entry.getValue().getEmail()))
                .filter(entry -> entry.getValue().getEmail().getValue().equals(userMail))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(ID_MAIL_PREFIX.concat(UUID.randomUUID().toString()));
    }

    protected static Optional<MutableUserFieldsDto> toUpdateUserRequest(
            Object user, UserResource foundUser, Optional<String> optUserMailRandomUuid) {

        String name;
        String surname;
        String email;

        if (user instanceof UserRequest userRequest) {
            name = userRequest.getName();
            surname = userRequest.getSurname();
            email = userRequest.getEmail();
        } else if (user instanceof UserRequesterDto userRequester) {
            name = userRequester.getName();
            surname = userRequester.getSurname();
            email = userRequester.getEmail();
        } else {
            throw new IllegalArgumentException("Unsupported user type: " + user.getClass());
        }

        Optional<MutableUserFieldsDto> mutableUserFieldsDto = Optional.empty();
        if (isFieldToUpdate(foundUser.getName(), name)) {
            MutableUserFieldsDto dto = new MutableUserFieldsDto();
            dto.setName(
                    new CertifiableFieldResourceOfstring()
                            .value(name)
                            .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
            mutableUserFieldsDto = Optional.of(dto);
        }
        if (isFieldToUpdate(foundUser.getFamilyName(), surname)) {
            MutableUserFieldsDto dto = mutableUserFieldsDto.orElseGet(MutableUserFieldsDto::new);
            dto.setFamilyName(
                    new CertifiableFieldResourceOfstring()
                            .value(surname)
                            .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
            mutableUserFieldsDto = Optional.of(dto);
        }

        if (optUserMailRandomUuid.isPresent()) {
            Optional<String> entryMail =
                    Objects.nonNull(foundUser.getWorkContacts())
                            ? foundUser.getWorkContacts().keySet().stream()
                            .filter(key -> key.equals(optUserMailRandomUuid.get()))
                            .findFirst()
                            : Optional.empty();

            if (entryMail.isEmpty()) {
                MutableUserFieldsDto dto = mutableUserFieldsDto.orElseGet(MutableUserFieldsDto::new);
                final WorkContactResource workContact = new WorkContactResource();
                workContact.setEmail(
                        new CertifiableFieldResourceOfstring()
                                .value(email)
                                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
                dto.setWorkContacts(Map.of(optUserMailRandomUuid.get(), workContact));
                mutableUserFieldsDto = Optional.of(dto);
            }
        }
        return mutableUserFieldsDto;
    }

    private static boolean isFieldToUpdate(
            CertifiableFieldResourceOfstring certifiedField, String value) {
        boolean isToUpdate = true;
        if (certifiedField != null) {
            boolean isNoneCertification =
                    CertifiableFieldResourceOfstring.CertificationEnum.NONE.equals(
                            certifiedField.getCertification());
            boolean isSameValue =
                    isNoneCertification
                            ? certifiedField.getValue().equals(value)
                            : certifiedField.getValue().equalsIgnoreCase(value);

            if (isSameValue) {
                isToUpdate = false;
            } else if (!isNoneCertification) {
                throw new InvalidRequestException(
                        USERS_UPDATE_NOT_ALLOWED.getMessage(), USERS_UPDATE_NOT_ALLOWED.getCode());
            }
        }
        return isToUpdate;
    }

    @Override
    public Uni<OnboardingGet> approve(String onboardingId) {

        return retrieveOnboardingAndCheckIfExpired(onboardingId)
                .onItem()
                .transformToUni(this::checkIfToBeValidated)
                // Fail if onboarding exists for a product
                .onItem()
                .transformToUni(
                        onboarding ->
                                product(onboarding.getProductId())
                                        .onItem()
                                        .transformToUni(
                                                product ->
                                                        verifyAlreadyOnboardingForProductAndProductParent(
                                                                onboarding.getInstitution(),
                                                                product.getId(),
                                                                product.getParentId()))
                                        .replaceWith(onboarding))
                .onItem()
                .transformToUni(
                        onboarding ->
                                onboardingOrchestrationEnabled
                                        ? orchestrationService.triggerOrchestration(onboarding.getId(), null)
                                        .map(ignore -> onboarding)
                                        : Uni.createFrom().item(onboarding))
                .flatMap(onboardingResponseFactory::toGetResponse);
    }

    @Override
    public Uni<Onboarding> uploadContractSigned(String onboardingId, FormItem formItem) {
        return retrieveOnboarding(onboardingId)
                .onItem()
                .transformToUni(this::checkIfCompleted)
                .onItem()
                .transformToUni(onboarding ->
                        retrieveOnboardingUserFiscalCodeList(onboarding)
                                .onItem()
                                .transformToUni(fiscalCodes ->
                                        uploadSignedContractAndUpdateDocument(
                                            onboarding,
                                            formItem,
                                            true,
                                            DocumentType.INSTITUTION,
                                            fiscalCodes)
                                            .onItem()
                                            .invoke(ignore -> onboarding.setUpdatedAt(LocalDateTime.now()))
                                            .replaceWith(onboarding)))
                .onItem()
                .transformToUni(onboarding ->
                        updateOnboarding(onboardingId, onboarding)
                                .onItem()
                                .transformToUni(ignore -> documentControllerApi.updateDocumentUpdatedAt(onboardingId))
                                .replaceWith(onboarding));
    }


    @Override
    public Uni<Onboarding> complete(String onboardingId, FormItem formItem) {
        return completeInternal(onboardingId, formItem, false, false);
    }

    @Override
    public Uni<Onboarding> completeWithoutSignatureVerification(
            String onboardingId, FormItem formItem) {
        return completeInternal(onboardingId, formItem, true, false);
    }

    @Override
    public Uni<Onboarding> completeOnboardingUsers(
            String onboardingId,
            FormItem formItem) {
        return completeInternal(onboardingId, formItem, false, true);
    }

    private Uni<Onboarding> completeInternal(
            String onboardingId,
            FormItem formItem,
            boolean skipSignatureVerification,
            boolean isUsersFlow) {

        return retrieveOnboardingAndCheckIfExpired(onboardingId)
                .onItem()
                .transformToUni(onboarding ->
                        verifyCompletionPreconditions(onboarding, isUsersFlow)
                                .replaceWith(onboarding))
                .onItem()
                .transformToUni(onboarding -> {
                    Uni<List<String>> fiscalCodesUni = retrieveOnboardingUserFiscalCodeList(onboarding);
                    return fiscalCodesUni.onItem().transformToUni(fiscalCodes ->
                            uploadSignedContractAndUpdateDocument(
                                onboarding,
                                formItem,
                                skipSignatureVerification,
                                isUsersFlow ? DocumentType.USER : DocumentType.INSTITUTION,
                                fiscalCodes)
                                .replaceWith(onboarding));
                })
                .onItem()
                .transformToUni(this::triggerOrchestrationIfEnabled);
    }

    private Uni<Boolean> verifyCompletionPreconditions(Onboarding onboarding, boolean isUsersFlow) {
        return product(onboarding.getProductId())
                .onItem()
                .transformToUni(product ->
                        isUsersFlow
                            ? verifyOnboardingNotExistForProductAndProductParent(
                            onboarding, product.getId(), product.getParentId())
                            : verifyAlreadyOnboardingForProductAndProductParent(
                            onboarding.getInstitution(), product.getId(), product.getParentId()));
    }

    private Uni<Onboarding> triggerOrchestrationIfEnabled(Onboarding onboarding) {
        return onboardingOrchestrationEnabled
                ? orchestrationService.triggerOrchestration(onboarding.getId(), null)
                .map(ignore -> onboarding)
                : Uni.createFrom().item(onboarding);
    }

    private Uni<Void> uploadSignedContractAndUpdateDocument(
      Onboarding onboarding,
      FormItem formItem,
      boolean skipSignatureVerification,
      DocumentType documentType,
      List<String> fiscalCodes) {
    String onboardingId = onboarding.getId();
    return getProductByOnboarding(onboarding)
        .flatMap(
            product ->
                    onboardingUtils.buildUploadSignedContractRequest(
                        onboarding,
                        skipSignatureVerification,
                        formItem.getFile(),
                        product,
                        documentType,
                        fiscalCodes))
        .flatMap(
            request -> documentContentControllerApi.uploadSignedContract(request, onboardingId))
        .replaceWith(Uni.createFrom().voidItem());
    }

    private Uni<Onboarding> retrieveOnboarding(String onboardingId) {
    // Retrieve Onboarding if exists
    return Onboarding.findByIdOptional(onboardingId)
        .onItem()
        .transformToUni(
            opt ->
                opt
                    // I must cast to Onboarding because findByIdOptional return a generic
                    // ReactiveEntity
                    .map(Onboarding.class::cast)
                    .map(onboarding -> Uni.createFrom().item(onboarding))
                    .orElse(
                        Uni.createFrom()
                            .failure(
                                new InvalidRequestException(
                                    String.format(
                                        "Onboarding with id '%s' not found", onboardingId)))));
    }

    private Uni<Onboarding> retrieveOnboardingAndCheckIfExpired(String onboardingId) {
    return Onboarding.findByIdOptional(onboardingId)
        .onItem()
        .transformToUni(
            opt ->
                opt.map(Onboarding.class::cast)
                    .filter(
                        onboarding ->
                            OnboardingStatus.TOBEVALIDATED.equals(onboarding.getStatus())
                                || !isOnboardingExpired(onboarding.getExpiringDate()))
                    .map(onboarding -> Uni.createFrom().item(onboarding))
                    .orElse(
                        Uni.createFrom()
                            .failure(
                                new InvalidRequestException(
                                    String.format(ONBOARDING_EXPIRED.getMessage(), onboardingId),
                                    ONBOARDING_EXPIRED.getCode()))));
    }

    private Uni<Onboarding> checkIfToBeValidated(Onboarding onboarding) {
    return OnboardingStatus.TOBEVALIDATED.equals(onboarding.getStatus())
        ? Uni.createFrom().item(onboarding)
        : Uni.createFrom()
            .failure(
                new InvalidRequestException(
                    String.format(
                        ONBOARDING_NOT_TO_BE_VALIDATED.getMessage(),
                        onboarding.getId(),
                        ONBOARDING_NOT_TO_BE_VALIDATED.getCode())));
    }

    private Uni<Onboarding> checkIfCompleted(Onboarding onboarding) {
    return COMPLETED.equals(onboarding.getStatus())
        ? Uni.createFrom().item(onboarding)
        : Uni.createFrom()
            .failure(
                new InvalidRequestException(
                    String.format(
                        ONBOARDING_NOT_COMPLETED.getMessage(),
                        onboarding.getId(),
                        onboarding.getStatus(),
                        ONBOARDING_NOT_COMPLETED.getCode())));
    }

    public static boolean isOnboardingExpired(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        return Objects.nonNull(dateTime) && (now.isEqual(dateTime) || now.isAfter(dateTime));
    }

    private Uni<List<String>> retrieveOnboardingUserFiscalCodeList(Onboarding onboarding) {
        return Multi.createFrom()
                .iterable(
                        onboarding.getUsers().stream()
                                .filter(user -> PartyRole.MANAGER.equals(user.getRole()))
                                .map(User::getId)
                                .toList())
                .onItem()
                .transformToUni(userId -> userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, userId))
                .merge()
                .collect()
                .asList()
                .onItem()
                .transform(
                        usersResource -> usersResource.stream().map(UserResource::getFiscalCode).toList());
    }

    @Override
    public Uni<OnboardingGetResponse> onboardingGet(OnboardingGetFilters filters) {
        Document sort = QueryUtils.buildSortDocument(Onboarding.Fields.createdAt.name(), SortEnum.DESC);
        Map<String, Object> queryParameter = QueryUtils.createMapForOnboardingQueryParameter(filters);
        Document query = QueryUtils.buildQuery(queryParameter);

        return Uni.combine()
                .all()
                .unis(
                        filters.isSkipPagination() ? runQuery(query, sort).list()
                                : runQuery(query, sort).page(filters.getPage(), filters.getSize()).list(),
                        runQuery(query, null).count())
                .asTuple()
                .onItem()
                .transformToUni(this::constructOnboardingGetResponse);
    }

    private ReactivePanacheQuery<Onboarding> runQuery(Document query, Document sort) {
        return Onboarding.find(query, sort);
    }

    private Uni<OnboardingGetResponse> constructOnboardingGetResponse(
            Tuple2<List<Onboarding>, Long> tuple) {
        return convertOnboardingListToResponse(tuple.getItem1())
                .onItem()
                .transform(items -> {
                    OnboardingGetResponse onboardingGetResponse = new OnboardingGetResponse();
                    onboardingGetResponse.setCount(tuple.getItem2());
                    onboardingGetResponse.setItems(items);
                    return onboardingGetResponse;
                });
    }

    private Uni<List<OnboardingGet>> convertOnboardingListToResponse(List<Onboarding> onboardings) {
        return Multi.createFrom().iterable(onboardings)
                .onItem()
                .transformToUniAndConcatenate(onboardingResponseFactory::toGetResponse)
                .collect()
                .asList();
    }

    @Override
    public Uni<Long> rejectOnboarding(String onboardingId, String reasonForReject) {
    return Onboarding.findById(onboardingId)
        .onItem()
        .transform(Onboarding.class::cast)
        .onItem()
        .transformToUni(
            onboardingGet ->
                COMPLETED.equals(onboardingGet.getStatus())
                    ? Uni.createFrom()
                        .failure(
                            new InvalidRequestException(
                                String.format("Onboarding with id %s is COMPLETED!", onboardingId)))
                    : Uni.createFrom().item(onboardingGet))
        .onItem()
        .transformToUni(id -> updateReasonForRejectAndUpdateStatus(onboardingId, reasonForReject))
        // Start async activity if onboardingOrchestrationEnabled is true
        .onItem()
        .transformToUni(
            onboarding ->
                onboardingOrchestrationEnabled
                    ? orchestrationService
                        .triggerOrchestration(onboardingId, "60")
                        .map(ignore -> onboarding)
                    : Uni.createFrom().item(onboarding));
    }

    /**
     * Returns an onboarding record by its ID only if its status is PENDING. This feature is crucial
     * for ensuring that the onboarding process can be completed only when the onboarding status is
     * appropriately set to PENDING.
     *
     * @param onboardingId String
     * @return OnboardingGet
     */
    @Override
    public Uni<OnboardingGet> onboardingPending(String onboardingId) {
        return onboardingGet(onboardingId)
                .flatMap(
                        onboardingGet ->
                                PENDING.name().equals(onboardingGet.getStatus())
                                        || OnboardingStatus.TOBEVALIDATED.name().equals(onboardingGet.getStatus())
                                        ? Uni.createFrom().item(onboardingGet)
                                        : Uni.createFrom()
                                        .failure(
                                                new ResourceNotFoundException(
                                                        String.format(
                                                                "Onboarding with id %s not found or not in PENDING status!",
                                                                onboardingId))));
    }

    @Override
    public Uni<List<OnboardingResponse>> institutionOnboardings(
            String taxCode, String subunitCode, String origin, String originId, OnboardingStatus status) {
        Map<String, Object> queryParameter =
                QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                        taxCode, subunitCode, origin, originId, status, null);
        Document query = QueryUtils.buildQuery(queryParameter);
        return Onboarding.find(query).stream()
                .map(Onboarding.class::cast)
                .map(onboardingMapper::toResponse)
                .collect()
                .asList();
    }

    record QueryParams(String taxCode, String originId) {}

    @Override
    public Uni<List<OnboardingResponse>> verifyOnboarding(
            String taxCode,
            String subunitCode,
            String origin,
            String originId,
            OnboardingStatus status,
            String productId,
            InstitutionType institutionType) {

        return buildQueryParams(taxCode, originId, institutionType)
                .onItem().transformToUni(queryParams -> {
                    if (Objects.isNull(queryParams)) {
                        return Uni.createFrom().item(List.of());
                    }
                    return findOnboardings(
                            queryParams.taxCode(),
                            queryParams.originId(),
                            subunitCode,
                            origin,
                            status,
                            productId
                    );
                });
    }

    private Uni<QueryParams> buildQueryParams(
            String taxCode,
            String originId,
            InstitutionType institutionType) {

        if (InstitutionType.PRV_PF.equals(institutionType)) {
            return userRegistryApi.searchUsingPOST(
                            USERS_FIELD_LIST,
                            new UserSearchDto().fiscalCode(taxCode))
                    .onItem().transform(userResource -> {
                        String userId = userResource.getId().toString();
                        return new QueryParams(userId, userId);
                    })
                    .onFailure().recoverWithUni(throwable -> {
                        if (throwable instanceof WebApplicationException wae &&
                                wae.getResponse().getStatus() == 404) {
                            return Uni.createFrom().nullItem();
                        }
                        return Uni.createFrom().failure(throwable);
                    });
        } else {
            return Uni.createFrom().item(new QueryParams(taxCode, originId));
        }
    }

    private Uni<List<OnboardingResponse>> findOnboardings(
            String taxCode,
            String originId,
            String subunitCode,
            String origin,
            OnboardingStatus status,
            String productId) {

        Map<String, Object> queryParameter =
                QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                        taxCode, subunitCode, origin, originId, status, productId);

        Document query = QueryUtils.buildQuery(queryParameter);

        return Onboarding.find(query).stream()
                .map(Onboarding.class::cast)
                .map(onboardingMapper::toResponse)
                .filter(response -> Objects.isNull(response.getReferenceOnboardingId()))
                .collect()
                .asList();
    }

    @Override
    public Uni<OnboardingGet> onboardingGet(String onboardingId) {
        return Onboarding.findByIdOptional(onboardingId)
                .onItem().transformToUni(opt ->
                        opt
                                .map(Onboarding.class::cast)
                                .map(onboardingResponseFactory::toGetResponse)
                                .orElseGet(() -> Uni.createFrom().failure(
                                        new ResourceNotFoundException(
                                                String.format("Onboarding with id %s not found!", onboardingId)
                                        )
                                ))
                );
    }

    @Override
    public Uni<OnboardingGet> onboardingGetWithUserInfo(String onboardingId) {
        return Onboarding.findByIdOptional(onboardingId)
                .onItem()
                .transformToUni(
                        opt ->
                                opt
                                        // I must cast to Onboarding because findByIdOptional return a generic
                                        // ReactiveEntity
                                        .map(Onboarding.class::cast)
                                        .map(onboardingGet -> Uni.createFrom().item(onboardingGet))
                                        .orElse(
                                                Uni.createFrom()
                                                        .failure(
                                                                new ResourceNotFoundException(
                                                                        String.format(
                                                                                "Onboarding with id %s not found!", onboardingId)))))
                .flatMap(onboarding ->
                        toUserResponseWithUserInfo(onboarding.getUsers())
                                .flatMap(userResponses ->
                                        onboardingResponseFactory.toGetResponse(onboarding)
                                                .invoke(onboardingGet -> onboardingGet.setUsers(userResponses))))
                .flatMap(onboardingGet ->
                        documentControllerApi.getAttachments(onboardingId)
                                .invoke(onboardingGet::setAttachments)
                                .replaceWith(onboardingGet));
    }

    private Uni<List<UserResponse>> toUserResponseWithUserInfo(List<User> users) {
        return Multi.createFrom()
                .iterable(users)
                .onItem()
                .transformToUni(
                        user ->
                                userRegistryApi
                                        .findByIdUsingGET(USERS_FIELD_LIST, user.getId())
                                        .onItem()
                                        .transform(
                                                userResource -> {
                                                    UserResponse userResponse = userMapper.toUserResponse(user);
                                                    userMapper.fillUserResponse(userResource, userResponse);

                                                    Optional.ofNullable(userResource.getWorkContacts())
                                                            .filter(map -> map.containsKey(user.getUserMailUuid()))
                                                            .map(map -> map.get(user.getUserMailUuid()))
                                                            .filter(
                                                                    workContract ->
                                                                            StringUtils.isNotBlank(workContract.getEmail().getValue()))
                                                            .map(workContract -> workContract.getEmail().getValue())
                                                            .ifPresent(userResponse::setEmail);
                                                    return userResponse;
                                                }))
                .merge()
                .collect()
                .asList();
    }

    private static Uni<Long> updateReasonForRejectAndUpdateStatus(
            String onboardingId, String reasonForReject) {
        Map<String, Object> queryParameter =
                QueryUtils.createMapForOnboardingReject(reasonForReject, OnboardingStatus.REJECTED.name());
        Document query = QueryUtils.buildUpdateDocument(queryParameter);
        return Onboarding.update(query)
                .where("_id", onboardingId)
                .onItem()
                .transformToUni(
                        updateItemCount -> {
                            if (updateItemCount == 0) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format(ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED.getMessage(), onboardingId)));
                            }
                            return Uni.createFrom().item(updateItemCount);
                        });
    }

    private static Uni<Long> updateOnboardingStatus(String id, Map<String, Object> queryParameter) {
        Document query = QueryUtils.buildUpdateDocument(queryParameter);
        return Onboarding.update(query)
                .where("_id", id)
                .onItem()
                .transformToUni(
                        updateItemCount -> {
                            if (updateItemCount == 0) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format(ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED.getMessage(), id)));
                            }
                            return Uni.createFrom().item(updateItemCount);
                        });
    }

    /**
     * Retrieves institution data from the Institution API based on the provided onboarding user request.
     * Supports multiple query strategies: by tax code and subunit code,
     * by tax code only, or by origin and origin ID.
     * Handles 404 errors by converting them to ResourceNotFoundException.
     * Validates the response to ensure exactly one institution is found,
     * filtering by institution type if multiple results are returned.
     *
     * @param request the onboarding user request containing institution identifiers
     * @return Uni emitting the institution response
     * @throws ResourceNotFoundException if no institution is found or if multiple institutions are found without a matching type
     */
    private Uni<InstitutionResponse> getInstitutionFromUserRequest(OnboardingUserRequest request) {
        Uni<InstitutionsResponse> responseUni;
        if (Objects.nonNull(request.getTaxCode()) && Objects.nonNull(request.getSubunitCode())) {
            responseUni =
                    institutionService.getInstitutionsUsingGET(
                            request.getTaxCode(), request.getSubunitCode(), null, null, null, null);
        } else if (Objects.nonNull(request.getTaxCode())) {
            responseUni = institutionService.getInstitutionsUsingGET(request.getTaxCode(), null, null, null, null, null);
        } else {
            responseUni =
                    institutionService.getInstitutionsUsingGET(
                            null, null, request.getOrigin(), request.getOriginId(), null, null);
        }
        return responseUni
                .onFailure(WebApplicationException.class)
                .recoverWithUni(
                        ex ->
                                ((WebApplicationException) ex).getResponse().getStatus() == 404
                                        ? Uni.createFrom()
                                        .failure(
                                                new ResourceNotFoundException(
                                                        String.format(
                                                                INSTITUTION_NOT_FOUND.getMessage(),
                                                                request.getTaxCode(),
                                                                request.getOrigin(),
                                                                request.getOriginId(),
                                                                request.getSubunitCode())))
                                        : Uni.createFrom().failure(ex))
                .onItem()
                .transformToUni(
                        response -> {
                            if (Objects.isNull(response.getInstitutions())
                                    || response.getInstitutions().isEmpty()) {
                                throw new ResourceNotFoundException(
                                        String.format(
                                                INSTITUTION_NOT_FOUND.getMessage(),
                                                request.getTaxCode(),
                                                request.getOrigin(),
                                                request.getOriginId(),
                                                request.getSubunitCode()));
                            } else if (response.getInstitutions().size() > 1) {
                                return Uni.createFrom()
                                        .item(
                                                response.getInstitutions().stream()
                                                        .filter(
                                                                institutionResponse ->
                                                                        institutionResponse
                                                                                .getInstitutionType()
                                                                                .equals(request.getInstitutionType().name()))
                                                        .findFirst()
                                                        .orElseThrow(
                                                                () ->
                                                                        new ResourceNotFoundException(
                                                                                String.format(
                                                                                        INSTITUTION_NOT_FOUND.getMessage(),
                                                                                        request.getTaxCode(),
                                                                                        request.getOrigin(),
                                                                                        request.getOriginId(),
                                                                                        request.getSubunitCode()))));
                            } else {
                                return Uni.createFrom().item(response.getInstitutions().get(0));
                            }
                        });
    }

    @Override
    public Uni<Long> updateOnboarding(String onboardingId, Onboarding onboarding) {
        return Onboarding.findById(onboardingId)
                .onItem()
                .transform(Onboarding.class::cast)
                .onItem()
                .transformToUni(
                        onboardingGet ->
                                Objects.isNull(onboardingGet)
                                        ? Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format(
                                                                "Onboarding with id %s is not present!", onboardingId)))
                                        : Uni.createFrom().item(onboardingGet))
                .onItem()
                .transformToUni(id -> updateOnboardingValues(onboardingId, onboarding));
    }

    @Override
    public Uni<CheckManagerResponse> checkManager(CheckManagerRequest checkManagerRequest) {
        CheckManagerResponse response = new CheckManagerResponse();
        UUID userId = checkManagerRequest.getUserId();
        return findOnboardingsByFilters(checkManagerRequest)
                .flatMap(
                        onboardings -> {
                            if (CollectionUtils.isEmpty(onboardings)) {
                                log.debug(
                                        "Onboarding for taxCode {}, origin {}, originId {}, productId {}, subunitCode {} not found",
                                        checkManagerRequest.getTaxCode(),
                                        checkManagerRequest.getOrigin(),
                                        checkManagerRequest.getOriginId(),
                                        checkManagerRequest.getProductId(),
                                        checkManagerRequest.getSubunitCode());

                                response.setResponse(false);
                                return Uni.createFrom().item(response);
                            }

                            String institutionId = onboardings.get(0).getInstitution().getId();
                            return isUserActiveManager(
                                    institutionId,
                                    checkManagerRequest.getProductId(),
                                    String.valueOf(userId))
                                    .map(
                                            isActiveManager -> {
                                                log.debug(
                                                        "User with uuid {} is active manager: {}",
                                                        userId, isActiveManager);
                                                response.setResponse(isActiveManager);
                                                return response;
                                            });
                        });
    }

    /**
     * Retrieves the onboarding record by the given filters.
     *
     * @param checkManagerRequest CheckManagerRequest
     * @return a Uni with the list of onboardings
     * @throws ResourceNotFoundException if the onboarding record is not found
     */
    private Uni<List<Onboarding>> findOnboardingsByFilters(
            CheckManagerRequest checkManagerRequest) {
        return getOnboardingByFilters(
                checkManagerRequest.getTaxCode(),
                checkManagerRequest.getSubunitCode(),
                checkManagerRequest.getOrigin(),
                checkManagerRequest.getOriginId(),
                checkManagerRequest.getProductId())
                .collect()
                .asList();
    }

    /**
     * Checks if the user is an active manager within the institution for the given product invoking
     * selfcare-user API.
     *
     * @param institutionId institution id
     * @param productId     product id
     * @param uuid          user uuid
     * @return a Uni with the result of the check
     */
    private Uni<Boolean> isUserActiveManager(String institutionId, String productId, String uuid) {
        return userService
                .retrieveUserInstitutions(
                        institutionId,
                        List.of(),
                        Objects.nonNull(productId) ? List.of(productId) : null,
                        List.of(String.valueOf(PartyRole.MANAGER)),
                        List.of(String.valueOf(OnboardedProductResponse.StatusEnum.ACTIVE)),
                        uuid)
                .onFailure()
                .invoke(e -> log.error("Error while checking if user is active manager", e))
                .onItem()
                .transform(CollectionUtils::isNotEmpty);
    }

    public Uni<CustomError> checkRecipientCode(String recipientCode, String originId) {
        return onboardingUtils
                .getUoFromRecipientCode(recipientCode)
                .onItem()
                .transformToUni(
                        uoResource -> onboardingUtils.getValidationRecipientCodeError(originId, uoResource));
    }

    private static Uni<Long> updateOnboardingValues(String onboardingId, Onboarding onboarding) {
        Map<String, Object> queryParameter = QueryUtils.createMapForOnboardingUpdate(onboarding);
        Document query = QueryUtils.buildUpdateDocument(queryParameter);
        return Onboarding.update(query)
                .where("_id", onboardingId)
                .onItem()
                .transformToUni(
                        updateItemCount -> {
                            if (updateItemCount == 0) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format(ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED.getMessage(), onboardingId)));
                            }
                            return Uni.createFrom().item(updateItemCount);
                        });
    }

    private Uni<List<Onboarding>> getOnboardingList(List<Onboarding> onboardings) {
        if (onboardings.isEmpty()) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(onboardings);
    }

    /**
     * Initiates the onboarding process for a user in the PG (Persona Giuridica) context. This method
     * performs the following steps:
     *
     * <ul>
     *   <li>Validates the provided user requests to ensure only one manager is onboarded.
     *   <li>Sets the workflow type and status of the onboarding to USERS_PG and PENDING,
     *       respectively.
     *   <li>Retrieves any previous completed onboarding data for the institution and product.
     *   <li>Copies relevant data from the previous onboarding to the current onboarding instance.
     *   <li>Retrieves and sets the manager UID for the new onboarding.
     *   <li>Checks if the user is already a manager within the institution.
     *   <li>Verifies the manager's association with the institution in external registries
     *       (Infocamere or ADE).
     *   <li>Persists the onboarding data and initiates orchestration.
     * </ul>
     *
     * @param onboarding   the onboarding data to process
     * @param userRequests the list of user requests associated with the onboarding
     * @return a Uni that emits the onboarding response upon successful completion
     * @throws InvalidRequestException   if the user list is invalid or the user is already a manager
     * @throws ResourceNotFoundException if no previous onboarding data is found for the institution
     */
    public Uni<OnboardingResponse> onboardingUserPg(
            Onboarding onboarding, List<UserRequest> userRequests) {
        checkOnboardingPgUserList(userRequests);

        return retrievePreviousCompletedOnboarding(onboarding)
                .map(
                        previousOnboarding ->
                                copyDataFromPreviousToCurrentOnboarding(previousOnboarding, onboarding))
                .flatMap(unused -> retrieveAndSetManagerUidOnNewOnboarding(onboarding, userRequests))
                .flatMap(unused -> checkIfUserIsAlreadyManager(onboarding))
                .flatMap(unused -> checkIfUserIsManagerOnRegistries(onboarding, userRequests))
                .onItem()
                .transformToUni(
                        unused ->
                                persistAndStartOrchestrationOnboarding(
                                        onboarding,
                                        orchestrationService.triggerOrchestration(onboarding.getId(), TIMEOUT_ORCHESTRATION_RESPONSE)))
                .onItem()
                .transform(onboardingMapper::toResponse);
    }

    /**
     * Validates the list of user requests to ensure only one manager is present.
     *
     * @param userRequests the list of user requests to validate
     * @throws InvalidRequestException if the user list is empty, contains more than one user, or the
     *                                 user role is not MANAGER
     */
    private void checkOnboardingPgUserList(List<UserRequest> userRequests) {
        if (CollectionUtils.isEmpty(userRequests)
                || userRequests.size() > 1
                || !PartyRole.MANAGER.equals(userRequests.get(0).getRole())) {
            throw new InvalidRequestException(
                    "This API allows the onboarding of only one user with role MANAGER");
        }
    }

    private Uni<Onboarding> retrievePreviousCompletedOnboarding(Onboarding onboarding) {
        log.info(
                "Retrieving previous completed onboarding for taxCode {}, origin {}, productId {}",
                onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getOrigin(),
                onboarding.getProductId());

        return getOnboardingByFilters(
                onboarding.getInstitution().getTaxCode(),
                null,
                String.valueOf(onboarding.getInstitution().getOrigin()),
                null,
                onboarding.getProductId())
                .collect()
                .asList()
                .onItem()
                .transformToUni(this::getOnboardingList)
                .onItem()
                .ifNull()
                .failWith(resourceNotFoundExceptionSupplier(onboarding))
                .map(
                        onboardings ->
                                onboardings.stream()
                                        .filter(o -> Objects.isNull(o.getReferenceOnboardingId()))
                                        .findFirst()
                                        .orElse(null));
    }

    private Supplier<ResourceNotFoundException> resourceNotFoundExceptionSupplier(
            Onboarding onboarding) {
        return () ->
                new ResourceNotFoundException(
                        String.format(
                                "Onboarding not found for taxCode %s, origin %s, productId %s",
                                onboarding.getInstitution().getTaxCode(),
                                onboarding.getInstitution().getOrigin(),
                                onboarding.getProductId()));
    }

    private Onboarding copyDataFromPreviousToCurrentOnboarding(
            Onboarding previousOnboarding, Onboarding currentOnboarding) {
        currentOnboarding.setReferenceOnboardingId(previousOnboarding.getId());
        currentOnboarding.setInstitution(previousOnboarding.getInstitution());
        return currentOnboarding;
    }

    private Uni<Onboarding> retrieveAndSetManagerUidOnNewOnboarding(
            Onboarding onboarding, List<UserRequest> userRequests) {
        return getProductByOnboarding(onboarding)
                .flatMap(
                        product ->
                                validationRole(
                                        userRequests,
                                        validRoles(
                                                product,
                                                PHASE_ADDITION_ALLOWED.ONBOARDING,
                                                onboarding.getInstitution().getInstitutionType()))
                                        .map(unused -> retrieveRoleMappingsFromProduct(product, onboarding)))
                .flatMap(roleMappings -> retrieveUserResources(userRequests, roleMappings))
                .onItem()
                .invoke(onboarding::setUsers)
                .replaceWith(onboarding);
    }

    private Map<PartyRole, ProductRoleInfo> retrieveRoleMappingsFromProduct(
            Product product, Onboarding onboarding) {
        return Objects.nonNull(product.getParent())
                ? product
                .getParent()
                .getRoleMappings(onboarding.getInstitution().getInstitutionType().name())
                : product.getRoleMappings(onboarding.getInstitution().getInstitutionType().name());
    }

    /**
     * Checks if the user is already a manager within the institution invoking selfcare-user API.
     *
     * @param currentOnboarding the current onboarding data
     * @return a Uni that completes if the user is not already a manager, otherwise fails
     * @throws InvalidRequestException if the user is already a manager of the institution
     */
    private Uni<Void> checkIfUserIsAlreadyManager(Onboarding currentOnboarding) {
        String newManagerId =
                currentOnboarding.getUsers().stream()
                        .filter(user -> PartyRole.MANAGER.equals(user.getRole()))
                        .map(User::getId)
                        .findAny()
                        .orElse(null);
        String institutionId = currentOnboarding.getInstitution().getId();

        log.info(
                "Checking if user with id: {} is already manager of the institution with id: {}",
                newManagerId, institutionId);

        return isUserActiveManager(institutionId, currentOnboarding.getProductId(), newManagerId)
                .flatMap(
                        isActiveManager -> {
                            if (isActiveManager) {
                                throw new InvalidRequestException("User is already manager of the institution");
                            }
                            return Uni.createFrom().voidItem();
                        });
    }

    /**
     * Checks if the user is a manager in the external registries based on the institution's origin.
     *
     * @param onboarding   the current onboarding data
     * @param userRequests the list of user requests associated with the onboarding
     * @return a Uni that completes if the user is a valid manager in the registry, otherwise fails
     * @throws InvalidRequestException if the user is not a manager in the external registry
     */
    private Uni<Void> checkIfUserIsManagerOnRegistries(
            Onboarding onboarding, List<UserRequest> userRequests) {
        log.info(
                "Checking if user is manager on registries for onboarding with origin {}",
                onboarding.getInstitution().getOrigin());
        String userTaxCode =
                userRequests.stream()
                        .filter(userRequest -> PartyRole.MANAGER.equals(userRequest.getRole()))
                        .map(UserRequest::getTaxCode)
                        .findAny()
                        .orElse(null);

        String businessTaxCode = onboarding.getInstitution().getTaxCode();

        if (onboarding.getInstitution().getOrigin() == Origin.INFOCAMERE) {
            return checkIfUserIsManagerOnInfocamere(userTaxCode, businessTaxCode);
        } else {
            return checkIfUserIsManagerOnADE(userTaxCode, businessTaxCode);
        }
    }

    /**
     * Checks if the user is a manager in the Infocamere registry.
     *
     * @param userTaxCode     the tax code of the user
     * @param businessTaxCode the tax code of the business (institution)
     * @return a Uni that completes if the user is a manager, otherwise fails
     * @throws InvalidRequestException if the user is not a manager in Infocamere
     */
    private Uni<Void> checkIfUserIsManagerOnInfocamere(String userTaxCode, String businessTaxCode) {
        return infocamereApi
                .institutionsByLegalTaxIdUsingPOST(toGetInstitutionsByLegalDto(userTaxCode))
                .flatMap(
                        businessesResource -> checkIfBusinessIsContained(businessesResource, businessTaxCode));
    }

    private GetInstitutionsByLegalDto toGetInstitutionsByLegalDto(String userTaxCode) {
        return GetInstitutionsByLegalDto.builder()
                .filter(GetInstitutionsByLegalFilterDto.builder().legalTaxId(userTaxCode).build())
                .build();
    }

    /**
     * Validates if the business tax code is contained within the retrieved businesses.
     *
     * @param businessesResource the resource containing businesses data
     * @param taxCode            the tax code to validate against
     * @return a Uni that completes if the tax code is found, otherwise fails
     * @throws InvalidRequestException if the tax code is not found in the businesses resource
     */
    private Uni<Void> checkIfBusinessIsContained(
            BusinessesResource businessesResource, String taxCode) {
        if (Objects.isNull(businessesResource)
                || Objects.isNull(businessesResource.getBusinesses())
                || businessesResource.getBusinesses().stream()
                .noneMatch(business -> business.getBusinessTaxId().equals(taxCode))) {
            throw new InvalidRequestException(NOT_MANAGER_OF_THE_INSTITUTION_ON_THE_REGISTRY.getMessage());
        }

        return Uni.createFrom().voidItem();
    }

    /**
     * Checks if the user is a manager in the ADE (Agenzia delle Entrate) registry.
     *
     * @param userTaxCode     the tax code of the user
     * @param businessTaxCode the tax code of the business (institution)
     * @return a Uni that completes if the user is a manager, otherwise fails
     * @throws InvalidRequestException if the user is not a manager in ADE
     */
    private Uni<Void> checkIfUserIsManagerOnADE(String userTaxCode, String businessTaxCode) {
        return nationalRegistriesApi
                .verifyLegalUsingGET(userTaxCode, businessTaxCode)
                .onItem()
                .transformToUni(
                        legalVerificationResult -> {
                            if (!legalVerificationResult.getVerificationResult()) {
                                throw new InvalidRequestException(NOT_MANAGER_OF_THE_INSTITUTION_ON_THE_REGISTRY.getMessage());
                            }
                            return Uni.createFrom().voidItem();
                        })
                .onFailure(WebApplicationException.class)
                .recoverWithUni(
                        ex -> {
                            // If the user is not manager of the institution, the response status code could be
                            // 400
                            if (((WebApplicationException) ex).getResponse().getStatus() == 400) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        NOT_MANAGER_OF_THE_INSTITUTION_ON_THE_REGISTRY.getMessage()));
                            }

                            return Uni.createFrom().failure(ex);
                        });
    }

    /**
     * Set status of onboarding to DELETED. After this, it invokes onboarding-fn
     * to delete references of users and institution
     *
     * @param onboardingId the id of onboarding
     * @return number of documents that have been updated
     */
    @Override
    public Uni<Long> deleteOnboarding(String onboardingId) {
        log.info("Deleting onboarding with id {}", onboardingId);
        return Onboarding.findById(onboardingId)
                .onItem()
                .transform(Onboarding.class::cast)
                .onItem()
                .transformToUni(
                        onboardingGet ->
                                PENDING.equals(onboardingGet.getStatus()) || USERS.equals(onboardingGet.getWorkflowType())
                                        ? Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format("Onboarding with id %s can't be deleted", onboardingId)))
                                        : Uni.createFrom().item(onboardingGet))
                .onItem()
                .transformToUni(id -> {
                    Map<String, Object> queryParameter = Map.of("status", OnboardingStatus.DELETED.name(),
                            "updatedAt", LocalDateTime.now(),
                            "closedAt", LocalDateTime.now());
                    return updateOnboardingStatus(onboardingId, queryParameter);
                })
                .onItem()
                .transformToUni(
                        onboarding -> orchestrationService.triggerOrchestrationDeleteInstitutionAndUser(onboardingId)
                                .map(ignore -> onboarding));
    }

    private Onboarding buildOnboardingFromAggregate(AggregateInstitutionRequest aggregate) {
        Onboarding onboarding = new Onboarding();
        onboarding.setInstitution(institutionMapper.toEntity(aggregate));
        return onboarding;
    }

    /**
     * Validates the institution tax code against product onboarding rules.
     * Checks if the tax code is an Italian individual CF (16 characters, specific format).
     * If it's an individual CF, allowIndividualOnboarding must be true.
     * If it's not an individual CF, allowCompanyOnboarding must be true.
     *
     * @param taxCode The tax code to validate
     * @param product The product with onboarding rules
     * @throws InvalidRequestException if validation fails
     */
    private void validateTaxCode(String taxCode, Product product) {
        if (StringUtils.isBlank(taxCode)) {
            return;
        }

        boolean isIndividual = isIndividualTaxCode(taxCode);

        if (isIndividual && !product.isAllowIndividualOnboarding()) {
            throw new InvalidRequestException(
                    INDIVIDUAL_ONBOARDING_NOT_ALLOWED.getMessage(),
                    INDIVIDUAL_ONBOARDING_NOT_ALLOWED.getCode()
            );
        }

        if (!isIndividual && !product.isAllowCompanyOnboarding()) {
            throw new InvalidRequestException(
                    COMPANY_ONBOARDING_NOT_ALLOWED.getMessage(),
                    COMPANY_ONBOARDING_NOT_ALLOWED.getCode()
            );
        }
    }

    private boolean isIndividualTaxCode(String taxCode) {
        return INDIVIDUAL_CF_PATTERN
                .matcher(taxCode.toUpperCase(Locale.ITALY))
                .matches();
    }

    @Override
    public Uni<OnboardingGet> retrieveOnboardingByInstitutionId(String institutionId, String productId) {
        return Onboarding.find(
                        "institution.id = ?1 and productId = ?2 and status = ?3",
                        institutionId, productId, COMPLETED)
                .firstResult()
                .map(Onboarding.class::cast)
                .onItem().ifNotNull().transformToUni(onboardingResponseFactory::toGetResponse)
                .onItem().ifNull().failWith(() ->
                        new ResourceNotFoundException(
                                String.format("Onboarding with institutionId=%s and productId=%s not found",
                                        institutionId, productId)
                        )
                );
    }
}
