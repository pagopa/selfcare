package it.pagopa.selfcare.onboarding.service.helper;

import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.COMPLETED;
import static it.pagopa.selfcare.onboarding.constants.CustomError.*;
import static it.pagopa.selfcare.onboarding.util.ErrorMessage.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.controller.request.AggregateInstitutionRequest;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.RegistryResourceFactory;
import it.pagopa.selfcare.onboarding.exception.*;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserSearchDto;

/**
 * Helper che raccoglie tutta la logica di validazione del processo di onboarding:
 * verifica duplicati, validazione tax code, ruoli, aggregati e permissioni prodotto.
 */
@Slf4j
@ApplicationScoped
public class OnboardingValidationHelper {

    private static final Pattern INDIVIDUAL_CF_PATTERN =
            Pattern.compile("^[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]$");

    @Inject
    ProductService productService;

    @RestClient
    @Inject
    UserApi userRegistryApi;

    @Inject
    OnboardingMapper onboardingMapper;

    @Inject
    RegistryResourceFactory registryResourceFactory;

    @Inject
    InstitutionMapper institutionMapper;

    // -------------------------------------------------------------------------
    // Verifica duplicati onboarding
    // -------------------------------------------------------------------------

    /**
     * Verifica se il prodotto (e il suo parent) è già stato sottoscritto dall'istituzione.
     * Nel flusso aggregates-increment, il conflitto è atteso: viene ignorato.
     */
    public Uni<Void> verifyAlreadyOnboarding(Institution institution, String productId,
                                              String parentId, boolean isAggregatesIncrement) {
        if (isAggregatesIncrement) {
            return verifyAlreadyOnboardingForProductAndProductParent(institution, productId, parentId)
                    .onFailure(ResourceConflictException.class).recoverWithNull()
                    .replaceWithVoid();
        }
        return verifyAlreadyOnboardingForProductAndProductParent(institution, productId, parentId)
                .replaceWithVoid();
    }

