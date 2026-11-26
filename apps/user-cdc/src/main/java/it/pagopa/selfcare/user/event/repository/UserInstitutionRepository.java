package it.pagopa.selfcare.user.event.repository;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.entity.UserInstitution.Fields;
import it.pagopa.selfcare.user.event.mapper.CloningMapper;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class UserInstitutionRepository {

    private final CloningMapper cloningMapper;

    @ConfigProperty(name = "user-cdc.tenant-id")
    DeploymentTenant deploymentTenant;

    public Uni<UserInstitution> propagateUserToAggregate(UserInstitution parentUser, OnboardedProduct parentProduct,
                                                         String aggregateId, String aggregateDescription,
                                                         PartyRole roleToPropagate, String productRoleToPropagate) {
        assert parentProduct.getRoleId() != null; // The roleId is required on the parent
        assert parentProduct.getToAddOnAggregates() != null; // The toAddOnAggregates is required on the parent
        if (parentUser.getTenantId() != null && !deploymentTenant.name().equals(parentUser.getTenantId())) {
            return Uni.createFrom().failure(new IllegalStateException("Source user institution belongs to another tenant"));
        }
        return UserInstitution.<UserInstitution>find("userId = ?1 and institutionId = ?2", parentUser.getUserId(), aggregateId)
                .firstResultOptional().onItem().transformToUni(userInstitution -> userInstitution.map(u -> {
                    // Update existing user institution
                    log.info("propagateUserToAggregate: Updating existing UserInstitution with roleId {} for userId {} and aggregateId {}",
                            parentProduct.getRoleId(), parentUser.getUserId(), aggregateId);
                    final UserInstitution user = u;
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
                    return updateAggregate(user);
                }).orElseGet(() -> {
                    // Create new user institution
                    log.info("propagateUserToAggregate: Creating new UserInstitution with roleId {} for userId {} and aggregateId {}",
                            parentProduct.getRoleId(), parentUser.getUserId(), aggregateId);
                    final OnboardedProduct product = new OnboardedProduct();
                    cloningMapper.copy(parentProduct, product);
                    product.setRole(roleToPropagate);
                    product.setProductRole(productRoleToPropagate);
                    final UserInstitution user = new UserInstitution();
                    user.setTenantId(deploymentTenant.name());
                    user.setInstitutionId(aggregateId);
                    user.setInstitutionDescription(aggregateDescription);
                    user.setInstitutionRootName(parentUser.getInstitutionDescription());
                    user.setProducts(List.of(product));
                    user.setUserId(parentUser.getUserId());
                    user.setUserMailUuid(parentUser.getUserMailUuid());
                    user.setUserMailUpdatedAt(parentUser.getUserMailUpdatedAt());
                    return user.persist().replaceWith(user);
                }));
    }

    private Uni<UserInstitution> updateAggregate(UserInstitution user) {
        Bson filter = Filters.and(Filters.eq("_id", user.getId()),
                Filters.or(Filters.eq(Fields.tenantId.name(), deploymentTenant.name()),
                        Filters.eq(Fields.tenantId.name(), null)));
        // Leave tenantId out of the update even when the document was read before the backfill.
        Bson update = Updates.combine(
                Updates.set(Fields.products.name(), user.getProducts()),
                setOrUnset(Fields.userMailUuid, user.getUserMailUuid()),
                setOrUnset(Fields.userMailUpdatedAt, user.getUserMailUpdatedAt()),
                setOrUnset(Fields.institutionRootName, user.getInstitutionRootName()),
                setOrUnset(Fields.institutionDescription, user.getInstitutionDescription()));
        return UserInstitution.mongoCollection().updateOne(filter, update)
                .onItem().transform(result -> {
                    if (result.getMatchedCount() != 1) {
                        throw new IllegalStateException("Target user institution no longer exists or belongs to another tenant");
                    }
                    return user;
                });
    }

    private static Bson setOrUnset(Fields field, Object value) {
        return value == null ? Updates.unset(field.name()) : Updates.set(field.name(), value);
    }

    public enum DeploymentTenant {
        AR, PNPG
    }
}
