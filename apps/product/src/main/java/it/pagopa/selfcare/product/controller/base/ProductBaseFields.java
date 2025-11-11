package it.pagopa.selfcare.product.controller.base;

import it.pagopa.selfcare.product.model.BackOfficeConfigurations;
import it.pagopa.selfcare.product.model.ContractTemplate;
import it.pagopa.selfcare.product.model.EmailTemplate;
import it.pagopa.selfcare.product.model.UserRolePermission;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import it.pagopa.selfcare.product.model.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ProductBaseFields {

    private String alias;

    private String productId;

    private List<String> allowedInstitutionTaxCode;

    private Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations;

    private List<String> consumers;

    @NotNull
    private Instant createdAt;

    @NotBlank
    private String createdBy;

    private boolean delegable;

    private String depictImageUrl;

    private String description;

    private Map<String, Map<String, List<EmailTemplate>>> emailTemplates;

    private boolean enabled;

    private Integer expirationDate;

    private String identityTokenAudience;

    private String logo;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "logoBgColor should be in the format #RRGGBB")
    private String logoBgColor;

    private Instant modifiedAt;

    private String modifiedBy;

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
}