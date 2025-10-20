package it.pagopa.selfcare.iam.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.model.ProductRoles;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.List;
import java.util.Optional;

import org.bson.codecs.pojo.annotations.BsonId;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(asEnum = true)
@MongoEntity(collection = "roles")
public class Roles extends ReactivePanacheMongoEntityBase {

  @BsonId
  private String name;
  private List<String> permissions;
}
