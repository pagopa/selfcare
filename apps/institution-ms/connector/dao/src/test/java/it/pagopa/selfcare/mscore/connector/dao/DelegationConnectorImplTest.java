package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.DelegationEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationInstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationPageEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.mapper.DelegationEntityMapper;
import it.pagopa.selfcare.mscore.connector.dao.model.mapper.DelegationEntityMapperImpl;
import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.constant.Order;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.model.delegation.*;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.selfcare.mscore.constant.GenericError.CREATE_DELEGATION_ERROR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {DelegationConnectorImpl.class})
@ExtendWith(MockitoExtension.class)
class DelegationConnectorImplTest {

    public static final int PAGE_SIZE = 0;
    public static final int MAX_PAGE_SIZE = 100;
    static Institution dummyInstitution;

    static {
        dummyInstitution = new Institution();
        dummyInstitution.setTaxCode("taxCode");
    }

    @InjectMocks
    private DelegationConnectorImpl delegationConnectorImpl;

    @Mock
    private DelegationRepository delegationRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Spy
    private DelegationEntityMapper delegationMapper = new DelegationEntityMapperImpl();

    @Test
    void testSaveDelegation() {
        DelegationEntity delegationEntity = new DelegationEntity();
        delegationEntity.setId("id");
        delegationEntity.setType(DelegationType.PT);
        delegationEntity.setIsTest(true);
        when(delegationRepository.save(Mockito.any())).thenReturn(delegationEntity);
        Delegation response = delegationConnectorImpl.save(new Delegation());
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(response.getId(), delegationEntity.getId());
        assertEquals(response.getType(), delegationEntity.getType());
        assertEquals(response.getIsTest(), delegationEntity.getIsTest());
    }

    @Test
    void testSaveDelegationWithError() {
        when(delegationRepository.save(any())).thenThrow(new MsCoreException(CREATE_DELEGATION_ERROR.getMessage(), CREATE_DELEGATION_ERROR.getCode()));
        assertThrows(MsCoreException.class, () -> delegationConnectorImpl.save(new Delegation()));
        verify(delegationRepository).save(any());
    }

    @Test
    void testCheckIfExists() {
        Delegation delegation = new Delegation();
        delegation.setTo("to");
        delegation.setFrom("from");
        delegation.setType(DelegationType.PT);
        delegation.setProductId("prod");
        delegation.setStatus(DelegationState.DELETED);
        when(delegationRepository.findByFromAndToAndProductIdAndTypeAndStatus(any(), any(), any(), any(), any())).thenReturn(Optional.of(new DelegationEntity()));
        boolean response = delegationConnectorImpl.checkIfExistsWithStatus(delegation, DelegationState.DELETED);
        assertTrue(response);

    }

    @Test
    void find_shouldGetData() {
        DelegationEntity delegationEntity = new DelegationEntity();
        delegationEntity.setId("id");
        delegationEntity.setProductId("productId");
        delegationEntity.setType(DelegationType.PT);
        delegationEntity.setTo("To");
        delegationEntity.setFrom("From");
        delegationEntity.setInstitutionFromName("setInstitutionFromName");
        delegationEntity.setInstitutionFromRootName("setInstitutionFromRootName");

        List<DelegationEntity> delegationEntities = List.of(delegationEntity);
        Page<DelegationEntity> delegationEntityPage = new PageImpl<>(delegationEntities);
        //When

        doReturn(delegationEntityPage)
                .when(delegationRepository)
                .find(any(), any(), any());

        List<Delegation> response = delegationConnectorImpl.find(delegationEntity.getFrom(),
                delegationEntity.getTo(), delegationEntity.getProductId(), null, null, Order.NONE, PAGE_SIZE, MAX_PAGE_SIZE);

        //Then
        assertNotNull(response);
        assertFalse(response.isEmpty());
        Delegation actual = response.get(0);

        assertEquals(actual.getId(), delegationEntity.getId());
        assertEquals(actual.getType(), delegationEntity.getType());
        assertEquals(actual.getProductId(), delegationEntity.getProductId());
        assertEquals(actual.getTo(), delegationEntity.getTo());
        assertEquals(actual.getFrom(), delegationEntity.getFrom());
        assertEquals(actual.getInstitutionFromName(), delegationEntity.getInstitutionFromName());
        assertEquals(actual.getInstitutionFromRootName(), delegationEntity.getInstitutionFromRootName());
        assertEquals(actual.getToTaxCode(), delegationEntity.getToTaxCode());
        assertEquals(actual.getFromTaxCode(), delegationEntity.getFromTaxCode());
    }

