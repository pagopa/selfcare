package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TenantOwnershipPolicy {

    private final TenantContext tenantContext;
    private final boolean strictDataIsolation;

    public TenantOwnershipPolicy(
            TenantContext tenantContext,
            @Value("${tenant.strict-data-isolation:true}") boolean strictDataIsolation) {
        this.tenantContext = tenantContext;
        this.strictDataIsolation = strictDataIsolation;
    }

    public Criteria criteria() {
        String tenantId = tenantContext.requiredTenantId();
        Criteria ownedDocument = Criteria.where(InstitutionEntity.Fields.tenantId.name()).is(tenantId);
        if (strictDataIsolation) {
            return ownedDocument;
        }
        return Criteria.where(InstitutionEntity.Fields.tenantId.name()).in(tenantId, null);
    }

    public Criteria and(Criteria domainCriteria) {
        Objects.requireNonNull(domainCriteria, "Domain criteria are required");
        return new Criteria().andOperator(domainCriteria, criteria());
    }

    public Query scope(Query query) {
        Objects.requireNonNull(query, "Query is required");
        return Query.of(query).addCriteria(criteria());
    }
}
