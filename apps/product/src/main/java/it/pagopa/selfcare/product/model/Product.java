package it.pagopa.selfcare.product.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import it.pagopa.selfcare.product.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.List;
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

    private String productId;
    private String alias;

    private String title;
    private String description;

    private ProductStatus status;

    @Builder.Default
    private int version = 1;

    private List<String> consumers;

    private VisualConfiguration visualConfiguration;

    private Features features;

    private List<RoleMapping> roleMappings;

    private List<ContractTemplateConfig> contracts;

    private List<OriginEntry> institutionOrigins;

    private List<EmailTemplateConfig> emailTemplates;

    private List<BackOfficeEnvironmentConfiguration> backOfficeEnvironmentConfigurations;

    private ProductMetadata metadata;
}
