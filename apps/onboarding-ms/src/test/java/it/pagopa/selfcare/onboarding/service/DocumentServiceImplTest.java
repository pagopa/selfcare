package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.impl.DocumentServiceImpl;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.api.DocumentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentResponse;

class DocumentServiceImplTest {

    @Test
    void getDocumentByOnboardingId_shouldDelegateToDocumentControllerApi() {
        //given
        DocumentControllerApi documentControllerApi = mock(DocumentControllerApi.class);
        DocumentContentControllerApi documentContentControllerApi = mock(DocumentContentControllerApi.class);
        DocumentServiceImpl service = new DocumentServiceImpl(documentControllerApi, documentContentControllerApi);
        DocumentResponse documentResponse = new DocumentResponse();
        documentResponse.setOnboardingId("onb-001");
        Uni<DocumentResponse> expectedUni = Uni.createFrom().item(documentResponse);
        when(documentControllerApi.getDocumentByOnboardingId("onb-001")).thenReturn(expectedUni);

        //when
        Uni<DocumentResponse> result = service.getDocumentByOnboardingId("onb-001");

        //then
        assertSame(expectedUni, result);
        verify(documentControllerApi).getDocumentByOnboardingId("onb-001");
    }
}
