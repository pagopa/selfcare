package it.pagopa.selfcare.onboarding.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.bson.Document;

@ApplicationScoped
public class TokenRepository {

    private static final String TENANT_ID = "tenantId";

    @Inject
    TenantContext tenantContext;

    public Uni<Optional<Token>> findByIdOptional(String tokenId) {
        String tenantId = tenantId();
        return Token.findByIdOptional(tokenId)
                .map(optional -> optional
                        .map(Token.class::cast)
                        .filter(token -> tenantId.equalsIgnoreCase(token.getTenantId())));
    }

    public ReactivePanacheQuery<Token> find(Document query) {
        return Token.find(scope(query));
    }

    public void validateTenant(Token token) {
        String currentTenant = tenantId();
        if (token.getTenantId() != null
                && !currentTenant.equalsIgnoreCase(token.getTenantId())) {
            throw new IllegalStateException("Token tenant does not match the current tenant");
        }
        token.setTenantId(currentTenant);
    }

    public Uni<Token> persist(Token token) {
        validateTenant(token);
        return Token.persist(token).replaceWith(token);
    }

    public Uni<Token> persistOrUpdate(Token token) {
        validateTenant(token);
        return Token.persistOrUpdate(List.of(token)).replaceWith(token);
    }

    private Document scope(Document query) {
        Document scopedQuery = new Document(TENANT_ID, tenantId());
        if (query != null && !query.isEmpty()) {
            return new Document("$and", List.of(scopedQuery, query));
        }
        return scopedQuery;
    }

    private String tenantId() {
        return tenantContext.requiredTenantId();
    }
}
