package it.pagopa.selfcare.user.event.repository;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.mapper.CloningMapper;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class UserInstitutionRepository {

    private final CloningMapper cloningMapper;

    public Uni<UserInstitution> propagateUserToAggregate(UserInstitution parentUser, OnboardedProduct parentProduct,
                                                         String aggregateId, String aggregateDescription,
                                                         PartyRole roleToPropagate, String productRoleToPropagate) {
        assert parentProduct.getRoleId() != null; // The roleId is required on the parent
        assert parentProduct.getToAddOnAggregates() != null; // The toAddOnAggregates is required on the parent
        final String userIdField = UserInstitution.Fields.userId.name();
        final String institutionIdField = UserInstitution.Fields.institutionId.name();
        return UserInstitution.find(userIdField + " = ?1 and " + institutionIdField + " = ?2", parentUser.getUserId(), aggregateId)
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
                    return UserInstitution.persist(user).onItem().transformToUni(v ->
                            Uni.createFrom().item(user));
                }));
    }

}

