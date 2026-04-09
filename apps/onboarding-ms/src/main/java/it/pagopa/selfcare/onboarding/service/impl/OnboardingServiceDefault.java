package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.CheckManagerResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.RegistryManager;
import it.pagopa.selfcare.onboarding.entity.registry.RegistryResourceFactory;
import it.pagopa.selfcare.onboarding.exception.*;
import it.pagopa.selfcare.onboarding.factory.OnboardingResponseFactory;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingDocumentMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.service.OrchestrationService;
import it.pagopa.selfcare.onboarding.service.helper.*;
import it.pagopa.selfcare.onboarding.service.util.*;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.OnboardingApi;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.COMPLETED;
import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.PENDING;
import static it.pagopa.selfcare.onboarding.common.WorkflowType.USERS;
import static it.pagopa.selfcare.onboarding.constants.CustomError.DEFAULT_ERROR;
import static it.pagopa.selfcare.onboarding.constants.CustomError.UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED;

@Slf4j
@ApplicationScoped
public class OnboardingServiceDefault implements OnboardingService {

    public static final String GSP_CATEGORY_INSTITUTION_TYPE = "L37";
    public static final String SCEC_CATEGORY_INSTITUTION_TYPE = "S01G";
    private static final String TIMEOUT_ORCHESTRATION_RESPONSE = "70";

    // -------------------------------------------------------------------------
    // Injected dependencies
    // -------------------------------------------------------------------------

    @RestClient @Inject OnboardingApi onboardingApi;
    @RestClient @Inject DocumentControllerApi documentControllerApi;
    @RestClient @Inject DocumentContentControllerApi documentContentControllerApi;

    @Inject OnboardingMapper onboardingMapper;
    @Inject OnboardingResponseFactory onboardingResponseFactory;
    @Inject InstitutionMapper institutionMapper;
    @Inject ProductService productService;
    @Inject OnboardingDocumentMapper onboardingDocumentMapper;
    @Inject RegistryResourceFactory registryResourceFactory;
    @Inject OrchestrationService orchestrationService;

    // Helpers
    @Inject
    UserRegistryHelper userRegistryHelper;
    @Inject
    OnboardingValidationHelper validationHelper;
    @Inject WorkflowTypeResolver workflowTypeResolver;
    @Inject
    OnboardingPgHelper pgHelper;
    @Inject
    OnboardingPersistenceHelper persistenceHelper;
    @Inject
    OnboardingQueryHelper queryHelper;
    @Inject OnboardingUtils onboardingUtils;

    @ConfigProperty(name = "onboarding.orchestration.enabled")
    Boolean onboardingOrchestrationEnabled;

    // -------------------------------------------------------------------------
    // Public interface — onboarding flows
    // -------------------------------------------------------------------------

    @Override
    public Uni<OnboardingResponse> onboarding(Onboarding onboarding, List<UserRequest> userRequests,
                                               List<AggregateInstitutionRequest> aggregates,
                                               UserRequesterDto userRequester) {
        onboarding.setStatus(OnboardingStatus.REQUEST);
        log.info("Starting onboarding: description={}, origin={}, institutionType={}",
                onboarding.getInstitution().getDescription(), onboarding.getInstitution().getOrigin(),
                onboarding.getInstitution().getInstitutionType());
        return workflowTypeResolver.resolve(onboarding)
                .onItem().invoke(workflowType -> {
                    onboarding.setWorkflowType(workflowType);
                    log.info("Resolved workflowType={} for institution: {}",
                            workflowType, onboarding.getInstitution().getDescription());
                })
                .onItem().transformToUni(workflowType -> computeExpiry(onboarding.getProductId()))
                .onItem().transformToUni(expiry -> {
                    onboarding.setExpiringDate(expiry);
                    return fillUsersAndOnboarding(onboarding, userRequests, aggregates, false, userRequester);
                });
    }

