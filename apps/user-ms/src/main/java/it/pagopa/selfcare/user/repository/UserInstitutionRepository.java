package it.pagopa.selfcare.user.repository;

import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.user.entity.UserInstitution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
public class UserInstitutionRepository {

  @Inject TenantContext tenantContext;

  @ConfigProperty(name = "selfcare.tenant.strict-data-isolation", defaultValue = "false")
  boolean strictDataIsolation;

  public Uni<UserInstitution> findById(ObjectId id) {
    String tenant = tenantContext.requiredTenantId();
    return UserInstitution.<UserInstitution>findById(id)
        .map(entity -> entity != null && visible(entity, tenant) ? entity : null);
  }

  public ReactivePanacheQuery<UserInstitution> find(Document query, Document sort) {
    return UserInstitution.find(scope(query), sort);
  }

  public ReactivePanacheQuery<UserInstitution> find(Document query) {
    return UserInstitution.find(scope(query));
  }

  public Uni<Long> count(Document query) {
    return UserInstitution.count(scope(query));
  }

  public Uni<List<UserInstitution>> list(Document query) {
    return UserInstitution.mongoCollection().find(scope(query), UserInstitution.class)
        .collect().asList();
  }

  public Uni<Long> update(Document update, Document query) {
    return UserInstitution.update(update).where(scope(query));
  }

  public Uni<Long> updateMany(Document query, Document update, UpdateOptions options) {
    return UserInstitution.mongoCollection().updateMany(scope(query), update, options)
        .map(UpdateResult::getModifiedCount);
  }

  public Uni<UserInstitution> persistOrUpdate(UserInstitution entity) {
    String tenant = tenantContext.requiredTenantId();
    if (entity.getTenantId() != null && !tenant.equals(entity.getTenantId())) {
      log.warn("Rejecting user institution write with conflicting tenant");
      throw new IllegalStateException("User institution tenant does not match the current tenant");
    }
    if (entity.getId() == null) {
      entity.setId(new ObjectId());
    }
    entity.setTenantId(tenant);
    // A conflicting _id produces a duplicate-key error, never an unscoped replacement.
    return UserInstitution.<UserInstitution>mongoCollection()
        .replaceOne(scope(new Document("_id", entity.getId())), entity, new ReplaceOptions().upsert(true))
        .onFailure().invoke(failure -> log.error("Failed scoped user institution replacement for tenant {}", tenant, failure))
        .replaceWith(entity);
  }

  Document scope(Document query) {
    String tenant = tenantContext.requiredTenantId();
    Document tenantScope = strictDataIsolation ? new Document("tenantId", tenant)
        : new Document("$or", List.of(new Document("tenantId", tenant), new Document("tenantId", null)));
    return query == null || query.isEmpty() ? tenantScope
        : new Document("$and", List.of(tenantScope, query));
  }

  private boolean visible(UserInstitution entity, String tenant) {
    return tenant.equals(entity.getTenantId()) || (!strictDataIsolation && entity.getTenantId() == null);
  }
}
