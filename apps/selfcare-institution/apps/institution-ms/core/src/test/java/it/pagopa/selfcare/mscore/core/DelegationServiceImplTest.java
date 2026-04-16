package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.api.DelegationConnector;
import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.constant.Order;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapper;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.exception.ResourceConflictException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.delegation.*;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static it.pagopa.selfcare.mscore.constant.GenericError.*;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DelegationServiceImplTest {
    @Mock
    private DelegationConnector delegationConnector;

    @InjectMocks
    private DelegationServiceImpl delegationServiceImpl;

    @Mock
    private MailNotificationService mailNotificationService;

    @Mock
    private InstitutionService institutionService;

    @Spy
    private InstitutionMapper delegationMapper = new InstitutionMapperImpl();

    private Delegation createDelegationTaxCode() {
        Delegation dummyDelegationTaxCode = new Delegation();
        dummyDelegationTaxCode.setToTaxCode("taxCodeTo");
        dummyDelegationTaxCode.setFromTaxCode("taxCodeFrom");
        dummyDelegationTaxCode.setProductId("prod-pagopa");
        dummyDelegationTaxCode.setId("id");
        return dummyDelegationTaxCode;
    }

    private Delegation createDelegationProdPagoPa() {
        Delegation dummyDelegationProdPagopa = new Delegation();
        dummyDelegationProdPagopa.setId("id");
        dummyDelegationProdPagopa.setProductId("prod-pagopa");
        dummyDelegationProdPagopa.setTo("taxCodeTo");
        dummyDelegationProdPagopa.setFrom("from");
        dummyDelegationProdPagopa.setType(DelegationType.EA);
        return dummyDelegationProdPagopa;
    }

    private Delegation createDelegationProdIo() {
        Delegation dummyDelegationProdIo = new Delegation();
        dummyDelegationProdIo.setId("id");
        dummyDelegationProdIo.setTo("to");
        dummyDelegationProdIo.setFrom("from");
        dummyDelegationProdIo.setProductId("prod-io");
        dummyDelegationProdIo.setType(DelegationType.PT);
        return dummyDelegationProdIo;
    }

    /**
     * Method under test: {@link DelegationServiceImpl#createDelegation(Delegation)}
     */
    @Test
    void testCreateDelegationWithProductProdIo() {
        Institution institutionTo = new Institution();
        Delegation dummyDelegationProdIo = createDelegationProdIo();
        institutionTo.setId("idTo");
        institutionTo.setTaxCode("taxCodeTo");
        Institution institutionFrom = new Institution();
        institutionTo.setId("idFrom");
        institutionTo.setTaxCode("taxCodeFrom");
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(String.valueOf(PROD_IO));
        onboarding.setInstitutionType(InstitutionType.PA);
        institutionTo.setOnboarding(List.of(onboarding));
        institutionFrom.setOnboarding(List.of(onboarding));
        when(delegationConnector.save(dummyDelegationProdIo)).thenAnswer(arg ->arg.getArguments()[0]);
        when(institutionService.retrieveInstitutionById(dummyDelegationProdIo.getFrom())).thenReturn(institutionFrom);
        when(institutionService.retrieveInstitutionById(dummyDelegationProdIo.getTo())).thenReturn(institutionTo);
        doNothing().when(mailNotificationService).sendMailForDelegation(any(), any(), any());
        doNothing().when(institutionService).updateInstitutionDelegation(any(),anyBoolean());
        Delegation response = delegationServiceImpl.createDelegation(dummyDelegationProdIo);
        verify(delegationConnector).save(dummyDelegationProdIo);
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(dummyDelegationProdIo.getId(), response.getId());
        assertEquals(institutionTo.getTaxCode(), response.getToTaxCode());
        assertEquals(institutionFrom.getTaxCode(), response.getFromTaxCode());
        assertEquals(institutionTo.getOnboarding().get(0).getInstitutionType(), response.getBrokerType());
        assertEquals(institutionFrom.getOnboarding().get(0).getInstitutionType(), response.getInstitutionType());
        assertNull(response.getUpdatedAt());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void testCreateDelegationWithParent() {
        Delegation dummyDelegationProdIo = createDelegationProdIo();
        Institution institutionTo = new Institution();
        institutionTo.setId("idTo");
        institutionTo.setTaxCode("taxCodeTo");
        Institution institutionFrom = new Institution();
        institutionTo.setId("idFrom");
        institutionTo.setTaxCode("taxCodeFrom");
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(String.valueOf(PROD_IO));
        onboarding.setInstitutionType(InstitutionType.PA);
        institutionTo.setOnboarding(List.of(onboarding));
        institutionFrom.setOnboarding(List.of(onboarding));
        dummyDelegationProdIo.setInstitutionFromRootName("test parent");
        when(delegationConnector.save(dummyDelegationProdIo)).thenAnswer(arg ->arg.getArguments()[0]);
        when(institutionService.retrieveInstitutionById(dummyDelegationProdIo.getFrom())).thenReturn(institutionFrom);
        when(institutionService.retrieveInstitutionById(dummyDelegationProdIo.getTo())).thenReturn(institutionTo);
        doNothing().when(mailNotificationService).sendMailForDelegation(any(), any(), any());
        doNothing().when(institutionService).updateInstitutionDelegation(any(),anyBoolean());
        Delegation response = delegationServiceImpl.createDelegation(dummyDelegationProdIo);
        verify(delegationConnector).save(dummyDelegationProdIo);
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(dummyDelegationProdIo.getId(), response.getId());
        assertEquals(institutionTo.getTaxCode(), response.getToTaxCode());
        assertEquals(institutionFrom.getTaxCode(), response.getFromTaxCode());
        assertEquals(institutionTo.getOnboarding().get(0).getInstitutionType(), response.getBrokerType());
        assertEquals(institutionFrom.getOnboarding().get(0).getInstitutionType(), response.getInstitutionType());
        assertEquals("test parent", response.getInstitutionFromRootName());
        assertNull(response.getUpdatedAt());
        assertNotNull(response.getCreatedAt());
    }

    /**
     * Method under test: {@link DelegationServiceImpl#createDelegation(Delegation)}
     */
    @Test
    void testCreateDelegationEA() {
        Delegation dummyDelegationProdPagopa = createDelegationProdPagoPa();
        Institution institutionTo = new Institution();
        institutionTo.setId("idTo");
        institutionTo.setTaxCode("taxCodeTo");
        Institution institutionFrom = new Institution();
        institutionTo.setId("idFrom");
        institutionTo.setTaxCode("taxCodeFrom");
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(String.valueOf(PROD_PAGOPA));
        onboarding.setInstitutionType(InstitutionType.PA);
        institutionTo.setOnboarding(List.of(onboarding));
        institutionFrom.setOnboarding(List.of(onboarding));
        when(delegationConnector.save(dummyDelegationProdPagopa)).thenAnswer(arg ->arg.getArguments()[0]);
        when(institutionService.retrieveInstitutionById(dummyDelegationProdPagopa.getFrom())).thenReturn(institutionFrom);
        when(institutionService.retrieveInstitutionById(dummyDelegationProdPagopa.getTo())).thenReturn(institutionTo);
        doNothing().when(institutionService).updateInstitutionDelegation(any(),anyBoolean());
        Delegation response = delegationServiceImpl.createDelegation(dummyDelegationProdPagopa);
        verify(delegationConnector).save(dummyDelegationProdPagopa);
        verifyNoInteractions(mailNotificationService);
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(dummyDelegationProdPagopa.getId(), response.getId());
        assertEquals(institutionTo.getTaxCode(), response.getToTaxCode());
        assertEquals(institutionFrom.getTaxCode(), response.getFromTaxCode());
        assertEquals(institutionTo.getOnboarding().get(0).getInstitutionType(), response.getBrokerType());
        assertEquals(institutionFrom.getOnboarding().get(0).getInstitutionType(), response.getInstitutionType());
        assertNull(response.getUpdatedAt());
        assertNotNull(response.getCreatedAt());
    }

    /**
     * Method under test: {@link DelegationServiceImpl#createDelegation(Delegation)}
     */
    @Test
    void testCreateDelegationWithSendMailError() {
        Institution institution = new Institution();
        institution.setId("id");
        when(institutionService.retrieveInstitutionById(any())).thenReturn(institution);
        doThrow(new MsCoreException(SEND_MAIL_FOR_DELEGATION_ERROR.getMessage(), SEND_MAIL_FOR_DELEGATION_ERROR.getCode()))
                .when(mailNotificationService)
                .sendMailForDelegation(any(), any(), any());
        assertDoesNotThrow(() -> delegationServiceImpl.createDelegation(createDelegationProdIo()));
        verify(mailNotificationService).sendMailForDelegation(any(), any(), any());
    }

    /**
     * Method under test: {@link DelegationServiceImpl#createDelegation(Delegation)}
     */
    @Test
    void testCreateDelegationWithError() {
        Institution institution = new Institution();
        institution.setId("id");
        when(institutionService.retrieveInstitutionById(any())).thenReturn(institution);
        when(delegationConnector.save(any())).thenThrow(new MsCoreException(CREATE_DELEGATION_ERROR.getMessage(), CREATE_DELEGATION_ERROR.getCode()));
        assertThrows(MsCoreException.class, () -> delegationServiceImpl.createDelegation(createDelegationProdIo()));
        verify(delegationConnector).save(any());
    }

    /**
     * Method under test: {@link DelegationServiceImpl#createDelegation(Delegation)}
     */
    @Test
    void testCreateDelegationWithConflict() {
        Delegation dummyDelegationProdIo = createDelegationProdIo();
        Institution institution = new Institution();
        institution.setId("id");
        when(institutionService.retrieveInstitutionById(dummyDelegationProdIo.getTo())).thenReturn(institution);
        when(institutionService.retrieveInstitutionById(dummyDelegationProdIo.getFrom())).thenReturn(institution);
        when(delegationServiceImpl.checkIfExistsWithStatus(dummyDelegationProdIo, DelegationState.ACTIVE)).thenReturn(true);
        assertThrows(ResourceConflictException.class, () -> delegationServiceImpl.createDelegation(dummyDelegationProdIo));
        verifyNoMoreInteractions(delegationConnector);
    }

    /**
     * Method under test: {@link DelegationService#checkIfExistsWithStatus(Delegation, DelegationState)}
     */
    @Test
    void testCheckIfExists() {
        Delegation delegation = new Delegation();
        when(delegationConnector.checkIfExistsWithStatus(any(), any())).thenReturn(true);
        boolean check = delegationServiceImpl.checkIfExistsWithStatus(delegation, DelegationState.ACTIVE);
        assertTrue(check);
    }

    /**
     * Method under test: {@link DelegationServiceImpl#getDelegations(String, String, String, String, String, Optional, Optional, Optional)}
     */
    @Test
    void find_shouldGetData() {
        //Given
        Delegation delegation = new Delegation();
        delegation.setId("id");
        when(delegationConnector.find(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(delegation));
        //When
        List<Delegation> response = delegationServiceImpl.getDelegations("from", "to", "productId", null,
                null, Optional.empty(), Optional.of(0), Optional.of(100));
        //Then
        verify(delegationConnector).find(any(), any(), any(), any(), any(), any(), any(), any());

        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertEquals(delegation.getId(), response.get(0).getId());
    }

    /**
     * Method under test: {@link DelegationServiceImpl#getDelegations(String, String, String, String, String, Optional, Optional, Optional)}
     */
    @Test
    void find_shouldGetData_fullMode() {
        //Given
        Delegation delegation = new Delegation();
        delegation.setId("id");
        when(delegationConnector.find(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(delegation));
        //When
        List<Delegation> response = delegationServiceImpl.getDelegations("from", null, "productId", null,
                null, Optional.of(Order.DESC), Optional.of(0), Optional.of(0));
        //Then
        verify(delegationConnector).find(any(), any(), any(), any(), any(), any(), any(), any());

        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertEquals(delegation.getId(), response.get(0).getId());
    }

    /**
     * Method under test: {@link DelegationServiceImpl#getDelegations(String, String, String, String, String, Optional, Optional, Optional)}
     */
    @Test
    void find_shouldGetData_fullMode_defaultPage() {
        //Given
        Delegation delegation = new Delegation();
        delegation.setId("id");
        when(delegationConnector.find(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(delegation));
        //When
        List<Delegation> response = delegationServiceImpl.getDelegations("from", null, "productId", null,
                null, Optional.of(Order.DESC), Optional.empty(), Optional.empty());
        //Then
        verify(delegationConnector).find(any(), any(), any(), any(), any(), any(), any(), any());

        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertEquals(delegation.getId(), response.get(0).getId());
    }

    /**
     * Method under test: {@link DelegationServiceImpl#getDelegationsV2(GetDelegationParameters)}
     */
    @Test
    void getDelegationsV2_shouldGetData() {
        //Given
        Delegation delegation = new Delegation();
        delegation.setId("id");
        DelegationWithPagination delegationWithPagination = new DelegationWithPagination(List.of(delegation), new PageInfo(10, 0, 10, 1));
        when(delegationConnector.findAndCount(any())).thenReturn(delegationWithPagination);
        GetDelegationParameters delegationParameters = createDelegationParameters("from", "to", "productId", null,
                null, null, 0, 100);

        //When
        DelegationWithPagination response = delegationServiceImpl.getDelegationsV2(delegationParameters);
        //Then
        ArgumentCaptor<GetDelegationParameters> argumentCaptor = ArgumentCaptor.forClass(GetDelegationParameters.class);
        verify(delegationConnector).findAndCount(argumentCaptor.capture());
        assertNotNull(argumentCaptor);
        assertEquals(argumentCaptor.getValue(), delegationParameters);

        assertNotNull(response);
        assertNotNull(response.getDelegations());
        assertNotNull(response.getPageInfo());
        assertFalse(response.getDelegations().isEmpty());
        assertEquals(delegation.getId(), response.getDelegations().get(0).getId());
    }

    @Test
    void testCreateDelegationFromTaxCode() {
        Delegation dummyDelegationTaxCode = createDelegationTaxCode();
        Institution institutionTo = new Institution();
        institutionTo.setId("id");
        institutionTo.setTaxCode("taxCodeTo");
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("prod-pagopa");
        onboarding.setInstitutionType(InstitutionType.GSP);
        institutionTo.setOnboarding(List.of(onboarding));
        when(delegationConnector.save(any())).thenReturn(dummyDelegationTaxCode);
        Institution institutionFrom = new Institution();
        institutionFrom.setId("id");
        institutionFrom.setTaxCode("taxCodeFrom");
        institutionFrom.setOnboarding(List.of(onboarding));
        when(institutionService.getInstitutions(dummyDelegationTaxCode.getToTaxCode(), dummyDelegationTaxCode.getToSubunitCode())).thenReturn(List.of(institutionTo));
        when(institutionService.getInstitutions(dummyDelegationTaxCode.getFromTaxCode(), dummyDelegationTaxCode.getFromSubunitCode())).thenReturn(List.of(institutionFrom));
        doNothing().when(institutionService).updateInstitutionDelegation(any(),anyBoolean());
        Delegation response = delegationServiceImpl.createDelegationFromInstitutionsTaxCode(dummyDelegationTaxCode);
        verify(delegationConnector).save(any());
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(dummyDelegationTaxCode.getId(), response.getId());
        assertEquals(institutionTo.getTaxCode(), response.getToTaxCode());
        assertEquals(institutionFrom.getTaxCode(), response.getFromTaxCode());
        assertEquals(onboarding.getInstitutionType(), response.getInstitutionType());
        assertEquals(onboarding.getInstitutionType(), response.getBrokerType());
        assertNull(response.getUpdatedAt());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void testCreateDelegationFromTaxCodeWithSubunitCode() {
        Delegation dummyDelegationTaxCode = createDelegationTaxCode();
        Institution institutionTo = new Institution();
        institutionTo.setId("id");
        institutionTo.setTaxCode("taxCodeTo");
        institutionTo.setIsTest(true);
        Institution institutionFrom = new Institution();
        institutionFrom.setId("id");
        institutionFrom.setTaxCode("taxCodeFrom");
        institutionFrom.setIsTest(false);
        when(delegationConnector.findAndActivate(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(dummyDelegationTaxCode);
        when(institutionService.getInstitutions(dummyDelegationTaxCode.getToTaxCode(), dummyDelegationTaxCode.getToSubunitCode())).thenReturn(List.of(institutionTo));
        when(institutionService.getInstitutions(dummyDelegationTaxCode.getFromTaxCode(), dummyDelegationTaxCode.getFromSubunitCode())).thenReturn(List.of(institutionFrom));
        doNothing().when(institutionService).updateInstitutionDelegation(any(),anyBoolean());
        when(delegationConnector.checkIfExistsWithStatus(dummyDelegationTaxCode, DelegationState.ACTIVE)).thenReturn(false);
        when(delegationConnector.checkIfExistsWithStatus(dummyDelegationTaxCode, DelegationState.DELETED)).thenReturn(true);
        Delegation response = delegationServiceImpl.createDelegationFromInstitutionsTaxCode(dummyDelegationTaxCode);
        verify(delegationConnector).findAndActivate(anyString(), anyString(), anyString(), anyBoolean());
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(dummyDelegationTaxCode.getId(), response.getId());
        assertEquals(dummyDelegationTaxCode.getFromTaxCode(), response.getFromTaxCode());
        assertEquals(dummyDelegationTaxCode.getToTaxCode(), response.getToTaxCode());
    }

    /**
     * Method under test: {@link DelegationServiceImpl#createDelegation(Delegation)}
     */
    @Test
    void testCreateDelegationFromTaxCodeWithError() {
        Delegation dummyDelegationTaxCode = createDelegationTaxCode();
        Institution institution = new Institution();
        institution.setId("id");
        when(institutionService.getInstitutions(dummyDelegationTaxCode.getToTaxCode(), null)).thenReturn(List.of(institution));
        when(institutionService.getInstitutions(dummyDelegationTaxCode.getFromTaxCode(), null)).thenReturn(List.of(institution));
        when(delegationConnector.save(any())).thenThrow(new MsCoreException(CREATE_DELEGATION_ERROR.getMessage(), CREATE_DELEGATION_ERROR.getCode()));
        assertThrows(MsCoreException.class, () -> delegationServiceImpl.createDelegationFromInstitutionsTaxCode(dummyDelegationTaxCode));
        verify(delegationConnector).save(any());
    }

    /**
     * Method under test: {@link DelegationServiceImpl#createDelegation(Delegation)}
     */
    @Test
    void testCreateDelegationFromTaxCodeWithResourceNotFoundException() {
        when(institutionService.getInstitutions(any(), any())).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> delegationServiceImpl.createDelegationFromInstitutionsTaxCode(createDelegationProdPagoPa()));
    }

    /**
     * Method under test: {@link DelegationServiceImpl#createDelegation(Delegation)}
     */
    @Test
    void testCreateDelegationFromTaxCodeConflict() {
        Delegation dummyDelegationProdPagopa = createDelegationProdPagoPa();

        Institution institution = new Institution();
        institution.setId("id");
        when(institutionService.getInstitutions(dummyDelegationProdPagopa.getToTaxCode(), dummyDelegationProdPagopa.getToSubunitCode())).thenReturn(List.of(institution));
        when(institutionService.getInstitutions(dummyDelegationProdPagopa.getFromTaxCode(), dummyDelegationProdPagopa.getFromSubunitCode())).thenReturn(List.of(institution));
        when(delegationConnector.checkIfExistsWithStatus(dummyDelegationProdPagopa, DelegationState.ACTIVE)).thenReturn(true);
        assertThrows(ResourceConflictException.class, () -> delegationServiceImpl.createDelegationFromInstitutionsTaxCode(dummyDelegationProdPagopa));
    }

    @Test
    void testDeleteDelegationByDelegationId_whenDelegationisNotActive() {
        Delegation delegation = new Delegation();
        delegation.setTo("id");
        delegation.setStatus(DelegationState.DELETED);
        when(delegationConnector.findByIdAndModifyStatus("id", DelegationState.DELETED)).thenReturn(delegation);
        when(delegationConnector.checkIfDelegationsAreActive("id")).thenReturn(false);
        Executable executable = () -> delegationServiceImpl.deleteDelegationByDelegationId("id");
        assertDoesNotThrow(executable);
        verify(institutionService).updateInstitutionDelegation("id", false);
    }

    @Test
    void testDeleteDelegationByDelegationId_whenDelegationisActive() {
        Delegation delegation = new Delegation();
        delegation.setTo("id");
        delegation.setStatus(DelegationState.DELETED);
        when(delegationConnector.findByIdAndModifyStatus("id", DelegationState.DELETED)).thenReturn(delegation);
        when(delegationConnector.checkIfDelegationsAreActive("id")).thenReturn(true);
        Executable executable = () -> delegationServiceImpl.deleteDelegationByDelegationId("id");
        assertDoesNotThrow(executable);
        verify(institutionService, times(0)).updateInstitutionDelegation("id", false);
    }

    @Test
    void testDeleteDelegationByDelegationId_whenFindAndModifyStatusThrowsException() {
        Delegation delegation = new Delegation();
        delegation.setTo("id");
        delegation.setStatus(DelegationState.DELETED);
        when(delegationConnector.findByIdAndModifyStatus("id", DelegationState.DELETED))
                .thenThrow(new MsCoreException(DELETE_DELEGATION_ERROR.getMessage(), DELETE_DELEGATION_ERROR.getCode()));
        assertThrows(MsCoreException.class, () -> delegationServiceImpl.deleteDelegationByDelegationId("id"));
        verify(delegationConnector, times(0)).checkIfDelegationsAreActive(any());
    }

    @Test
    void testDeleteDelegationByDelegationId_whenUpdateInstitutionDelegationThrowsException() {
        Delegation delegation = new Delegation();
        delegation.setTo("id");
        delegation.setStatus(DelegationState.DELETED);
        when(delegationConnector.findByIdAndModifyStatus("id", DelegationState.DELETED)).thenReturn(delegation);
        when(delegationConnector.checkIfDelegationsAreActive("id")).thenReturn(false);
        doThrow(new MsCoreException(DELETE_DELEGATION_ERROR.getMessage(), DELETE_DELEGATION_ERROR.getCode()))
                .when(institutionService).updateInstitutionDelegation("id", false);
        assertThrows(MsCoreException.class, () -> delegationServiceImpl.deleteDelegationByDelegationId("id"));
        verify(delegationConnector, times(1)).findByIdAndModifyStatus("id", DelegationState.ACTIVE);
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
    void getDelegatorsAndDelegatesTest() {
        final DelegationInstitution delInst = new DelegationInstitution();
        delInst.setId(0L);
        delInst.setDelegationId("456");
        final Institution inst = new Institution();
        inst.setId("456");
        delInst.setInstitution(inst);

        final DelegationInstitution delInstWithOnboarding = new DelegationInstitution();
        delInstWithOnboarding.setId(100L);
        delInstWithOnboarding.setDelegationId("789");
        delInstWithOnboarding.setDelegationProductId("prod-test");
        final Institution instWithOnboarding = new Institution();
        instWithOnboarding.setId("789");
        final Onboarding onboarding1 = new Onboarding();
        onboarding1.setProductId("prod-test-x");
        final Onboarding onboarding2 = new Onboarding();
        onboarding2.setProductId("prod-test");
        instWithOnboarding.setOnboarding(List.of(onboarding1, onboarding2));
        delInstWithOnboarding.setInstitution(instWithOnboarding);

        when(delegationConnector.findDelegators("institutionId", "productId", DelegationType.EA, 123L, 100)).thenReturn(List.of(delInst, delInstWithOnboarding));
        when(delegationConnector.findDelegates("institutionId", "productId", DelegationType.EA, 123L, 100)).thenReturn(List.of(delInst, delInstWithOnboarding));

        final List<DelegationInstitution> delegators = delegationServiceImpl.getDelegators("institutionId", "productId", DelegationType.EA, 123L, 100);
        final List<DelegationInstitution> delegates = delegationServiceImpl.getDelegates("institutionId", "productId", DelegationType.EA, 123L, 100);

        verify(delegationConnector, times(1)).findDelegators("institutionId", "productId", DelegationType.EA, 123L, 100);
        verify(delegationConnector, times(1)).findDelegates("institutionId", "productId", DelegationType.EA, 123L, 100);

        assertEquals(1, delegators.size());
        assertEquals(1, delegates.size());
        assertEquals(100L, delegators.get(0).getId());
        assertEquals(100L, delegates.get(0).getId());
        assertEquals("789", delegators.get(0).getDelegationId());
        assertEquals("789", delegates.get(0).getDelegationId());
        assertEquals("789", delegators.get(0).getInstitution().getId());
        assertEquals("789", delegates.get(0).getInstitution().getId());
        assertEquals(1, delegators.get(0).getInstitution().getOnboarding().size());
        assertEquals(1, delegates.get(0).getInstitution().getOnboarding().size());
        assertEquals("prod-test", delegators.get(0).getInstitution().getOnboarding().get(0).getProductId());
        assertEquals("prod-test", delegates.get(0).getInstitution().getOnboarding().get(0).getProductId());
    }

}