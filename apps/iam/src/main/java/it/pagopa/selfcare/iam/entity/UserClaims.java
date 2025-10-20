package it.pagopa.selfcare.iam.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.model.ProductRoles;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.*;
import java.util.stream.Collectors;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(asEnum = true)
@MongoEntity(collection = "userClaims")
public class UserClaims extends ReactivePanacheMongoEntityBase {

  @BsonId
  private String uid;
  private String email;
  private String name;
  private String familyName;
  @Builder.Default
  private Map<String, ProductRoles> productRoles = new HashMap<>();


  public static Uni<UserClaims> findByUid(String uid) {
    return find("_id", uid).firstResult().map(entity -> (UserClaims) entity);
  }

  public static Uni<UserClaims> findByEmail(String email) {
    return find("email", email).firstResult().map(entity -> (UserClaims) entity);
  }

  public static Uni<UserClaims> findByUidAndProductId(String uid, String productId) {
    return Optional.ofNullable(productId).isPresent() ?
      find("_id = ?1 and productRoles.productId = ?2", uid, productId)
      .firstResult()
      .map(entity -> (UserClaims) entity) :
      findByUid(uid);
  }
}
