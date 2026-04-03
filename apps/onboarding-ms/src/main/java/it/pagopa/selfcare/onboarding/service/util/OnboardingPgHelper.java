package it.pagopa.selfcare.onboarding.service.util;

import static it.pagopa.selfcare.onboarding.constants.CustomError.*;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.OrchestrationService;
import it.pagopa.selfcare.onboarding.service.UserService;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.*;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessesResource;
import org.openapi.quarkus.party_registry_proxy_json.model.GetInstitutionsByLegalDto;
import org.openapi.quarkus.party_registry_proxy_json.model.GetInstitutionsByLegalFilterDto;

import static it.pagopa.selfcare.product.utils.ProductUtils.validRoles;

/**
 * Helper che gestisce il flusso di onboarding specifico per le
 * Persone Giuridiche (PG), inclusa la verifica sui registri esterni.
 */
@Slf4j
@ApplicationScoped
public class OnboardingPgHelper {

    private static final String TIMEOUT_ORCHESTRATION_RESPONSE = "70";

    @RestClient @Inject InfocamereApi infocamereApi;
    @RestClient @Inject NationalRegistriesApi nationalRegistriesApi;

    @Inject OnboardingMapper onboardingMapper;
    @Inject OrchestrationService orchestrationService;
    @Inject ProductService productService;
    @Inject UserService userService;
    @Inject UserRegistryHelper userRegistryHelper;
    @Inject OnboardingPersistenceHelper persistenceHelper;
    @Inject OnboardingValidationHelper validationHelper;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Avvia il flusso di onboarding per un utente PG, verificando
     * ruolo manager, associazione al registro e assenza di duplicati.
     */
    public Uni<OnboardingResponse> onboardingUserPg(Onboarding onboarding, List<UserRequest> userRequests) {
        checkOnboardingPgUserList(userRequests);
        return retrievePreviousCompletedOnboarding(onboarding)
                .map(prev -> copyDataFromPrevious(prev, onboarding))
                .flatMap(unused -> retrieveAndSetManagerUid(onboarding, userRequests))
                .flatMap(unused -> checkIfUserIsAlreadyManager(onboarding))
                .flatMap(unused -> checkIfUserIsManagerOnRegistries(onboarding, userRequests))
                .onItem().transformToUni(unused ->
                        persistenceHelper.persistAndStartOrchestrationOnboarding(onboarding,
                                orchestrationService.triggerOrchestration(onboarding.getId(), TIMEOUT_ORCHESTRATION_RESPONSE)))
                .onItem().transform(onboardingMapper::toResponse);
    }

