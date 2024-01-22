package it.pagopa.selfcare.user.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.mapper.UserInstitutionMapper;
import it.pagopa.selfcare.user.util.QueryUtils;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserInstitutionServiceDefault implements UserInstitutionService {

    private static final String CURRENT = ".$.";
    private static final String CURRENT_ANY = ".$[].";


    private final UserInstitutionMapper userInstitutionMapper;
    private final QueryUtils queryUtils;

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
    public Uni<Long> updateUserStatusDao(String userId, String institutionId, String productId, PartyRole role, String productRole, OnboardedProductState status) {
        String roleString = Objects.isNull(role) ? null : role.name();

        Map<String, Object> fieldToUpdateMap = new HashMap<>();
        if(productFilterIsEmpty(productId, role, productRole)) {
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT_ANY + OnboardedProduct.Fields.status.name(), status);
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT_ANY + OnboardedProduct.Fields.updatedAt.name(), LocalDateTime.now());
        }else{
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT + OnboardedProduct.Fields.status.name(), status);
            fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT + OnboardedProduct.Fields.updatedAt.name(), LocalDateTime.now());
        }

        Map<String, Object> parametersMap = queryUtils.createMapForUserUpdateParameter(userId, institutionId, productId, null, roleString, productRole, null);

        return UserInstitution.update(queryUtils.buildUpdateDocument(fieldToUpdateMap))
                .where(queryUtils.buildQueryDocument(parametersMap));
    }

    @Override
    public Uni<Long> updateUserStatusDaoByRelationshipId(String relationshipId, OnboardedProductState status) {

        Map<String, Object> fieldToUpdateMap = new HashMap<>();
        fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT + OnboardedProduct.Fields.status.name(), status);
        fieldToUpdateMap.put(UserInstitution.Fields.products.name() + CURRENT + OnboardedProduct.Fields.updatedAt.name(), LocalDateTime.now());

        Map<String, Object> parametersMap = queryUtils.createMapForUserUpdateParameter(null, null, null, null, null, null, relationshipId);

        return UserInstitution.update(queryUtils.buildUpdateDocument(fieldToUpdateMap))
                .where(queryUtils.buildQueryDocument(parametersMap));
    }

    @Override
    public Uni<List<UserInstitution>> paginatedFindAllWithFilter(Map<String, Object> queryParameter, Integer page, Integer size) {
        Document query = queryUtils.buildQueryDocument(queryParameter);
        return runUserInstitutionFindQuery(query, null).page(page, size).list();
    }

    @Override
    public Uni<UserInstitution> retrieveFirstFilteredUserInstitution(Map<String, Object> queryParameter) {
        Document query = queryUtils.buildQueryDocument(queryParameter);
        return runUserInstitutionFindQuery(query, null).firstResult();
    }

    private boolean productFilterIsEmpty(String productId, PartyRole role, String productRole) {
        return StringUtils.isBlank(productId) && StringUtils.isBlank(productRole) && role == null;
    }

    public ReactivePanacheQuery<UserInstitution> runUserInstitutionFindQuery(Document query, Document sort) {
        return UserInstitution.find(query, sort);
    }
}
