package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InstitutionRepository extends MongoRepository<InstitutionEntity, String>, MongoCustomConnector {

    @Query(value = "{ '_id' : ?0, 'onboarding' : { $elemMatch: { 'productId' : ?1 } } }",
            fields = "{ 'onboarding': { $elemMatch: { 'productId' : ?1 } } }")
    InstitutionEntity findByInstitutionIdAndOnboardingProductId(String institutionId, String productId);


}
