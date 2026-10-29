package it.pagopa.selfcare.contractscrm.model.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

/** Contract of a PSP institution, as stored in the contract, membership and recipient fields. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionContract {

  private String institutionId;

  // A property named "id" would be stored as "_id" by the POJO codec.
  @BsonProperty("id")
  private String contractId;

  private Instant date;
  private ContractStatus status;
}