    @Override
    public Uni<OnboardingResponse> onboardingIncrement(Onboarding onboarding, List<UserRequest> userRequests,
                                                        List<AggregateInstitutionRequest> aggregates,
                                                        UserRequesterDto userRequester) {
        onboarding.setWorkflowType(WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR);
        onboarding.setStatus(PENDING);
        log.info("Starting onboardingIncrement: description={}, origin={}, institutionType={}",
                onboarding.getInstitution().getDescription(), onboarding.getInstitution().getOrigin(),
                onboarding.getInstitution().getInstitutionType());
        return computeExpiry(onboarding.getProductId())
                .onItem().transformToUni(expiry -> {
                    onboarding.setExpiringDate(expiry);
                    return persistenceHelper.addReferencedOnboardingId(onboarding)
                            .flatMap(o -> fillUsersAndOnboarding(o, userRequests, aggregates, true, userRequester));
                });
    }

    @Override
    public Uni<OnboardingResponse> onboardingUsers(OnboardingUserRequest request, String userId,
                                                    WorkflowType workflowType) {
        log.info("Starting onboardingUsers: origin={}, institutionType={}, workflowType={}",
                request.getOrigin(), request.getInstitutionType(), workflowType);
        return Uni.createFrom()
                .item(() -> productService.getProductExpirationDate(request.getProductId()))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transformToUni(expirationDays ->
                        queryHelper.getInstitutionFromUserRequest(request)
                                .onItem().transform(response -> {
                                    Onboarding onboarding = onboardingMapper.toEntity(request, userId, workflowType);
                                    it.pagopa.selfcare.onboarding.entity.Institution institution = institutionMapper.toEntity(response);
                                    institution.setInstitutionType(request.getInstitutionType());
                                    onboarding.setInstitution(institution);
                                    onboarding.setExpiringDate(OffsetDateTime.now().plusDays(expirationDays).toLocalDateTime());
                                    return onboarding;
                                })
                                .onItem().transformToUni(onboarding -> verifyExistingOnboarding(onboarding, request.getUsers())));
    }

    @Override
    public Uni<OnboardingResponse> onboardingCompletion(Onboarding onboarding, List<UserRequest> userRequests,
                                                         UserRequesterDto userRequester) {
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
        onboarding.setStatus(OnboardingStatus.REQUEST);
        return fillUsersAndOnboarding(onboarding, userRequests, null, false, userRequester);
    }

    @Override
    public Uni<OnboardingResponse> onboardingPgCompletion(Onboarding onboarding, List<UserRequest> userRequests) {
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
        onboarding.setStatus(PENDING);
        return fillUsersAndOnboarding(onboarding, userRequests, null, false, null);
    }

