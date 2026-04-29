package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.inner.BillingEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.inner.DataProtectionOfficerEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.inner.OnboardingEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.inner.PaymentServiceProviderEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.mapper.InstitutionEntityMapper;
import it.pagopa.selfcare.mscore.connector.dao.model.mapper.InstitutionEntityMapperImpl;
import it.pagopa.selfcare.mscore.connector.dao.utils.TestUtils;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.constant.SearchMode;
import it.pagopa.selfcare.mscore.exception.InvalidRequestException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.institution.*;
import it.pagopa.selfcare.mscore.model.onboarding.VerifyOnboardingFilters;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.*;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.mscore.constant.CustomError.INSTITUTION_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {InstitutionConnectorImpl.class})
@ExtendWith(MockitoExtension.class)
class InstitutionConnectorImplTest {

    @InjectMocks
    InstitutionConnectorImpl institutionConnectorImpl;

    @Mock
    InstitutionRepository institutionRepository;

    @Spy
    InstitutionEntityMapper institutionMapper = new InstitutionEntityMapperImpl();

    @Captor
    ArgumentCaptor<Query> queryArgumentCaptor;

    @Captor
    ArgumentCaptor<Update> updateArgumentCaptor;

    @Captor
    ArgumentCaptor<FindAndModifyOptions> findAndModifyOptionsArgumentCaptor;

    @Captor
    ArgumentCaptor<Pageable> pageableArgumentCaptor;

    /**
     * Method under test: {@link InstitutionConnectorImpl#findAll()}
     */
    @Test
    void testFindAll() {
        when(institutionRepository.findAll()).thenReturn(new ArrayList<>());
        assertTrue(institutionConnectorImpl.findAll().isEmpty());
        verify(institutionRepository).findAll();
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findAll()}
     */
    @Test
    void testFindAll2() {
        InstitutionEntity institutionEntity = TestUtils.createSimpleInstitutionEntity();

        ArrayList<InstitutionEntity> institutionEntityList = new ArrayList<>();
        institutionEntityList.add(institutionEntity);
        when(institutionRepository.findAll()).thenReturn(institutionEntityList);
        assertEquals(1, institutionConnectorImpl.findAll().size());
        verify(institutionRepository).findAll();
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findAll()}
     */
    @Test
    void testFindAll4() {
        when(institutionRepository.findAll()).thenThrow(new InvalidRequestException("An error occurred", "Code"));
        assertThrows(InvalidRequestException.class, () -> institutionConnectorImpl.findAll());
        verify(institutionRepository).findAll();
    }

