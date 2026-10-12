package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.AttachmentTemplate;
import it.pagopa.selfcare.onboarding.client.model.ContractTemplate;
import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.client.model.ProductRole;
import it.pagopa.selfcare.onboarding.client.model.ProductRoleInfo;
import it.pagopa.selfcare.onboarding.client.model.ProductStatus;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.client.model.StorageOrigin;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import org.mapstruct.Mapper;
import org.openapi.quarkus.product_json.model.ContractTemplateConfig;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.openapi.quarkus.product_json.model.RoleMapping;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mapper(componentModel = "cdi")
public interface ProductMapper {

    OriginResult toOriginResult(ProductOriginResponse productOriginResponse);

    default Product toProduct(ProductResponse response) {
        Objects.requireNonNull(response, "Product response is required");
        Product product = new Product();
        product.setId(response.getProductId());
        product.setTitle(response.getTitle());
        product.setDescription(response.getDescription());
        product.setParentId(response.getParentId());
        product.setAlias(response.getAlias());
        product.setStatus(response.getStatus() == null ? null : ProductStatus.valueOf(response.getStatus().value()));
        product.setInstitutionTypesAllowed(response.getInstitutionTypesAllowed());
        product.setConsumers(response.getConsumers());
        product.setTestEnvProductIds(response.getTestEnvProductIds());
        if (response.getVisualConfiguration() != null) {
            product.setLogo(response.getVisualConfiguration().getLogoUrl());
            product.setDepictImageUrl(response.getVisualConfiguration().getDepictImageUrl());
            product.setLogoBgColor(response.getVisualConfiguration().getLogoBgColor());
        }
        if (response.getFeatures() != null) {
            product.setEnabled(Boolean.TRUE.equals(response.getFeatures().getEnabled()));
            product.setDelegable(Boolean.TRUE.equals(response.getFeatures().getDelegable()));
            product.setInvoiceable(Boolean.TRUE.equals(response.getFeatures().getInvoiceable()));
            product.setRequiresParentOnboarding(Boolean.TRUE.equals(response.getFeatures().getRequiresParentOnboarding()));
            product.setAllowCompanyOnboarding(Boolean.TRUE.equals(response.getFeatures().getAllowCompanyOnboarding()));
            product.setAllowIndividualOnboarding(Boolean.TRUE.equals(response.getFeatures().getAllowIndividualOnboarding()));
            product.setAllowedInstitutionTaxCode(response.getFeatures().getAllowedInstitutionTaxCode());
            product.setExpirationDate(response.getFeatures().getExpirationDays());
        }
        mapContracts(response.getContracts(), product);
        mapRoleMappings(response.getRoleMappings(), product);
        return product;
    }

    private static void mapContracts(List<ContractTemplateConfig> configs, Product product) {
        Map<String, ContractTemplate> institution = new HashMap<>();
        Map<String, ContractTemplate> institutionAggregator = new HashMap<>();
        Map<String, ContractTemplate> user = new HashMap<>();
        Map<String, ContractTemplate> userAggregator = new HashMap<>();
        for (ContractTemplateConfig config : configs == null ? List.<ContractTemplateConfig>of() : configs) {
            if (config == null || Boolean.FALSE.equals(config.getEnabled())) {
                continue;
            }
            String onboardingType = config.getOnboardingType() == null ? "INSTITUTION" : config.getOnboardingType().value();
            String institutionType = config.getInstitutionType() == null ? "DEFAULT" : config.getInstitutionType().value();
            Map<String, ContractTemplate> target = switch (onboardingType) {
                case "INSTITUTION_AGGREGATOR" -> institutionAggregator;
                case "USER" -> user;
                case "USER_AGGREGATOR" -> userAggregator;
                default -> institution;
            };
            ContractTemplate template = target.computeIfAbsent(institutionType, ignored -> new ContractTemplate());
            if (config.getContractType() == null || config.getContractType().value().equals("CONTRACT")) {
                template.setContractTemplatePath(config.getPath());
                template.setContractTemplateVersion(config.getVersion());
            } else if (config.getName() != null) {
                AttachmentTemplate attachment = new AttachmentTemplate();
                attachment.setName(config.getName());
                attachment.setTemplatePath(config.getPath());
                attachment.setTemplateVersion(config.getVersion());
                attachment.setMandatory(Boolean.TRUE.equals(config.getMandatory()));
                attachment.setGenerated(Boolean.TRUE.equals(config.getGenerated()));
                attachment.setOrder(config.getOrder() == null ? 0 : config.getOrder());
                attachment.setWorkflowType(config.getWorkflowType() == null ? List.of() : config.getWorkflowType().stream()
                        .map(type -> WorkflowType.valueOf(type.value()))
                        .toList());
                attachment.setWorkflowState(config.getWorkflowState() == null ? null
                        : OnboardingStatus.valueOf(config.getWorkflowState()));
                template.setAttachments(append(template.getAttachments(), attachment));
            }
        }
        product.setInstitutionContractMappings(institution);
        product.setInstitutionAggregatorContractMappings(institutionAggregator);
        product.setUserContractMappings(user);
        product.setUserAggregatorContractMappings(userAggregator);
    }

