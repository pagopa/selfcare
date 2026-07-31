package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.DelegationEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface DelegationRepository extends MongoRepository<DelegationEntity, String>, MongoCustomConnector<DelegationEntity, String> {

    @Override
    default Optional<DelegationEntity> findById(String id) {
        return find(Query.query(Criteria.where(DelegationEntity.Fields.id.name()).is(id)), DelegationEntity.class).stream().findFirst();
    }

    @Override
    default List<DelegationEntity> findAll() {
        return find(new Query(), DelegationEntity.class);
    }

    @Override
    default List<DelegationEntity> findAllById(Iterable<String> ids) {
        List<String> idList = new ArrayList<>();
        ids.forEach(idList::add);
        return find(Query.query(Criteria.where(DelegationEntity.Fields.id.name()).in(idList)), DelegationEntity.class);
    }

    @Override
    default void deleteById(String id) {
        findAndRemove(Query.query(Criteria.where(DelegationEntity.Fields.id.name()).is(id)), DelegationEntity.class);
    }
}
