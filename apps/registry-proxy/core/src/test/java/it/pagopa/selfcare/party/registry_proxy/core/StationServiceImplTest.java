package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.DummyPDNDQueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station.Field;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.core.exception.TooManyResourceFoundException;
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
class StationServiceImplTest {

    @Mock
    private IndexSearchService<Station> indexSearchService;

    @InjectMocks
    private StationServiceImpl pdndService;

    @Test
    void search_emptySearchText() {
        // given
        final Optional<String> searchText = Optional.empty();
        final int page = 0;
        final int limit = 0;

        final DummyPDNDQueryResult queryResultMock = new DummyPDNDQueryResult();
        when(indexSearchService.findAll(anyInt(), anyInt(), anyString()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<Station> queryResult = pdndService.search(searchText, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .findAll(page, limit, Entity.STATION.toString());
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void search_notEmptySearchText() {
        // given
        final Optional<String> searchText = Optional.of("pippo");
        final int page = 0;
        final int limit = 0;

        final DummyPDNDQueryResult queryResultMock = new DummyPDNDQueryResult();
        when(indexSearchService.fullTextSearch(any(), anyString(), anyInt(), anyInt()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<Station> queryResult = pdndService.search(searchText, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .fullTextSearch(Field.DESCRIPTION, searchText.get(), page, limit);
        verifyNoMoreInteractions(indexSearchService);
    }


    @Test
    void findById_ResourceNotFound() {
        // given
        final String taxId = "taxId";
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of());
        // when
        final Executable executable = () -> pdndService.findByTaxId(taxId);
        // then
        assertThrows(ResourceNotFoundException.class, executable);
    }


    @Test
    void findById_TooManyResourceFound() {
        // given
        final String taxId = "taxId";
        final DummyPDND dummyPDND = new DummyPDND();
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(dummyPDND, dummyPDND));
        // when
        final Executable executable = () -> pdndService.findByTaxId(taxId);
        // then
        assertThrows(TooManyResourceFoundException.class, executable);
    }


    @Test
    void findById_found() {
        // given
        final String taxId = "taxId";
        final DummyPDND dummyPDND = new DummyPDND();
        dummyPDND.setId("id");
        dummyPDND.setTaxCode("taxId");
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(dummyPDND));
        // when
        final Station result = pdndService.findByTaxId(taxId);
        // then
        assertSame(dummyPDND, result);
    }
}