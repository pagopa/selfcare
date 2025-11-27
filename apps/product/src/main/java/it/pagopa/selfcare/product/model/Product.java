package it.pagopa.selfcare.product.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@MongoEntity(collection = "products")
public class Product {

    @BsonId
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String alias;

    private String productId;

    private List<String> allowedInstitutionTaxCode;

    private Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations;

    private List<String> consumers;

    private Instant createdAt;

    private String createdBy;

    @Builder.Default
    private int version = 1;

    private boolean delegable;

    private String depictImageUrl;

    private String description;

    private boolean enabled;

    @Builder.Default
    private int expirationDate = 30;

    private String identityTokenAudience;

    private String logo;
    private String logoBgColor;

    private Map<String, UserRolePermission> roleMappings;

    private ProductStatus status;

    private String title;

    private String urlBO;
    private String urlPublic;

    private Map<String, ContractTemplate> institutionAggregatorContractMappings;
    private Map<String, ContractTemplate> institutionContractMappings;

    private Map<String, ContractTemplate> userAggregatorContractMappings;
    private Map<String, ContractTemplate> userContractMappings;

    private Map<String, Map<String, List<EmailTemplate>>> emailTemplates;

    private boolean invoiceable;

    private List<OriginEntry> institutionOrigins;

    private boolean allowIndividualOnboarding;
    private boolean allowCompanyOnboarding;
}
