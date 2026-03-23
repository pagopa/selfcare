package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.OpenDataRestClient;
import it.pagopa.selfcare.party.registry_proxy.core.exception.TooManyResourceFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UOServiceImplTest {

    @Mock
    private IndexSearchService<UO> indexSearchService;

    @Mock
    private InstitutionService institutionService;

    @Mock
    private OpenDataRestClient openDataRestClient;

    @InjectMocks
    private UOServiceImpl uoService;

    @Test
    void search_emptyOrigin() {
        // given
        final int page = 0;
        final int limit = 0;
        final Optional<String> taxCodeInvoicing = Optional.empty();

        final DummyUOQueryResult queryResultMock = new DummyUOQueryResult();
        when(indexSearchService.findAll(anyInt(), anyInt(), anyString()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<UO> queryResult = uoService.findAll(taxCodeInvoicing, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .findAll(page, limit, Entity.UO.toString());
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void search_notEmptyOrigin() {
        final int page = 0;
        final int limit = 0;
        final Optional<String> taxCodeInvoicing = Optional.of("pippo");
        final DummyUOQueryResult queryResultMock = new DummyUOQueryResult();
        when(indexSearchService.fullTextSearch(any(), anyString(), anyInt(), anyInt()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<UO> queryResult = uoService.findAll(taxCodeInvoicing, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
    }

    @Test
    void findById_ipa() {
        // given
        final String id = "pippo";
        // when
        final Executable executable = () -> uoService.findByUnicode(id, new ArrayList<>());
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
        final Executable executable = () -> uoService.findByUnicode(id, new ArrayList<>());
        // then
        assertThrows(ResourceNotFoundException.class, executable);
    }

    @Test
    void findById_TooManyResourceFound() {
        // given
        final String id = "pippo";
        final DummyUO UO = new DummyUO();
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(UO, UO));
        // when
        final Executable executable = () -> uoService.findByUnicode(id, new ArrayList<>());
        // then
        assertThrows(TooManyResourceFoundException.class, executable);
    }

    @Test
    void findById_found() {
        // given
        final String id = "pippo";
        final DummyUO UO = new DummyUO();
        UO.setCodiceFiscaleEnte("cod");
        UO.setOrigin(Origin.IPA);
        final Institution institution = new DummyInstitution();
        ArrayList<String> categories = new ArrayList<>();
        categories.add("L4");
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(UO));
        when(institutionService.findById("cod", Optional.of(Origin.IPA), categories))
                .thenReturn(institution);
        // when
        final UO result = uoService.findByUnicode(id, categories);
        // then
        assertSame(UO, result);
    }

    @Test
    void updateUosIndex() {
        final String response = "id,Codice_IPA,Denominazione_ente,Codice_fiscale_ente,Codice_fiscale_sfe,Codice_uni_uo,Codice_uni_uo_padre,Codice_uni_aoo,Descrizione_uo,Mail1,Data_aggiornamento\n" +
                "id,Codice_IPA,Denominazione_ente,Codice_fiscale_ente,Codice_fiscale_sfe,Codice_uni_uo,Codice_uni_uo_padre,Codice_uni_aoo,Descrizione_uo,Mail1,2024-01-01";
        when(openDataRestClient.retrieveUOs()).thenReturn(response);
        when(openDataRestClient.retrieveUOsWithSfe()).thenReturn(response);
        Executable executable = () -> uoService.updateUOsIndex();
        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    void updateUosIndexWithMalformedDate() {
        final String response = "id,Codice_IPA,Denominazione_ente,Codice_fiscale_ente,Codice_fiscale_sfe,Codice_uni_uo,Codice_uni_uo_padre,Codice_uni_aoo,Descrizione_uo,Mail1,Data_aggiornamento\n" +
                "id,Codice_IPA,Denominazione_ente,Codice_fiscale_ente,Codice_fiscale_sfe,Codice_uni_uo,Codice_uni_uo_padre,Codice_uni_aoo,Descrizione_uo,Mail1,2024-15-12";
        when(openDataRestClient.retrieveUOs()).thenReturn(response);
        when(openDataRestClient.retrieveUOsWithSfe()).thenReturn(response);
        Executable executable = () -> uoService.updateUOsIndex();
        Assertions.assertDoesNotThrow(executable);
    }

}