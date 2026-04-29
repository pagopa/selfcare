package it.pagopa.selfcare.delegation.event.service;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.delegation.event.DelegationCdcService;
import it.pagopa.selfcare.delegation.event.client.EventHubRestClient;
import it.pagopa.selfcare.delegation.event.config.ConfigUtilsBean;
import it.pagopa.selfcare.delegation.event.constant.DelegationType;
import it.pagopa.selfcare.delegation.event.constant.RelationshipState;
import it.pagopa.selfcare.delegation.event.entity.DelegationsEntity;
import it.pagopa.selfcare.delegation.event.entity.Institution;
import it.pagopa.selfcare.delegation.event.entity.OnboardingEntity;
import it.pagopa.selfcare.delegation.event.mapper.DelegationMapper;
import it.pagopa.selfcare.delegation.event.repository.DelegationRepository;
import it.pagopa.selfcare.delegation.event.repository.InstitutionRepository;
import jakarta.inject.Inject;
import org.bson.BsonDocument;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class DelegationCdcServiceTest {

    @ConfigProperty(name = "delegation-cdc.retry.min-backoff")
    int retryMinBackOff = 1;

    @ConfigProperty(name = "delegation-cdc.retry.max-backoff")
    int retryMaxBackOff = 1;

    @ConfigProperty(name = "delegation-cdc.retry")
    int maxRetry = 0;

    @ConfigProperty(name = "delegation-cdc.products.available")
    List<String> availableProducts;

    @Inject
    DelegationCdcService delegationCdcService;

    @InjectMock
    ConfigUtilsBean configUtilsBean;

    @InjectMock
    InstitutionRepository institutionRepository;

    @InjectMock
    DelegationRepository delegationRepository;

    @Inject
    DelegationMapper delegationMapper;

    @InjectMock
    TelemetryClient telemetryClient;

    @InjectMock
    TableClient tableClient;

    @RestClient
    @InjectMock
    EventHubRestClient eventHubRestClient;

    @Test
    void propagateDocumentToConsumers() {
        //given
        ChangeStreamDocument<DelegationsEntity> document = mock(ChangeStreamDocument.class);
        DelegationsEntity delegationsEntity = new DelegationsEntity();
        delegationsEntity.setId("id");
        delegationsEntity.setType(DelegationType.PT);
        delegationsEntity.setProductId(PROD_PAGOPA.getValue());
        delegationsEntity.setFrom("institutionId");
        delegationsEntity.setTo("toInstitutionId");

        Institution institution = new Institution();
        OnboardingEntity onboardingEntity = new OnboardingEntity();
        onboardingEntity.setStatus(RelationshipState.ACTIVE);
        onboardingEntity.setCreatedAt("2024-07-09T12:40:21.414946089Z");
        onboardingEntity.setProductId(PROD_PAGOPA.getValue());
        onboardingEntity.setIsAggregator(Boolean.TRUE);
        institution.setOnboarding(List.of(onboardingEntity));

        DelegationsEntity delegationEA = new DelegationsEntity();
        delegationEA.setId("idEA");
        delegationEA.setFrom("institutionIdFromEA");
        delegationEA.setTo("institutionIdToEA");
        delegationEA.setProductId(PROD_PAGOPA.getValue());

        BsonDocument bsonDocument = mock(BsonDocument.class);
        BsonDocument bsonDocument1 = new BsonDocument();

        //when
        when(document.getFullDocument()).thenReturn(delegationsEntity);
        when(document.getDocumentKey()).thenReturn(bsonDocument);
        when(document.getResumeToken()).thenReturn(bsonDocument1);
        when(bsonDocument.toJson()).thenReturn("toJson");
        when(institutionRepository.findInstitutionById(anyString())).thenReturn(Uni.createFrom().item(institution));
        when(delegationRepository.getInstitutionsAlreadyPresent(anyString(), anyString())).thenReturn(Multi.createFrom().empty());
        when(delegationRepository.getDelegationsEA(anyString(), anyString())).thenReturn(Multi.createFrom().items(delegationEA));
        when(delegationRepository.insertDelegations(ArgumentMatchers.any())).thenReturn(Uni.createFrom().nullItem());

        // then
        final Executable executable = () -> delegationCdcService.consumerDelegationRepositoryEvent(document);
        assertDoesNotThrow(executable);

        // verify
        verify(institutionRepository).findInstitutionById(anyString());
        verify(delegationRepository).getInstitutionsAlreadyPresent(anyString(), anyString());
        verify(delegationRepository).getDelegationsEA(anyString(), anyString());
        verify(delegationRepository).insertDelegations(ArgumentMatchers.any());
        verify(eventHubRestClient).sendMessage(any());
        verify(tableClient, times(1)).upsertEntity(any());

        ArgumentCaptor<Map<String, Double>> metricsName = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient, times(2)).trackEvent(eq("DELEGATION_CDC"), any(), metricsName.capture());
        final Set<String> allValues = metricsName.getAllValues().stream().flatMap(x -> x.keySet().stream()).collect(Collectors.toSet());
        assertTrue(allValues.containsAll(List.of("DelegationInsert_success", "DelegationEvent_success")));
    }

    @Test
    void propagateDocumentToConsumersWithEventsFailure() {
        //given
        ChangeStreamDocument<DelegationsEntity> document = mock(ChangeStreamDocument.class);
        DelegationsEntity delegationsEntity = new DelegationsEntity();
        delegationsEntity.setId("id");
        delegationsEntity.setType(DelegationType.PT);
        delegationsEntity.setProductId(PROD_PAGOPA.getValue());
        delegationsEntity.setFrom("institutionId");
        delegationsEntity.setTo("toInstitutionId");

        Institution institution = new Institution();
        OnboardingEntity onboardingEntity = new OnboardingEntity();
        onboardingEntity.setStatus(RelationshipState.ACTIVE);
        onboardingEntity.setCreatedAt("2024-07-09T12:40:21.414946089Z");
        onboardingEntity.setProductId(PROD_PAGOPA.getValue());
        onboardingEntity.setIsAggregator(Boolean.TRUE);
        institution.setOnboarding(List.of(onboardingEntity));

        DelegationsEntity delegationEA = new DelegationsEntity();
        delegationEA.setId("idEA");
        delegationEA.setFrom("institutionIdFromEA");
        delegationEA.setTo("institutionIdToEA");
        delegationEA.setProductId(PROD_PAGOPA.getValue());

        BsonDocument bsonDocument = mock(BsonDocument.class);
        BsonDocument bsonDocument1 = new BsonDocument();

        //when
        when(document.getFullDocument()).thenReturn(delegationsEntity);
        when(document.getDocumentKey()).thenReturn(bsonDocument);
        when(document.getResumeToken()).thenReturn(bsonDocument1);
        when(bsonDocument.toJson()).thenReturn("toJson");
        when(institutionRepository.findInstitutionById(anyString())).thenReturn(Uni.createFrom().item(institution));
        when(delegationRepository.getInstitutionsAlreadyPresent(anyString(), anyString())).thenReturn(Multi.createFrom().empty());
        when(delegationRepository.getDelegationsEA(anyString(), anyString())).thenReturn(Multi.createFrom().items(delegationEA));
        when(delegationRepository.insertDelegations(ArgumentMatchers.any())).thenReturn(Uni.createFrom().nullItem());
        when(eventHubRestClient.sendMessage(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("EventHub failure")));

        // then
        final Executable executable = () -> delegationCdcService.consumerDelegationRepositoryEvent(document);
        assertDoesNotThrow(executable);

        // verify
        verify(institutionRepository).findInstitutionById(anyString());
        verify(delegationRepository).getInstitutionsAlreadyPresent(anyString(), anyString());
        verify(delegationRepository).getDelegationsEA(anyString(), anyString());
        verify(delegationRepository).insertDelegations(ArgumentMatchers.any());
        verify(eventHubRestClient).sendMessage(any());
        verify(tableClient, never()).upsertEntity(any());

        ArgumentCaptor<Map<String, Double>> metricsName = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient, times(2)).trackEvent(eq("DELEGATION_CDC"), any(), metricsName.capture());
        final Set<String> allValues = metricsName.getAllValues().stream().flatMap(x -> x.keySet().stream()).collect(Collectors.toSet());
        assertTrue(allValues.containsAll(List.of("DelegationInsert_success", "DelegationEvent_failure")));
    }

    @Test
    void propagateDocumentToConsumersWithoutEvents() {
        //given
        ChangeStreamDocument<DelegationsEntity> document = mock(ChangeStreamDocument.class);
        DelegationsEntity delegationsEntity = new DelegationsEntity();
        delegationsEntity.setId("id");
        delegationsEntity.setType(DelegationType.PT);
        delegationsEntity.setProductId(PROD_PAGOPA.getValue());
        delegationsEntity.setFrom("institutionId");
        delegationsEntity.setTo("toInstitutionId");

        Institution institution = new Institution();
        OnboardingEntity onboardingEntity = new OnboardingEntity();
        onboardingEntity.setStatus(RelationshipState.ACTIVE);
        onboardingEntity.setCreatedAt("2024-07-09T12:40:21.414946089Z");
        onboardingEntity.setProductId(PROD_PAGOPA.getValue());
        onboardingEntity.setIsAggregator(Boolean.TRUE);
        institution.setOnboarding(List.of(onboardingEntity));

        DelegationsEntity delegationEA = new DelegationsEntity();
        delegationEA.setId("idEA");
        delegationEA.setFrom("institutionIdFromEA");
        delegationEA.setTo("institutionIdToEA");
        delegationEA.setProductId(PROD_PAGOPA.getValue());

        BsonDocument bsonDocument = mock(BsonDocument.class);
        BsonDocument bsonDocument1 = new BsonDocument();

        //when
        when(document.getFullDocument()).thenReturn(delegationsEntity);
        when(document.getDocumentKey()).thenReturn(bsonDocument);
        when(document.getResumeToken()).thenReturn(bsonDocument1);
        when(bsonDocument.toJson()).thenReturn("toJson");
        when(institutionRepository.findInstitutionById(anyString())).thenReturn(Uni.createFrom().item(institution));
        when(delegationRepository.getInstitutionsAlreadyPresent(anyString(), anyString())).thenReturn(Multi.createFrom().empty());
        when(delegationRepository.getDelegationsEA(anyString(), anyString())).thenReturn(Multi.createFrom().items(delegationEA));
        when(delegationRepository.insertDelegations(ArgumentMatchers.any())).thenReturn(Uni.createFrom().nullItem());

        TelemetryContext context = mock(TelemetryContext.class);
        OperationContext operationContext = mock(OperationContext.class);
        when(telemetryClient.getContext()).thenReturn(context);
        when(context.getOperation()).thenReturn(operationContext);

        ReactiveMongoClient mongoClientMock = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase mongoDatabase = mock(ReactiveMongoDatabase.class);
        ReactiveMongoCollection<DelegationsEntity> collectionMock = mock(ReactiveMongoCollection.class);
        when(mongoClientMock.getDatabase(anyString())).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection(anyString(), eq(DelegationsEntity.class))).thenReturn(collectionMock);

        // then
        final DelegationCdcService delegationCdcServiceImpl = new DelegationCdcService(
                mongoClientMock, "testDatabase", telemetryClient, tableClient,
                configUtilsBean, institutionRepository, delegationRepository, retryMinBackOff, retryMaxBackOff, maxRetry,
                availableProducts, false);
        final Executable executable = () -> delegationCdcServiceImpl.consumerDelegationRepositoryEvent(document);
        assertDoesNotThrow(executable);

        // verify
        verify(institutionRepository).findInstitutionById(anyString());
        verify(delegationRepository).getInstitutionsAlreadyPresent(anyString(), anyString());
        verify(delegationRepository).getDelegationsEA(anyString(), anyString());
        verify(delegationRepository).insertDelegations(ArgumentMatchers.any());
        verify(eventHubRestClient, never()).sendMessage(any());
        verify(tableClient, times(1)).upsertEntity(any());

        ArgumentCaptor<Map<String, Double>> metricsName = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient, times(1)).trackEvent(eq("DELEGATION_CDC"), any(), metricsName.capture());
        final Set<String> allValues = metricsName.getAllValues().stream().flatMap(x -> x.keySet().stream()).collect(Collectors.toSet());
        assertTrue(allValues.contains("DelegationInsert_success"));
    }

    @Test
    void propagateDocumentToConsumers_exception() {
        //given
        ChangeStreamDocument<DelegationsEntity> document = mock(ChangeStreamDocument.class);
        DelegationsEntity delegationsEntity = new DelegationsEntity();
        delegationsEntity.setId("id");
        delegationsEntity.setType(DelegationType.EA);
        delegationsEntity.setProductId(PROD_PAGOPA.getValue());
        delegationsEntity.setFrom("institutionId");
        delegationsEntity.setTo("toInstitutionId");

        // then
        final Executable executable = () -> delegationCdcService.consumerDelegationRepositoryEvent(document);
        assertThrows(AssertionError.class, executable);

        // verify
        verify(institutionRepository, never()).findInstitutionById(anyString());
        verify(delegationRepository, never()).getInstitutionsAlreadyPresent(anyString(), anyString());
        verify(delegationRepository, never()).getDelegationsEA(anyString(), anyString());
        verify(delegationRepository, never()).insertDelegations(ArgumentMatchers.any());
        verify(eventHubRestClient, never()).sendMessage(any());
        verify(tableClient, never()).upsertEntity(any());
    }


    @Test
    void propagateDocumentToConsumers_notPT() {
        //given
        ChangeStreamDocument<DelegationsEntity> document = mock(ChangeStreamDocument.class);
        DelegationsEntity delegationsEntity = new DelegationsEntity();
        delegationsEntity.setId("id");
        delegationsEntity.setType(DelegationType.EA);
        delegationsEntity.setProductId(PROD_PAGOPA.getValue());
        delegationsEntity.setFrom("institutionId");
        delegationsEntity.setTo("toInstitutionId");

        BsonDocument bsonDocument = mock(BsonDocument.class);
        BsonDocument bsonDocument1 = new BsonDocument();

        //when
        when(document.getFullDocument()).thenReturn(delegationsEntity);
        when(document.getDocumentKey()).thenReturn(bsonDocument);
        when(document.getResumeToken()).thenReturn(bsonDocument1);
        when(bsonDocument.toJson()).thenReturn("toJson");

        // then
        final Executable executable = () -> delegationCdcService.consumerDelegationRepositoryEvent(document);
        assertDoesNotThrow(executable);

        // verify
        verify(institutionRepository, never()).findInstitutionById(anyString());
        verify(delegationRepository, never()).getInstitutionsAlreadyPresent(anyString(), anyString());
        verify(delegationRepository, never()).getDelegationsEA(anyString(), anyString());
        verify(delegationRepository, never()).insertDelegations(ArgumentMatchers.any());
        verify(eventHubRestClient, times(1)).sendMessage(any());
        verify(tableClient, times(1)).upsertEntity(any());

        ArgumentCaptor<Map<String, Double>> metricsName = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient, times(1)).trackEvent(eq("DELEGATION_CDC"), any(), metricsName.capture());
        final Set<String> allValues = metricsName.getAllValues().stream().flatMap(x -> x.keySet().stream()).collect(Collectors.toSet());
        assertTrue(allValues.contains("DelegationEvent_success"));
    }

    @Test
    void propagateDocumentToConsumers_notPagoPA() {
        //given
        ChangeStreamDocument<DelegationsEntity> document = mock(ChangeStreamDocument.class);
        DelegationsEntity delegationsEntity = new DelegationsEntity();
        delegationsEntity.setId("id");
        delegationsEntity.setType(DelegationType.PT);
        delegationsEntity.setProductId(PROD_IO.getValue());
        delegationsEntity.setFrom("institutionId");
        delegationsEntity.setTo("toInstitutionId");

        BsonDocument bsonDocument = mock(BsonDocument.class);
        BsonDocument bsonDocument1 = new BsonDocument();

        //when
        when(document.getFullDocument()).thenReturn(delegationsEntity);
        when(document.getDocumentKey()).thenReturn(bsonDocument);
        when(document.getResumeToken()).thenReturn(bsonDocument1);
        when(bsonDocument.toJson()).thenReturn("toJson");

        // then
        final Executable executable = () -> delegationCdcService.consumerDelegationRepositoryEvent(document);
        assertDoesNotThrow(executable);

        // verify
        verify(institutionRepository, never()).findInstitutionById(anyString());
        verify(delegationRepository, never()).getInstitutionsAlreadyPresent(anyString(), anyString());
        verify(delegationRepository, never()).getDelegationsEA(anyString(), anyString());
        verify(delegationRepository, never()).insertDelegations(ArgumentMatchers.any());
        verify(eventHubRestClient, times(1)).sendMessage(any());
        verify(tableClient, times(1)).upsertEntity(any());

        ArgumentCaptor<Map<String, Double>> metricsName = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient, times(1)).trackEvent(eq("DELEGATION_CDC"), any(), metricsName.capture());
        final Set<String> allValues = metricsName.getAllValues().stream().flatMap(x -> x.keySet().stream()).collect(Collectors.toSet());
        assertTrue(allValues.contains("DelegationEvent_success"));
    }


    @Test
    void propagateDocumentToConsumers_institutionIsNull() {
        //given
        ChangeStreamDocument<DelegationsEntity> document = mock(ChangeStreamDocument.class);
        DelegationsEntity delegationsEntity = new DelegationsEntity();
        delegationsEntity.setId("id");
        delegationsEntity.setType(DelegationType.PT);
        delegationsEntity.setProductId(PROD_PAGOPA.getValue());
        delegationsEntity.setFrom("institutionId");
        delegationsEntity.setTo("toInstitutionId");

        BsonDocument bsonDocument = mock(BsonDocument.class);
        BsonDocument bsonDocument1 = new BsonDocument();

        //when
        when(document.getFullDocument()).thenReturn(delegationsEntity);
        when(document.getDocumentKey()).thenReturn(bsonDocument);
        when(document.getResumeToken()).thenReturn(bsonDocument1);
        when(bsonDocument.toJson()).thenReturn("toJson");

        when(institutionRepository.findInstitutionById(anyString())).thenReturn(Uni.createFrom().nullItem());

        // then
        final Executable executable = () -> delegationCdcService.consumerDelegationRepositoryEvent(document);
        assertDoesNotThrow(executable);

        // Verify
        verify(institutionRepository).findInstitutionById(anyString());
        verify(delegationRepository, never()).getInstitutionsAlreadyPresent(anyString(), anyString());
        verify(delegationRepository, never()).getDelegationsEA(anyString(), anyString());
        verify(delegationRepository, never()).insertDelegations(ArgumentMatchers.any());
        verify(eventHubRestClient, times(1)).sendMessage(any());
        verify(tableClient, never()).upsertEntity(any());

        ArgumentCaptor<Map<String, Double>> metricsName = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient, times(2)).trackEvent(eq("DELEGATION_CDC"), any(), metricsName.capture());
        final Set<String> allValues = metricsName.getAllValues().stream().flatMap(x -> x.keySet().stream()).collect(Collectors.toSet());
        assertTrue(allValues.containsAll(List.of("DelegationEvent_success", "DelegationInsert_failure")));
    }

    @Test
    void propagateDocumentToConsumers_isAggregatorNotTrue() {
        //given
        ChangeStreamDocument<DelegationsEntity> document = mock(ChangeStreamDocument.class);
        DelegationsEntity delegationsEntity = new DelegationsEntity();
        delegationsEntity.setId("id");
        delegationsEntity.setType(DelegationType.PT);
        delegationsEntity.setProductId(PROD_PAGOPA.getValue());
        delegationsEntity.setFrom("institutionId");
        delegationsEntity.setTo("toInstitutionId");

        Institution institution = new Institution();
        OnboardingEntity onboardingEntity = new OnboardingEntity();
        onboardingEntity.setStatus(RelationshipState.ACTIVE);
        onboardingEntity.setProductId(PROD_PAGOPA.getValue());
        onboardingEntity.setIsAggregator(Boolean.TRUE);
        onboardingEntity.setCreatedAt("2024-07-09T12:40:21.414946089Z");
        OnboardingEntity onboardingEntity2 = new OnboardingEntity();
        onboardingEntity2.setStatus(RelationshipState.ACTIVE);
        onboardingEntity2.setProductId(PROD_PAGOPA.getValue());
        onboardingEntity2.setIsAggregator(Boolean.FALSE);
        onboardingEntity2.setCreatedAt("2024-10-12T18:31:21.314946089Z");
        institution.setOnboarding(List.of(onboardingEntity, onboardingEntity2));

        BsonDocument bsonDocument = mock(BsonDocument.class);
        BsonDocument bsonDocument1 = new BsonDocument();

        //when
        when(document.getFullDocument()).thenReturn(delegationsEntity);
        when(document.getDocumentKey()).thenReturn(bsonDocument);
        when(document.getResumeToken()).thenReturn(bsonDocument1);
        when(bsonDocument.toJson()).thenReturn("toJson");
        when(institutionRepository.findInstitutionById(anyString())).thenReturn(Uni.createFrom().item(institution));

        // then
        final Executable executable = () -> delegationCdcService.consumerDelegationRepositoryEvent(document);
        assertDoesNotThrow(executable);

        // Verify
        verify(institutionRepository).findInstitutionById(anyString());
        verify(delegationRepository, never()).getInstitutionsAlreadyPresent(anyString(), anyString());
        verify(delegationRepository, never()).getDelegationsEA(anyString(), anyString());
        verify(delegationRepository, never()).insertDelegations(ArgumentMatchers.any());
        verify(eventHubRestClient, times(1)).sendMessage(any());
        verify(tableClient, times(1)).upsertEntity(any());

        ArgumentCaptor<Map<String, Double>> metricsName = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient, times(2)).trackEvent(eq("DELEGATION_CDC"), any(), metricsName.capture());
        final Set<String> allValues = metricsName.getAllValues().stream().flatMap(x -> x.keySet().stream()).collect(Collectors.toSet());
        assertTrue(allValues.containsAll(List.of("DelegationEvent_success", "DelegationInsert_success")));
    }


    @Test
    void consumerDelegationRepositoryEvent_failure() {
        //given
        ChangeStreamDocument<DelegationsEntity> document = mock(ChangeStreamDocument.class);
        DelegationsEntity delegationsEntity = new DelegationsEntity();
        delegationsEntity.setId("id");
        delegationsEntity.setType(DelegationType.PT);
        delegationsEntity.setProductId(PROD_PAGOPA.getValue());
        delegationsEntity.setFrom("institutionId");
        delegationsEntity.setTo("toInstitutionId");

        Institution institution = new Institution();
        OnboardingEntity onboardingEntity = new OnboardingEntity();
        onboardingEntity.setStatus(RelationshipState.ACTIVE);
        onboardingEntity.setCreatedAt("2024-07-09T12:40:21.414946089Z");
        onboardingEntity.setProductId(PROD_PAGOPA.getValue());
        onboardingEntity.setIsAggregator(Boolean.TRUE);
        institution.setOnboarding(List.of(onboardingEntity));

        BsonDocument bsonDocument = mock(BsonDocument.class);

        //when
        when(document.getFullDocument()).thenReturn(delegationsEntity);
        when(document.getDocumentKey()).thenReturn(bsonDocument);
        when(bsonDocument.toJson()).thenReturn("toJson");
        when(institutionRepository.findInstitutionById(anyString())).thenReturn(Uni.createFrom().item(institution));
        when(delegationRepository.getInstitutionsAlreadyPresent(anyString(), anyString())).thenReturn(Multi.createFrom().empty());
        when(delegationRepository.getDelegationsEA(anyString(), anyString())).thenReturn(Multi.createFrom().items(new DelegationsEntity()));
        when(delegationRepository.insertDelegations(ArgumentMatchers.any())).thenReturn(Uni.createFrom().failure(new RuntimeException("Insert failed")));

        // then
        final Executable executable = () -> delegationCdcService.consumerDelegationRepositoryEvent(document);
        assertDoesNotThrow(executable);

        // verify
        verify(institutionRepository).findInstitutionById(anyString());
        verify(delegationRepository).getInstitutionsAlreadyPresent(anyString(), anyString());
        verify(delegationRepository).getDelegationsEA(anyString(), anyString());
        verify(delegationRepository).insertDelegations(ArgumentMatchers.any());
        verify(eventHubRestClient, times(1)).sendMessage(any());
        verify(tableClient, never()).upsertEntity(any());

        ArgumentCaptor<Map<String, Double>> metricsName = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient, times(2)).trackEvent(eq("DELEGATION_CDC"), anyMap(), metricsName.capture());
        final Set<String> allValues = metricsName.getAllValues().stream().flatMap(x -> x.keySet().stream()).collect(Collectors.toSet());
        assertTrue(allValues.containsAll(List.of("DelegationEvent_success", "DelegationInsert_failure")));
    }


    @Test
    void testDelegationCdcServiceConstructorNotTest() {
        // Configura il mock per il TableClient
        TableClient tableClientMock = mock(TableClient.class);
        when(tableClientMock.getEntity(anyString(), anyString())).thenReturn(null);

        // Configura il mock per il ReactiveMongoClient
        ReactiveMongoClient mongoClientMock = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase mongoDatabase = mock(ReactiveMongoDatabase.class);
        ReactiveMongoCollection<DelegationsEntity> collectionMock = mock(ReactiveMongoCollection.class);
        when(mongoClientMock.getDatabase(anyString())).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection(anyString(), eq(DelegationsEntity.class))).thenReturn(collectionMock);

        //Configura il mock per il TelemetryClient
        TelemetryClient telemetryClient = mock(TelemetryClient.class);
        TelemetryContext context = mock(TelemetryContext.class);
        OperationContext operationContext = mock(OperationContext.class);
        when(telemetryClient.getContext()).thenReturn(context);
        when(context.getOperation()).thenReturn(operationContext);

        Mockito.when(configUtilsBean.getProfiles()).thenReturn(List.of("uat"));

        // Crea l'istanza del servizio
        new DelegationCdcService(mongoClientMock, "testDatabase", telemetryClient, tableClientMock, configUtilsBean, institutionRepository, delegationRepository, retryMinBackOff, retryMaxBackOff, maxRetry, availableProducts, false);

        // Verifica che il metodo watch sia stato chiamato
        verify(collectionMock).watch(anyList(), eq(DelegationsEntity.class), any(ChangeStreamOptions.class));
    }

    @Test
    void testDelegationCdcServiceConstructorNotTestAndTableEntityNotNull() {
        // Configura il mock per il TableClient
        TableClient tableClientMock = mock(TableClient.class);
        when(tableClientMock.getEntity(anyString(), anyString())).thenReturn(mock(TableEntity.class));

        // Configura il mock per il ReactiveMongoClient
        ReactiveMongoClient mongoClientMock = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase mongoDatabase = mock(ReactiveMongoDatabase.class);
        ReactiveMongoCollection<DelegationsEntity> collectionMock = mock(ReactiveMongoCollection.class);
        when(mongoClientMock.getDatabase(anyString())).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection(anyString(), eq(DelegationsEntity.class))).thenReturn(collectionMock);

        //Configura il mock per il TelemetryClient
        TelemetryClient telemetryClient = mock(TelemetryClient.class);
        TelemetryContext context = mock(TelemetryContext.class);
        OperationContext operationContext = mock(OperationContext.class);
        when(telemetryClient.getContext()).thenReturn(context);
        when(context.getOperation()).thenReturn(operationContext);

        Mockito.when(configUtilsBean.getProfiles()).thenReturn(List.of("uat"));

        // Crea l'istanza del servizio
        new DelegationCdcService(mongoClientMock, "testDatabase", telemetryClient, tableClientMock, configUtilsBean, institutionRepository, delegationRepository, retryMinBackOff, retryMaxBackOff, maxRetry, availableProducts, false);

        // Verifica che il metodo watch sia stato chiamato
        verify(collectionMock).watch(anyList(), eq(DelegationsEntity.class), any(ChangeStreamOptions.class));
    }

    @Test
    void testDelegationCdcServiceConstructorNotTestWithException() {
        // Configura il mock per il TableClient
        TableClient tableClientMock = mock(TableClient.class);
        when(tableClientMock.getEntity(anyString(), anyString())).thenThrow(mock(TableServiceException.class));

        // Configura il mock per il ReactiveMongoClient
        ReactiveMongoClient mongoClientMock = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase mongoDatabase = mock(ReactiveMongoDatabase.class);
        ReactiveMongoCollection<DelegationsEntity> collectionMock = mock(ReactiveMongoCollection.class);
        when(mongoClientMock.getDatabase(anyString())).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection(anyString(), eq(DelegationsEntity.class))).thenReturn(collectionMock);

        //Configura il mock per il TelemetryClient
        TelemetryClient telemetryClient = mock(TelemetryClient.class);
        TelemetryContext context = mock(TelemetryContext.class);
        OperationContext operationContext = mock(OperationContext.class);
        when(telemetryClient.getContext()).thenReturn(context);
        when(context.getOperation()).thenReturn(operationContext);

        Mockito.when(configUtilsBean.getProfiles()).thenReturn(List.of("uat"));

        // Crea l'istanza del servizio
        new DelegationCdcService(mongoClientMock, "testDatabase", telemetryClient, tableClientMock, configUtilsBean, institutionRepository, delegationRepository, retryMinBackOff, retryMaxBackOff, maxRetry, availableProducts, false);

        // Verifica che il metodo watch sia stato chiamato
        verify(collectionMock).watch(anyList(), eq(DelegationsEntity.class), any(ChangeStreamOptions.class));
    }

    @Test
    void testDelegationCdcServiceConstructorTest() {
        // Configura il mock per il TableClient
        TableClient tableClientMock = mock(TableClient.class);
        when(tableClientMock.getEntity(anyString(), anyString())).thenReturn(null);

        // Configura il mock per il ReactiveMongoClient
        ReactiveMongoClient mongoClientMock = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase mongoDatabase = mock(ReactiveMongoDatabase.class);
        ReactiveMongoCollection<DelegationsEntity> collectionMock = mock(ReactiveMongoCollection.class);
        when(mongoClientMock.getDatabase(anyString())).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection(anyString(), eq(DelegationsEntity.class))).thenReturn(collectionMock);

        //Configura il mock per il TelemetryClient
        TelemetryClient telemetryClient = mock(TelemetryClient.class);
        TelemetryContext context = mock(TelemetryContext.class);
        OperationContext operationContext = mock(OperationContext.class);
        when(telemetryClient.getContext()).thenReturn(context);
        when(context.getOperation()).thenReturn(operationContext);

        // Crea l'istanza del servizio
        new DelegationCdcService(mongoClientMock, "testDatabase", telemetryClient, tableClientMock, configUtilsBean, institutionRepository, delegationRepository, retryMinBackOff, retryMaxBackOff, maxRetry, availableProducts, false);

        // Verifica che il metodo watch sia stato chiamato
        verify(collectionMock).watch(anyList(), eq(DelegationsEntity.class), any(ChangeStreamOptions.class));
    }

}
