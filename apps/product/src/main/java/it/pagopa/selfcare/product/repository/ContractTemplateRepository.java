package it.pagopa.selfcare.product.repository;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.ContractTemplate;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.conversions.Bson;

@ApplicationScoped
public class ContractTemplateRepository
    implements ReactivePanacheMongoRepositoryBase<ContractTemplate, String> {

  @Inject TenantContext tenantContext;

  public Uni<Long> countWithFilters(
      String tenantId, String productId, String name, String version) {
    return count(buildFilter(tenantId, productId, name, version));
  }

  public Uni<List<ContractTemplate>> listWithFilters(
      String tenantId, String productId, String name, String version) {
    return list(buildFilter(tenantId, productId, name, version), Sorts.descending("createdAt"));
  }

  private Bson buildFilter(String tenantId, String productId, String name, String version) {
    final List<Bson> filters = new ArrayList<>();
    filters.add(Filters.eq("tenantId", tenantContext.requiredTenantId()));
    Optional.ofNullable(tenantId)
        .ifPresent(
            t ->
                filters.add(
                    Filters.regex(
                        "tenantId", Pattern.compile(Pattern.quote(t), Pattern.CASE_INSENSITIVE))));
    Optional.ofNullable(productId)
        .ifPresent(
            p ->
                filters.add(
                    Filters.regex(
                        "productId", Pattern.compile(Pattern.quote(p), Pattern.CASE_INSENSITIVE))));
    Optional.ofNullable(name)
        .ifPresent(
            n ->
                filters.add(
                    Filters.regex(
                        "name", Pattern.compile(Pattern.quote(n), Pattern.CASE_INSENSITIVE))));
    Optional.ofNullable(version)
        .ifPresent(
            v ->
                filters.add(
                    Filters.regex(
                        "version", Pattern.compile(Pattern.quote(v), Pattern.CASE_INSENSITIVE))));
    return filters.isEmpty() ? new Document() : Filters.and(filters);
  }
}