    /** Verifica se l'utente è già manager attivo dell'istituzione per il prodotto dato. */
    public Uni<Boolean> isUserActiveManager(String institutionId, String productId, String uuid) {
        return userService.retrieveUserInstitutions(
                        institutionId, List.of(),
                        Objects.nonNull(productId) ? List.of(productId) : null,
                        List.of(String.valueOf(PartyRole.MANAGER)),
                        List.of(String.valueOf(OnboardedProductResponse.StatusEnum.ACTIVE)),
                        uuid)
                .onFailure().invoke(e -> log.error("Error while checking if user is active manager", e))
                .onItem().transform(CollectionUtils::isNotEmpty);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void checkOnboardingPgUserList(List<UserRequest> userRequests) {
        if (CollectionUtils.isEmpty(userRequests) || userRequests.size() > 1
                || !PartyRole.MANAGER.equals(userRequests.get(0).getRole())) {
            throw new InvalidRequestException("This API allows the onboarding of only one user with role MANAGER");
        }
    }

    private Uni<Onboarding> retrievePreviousCompletedOnboarding(Onboarding onboarding) {
        log.info("Retrieving previous completed onboarding for taxCode {}, origin {}, productId {}",
                onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getOrigin(),
                onboarding.getProductId());
        return persistenceHelper.getOnboardingByFilters(
                        onboarding.getInstitution().getTaxCode(), null,
                        String.valueOf(onboarding.getInstitution().getOrigin()), null,
                        onboarding.getProductId())
                .collect().asList()
                .onItem().transformToUni(list ->
                        list.isEmpty() ? Uni.createFrom().nullItem() : Uni.createFrom().item(list))
                .onItem().ifNull().failWith(resourceNotFoundFor(onboarding))
                .onItem().transform(list -> list.stream()
                        .filter(o -> Objects.isNull(o.getReferenceOnboardingId()))
                        .findFirst().orElse(null));
    }

    private Onboarding copyDataFromPrevious(Onboarding previous, Onboarding current) {
        current.setReferenceOnboardingId(previous.getId());
        current.setInstitution(previous.getInstitution());
        return current;
    }

    private Uni<Onboarding> retrieveAndSetManagerUid(Onboarding onboarding, List<UserRequest> userRequests) {
        return getProductByOnboarding(onboarding)
                .flatMap(product ->
                        validationHelper.validationRole(userRequests,
                                        validRoles(product, PHASE_ADDITION_ALLOWED.ONBOARDING,
                                                onboarding.getInstitution().getInstitutionType()))
                                .map(unused -> resolveRoleMappings(product, onboarding)))
                .flatMap(roleMappings -> userRegistryHelper.retrieveUserResources(userRequests, roleMappings))
                .onItem().invoke(onboarding::setUsers)
                .replaceWith(onboarding);
    }

    private Uni<Void> checkIfUserIsAlreadyManager(Onboarding onboarding) {
        String newManagerId = onboarding.getUsers().stream()
                .filter(u -> PartyRole.MANAGER.equals(u.getRole()))
                .map(User::getId).findAny().orElse(null);
        String institutionId = onboarding.getInstitution().getId();
        log.info("Checking if user {} is already manager of institution {}", newManagerId, institutionId);
        return isUserActiveManager(institutionId, onboarding.getProductId(), newManagerId)
                .flatMap(isActive -> {
                    if (isActive) throw new InvalidRequestException("User is already manager of the institution");
                    return Uni.createFrom().voidItem();
                });
    }

    private Uni<Void> checkIfUserIsManagerOnRegistries(Onboarding onboarding, List<UserRequest> userRequests) {
        log.info("Checking if user is manager on registries, origin {}", onboarding.getInstitution().getOrigin());
        String userTaxCode = userRequests.stream()
                .filter(u -> PartyRole.MANAGER.equals(u.getRole()))
                .map(UserRequest::getTaxCode).findAny().orElse(null);
        String businessTaxCode = onboarding.getInstitution().getTaxCode();
        return Origin.INFOCAMERE.equals(onboarding.getInstitution().getOrigin())
                ? checkManagerOnInfocamere(userTaxCode, businessTaxCode)
                : checkManagerOnAde(userTaxCode, businessTaxCode);
    }

    private Uni<Void> checkManagerOnInfocamere(String userTaxCode, String businessTaxCode) {
        return infocamereApi.institutionsByLegalTaxIdUsingPOST(toGetInstitutionsByLegalDto(userTaxCode))
                .flatMap(businesses -> validateBusinessContained(businesses, businessTaxCode));
    }

    private Uni<Void> validateBusinessContained(BusinessesResource businessesResource, String taxCode) {
        if (Objects.isNull(businessesResource)
                || Objects.isNull(businessesResource.getBusinesses())
                || businessesResource.getBusinesses().stream()
                        .noneMatch(b -> b.getBusinessTaxId().equals(taxCode))) {
            throw new InvalidRequestException(NOT_MANAGER_OF_THE_INSTITUTION_ON_THE_REGISTRY.getMessage());
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> checkManagerOnAde(String userTaxCode, String businessTaxCode) {
        return nationalRegistriesApi.verifyLegalUsingGET(userTaxCode, businessTaxCode)
                .onItem().transformToUni(result -> {
                    if (!result.getVerificationResult()) {
                        throw new InvalidRequestException(NOT_MANAGER_OF_THE_INSTITUTION_ON_THE_REGISTRY.getMessage());
                    }
                    return Uni.createFrom().voidItem();
                })
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex -> {
                    if (((WebApplicationException) ex).getResponse().getStatus() == 400) {
                        return Uni.createFrom().failure(
                                new InvalidRequestException(NOT_MANAGER_OF_THE_INSTITUTION_ON_THE_REGISTRY.getMessage()));
                    }
                    return Uni.createFrom().failure(ex);
                });
    }

    private GetInstitutionsByLegalDto toGetInstitutionsByLegalDto(String userTaxCode) {
        return GetInstitutionsByLegalDto.builder()
                .filter(GetInstitutionsByLegalFilterDto.builder().legalTaxId(userTaxCode).build())
                .build();
    }

    private static Map<PartyRole, ProductRoleInfo> resolveRoleMappings(Product product, Onboarding onboarding) {
        return Objects.nonNull(product.getParent())
                ? product.getParent().getRoleMappings(onboarding.getInstitution().getInstitutionType().name())
                : product.getRoleMappings(onboarding.getInstitution().getInstitutionType().name());
    }

    private Uni<Product> getProductByOnboarding(Onboarding onboarding) {
        return Uni.createFrom()
                .item(() -> productService.getProductIsValid(onboarding.getProductId()))
                .onFailure().transform(ex -> new OnboardingNotAllowedException(
                        String.format(UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED.getMessage(),
                                onboarding.getInstitution().getTaxCode(), onboarding.getProductId()),
                        DEFAULT_ERROR.getCode()));
    }

    private Supplier<ResourceNotFoundException> resourceNotFoundFor(Onboarding onboarding) {
        return () -> new ResourceNotFoundException(
                String.format("Onboarding not found for taxCode %s, origin %s, productId %s",
                        onboarding.getInstitution().getTaxCode(),
                        onboarding.getInstitution().getOrigin(),
                        onboarding.getProductId()));
    }
}

