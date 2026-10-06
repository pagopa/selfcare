package it.pagopa.selfcare.onboarding.connector.model.product;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** BFF-owned product snapshot; REST DTOs are translated into this model at the connector boundary. */
@Data
public class Product {

    public static final String CONTRACT_TYPE_DEFAULT = "DEFAULT";

    private String id;
    private String title;
    private String description;
    private String parentId;
    private String alias;
    private String logo;
    private String depictImageUrl;
    private String logoBgColor;
    private ProductStatus status;
    private boolean enabled;
    private boolean delegable;
    private boolean invoiceable;
    private boolean requiresParentOnboarding;
    private boolean allowCompanyOnboarding;
    private boolean allowIndividualOnboarding;
    private Integer expirationDate;
    private List<String> institutionTypesAllowed;
    private List<String> consumers;
    private List<String> testEnvProductIds;
    private List<String> allowedInstitutionTaxCode;
    private Map<String, ContractTemplate> institutionContractMappings;
    private Map<String, ContractTemplate> institutionAggregatorContractMappings;
    private Map<String, ContractTemplate> userContractMappings;
    private Map<String, ContractTemplate> userAggregatorContractMappings;
    private Map<PartyRole, ProductRoleInfo> roleMappings;
    private Map<String, Map<PartyRole, ProductRoleInfo>> roleMappingsByInstitutionType;

    public ContractTemplate getUserContractTemplate(String institutionType) {
        return getContractTemplate(userContractMappings, institutionType);
    }

    public ContractTemplate getInstitutionContractTemplate(String institutionType) {
        return getContractTemplate(institutionContractMappings, institutionType);
    }

    public ContractTemplate getUserAggregatorContractTemplate(String institutionType) {
        return getContractTemplate(userAggregatorContractMappings, institutionType);
    }

    public ContractTemplate getInstitutionAggregatorContractTemplate(String institutionType) {
        return getContractTemplate(institutionAggregatorContractMappings, institutionType);
    }

    public Map<PartyRole, ProductRoleInfo> getRoleMappings(String institutionType) {
        if (institutionType != null && roleMappingsByInstitutionType != null
                && roleMappingsByInstitutionType.containsKey(institutionType)) {
            return roleMappingsByInstitutionType.get(institutionType);
        }
        return roleMappings;
    }

    private ContractTemplate getContractTemplate(Map<String, ContractTemplate> templates, String institutionType) {
        if (templates != null) {
            if (institutionType != null && templates.containsKey(institutionType)) {
                return templates.get(institutionType);
            }
            if (templates.containsKey(CONTRACT_TYPE_DEFAULT)) {
                return templates.get(CONTRACT_TYPE_DEFAULT);
            }
        }
        return new ContractTemplate();
    }
}


