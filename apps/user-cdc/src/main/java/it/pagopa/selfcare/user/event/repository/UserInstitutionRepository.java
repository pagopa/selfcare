package it.pagopa.selfcare.user.event.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.entity.UserInfo;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.event.mapper.CloningMapper;
import it.pagopa.selfcare.user.event.mapper.UserMapper;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;

import static java.util.function.Predicate.not;


@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class UserInstitutionRepository {
    private static final List<OnboardedProductState> VALID_PRODUCT_STATE = List.of(OnboardedProductState.ACTIVE, OnboardedProductState.PENDING, OnboardedProductState.TOBEVALIDATED);
    private static final String TENANT_ID_FIELD = "tenantId";

    /**
     * Whether untagged documents are still treated as belonging to the event's tenant. Configuration
     * rather than a code constant because the backfill runs at a different time in each environment
     * (Step_1/EPIC.md sub-tasks 2 and 10); both the flag and the null branch must be deleted once
     * every environment runs strict.
     */
    @ConfigProperty(name = "selfcare.tenant.strict-data-isolation", defaultValue = "false")
    boolean strictTenantIsolation;

    private final UserMapper userMapper;
    private final CloningMapper cloningMapper;

    public Uni<Void> updateUser(UserInstitution userInstitution) {
        Optional<OnboardedProductState> optStateToSet = retrieveStatusForGivenInstitution(userInstitution.getProducts());
        return findUserInfoByUserId(userInstitution)
                .onItem().transformToUni(opt -> opt
                        .map(entityBase -> {
                            // Check if user has a record with valid state and role to enter inside dashboard,
                            // in case we must add or update institution reference record
                            if (optStateToSet.isPresent() && VALID_PRODUCT_STATE.contains(optStateToSet.get())) {
                                Optional<PartyRole> optRoleToSet = retrieveRoleForGivenInstitution(userInstitution.getProducts());
                                return optRoleToSet.isPresent()
                                    ? addOrUpdateUserInstitutionRole(opt.get(), userInstitution, optRoleToSet.get(), optStateToSet.get())
                                    : Uni.createFrom().voidItem();
                            } else {
                                return deleteInstitutionOrAllUserInfo(opt.get(), userInstitution);
                            }
                        })
                        .orElse(createNewUserInfo(userInstitution)));
    }

    private Uni<Void> createNewUserInfo(UserInstitution userInstitution) {
        if(CollectionUtils.isEmpty(userInstitution.getProducts())){
            return Uni.createFrom().voidItem();
        }

        Optional<PartyRole> maxRole = retrieveRoleForGivenInstitution(userInstitution.getProducts());
        if(maxRole.isEmpty()){
            return Uni.createFrom().voidItem();
        }

        UserInstitutionRole institutionRole = userInstitution.getProducts().stream()
                .filter(product -> VALID_PRODUCT_STATE.contains(product.getStatus()))
                .filter(product -> maxRole.get().equals(product.getRole()))
                .map(product -> userMapper.toUserInstitutionRole(userInstitution, product.getRole(), product.getStatus()))
                .findAny().orElse(null);

        if(Objects.isNull(institutionRole)){
            return Uni.createFrom().voidItem();
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userInstitution.getUserId());
        userInfo.setInstitutions(List.of(institutionRole));
        userInfo.setTenantId(userInstitution.getTenantId());

        // flow of new user must persist userInfo, if already exists it must be failed
        return UserInfo.persist(userInfo)
                .invoke(() -> log.info("createNewUserInfo for userId {} and institution {}",
                        userInstitution.getUserId(),userInstitution.getInstitutionId()))
                .onFailure().invoke(() -> log.error("createNewUserInfo failed for userId {} and institution {}",
                        userInstitution.getUserId(),userInstitution.getInstitutionId()))
                .replaceWithVoid();
    }

    private Uni<Void> deleteInstitutionOrAllUserInfo(ReactivePanacheMongoEntityBase entityBase, UserInstitution userInstitution) {
        return Uni.createFrom().item((UserInfo) entityBase)
                .flatMap(userInfo -> {
                    if (userInfo.getInstitutions().stream()
                            .anyMatch(userInstitutionRole -> userInstitutionRole.getInstitutionId().equalsIgnoreCase(userInstitution.getInstitutionId()))) {

                        userInfo.getInstitutions().removeIf(userInstitutionRole -> userInstitutionRole.getInstitutionId().equalsIgnoreCase(userInstitution.getInstitutionId()));

                        if (CollectionUtils.isEmpty(userInfo.getInstitutions())) {
                            log.info("deleteInstitutionOrAllUserInfo removing userInfo for userId: {}", userInstitution.getUserId());
                            return UserInfo.deleteById(userInstitution.getUserId()).replaceWithVoid();
                        } else {
                            log.info("deleteInstitutionOrAllUserInfo removing institution {} for userId {}",
                                    userInstitution.getInstitutionId(), userInstitution.getUserId());
                            stampTenantIfMissing(userInfo, userInstitution.getTenantId());
                            return UserInfo.persistOrUpdate(userInfo);
                        }
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    private Uni<Void> addOrUpdateUserInstitutionRole(ReactivePanacheMongoEntityBase entityBase, UserInstitution userInstitution, PartyRole role, OnboardedProductState state) {
        return Uni.createFrom().item((UserInfo) entityBase)
                .onItem().transformToUni(userInfo -> {

                    // If the institution record already exists we must update the record using $set,
                    // otherwise we must add a new record inside array using $addToSet
                    Document institutionRoleAsDocument = getUserInstitutionRoleAsDocument(userInstitution, role, state);

                    return userInfo.getInstitutions().stream()
                            .filter(userInstitutionRole -> userInstitution.getInstitutionId().equalsIgnoreCase(userInstitutionRole.getInstitutionId()))
                            .findAny()
                            .map(userInstitutionRole -> updateUserInstitutionRole(userInstitution.getUserId(), userInstitution.getInstitutionId(), institutionRoleAsDocument,
                                    tenantToSetIfMissing(userInfo, userInstitution)))
                            .orElse(addUserInstitutionRole(userInstitution.getUserId(), institutionRoleAsDocument,
                                    tenantToSetIfMissing(userInfo, userInstitution)));

                })
                .replaceWithVoid();
    }

    private Uni<Long> updateUserInstitutionRole(String userId, String institutionId, Document institution, String tenantIdToSet){
        Document setDocument = new Document("institutions.$", institution);
        Optional.ofNullable(tenantIdToSet).ifPresent(tenant -> setDocument.append(TENANT_ID_FIELD, tenant));
        Document updateAddToSet = new Document("$set", setDocument);
        Document filter = new Document("_id", userId)
                .append("institutions.institutionId", institutionId);

        return UserInfo
                .update(updateAddToSet)
                .where(filter);
    }

    private Uni<Long> addUserInstitutionRole(String userId, Document institution, String tenantIdToSet){
        Document updateAddToSet = new Document("$addToSet", new Document("institutions", institution));
        Optional.ofNullable(tenantIdToSet)
                .ifPresent(tenant -> updateAddToSet.append("$set", new Document(TENANT_ID_FIELD, tenant)));

        return UserInfo
                .update(updateAddToSet)
                .where("_id", userId);
    }

    private static Document getUserInstitutionRoleAsDocument(UserInstitution userInstitution, PartyRole role, OnboardedProductState state) {
        Document institution = new Document("institutionId", userInstitution.getInstitutionId())
                .append("role", role)
                .append("status", state);

        Optional.ofNullable(userInstitution.getInstitutionDescription())
                .ifPresent(value -> institution.append("institutionName", value));
        Optional.ofNullable(userInstitution.getInstitutionRootName())
                .ifPresent(value -> institution.append("institutionRootName", value));
        Optional.ofNullable(userInstitution.getUserMailUuid())
                .ifPresent(value -> institution.append("userMailUuid", value));
        return institution;
    }

    private Optional<PartyRole> retrieveRoleForGivenInstitution(List<OnboardedProduct> products) {
        List<PartyRole> list = products.stream()
                .filter(onboardedProduct -> VALID_PRODUCT_STATE.contains(onboardedProduct.getStatus()))
                .map(OnboardedProduct::getRole)
                .toList();
        return list.isEmpty() ? Optional.empty() : Optional.of(Collections.min(list));

    }

    private Optional<OnboardedProductState> retrieveStatusForGivenInstitution(List<OnboardedProduct> products) {
        return Optional.ofNullable(products)
                .map(productsList -> productsList.stream()
                    .map(OnboardedProduct::getStatus)
                    .toList())
                .filter(not(List::isEmpty))
                .map(Collections::min);
    }

    public Uni<UserInstitution> propagateUserToAggregate(UserInstitution parentUser, OnboardedProduct parentProduct,
                                                         String aggregateId, String aggregateDescription,
                                                         PartyRole roleToPropagate, String productRoleToPropagate) {
        assert parentProduct.getRoleId() != null; // The roleId is required on the parent
        assert parentProduct.getToAddOnAggregates() != null; // The toAddOnAggregates is required on the parent
        final String userIdField = UserInstitution.Fields.userId.name();
        final String institutionIdField = UserInstitution.Fields.institutionId.name();
        return findAggregateUserInstitution(parentUser, aggregateId, userIdField, institutionIdField)
                .firstResultOptional().onItem().transformToUni(userInstitution -> userInstitution.map(u -> {
                    // Update existing user institution
                    log.info("propagateUserToAggregate: Updating existing UserInstitution with roleId {} for userId {} and aggregateId {}",
                            parentProduct.getRoleId(), parentUser.getUserId(), aggregateId);
                    final UserInstitution user = (UserInstitution) u;
                    user.setProducts(user.getProducts() == null ? new ArrayList<>() : new ArrayList<>(user.getProducts()));
                    user.setUserMailUuid(parentUser.getUserMailUuid());
                    user.setUserMailUpdatedAt(parentUser.getUserMailUpdatedAt());
                    user.setInstitutionRootName(parentUser.getInstitutionDescription());
                    user.setInstitutionDescription(aggregateDescription);
                    stampTenantIfMissing(user, parentUser.getTenantId());
                    // Find existing product by roleId, or create a new one if not found
                    final OnboardedProduct product = user.getProducts().stream().filter(p -> parentProduct.getRoleId().equals(p.getRoleId())).findFirst()
                            .orElseGet(() -> {
                                log.info("propagateUserToAggregate: roleId {} not found for userId {} and aggregateId {}, adding new product",
                                        parentProduct.getRoleId(), parentUser.getUserId(), aggregateId);
                                final OnboardedProduct newProduct = new OnboardedProduct();
                                user.getProducts().add(newProduct);
                                return newProduct;
                            });
                    // Copy all properties from parentProduct, but override role and productRole
                    cloningMapper.copy(parentProduct, product);
                    product.setRole(roleToPropagate);
                    product.setProductRole(productRoleToPropagate);
                    return u.update().onItem().transformToUni(up ->
                            Uni.createFrom().item((UserInstitution) up));
                }).orElseGet(() -> {
                    // Create new user institution
                    log.info("propagateUserToAggregate: Creating new UserInstitution with roleId {} for userId {} and aggregateId {}",
                            parentProduct.getRoleId(), parentUser.getUserId(), aggregateId);
                    final OnboardedProduct product = new OnboardedProduct();
                    cloningMapper.copy(parentProduct, product);
                    product.setRole(roleToPropagate);
                    product.setProductRole(productRoleToPropagate);
                    final UserInstitution user = new UserInstitution();
                    user.setInstitutionId(aggregateId);
                    user.setInstitutionDescription(aggregateDescription);
                    user.setInstitutionRootName(parentUser.getInstitutionDescription());
                    user.setProducts(List.of(product));
                    user.setUserId(parentUser.getUserId());
                    user.setUserMailUuid(parentUser.getUserMailUuid());
                    user.setUserMailUpdatedAt(parentUser.getUserMailUpdatedAt());
                    user.setTenantId(parentUser.getTenantId());
                    return UserInstitution.persist(user).onItem().transformToUni(v ->
                            Uni.createFrom().item(user));
                }));
    }

    private Uni<Optional<ReactivePanacheMongoEntityBase>> findUserInfoByUserId(UserInstitution userInstitution) {
        if (userInstitution.getTenantId() == null || userInstitution.getTenantId().isBlank()) {
            // Migration phase: legacy CDC events may carry no tenant, so keep the pre-multitenant
            // unscoped lookup instead of creating an unsatisfiable query. This is not a security boundary.
            return UserInfo.findByIdOptional(userInstitution.getUserId());
        }
        Document query = tenantScopedQuery(new Document("_id", userInstitution.getUserId()), userInstitution.getTenantId());
        return UserInfo.find(query).firstResultOptional();
    }

    private ReactivePanacheQuery<ReactivePanacheMongoEntityBase> findAggregateUserInstitution(UserInstitution parentUser, String aggregateId,
                                                                                             String userIdField, String institutionIdField) {
        if (parentUser.getTenantId() == null || parentUser.getTenantId().isBlank()) {
            // Migration phase: legacy CDC events may carry no tenant, so keep the pre-multitenant
            // unscoped lookup instead of creating an unsatisfiable query. This is not a security boundary.
            return UserInstitution.find(userIdField + " = ?1 and " + institutionIdField + " = ?2", parentUser.getUserId(), aggregateId);
        }
        Document query = tenantScopedQuery(new Document(userIdField, parentUser.getUserId())
                .append(institutionIdField, aggregateId), parentUser.getTenantId());
        return UserInstitution.find(query);
    }

    private Document tenantScopedQuery(Document query, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            // Migration phase: legacy CDC events may carry no tenant, so keep the pre-multitenant
            // unscoped lookup instead of creating an unsatisfiable query. This is not a security boundary.
            return query;
        }
        if (strictTenantIsolation) {
            return query.append(TENANT_ID_FIELD, tenantId);
        }
        // Migration phase: tenantId == null keeps pre-backfill documents visible. The null branch
        // goes away when selfcare.tenant.strict-data-isolation is turned on after the backfill.
        return query.append("$or", List.of(new Document(TENANT_ID_FIELD, tenantId), new Document(TENANT_ID_FIELD, null)));
    }

    private static String tenantToSetIfMissing(UserInfo userInfo, UserInstitution userInstitution) {
        return userInfo.getTenantId() == null ? userInstitution.getTenantId() : null;
    }

    private static void stampTenantIfMissing(UserInfo userInfo, String tenantId) {
        if (userInfo.getTenantId() == null) {
            userInfo.setTenantId(tenantId);
        }
    }

    private static void stampTenantIfMissing(UserInstitution userInstitution, String tenantId) {
        if (userInstitution.getTenantId() == null) {
            userInstitution.setTenantId(tenantId);
        }
    }

}
