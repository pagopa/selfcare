package it.pagopa.selfcare.contractscrm.model.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;

@Data
@NoArgsConstructor
@MongoEntity(collection = "contractDelegations")
public class ContractDelegation {

  public static final String PRODUCT_ID_FIELD = "productId";
  public static final String CONTRACT_ID_FIELD = "contract.id";

  @BsonId private String id;
  private String productId;
  private InstitutionContract contract;
  private InstitutionContract membership;
  private InstitutionContract recipient;
  private Instant createdAt;
  private Instant updatedAt;
}
