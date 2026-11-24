package it.pagopa.selfcare.user.repository;

import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.UnresolvedTenantException;
import it.pagopa.selfcare.user.entity.UserInstitution;
import jakarta.inject.Inject;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class UserInstitutionRepositoryTest {
  @Inject TenantContext context;
  @Inject UserInstitutionRepository repository;

  @BeforeEach void setup() {
    repository = io.quarkus.arc.ClientProxy.unwrap(repository);
    context.setTenantId("AR");
    repository.strictDataIsolation = false;
    PanacheMock.mock(UserInstitution.class);
  }

  @Test void scopesLegacyQueriesWithoutMutatingBusinessPredicate() {
    Document business = new Document("$or", List.of(new Document("userId", "a"), new Document("userId", "b")));
    Document snapshot = Document.parse(business.toJson());
    assertEquals(new Document("$and", List.of(
        new Document("$or", List.of(new Document("tenantId", "AR"), new Document("tenantId", null))), business)),
        repository.scope(business));
    assertEquals(snapshot, business);
  }

  @Test void strictScopeExcludesLegacyDocuments() {
    repository.strictDataIsolation = true;
    assertEquals(new Document("tenantId", "AR"), repository.scope(new Document()));
  }

  @Test void scopeFollowsCurrentTenantAndFailsClosedWithoutContext() {
    context.setTenantId("PNPG");
    assertTrue(repository.scope(new Document()).toJson().contains("PNPG"));
    context.clear();
    assertThrows(UnresolvedTenantException.class, () -> repository.count(new Document()));
    assertThrows(UnresolvedTenantException.class, () -> repository.persistOrUpdate(new UserInstitution()));
  }

  @Test void findByIdRejectsAnotherTenantAndStrictLegacy() {
    UserInstitution entity = new UserInstitution();
    entity.setTenantId("PNPG");
    ObjectId id = new ObjectId();
    when(UserInstitution.findById(id)).thenReturn(Uni.createFrom().item(entity));
    assertNull(repository.findById(id).await().indefinitely());
    entity.setTenantId(null);
    assertSame(entity, repository.findById(id).await().indefinitely());
    repository.strictDataIsolation = true;
    assertNull(repository.findById(id).await().indefinitely());
    entity.setTenantId("AR");
    assertSame(entity, repository.findById(id).await().indefinitely());
  }

  @Test void rejectsConflictingDocumentBeforeWriting() {
    UserInstitution entity = new UserInstitution();
    entity.setTenantId("PNPG");
    assertThrows(IllegalStateException.class, () -> repository.persistOrUpdate(entity));
    PanacheMock.verify(UserInstitution.class, never()).mongoCollection();
  }

  @Test void atomicReplacementStampsTenantAndScopesTheUpsert() {
    ReactiveMongoCollection<UserInstitution> collection = mock(ReactiveMongoCollection.class);
    when(UserInstitution.<UserInstitution>mongoCollection()).thenReturn(collection);
    when(collection.replaceOne(any(Document.class), any(UserInstitution.class), any(ReplaceOptions.class)))
        .thenReturn(Uni.createFrom().item(UpdateResult.acknowledged(1, 1L, null)));
    UserInstitution entity = new UserInstitution();
    assertSame(entity, repository.persistOrUpdate(entity).await().indefinitely());
    assertNotNull(entity.getId());
    assertEquals("AR", entity.getTenantId());
    ArgumentCaptor<Document> filter = ArgumentCaptor.forClass(Document.class);
    ArgumentCaptor<ReplaceOptions> options = ArgumentCaptor.forClass(ReplaceOptions.class);
    verify(collection).replaceOne(filter.capture(), eq(entity), options.capture());
    assertEquals(repository.scope(new Document("_id", entity.getId())), filter.getValue());
    assertTrue(options.getValue().isUpsert());
  }

  @Test void scopesCountsAndBothUpdatePaths() {
    Document query = new Document("institutionId", "institution");
    Document update = new Document("$set", new Document("institutionDescription", "updated"));
    when(UserInstitution.count(any(Document.class))).thenReturn(Uni.createFrom().item(2L));
    assertEquals(2L, repository.count(query).await().indefinitely());
    PanacheMock.verify(UserInstitution.class).count(repository.scope(query));
    ReactivePanacheUpdate panacheUpdate = mock(ReactivePanacheUpdate.class);
    when(UserInstitution.update(update)).thenReturn(panacheUpdate);
    when(panacheUpdate.where(any(Document.class))).thenReturn(Uni.createFrom().item(1L));
    assertEquals(1L, repository.update(update, query).await().indefinitely());
    verify(panacheUpdate).where(repository.scope(query));
    ReactiveMongoCollection<UserInstitution> collection = mock(ReactiveMongoCollection.class);
    when(UserInstitution.<UserInstitution>mongoCollection()).thenReturn(collection);
    UpdateOptions options = new UpdateOptions();
    when(collection.updateMany(any(Document.class), eq(update), eq(options)))
        .thenReturn(Uni.createFrom().item(UpdateResult.acknowledged(1, 1L, null)));
    assertEquals(1L, repository.updateMany(query, update, options).await().indefinitely());
    verify(collection).updateMany(repository.scope(query), update, options);
  }
}
