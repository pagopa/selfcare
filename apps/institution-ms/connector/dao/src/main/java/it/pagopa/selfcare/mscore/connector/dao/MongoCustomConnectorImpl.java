package it.pagopa.selfcare.mscore.connector.dao;

import com.mongodb.client.result.UpdateResult;
import it.pagopa.selfcare.mscore.model.aggregation.UserInstitutionFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GraphLookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MongoCustomConnectorImpl implements MongoCustomConnector {
    private static final String INSTITUTION_ID = "institutionId";
    private static final String INSTITUTIONS = "institutions";
    private static final String ENTITY_ID = "_id";
    private static final String BINDINGS = "$bindings";
    private final MongoOperations mongoOperations;

    public MongoCustomConnectorImpl(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }


    @Override
    public <O> boolean exists(Query query, Class<O> outputType) {
        return mongoOperations.exists(query, outputType);
    }

    @Override
    public <O> Long count(Query query, Class<O> outputType) {
        return mongoOperations.count(query, outputType);
    }

    @Override
    public <O> List<O> find(Query query, Class<O> outputType) {
        return mongoOperations.find(query, outputType);
    }

    @Override
    public <O> Page<O> find(Query query, Pageable pageable, Class<O> outputType) {
        long count = mongoOperations.count(query, outputType);
        List<O> list = new ArrayList<>();
        if (count > 0) {
            list = mongoOperations.find(query.with(pageable), outputType);
        }
        return new PageImpl<>(list, pageable, count);
    }

    @Override
    public <O> O findAndModify(Query query, UpdateDefinition updateDefinition, FindAndModifyOptions findAndModifyOptions, Class<O> outputType) {
        return mongoOperations.findAndModify(query, updateDefinition, findAndModifyOptions, outputType);
    }

    @Override
    public <O> O findAndRemove(Query query, Class<O> outputType) {
        return mongoOperations.findAndRemove(query, outputType);
    }

    @Override
    public <O> List<O> findUserInstitutionAggregation(UserInstitutionFilter filter, Class<O> outputType) {
        MatchOperation matchUserId = Aggregation.match(Criteria.where(ENTITY_ID).is(filter.getUserId()));
        //Output a new document for each userBindings
        UnwindOperation unwindBindings = Aggregation.unwind(BINDINGS);
        //retrieve institution for each binding
        GraphLookupOperation.GraphLookupOperationBuilder graphLookupInstitution = Aggregation.graphLookup(filter.getToCollection())
                .startWith(BINDINGS + "." + INSTITUTION_ID)
                .connectFrom(INSTITUTION_ID)
                .connectTo(ENTITY_ID)
                .maxDepth(2);
        //Output a new document for each product in bindings
        UnwindOperation unwindProducts = Aggregation.unwind(BINDINGS + ".products");
        //remove document with product status not in filter
        MatchOperation matchProductStatus = Aggregation.match(Criteria.where("bindings.products.status").in(filter.getStates()));
        //remove document with no institution
        MatchOperation matchInstitutionExist = Aggregation.match(Criteria.where(INSTITUTIONS).size(1));

        Aggregation aggregation;
        if (StringUtils.hasText(filter.getInstitutionId())) {
            MatchOperation matchInstitutionId = checkIfInstitutionIdIsPresent(filter);
            aggregation = Aggregation.newAggregation(matchUserId, unwindBindings,  graphLookupInstitution.as(INSTITUTIONS), matchInstitutionId, unwindProducts, matchProductStatus, matchInstitutionExist);
        } else if (StringUtils.hasText(filter.getExternalId())) {
            checkIfExternalIdIsPresent(filter, graphLookupInstitution);
            aggregation = Aggregation.newAggregation(matchUserId, unwindBindings, graphLookupInstitution.as(INSTITUTIONS), unwindProducts, matchProductStatus, matchInstitutionExist);
        } else {
            aggregation = Aggregation.newAggregation(matchUserId, unwindBindings, graphLookupInstitution.as(INSTITUTIONS), unwindProducts, matchProductStatus, matchInstitutionExist);
        }
        return mongoOperations.aggregate(aggregation, filter.getFromCollection(), outputType).getMappedResults();
    }

    private MatchOperation checkIfInstitutionIdIsPresent(UserInstitutionFilter filter) {
        return Aggregation.match(Criteria.where("bindings." + INSTITUTION_ID).is(filter.getInstitutionId()));
    }

    private void checkIfExternalIdIsPresent(UserInstitutionFilter filter, GraphLookupOperation.GraphLookupOperationBuilder graphLookupOperation) {
        graphLookupOperation.restrict(Criteria.where("externalId").is(filter.getExternalId()));
    }

    @Override
    public <O> UpdateResult updateMulti(Query query, UpdateDefinition updateDefinition, Class<O> outputType){
        return mongoOperations.updateMulti(query, updateDefinition, outputType);
    }

    @Override
    public <O> UpdateResult upsert(Query query, UpdateDefinition updateDefinition, Class<O> outputType) {
        return mongoOperations.upsert(query, updateDefinition, outputType);
    }
}
