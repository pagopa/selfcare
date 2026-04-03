package it.pagopa.selfcare.onboarding.service.util;

import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.COMPLETED;
import static it.pagopa.selfcare.product.utils.ProductUtils.validRoles;

import io.quarkus.mongodb.panache.common.reactive.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.AggregateInstitutionRequest;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.OrchestrationService;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.onboarding.util.SortEnum;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;

/**
 * Helper che gestisce la persistenza degli onboarding e
 * l'avvio dell'orchestrazione associata.
 */
@Slf4j
@ApplicationScoped
public class OnboardingPersistenceHelper {

    private static final String INTEGRATION_PROFILE = "integrationProfile";

    @ConfigProperty(name = "onboarding.orchestration.enabled")
    Boolean onboardingOrchestrationEnabled;

    @ConfigProperty(name = "quarkus.profile")
    String activeProfile;

    @Inject
    UserRegistryHelper userRegistryHelper;

    @Inject
    OnboardingValidationHelper validationHelper;

    @Inject
    OrchestrationService orchestrationService;

    // -------------------------------------------------------------------------
    // Persistenza onboarding
    // -------------------------------------------------------------------------

    /**
     * Persiste l'onboarding, recupera le risorse utente e
     * (se presente un parentId) risolve l'institutionId dal prodotto padre.
     */
    public Uni<Onboarding> persistOnboarding(Onboarding onboarding, List<UserRequest> userRequests,
                                              Product product, List<AggregateInstitutionRequest> aggregates) {
        log.info("Persist onboarding for: product {}, product parent {}", product.getId(), product.getParentId());

        Map<PartyRole, ProductRoleInfo> roleMappings = resolveRoleMappings(product, onboarding);

        Uni<Onboarding> withInstitutionId = Objects.nonNull(product.getParentId())
                ? setInstitutionId(onboarding, product.getParentId())
                : Uni.createFrom().item(onboarding);

        if (INTEGRATION_PROFILE.equals(activeProfile)) {
            return withInstitutionId.onItem().transformToUni(o ->
                    storeAndValidateOnboarding(o, userRequests, product, aggregates, roleMappings));
        }
        return withInstitutionId.onItem().transformToUni(o ->
                Panache.withTransaction(() ->
                        storeAndValidateOnboarding(o, userRequests, product, aggregates, roleMappings)));
    }

    /**
     * Persiste l'onboarding e avvia l'orchestrazione se abilitata.
     */
    public Uni<Onboarding> persistAndStartOrchestrationOnboarding(Onboarding onboarding,
                                                                    Uni<OrchestrationResponse> orchestration) {
        log.info("Persist onboarding and start orchestration {}: taxCode {}, subunitCode {}, type {}",
                onboardingOrchestrationEnabled,
                onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(),
                onboarding.getInstitution().getInstitutionType());

        List<Onboarding> list = List.of(onboarding);
        if (Boolean.TRUE.equals(onboardingOrchestrationEnabled)) {
            return Onboarding.persistOrUpdate(list)
                    .onItem().transformToUni(saved -> orchestration)
                    .replaceWith(onboarding);
        }
        return Onboarding.persistOrUpdate(list).replaceWith(onboarding);
    }

