package it.pagopa.selfcare.onboarding.service.util;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import org.openapi.quarkus.product_json.model.ContractTemplateConfig;
import org.openapi.quarkus.product_json.model.ContractType;
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
        return product != null ? product.getProductId() : null;
    }

    public static int expirationDays(ProductResponse product) {
        return Optional.ofNullable(product)
                .map(ProductResponse::getFeatures)
                .map(features -> features.getExpirationDays())
                .orElse(DEFAULT_EXPIRATION_DAYS);
    }

    public static boolean delegable(ProductResponse product) {
        return Optional.ofNullable(product)
                .map(ProductResponse::getFeatures)
                .map(features -> features.getDelegable())
                .orElse(Boolean.FALSE);
    }

    public static List<PartyRole> validRoles(ProductResponse product, InstitutionType institutionType) {
        return roleMappings(product, institutionType).values().stream()
                .filter(mapping -> Optional.ofNullable(mapping.getPhasesAdditionAllowed())
                        .orElse(List.of()).stream()
                        .anyMatch(phase -> ONBOARDING_PHASE.equalsIgnoreCase(phase)))
                .map(RoleMapping::getRole)
                .filter(Objects::nonNull)
                .map(PartyRole::valueOf)
                .toList();
    }

    public static Map<PartyRole, RoleMapping> roleMappings(ProductResponse product, InstitutionType institutionType) {
        List<RoleMapping> roleMappings = Optional.ofNullable(product)
                .map(ProductResponse::getRoleMappings)
                .orElse(List.of());
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
            matching = roleMappings;
        }
        return matching.stream()
                .filter(mapping -> mapping.getRole() != null)
                .collect(Collectors.toMap(
                        mapping -> PartyRole.valueOf(mapping.getRole()),
                        mapping -> mapping,
                        (first, second) -> first));
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
        return mapping.getInstitutionType() != null
                && mapping.getInstitutionType().name().equals(institutionType);
    }

    private static boolean matchesInstitutionType(ContractTemplateConfig config, String institutionType) {
        return config.getInstitutionType() != null
                && config.getInstitutionType().name().equals(institutionType);
    }

    private static boolean isDefaultInstitutionType(RoleMapping mapping) {
        return mapping.getInstitutionType() == null
                || DEFAULT_INSTITUTION_TYPE.equals(mapping.getInstitutionType().name());
    }

    private static boolean isDefaultInstitutionType(ContractTemplateConfig config) {
        return config.getInstitutionType() == null
                || DEFAULT_INSTITUTION_TYPE.equals(config.getInstitutionType().name());
    }
}
