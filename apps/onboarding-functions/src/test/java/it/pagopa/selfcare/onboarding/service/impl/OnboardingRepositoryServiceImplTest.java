package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.common.PanacheUpdate;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.DatabaseThrottledException;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@QuarkusTest
class OnboardingRepositoryServiceImplTest {

  @Test
  void findById_shouldDelegateToRepository() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    Onboarding expected = new Onboarding();
    when(onboardingRepository.findByIdOptional("onb-id")).thenReturn(Optional.of(expected));

    // when
    Optional<Onboarding> actual = service.findById("onb-id");

    // then
    assertEquals(Optional.of(expected), actual);
    verify(onboardingRepository).findByIdOptional("onb-id");
  }

  @Test
  void update_shouldDelegateToRepository() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    Onboarding onboarding = new Onboarding();

    // when
    service.update(onboarding);

    // then
    verify(onboardingRepository).update(onboarding);
  }

  @Test
  void updateStatus_shouldExecuteUpdateWhere() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    PanacheUpdate panacheUpdate = Mockito.mock(PanacheUpdate.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    LocalDateTime updatedAt = LocalDateTime.now();
    when(onboardingRepository.update("status = ?1 and updatedAt = ?2", "COMPLETED", updatedAt))
      .thenReturn(panacheUpdate);

    // when
    service.updateStatus("onb-id", "COMPLETED", updatedAt);

    // then
    verify(panacheUpdate).where("_id", "onb-id");
  }

  @Test
  void updateStatusAndInstanceId_shouldExecuteUpdateWhere() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    PanacheUpdate panacheUpdate = Mockito.mock(PanacheUpdate.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    LocalDateTime updatedAt = LocalDateTime.now();
    when(
      onboardingRepository.update(
        "status = ?1 and workflowInstanceId = ?2 and updatedAt = ?3",
        "COMPLETED",
        "instance-id",
        updatedAt))
      .thenReturn(panacheUpdate);

    // when
    service.updateStatusAndInstanceId("onb-id", "COMPLETED", "instance-id", updatedAt);

    // then
    verify(panacheUpdate).where("_id", "onb-id");
  }

  @Test
  void countByQuery_shouldDelegateToRepositoryFindCount() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    PanacheQuery<Onboarding> panacheQuery = Mockito.mock(PanacheQuery.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    Document query = new Document("status", "COMPLETED");
    when(onboardingRepository.find(query)).thenReturn(panacheQuery);
    when(panacheQuery.count()).thenReturn(10L);

    // when
    long actual = service.countByQuery(query);

    // then
    assertEquals(10L, actual);
    verify(onboardingRepository).find(query);
    verify(panacheQuery).count();
  }

  @Test
  void countByProduct_shouldGroupCountsByProductId() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    MongoCollection<Onboarding> collection = Mockito.mock(MongoCollection.class);
    AggregateIterable<Document> aggregate = Mockito.mock(AggregateIterable.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    Document query = new Document("status", "COMPLETED");
    when(onboardingRepository.mongoCollection()).thenReturn(collection);
    when(collection.aggregate(anyList(), eq(Document.class))).thenReturn(aggregate);
    List<Document> groups =
      List.of(
        new Document("_id", "prod-a").append("count", 3),
        new Document("_id", "prod-b").append("count", 5L),
        new Document("_id", null).append("count", 1));
    when(aggregate.into(any())).thenReturn(groups);

    // when
    Map<String, Long> actual = service.countByProduct(query);

    // then
    assertEquals(Map.of("prod-a", 3L, "prod-b", 5L), actual);
    ArgumentCaptor<List<Document>> pipeline = ArgumentCaptor.forClass(List.class);
    verify(collection).aggregate(pipeline.capture(), eq(Document.class));
    assertEquals(new Document("$match", query), pipeline.getValue().get(0));
  }

  @Test
  void countByProduct_shouldMapCosmosTooManyRequestsToThrottledException() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    MongoCollection<Onboarding> collection = Mockito.mock(MongoCollection.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    when(onboardingRepository.mongoCollection()).thenReturn(collection);
    when(collection.aggregate(anyList(), eq(Document.class)))
      .thenThrow(new MongoException(16500, "Error=16500, RetryAfterMs=2500, Details='Request rate is large'"));

    // when
    DatabaseThrottledException exception =
      assertThrows(DatabaseThrottledException.class, () -> service.countByProduct(new Document()));

    // then
    assertEquals(2500L, exception.getRetryAfterMs());
  }

  @Test
  void countByProduct_shouldRethrowOtherMongoExceptions() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    MongoCollection<Onboarding> collection = Mockito.mock(MongoCollection.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    when(onboardingRepository.mongoCollection()).thenReturn(collection);
    when(collection.aggregate(anyList(), eq(Document.class)))
      .thenThrow(new MongoException(1, "boom"));

    // when / then
    assertThrows(MongoException.class, () -> service.countByProduct(new Document()));
  }

  @Test
  void findByQueryPaged_shouldDelegateToRepositoryPagination() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    PanacheQuery<Onboarding> panacheQuery = Mockito.mock(PanacheQuery.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    Document query = new Document("status", "COMPLETED");
    List<Onboarding> expected = List.of(new Onboarding());
    when(onboardingRepository.find(query)).thenReturn(panacheQuery);
    when(panacheQuery.page(0, 20)).thenReturn(panacheQuery);
    when(panacheQuery.list()).thenReturn(expected);

    // when
    List<Onboarding> actual = service.findByQueryPaged(query, 0, 20);

    // then
    assertSame(expected, actual);
    verify(onboardingRepository).find(query);
    verify(panacheQuery).page(0, 20);
    verify(panacheQuery).list();
  }

  @Test
  void findByFilters_shouldDelegateToRepository() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    List<Onboarding> expected = List.of(new Onboarding());
    when(onboardingRepository.findByFilters("tax", "sub", "origin", "originId", "product"))
      .thenReturn(expected);

    // when
    List<Onboarding> actual = service.findByFilters("tax", "sub", "origin", "originId", "product");

    // then
    assertSame(expected, actual);
    verify(onboardingRepository).findByFilters("tax", "sub", "origin", "originId", "product");
  }

  @Test
  void findByOnboardingUsers_shouldDelegateToRepository() {
    // given
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    List<Onboarding> expected = List.of(new Onboarding());
    when(onboardingRepository.findByOnboardingUsers("institution-id", "product-id")).thenReturn(expected);

    // when
    List<Onboarding> actual = service.findByOnboardingUsers("institution-id", "product-id");

    // then
    assertSame(expected, actual);
    verify(onboardingRepository).findByOnboardingUsers("institution-id", "product-id");
  }

  @Test
  @SuppressWarnings("unchecked")
  void updateStatusByQuery_shouldSetStatusAndUpdatedAt() {
    OnboardingRepository onboardingRepository = Mockito.mock(OnboardingRepository.class);
    MongoCollection<Onboarding> collection = Mockito.mock(MongoCollection.class);
    when(onboardingRepository.mongoCollection()).thenReturn(collection);
    OnboardingRepositoryServiceImpl service = new OnboardingRepositoryServiceImpl(onboardingRepository);
    Document query = new Document("referenceOnboardingId", "adhesion-id");
    LocalDateTime now = LocalDateTime.now();

    service.updateStatusByQuery(query, "REJECTED", now);

    verify(collection).updateMany(query,
        new Document("$set", new Document("status", "REJECTED").append("updatedAt", now)));
  }
}