    /**
     * Recupera gli onboarding completati filtrati per i parametri forniti,
     * ordinati per data di creazione discendente.
     */
    public Multi<Onboarding> getOnboardingByFilters(String taxCode, String subunitCode,
                                                      String origin, String originId, String productId) {
        Map<String, Object> params = QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                taxCode, subunitCode, origin, originId, COMPLETED, productId);
        Document sort = QueryUtils.buildSortDocument(Onboarding.Fields.createdAt.name(), SortEnum.DESC);
        Document query = QueryUtils.buildQuery(params);
        return Onboarding.find(query, sort).stream();
    }

    /**
     * Cerca l'onboarding precedente completato e imposta su quello corrente
     * il referenceOnboardingId, il billing e il previousManagerId.
     */
    public Uni<Onboarding> addReferencedOnboardingId(Onboarding onboarding) {
        final String taxCode = onboarding.getInstitution().getTaxCode();
        final String origin = onboarding.getInstitution().getOrigin().name();
        final String originId = onboarding.getInstitution().getOriginId();
        final String productId = onboarding.getProductId();
        final String subunitCode = onboarding.getInstitution().getSubunitCode();

        Multi<Onboarding> onboardings = getOnboardingByFilters(taxCode, subunitCode, origin, originId, productId);

        Uni<Onboarding> current = onboardings
                .filter(item -> Objects.isNull(item.getReferenceOnboardingId()))
                .toUni().onItem().ifNull()
                .failWith(() -> new ResourceNotFoundException(
                        String.format("Onboarding for taxCode %s, origin %s, originId %s, productId %s, subunitCode %s not found",
                                taxCode, origin, originId, productId, subunitCode)))
                .invoke(prev -> {
                    onboarding.setReferenceOnboardingId(prev.getId());
                    onboarding.setBilling(prev.getBilling());
                });

        return current
                .onItem().transformToUni(ignored -> onboardings.collect().first())
                .onItem().invoke(last -> {
                    String prevManagerId = last.getUsers().stream()
                            .filter(u -> u.getRole().equals(PartyRole.MANAGER))
                            .map(User::getId).findFirst().orElse(null);
                    onboarding.setPreviousManagerId(prevManagerId);
                })
                .replaceWith(onboarding);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Uni<Onboarding> storeAndValidateOnboarding(Onboarding onboarding, List<UserRequest> userRequests,
                                                         Product product, List<AggregateInstitutionRequest> aggregates,
                                                         Map<PartyRole, ProductRoleInfo> roleMappings) {
        List<PartyRole> allowedRoles = validRoles(product, PHASE_ADDITION_ALLOWED.ONBOARDING,
                onboarding.getInstitution().getInstitutionType());
        return Onboarding.persist(onboarding)
                .replaceWith(onboarding)
                .onItem().transformToUni(persisted ->
                        validationHelper.validationRole(userRequests, allowedRoles)
                                .onItem().transformToUni(ignore ->
                                        validationHelper.validateUserAggregatesRoles(aggregates, allowedRoles))
                                .onItem().transformToUni(ignore ->
                                        retrieveAndSetUserAggregatesResources(persisted, product, aggregates))
                                .onItem().transformToUni(ignore ->
                                        userRegistryHelper.retrieveUserResources(userRequests, roleMappings))
                                .onItem().invoke(persisted::setUsers)
                                .replaceWith(persisted));
    }

    private Uni<Void> retrieveAndSetUserAggregatesResources(Onboarding onboarding, Product product,
                                                              List<AggregateInstitutionRequest> aggregates) {
        if (aggregates == null || aggregates.isEmpty()) return Uni.createFrom().voidItem();

        Map<PartyRole, ProductRoleInfo> roleMappings = resolveRoleMappings(product, onboarding);

        return Multi.createFrom().iterable(aggregates)
                .filter(a -> a.getUsers() != null && !a.getUsers().isEmpty())
                .onItem().invoke(a -> log.debug("Retrieving user resources for aggregate: {}", a.getTaxCode()))
                .onItem().transformToUni(a ->
                        userRegistryHelper.retrieveUserResources(a.getUsers(), roleMappings)
                                .onFailure().invoke(t -> log.error("Error retrieving user resources for aggregate: {}", a.getTaxCode(), t))
                                .onItem().invoke(users -> setUsersInAggregate(onboarding, a, users)))
                .concatenate().onItem().ignoreAsUni();
    }

    private static void setUsersInAggregate(Onboarding onboarding, AggregateInstitutionRequest aggregate,
                                             List<User> users) {
        onboarding.getAggregates().stream()
                .filter(ai -> Optional.ofNullable(ai.getSubunitCode()).equals(Optional.ofNullable(aggregate.getSubunitCode()))
                        && ai.getTaxCode().equals(aggregate.getTaxCode()))
                .findAny()
                .ifPresent(ai -> ai.setUsers(users));
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
                .onItem().transformToUni(list -> {
                    if (!list.isEmpty()) {
                        onboarding.getInstitution().setId(list.get(0).getInstitution().getId());
                    } else {
                        throw new ResourceNotFoundException(
                                String.format("Onboarding for taxCode %s, origin %s, originId %s, parentId %s, subunitCode %s not found and institutionType %s",
                                        taxCode, origin, originId, parentId, subunitCode, institutionType));
                    }
                    return Uni.createFrom().item(onboarding);
                });
    }

    private static Map<PartyRole, ProductRoleInfo> resolveRoleMappings(Product product, Onboarding onboarding) {
        return Objects.nonNull(product.getParent())
                ? product.getParent().getRoleMappings(onboarding.getInstitution().getInstitutionType().name())
                : product.getRoleMappings(onboarding.getInstitution().getInstitutionType().name());
    }
}


