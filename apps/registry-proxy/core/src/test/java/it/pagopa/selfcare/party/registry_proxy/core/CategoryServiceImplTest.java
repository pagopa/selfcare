package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category.Field;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
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
class CategoryServiceImplTest {

    @Mock
    private IndexSearchService<Category> indexSearchService;

    @InjectMocks
    private CategoryServiceImpl categoryService;


    @Test
    void search_emptyOrigin() {
        // given
        final Optional<Origin> origin = Optional.empty();
        final int page = 0;
        final int limit = 0;

        final DummyCategoryQueryResult queryResultMock = new DummyCategoryQueryResult();
        when(indexSearchService.findAll(anyInt(), anyInt(), anyString()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<Category> queryResult = categoryService.search(origin, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
        verify(indexSearchService, times(1))
                .findAll(page, limit, "CATEGORY");
        verifyNoMoreInteractions(indexSearchService);
    }


    @Test
    void search_notEmptyOrigin() {
        // given
        final Optional<Origin> origin = Optional.of(Origin.IPA);
        final int page = 0;
        final int limit = 0;
        final DummyCategoryQueryResult queryResultMock = new DummyCategoryQueryResult();
        when(indexSearchService.findAll(anyInt(), anyInt(), any(), any()))
                .thenReturn(queryResultMock);
        // when
        final QueryResult<Category> queryResult = categoryService.search(origin, page, limit);
        // then
        assertSame(queryResultMock, queryResult);
    }


    private static String createId(Origin origin, String code) {
        return origin + "_" + code;
    }


    @Test
    void findById_infocamere() {
        // given
        final String id = "pippo";
        final Origin origin = Origin.INFOCAMERE;
        // when
        final Executable executable = () -> categoryService.findById(id, origin);
        // then
        assertThrows(RuntimeException.class, executable);
        verifyNoInteractions(indexSearchService);
    }


    @Test
    void findById_ResourceNotFound() {
        // given
        final String id = "pippo";
        final Origin origin = Origin.MOCK;
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of());
        // when
        final Executable executable = () -> categoryService.findById(id, origin);
        // then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(indexSearchService, times(1))
                .findById(Field.ID, createId(origin, id));
        verifyNoMoreInteractions(indexSearchService);
    }


    @Test
    void findById_TooManyResourceFound() {
        // given
        final String id = "pippo";
        final Origin origin = Origin.MOCK;
        final DummyCategory category = new DummyCategory();
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(category, category));
        // when
        final Executable executable = () -> categoryService.findById(id, origin);
        // then
        assertThrows(TooManyResourceFoundException.class, executable);
        verify(indexSearchService, times(1))
                .findById(Field.ID, createId(origin, id));
        verifyNoMoreInteractions(indexSearchService);
    }


    @Test
    void findById_found() {
        // given
        final String id = "pippo";
        final Origin origin = Origin.IPA;
        final DummyCategory category = new DummyCategory();
        when(indexSearchService.findById(any(), anyString()))
                .thenReturn(List.of(category));
        // when
        final Category result = categoryService.findById(id, origin);
        // then
        assertSame(category, result);
        verify(indexSearchService, times(1))
                .findById(Field.ID, createId(origin, id));
        verifyNoMoreInteractions(indexSearchService);
    }

}