    public Uni<Boolean> verifyAlreadyOnboardingForProductAndProductParent(
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
                                                            productParentId, institution.getTaxCode()),
                                                    PARENT_PRODUCT_NOT_ONBOARDED.getCode())))
                                    .onFailure(ResourceConflictException.class)
                                    .recoverWithNull().replaceWith(Uni.createFrom().item(true)));
        } else {
            log.info("Verifying already onboarding for institution: {}, productId: {}",
                    institution.getDescription(), productId);
            return checkIfAlreadyOnboardingAndValidateAllowedProductList(institution, productId);
        }
    }

    public Uni<Boolean> verifyOnboardingNotExistForProductAndProductParent(
            Onboarding onboarding, String productId, String productParentId) {
        return Objects.nonNull(productParentId)
                ? checkIfOnboardingNotExistAndValidateAllowedProductList(onboarding, productParentId)
                        .onFailure(ResourceConflictException.class)
                        .recoverWithUni(ignore -> checkIfOnboardingNotExistAndValidateAllowedProductList(onboarding, productId))
                : checkIfOnboardingNotExistAndValidateAllowedProductList(onboarding, productId);
    }

    /** Gestisce il caso di conflitto nel flusso di import aggregator. */
    public Uni<Product> handleConflictForImport(Onboarding onboarding, Product product) {
        log.info("Handling conflict for import: institutionTaxCode: {}, productId: {}, isAggregator: {}",
                onboarding.getInstitution().getTaxCode(), product.getId(), onboarding.getIsAggregator());
        if (!isAggregatorProdIo(onboarding)) {
            return Uni.createFrom().failure(createConflictException(product, onboarding.getInstitution()));
        }
        Institution institution = onboarding.getInstitution();
        String origin = institution.getOrigin() != null ? institution.getOrigin().getValue() : null;
        return verifyOnboarding(institution.getTaxCode(), institution.getSubunitCode(),
                        origin, institution.getOriginId(), COMPLETED, product.getId(), institution.getInstitutionType())
                .onItem().transformToUni(responses -> {
                    boolean hasNonConfirmationAggregate = responses.stream()
                            .anyMatch(r -> !Objects.equals(r.getWorkflowType(),
                                    it.pagopa.selfcare.onboarding.common.WorkflowType.CONFIRMATION_AGGREGATE.name()));
                    if (hasNonConfirmationAggregate) {
                        log.info("Found onboarding with workflowType != {}, taxCode: {}",
                                it.pagopa.selfcare.onboarding.common.WorkflowType.CONFIRMATION_AGGREGATE.name(),
                                institution.getTaxCode());
                        return Uni.createFrom().failure(createIncrementRequiredException(product, institution));
                    }
                    log.info("All onboardings are {}, ignoring conflict for taxCode: {}",
                            it.pagopa.selfcare.onboarding.common.WorkflowType.CONFIRMATION_AGGREGATE.name(),
                            institution.getTaxCode());
                    return Uni.createFrom().item(product);
                });
    }

    // -------------------------------------------------------------------------
    // Validazione utenti e ruoli
    // -------------------------------------------------------------------------

    /**
     * Verifica che lo stesso codice fiscale non abbia più email distinte
     * tra i ruoli MANAGER e DELEGATE.
     */
    public Uni<Void> verifyAllowManagerAsDelegate(List<UserRequest> userRequests) {
        log.info("Starting verifyAllowManagerAsDelegate");
        boolean ok = userRequests.stream()
                .filter(u -> u.getRole() == PartyRole.MANAGER || u.getRole() == PartyRole.DELEGATE)
                .filter(u -> u.getTaxCode() != null && !u.getTaxCode().isBlank())
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .collect(Collectors.groupingBy(
                        u -> u.getTaxCode().trim().toLowerCase(),
                        Collectors.mapping(u -> u.getEmail().trim().toLowerCase(), Collectors.toSet())))
                .values().stream()
                .allMatch(emails -> emails.size() <= 1);

        if (!ok) {
            return Uni.createFrom().failure(new InvalidRequestException(
                    VALIDATION_USER_BY_TAXCODE.getMessage(), VALIDATION_USER_BY_TAXCODE.getCode()));
        }
        return Uni.createFrom().voidItem();
    }

    public Uni<List<UserRequest>> validationRole(List<UserRequest> users, List<PartyRole> validRoles) {
        List<UserRequest> invalid = users.stream().filter(u -> !validRoles.contains(u.getRole())).toList();
        if (!invalid.isEmpty()) {
            String rolesStr = invalid.stream().map(u -> u.getRole().toString()).collect(Collectors.joining(","));
            return Uni.createFrom().failure(new InvalidRequestException(
                    String.format(ROLES_NOT_ADMITTED_ERROR.getMessage(), rolesStr),
                    ROLES_NOT_ADMITTED_ERROR.getCode()));
        }
        return Uni.createFrom().item(users);
    }

    public Uni<Void> validateUserAggregatesRoles(List<AggregateInstitutionRequest> aggregates,
                                                  List<PartyRole> validRoles) {
        log.debug("Starting validateUserAggregatesRoles");
        if (aggregates == null || aggregates.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        return Multi.createFrom().iterable(aggregates)
                .filter(a -> a.getUsers() != null && !a.getUsers().isEmpty())
                .onItem().invoke(a -> log.debug("Validating roles for aggregate: {}", a.getTaxCode()))
                .onItem().transformToUniAndMerge(a ->
                        validationRole(a.getUsers(), validRoles)
                                .onFailure().invoke(t -> log.error("Error validating roles for aggregate: {}", a.getTaxCode(), t)))
                .collect().asList().replaceWithVoid();
    }

    // -------------------------------------------------------------------------
    // Validazione tax code
    // -------------------------------------------------------------------------

    /**
     * Valida il tax code rispetto alle regole del prodotto:
     * se è un CF persona fisica richiede {@code allowIndividualOnboarding},
     * altrimenti {@code allowCompanyOnboarding}.
     */
    public void validateTaxCode(String taxCode, Product product) {
        if (StringUtils.isBlank(taxCode)) return;
        boolean isIndividual = INDIVIDUAL_CF_PATTERN.matcher(taxCode.toUpperCase(Locale.ITALY)).matches();
        if (isIndividual && !product.isAllowIndividualOnboarding()) {
            throw new InvalidRequestException(
                    INDIVIDUAL_ONBOARDING_NOT_ALLOWED.getMessage(), INDIVIDUAL_ONBOARDING_NOT_ALLOWED.getCode());
        }
        if (!isIndividual && !product.isAllowCompanyOnboarding()) {
            throw new InvalidRequestException(
                    COMPANY_ONBOARDING_NOT_ALLOWED.getMessage(), COMPANY_ONBOARDING_NOT_ALLOWED.getCode());
        }
    }

    // -------------------------------------------------------------------------
    // Validazione aggregati
    // -------------------------------------------------------------------------

    /**
     * Valida gli aggregati verificando che i dati presenti nel registry proxy
     * corrispondano a quelli della richiesta.
     */
    public Uni<Void> validateAggregates(List<AggregateInstitutionRequest> aggregates,
                                         String managerTaxCode) {
        if (aggregates == null) return Uni.createFrom().voidItem();

        List<Uni<Void>> validations = aggregates.stream()
                .map(aggregate -> Uni.createFrom()
                        .item(registryResourceFactory.create(buildOnboardingFromAggregate(aggregate), managerTaxCode))
                        .onItem().invoke(rm -> rm.setResource(rm.retrieveInstitution()))
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .onItem().transformToUni(rm ->
                                rm.isValid()
                                        .onItem().invoke(() -> log.debug("Aggregate valid: taxCode: {}, originId: {}",
                                                aggregate.getTaxCode(), aggregate.getOriginId()))
                                        .onFailure().invoke(f -> log.warn("Aggregate validation failed: taxCode: {}, originId: {}, reason: {}",
                                                aggregate.getTaxCode(), aggregate.getOriginId(), f.getMessage()))
                                        .onFailure().recoverWithUni(Uni.createFrom()::failure))
                        .replaceWithVoid())
                .collect(Collectors.toList());

        return Uni.combine().all().unis(validations).discardItems();
    }

    // -------------------------------------------------------------------------
    // Query verifyOnboarding
    // -------------------------------------------------------------------------

    public Uni<List<OnboardingResponse>> verifyOnboarding(
            String taxCode, String subunitCode, String origin, String originId,
            OnboardingStatus status, String productId, InstitutionType institutionType) {

        return buildQueryParams(taxCode, originId, institutionType)
                .onItem().transformToUni(queryParams -> {
                    if (Objects.isNull(queryParams)) return Uni.createFrom().item(List.of());
                    return findOnboardings(queryParams.taxCode(), queryParams.originId(),
                            subunitCode, origin, status, productId);
                });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Uni<Boolean> checkIfAlreadyOnboardingAndValidateAllowedProductList(
            Institution institution, String productId) {
        return validateAllowedProductList(institution.getTaxCode(), institution.getSubunitCode(), productId)
                .flatMap(ignored -> {
                    String origin = institution.getOrigin() != null ? institution.getOrigin().getValue() : null;
                    return verifyOnboarding(institution.getTaxCode(), institution.getSubunitCode(),
                                    origin, institution.getOriginId(), COMPLETED, productId, institution.getInstitutionType())
                            .flatMap(responses -> handleOnboardingResponses(responses, productId, institution.getTaxCode()));
                });
    }

    private Uni<Boolean> handleOnboardingResponses(List<OnboardingResponse> responses,
                                                    String productId, String taxCode) {
        if (responses.isEmpty()) return Uni.createFrom().item(Boolean.TRUE);
        if (isProductIoWithoutReferenceOnboarding(productId, responses)) return Uni.createFrom().item(Boolean.TRUE);
        return Uni.createFrom().failure(new ResourceConflictException(
                String.format(PRODUCT_ALREADY_ONBOARDED.getMessage(), productId, taxCode),
                PRODUCT_ALREADY_ONBOARDED.getCode()));
    }

    private boolean isProductIoWithoutReferenceOnboarding(String productId, List<OnboardingResponse> responses) {
        return ProductId.PROD_IO.getValue().equals(productId)
                && responses.stream().allMatch(r -> Objects.nonNull(r.getReferenceOnboardingId()));
    }

    private Uni<Boolean> checkIfOnboardingNotExistAndValidateAllowedProductList(
            Onboarding onboarding, String productId) {
        return validateAllowedProductList(onboarding.getInstitution().getTaxCode(),
                        onboarding.getInstitution().getSubunitCode(), productId)
                .flatMap(ignored -> {
                    if (Objects.isNull(onboarding.getReferenceOnboardingId())) {
                        return Uni.createFrom().failure(new InvalidRequestException(
                                INVALID_REFERENCE_ONBORADING.getMessage(), INVALID_REFERENCE_ONBORADING.getCode()));
                    }
                    return Onboarding.findByIdOptional(onboarding.getReferenceOnboardingId())
                            .onItem().transformToUni(opt ->
                                    opt.map(Onboarding.class::cast)
                                            .filter(ref -> ref.getStatus().equals(COMPLETED))
                                            .map(ref -> Uni.createFrom().item(Boolean.TRUE))
                                            .orElse(Uni.createFrom().failure(new InvalidRequestException(
                                                    String.format(PRODUCT_NOT_ONBOARDED.getMessage(),
                                                            onboarding.getProductId(),
                                                            onboarding.getInstitution().getTaxCode(),
                                                            PRODUCT_NOT_ONBOARDED.getCode())))));
                });
    }

    private Uni<Boolean> validateAllowedProductList(String taxCode, String subunitCode, String productId) {
        log.info("Validating allowed map for: taxCode {}, subunitCode {}, product {}", taxCode, subunitCode, productId);
        if (!validateByProductOrInstitutionTaxCode(productId, taxCode)) {
            return Uni.createFrom().failure(new OnboardingNotAllowedException(
                    String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE.getMessage(), taxCode, productId),
                    DEFAULT_ERROR.getCode()));
        }
        return Uni.createFrom().item(Boolean.TRUE);
    }

    private boolean validateByProductOrInstitutionTaxCode(String productId, String taxCode) {
        return productService.isProductEnabled(productId)
                || productService.verifyAllowedByInstitutionTaxCode(productId, taxCode);
    }

    record QueryParams(String taxCode, String originId) {}

    private Uni<QueryParams> buildQueryParams(String taxCode, String originId, InstitutionType institutionType) {
        if (InstitutionType.PRV_PF.equals(institutionType)) {
            return userRegistryApi.searchUsingPOST(UserRegistryHelper.USERS_FIELD_LIST,
                            new UserSearchDto().fiscalCode(taxCode))
                    .onItem().transform(u -> {
                        String userId = u.getId().toString();
                        return new QueryParams(userId, userId);
                    })
                    .onFailure().recoverWithUni(t -> {
                        if (t instanceof WebApplicationException wae && wae.getResponse().getStatus() == 404) {
                            return Uni.createFrom().nullItem();
                        }
                        return Uni.createFrom().failure(t);
                    });
        }
        return Uni.createFrom().item(new QueryParams(taxCode, originId));
    }

    private Uni<List<OnboardingResponse>> findOnboardings(String taxCode, String originId, String subunitCode,
                                                            String origin, OnboardingStatus status, String productId) {
        Map<String, Object> params = QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                taxCode, subunitCode, origin, originId, status, productId);
        Document query = QueryUtils.buildQuery(params);
        return Onboarding.find(query).stream()
                .map(Onboarding.class::cast)
                .map(onboardingMapper::toResponse)
                .filter(r -> Objects.isNull(r.getReferenceOnboardingId()))
                .collect().asList();
    }

    private boolean isAggregatorProdIo(Onboarding onboarding) {
        return Boolean.TRUE.equals(onboarding.getIsAggregator())
                && it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO.getValue().equals(onboarding.getProductId());
    }

    public ResourceConflictException createConflictException(Product product, Institution institution) {
        return new ResourceConflictException(
                String.format(PRODUCT_ALREADY_ONBOARDED.getMessage(), product.getId(), institution.getTaxCode()),
                PRODUCT_ALREADY_ONBOARDED.getCode());
    }

    private IncrementRequiredException createIncrementRequiredException(Product product, Institution institution) {
        return new IncrementRequiredException(
                String.format(PRODUCT_ALREADY_ONBOARDED.getMessage(), product.getId(), institution.getTaxCode()),
                PRODUCT_ALREADY_ONBOARDED.getCode());
    }

    public Onboarding buildOnboardingFromAggregate(AggregateInstitutionRequest aggregate) {
        Onboarding onboarding = new Onboarding();
        onboarding.setInstitution(institutionMapper.toEntity(aggregate));
        return onboarding;
    }
}