    @Test
    void findByIdAndModifyStatus() {
        DelegationEntity delegationEntity = new DelegationEntity();
        delegationEntity.setId("id");
        delegationEntity.setStatus(DelegationState.ACTIVE);
        when(delegationRepository.findAndModify(any(), any(), any(), any())).thenReturn(delegationEntity);
        Delegation delegation = delegationConnectorImpl.findByIdAndModifyStatus(delegationEntity.getId(), DelegationState.DELETED);
        assertNotNull(delegation);
        assertEquals(delegation.getId(), delegationEntity.getId());
    }

    @Test
    void findByIdAndModifyStatus_whenDeleted_shouldSetClosedAt() {
        DelegationEntity delegationEntity = new DelegationEntity();
        delegationEntity.setId("id");
        delegationEntity.setStatus(DelegationState.ACTIVE);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        when(delegationRepository.findAndModify(any(), updateCaptor.capture(), any(), any()))
                .thenReturn(delegationEntity);

        delegationConnectorImpl.findByIdAndModifyStatus(delegationEntity.getId(), DelegationState.DELETED);

        Update update = updateCaptor.getValue();

        Document updateDoc = update.getUpdateObject();

        assertTrue(update.modifies(DelegationEntity.Fields.closedAt.name()));
        assertTrue(updateDoc.containsKey("$set"));
        Document setDoc = (Document) updateDoc.get("$set");
        assertTrue(setDoc.containsKey(DelegationEntity.Fields.closedAt.name()));
        assertNotNull(setDoc.get(DelegationEntity.Fields.closedAt.name()));
    }

    @Test
    void findByIdAndModifyStatus_whenActivated_shouldSetClosedAtToNull() {
        DelegationEntity delegationEntity = new DelegationEntity();
        delegationEntity.setId("id");
        delegationEntity.setStatus(DelegationState.DELETED);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        when(delegationRepository.findAndModify(any(), updateCaptor.capture(), any(), any()))
                .thenReturn(delegationEntity);

        delegationConnectorImpl.findByIdAndModifyStatus("id", DelegationState.ACTIVE);

        Update update = updateCaptor.getValue();

        Document updateDoc = update.getUpdateObject();

        assertTrue(update.modifies(DelegationEntity.Fields.closedAt.name()));
        assertTrue(updateDoc.containsKey("$set"));
        Document setDoc = (Document) updateDoc.get("$set");
        assertTrue(setDoc.containsKey(DelegationEntity.Fields.closedAt.name()));
        assertNull(setDoc.get(DelegationEntity.Fields.closedAt.name()));
    }

    @Test
    void checkIfDelegationsAreActive_true() {
        when(delegationRepository.findByToAndStatus(anyString(), any())).thenReturn(Optional.of(List.of(new DelegationEntity())));
        boolean response = delegationConnectorImpl.checkIfDelegationsAreActive("id");
        assertTrue(response);
    }

    @Test
    void checkIfDelegationsAreActive_false() {
        when(delegationRepository.findByToAndStatus(anyString(), any())).thenReturn(Optional.of(Collections.emptyList()));
        boolean response = delegationConnectorImpl.checkIfDelegationsAreActive("id");
        assertFalse(response);
    }


    @Test
    void find_shouldGetDataPaginated() {
        DelegationEntity delegationEntity = new DelegationEntity();
        delegationEntity.setId("id");
        delegationEntity.setProductId("productId");
        delegationEntity.setType(DelegationType.PT);
        delegationEntity.setTo("To");
        delegationEntity.setFrom("From");
        delegationEntity.setInstitutionFromName("setInstitutionFromName");
        delegationEntity.setInstitutionFromRootName("setInstitutionFromRootName");

        List<DelegationEntity> delegationEntities = List.of(delegationEntity);
        Page<DelegationEntity> delegationEntityPage = new PageImpl<>(delegationEntities);

        //When
        doReturn(delegationEntityPage)
                .when(delegationRepository)
                .find(any(), any(), any());

        List<Delegation> response = delegationConnectorImpl.find(null, delegationEntity.getTo(), "productId",
                null, null, Order.NONE, 0, 1);

        //Then
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertEquals(1, response.size());

        Delegation actual = response.get(0);

        assertEquals(actual.getId(), delegationEntity.getId());
        assertEquals(actual.getType(), delegationEntity.getType());
        assertEquals(actual.getProductId(), delegationEntity.getProductId());
        assertEquals(actual.getTo(), delegationEntity.getTo());
        assertEquals(actual.getFrom(), delegationEntity.getFrom());
        assertEquals(actual.getFromTaxCode(), delegationEntity.getFromTaxCode());
        assertEquals(actual.getToTaxCode(), delegationEntity.getToTaxCode());

    }

