package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IvassDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.core.exception.TooManyResourceFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IvassServiceImplTest {

    @Mock
    private IndexSearchService<InsuranceCompany> indexSearchService;
    @Mock
    private IvassDataConnector ivassDataConnector;
    @Mock
    private IndexWriterService<InsuranceCompany> indexWriterService;
    @InjectMocks
    private IvassServiceImpl ivassService;

    @Test
    void findByOriginId_ResourceNotFound() {
        // given
        final String originId = "originId";
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of());
        // when
        final Executable executable = () -> ivassService.findByOriginId(originId);
        // then
        assertThrows(ResourceNotFoundException.class, executable);
    }

    @Test
    void findByOriginId_TooManyResourceFound() {
        // given
        final String originId = "originId";
        final DummyInsuranceCompany dummyCompany = new DummyInsuranceCompany();
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(dummyCompany, dummyCompany));
        // when
        final Executable executable = () -> ivassService.findByOriginId(originId);
        // then
        assertThrows(TooManyResourceFoundException.class, executable);
    }

    @Test
    void findByOriginId_found() {
        // given
        final String originId = "originId";
        final DummyInsuranceCompany dummyCompany = new DummyInsuranceCompany();
        dummyCompany.setId("id");
        dummyCompany.setOriginId("originId");
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(dummyCompany));
        // when
        final InsuranceCompany result = ivassService.findByOriginId(originId);
        // then
        assertSame(dummyCompany, result);
    }

    @Test
    void search_emptySearchText() {
        // given
        final Optional<String> searchText = Optional.empty();
        final int page = 0;
        final int limit = 0;

        final DummyInsuranceQueryResult queryResultMock = new DummyInsuranceQueryResult();
        when(indexSearchService.findAll(anyInt(), anyInt(), anyString()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<InsuranceCompany> queryResult = ivassService.search(searchText, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .findAll(page, limit, Entity.INSURANCE_COMPANY.toString());
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void search_notEmptySearchText() {
        // given
        final Optional<String> searchText = Optional.of("pippo");
        final int page = 0;
        final int limit = 0;

        final DummyInsuranceQueryResult queryResultMock = new DummyInsuranceQueryResult();
        when(indexSearchService.fullTextSearch(any(), anyString(), anyInt(), anyInt()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<InsuranceCompany> queryResult = ivassService.search(searchText, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .fullTextSearch(InsuranceCompany.Field.DESCRIPTION, searchText.get(), page, limit);
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void updateIvassIndex() {
        List<InsuranceCompany> companies = List.of(new DummyInsuranceCompany());
        when(ivassDataConnector.getInsurances()).thenReturn(companies);
        Executable executable = () -> ivassService.updateIvassIndex();
        Assertions.assertDoesNotThrow(executable);
    }
}