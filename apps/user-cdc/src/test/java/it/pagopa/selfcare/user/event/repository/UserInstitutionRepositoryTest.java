package it.pagopa.selfcare.user.event.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mongodb.client.model.Filters;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class UserInstitutionRepositoryTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String AGGREGATE_ID = "aggregate";
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-11-25T10:15:00Z");

    @Inject UserInstitutionRepository repository;
    @Inject ReactiveMongoClient mongo;

    @Test
    void insertsATaggedAggregateEvenWhenTheParentIsLegacy() {
        var parent = parent();
        var created = repository.propagateUserToAggregate(parent, parentProduct(),
                AGGREGATE_ID, "Aggregate name", PartyRole.ADMIN_EA, "admin")
                .await().atMost(TIMEOUT);

        assertEquals("AR", created.getTenantId());
        assertEquals(parent.getUserId(), created.getUserId());
        assertEquals(AGGREGATE_ID, created.getInstitutionId());
        assertEquals("Aggregate name", created.getInstitutionDescription());
        assertEquals(parent.getInstitutionDescription(), created.getInstitutionRootName());
        assertEquals(parent.getUserMailUuid(), created.getUserMailUuid());
        assertEquals(1, created.getProducts().size());
        assertPropagatedProduct(created.getProducts().get(0));
        assertEquals("AR", read(created.getId()).getString("tenantId"));
    }

    @Test
    void updatesAnExistingRoleWithoutRemovingItsBackfilledTagOrOtherFields() {
        var parent = parent();
        parent.setTenantId("AR");
        var id = seed(parent, "AR", List.of(
                new Document("productId", "unrelated").append("role", "OPERATOR"),
                new Document("productId", "old").append("roleId", "shared-role").append("role", "OPERATOR")));

        var updated = repository.propagateUserToAggregate(parent, parentProduct(),
                AGGREGATE_ID, "Aggregate name", PartyRole.ADMIN_EA, "admin")
                .await().atMost(TIMEOUT);

        assertEquals(id, updated.getId());
        assertEquals(2, updated.getProducts().size());
        assertEquals("unrelated", updated.getProducts().get(0).getProductId());
        assertPropagatedProduct(updated.getProducts().get(1));
        var stored = read(id);
        assertEquals("AR", stored.getString("tenantId"));
        assertEquals("must survive CDC", stored.getString("unmappedField"));
        assertEquals("Aggregate name", stored.getString("institutionDescription"));
        assertEquals(parent.getInstitutionDescription(), stored.getString("institutionRootName"));
        assertEquals(parent.getUserMailUuid(), stored.getString("userMailUuid"));
        assertEquals(Date.from(UPDATED_AT.toInstant()), stored.getDate("userMailUpdatedAt"));
        assertEquals("shared-role", stored.getList("products", Document.class).get(1).getString("roleId"));
        assertEquals("ADMIN_EA", stored.getList("products", Document.class).get(1).getString("role"));
    }

    @Test
    void updatesALegacyRecordAndAppendsTheNewRoleWithoutOverwritingItsTag() {
        var parent = parent();
        parent.setUserMailUuid(null);
        parent.setUserMailUpdatedAt(null);
        var id = seed(parent, null, List.of(new Document("productId", "unrelated").append("role", "OPERATOR")));

        var updated = repository.propagateUserToAggregate(parent, parentProduct(),
                AGGREGATE_ID, "Aggregate name", PartyRole.ADMIN_EA, "admin")
                .await().atMost(TIMEOUT);

        assertEquals(id, updated.getId());
        assertEquals(2, updated.getProducts().size());
        assertEquals("unrelated", updated.getProducts().get(0).getProductId());
        assertPropagatedProduct(updated.getProducts().get(1));
        var stored = read(id);
        assertFalse(stored.containsKey("tenantId"));
        assertFalse(stored.containsKey("userMailUuid"));
        assertFalse(stored.containsKey("userMailUpdatedAt"));
    }

    @Test
    void refusesToChangeAnAggregateTaggedForAnotherTenant() {
        var parent = parent();
        var id = seed(parent, "PNPG", List.of());
        var before = read(id);
        var failure = assertThrows(IllegalStateException.class, () ->
                repository.propagateUserToAggregate(parent, parentProduct(), AGGREGATE_ID,
                        "Aggregate name", PartyRole.ADMIN_EA, "admin").await().atMost(TIMEOUT));
        assertEquals("Target user institution no longer exists or belongs to another tenant", failure.getMessage());
        assertEquals(before, read(id));
        assertEquals(1L, documents().countDocuments(Filters.eq("userId", parent.getUserId())).await().atMost(TIMEOUT));
    }

    @Test
    void refusesToPropagateAParentTaggedForAnotherTenant() {
        var parent = parent();
        parent.setTenantId("PNPG");
        var failure = assertThrows(IllegalStateException.class, () ->
                repository.propagateUserToAggregate(parent, parentProduct(), AGGREGATE_ID,
                        "Aggregate name", PartyRole.ADMIN_EA, "admin").await().atMost(TIMEOUT));
        assertEquals("Source user institution belongs to another tenant", failure.getMessage());
        assertEquals(0L, documents().countDocuments(Filters.eq("userId", parent.getUserId())).await().atMost(TIMEOUT));
    }

    private UserInstitution parent() {
        var parent = new UserInstitution();
        parent.setUserId(UUID.randomUUID().toString());
        parent.setInstitutionDescription("Parent name");
        parent.setUserMailUuid("new-mail");
        parent.setUserMailUpdatedAt(UPDATED_AT);
        return parent;
    }

    private OnboardedProduct parentProduct() {
        var product = new OnboardedProduct();
        product.setRoleId("shared-role");
        product.setProductId("prod-io");
        product.setRole(PartyRole.DELEGATE);
        product.setStatus(OnboardedProductState.ACTIVE);
        product.setToAddOnAggregates(true);
        product.setUpdatedAt(UPDATED_AT);
        return product;
    }

    private void assertPropagatedProduct(OnboardedProduct product) {
        assertEquals("shared-role", product.getRoleId());
        assertEquals("prod-io", product.getProductId());
        assertEquals(PartyRole.ADMIN_EA, product.getRole());
        assertEquals("admin", product.getProductRole());
        assertEquals(OnboardedProductState.ACTIVE, product.getStatus());
        assertNull(product.getToAddOnAggregates());
    }

    private ObjectId seed(UserInstitution parent, String tenant, List<Document> products) {
        var id = new ObjectId();
        var document = new Document("_id", id)
                .append("userId", parent.getUserId())
                .append("institutionId", AGGREGATE_ID)
                .append("institutionDescription", "Old aggregate")
                .append("userMailUuid", "old-mail")
                .append("userMailUpdatedAt", new Date(0))
                .append("unmappedField", "must survive CDC")
                .append("products", products);
        if (tenant != null) {
            document.append("tenantId", tenant);
        }
        documents().insertOne(document).await().atMost(TIMEOUT);
        return id;
    }

    private Document read(ObjectId id) {
        var document = documents().find(Filters.eq("_id", id)).collect().first().await().atMost(TIMEOUT);
        assertNotNull(document);
        return document;
    }

    private ReactiveMongoCollection<Document> documents() {
        return mongo.getDatabase("selcUser").getCollection("userInstitutions");
    }
}
