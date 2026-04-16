package it.pagopa.selfcare.party.registry_proxy.core;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.Origin.IPA;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.OpenDataRestClient;
import it.pagopa.selfcare.party.registry_proxy.core.exception.TooManyResourceFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AOOServiceImplTest {

    @Mock
    private IndexSearchService<AOO> indexSearchService;

    @Mock
    private InstitutionService institutionService;

    @Mock
    private OpenDataRestClient openDataRestClient;

    @InjectMocks
    private AOOServiceImpl aooService;

    @Mock
    private IndexWriterService<AOO> aooIndexWriterService;


    @Test
    void search_emptyOrigin() {
        // given
        final int page = 0;
        final int limit = 0;

        final DummyAOOQueryResult queryResultMock = new DummyAOOQueryResult();
        when(indexSearchService.findAll(anyInt(), anyInt(), anyString()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<AOO> queryResult = aooService.findAll(page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .findAll(page, limit, Entity.AOO.toString());
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void search_notEmptyOrigin() {
        // given
        final int page = 0;
        final int limit = 0;
        final DummyAOOQueryResult queryResultMock = new DummyAOOQueryResult();
        when(indexSearchService.findAll(anyInt(), anyInt(), any()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<AOO> queryResult = aooService.findAll(page, limit);
        // then
        assertSame(queryResultMock, queryResult);
    }

    @Test
    void findById_ipa() {
        // given
        final String id = "pippo";
        // when
        final Executable executable = () -> aooService.findByUnicode(id, new ArrayList<>());
        // then
        assertThrows(RuntimeException.class, executable);
    }


    @Test
    void findById_ResourceNotFound() {
        // given
        final String id = "pippo";
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of());
        // when
        final Executable executable = () -> aooService.findByUnicode(id, new ArrayList<>());
        // then
        assertThrows(ResourceNotFoundException.class, executable);
    }


    @Test
    void findById_TooManyResourceFound() {
        // given
        final String id = "pippo";
        final DummyAOO aoo = new DummyAOO();
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(aoo, aoo));
        // when
        final Executable executable = () -> aooService.findByUnicode(id, new ArrayList<>());
        // then
        assertThrows(TooManyResourceFoundException.class, executable);
    }


    @Test
    void findById_found() {
        // given
        final String id = "pippo";
        final DummyAOO AOO = new DummyAOO();
        AOO.setCodiceFiscaleEnte("cod");
        AOO.setOrigin(IPA);
        final Institution institution = new DummyInstitution();
        List<String> categories = new ArrayList<>();
        categories.add("L4");
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(AOO));
        when(institutionService.findById("cod", Optional.of(IPA), categories))
                .thenReturn(institution);

        // when
        final AOO result = aooService.findByUnicode(id, categories);
        // then
        assertSame(AOO, result);
    }

    @Test
    void updateAoosIndex() {
        final String response = "id,Codice_IPA,Denominazione_ente,Codice_fiscale_ente,Codice_uni_aoo,Descrizione_aoo,Mail1,Data_aggiornamento\n" +
                "id,Codice_IPA,Denominazione_ente,Codice_fiscale_ente,Codice_uni_aoo,Descrizione_aoo,Mail1,2024-01-01";
        when(openDataRestClient.retrieveAOOs()).thenReturn(response);
        Executable executable = () -> aooService.updateAOOsIndex();
        Assertions.assertDoesNotThrow(executable);
    }

}