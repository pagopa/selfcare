package it.pagopa.selfcare.product.utils;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProductUtils {

    /**
     * Returns list of product's PartyRole associates with that PHASE_ADDITION_ALLOWED
     * @param product Product
     * @param phase PHASE_ADDITION_ALLOWED
     * @return List<PartyRole>
     */
    public static List<PartyRole> validRoles(Product product, PHASE_ADDITION_ALLOWED phase, InstitutionType institutionType) {
        return validEntryRoles(product, phase, institutionType)
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Returns list of product's PartyRole associates with that PHASE_ADDITION_ALLOWED and filtering for productRole
     * @param product Product
     * @param phase PHASE_ADDITION_ALLOWED
     * @param productRole String
     * @return List<PartyRole>
     */
    public static List<PartyRole> validRolesByProductRole(Product product, PHASE_ADDITION_ALLOWED phase, String productRole, InstitutionType institutionType) {
        if(Objects.isNull(product)) return List.of();
        return validEntryRoles(product, phase, institutionType)
                .stream()
                .filter(entry -> Objects.nonNull(entry.getValue().getRoles()) &&
                        entry.getValue().getRoles().stream()
                        .anyMatch(item -> productRole.equals(item.getCode())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static List<Map.Entry<PartyRole, ProductRoleInfo>> validEntryRoles(Product product, PHASE_ADDITION_ALLOWED phase, InstitutionType institutionType) {
        if (Objects.isNull(product)) {
            throw new IllegalArgumentException("Product must not be null!");
        }

        String institutionTypeName = Optional.ofNullable(institutionType)
                .map(InstitutionType::name)
                .orElse(null);

        return Optional.ofNullable(product.getRoleMappings(institutionTypeName))
                .orElse(Map.of())
                .entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue().getPhasesAdditionAllowed()) &&
                        entry.getValue().getPhasesAdditionAllowed().contains(phase.value))
                .collect(Collectors.toList());
    }


    /**
     * The getProductRole function takes a productRole, role, and product as parameters.
     * It returns the ProductRole object that matches the given role and product.
     * @param productRole String
     * @param role PartyRole
     * @param product Product
     * @return ProductRole
     * @throws IllegalArgumentException if productRoleInfos not exists for role
     */
    public static ProductRole getProductRole(String productRole, PartyRole role, Product product) {
        List<ProductRoleInfo> productRoleInfos = product.getAllRoleMappings().get(role);
        if (Objects.isNull(productRoleInfos) || productRoleInfos.isEmpty()) {
            throw new IllegalArgumentException(String.format("Role %s not found", role));
        }
        return productRoleInfos.stream()
                .filter(productRoleInfo -> Objects.nonNull(productRoleInfo.getRoles()))
                .flatMap(productRoleInfo -> productRoleInfo.getRoles().stream())
                .filter(prodRole -> prodRole.getCode().equals(productRole))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("ProductRole %s not found for role %s", productRole, role)));
    }
}
