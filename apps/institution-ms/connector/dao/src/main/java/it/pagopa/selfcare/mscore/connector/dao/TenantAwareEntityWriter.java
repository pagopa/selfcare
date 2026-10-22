package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.BiConsumer;

@Component
public class TenantAwareEntityWriter {

    private final MongoOperations mongoOperations;
    private final TenantOwnershipPolicy ownershipPolicy;
    private final TenantContext tenantContext;

    public TenantAwareEntityWriter(
            MongoOperations mongoOperations,
            TenantOwnershipPolicy ownershipPolicy,
            TenantContext tenantContext) {
        this.mongoOperations = mongoOperations;
        this.ownershipPolicy = ownershipPolicy;
        this.tenantContext = tenantContext;
    }

    public <T> T replace(
            String id,
            T replacement,
            Class<T> entityType,
            BiConsumer<T, String> tenantIdSetter) {
        Objects.requireNonNull(id, "Entity id is required");
        Objects.requireNonNull(replacement, "Replacement entity is required");
        Objects.requireNonNull(entityType, "Entity type is required");
        Objects.requireNonNull(tenantIdSetter, "Tenant id setter is required");

        String tenantId = tenantContext.requiredTenantId();
        tenantIdSetter.accept(replacement, tenantId);
        Query query = ownershipPolicy.scope(
                Query.query(Criteria.where("_id").is(id)));
        try {
            return mongoOperations.findAndReplace(
                    query,
                    replacement,
                    FindAndReplaceOptions.options().upsert().returnNew());
        } catch (DuplicateKeyException exception) {
            Query otherOwnerQuery = Query.query(
                    Criteria.where("_id").is(id)
                            .and("tenantId").ne(tenantId));
            if (mongoOperations.exists(otherOwnerQuery, entityType)) {
                throw new TenantOwnershipConflictException();
            }
            throw exception;
        }
    }
}
