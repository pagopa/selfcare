package it.pagopa.selfcare.user.event.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.constant.OnboardedProductState;
import it.pagopa.selfcare.user.event.entity.OnboardedProduct;
import it.pagopa.selfcare.user.event.entity.UserInfo;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.event.mapper.UserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;


@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class UserInstitutionRepository {
    private static final List<OnboardedProductState> VALID_PRODUCT_STATE = List.of(OnboardedProductState.ACTIVE, OnboardedProductState.PENDING, OnboardedProductState.TOBEVALIDATED);

    private final UserMapper userMapper;

    public Uni<Void> updateUser(UserInstitution userInstitution) {
        OnboardedProductState state = retrieveStatusForGivenInstitution(userInstitution.getProducts());
        return UserInfo.findByIdOptional(userInstitution.getUserId())
                .onItem().transformToUni(opt -> opt.map(entityBase -> {
                            if (VALID_PRODUCT_STATE.contains(state)) {
                                PartyRole role = retrieveRoleForGivenInstitution(userInstitution.getProducts());
                                return updateOrCreateNewUserInfo(opt.get(), userInstitution, role, state);
                            } else {
                                return deleteInstitutionOrAllUserInfo(opt.get(), userInstitution);
                            }
                        })
                        .orElse(Uni.createFrom().voidItem()));
    }

    private Uni<Void> deleteInstitutionOrAllUserInfo(ReactivePanacheMongoEntityBase entityBase, UserInstitution userInstitution) {
        return Uni.createFrom().item((UserInfo) entityBase)
                .flatMap(userInfo -> {
                    if (userInfo.getInstitutions().stream()
                            .anyMatch(userInstitutionRole -> userInstitutionRole.getInstitutionId().equalsIgnoreCase(userInstitution.getInstitutionId()))) {

                        List<UserInstitutionRole> roleList = userInfo.getInstitutions().stream()
                                .filter(userInstitutionRole -> userInstitutionRole.getInstitutionId()
                                        .equalsIgnoreCase(userInstitution.getInstitutionId())).toList();

                        userInfo.setInstitutions(roleList);

                        if (roleList.isEmpty()) {
                            return UserInfo.deleteById(userInstitution.getUserId()).replaceWith(Uni.createFrom().voidItem());
                        } else {
                            return UserInfo.persistOrUpdate(userInfo);
                        }
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    private Uni<Void> updateOrCreateNewUserInfo(ReactivePanacheMongoEntityBase entityBase, UserInstitution userInstitution, PartyRole role, OnboardedProductState state) {
        return Uni.createFrom().item((UserInfo) entityBase)
                .map(userInfo -> replaceOrAddInstitution(userInfo, userInstitution, role, state))
                .flatMap(userInfo -> UserInfo.persistOrUpdate(userInfo));
    }

    private UserInfo replaceOrAddInstitution(UserInfo userInfo, UserInstitution userInstitution, PartyRole role, OnboardedProductState state) {
        if (!CollectionUtils.isEmpty(userInfo.getInstitutions())) {
            userInfo.getInstitutions().stream()
                    .filter(userInstitutionRole -> userInstitution.getInstitutionId().equalsIgnoreCase(userInstitutionRole.getInstitutionId()))
                    .findFirst()
                    .ifPresentOrElse(userInstitutionRole -> {
                                userInstitutionRole.setRole(role);
                                userInstitutionRole.setState(state);
                            },
                            () -> {
                                List<UserInstitutionRole> roleList = new ArrayList<>(userInfo.getInstitutions());
                                roleList.add(userMapper.toUserInstitutionRole(userInstitution, role, state));
                                userInfo.setInstitutions(roleList);
                            });
        }
        return userInfo;
    }

    private PartyRole retrieveRoleForGivenInstitution(List<OnboardedProduct> products) {
        List<PartyRole> list = products.stream()
                .filter(onboardedProduct -> VALID_PRODUCT_STATE.contains(onboardedProduct.getStatus()))
                .map(OnboardedProduct::getRole)
                .toList();
        return Collections.min(list);

    }

    private OnboardedProductState retrieveStatusForGivenInstitution(List<OnboardedProduct> products) {
        List<OnboardedProductState> list = products.stream()
                .map(OnboardedProduct::getStatus)
                .toList();
        return Collections.min(list);
    }
}

