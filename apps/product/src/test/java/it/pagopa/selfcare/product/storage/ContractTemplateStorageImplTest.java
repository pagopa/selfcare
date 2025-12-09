package it.pagopa.selfcare.product.storage;

import com.azure.core.http.HttpResponse;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.models.BlobDownloadContentAsyncResponse;
import com.azure.storage.blob.models.BlobStorageException;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.product.exception.ConflictException;
import it.pagopa.selfcare.product.exception.InternalException;
import it.pagopa.selfcare.product.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.model.ContractTemplateFile;
import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class ContractTemplateStorageImplTest {

    private ContractTemplateStorageImpl contractTemplateStorage;

    private BlobAsyncClient blob;

    @BeforeEach
    void setup() {
        final BlobServiceAsyncClient blobServiceAsyncClient = Mockito.mock(BlobServiceAsyncClient.class);
        final BlobContainerAsyncClient blobContainer = Mockito.mock(BlobContainerAsyncClient.class);
        blob = Mockito.mock(BlobAsyncClient.class);
        Mockito.when(blobServiceAsyncClient.getBlobContainerAsyncClient("test-container")).thenReturn(blobContainer);
        Mockito.when(blobContainer.getBlobAsyncClient(Mockito.anyString())).thenReturn(blob);
        contractTemplateStorage = new ContractTemplateStorageImpl("test-container", blobServiceAsyncClient);
    }

    @Test
    void upload_ShouldUploadFileSuccessfully() throws URISyntaxException {
        final ContractTemplateFile contractTemplateFile = ContractTemplateFile.builder()
                .file(new File(getClass().getResource("/request/contract-template-fragment.html").toURI()))
                .data(new byte[0])
                .type(ContractTemplateFileType.HTML)
                .build();
        Mockito.when(blob.uploadFromFile(any(), any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        contractTemplateStorage.upload("prod-test", "123", contractTemplateFile)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();
        Mockito.verify(blob, Mockito.times(1)).uploadFromFile(any(), any(), any(), any(), any(), any());
        Mockito.verify(blob, Mockito.never()).uploadWithResponse(any(), any(), any(), any(), any(), any());
    }

    @Test
    void upload_ShouldUploadBytesSuccessfully() {
        final ContractTemplateFile contractTemplateFile = ContractTemplateFile.builder()
                .data(new byte[0])
                .type(ContractTemplateFileType.HTML)
                .build();
        Mockito.when(blob.uploadWithResponse(any(), any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        contractTemplateStorage.upload("prod-test", "123", contractTemplateFile)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();
        Mockito.verify(blob, Mockito.never()).uploadFromFile(any(), any(), any(), any(), any(), any());
        Mockito.verify(blob, Mockito.times(1)).uploadWithResponse(any(), any(), any(), any(), any(), any());
    }

    @Test
    void upload_Conflict() {
        final ContractTemplateFile contractTemplateFile = ContractTemplateFile.builder()
                .data(new byte[0])
                .type(ContractTemplateFileType.HTML)
                .build();
        final HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.getStatusCode()).thenReturn(409);
        final BlobStorageException blobStorageException = new BlobStorageException("Conflict", httpResponse, null);
        Mockito.when(blob.uploadWithResponse(any(), any(), any(), any(), any(), any())).thenReturn(Mono.error(blobStorageException));
        var sub = contractTemplateStorage.upload("prod-test", "123", contractTemplateFile)
                        .subscribe().withSubscriber(UniAssertSubscriber.create()).assertFailed();
        Assertions.assertInstanceOf(ConflictException.class, sub.getFailure());
        Assertions.assertEquals("409", ((ConflictException) sub.getFailure()).getCode());
    }

    @Test
    void upload_Error() {
        final ContractTemplateFile contractTemplateFile = ContractTemplateFile.builder()
                .data(new byte[0])
                .type(ContractTemplateFileType.HTML)
                .build();
        final HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.getStatusCode()).thenReturn(500);
        final BlobStorageException blobStorageException = new BlobStorageException("Error", httpResponse, null);
        Mockito.when(blob.uploadWithResponse(any(), any(), any(), any(), any(), any())).thenReturn(Mono.error(blobStorageException));
        var sub = contractTemplateStorage.upload("prod-test", "123", contractTemplateFile)
                        .subscribe().withSubscriber(UniAssertSubscriber.create()).assertFailed();
        Assertions.assertInstanceOf(InternalException.class, sub.getFailure());
        Assertions.assertEquals("500", ((InternalException) sub.getFailure()).getCode());
    }

    @Test
    void download_ShouldDownloadFileSuccessfully() {
        final BlobDownloadContentAsyncResponse response = Mockito.mock(BlobDownloadContentAsyncResponse.class);
        Mockito.when(response.getValue()).thenReturn(BinaryData.fromString("test"));
        Mockito.when(blob.downloadContentWithResponse(any(), any())).thenReturn(Mono.just(response));
        // HTML
        var sub1 = contractTemplateStorage.download("prod-test", "123", ContractTemplateFileType.HTML)
                        .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();
        Assertions.assertNull(sub1.getItem().getFile());
        Assertions.assertNotNull(sub1.getItem().getData());
        Assertions.assertEquals(ContractTemplateFileType.HTML, sub1.getItem().getType());
        // PDF
        var sub2 = contractTemplateStorage.download("prod-test", "123", ContractTemplateFileType.PDF)
                        .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();
        Assertions.assertNull(sub2.getItem().getFile());
        Assertions.assertNotNull(sub2.getItem().getData());
        Assertions.assertEquals(ContractTemplateFileType.PDF, sub2.getItem().getType());
    }

    @Test
    void download_NotFound() {
        final HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.getStatusCode()).thenReturn(404);
        final BlobStorageException blobStorageException = new BlobStorageException("Not Found", httpResponse, null);
        Mockito.when(blob.downloadContentWithResponse(any(), any())).thenReturn(Mono.error(blobStorageException));
        var sub = contractTemplateStorage.download("prod-test", "123", ContractTemplateFileType.HTML)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ResourceNotFoundException.class);
        Assertions.assertEquals("404", ((ResourceNotFoundException) sub.getFailure()).getCode());
    }

    @Test
    void download_Error() {
        final HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.getStatusCode()).thenReturn(500);
        final BlobStorageException blobStorageException = new BlobStorageException("Error", httpResponse, null);
        Mockito.when(blob.downloadContentWithResponse(any(), any())).thenReturn(Mono.error(blobStorageException));
        var sub = contractTemplateStorage.download("prod-test", "123", ContractTemplateFileType.HTML)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(InternalException.class);
        Assertions.assertEquals("500", ((InternalException) sub.getFailure()).getCode());
    }

    @Test
    void getContractTemplatePath() {
        Assertions.assertEquals(
                "contract-templates/prod-test/123.html",
                contractTemplateStorage.getContractTemplatePath("prod-test", "123", "html")
        );
    }

}
