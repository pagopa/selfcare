package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution.Field;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.party.registry_proxy.connector.model.Origin.IPA;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceImplTest {

    @Mock
    private IndexSearchService<Institution> indexSearchService;

    @InjectMocks
    private InstitutionServiceImpl institutionService;

    @Mock
    private OpenDataRestClient openDataRestClient;

    @Mock
    private IndexWriterService<Institution> institutionIndexWriterService;


    @Test
    void search_emptySearchText() {
        // given
        final Optional<String> searchText = Optional.empty();
        final int page = 0;
        final int limit = 0;

        final DummyInstitutionQueryResult queryResultMock = new DummyInstitutionQueryResult();
        when(indexSearchService.findAll(anyInt(), anyInt(), anyString()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<Institution> queryResult = institutionService.search(searchText, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .findAll(page, limit, Entity.INSTITUTION.toString());
        verifyNoMoreInteractions(indexSearchService);
    }


    @Test
    void search_notEmptySearchText() {
        // given
        final Optional<String> searchText = Optional.of("pippo");
        final int page = 0;
        final int limit = 0;

        final DummyInstitutionQueryResult queryResultMock = new DummyInstitutionQueryResult();
        when(indexSearchService.fullTextSearch(any(), anyString(), anyInt(), anyInt()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<Institution> queryResult = institutionService.search(searchText, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .fullTextSearch(Field.DESCRIPTION, searchText.get(), page, limit);
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void search_filtered_emptySearchText() {
        // given
        final Optional<String> searchText = Optional.empty();
        final int page = 0;
        final int limit = 0;
        final String categories = "cat1,cat2,cat3";
        final DummyInstitutionQueryResult queryResultMock = new DummyInstitutionQueryResult();
        when(indexSearchService.findAll(anyInt(), anyInt(), anyString()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<Institution> queryResult = institutionService.search(searchText, categories, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .findAll(page, limit, Entity.INSTITUTION.toString());
        verifyNoMoreInteractions(indexSearchService);
    }


    @Test
    void search_filtered_notEmptySearchText() {
        // given
        final Optional<String> searchText = Optional.of("pippo");
        final int page = 0;
        final int limit = 0;
        final String categories = "cat1,cat2,cat3";

        final DummyInstitutionQueryResult queryResultMock = new DummyInstitutionQueryResult();
        when(indexSearchService.fullTextSearch(any(), anyString(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<Institution> queryResult = institutionService.search(searchText, categories, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .fullTextSearch(Field.DESCRIPTION, searchText.get(), Field.CATEGORY, categories, page, limit);
        verifyNoMoreInteractions(indexSearchService);
    }


    @Test
    void findById_infocamere() {
        // given
        final String id = "pippo";

        final List<String> categoriesMatcher = List.of("cat1", "cat2", "cat3");
        final Optional<Origin> origin = Optional.of(Origin.INFOCAMERE);
        // when
        final Executable executable = () -> institutionService.findById(id, origin, categoriesMatcher);
        // then
        assertThrows(RuntimeException.class, executable);
        verifyNoInteractions(indexSearchService);
    }


    @Test
    void findById_ResourceNotFound1() {
        // given
        final String id = "pippo";

        final List<String> categoriesMatcher = List.of("cat1", "cat2", "cat3");
        final Optional<Origin> origin = Optional.of(Origin.MOCK);
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of());
        // when
        final Executable executable = () -> institutionService.findById(id, origin, categoriesMatcher);
        // then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(indexSearchService, times(1))
                .findById(Field.ID, id);
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void findById_ResourceNotFound2() {
        // given
        final String id = "pippo";
        final List<String> categories = List.of("cat1", "cat2");
        final Optional<Origin> origin = Optional.of(IPA);
        final DummyInstitution institution = mockInstance(new DummyInstitution());
        institution.setOrigin(Origin.MOCK);
        institution.setCategory("cat1");
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(institution));
        // when
        final Executable executable = () -> institutionService.findById(id, origin, categories);
        // then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(indexSearchService, times(1))
                .findById(Field.ID, id);
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void findById_ResourceNotFound3() {
        // given
        final String id = "pippo";
        final List<String> categories = List.of("cat1", "cat2");
        final Optional<Origin> origin = Optional.of(IPA);
        final DummyInstitution institution = mockInstance(new DummyInstitution());
        institution.setOrigin(origin.get());
        institution.setCategory("cat3");
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(institution));
        // when
        final Executable executable = () -> institutionService.findById(id, origin, categories);
        // then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(indexSearchService, times(1))
                .findById(Field.ID, id);
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void findById_ResourceNotFound4() {
        // given
        final String id = "pippo";
        final List<String> categories = Collections.emptyList();
        final Optional<Origin> origin = Optional.of(IPA);
        final DummyInstitution institution = mockInstance(new DummyInstitution());
        institution.setOrigin(Origin.MOCK);
        institution.setCategory("cat3");
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(institution));
        // when
        final Executable executable = () -> institutionService.findById(id, origin, categories);
        // then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(indexSearchService, times(1))
                .findById(Field.ID, id);
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void findById_emptyCategories() {
        // given
        final String id = "pippo";
        final List<String> categories = Collections.emptyList();
        final Optional<Origin> origin = Optional.of(IPA);
        final DummyInstitution institution = mockInstance(new DummyInstitution());
        institution.setOrigin(origin.get());
        institution.setCategory("cat3");
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(institution));
        // when
        final Institution result = institutionService.findById(id, origin, categories);
        // then
        assertSame(institution, result);
        verify(indexSearchService, times(1))
                .findById(Field.ID, id);
        verifyNoMoreInteractions(indexSearchService);

    }

    @Test
    void findById_TooManyResourceFound() {
        // given
        final String id = "pippo";

        final List<String> categoriesMatcher = Collections.emptyList();
        final Optional<Origin> origin = Optional.of(IPA);
        final DummyInstitution institution = mockInstance(new DummyInstitution());
        institution.setOrigin(IPA);
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(institution, institution));
        // when
        final Executable executable = () -> institutionService.findById(id, origin, categoriesMatcher);
        // then
        assertThrows(TooManyResourceFoundException.class, executable);
        verify(indexSearchService, times(1))
                .findById(Field.ID, id);
        verifyNoMoreInteractions(indexSearchService);
    }


    @Test
    void findById_filtered_found() {
        // given
        final String id = "pippo";
        final List<String> categories = List.of("cat1", "cat2");
        final Optional<Origin> origin = Optional.of(IPA);
        final DummyInstitution institution = mockInstance(new DummyInstitution());
        institution.setOrigin(origin.get());
        institution.setCategory("cat1");
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(institution));
        // when
        final Institution result = institutionService.findById(id, origin, categories);
        // then
        assertSame(institution, result);
        verify(indexSearchService, times(1))
                .findById(Field.ID, id);
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void findById_TooManyResourceFound2() {
        // given
        final String id = "pippo";

        final List<String> categoriesMatcher = Collections.emptyList();
        final DummyInstitution institution = mockInstance(new DummyInstitution());
        institution.setOrigin(IPA);
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(institution, institution));
        // when
        final Executable executable = () -> institutionService.findById(id, Optional.of(IPA), categoriesMatcher);
        // then
        assertThrows(TooManyResourceFoundException.class, executable);
        verify(indexSearchService, times(1))
                .findById(Field.ID, id);
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void findById_ResourceNotFound5() {
        // given
        final String id = "pippo";
        final List<String> categories = Collections.emptyList();
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(new ArrayList<>());
        // when
        final Executable executable = () -> institutionService.findById(id, Optional.of(IPA), categories);
        // then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(indexSearchService, times(1))
                .findById(Field.ID, id);
        verifyNoMoreInteractions(indexSearchService);
    }

    @Test
    void updateInstitutionsIndex() {
        final String response = "id,Codice_IPA,Denominazione_ente,Codice_fiscale_ente,Mail1,Data_aggiornamento\n" +
                "id,Codice_IPA,Denominazione_ente,Codice_fiscale_ente,Mail1,2024-01-01";
        when(openDataRestClient.retrieveInstitutions()).thenReturn(response);
        Executable executable = () -> institutionService.updateInstitutionsIndex();
        Assertions.assertDoesNotThrow(executable);
    }

}