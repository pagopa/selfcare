package it.pagopa.selfcare.product.controller.request;

import it.pagopa.selfcare.product.model.*;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.model.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@ToString(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class ProductPatchRequest {

    private String alias;

    private List<String> allowedInstitutionTaxCode;

    private Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations;

    private List<String> consumers;

    private String createdBy;

    private Boolean delegable;

    private String depictImageUrl;

    private String description;

    private Map<String, List<EmailTemplate>> emailTemplates;

    private Boolean enabled;

    private Integer expirationDate;

    private String identityTokenAudience;

    private String logo;

    private String logoBgColor;

    private Map<UserRole, UserRolePermission> roleMappings;

    private ProductStatus status;

    private String title;

    private String urlBO;
    private String urlPublic;

    private Map<String, ContractTemplate> institutionAggregatorContractMappings;
    private Map<String, ContractTemplate> institutionContractMappings;

    private Map<String, ContractTemplate> userAggregatorContractMappings;
    private Map<String, ContractTemplate> userContractMappings;

    private Boolean invoiceable;

    private List<OriginEntry> institutionOrigins;

    private Boolean allowIndividualOnboarding;
    private Boolean allowCompanyOnboarding;
}