    private static List<AttachmentTemplate> append(List<AttachmentTemplate> attachments, AttachmentTemplate attachment) {
        List<AttachmentTemplate> result = attachments == null ? new ArrayList<>() : new ArrayList<>(attachments);
        result.add(attachment);
        return result;
    }

    private static void mapRoleMappings(List<RoleMapping> mappings, Product product) {
        if (mappings == null) {
            return;
        }
        Map<PartyRole, ProductRoleInfo> global = new EnumMap<>(PartyRole.class);
        Map<String, Map<PartyRole, ProductRoleInfo>> byInstitutionType = new HashMap<>();
        for (RoleMapping mapping : mappings) {
            if (mapping.getRole() == null) {
                continue;
            }
            PartyRole partyRole = PartyRole.valueOf(mapping.getRole());
            Map<PartyRole, ProductRoleInfo> target = mapping.getInstitutionType() == null
                    || mapping.getInstitutionType().value().equals("DEFAULT")
                    ? global
                    : byInstitutionType.computeIfAbsent(mapping.getInstitutionType().value(), ignored -> new EnumMap<>(PartyRole.class));
            ProductRoleInfo roleInfo = target.computeIfAbsent(partyRole, ignored -> new ProductRoleInfo());
            roleInfo.setSkipUserCreation(Boolean.TRUE.equals(mapping.getSkipUserCreation()));
            roleInfo.setExcludeRoleFromUserGroups(Boolean.TRUE.equals(mapping.getExcludeRoleFromUserGroups()));
            roleInfo.setPhasesAdditionAllowed(mapping.getPhasesAdditionAllowed());
            List<ProductRole> roles = roleInfo.getRoles() == null ? new ArrayList<>() : new ArrayList<>(roleInfo.getRoles());
            if (mapping.getBackOfficeRoles() != null) {
                mapping.getBackOfficeRoles().stream().map(role -> {
                    ProductRole mappedRole = new ProductRole();
                    mappedRole.setCode(role.getCode());
                    mappedRole.setLabel(role.getLabel());
                    mappedRole.setDescription(role.getDescription());
                    mappedRole.setProductLabel(role.getProductLabel());
                    mappedRole.setMultiroleGroups(role.getMultiroleGroups());
                    return mappedRole;
                }).forEach(roles::add);
            }
            roleInfo.setRoles(roles);
        }
        product.setRoleMappings(global);
        product.setRoleMappingsByInstitutionType(byInstitutionType);
    }

    RequiredDocumentModel toRequiredDocumentModel(RequiredDocumentResponse response);

    List<RequiredDocumentModel> toRequiredDocumentModelList(List<RequiredDocumentResponse> responses);

    default StorageOrigin toStorageOrigin(String value) {
        return Objects.nonNull(value) ? StorageOrigin.valueOf(value) : null;
    }
}