    @Test
    void findAndActivate_shouldSetIsTest() {
        DelegationEntity delegationEntity = new DelegationEntity();
        delegationEntity.setId("id");
        delegationEntity.setFrom("from");
        delegationEntity.setTo("to");
        delegationEntity.setProductId("prod-io");
        delegationEntity.setStatus(DelegationState.ACTIVE);
        delegationEntity.setIsTest(true);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        when(delegationRepository.findAndModify(any(), updateCaptor.capture(), any(), any())).thenReturn(delegationEntity);
        Delegation delegation = delegationConnectorImpl.findAndActivate(delegationEntity.getFrom(), delegationEntity.getTo(), delegationEntity.getProductId(), delegationEntity.getIsTest());
        Update update = updateCaptor.getValue();
        Document updateDoc = update.getUpdateObject();

        assertTrue(updateDoc.containsKey("$set"));
        Document setDoc = (Document) updateDoc.get("$set");
        assertTrue(setDoc.containsKey(DelegationEntity.Fields.isTest.name()));
        assertNotNull(setDoc.get(DelegationEntity.Fields.isTest.name()));
        assertEquals(delegationEntity.getIsTest(), setDoc.get(DelegationEntity.Fields.isTest.name()));
        assertNotNull(delegation);
        assertEquals(delegation.getId(), delegationEntity.getId());
        assertEquals(delegation.getIsTest(), delegationEntity.getIsTest());
    }

    @Test
    void findAndActivate_shouldNotSetIsTest() {
        DelegationEntity delegationEntity = new DelegationEntity();
        delegationEntity.setId("id");
        delegationEntity.setFrom("from");
        delegationEntity.setTo("to");
        delegationEntity.setProductId("prod-io");
        delegationEntity.setStatus(DelegationState.ACTIVE);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        when(delegationRepository.findAndModify(any(), updateCaptor.capture(), any(), any())).thenReturn(delegationEntity);
        Delegation delegation = delegationConnectorImpl.findAndActivate(delegationEntity.getFrom(), delegationEntity.getTo(), delegationEntity.getProductId(), delegationEntity.getIsTest());
        Update update = updateCaptor.getValue();
        Document updateDoc = update.getUpdateObject();

        assertTrue(updateDoc.containsKey("$set"));
        Document setDoc = (Document) updateDoc.get("$set");
        assertFalse(setDoc.containsKey(DelegationEntity.Fields.isTest.name()));
        assertNull(setDoc.get(DelegationEntity.Fields.isTest.name()));
        assertNotNull(delegation);
        assertEquals(delegation.getId(), delegationEntity.getId());
    }

