package it.pagopa.selfcare.product.model.dto.base;

import it.pagopa.selfcare.product.model.*;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
public class ProductBaseFields {

    @NotBlank
    private String productId;

    private String alias;
    private String title;
    private String description;

    private ProductStatus status;
    private List<String> consumers;

    private VisualConfiguration visualConfiguration;
    private Features features;

    private List<RoleMapping> roleMappings;
    private List<ContractTemplateConfig> contracts;
    private List<OriginEntry> institutionOrigins;
    private List<EmailTemplateConfig> emailTemplates;
    private List<BackOfficeEnvironmentConfiguration> backOfficeEnvironmentConfigurations;
}