package it.pagopa.selfcare.product.controller.base;

import it.pagopa.selfcare.product.model.*;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.model.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductBaseFields {

    private String alias;

    @NotBlank
    private String productId;

    private List<String> allowedInstitutionTaxCode;

    private Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations;

    private List<String> consumers;

    private Instant createdAt;

    private String createdBy;

    private boolean delegable;

    private String depictImageUrl;

    @NotBlank
    private String description;

    private Map<String, Map<String, List<EmailTemplate>>> emailTemplates;

    private boolean enabled;

    private Integer expirationDate;

    private String identityTokenAudience;

    private String logo;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "logoBgColor should be in the format #RRGGBB")
    private String logoBgColor;

    private Map<UserRole, UserRolePermission> roleMappings;

    private ProductStatus status;

    @NotBlank
    private String title;

    private String urlBO;
    private String urlPublic;

    private Map<String, ContractTemplate> institutionAggregatorContractMappings;
    private Map<String, ContractTemplate> institutionContractMappings;
    private Map<String, ContractTemplate> userAggregatorContractMappings;
    private Map<String, ContractTemplate> userContractMappings;

    private boolean invoiceable;

    private List<OriginEntry> institutionOrigins;

    private boolean allowIndividualOnboarding;
    private boolean allowCompanyOnboarding;

    private Integer version;
}