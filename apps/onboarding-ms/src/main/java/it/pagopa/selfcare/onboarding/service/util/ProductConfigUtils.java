package it.pagopa.selfcare.onboarding.service.util;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import org.openapi.quarkus.product_json.model.ContractTemplateConfig;
import org.openapi.quarkus.product_json.model.ContractType;
import org.openapi.quarkus.product_json.model.Features;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RoleMapping;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ProductConfigUtils {

    public static final int DEFAULT_EXPIRATION_DAYS = 30;
    public static final String DEFAULT_INSTITUTION_TYPE = "DEFAULT";
    public static final String ONBOARDING_PHASE = "onboarding";

    private ProductConfigUtils() {
    }

    public static String productId(ProductResponse product) {
        return Objects.nonNull(product) ? product.getProductId() : null;
    }

    public static int expirationDays(ProductResponse product) {
        return Optional.ofNullable(product)
                .map(ProductResponse::getFeatures)
                .map(Features::getExpirationDays)
                .orElse(DEFAULT_EXPIRATION_DAYS);
    }

    public static boolean delegable(ProductResponse product) {
        return Optional.ofNullable(product)
                .map(ProductResponse::getFeatures)
                .map(Features::getDelegable)
                .orElse(Boolean.FALSE);
    }

    public static List<PartyRole> validRoles(ProductResponse product, InstitutionType institutionType) {
        return roleMappings(product, institutionType).values().stream()
                .filter(mapping -> Optional.ofNullable(mapping.getPhasesAdditionAllowed())
                        .orElse(List.of()).stream()
                        .anyMatch(ONBOARDING_PHASE::equalsIgnoreCase))
                .map(RoleMapping::getRole)
                .filter(Objects::nonNull)
                .map(PartyRole::valueOf)
                .toList();
    }

    public static Map<PartyRole, RoleMapping> roleMappings(ProductResponse product, InstitutionType institutionType) {
        if (Objects.isNull(product) || Objects.isNull(product.getRoleMappings())) {
            throw new IllegalStateException("Role mappings are missing for product " + productId(product));
        }
        List<RoleMapping> roleMappings = product.getRoleMappings();
        if (roleMappings.isEmpty()) {
            return Map.of();
        }
        if (roleMappings.stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("Null role mapping for product " + productId(product));
        }
        String institutionTypeName = Optional.ofNullable(institutionType).map(Enum::name).orElse(null);
        List<RoleMapping> matching = roleMappings.stream()
                .filter(mapping -> matchesInstitutionType(mapping, institutionTypeName))
                .toList();
        if (matching.isEmpty()) {
            matching = roleMappings.stream()
                    .filter(ProductConfigUtils::isDefaultInstitutionType)
                    .toList();
        }
        if (matching.isEmpty()) {
            throw new IllegalStateException("Role mappings are missing for product " + productId(product)
                    + " and institution type " + institutionTypeName);
        }
        return matching.stream()
                .collect(Collectors.toMap(
                        mapping -> partyRole(mapping, product),
                        mapping -> mapping,
                        (first, second) -> first));
    }

    private static PartyRole partyRole(RoleMapping mapping, ProductResponse product) {
        try {
            return PartyRole.valueOf(mapping.getRole());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("Invalid role mapping for product " + productId(product), exception);
        }
    }

    public static ContractTemplateConfig institutionContractTemplate(ProductResponse product, String institutionType) {
        List<ContractTemplateConfig> contracts = Optional.ofNullable(product)
                .map(ProductResponse::getContracts)
                .orElse(List.of());
        return contracts.stream()
                .filter(config -> ContractType.CONTRACT.equals(config.getContractType()))
                .filter(config -> matchesInstitutionType(config, institutionType))
                .findFirst()
                .or(() -> contracts.stream()
                .filter(config -> ContractType.CONTRACT.equals(config.getContractType()))
                        .filter(ProductConfigUtils::isDefaultInstitutionType)
                        .findFirst())
                .orElseGet(ContractTemplateConfig::new);
    }

    private static boolean matchesInstitutionType(RoleMapping mapping, String institutionType) {
        return Objects.nonNull(mapping.getInstitutionType())
                && mapping.getInstitutionType().name().equals(institutionType);
    }

    private static boolean matchesInstitutionType(ContractTemplateConfig config, String institutionType) {
        return Objects.nonNull(config.getInstitutionType())
                && config.getInstitutionType().name().equals(institutionType);
    }

    private static boolean isDefaultInstitutionType(RoleMapping mapping) {
        return Objects.isNull(mapping.getInstitutionType())
                || DEFAULT_INSTITUTION_TYPE.equals(mapping.getInstitutionType().name());
    }

    private static boolean isDefaultInstitutionType(ContractTemplateConfig config) {
        return Objects.isNull(config.getInstitutionType())
                || DEFAULT_INSTITUTION_TYPE.equals(config.getInstitutionType().name());
    }
}