    @Override
    public Uni<OnboardingResponse> onboardingAggregationCompletion(Onboarding onboarding,
                                                                    List<UserRequest> userRequests,
                                                                    List<AggregateInstitutionRequest> aggregates,
                                                                    UserRequesterDto userRequester) {
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATOR);
        onboarding.setStatus(OnboardingStatus.REQUEST);
        return fillUsersAndOnboarding(onboarding, userRequests, aggregates, false, null);
    }

    @Override
    public Uni<OnboardingResponse> onboardingAggregationImport(Onboarding onboarding,
                                                                OnboardingImportContract contractImported,
                                                                List<UserRequest> userRequests,
                                                                List<AggregateInstitutionRequest> aggregates,
                                                                UserRequesterDto userRequester) {
        onboarding.setWorkflowType(WorkflowType.IMPORT_AGGREGATION);
        onboarding.setStatus(PENDING);
        return fillUsersAndOnboardingForImport(onboarding, userRequests, aggregates, contractImported, userRequester);
    }

    @Override
    public Uni<OnboardingResponse> onboardingImport(Onboarding onboarding, List<UserRequest> userRequests,
                                                     OnboardingImportContract contractImported,
                                                     UserRequesterDto userRequester) {
        onboarding.setWorkflowType(WorkflowType.IMPORT);
        onboarding.setStatus(PENDING);
        return fillUsersAndOnboardingForImport(onboarding, userRequests, null, contractImported, userRequester);
    }

    @Override
    public Uni<OnboardingResponse> onboardingUserPg(Onboarding onboarding, List<UserRequest> userRequests) {
        return pgHelper.onboardingUserPg(onboarding, userRequests);
    }

    // -------------------------------------------------------------------------
    // Public interface — lifecycle management
    // -------------------------------------------------------------------------

    @Override
    public Uni<OnboardingGet> approve(String onboardingId) {
        return queryHelper.retrieveOnboardingAndCheckIfExpired(onboardingId)
                .onItem().transformToUni(queryHelper::checkIfToBeValidated)
                .onItem().transformToUni(onboarding ->
                        product(onboarding.getProductId())
                                .onItem().transformToUni(product ->
                                        validationHelper.verifyAlreadyOnboardingForProductAndProductParent(
                                                onboarding.getInstitution(), product.getId(), product.getParentId()))
                                .replaceWith(onboarding))
                .onItem().transformToUni(onboarding ->
                        onboardingOrchestrationEnabled
                                ? orchestrationService.triggerOrchestration(onboarding.getId(), null).map(ignore -> onboarding)
                                : Uni.createFrom().item(onboarding))
                .flatMap(onboardingResponseFactory::toGetResponse);
    }

    @Override
    public Uni<Onboarding> uploadContractSigned(String onboardingId, FormItem formItem) {
        return queryHelper.retrieveOnboarding(onboardingId)
                .onItem().transformToUni(queryHelper::checkIfCompleted)
                .onItem().transformToUni(onboarding ->
                        userRegistryHelper.retrieveOnboardingUserFiscalCodeList(onboarding)
                                .onItem().transformToUni(fiscalCodes ->
                                        uploadSignedContractAndUpdateDocument(onboarding, formItem, true,
                                                DocumentType.INSTITUTION, fiscalCodes)
                                                .onItem().invoke(ignore -> onboarding.setUpdatedAt(LocalDateTime.now()))
                                                .replaceWith(onboarding)))
                .onItem().transformToUni(onboarding ->
                        updateOnboarding(onboardingId, onboarding)
                                .onItem().transformToUni(ignore ->
                                        onboardingUtils.ensureSuccessfulDocumentResponse(
                                                documentControllerApi.updateDocumentUpdatedAt(onboardingId),
                                                "updateDocumentUpdatedAt", onboardingId))
                                .replaceWith(onboarding));
    }

    @Override
    public Uni<Onboarding> complete(String onboardingId, FormItem formItem) {
        return completeInternal(onboardingId, formItem, false, false);
    }

    @Override
    public Uni<Onboarding> completeWithoutSignatureVerification(String onboardingId, FormItem formItem) {
        return completeInternal(onboardingId, formItem, true, false);
    }

    @Override
    public Uni<Onboarding> completeOnboardingUsers(String onboardingId, FormItem formItem) {
        return completeInternal(onboardingId, formItem, false, true);
    }

    @Override
    public Uni<Long> rejectOnboarding(String onboardingId, String reasonForReject) {
        return Onboarding.findById(onboardingId)
                .onItem().transform(Onboarding.class::cast)
                .onItem().transformToUni(o ->
                        COMPLETED.equals(o.getStatus())
                                ? Uni.createFrom().failure(new InvalidRequestException(
                                        String.format("Onboarding with id %s is COMPLETED!", onboardingId)))
                                : Uni.createFrom().item(o))
                .onItem().transformToUni(id ->
                        OnboardingQueryHelper.updateReasonForRejectAndUpdateStatus(onboardingId, reasonForReject))
                .onItem().transformToUni(onboarding ->
                        onboardingOrchestrationEnabled
                                ? orchestrationService.triggerOrchestration(onboardingId, "60").map(ignore -> onboarding)
                                : Uni.createFrom().item(onboarding));
    }

    @Override
    public Uni<Long> deleteOnboarding(String onboardingId) {
        log.info("Deleting onboarding with id {}", onboardingId);
        return Onboarding.findById(onboardingId)
                .onItem().transform(Onboarding.class::cast)
                .onItem().transformToUni(o ->
                        PENDING.equals(o.getStatus()) || USERS.equals(o.getWorkflowType())
                                ? Uni.createFrom().failure(new InvalidRequestException(
                                        String.format("Onboarding with id %s can't be deleted", onboardingId)))
                                : Uni.createFrom().item(o))
                .onItem().transformToUni(id -> {
                    Map<String, Object> params = Map.of(
                            "status", OnboardingStatus.DELETED.name(),
                            "updatedAt", LocalDateTime.now(),
                            "deletedAt", LocalDateTime.now());
                    return OnboardingQueryHelper.updateOnboardingStatus(onboardingId, params);
                })
                .onItem().transformToUni(onboarding ->
                        orchestrationService.triggerOrchestrationDeleteInstitutionAndUser(onboardingId)
                                .map(ignore -> onboarding));
    }

    // -------------------------------------------------------------------------
    // Public interface — queries
    // -------------------------------------------------------------------------

    @Override
    public Uni<OnboardingGet> onboardingPending(String onboardingId) {
        return onboardingGet(onboardingId)
                .flatMap(o ->
                        PENDING.name().equals(o.getStatus())
                                || OnboardingStatus.TOBEVALIDATED.name().equals(o.getStatus())
                                ? Uni.createFrom().item(o)
                                : Uni.createFrom().failure(new ResourceNotFoundException(
                                        String.format("Onboarding with id %s not found or not in PENDING status!", onboardingId))));
    }

    @Override
    public Uni<OnboardingGetResponse> onboardingGet(OnboardingGetFilters filters) {
        Document sort = queryHelper.buildSortByCreatedAtDesc();
        Map<String, Object> params = QueryUtils.createMapForOnboardingQueryParameter(filters);
        Document query = QueryUtils.buildQuery(params);
        return Uni.combine().all().unis(
                        filters.isSkipPagination()
                                ? queryHelper.runQuery(query, sort).list()
                                : queryHelper.runQuery(query, sort).page(filters.getPage(), filters.getSize()).list(),
                        queryHelper.runQuery(query, null).count())
                .asTuple()
                .onItem().transformToUni(queryHelper::constructOnboardingGetResponse);
    }

    @Override
    public Uni<OnboardingGet> onboardingGet(String onboardingId) {
        return Onboarding.findByIdOptional(onboardingId)
                .onItem().transformToUni(opt ->
                        opt.map(Onboarding.class::cast)
                                .map(onboardingResponseFactory::toGetResponse)
                                .orElseGet(() -> Uni.createFrom().failure(
                                        new ResourceNotFoundException(
                                                String.format("Onboarding with id %s not found!", onboardingId)))));
    }

    @Override
    public Uni<OnboardingGet> onboardingGetWithUserInfo(String onboardingId) {
        return Onboarding.findByIdOptional(onboardingId)
                .onItem().transformToUni(opt ->
                        opt.map(Onboarding.class::cast)
                                .map(o -> Uni.createFrom().item(o))
                                .orElse(Uni.createFrom().failure(
                                        new ResourceNotFoundException(
                                                String.format("Onboarding with id %s not found!", onboardingId)))))
                .flatMap(onboarding ->
                        userRegistryHelper.toUserResponseWithUserInfo(onboarding.getUsers())
                                .flatMap(userResponses ->
                                        onboardingResponseFactory.toGetResponse(onboarding)
                                                .invoke(og -> og.setUsers(userResponses))))
                .flatMap(onboardingGet ->
                        documentControllerApi.getAttachments(onboardingId)
                                .invoke(onboardingGet::setAttachments)
                                .replaceWith(onboardingGet));
    }

    @Override
    public Uni<List<OnboardingResponse>> institutionOnboardings(String taxCode, String subunitCode,
                                                                 String origin, String originId,
                                                                 OnboardingStatus status) {
        Map<String, Object> params = QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                taxCode, subunitCode, origin, originId, status, null);
        Document query = QueryUtils.buildQuery(params);
        return Onboarding.find(query).stream()
                .map(Onboarding.class::cast)
                .map(onboardingMapper::toResponse)
                .collect().asList();
    }

    @Override
    public Uni<List<OnboardingResponse>> verifyOnboarding(String taxCode, String subunitCode, String origin,
                                                           String originId, OnboardingStatus status,
                                                           String productId, InstitutionType institutionType) {
        return validationHelper.verifyOnboarding(taxCode, subunitCode, origin, originId, status, productId, institutionType);
    }

    @Override
    public Uni<Long> updateOnboarding(String onboardingId, Onboarding onboarding) {
        return Onboarding.findById(onboardingId)
                .onItem().transform(Onboarding.class::cast)
                .onItem().transformToUni(o ->
                        Objects.isNull(o)
                                ? Uni.createFrom().failure(new InvalidRequestException(
                                        String.format("Onboarding with id %s is not present!", onboardingId)))
                                : Uni.createFrom().item(o))
                .onItem().transformToUni(id -> OnboardingQueryHelper.updateOnboardingValues(onboardingId, onboarding));
    }

    @Override
    public Uni<CheckManagerResponse> checkManager(CheckManagerRequest request) {
        CheckManagerResponse response = new CheckManagerResponse();
        UUID userId = request.getUserId();
        return queryHelper.findOnboardingsByFilters(request)
                .flatMap(onboardings -> {
                    if (CollectionUtils.isEmpty(onboardings)) {
                        log.debug("No onboarding found for checkManager request: taxCode={}", request.getTaxCode());
                        response.setResponse(false);
                        return Uni.createFrom().item(response);
                    }
                    String institutionId = onboardings.get(0).getInstitution().getId();
                    return pgHelper.isUserActiveManager(institutionId, request.getProductId(), String.valueOf(userId))
                            .map(isActive -> {
                                response.setResponse(isActive);
                                return response;
                            });
                });
    }

    @Override
    public Uni<CustomError> checkRecipientCode(String recipientCode, String originId) {
        return onboardingUtils.getUoFromRecipientCode(recipientCode)
                .onItem().transformToUni(uo -> onboardingUtils.getValidationRecipientCodeError(originId, uo));
    }

    @Override
    public Uni<OnboardingGet> retrieveOnboardingByInstitutionId(String institutionId, String productId) {
        return Onboarding.find("institution.id = ?1 and productId = ?2 and status = ?3",
                        institutionId, productId, COMPLETED)
                .firstResult()
                .map(Onboarding.class::cast)
                .onItem().ifNotNull().transformToUni(onboardingResponseFactory::toGetResponse)
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(
                        String.format("Onboarding with institutionId=%s and productId=%s not found",
                                institutionId, productId)));
    }

    public static boolean isOnboardingExpired(LocalDateTime dateTime) {
        return OnboardingQueryHelper.isOnboardingExpired(dateTime);
    }

    // -------------------------------------------------------------------------
    // Private orchestration helpers
    // -------------------------------------------------------------------------

    private Uni<OnboardingResponse> fillUsersAndOnboarding(Onboarding onboarding, List<UserRequest> userRequests,
                                                            List<AggregateInstitutionRequest> aggregates,
                                                            boolean isAggregatesIncrement,
                                                            UserRequesterDto userRequester) {
        onboarding.setCreatedAt(LocalDateTime.now());
        return verifyExistingOnboarding(onboarding, isAggregatesIncrement)
                .onItem().transformToUni(product ->
                        handleOnboarding(onboarding, userRequests, aggregates, product, userRequester));
    }

    private Uni<Product> verifyExistingOnboarding(Onboarding onboarding, boolean isAggregatesIncrement) {
        return getProductByOnboarding(onboarding)
                .onItem().transformToUni(product ->
                        validationHelper.verifyAlreadyOnboarding(onboarding.getInstitution(),
                                        product.getId(), product.getParentId(), isAggregatesIncrement)
                                .replaceWith(product));
    }

    private Uni<OnboardingResponse> handleOnboarding(Onboarding onboarding, List<UserRequest> userRequests,
                                                      List<AggregateInstitutionRequest> aggregates,
                                                      Product product, UserRequesterDto userRequester) {
        return Uni.createFrom()
                .item(registryResourceFactory.create(onboarding, getManagerTaxCode(userRequests)))
                .onItem().invoke(rm -> rm.setResource(rm.retrieveInstitution()))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transformToUni(rm ->
                        validateAndPersistOnboarding(rm, onboarding, userRequests, aggregates, product, userRequester));
    }

    private Uni<OnboardingResponse> validateAndPersistOnboarding(RegistryManager<?> rm, Onboarding onboarding,
                                                                   List<UserRequest> userRequests,
                                                                   List<AggregateInstitutionRequest> aggregates,
                                                                   Product product, UserRequesterDto userRequester) {
        log.info("Starting validateAndPersistOnboarding for institution: {}",
                onboarding.getInstitution().getDescription());
        return rm.isValid()
                .onItem().transformToUni(ignored -> rm.validateInstitutionType(product))
                .onItem().invoke(() -> validationHelper.validateTaxCode(onboarding.getInstitution().getTaxCode(), product))
                .onItem().transformToUni(ignored -> validationHelper.verifyAllowManagerAsDelegate(userRequests))
                .onItem().transformToUni(ignored -> userRegistryHelper.addUserRequester(userRequester, onboarding.getUserRequester()))
                .onItem().transformToUni(ignored -> rm.customValidation(product))
                .onItem().invoke(() -> onboarding.setTestEnvProductIds(product.getTestEnvProductIds()))
                .onItem().transformToUni(ignored -> validationHelper.validateAggregates(aggregates, getManagerTaxCode(userRequests)))
                .onItem().transformToUni(current -> persistenceHelper.persistOnboarding(onboarding, userRequests, product, aggregates))
                .onItem().transformToUni(persisted ->
                        persistenceHelper.persistAndStartOrchestrationOnboarding(persisted,
                                orchestrationService.triggerOrchestration(persisted.getId(), null)))
                .onItem().transform(onboardingMapper::toResponse);
    }

    private Uni<OnboardingResponse> fillUsersAndOnboardingForImport(Onboarding onboarding,
                                                                      List<UserRequest> userRequests,
                                                                      List<AggregateInstitutionRequest> aggregates,
                                                                      OnboardingImportContract contract,
                                                                      UserRequesterDto userRequester) {
        onboarding.setCreatedAt(LocalDateTime.now());
        return verifyExistingOnboardingForImport(onboarding)
                .onItem().transformToUni(product ->
                        handleOnboardingForImport(onboarding, userRequests, aggregates, product, contract))
                .onFailure(IncrementRequiredException.class)
                .recoverWithUni(throwable -> {
                    log.info("Existing onboarding found for {} and {}, calling onboardingIncrement",
                            onboarding.getInstitution().getTaxCode(), onboarding.getProductId());
                    return onboardingIncrement(onboarding, userRequests, aggregates, userRequester);
                });
    }

    private Uni<Product> verifyExistingOnboardingForImport(Onboarding onboarding) {
        return getProductByOnboarding(onboarding)
                .onItem().transformToUni(product ->
                        validationHelper.verifyAlreadyOnboardingForProductAndProductParent(
                                        onboarding.getInstitution(), product.getId(), product.getParentId())
                                .replaceWith(product)
                                .onFailure(ResourceConflictException.class)
                                .recoverWithUni(throwable ->
                                        validationHelper.handleConflictForImport(onboarding, product)));
    }

    private Uni<OnboardingResponse> handleOnboardingForImport(Onboarding onboarding, List<UserRequest> userRequests,
                                                               List<AggregateInstitutionRequest> aggregates,
                                                               Product product,
                                                               OnboardingImportContract contract) {
        return Uni.createFrom()
                .item(registryResourceFactory.create(onboarding, getManagerTaxCode(userRequests)))
                .onItem().invoke(rm -> rm.setResource(rm.retrieveInstitution()))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transformToUni(rm ->
                        validateAndPersistOnboardingForImport(rm, onboarding, userRequests, aggregates, product, contract));
    }

    private Uni<OnboardingResponse> validateAndPersistOnboardingForImport(RegistryManager<?> rm,
                                                                            Onboarding onboarding,
                                                                            List<UserRequest> userRequests,
                                                                            List<AggregateInstitutionRequest> aggregates,
                                                                            Product product,
                                                                            OnboardingImportContract contract) {
        return rm.isValid()
                .onItem().transformToUni(ignored -> rm.customValidation(product))
                .onItem().invoke(() -> onboarding.setTestEnvProductIds(product.getTestEnvProductIds()))
                .onItem().transformToUni(ignored ->
                        persistenceHelper.persistOnboarding(onboarding, userRequests, product, aggregates))
                .onItem().call(persisted ->
                        onboardingUtils.ensureSuccessfulDocumentResponse(
                                documentControllerApi.persistDocumentForImport(
                                        onboardingDocumentMapper.toRequest(persisted, product, contract)),
                                "persistDocumentForImport", persisted.getId()))
                .onItem().transformToUni(persisted ->
                        persistenceHelper.persistAndStartOrchestrationOnboarding(persisted,
                                orchestrationService.triggerOrchestration(persisted.getId(), TIMEOUT_ORCHESTRATION_RESPONSE)))
                .onItem().transform(onboardingMapper::toResponse);
    }

    private Uni<OnboardingResponse> verifyExistingOnboarding(Onboarding onboarding, List<UserRequest> userRequests) {
        onboarding.setCreatedAt(LocalDateTime.now());
        return getProductByOnboarding(onboarding)
                .onItem().transformToUni(product ->
                        persistenceHelper.addReferencedOnboardingId(onboarding)
                                .onItem().invoke(current -> onboarding.setTestEnvProductIds(product.getTestEnvProductIds()))
                                .onItem().invoke(() -> validationHelper.verifyAllowManagerAsDelegate(userRequests))
                                .onItem().transformToUni(current ->
                                        persistenceHelper.persistOnboarding(onboarding, userRequests, product, null))
                                .onItem().transformToUni(persisted ->
                                        persistenceHelper.persistAndStartOrchestrationOnboarding(persisted,
                                                orchestrationService.triggerOrchestration(persisted.getId(), null)))
                                .onItem().transform(onboardingMapper::toResponse));
    }

    private Uni<Onboarding> completeInternal(String onboardingId, FormItem formItem,
                                              boolean skipSignatureVerification, boolean isUsersFlow) {
        return queryHelper.retrieveOnboardingAndCheckIfExpired(onboardingId)
                .onItem().transformToUni(onboarding ->
                        verifyCompletionPreconditions(onboarding, isUsersFlow).replaceWith(onboarding))
                .onItem().transformToUni(onboarding ->
                        userRegistryHelper.retrieveOnboardingUserFiscalCodeList(onboarding)
                                .onItem().transformToUni(fiscalCodes ->
                                        uploadSignedContractAndUpdateDocument(onboarding, formItem,
                                                skipSignatureVerification,
                                                isUsersFlow ? DocumentType.USER : DocumentType.INSTITUTION,
                                                fiscalCodes)
                                                .replaceWith(onboarding)))
                .onItem().transformToUni(this::triggerOrchestrationIfEnabled);
    }

    private Uni<Boolean> verifyCompletionPreconditions(Onboarding onboarding, boolean isUsersFlow) {
        return product(onboarding.getProductId())
                .onItem().transformToUni(product ->
                        isUsersFlow
                                ? validationHelper.verifyOnboardingNotExistForProductAndProductParent(
                                        onboarding, product.getId(), product.getParentId())
                                : validationHelper.verifyAlreadyOnboardingForProductAndProductParent(
                                        onboarding.getInstitution(), product.getId(), product.getParentId()));
    }

    private Uni<Onboarding> triggerOrchestrationIfEnabled(Onboarding onboarding) {
        return onboardingOrchestrationEnabled
                ? orchestrationService.triggerOrchestration(onboarding.getId(), null).map(ignore -> onboarding)
                : Uni.createFrom().item(onboarding);
    }

    private Uni<Void> uploadSignedContractAndUpdateDocument(Onboarding onboarding, FormItem formItem,
                                                             boolean skipSignatureVerification,
                                                             DocumentType documentType, List<String> fiscalCodes) {
        return getProductByOnboarding(onboarding)
                .flatMap(product -> onboardingUtils.buildUploadSignedContractRequest(
                        onboarding, skipSignatureVerification, formItem, product, documentType, fiscalCodes))
                .flatMap(request ->
                        onboardingUtils.ensureSuccessfulDocumentResponse(
                                documentContentControllerApi.uploadSignedContract(request, onboarding.getId()),
                                "uploadSignedContract", onboarding.getId()));
    }

    private Uni<Product> product(String productId) {
        return Uni.createFrom()
                .item(() -> productService.getProductIsValid(productId))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public Uni<Product> getProductByOnboarding(Onboarding onboarding) {
        return product(onboarding.getProductId())
                .onFailure().transform(ex -> new OnboardingNotAllowedException(
                        String.format(UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED.getMessage(),
                                onboarding.getInstitution().getTaxCode(), onboarding.getProductId()),
                        DEFAULT_ERROR.getCode()));
    }

    private String getManagerTaxCode(List<UserRequest> userRequests) {
        if (Objects.isNull(userRequests)) return null;
        return userRequests.stream()
                .filter(u -> u.getRole().equals(PartyRole.MANAGER))
                .map(UserRequest::getTaxCode)
                .findFirst().orElse(null);
    }

    private Uni<LocalDateTime> computeExpiry(String productId) {
        return Uni.createFrom()
                .item(() -> OffsetDateTime.now()
                        .plusDays(productService.getProductExpirationDate(productId))
                        .toLocalDateTime())
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