    @Test
    void findById() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId("507f1f77bcf86cd799439011");
        Optional<Institution> response = institutionConnectorImpl.findByExternalId("id");
        Assertions.assertTrue(response.isEmpty());
    }

    @Test
    void findByIdAndProduct() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId("507f1f77bcf86cd799439011");

        OnboardingEntity onboardingEntity = new OnboardingEntity();
        onboardingEntity.setProductId("productId_1");

        OnboardingEntity onboardingEntity2 = new OnboardingEntity();
        onboardingEntity2.setProductId("productId_2");

        institutionEntity.setOnboarding(List.of(onboardingEntity, onboardingEntity2));

        when(institutionRepository.findById(any())).thenReturn(Optional.of(institutionEntity));

        Institution response = institutionConnectorImpl.findByIdAndProduct("507f1f77bcf86cd799439011", "productId_1");
        Assertions.assertEquals(onboardingEntity.getProductId() ,response.getOnboarding().get(0).getProductId());
    }

    @Test
    void findByIdAndProduct_NoOnboardings() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId("507f1f77bcf86cd799439011");

        institutionEntity.setOnboarding(Collections.emptyList());

        when(institutionRepository.findById(any())).thenReturn(Optional.of(institutionEntity));

        final Executable executable = () ->  institutionConnectorImpl.findByIdAndProduct("507f1f77bcf86cd799439011", "productId_1");

        final Exception e = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals(String.format(INSTITUTION_NOT_FOUND.getMessage(), "507f1f77bcf86cd799439011", "UNDEFINED"), e.getMessage());
    }

    @Test
    void findByIdAndProduct_NoOnboardingForProduct() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId("507f1f77bcf86cd799439011");

        OnboardingEntity onboardingEntity2 = new OnboardingEntity();
        onboardingEntity2.setProductId("productId_2");

        institutionEntity.setOnboarding(List.of(onboardingEntity2));

        when(institutionRepository.findById(any())).thenReturn(Optional.of(institutionEntity));

        final Executable executable = () ->  institutionConnectorImpl.findByIdAndProduct("507f1f77bcf86cd799439011", "productId_1");

        final Exception e = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals(String.format(INSTITUTION_NOT_FOUND.getMessage(), "507f1f77bcf86cd799439011", "UNDEFINED"), e.getMessage());
    }

    private Institution dummyInstitution() {

        List<InstitutionGeographicTaxonomies> geographicTaxonomies = new ArrayList<>();
        geographicTaxonomies.add(new InstitutionGeographicTaxonomies());

        List<Onboarding> onboardings = new ArrayList<>();
        onboardings.add(dummyOnboarding());

        Institution institution = new Institution();
        institution.setExternalId("ext");
        institution.setId("507f1f77bcf86cd799439011");
        institution.setParentDescription("parentDescription");
        institution.setOnboarding(onboardings);
        institution.setZipCode("zipCpde");
        institution.setRea("rea");
        institution.setGeographicTaxonomies(geographicTaxonomies);

        return institution;
    }

    private Onboarding dummyOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setBilling(new Billing());
        onboarding.setContract("contract");
        onboarding.setStatus(RelationshipState.ACTIVE);
        onboarding.setCreatedAt(OffsetDateTime.now());
        onboarding.setProductId("productId");
        onboarding.setUpdatedAt(OffsetDateTime.now());
        onboarding.setPricingPlan("pricingPal");
        return onboarding;
    }

    @Test
    void shouldSaveInstitution() {

        Onboarding onboarding = dummyOnboarding();
        Institution institution = dummyInstitution();
        when(institutionRepository.save(any())).thenAnswer(arg -> arg.getArguments()[0]);

        Institution response = institutionConnectorImpl.save(institution);
        Assertions.assertEquals(institution.getId(), response.getId());
        Assertions.assertEquals(institution.getExternalId(), response.getExternalId());
        Assertions.assertEquals(institution.getParentDescription(), response.getParentDescription());
        Assertions.assertEquals(institution.getRea(), response.getRea());
        Assertions.assertEquals(institution.getZipCode(), response.getZipCode());
        Assertions.assertEquals(institution.getOnboarding().size(), response.getOnboarding().size());

        Onboarding responseOnboarding = institution.getOnboarding().get(0);
        Assertions.assertEquals(onboarding.getContract(), responseOnboarding.getContract());
        Assertions.assertEquals(onboarding.getStatus(), responseOnboarding.getStatus());
        Assertions.assertEquals(onboarding.getProductId(), responseOnboarding.getProductId());
        Assertions.assertEquals(onboarding.getPricingPlan(), responseOnboarding.getPricingPlan());
    }



    @Test
    void shouldSaveInstitution_whenDuplicateKeyError() {

        when(institutionRepository.save(any())).
                thenThrow(DuplicateKeyException.class)
                .thenAnswer(arg -> arg.getArguments()[0]);

        Institution response = institutionConnectorImpl.save(dummyInstitution());

        ArgumentCaptor<InstitutionEntity> captor = ArgumentCaptor.forClass(InstitutionEntity.class);
        Mockito.verify(institutionRepository, times(2))
                .save(captor.capture());

        List<InstitutionEntity> entities = captor.getAllValues();
        Assertions.assertEquals(entities.get(1).getId(), response.getId());
        Assertions.assertEquals(entities.get(1).getExternalId(), response.getExternalId());
        Assertions.assertEquals(entities.get(1).getParentDescription(), response.getParentDescription());
        Assertions.assertEquals(entities.get(1).getRea(), response.getRea());
        Assertions.assertEquals(entities.get(1).getZipCode(), response.getZipCode());
        Assertions.assertEquals(entities.get(1).getOnboarding().size(), response.getOnboarding().size());
    }

    @Test
    void findByExternalIdTest() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId("507f1f77bcf86cd799439011");
        when(institutionRepository.find(any(), any())).thenReturn(new ArrayList<>());
        Optional<Institution> response = institutionConnectorImpl.findByExternalId("ext");
        Assertions.assertFalse(response.isPresent());
    }

    @Test
    void findByExternalIdNotFoundTest() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId("507f1f77bcf86cd799439011");
        Optional<Institution> response = institutionConnectorImpl.findByExternalId("ext");
        Assertions.assertTrue(response.isEmpty());
    }

    @Test
    void deleteById() {
        doNothing().when(institutionRepository).deleteById(any());
        Assertions.assertDoesNotThrow(() -> institutionConnectorImpl.deleteById("507f1f77bcf86cd799439011"));
    }

    @Test
    void testFindById() {
        when(institutionRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> institutionConnectorImpl.findById("42"));
    }

    @Test
    void testFindById2() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        List<OnboardingEntity> onboardings = new ArrayList<>();
        onboardings.add(new OnboardingEntity());
        institutionEntity.setAttributes(new ArrayList<>());
        institutionEntity.setOnboarding(onboardings);
        institutionEntity.setGeographicTaxonomies(new ArrayList<>());
        institutionEntity.setPaymentServiceProvider(new PaymentServiceProviderEntity());
        institutionEntity.setDataProtectionOfficer(new DataProtectionOfficerEntity());
        when(institutionRepository.findById(any())).thenReturn(Optional.of(institutionEntity));
        assertNotNull(institutionConnectorImpl.findById("id"));
    }

    @Test
    void testFindAndUpdate() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        List<OnboardingEntity> onboardings = new ArrayList<>();
        onboardings.add(new OnboardingEntity());
        institutionEntity.setAttributes(new ArrayList<>());
        institutionEntity.setOnboarding(onboardings);
        institutionEntity.setGeographicTaxonomies(new ArrayList<>());
        institutionEntity.setPaymentServiceProvider(new PaymentServiceProviderEntity());
        institutionEntity.setDataProtectionOfficer(new DataProtectionOfficerEntity());
        when(institutionRepository.findAndModify(any(), any(), any(), any())).thenReturn(institutionEntity);
        List<InstitutionGeographicTaxonomies> geographicTaxonomies = new ArrayList<>();
        InstitutionGeographicTaxonomies geographicTaxonomies1 = new InstitutionGeographicTaxonomies();
        geographicTaxonomies1.setCode("code");
        geographicTaxonomies1.setDesc("desc");
        geographicTaxonomies.add(geographicTaxonomies1);
        Institution response = institutionConnectorImpl.findAndUpdate("institutionId", geographicTaxonomies, new InstitutionUpdate());
        assertNotNull(response);
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findByGeotaxonomies(List, SearchMode)}
     */
    @Test
    void testFindByGeotaxonomies() {
        when(institutionRepository.find(org.mockito.Mockito.any(), org.mockito.Mockito.any())).thenReturn(new ArrayList<>());
        assertTrue(institutionConnectorImpl.findByGeotaxonomies(new ArrayList<>(), SearchMode.ALL).isEmpty());
        verify(institutionRepository).find(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findByGeotaxonomies(List, SearchMode)}
     */
    @Test
    void testFindByGeotaxonomies2() {
        InstitutionEntity institutionEntity = TestUtils.createSimpleInstitutionEntity();

        ArrayList<InstitutionEntity> institutionEntityList = new ArrayList<>();
        institutionEntityList.add(institutionEntity);
        when(institutionRepository.find(org.mockito.Mockito.any(),
                (Class<InstitutionEntity>) org.mockito.Mockito.any())).thenReturn(institutionEntityList);
        assertEquals(1, institutionConnectorImpl.findByGeotaxonomies(new ArrayList<>(), SearchMode.ALL).size());
        verify(institutionRepository).find(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findByGeotaxonomies(List, SearchMode)}
     */
    @Test
    void testFindByGeotaxonomies4() {
        when(institutionRepository.find(org.mockito.Mockito.any(),org.mockito.Mockito.any())).thenReturn(new ArrayList<>());
        assertTrue(institutionConnectorImpl.findByGeotaxonomies(new ArrayList<>(), SearchMode.ANY).isEmpty());
        verify(institutionRepository).find(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findByGeotaxonomies(List, SearchMode)}
     */
    @Test
    void testFindByGeotaxonomies5() {
        when(institutionRepository.find(org.mockito.Mockito.any(), org.mockito.Mockito.any())).thenReturn(new ArrayList<>());
        assertTrue(institutionConnectorImpl.findByGeotaxonomies(new ArrayList<>(), SearchMode.EXACT).isEmpty());
        verify(institutionRepository).find(org.mockito.Mockito.any(),org.mockito.Mockito.any());
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findByGeotaxonomies(List, SearchMode)}
     */
    @Test
    void testFindByGeotaxonomies6() {
        when(institutionRepository.find(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenThrow(new InvalidRequestException("An error occurred", "Code"));
        List<String> list = new ArrayList<>();
        assertThrows(InvalidRequestException.class,
                () -> institutionConnectorImpl.findByGeotaxonomies(list, SearchMode.ALL));
        verify(institutionRepository).find(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findByProductId}
     */
    @Test
    void testFindByProductId() {
        when(institutionRepository.find(org.mockito.Mockito.any(),org.mockito.Mockito.any())).thenReturn(new ArrayList<>());
        assertTrue(institutionConnectorImpl.findByProductId("42").isEmpty());
        verify(institutionRepository).find(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findByProductId}
     */
    @Test
    void testFindByProductId2() {
        InstitutionEntity institutionEntity = TestUtils.createSimpleInstitutionEntity();

        ArrayList<InstitutionEntity> institutionEntityList = new ArrayList<>();
        institutionEntityList.add(institutionEntity);
        when(institutionRepository.find(org.mockito.Mockito.any(),
                (Class<InstitutionEntity>) org.mockito.Mockito.any())).thenReturn(institutionEntityList);
        assertEquals(1, institutionConnectorImpl.findByProductId("42").size());
        verify(institutionRepository).find(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findByProductId}
     */
    @Test
    void testFindByProductId4() {
        when(institutionRepository.find(org.mockito.Mockito.any(),
                (Class<InstitutionEntity>) org.mockito.Mockito.any()))
                .thenThrow(new InvalidRequestException("An error occurred", "."));
        assertThrows(InvalidRequestException.class, () -> institutionConnectorImpl.findByProductId("42"));
        verify(institutionRepository).find(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findAllByIds(List)}
     */
    @Test
    void testFindAllByIds3() {
        when(institutionRepository.findAllById(org.mockito.Mockito.any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        List<String> ids = new ArrayList<>();
        assertThrows(ResourceNotFoundException.class, () -> institutionConnectorImpl.findAllByIds(ids));
        verify(institutionRepository).findAllById(org.mockito.Mockito.any());
    }

    @Test
    void testFindAllByIds2() {
        when(institutionRepository.findAllById(org.mockito.Mockito.any())).thenReturn(List.of(new InstitutionEntity()));
        List<String> ids = new ArrayList<>();
        assertDoesNotThrow(() -> institutionConnectorImpl.findAllByIds(ids));
        verify(institutionRepository).findAllById(org.mockito.Mockito.any());
    }

    @Test
    void testFindWithFilter() {
        when(institutionRepository.find(any(), any())).thenReturn(new ArrayList<>());
        List<Institution> response = institutionConnectorImpl.findWithFilter("externalId", "productId", new ArrayList<>());
        assertNotNull(response);
    }

    @Test
    void testFindInstitutionProduct() {
        when(institutionRepository.find(any(), any())).thenReturn(new ArrayList<>());
        assertThrows(ResourceNotFoundException.class, () -> institutionConnectorImpl.findByExternalIdAndProductId("externalId", "productId"));
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findByExternalIdsAndProductId(List, String)}
     */
    @Test
    void testFindByExternalIdAndProductId() {
        when(institutionRepository.find(org.mockito.Mockito.any(),org.mockito.Mockito.any())).thenReturn(new ArrayList<>());
        assertTrue(institutionConnectorImpl.findByExternalIdsAndProductId(new ArrayList<>(), "42").isEmpty());
        verify(institutionRepository).find( org.mockito.Mockito.any(),org.mockito.Mockito.any());
    }

    /**
     * Method under test: {@link InstitutionConnectorImpl#findByExternalIdsAndProductId(List, String)}
     */
    @Test
    void testFindByExternalIdAndProductId2() {
        when(institutionRepository.find( org.mockito.Mockito.any(),org.mockito.Mockito.any()))
                .thenThrow(new InvalidRequestException("An error occurred", "."));
        List<ValidInstitution> list = new ArrayList<>();
        assertThrows(InvalidRequestException.class,
                () -> institutionConnectorImpl.findByExternalIdsAndProductId(list, "42"));
        verify(institutionRepository).find(org.mockito.Mockito.any(),org.mockito.Mockito.any());
    }

    @Test
    void findAndUpdateStatus() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        List<OnboardingEntity> onboardings = new ArrayList<>();
        onboardings.add(new OnboardingEntity());
        institutionEntity.setAttributes(new ArrayList<>());
        institutionEntity.setOnboarding(onboardings);
        institutionEntity.setGeographicTaxonomies(new ArrayList<>());
        institutionEntity.setPaymentServiceProvider(new PaymentServiceProviderEntity());
        institutionEntity.setDataProtectionOfficer(new DataProtectionOfficerEntity());
        when(institutionRepository.findAndModify(any(), any(), any(), any())).thenReturn(institutionEntity);
        institutionConnectorImpl.findAndUpdateStatus("institutionId", "tokenId", RelationshipState.ACTIVE);
        assertNotNull(institutionEntity);
    }

    @Test
    void findAndUpdateStatus2() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        List<OnboardingEntity> onboardings = new ArrayList<>();
        onboardings.add(new OnboardingEntity());
        institutionEntity.setAttributes(new ArrayList<>());
        institutionEntity.setOnboarding(onboardings);
        institutionEntity.setGeographicTaxonomies(new ArrayList<>());
        institutionEntity.setPaymentServiceProvider(new PaymentServiceProviderEntity());
        institutionEntity.setDataProtectionOfficer(new DataProtectionOfficerEntity());
        when(institutionRepository.findAndModify(any(), any(), any(), any())).thenReturn(institutionEntity);
        institutionConnectorImpl.findAndUpdateStatus("institutionId", "tokenId", RelationshipState.DELETED);
        assertNotNull(institutionEntity);
    }

    @Test
    void findAndRemoveOnboarding() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        List<OnboardingEntity> onboardings = new ArrayList<>();
        onboardings.add(new OnboardingEntity());
        institutionEntity.setAttributes(new ArrayList<>());
        institutionEntity.setOnboarding(onboardings);
        institutionEntity.setGeographicTaxonomies(new ArrayList<>());
        institutionEntity.setPaymentServiceProvider(new PaymentServiceProviderEntity());
        institutionEntity.setDataProtectionOfficer(new DataProtectionOfficerEntity());
        when(institutionRepository.findAndModify(any(), any(), any(), any())).thenReturn(institutionEntity);
        institutionConnectorImpl.findAndRemoveOnboarding("institutionId", new Onboarding());
        assertNotNull(institutionEntity);
    }

    @Test
    void updateOnboardedProductCreatedAt() {
        // Given
        String institutionIdMock = "InstitutionIdMock";
        String productIdMock = "ProductIdMock";
        OffsetDateTime createdAt = OffsetDateTime.parse("2020-11-01T02:15:30+01:00");
        InstitutionEntity updatedInstitutionMock = mockInstance(new InstitutionEntity());
        OnboardingEntity onboardingEntityMock = mockInstance(new OnboardingEntity());
        onboardingEntityMock.setProductId(productIdMock);
        updatedInstitutionMock.setOnboarding(List.of(onboardingEntityMock));
        updatedInstitutionMock.setId(institutionIdMock);
        updatedInstitutionMock.setOrigin(Origin.IPA);
        when(institutionRepository.findAndModify(any(), any(), any(), any()))
                .thenReturn(updatedInstitutionMock);
        // When
        Institution result = institutionConnectorImpl.updateOnboardedProductCreatedAt(institutionIdMock, productIdMock, createdAt);
        // Then
        assertNotNull(result);
        assertEquals(result.getId(), institutionIdMock);
        verify(institutionRepository, times(2))
                .findAndModify(queryArgumentCaptor.capture(), updateArgumentCaptor.capture(), findAndModifyOptionsArgumentCaptor.capture(), Mockito.eq(InstitutionEntity.class));
        List<Query> capturedQuery = queryArgumentCaptor.getAllValues();
        assertEquals(2, capturedQuery.size());
        assertSame(capturedQuery.get(0).getQueryObject().get(InstitutionEntity.Fields.id.name()), institutionIdMock);
        assertSame(capturedQuery.get(1).getQueryObject().get(InstitutionEntity.Fields.id.name()), institutionIdMock);
        assertEquals(2, updateArgumentCaptor.getAllValues().size());
        Update updateOnboardedProduct = updateArgumentCaptor.getAllValues().get(0);
        Update updateInstitutionEntityUpdatedAt = updateArgumentCaptor.getAllValues().get(1);
        assertEquals(1, updateOnboardedProduct.getArrayFilters().size());
        assertTrue(updateInstitutionEntityUpdatedAt.getArrayFilters().isEmpty());
        assertTrue(updateInstitutionEntityUpdatedAt.getUpdateObject().get("$set").toString().contains(InstitutionEntity.Fields.updatedAt.name()));
        assertTrue(updateOnboardedProduct.getUpdateObject().get("$set").toString().contains("onboarding.$[current].createdAt") &&
                updateOnboardedProduct.getUpdateObject().get("$set").toString().contains("onboarding.$[current].updatedAt") &&
                updateOnboardedProduct.getUpdateObject().get("$set").toString().contains(createdAt.toString()));
        verifyNoMoreInteractions(institutionRepository);
    }

    @Test
    void shouldFindOnboardingByIdAndProductId() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setOnboarding(List.of(new OnboardingEntity()));
        when(institutionRepository.findByInstitutionIdAndOnboardingProductId(anyString(), anyString()))
                .thenReturn(institutionEntity);

        List<Onboarding> onboardings = institutionConnectorImpl
                .findOnboardingByIdAndProductId("example", "example");

        assertFalse(onboardings.isEmpty());
    }

    @Test
    void shouldFindOnboardingByIdAndProductIdIfProductIsNull() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setOnboarding(List.of(new OnboardingEntity()));
        when(institutionRepository.findById(anyString()))
                .thenReturn(Optional.of(institutionEntity));

        List<Onboarding> onboardings = institutionConnectorImpl
                .findOnboardingByIdAndProductId("example", null);

        assertFalse(onboardings.isEmpty());
    }

    @Test
    void shouldFindByTaxCodeAndSubunitCode() {
        InstitutionEntity institutionEntity = new InstitutionEntity();

        when(institutionRepository.find(any(), eq(InstitutionEntity.class)))
                .thenReturn(List.of(institutionEntity));

        List<Institution> onboardings = institutionConnectorImpl
                .findByTaxCodeAndSubunitCode("example", "example", "example");

        assertFalse(onboardings.isEmpty());
    }

    @Test
    void shouldExistsByTaxCodeAndSubunitCodeAndProductAndStatusList() {

        when(institutionRepository.exists(any(), any())).thenReturn(Boolean.TRUE);

        Boolean exists = institutionConnectorImpl.existsByTaxCodeAndSubunitCodeAndProductAndStatusList("example",
                "example", Optional.of("example"), List.of());

        assertTrue(exists);
    }

    @Test
    void testRetrieveByProduct() {
        String productId = "productId";
        Integer pageNumber = 0;
        Integer sizeNumber = 5;

        BillingEntity billingEntity = createSimpleBillingEntity();
        DataProtectionOfficerEntity dataProtectionOfficerEntity = createSimpleDPOEntity();
        PaymentServiceProviderEntity paymentServiceProviderEntity = createSimpleServiceProviderEntity();
        OnboardingEntity onboardingEntity = createSimpleOnboardingEntity(productId, billingEntity);
        InstitutionEntity institutionEntity = createSimpleInstitutionEntity(billingEntity, dataProtectionOfficerEntity, onboardingEntity, paymentServiceProviderEntity);

        BillingEntity billingEntity1 = createSimpleBillingEntity();
        DataProtectionOfficerEntity dataProtectionOfficerEntity1 = createSimpleDPOEntity();
        PaymentServiceProviderEntity paymentServiceProviderEntity1 = createSimpleServiceProviderEntity();
        OnboardingEntity onboardingEntity1 = createSimpleOnboardingEntity(productId, billingEntity1);
        InstitutionEntity institutionEntity1 = createSimpleInstitutionEntity(billingEntity1, dataProtectionOfficerEntity1, onboardingEntity1, paymentServiceProviderEntity1);


        List<InstitutionEntity> institutionEntityList = new ArrayList<>();
        institutionEntityList.add(institutionEntity1);
        institutionEntityList.add(institutionEntity);
        Page<InstitutionEntity> institutionEntityPage = new PageImpl<>(institutionEntityList);

        doReturn(institutionEntityPage)
                .when(institutionRepository)
                .find(any(), any(), any());

        // When
        List<Institution> institutionsResult = institutionConnectorImpl.findInstitutionsByProductId(productId,
                pageNumber, sizeNumber);
        // Then
        assertNotNull(institutionsResult);
        assertEquals(2, institutionsResult.size());
        verify(institutionRepository, times(1))
                .find(queryArgumentCaptor.capture(), pageableArgumentCaptor.capture(), Mockito.eq(InstitutionEntity.class));

        Pageable capturedPage = pageableArgumentCaptor.getValue();
        assertEquals(pageNumber, capturedPage.getPageNumber());
        verifyNoMoreInteractions(institutionRepository);
    }

    @Test
    void findBrokers() {

        final String productId = "productId";

        BillingEntity billingEntity = createSimpleBillingEntity();
        DataProtectionOfficerEntity dataProtectionOfficerEntity = createSimpleDPOEntity();
        PaymentServiceProviderEntity paymentServiceProviderEntity = createSimpleServiceProviderEntity();
        OnboardingEntity onboardingEntity = createSimpleOnboardingEntity(productId, billingEntity);
        InstitutionEntity institutionEntity = createSimpleInstitutionEntity(billingEntity, dataProtectionOfficerEntity, onboardingEntity, paymentServiceProviderEntity);

        BillingEntity billingEntity1 = createSimpleBillingEntity();
        DataProtectionOfficerEntity dataProtectionOfficerEntity1 = createSimpleDPOEntity();
        PaymentServiceProviderEntity paymentServiceProviderEntity1 = createSimpleServiceProviderEntity();
        OnboardingEntity onboardingEntity1 = createSimpleOnboardingEntity(productId, billingEntity1);
        InstitutionEntity institutionEntity1 =createSimpleInstitutionEntity(billingEntity1, dataProtectionOfficerEntity1, onboardingEntity1, paymentServiceProviderEntity1);

        List<InstitutionEntity> institutionEntityList = List.of(institutionEntity, institutionEntity1);

        doReturn(institutionEntityList)
                .when(institutionRepository)
                .find(any(), any());

        // When
        List<Institution> institutionsResult = institutionConnectorImpl.findBrokers(productId, InstitutionType.PT);
        // Then
        assertNotNull(institutionsResult);
        assertEquals(2, institutionsResult.size());
        verify(institutionRepository, times(1))
                .find(queryArgumentCaptor.capture(), Mockito.eq(InstitutionEntity.class));
        verifyNoMoreInteractions(institutionRepository);
    }

    private BillingEntity createSimpleBillingEntity() {
        BillingEntity billingEntity = new BillingEntity();
        billingEntity.setPublicServices(true);
        billingEntity.setRecipientCode("Recipient Code");
        billingEntity.setVatNumber("42");
        return billingEntity;
    }

    private DataProtectionOfficerEntity createSimpleDPOEntity() {
        DataProtectionOfficerEntity dataProtectionOfficerEntity = new DataProtectionOfficerEntity();
        dataProtectionOfficerEntity.setAddress("42 Main St");
        dataProtectionOfficerEntity.setEmail("jane.doe@example.org");
        dataProtectionOfficerEntity.setPec("Pec");
        return dataProtectionOfficerEntity;
    }

    private PaymentServiceProviderEntity createSimpleServiceProviderEntity() {
        PaymentServiceProviderEntity paymentServiceProviderEntity = new PaymentServiceProviderEntity();
        paymentServiceProviderEntity.setAbiCode("Abi Code");
        paymentServiceProviderEntity.setBusinessRegisterNumber("42");
        paymentServiceProviderEntity.setLegalRegisterName("Legal Register Name");
        paymentServiceProviderEntity.setLegalRegisterNumber("42");
        paymentServiceProviderEntity.setVatNumberGroup(true);
        return paymentServiceProviderEntity;
    }

    private OnboardingEntity createSimpleOnboardingEntity(String productId, BillingEntity billingEntity) {
        OnboardingEntity onboardingEntity = new OnboardingEntity();
        onboardingEntity.setBilling(billingEntity);
        onboardingEntity.setClosedAt(null);
        onboardingEntity.setContract("Contract");
        onboardingEntity.setCreatedAt(null);
        onboardingEntity.setPricingPlan("Pricing Plan");
        onboardingEntity.setProductId(productId);
        onboardingEntity.setStatus(RelationshipState.PENDING);
        onboardingEntity.setTokenId("42");
        onboardingEntity.setUpdatedAt(null);
        return onboardingEntity;
    }

    private InstitutionEntity createSimpleInstitutionEntity(BillingEntity billingEntity,
                                                            DataProtectionOfficerEntity dpoEntity,
                                                            OnboardingEntity onboardingEntity,
                                                            PaymentServiceProviderEntity serviceProviderEntity) {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setAddress("42 Main St");
        institutionEntity.setAttributes(new ArrayList<>());
        institutionEntity.setBilling(billingEntity);
        institutionEntity.setBusinessRegisterPlace("Business Register Place");
        institutionEntity.setCreatedAt(null);
        institutionEntity.setDataProtectionOfficer(dpoEntity);
        institutionEntity.setDescription("The characteristics of someone or something");
        institutionEntity.setDigitalAddress("42 Main St");
        institutionEntity.setExternalId("42");
        institutionEntity.setGeographicTaxonomies(new ArrayList<>());
        institutionEntity.setId("42");
        institutionEntity.setImported(true);
        institutionEntity.setOnboarding(List.of(onboardingEntity));
        institutionEntity.setOrigin(Origin.MOCK);
        institutionEntity.setOriginId("42");
        institutionEntity.setPaymentServiceProvider(serviceProviderEntity);
        institutionEntity.setRea("Rea");
        institutionEntity.setShareCapital("Share Capital");
        institutionEntity.setSupportEmail("jane.doe@example.org");
        institutionEntity.setSupportPhone("6625550144");
        institutionEntity.setTaxCode("Tax Code");
        institutionEntity.setUpdatedAt(null);
        institutionEntity.setZipCode("21654");
        return institutionEntity;
    }

    @Test
    void findByOriginOriginId() {
        InstitutionEntity institutionEntity = new InstitutionEntity();

        when(institutionRepository.find(any(), any()))
                .thenReturn(List.of(institutionEntity));

        List<Institution> onboardings = institutionConnectorImpl
                .findByOriginAndOriginId("example", "example", "example");

        assertFalse(onboardings.isEmpty());
    }

    @Test
    @DisplayName("Should return true when onboarding exists by filters")
    void shouldReturnTrueWhenOnboardingExistsByFilters() {
        // Given
        VerifyOnboardingFilters filters = new VerifyOnboardingFilters("productId", "externalId", "taxCode", "origin", "originId", "subunitCode");
        filters.setValidRelationshipStates(List.of(RelationshipState.ACTIVE));
        when(institutionRepository.exists(any(Query.class), any())).thenReturn(true);

        // When
        Boolean exists = institutionConnectorImpl.existsOnboardingByFilters(filters);

        // Then
        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return true when onboarding exists by filters when subunitCode is null")
    void shouldReturnTrueWhenOnboardingExistsByFiltersAndSubunitCodeIsNull() {
        // Given
        VerifyOnboardingFilters filters = new VerifyOnboardingFilters("productId", "externalId", "taxCode", "origin", "originId", null);
        filters.setValidRelationshipStates(List.of(RelationshipState.ACTIVE));
        when(institutionRepository.exists(any(Query.class), any())).thenReturn(true);

        // When
        Boolean exists = institutionConnectorImpl.existsOnboardingByFilters(filters);

        // Then
        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when onboarding exists by filters when subunitCode is null")
    void shouldReturnFalseWhenOnboardingExistsByFiltersAndSubunitCodeIsNull() {
        // Given
        VerifyOnboardingFilters filters = new VerifyOnboardingFilters("productId", "externalId", "taxCode", "origin", "originId", null);
        filters.setValidRelationshipStates(List.of(RelationshipState.ACTIVE));
        when(institutionRepository.exists(any(Query.class), any())).thenReturn(false);

        // When
        Boolean exists = institutionConnectorImpl.existsOnboardingByFilters(filters);

        // Then
        assertFalse(exists);
    }

    @Test
    @DisplayName("Should return false when onboarding does not exist by filters")
    void shouldReturnFalseWhenOnboardingDoesNotExistByFilters() {
        // Given
        VerifyOnboardingFilters filters = new VerifyOnboardingFilters("productId", "externalId", "taxCode", "origin", "originId", "subunitCode");
        filters.setValidRelationshipStates(List.of(RelationshipState.ACTIVE));

        when(institutionRepository.exists(any(Query.class), any())).thenReturn(false);

        // When
        Boolean exists = institutionConnectorImpl.existsOnboardingByFilters(filters);

        // Then
        assertFalse(exists);
    }
    @Test
    public void testFindAndDeleteOnboarding() {

        String institutionId = UUID.randomUUID().toString();
        String productId = "prod-io";

        OnboardingEntity mockOnboarding = new OnboardingEntity();
        mockOnboarding.setProductId(productId);
        mockOnboarding.setUpdatedAt(OffsetDateTime.now());
        mockOnboarding.setStatus(RelationshipState.ACTIVE);

        InstitutionEntity mockEntity = new InstitutionEntity();
        mockEntity.setId(institutionId);
        mockEntity.setOnboarding(Collections.singletonList(mockOnboarding));

        when(institutionRepository.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(InstitutionEntity.class)))
                .thenReturn(mockEntity);

        institutionConnectorImpl.findAndDeleteOnboarding(institutionId, productId);

        verify(institutionRepository, times(1)).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(InstitutionEntity.class));
        assertNotNull(mockEntity);
    }
}