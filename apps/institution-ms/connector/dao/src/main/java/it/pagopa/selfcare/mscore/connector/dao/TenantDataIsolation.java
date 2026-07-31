package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.TenantOwnedEntity;
import it.pagopa.selfcare.mscore.exception.InvalidRequestException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TenantDataIsolation {

    static final String TENANT_ID_FIELD = "tenantId";

    private final CurrentTenantProvider currentTenantProvider;
    private final MongoOperations mongoOperations;

    /**
     * Whether untagged documents are still treated as belonging to the current tenant.
     *
     * <p>Configuration rather than a code constant because the backfill runs at a different time in
     * each environment, so the strict build must be promotable before every environment has been
     * migrated (Step_1/EPIC.md sub-tasks 2 and 10). Defaults to the lenient behaviour; both the flag
     * and the {@code tenantId == null} branch must be deleted once every environment runs strict.
     */
    private final boolean strictTenantIsolation;

    public TenantDataIsolation(CurrentTenantProvider currentTenantProvider, MongoOperations mongoOperations,
                               @Value("${selfcare.tenant.strict-data-isolation:false}") boolean strictTenantIsolation) {
        this.currentTenantProvider = currentTenantProvider;
        this.mongoOperations = mongoOperations;
        this.strictTenantIsolation = strictTenantIsolation;
    }

    public Query tenantScoped(Query query, Class<?> outputType) {
        Query scopedQuery = Query.of(query);
        if (!TenantOwnedEntity.class.isAssignableFrom(outputType)) {
            return scopedQuery;
        }

        Optional<String> tenantId = currentTenantProvider.currentTenantId();
        if (tenantId.isEmpty()) {
            /*
             * Migration phase: calls outside a request (schedulers, consumers, startup tasks) keep
             * the pre-multitenant unscoped behaviour. This is a concession for compatibility, not a
             * security boundary.
             */
            return scopedQuery;
        }

        scopedQuery.addCriteria(tenantCriteria(tenantId.get()));
        return scopedQuery;
    }

    public Criteria tenantCriteria(String tenantId) {
        /*
         * Migration phase: legacy documents have no discriminator. The tenantId == null branch is
         * dropped by selfcare.tenant.strict-data-isolation once the backfill has tagged every
         * document.
         */
        if (strictTenantIsolation) {
            return Criteria.where(TENANT_ID_FIELD).is(tenantId);
        }
        return new Criteria().orOperator(
                Criteria.where(TENANT_ID_FIELD).is(tenantId),
                Criteria.where(TENANT_ID_FIELD).is(null)
        );
    }

    public Optional<Criteria> currentTenantCriteria() {
        return currentTenantProvider.currentTenantId().map(this::tenantCriteria);
    }

    public <O> O stampTenantForSave(O entity) {
        if (!(entity instanceof TenantOwnedEntity tenantOwned)) {
            return entity;
        }

        Optional<String> currentTenantId = currentTenantProvider.currentTenantId();
        if (currentTenantId.isEmpty()) {
            return entity;
        }

        if (tenantOwned.getTenantId() != null) {
            return entity;
        }

        Optional<TenantOwnedEntity> existing = findExistingTenantOwnedEntity(entity);
        Optional<String> existingTenant = existing.map(TenantOwnedEntity::getTenantId);
        if (existingTenant.filter(tenant -> !tenant.equals(currentTenantId.get())).isPresent()) {
            throw new InvalidRequestException(
                    String.format("Cannot write %s owned by tenant %s from tenant %s",
                            entity.getClass().getSimpleName(), existingTenant.get(), currentTenantId.get()),
                    "0000");
        }

        tenantOwned.setTenantId(currentTenantId.get());
        return entity;
    }

    public void stampTenantForUpdate(UpdateDefinition updateDefinition, Class<?> outputType) {
        if (!TenantOwnedEntity.class.isAssignableFrom(outputType) || !(updateDefinition instanceof Update update)) {
            return;
        }
        currentTenantProvider.currentTenantId().ifPresent(tenantId -> update.set(TENANT_ID_FIELD, tenantId));
    }

    private Optional<TenantOwnedEntity> findExistingTenantOwnedEntity(Object entity) {
        Object id = readId(entity);
        if (id == null) {
            return Optional.empty();
        }
        Object existing = mongoOperations.findById(id, entity.getClass());
        return existing instanceof TenantOwnedEntity tenantOwned ? Optional.of(tenantOwned) : Optional.empty();
    }

    private Object readId(Object entity) {
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        return wrapper.isReadableProperty("id") ? wrapper.getPropertyValue("id") : null;
    }
}
