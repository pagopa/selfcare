package it.pagopa.selfcare.user.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.mapper.UserInstitutionMapper;
import it.pagopa.selfcare.user.util.QueryUtils;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.*;

import static it.pagopa.selfcare.user.constant.CollectionUtil.*;
import static it.pagopa.selfcare.user.constant.OnboardedProductState.ACTIVE;
import static it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter.OnboardedProductEnum.*;
import static it.pagopa.selfcare.user.util.GeneralUtils.formatQueryParameterList;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserInstitutionServiceDefault implements UserInstitutionService {

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
    public Uni<Long> updateUserCreatedAtByInstitutionAndProduct(String institutionId, List<String> userIds, String productId, LocalDateTime createdAt) {
        Map<String, Object> onboardedProductFilterMap = OnboardedProductFilter.builder().productId(productId).build().constructMap();
        Map<String, Object> userInstitutionFilterMap = UserInstitutionFilter.builder().userId(formatQueryParameterList(userIds)).institutionId(institutionId).build().constructMap();
        Map<String, Object> filterMap = userUtils.retrieveMapForFilter(onboardedProductFilterMap, userInstitutionFilterMap);
        Map<String, Object> fieldToUpdateMap = Map.of(UserInstitution.Fields.products.name() + CURRENT_ANY + OnboardedProduct.Fields.createdAt.name(), createdAt,
                                                      UserInstitution.Fields.products.name() + CURRENT_ANY + OnboardedProduct.Fields.updatedAt.name(), LocalDateTime.now());
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

    private Uni<Long> updateUserStatusDao(Map<String, Object> filterMap, OnboardedProductState status) {

        Map<String, Object> fieldToUpdateMap = new HashMap<>();
        if(productFilterIsEmpty(filterMap)) {
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT_ANY + OnboardedProduct.Fields.status.name(), status);
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT_ANY + OnboardedProduct.Fields.updatedAt.name(), LocalDateTime.now());
        }else{
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT + OnboardedProduct.Fields.status.name(), status);
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT + OnboardedProduct.Fields.updatedAt.name(), LocalDateTime.now());
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


    private boolean productFilterIsEmpty(Map<String, Object> filterMap) {
        return !filterMap.containsKey(PRODUCT_ID.getChild())
                && !filterMap.containsKey(PRODUCT_ROLE.getChild())
                && !filterMap.containsKey(ROLE.getChild());
    }

    public ReactivePanacheQuery<UserInstitution> runUserInstitutionFindQuery(Document query, Document sort) {
        return UserInstitution.find(query, sort);
    }
}
