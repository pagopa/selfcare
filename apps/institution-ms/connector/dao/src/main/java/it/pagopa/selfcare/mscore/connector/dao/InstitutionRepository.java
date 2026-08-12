package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstitutionRepository extends MongoRepository<InstitutionEntity, String>, MongoCustomConnector<InstitutionEntity, String> {

    @Override
    default Optional<InstitutionEntity> findById(String id) {
        return find(Query.query(Criteria.where(InstitutionEntity.Fields.id.name()).is(id)), InstitutionEntity.class).stream().findFirst();
    }

    @Override
    default List<InstitutionEntity> findAll() {
        return find(new Query(), InstitutionEntity.class);
    }

    @Override
    default List<InstitutionEntity> findAllById(Iterable<String> ids) {
        List<String> idList = new ArrayList<>();
        ids.forEach(idList::add);
        return find(Query.query(Criteria.where(InstitutionEntity.Fields.id.name()).in(idList)), InstitutionEntity.class);
    }

    @Override
    default void deleteById(String id) {
        findAndRemove(Query.query(Criteria.where(InstitutionEntity.Fields.id.name()).is(id)), InstitutionEntity.class);
    }
}
