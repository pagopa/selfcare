package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.DelegationEntity;
import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DelegationRepository extends MongoRepository<DelegationEntity, String>, MongoCustomConnector {

    Optional<DelegationEntity> findByFromAndToAndProductIdAndTypeAndStatus(String from, String to, String productId, DelegationType type, DelegationState status);

    Optional<List<DelegationEntity>> findByToAndStatus(String to, DelegationState status);
}
