package it.pagopa.selfcare.user.service;

import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import it.pagopa.selfcare.user.constant.SelfCareRole;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.mapper.UserInstitutionMapper;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import it.pagopa.selfcare.user.util.QueryUtils;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.selfcare.user.constant.CollectionUtil.*;
import static it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter.OnboardedProductEnum.*;
import static it.pagopa.selfcare.user.model.constants.OnboardedProductState.*;
import static it.pagopa.selfcare.user.util.GeneralUtils.formatQueryParameterList;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserInstitutionServiceDefault implements UserInstitutionService {

    @ConfigProperty(name = "user-ms.eventhub.users.page-size")
    Integer pageSizeFindUserInstitutions;

    private final UserInstitutionMapper userInstitutionMapper;
    private final QueryUtils queryUtils;
    private final UserUtils userUtils;

    @Override
    public Uni<UserInstitutionResponse> findById(String id) {
        Uni<UserInstitution> userInstitution = UserInstitution.findById(new ObjectId(id));
        return userInstitution.onItem().transform(userInstitutionMapper::toResponse);
    }

    @Override
    public Uni<UserInstitutionResponse> findByInstitutionId(String institutionId) {
        Uni<UserInstitution> userInstitution = UserInstitution.find(UserInstitution.Fields.institutionId.name(), institutionId).firstResult();
        return userInstitution.onItem().transform(userInstitutionMapper::toResponse);
    }

    @Override
    public Multi<UserInstitutionResponse> findByUserId(String userId) {
        Multi<UserInstitution> userInstitutions = UserInstitution.find(UserInstitution.Fields.userId.name(), userId).stream();
        return userInstitutions.onItem().transform(userInstitutionMapper::toResponse);
    }

    @Override
    public Multi<UserInstitution> paginatedFindAllWithFilter(Map<String, Object> queryParameter, Integer page, Integer size) {
        Document query = queryUtils.buildQueryDocument(queryParameter, USER_INSTITUTION_COLLECTION);
        return runUserInstitutionFindQuery(query, null).page(page, size).stream();
    }

    @Override
    public Uni<UserInstitution> retrieveFirstFilteredUserInstitution(Map<String, Object> queryParameter) {
        Document query = queryUtils.buildQueryDocument(queryParameter, USER_INSTITUTION_COLLECTION);
        return runUserInstitutionFindQuery(query, null).firstResult();
    }

    @Override
    public Uni<Long> deleteUserInstitutionProduct(String userId, String institutionId, String productId) {
        OnboardedProductFilter onboardedProductFilter = OnboardedProductFilter.builder().productId(productId).status(ACTIVE.name()).build();
        UserInstitutionFilter userInstitutionFilter = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build();
        Map<String, Object> filterMap = userUtils.retrieveMapForFilter(onboardedProductFilter.constructMap(), userInstitutionFilter.constructMap());
        return updateUserStatusDao(filterMap, OnboardedProductState.DELETED);
    }

    @Override
    public Uni<Long> deleteUserInstitutionProductUsers(String institutionId, String productId) {
        final String institutionIdField = UserInstitution.Fields.institutionId.name();
        final String statusField = UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.status.name();
        final String productIdField = UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.productId.name();
        final String productsElemStatus = UserInstitution.Fields.products.name() + ".$[elem]." + OnboardedProduct.Fields.status.name();
        final String productsElemUpdatedAt = UserInstitution.Fields.products.name() + ".$[elem]." + OnboardedProduct.Fields.updatedAt.name();
        final String elemProductId = "elem." + OnboardedProduct.Fields.productId.name();
        final String elemStatus = "elem." + OnboardedProduct.Fields.status.name();
        return UserInstitution.mongoCollection().updateMany(
                new Document(institutionIdField, institutionId)
                        .append(statusField, new Document("$in", List.of(ACTIVE, SUSPENDED)))
                        .append(productIdField, productId),
                new Document("$set", new Document(productsElemStatus, DELETED)
                        .append(productsElemUpdatedAt, Instant.now())),
                new UpdateOptions().arrayFilters(List.of(new Document(elemProductId, productId)
                        .append(elemStatus, new Document("$in", List.of(ACTIVE, SUSPENDED)))
                ))
        ).map(UpdateResult::getModifiedCount);
    }

    @Override
    public Uni<Long> updateUserStatusWithOptionalFilterByInstitutionAndProduct(String userId, String institutionId, String productId, PartyRole role, String productRole, OnboardedProductState status) {
        Map<String, Object> userInstitutionFilterMap = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        Map<String, Object> filterMap = OnboardedProductFilter.builder().productId(productId).role(role).productRole(productRole).build().constructMap();
        return retrieveFirstFilteredUserInstitution(userUtils.retrieveMapForFilter(userInstitutionFilterMap, filterMap))
                .onItem().transformToUni(userInstitution -> userUtils.checkProductRole(productId, retrieveRoleFromUserInstitution(userInstitution, productId, productRole), productRole)
                        .replaceWith(userInstitution))
                .onItem().transformToUni(userInstitution -> evaluateStatusAndUpdateUserInstitutionProduct(userInstitution, userId, institutionId, productId, role, productRole, status));

    }

    private PartyRole retrieveRoleFromUserInstitution(UserInstitution userInstitution, String productId, String productRole) {
        return userInstitution.getProducts().stream()
                .filter(onboardedProduct -> Objects.nonNull(onboardedProduct.getProductRole()) && onboardedProduct.getProductRole().equalsIgnoreCase(productRole)
                && onboardedProduct.getProductId().equalsIgnoreCase(productId))
                .findFirst()
                .map(OnboardedProduct::getRole)
                .orElse(null);
    }

    private Uni<Long> evaluateStatusAndUpdateUserInstitutionProduct(UserInstitution userInstitution, String userId, String institutionId, String productId, PartyRole role, String productRole, OnboardedProductState status) {
        boolean isProductRoleAlreadyActive = userInstitution.getProducts().stream()
                .anyMatch(onboardedProduct -> onboardedProduct.getProductId().equalsIgnoreCase(productId)
                        && onboardedProduct.getProductRole().equalsIgnoreCase(productRole)
                        && OnboardedProductState.ACTIVE.equals(onboardedProduct.getStatus()));

        if (isProductRoleAlreadyActive && status.equals(ACTIVE)) {
            return Uni.createFrom().item(0L);
        }

        OnboardedProductFilter.OnboardedProductFilterBuilder filterBuilder = OnboardedProductFilter.builder()
                .productId(productId)
                .role(role)
                .productRole(productRole);

        switch (status) {
            case ACTIVE:
                filterBuilder.status(OnboardedProductState.SUSPENDED.name());
                break;
            case SUSPENDED:
            case DELETED:
                filterBuilder.status(OnboardedProductState.ACTIVE.name());
                break;
            default:
                break;
        }

        Map<String, Object> onboardedProductFilterMap = filterBuilder.build().constructMap();
        Map<String, Object> userInstitutionToUpdateFilterMap = UserInstitutionFilter.builder()
                .userId(userId)
                .institutionId(institutionId)
                .build()
                .constructMap();

        Map<String, Object> filterUpdateMap = userUtils.retrieveMapForFilter(onboardedProductFilterMap, userInstitutionToUpdateFilterMap);
        return updateUserStatusDao(filterUpdateMap, status);
    }

    @Override
    public Uni<Long> updateUserCreatedAtByInstitutionAndProduct(String institutionId, List<String> userIds, String productId, OffsetDateTime createdAt) {
        Map<String, Object> onboardedProductFilterMap = OnboardedProductFilter.builder().productId(productId).build().constructMap();
        Map<String, Object> userInstitutionFilterMap = UserInstitutionFilter.builder().userId(formatQueryParameterList(userIds)).institutionId(institutionId).build().constructMap();
        Map<String, Object> filterMap = userUtils.retrieveMapForFilter(onboardedProductFilterMap, userInstitutionFilterMap);
        Map<String, Object> fieldToUpdateMap = Map.of(UserInstitution.Fields.products.name() + CURRENT_ANY + OnboardedProduct.Fields.createdAt.name(), createdAt.toInstant(),
                                                      UserInstitution.Fields.products.name() + CURRENT_ANY + OnboardedProduct.Fields.updatedAt.name(), Instant.now());
        log.info("Update user institution with filter: {} and field to update: {}", filterMap, fieldToUpdateMap);
        return UserInstitution.update(queryUtils.buildUpdateDocument(fieldToUpdateMap))
                .where(queryUtils.buildQueryDocument(filterMap, USER_INSTITUTION_COLLECTION));
    }

    @Override
    public Multi<UserInstitution> findAllWithFilter(Map<String, Object> queryParameter) {
        Document query = queryUtils.buildQueryDocument(queryParameter, USER_INSTITUTION_COLLECTION);
        log.debug("Query: {}", query);
        return runUserInstitutionFindQuery(query, null).stream();
    }

    @Override
    public Multi<UserInstitution> findUserInstitutionsAfterDateWithFilter(Map<String, Object> queryParameter, OffsetDateTime fromDate) {
        Document query = queryUtils.buildQueryDocumentByDate(queryParameter, USER_INSTITUTION_COLLECTION, fromDate);
        return runUserInstitutionFindQuery(query, null).stream();
    }

    @Override
    public Multi<UserInstitution> findUserInstitutionsAfterDateWithFilter(Map<String, Object> queryParameter, OffsetDateTime fromDate, Integer page) {
        Document query = queryUtils.buildQueryDocumentByDate(queryParameter, USER_INSTITUTION_COLLECTION, fromDate);
        return runUserInstitutionFindQuery(query, null).page(Page.ofSize(pageSizeFindUserInstitutions).index(page)).stream();
    }

    @Override
    public Uni<Integer> pageCountUserInstitutionsAfterDateWithFilter(Map<String, Object> queryParameter, OffsetDateTime fromDate) {
        Document query = queryUtils.buildQueryDocumentByDate(queryParameter, USER_INSTITUTION_COLLECTION, fromDate);
        return runUserInstitutionFindQuery(query, null).page(Page.ofSize(pageSizeFindUserInstitutions)).pageCount();
    }

    private Uni<Long> updateUserStatusDao(Map<String, Object> filterMap, OnboardedProductState status) {

        Map<String, Object> fieldToUpdateMap = new HashMap<>();
        if(productFilterIsEmpty(filterMap)) {
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT_ANY + OnboardedProduct.Fields.status.name(), status);
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT_ANY + OnboardedProduct.Fields.updatedAt.name(), Instant.now());
        }else{
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT + OnboardedProduct.Fields.status.name(), status);
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT + OnboardedProduct.Fields.updatedAt.name(), Instant.now());
        }
        return UserInstitution.update(queryUtils.buildUpdateDocument(fieldToUpdateMap))
                .where(queryUtils.buildQueryDocument(filterMap, USER_INSTITUTION_COLLECTION));
    }

    @Override
    public Uni<List<UserInstitution>> retrieveFilteredUserInstitution(Map<String, Object> queryParameter) {
        Document query = queryUtils.buildQueryDocument(queryParameter, USER_INSTITUTION_COLLECTION);
        return runUserInstitutionFindQuery(query, null).list();
    }

    @Override
    public Uni<UserInstitution> findByUserIdAndInstitutionId(String userId, String institutionId) {
        Map<String, Object> userInstitutionFilterMap = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        Document query = queryUtils.buildQueryDocument(userInstitutionFilterMap, USER_INSTITUTION_COLLECTION);
        return runUserInstitutionFindQuery(query, null).firstResult();
    }

    @Override
    public Uni<UserInstitution> persistOrUpdate(UserInstitution userInstitution) {
        return UserInstitution.persistOrUpdate(userInstitution)
                .replaceWith(userInstitution);
    }

    @Override
    public Uni<Boolean> existsValidUserProduct(String userId, String institutionId, String productId, PermissionTypeEnum permission) {

        Map<String, Object> userInstitutionFilterMap = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        OnboardedProductFilter.OnboardedProductFilterBuilder builder = OnboardedProductFilter.builder().productId(productId).status(List.of(ACTIVE, PENDING, TOBEVALIDATED));
        if (permission.equals(PermissionTypeEnum.ADMIN)) {
            builder.role(SelfCareRole.fromSelfCareAuthority(permission.name()));
        }
        Map<String, Object> onboardedProductFilterMap = builder.build().constructMap();
        Map<String, Object> filterMap = userUtils.retrieveMapForFilter(userInstitutionFilterMap, onboardedProductFilterMap);
        Document query = queryUtils.buildQueryDocument(filterMap, USER_INSTITUTION_COLLECTION);

        return runUserInstitutionCountQuery(query);
    }

    @Override
    public Uni<Long> updateInstitutionDescription(String institutionId, UpdateDescriptionDto descriptionDto) {

        Map<String, Object> fieldToUpdateMap = new HashMap<>();
        fieldToUpdateMap.put(UserInstitution.Fields.institutionDescription.name() , descriptionDto.getInstitutionDescription());

        if(Objects.nonNull(descriptionDto.getInstitutionRootName())) {
            fieldToUpdateMap.put(UserInstitution.Fields.institutionRootName.name(), descriptionDto.getInstitutionRootName());
        }

        Map<String, Object> filterMap = Map.of(UserInstitution.Fields.institutionId.name(), institutionId);

        return UserInstitution.update(queryUtils.buildUpdateDocument(fieldToUpdateMap))
                .where(queryUtils.buildQueryDocument(filterMap, USER_INSTITUTION_COLLECTION));

    }


    private boolean productFilterIsEmpty(Map<String, Object> filterMap) {
        return !filterMap.containsKey(PRODUCT_ID.getChild())
                && !filterMap.containsKey(PRODUCT_ROLE.getChild())
                && !filterMap.containsKey(ROLE.getChild());
    }

    public ReactivePanacheQuery<UserInstitution> runUserInstitutionFindQuery(Document query, Document sort) {
        return UserInstitution.find(query, sort);
    }

    public Uni<Boolean> runUserInstitutionCountQuery(Document query) {
        Uni<Long> count = UserInstitution.count(query);
        return count.onItem().transform(c -> c > 0);
    }

    @Override
    public Uni<Long> countUsers(String institutionId, String productId, List<PartyRole> roles, List<OnboardedProductState> status) {
        final Map<String, Object> userFilter = UserInstitutionFilter.builder()
                .institutionId(institutionId)
                .build().constructMap();
        final Map<String, Object> productFilter = OnboardedProductFilter.builder()
                .productId(productId)
                .role(roles)
                .status(status)
                .build().constructMap();

        final Document query = queryUtils.buildQueryDocument(userUtils.retrieveMapForFilter(userFilter, productFilter), USER_INSTITUTION_COLLECTION);
        log.debug("Query: {}", query);

        return UserInstitution.count(query);
    }

}
