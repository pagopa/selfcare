package it.pagopa.selfcare.contractscrm.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.contractscrm.model.entity.ContractDelegation;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;

@ApplicationScoped
public class ContractDelegationRepository
    implements ReactivePanacheMongoRepositoryBase<ContractDelegation, String> {

  public Uni<ContractDelegation> findByProductIdAndContractId(String productId, String contractId) {
    return find(
            new Document(ContractDelegation.PRODUCT_ID_FIELD, productId)
                .append(ContractDelegation.CONTRACT_ID_FIELD, contractId))
        .firstResult();
  }
}
