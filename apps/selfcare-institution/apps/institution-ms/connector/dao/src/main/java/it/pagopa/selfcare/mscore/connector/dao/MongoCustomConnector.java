package it.pagopa.selfcare.mscore.connector.dao;

import com.mongodb.client.result.UpdateResult;
import it.pagopa.selfcare.mscore.model.aggregation.UserInstitutionFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.util.List;

public interface MongoCustomConnector {

    <O> boolean exists(Query query, Class<O> outputType);

    <O> Long count(Query query, Class<O> outputType);

    <O> List<O> find(Query query, Class<O> outputType);

    <O> Page<O> find(Query query, Pageable pageable, Class<O> outputType);

    <O> O findAndModify(Query query, UpdateDefinition updateDefinition, FindAndModifyOptions findAndModifyOptions, Class<O> outputType);

    <O> O findAndRemove(Query query, Class<O> outputType);

    <O> List<O> findUserInstitutionAggregation(UserInstitutionFilter filter, Class<O> outputType);

    <O> UpdateResult updateMulti(Query query, UpdateDefinition updateDefinition, Class<O> outputType);

    <O> UpdateResult upsert(Query query, UpdateDefinition updateDefinition, Class<O> outputType);

}
