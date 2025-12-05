package it.pagopa.selfcare.product.repository;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.ContractTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@ApplicationScoped
public class ContractTemplateRepository implements ReactivePanacheMongoRepositoryBase<ContractTemplate, String> {

    public Uni<Long> countWithFilters(String productId, String name, String version) {
        return count(buildFilter(productId, name, version));
    }

    public Uni<List<ContractTemplate>> listWithFilters(String productId, String name, String version) {
        return list(buildFilter(productId, name, version), Sorts.descending("createdAt"));
    }

    private Bson buildFilter(String productId, String name, String version) {
        final List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("productId", productId));
        Optional.ofNullable(name).ifPresent(n -> filters.add(Filters.regex("name", Pattern.compile(Pattern.quote(n), Pattern.CASE_INSENSITIVE))));
        Optional.ofNullable(version).ifPresent(v -> filters.add(Filters.regex("version", Pattern.compile(Pattern.quote(v), Pattern.CASE_INSENSITIVE))));
        return Filters.and(filters);
    }

}