    @Test
    void findAndCount_shouldGetData() {

        DelegationEntity delegationEntity = new DelegationEntity();
        delegationEntity.setId("id");
        delegationEntity.setProductId("productId");
        delegationEntity.setType(DelegationType.PT);
        delegationEntity.setTo("To");
        delegationEntity.setFrom("From");
        delegationEntity.setInstitutionFromName("setInstitutionFromName");
        delegationEntity.setInstitutionFromRootName("setInstitutionFromRootName");


        List<DelegationEntity> delegationEntities = List.of(delegationEntity);
        Page<DelegationEntity> delegationEntityPage = new PageImpl<>(delegationEntities);

        Pageable pageable = PageRequest.of(PAGE_SIZE, MAX_PAGE_SIZE);
        Page<Delegation> result = PageableExecutionUtils.getPage(List.of(new Delegation()), pageable, () -> 1L);
        PageInfo expectedPageInfo = new PageInfo(result.getSize(), result.getNumber(), result.getTotalElements(), result.getTotalPages());

        //When

        doReturn(delegationEntityPage)
                .when(delegationRepository)
                .find(any(), any(), any());

        doReturn(1L)
                .when(mongoTemplate)
                .count(any(), eq(DelegationEntity.class));

        DelegationWithPagination response = delegationConnectorImpl.findAndCount(createDelegationParameters(delegationEntity.getFrom(),
                delegationEntity.getTo(), delegationEntity.getProductId(), null, null, Order.NONE, PAGE_SIZE, MAX_PAGE_SIZE));

        //Then
        assertNotNull(response);
        assertNotNull(response.getDelegations());
        assertNotNull(response.getPageInfo());
        assertFalse(response.getDelegations().isEmpty());
        Delegation actualDelegation = response.getDelegations().get(0);
        PageInfo actualPageInfo = response.getPageInfo();

        assertEquals(actualDelegation.getId(), delegationEntity.getId());
        assertEquals(actualDelegation.getType(), delegationEntity.getType());
        assertEquals(actualDelegation.getProductId(), delegationEntity.getProductId());
        assertEquals(actualDelegation.getTo(), delegationEntity.getTo());
        assertEquals(actualDelegation.getFrom(), delegationEntity.getFrom());
        assertEquals(actualDelegation.getInstitutionFromName(), delegationEntity.getInstitutionFromName());
        assertEquals(actualDelegation.getInstitutionFromRootName(), delegationEntity.getInstitutionFromRootName());
        assertEquals(actualPageInfo, expectedPageInfo);
    }
    private GetDelegationParameters createDelegationParameters(String from, String to, String productId,
                                                               String search, String taxCode, Order order,
                                                               Integer page, Integer size) {
        return GetDelegationParameters.builder()
                .from(from)
                .to(to)
                .productId(productId)
                .search(search)
                .taxCode(taxCode)
                .order(order)
                .page(page)
                .size(size)
                .build();
    }

    @Test
    void updateDelegation() {

        String description = "description";
        String rootName = "rootName";
        String institutionId = "institutionId";

        Institution institutionUpdate = new Institution();
        institutionUpdate.setId(institutionId);
        institutionUpdate.setDescription(description);
        institutionUpdate.setParentDescription(rootName);
        institutionUpdate.setDelegation(true);

        //when
        final Executable executable = () -> delegationConnectorImpl.updateDelegation(institutionUpdate);

        Assertions.assertDoesNotThrow(executable);
        verify(delegationRepository, times(2)).updateMulti(any(), any(), any());

    }

    @Test
    void updateDelegation_noDelegation() {

        String description = "description";
        String institutionId = "institutionId";

        Institution institutionUpdate = new Institution();
        institutionUpdate.setId(institutionId);
        institutionUpdate.setDescription(description);

        //when
        final Executable executable = () -> delegationConnectorImpl.updateDelegation(institutionUpdate);

        Assertions.assertDoesNotThrow(executable);
        verify(delegationRepository, times(1)).updateMulti(any(), any(), any());

    }

