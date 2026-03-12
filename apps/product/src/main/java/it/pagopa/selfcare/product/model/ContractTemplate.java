package it.pagopa.selfcare.product.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;

@Data
@Builder
@MongoEntity(collection = "contractTemplates")
@AllArgsConstructor
@NoArgsConstructor
public class ContractTemplate {

  @BsonId @Builder.Default private String id = UUID.randomUUID().toString();

  private String productId;

  private String name;

  private String version;

  private String description;

  private ContractTemplateFileType fileType;

  @Builder.Default private Instant createdAt = Instant.now();

  private String createdBy;
}
