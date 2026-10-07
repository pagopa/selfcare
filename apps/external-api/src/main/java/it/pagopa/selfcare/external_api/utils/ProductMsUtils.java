package it.pagopa.selfcare.external_api.utils;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ContractTemplateConfig;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.OnboardingType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RoleMapping;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ContractType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeEnvironmentConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ProductMsUtils {

    private static final String DEFAULT = "DEFAULT";

    private ProductMsUtils() {
    }

    public static PartyRole toPartyRole(String role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case "MANAGER" -> PartyRole.MANAGER;
            case "DELEGATE" -> PartyRole.DELEGATE;
            case "SUB_DELEGATE" -> PartyRole.SUB_DELEGATE;
            case "ADMIN_EA" -> PartyRole.ADMIN_EA;
            default -> PartyRole.OPERATOR;
        };
    }

    public static Map<PartyRole, List<RoleMapping>> getRoleMappings(ProductResponse product, String institutionType) {
        if (product == null || product.getRoleMappings() == null) {
            return null;
        }

        List<RoleMapping> mappings = product.getRoleMappings();
        List<RoleMapping> institutionMappings = mappings.stream()
                .filter(Objects::nonNull)
                .filter(mapping -> isInstitutionType(mapping, institutionType))
                .toList();
        List<RoleMapping> selected = institutionMappings.isEmpty()
                ? mappings.stream().filter(ProductMsUtils::isDefault).toList()
                : institutionMappings;

        Map<PartyRole, List<RoleMapping>> result = new EnumMap<>(PartyRole.class);
        selected.stream()
                .filter(Objects::nonNull)
                .filter(mapping -> mapping.getRole() != null)
                .forEach(mapping -> result.computeIfAbsent(toPartyRole(mapping.getRole()), ignored -> new ArrayList<>()).add(mapping));
        return result;
    }

    public static Optional<ContractTemplateConfig> getInstitutionContract(ProductResponse product, String institutionType) {
        if (product == null || product.getContracts() == null) {
            return Optional.empty();
        }
        Map<String, ContractTemplateConfig> byType = product.getContracts().stream()
                .filter(Objects::nonNull)
                .filter(contract -> contract.getOnboardingType() == OnboardingType.INSTITUTION)
                .filter(contract -> contract.getContractType() == ContractType.CONTRACT)
                .collect(Collectors.toMap(
                        ProductMsUtils::contractInstitutionType,
                        Function.identity(),
                        (previous, current) -> current));

        ContractTemplateConfig contract = institutionType == null ? null : byType.get(institutionType);
        if (contract == null) {
            contract = byType.get(DEFAULT);
        }
        return Optional.ofNullable(contract);
    }

    public static Optional<BackOfficeEnvironmentConfiguration> getProdBackOfficeConfiguration(ProductResponse product) {
        if (product == null || product.getBackOfficeEnvironmentConfigurations() == null) {
            return Optional.empty();
        }
        return product.getBackOfficeEnvironmentConfigurations().stream()
                .filter(Objects::nonNull)
                .filter(configuration -> configuration.getEnv() != null)
                .filter(configuration -> configuration.getEnv().equalsIgnoreCase("prod"))
                .findFirst();
    }

    public static BackOfficeRole getProductRole(String productRole, PartyRole role, ProductResponse product) {
        List<RoleMapping> roleMappings = Stream.concat(
                        Optional.ofNullable(product == null ? null : product.getRoleMappings()).orElse(List.of()).stream()
                                .filter(ProductMsUtils::isDefault),
                        Optional.ofNullable(product == null ? null : product.getRoleMappings()).orElse(List.of()).stream()
                                .filter(mapping -> !isDefault(mapping)))
                .filter(Objects::nonNull)
                .filter(mapping -> Objects.equals(toPartyRole(mapping.getRole()), role))
                .toList();

        if (roleMappings.isEmpty()) {
            throw new IllegalArgumentException(String.format("Role %s not found", role));
        }
        return roleMappings.stream()
                .filter(mapping -> mapping.getBackOfficeRoles() != null)
                .flatMap(mapping -> mapping.getBackOfficeRoles().stream())
                .filter(Objects::nonNull)
                .filter(backOfficeRole -> Objects.equals(backOfficeRole.getCode(), productRole))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("ProductRole %s not found for role %s", productRole, role)));
    }

    private static boolean isDefault(RoleMapping mapping) {
        return mapping == null || mapping.getInstitutionType() == null
                || mapping.getInstitutionType() == InstitutionType.DEFAULT;
    }

    private static boolean isInstitutionType(RoleMapping mapping, String institutionType) {
        return institutionType != null
                && !Objects.equals(mapping.getInstitutionType(), InstitutionType.DEFAULT)
                && Objects.equals(mapping.getInstitutionType() == null ? null : mapping.getInstitutionType().getValue(), institutionType);
    }

    private static String contractInstitutionType(ContractTemplateConfig contract) {
        return contract.getInstitutionType() == null ? DEFAULT : contract.getInstitutionType().getValue();
    }
}