    @Test
    void findDelegatorsAndDelegatesTest() {
        final DelegationInstitutionEntity entity1 = new DelegationInstitutionEntity();
        entity1.setId("456");
        entity1.setCreatedAt(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        entity1.setProductId("productId");
        entity1.setType(DelegationType.EA);
        final InstitutionEntity instEntity1 = new InstitutionEntity();
        instEntity1.setId("456");
        instEntity1.setDigitalAddress("test1@test.com");
        instEntity1.setDescription("Institution1");
        entity1.setInstitution(instEntity1);

        final DelegationInstitutionEntity entity2 = new DelegationInstitutionEntity();
        entity2.setId("789");
        entity2.setCreatedAt(OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        entity2.setProductId("productId");
        entity2.setType(DelegationType.EA);
        final InstitutionEntity instEntity2 = new InstitutionEntity();
        instEntity2.setId("789");
        instEntity2.setDigitalAddress("test2@test.com");
        instEntity2.setDescription("Institution2");
        entity2.setInstitution(instEntity2);

        final AggregationResults<DelegationInstitutionEntity> aggregationResults = new AggregationResults<>(List.of(entity1, entity2), new Document());
        when(mongoTemplate.aggregate(any(Aggregation.class), anyString(), any(Class.class))).thenReturn(aggregationResults);

        final List<DelegationInstitution> delegators = delegationConnectorImpl.findDelegators("institutionId", "productId", DelegationType.EA, 123L, 100);
        final List<DelegationInstitution> delegates = delegationConnectorImpl.findDelegates("institutionId", "productId", DelegationType.EA, 123L, 100);

        assertEquals(2, delegators.size());
        assertEquals(2, delegates.size());

        assertEquals(1704067200000L, delegators.get(0).getId());
        assertEquals(1704067200000L, delegates.get(0).getId());
        assertEquals("456", delegators.get(0).getDelegationId());
        assertEquals("456", delegates.get(0).getDelegationId());
        assertEquals("456", delegators.get(0).getInstitution().getId());
        assertEquals("456", delegates.get(0).getInstitution().getId());
        assertEquals("productId", delegators.get(0).getDelegationProductId());
        assertEquals("productId", delegates.get(0).getDelegationProductId());
        assertEquals(DelegationType.EA, delegators.get(0).getDelegationType());
        assertEquals(DelegationType.EA, delegates.get(0).getDelegationType());
        assertEquals("test1@test.com", delegators.get(0).getInstitution().getDigitalAddress());
        assertEquals("test1@test.com", delegates.get(0).getInstitution().getDigitalAddress());
        assertEquals("Institution1", delegators.get(0).getInstitution().getDescription());
        assertEquals("Institution1", delegates.get(0).getInstitution().getDescription());

        assertEquals(1735689600000L, delegators.get(1).getId());
        assertEquals(1735689600000L, delegates.get(1).getId());
        assertEquals("789", delegators.get(1).getDelegationId());
        assertEquals("789", delegates.get(1).getDelegationId());
        assertEquals("789", delegators.get(1).getInstitution().getId());
        assertEquals("789", delegates.get(1).getInstitution().getId());
        assertEquals("productId", delegators.get(1).getDelegationProductId());
        assertEquals("productId", delegates.get(1).getDelegationProductId());
        assertEquals(DelegationType.EA, delegators.get(1).getDelegationType());
        assertEquals(DelegationType.EA, delegates.get(1).getDelegationType());
        assertEquals("test2@test.com", delegators.get(1).getInstitution().getDigitalAddress());
        assertEquals("test2@test.com", delegates.get(1).getInstitution().getDigitalAddress());
        assertEquals("Institution2", delegators.get(1).getInstitution().getDescription());
        assertEquals("Institution2", delegates.get(1).getInstitution().getDescription());
    }

    @Test
    void findFromDateTest() {
        final DelegationPageEntity delegationPageEntity = new DelegationPageEntity();
        delegationPageEntity.setDelegations(List.of(
                new DelegationEntity(),
                new DelegationEntity(),
                new DelegationEntity()
        ));
        final DelegationPageEntity.PageInfo pageInfo = new DelegationPageEntity.PageInfo();
        pageInfo.setTotalElements(3L);
        delegationPageEntity.setPageInfo(List.of(pageInfo));

        when(mongoTemplate.aggregate(any(Aggregation.class), eq("Delegations"), eq(DelegationPageEntity.class)))
                .thenReturn(new AggregationResults<>(List.of(delegationPageEntity), new Document()));

        final DelegationWithPagination result0 = delegationConnectorImpl.findFromDate(OffsetDateTime.now(), 0L, 10L);
        assertEquals(3, result0.getDelegations().size());
        assertEquals(0, result0.getPageInfo().getPageNo());
        assertEquals(1, result0.getPageInfo().getTotalPages());
        assertEquals(3, result0.getPageInfo().getTotalElements());
        assertEquals(10, result0.getPageInfo().getPageSize());

        final DelegationWithPagination result1 = delegationConnectorImpl.findFromDate(OffsetDateTime.now(), null, null);
        assertEquals(3, result1.getDelegations().size());
        assertEquals(0, result1.getPageInfo().getPageNo());
        assertEquals(1, result1.getPageInfo().getTotalPages());
        assertEquals(3, result1.getPageInfo().getTotalElements());
        assertEquals(100, result1.getPageInfo().getPageSize());

        final DelegationWithPagination result2 = delegationConnectorImpl.findFromDate(OffsetDateTime.now(), -1L, -1L);
        assertEquals(3, result2.getDelegations().size());
        assertEquals(0, result2.getPageInfo().getPageNo());
        assertEquals(1, result2.getPageInfo().getTotalPages());
        assertEquals(3, result2.getPageInfo().getTotalElements());
        assertEquals(100, result2.getPageInfo().getPageSize());
    }

}
