package it.pagopa.selfcare.dashboard.controller;

import it.pagopa.selfcare.dashboard.model.document.DocumentStatusResource;
import it.pagopa.selfcare.dashboard.model.document.ProductDocumentStatusResource;
import it.pagopa.selfcare.dashboard.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest extends BaseControllerTest {

    private static final String BASE_URL = "/v2/documents";

    @InjectMocks
    private DocumentController documentController;

    @Mock
    private DocumentService documentServiceMock;

    @BeforeEach
    void setUp() {
        super.setUp(documentController);
    }

    @Test
    void getDocumentStatus() throws Exception {
        // given
        final String institutionId = "institutionId";
        when(documentServiceMock.getDocumentStatus(institutionId, List.of("prod-io", "prod-pagopa")))
                .thenReturn(new DocumentStatusResource(List.of(
                        new ProductDocumentStatusResource("prod-io", true, false),
                        new ProductDocumentStatusResource("prod-pagopa", true, true))));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/status")
                        .param("institutionId", institutionId)
                        .param("productIds", "prod-io,prod-pagopa")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].productId", is("prod-io")))
                .andExpect(jsonPath("$.products[0].contractUpdateRequired", is(true)))
                .andExpect(jsonPath("$.products[0].contractUpdateInProgress", is(false)))
                .andExpect(jsonPath("$.products[1].productId", is("prod-pagopa")))
                .andExpect(jsonPath("$.products[1].contractUpdateRequired", is(true)))
                .andExpect(jsonPath("$.products[1].contractUpdateInProgress", is(true)));

        // then
        verify(documentServiceMock, times(1)).getDocumentStatus(institutionId, List.of("prod-io", "prod-pagopa"));
        verifyNoMoreInteractions(documentServiceMock);
    }

    @Test
    void getDocumentStatus_withoutProductIds_returnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/status")
                        .param("institutionId", "institutionId")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentServiceMock);
    }

}
