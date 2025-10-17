package it.pagopa.selfcare.iam.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.smallrye.mutiny.Uni;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.List;

import org.bson.codecs.pojo.annotations.BsonId;

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
  private List<String> roles;

  public static Uni<UserClaims> findByUid(String uid) {
    return find("uid", uid).firstResult().map(entity -> (UserClaims) entity);
  }

  public static Uni<UserClaims> findByEmail(String email) {
    return find("email", email).firstResult().map(entity -> (UserClaims) entity);
  }
}
