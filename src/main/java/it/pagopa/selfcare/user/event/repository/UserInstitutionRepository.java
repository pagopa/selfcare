package it.pagopa.selfcare.user.event.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.entity.UserInfo;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.event.mapper.UserMapper;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.function.Predicate.not;


@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class UserInstitutionRepository {
    private static final List<OnboardedProductState> VALID_PRODUCT_STATE = List.of(OnboardedProductState.ACTIVE, OnboardedProductState.PENDING, OnboardedProductState.TOBEVALIDATED);

    private final UserMapper userMapper;

    public Uni<Void> updateUser(UserInstitution userInstitution) {
        Optional<OnboardedProductState> optStateToSet = retrieveStatusForGivenInstitution(userInstitution.getProducts());
        return UserInfo.findByIdOptional(userInstitution.getUserId())
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
                            .map(userInstitutionRole -> updateUserInstitutionRole(userInstitution.getUserId(), userInstitution.getInstitutionId(), institutionRoleAsDocument))
                            .orElse(addUserInstitutionRole(userInstitution.getUserId(), institutionRoleAsDocument));

                })
                .replaceWithVoid();
    }

    private Uni<Long> updateUserInstitutionRole(String userId, String institutionId, Document institution){
        Document updateAddToSet = new Document("$set", new Document("institutions.$", institution));
        Document filter = new Document("_id", userId)
                .append("institutions.institutionId", institutionId);

        return UserInfo
                .update(updateAddToSet)
                .where(filter);
    }

    private Uni<Long> addUserInstitutionRole(String userId, Document institution){
        Document updateAddToSet = new Document("$addToSet", new Document("institutions", institution));

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
}

