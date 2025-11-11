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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@MongoEntity(collection = "products")
public class Product {

    @BsonId
    private String id;

    private String alias;

    private String productId;

    private List<String> allowedInstitutionTaxCode;

    private Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations;

    private List<String> consumers;

    private Instant createdAt;

    private String createdBy;

    private Instant updatedAt;

    private Integer version;

    private boolean delegable;

    private String depictImageUrl;

    private String description;

    private Map<String, Map<String, List<EmailTemplate>>> emailTemplates;

    private boolean enabled;

    private Integer expirationDate;

    private String identityTokenAudience;

    private String logo;
    private String logoBgColor;

    private Instant modifiedAt;
    private String modifiedBy;

    private Map<String, UserRolePermission> roleMappings;

    private ProductStatus status;

    private String title;

    private String urlBO;
    private String urlPublic;

    private Map<String, ContractTemplate> institutionAggregatorContractMappings;
    private Map<String, ContractTemplate> institutionContractMappings;
    private Map<String, ContractTemplate> userAggregatorContractMappings;
    private Map<String, ContractTemplate> userContractMappings;

    private boolean invoiceable;

}
