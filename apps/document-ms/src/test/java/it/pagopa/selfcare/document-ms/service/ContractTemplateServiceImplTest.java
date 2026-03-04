package it.pagopa.selfcare.product.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.product.exception.ConflictException;
import it.pagopa.selfcare.product.model.ContractTemplate;
import it.pagopa.selfcare.product.model.ContractTemplateFile;
import it.pagopa.selfcare.product.model.dto.request.ContractTemplateUploadRequest;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponse;
import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import it.pagopa.selfcare.product.repository.ContractTemplateRepository;
import it.pagopa.selfcare.product.storage.ContractTemplateStorage;
import jakarta.inject.Inject;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class ContractTemplateServiceImplTest {

  @Inject private ContractTemplateServiceImpl contractTemplateService;

  @InjectMock private ContractTemplateStorage contractTemplateStorage;

  @InjectMock private ContractTemplateRepository contractTemplateRepository;

  @Test
  void upload_shouldReturnOk() throws URISyntaxException {
    final FileUpload fileUpload = Mockito.mock(FileUpload.class);
    Mockito.when(fileUpload.contentType()).thenReturn("text/html");
    Mockito.when(fileUpload.uploadedFile())
        .thenReturn(
            Path.of(getClass().getResource("/request/contract-template-fragment.html").toURI()));

    final ContractTemplateUploadRequest uploadRequest =
        ContractTemplateUploadRequest.builder()
            .productId("prod-test")
            .name("template-test")
            .file(fileUpload)
            .version("1.0.0")
            .build();

    final ContractTemplate contractTemplate =
        ContractTemplate.builder()
            .id("123")
            .productId(uploadRequest.getProductId())
            .name(uploadRequest.getName())
            .version(uploadRequest.getVersion())
            .fileType(ContractTemplateFileType.HTML)
            .build();

    Mockito.when(
            contractTemplateRepository.countWithFilters(
                Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().item(0L));
    Mockito.when(contractTemplateRepository.persist(Mockito.any(ContractTemplate.class)))
        .thenReturn(Uni.createFrom().item(contractTemplate));
    Mockito.when(contractTemplateStorage.upload(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().voidItem());
    Mockito.when(
            contractTemplateStorage.getContractTemplatePath(
                Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn("contract-templates/prod-test/123.html");

    final ContractTemplateResponse response =
        contractTemplateService
            .upload(uploadRequest)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertEquals("123", response.getContractTemplateId());
    Assertions.assertEquals(
        "contract-templates/prod-test/123.html", response.getContractTemplatePath());
    Assertions.assertEquals("1.0.0", response.getContractTemplateVersion());
    Assertions.assertEquals("prod-test", response.getProductId());
    Assertions.assertEquals("template-test", response.getName());
  }

  @Test
  void upload_StorageError() throws URISyntaxException {
    final FileUpload fileUpload = Mockito.mock(FileUpload.class);
    Mockito.when(fileUpload.contentType()).thenReturn("text/html");
    Mockito.when(fileUpload.uploadedFile())
        .thenReturn(
            Path.of(getClass().getResource("/request/contract-template-fragment.html").toURI()));

    final ContractTemplateUploadRequest uploadRequest =
        ContractTemplateUploadRequest.builder()
            .productId("prod-test")
            .name("template-test")
            .file(fileUpload)
            .version("1.0.0")
            .build();

    final ContractTemplate contractTemplate =
        ContractTemplate.builder()
            .id("123")
            .productId(uploadRequest.getProductId())
            .name(uploadRequest.getName())
            .version(uploadRequest.getVersion())
            .fileType(ContractTemplateFileType.HTML)
            .build();

    Mockito.when(
            contractTemplateRepository.countWithFilters(
                Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().item(0L));
    Mockito.when(contractTemplateRepository.persist(Mockito.any(ContractTemplate.class)))
        .thenReturn(Uni.createFrom().item(contractTemplate));
    Mockito.when(contractTemplateRepository.deleteById(Mockito.any()))
        .thenReturn(Uni.createFrom().item(true));
    Mockito.when(contractTemplateStorage.upload(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().failure(new Exception("Storage error")));

    contractTemplateService
        .upload(uploadRequest)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed();
    Mockito.verify(contractTemplateRepository, Mockito.times(1))
        .countWithFilters(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(contractTemplateRepository, Mockito.times(1))
        .persist(Mockito.any(ContractTemplate.class));
    Mockito.verify(contractTemplateRepository, Mockito.times(1)).deleteById(Mockito.any());
    Mockito.verify(contractTemplateStorage, Mockito.times(1))
        .upload(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  void upload_Conflict() throws URISyntaxException {
    final FileUpload fileUpload = Mockito.mock(FileUpload.class);
    Mockito.when(fileUpload.contentType()).thenReturn("text/html");
    Mockito.when(fileUpload.uploadedFile())
        .thenReturn(
            Path.of(getClass().getResource("/request/contract-template-fragment.html").toURI()));

    final ContractTemplateUploadRequest uploadRequest =
        ContractTemplateUploadRequest.builder()
            .productId("prod-test")
            .name("template-test")
            .file(fileUpload)
            .version("1.0.0")
            .build();

    Mockito.when(
            contractTemplateRepository.countWithFilters(
                Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().item(1L));

    var sub =
        contractTemplateService
            .upload(uploadRequest)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(ConflictException.class);
    Assertions.assertEquals("409", ((ConflictException) sub.getFailure()).getCode());
    Mockito.verify(contractTemplateRepository, Mockito.times(1))
        .countWithFilters(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(contractTemplateRepository, Mockito.never())
        .persist(Mockito.any(ContractTemplate.class));
    Mockito.verifyNoInteractions(contractTemplateStorage);
  }

  @Test
  void upload_CountError() throws URISyntaxException {
    final FileUpload fileUpload = Mockito.mock(FileUpload.class);
    Mockito.when(fileUpload.contentType()).thenReturn("text/html");
    Mockito.when(fileUpload.uploadedFile())
        .thenReturn(
            Path.of(getClass().getResource("/request/contract-template-fragment.html").toURI()));

    final ContractTemplateUploadRequest uploadRequest =
        ContractTemplateUploadRequest.builder()
            .productId("prod-test")
            .name("template-test")
            .file(fileUpload)
            .version("1.0.0")
            .build();

    Mockito.when(
            contractTemplateRepository.countWithFilters(
                Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().failure(new Exception("DB error")));

    contractTemplateService
        .upload(uploadRequest)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed();
    Mockito.verify(contractTemplateRepository, Mockito.times(1))
        .countWithFilters(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(contractTemplateRepository, Mockito.never())
        .persist(Mockito.any(ContractTemplate.class));
    Mockito.verifyNoInteractions(contractTemplateStorage);
  }

  @Test
  void upload_PersistError() throws URISyntaxException {
    final FileUpload fileUpload = Mockito.mock(FileUpload.class);
    Mockito.when(fileUpload.contentType()).thenReturn("text/html");
    Mockito.when(fileUpload.uploadedFile())
        .thenReturn(
            Path.of(getClass().getResource("/request/contract-template-fragment.html").toURI()));

    final ContractTemplateUploadRequest uploadRequest =
        ContractTemplateUploadRequest.builder()
            .productId("prod-test")
            .name("template-test")
            .file(fileUpload)
            .version("1.0.0")
            .build();

    Mockito.when(
            contractTemplateRepository.countWithFilters(
                Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().item(0L));
    Mockito.when(contractTemplateRepository.persist(Mockito.any(ContractTemplate.class)))
        .thenReturn(Uni.createFrom().failure(new Exception("DB error")));

    contractTemplateService
        .upload(uploadRequest)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed();
    Mockito.verify(contractTemplateRepository, Mockito.times(1))
        .countWithFilters(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(contractTemplateRepository, Mockito.times(1))
        .persist(Mockito.any(ContractTemplate.class));
    Mockito.verifyNoInteractions(contractTemplateStorage);
  }

  @Test
  void download_shouldReturnOk() {
    Mockito.when(contractTemplateStorage.download(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().item(ContractTemplateFile.builder().build()));
    contractTemplateService
        .download("prod-test", "template-test", ContractTemplateFileType.HTML)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
    Mockito.verify(contractTemplateStorage, Mockito.times(1))
        .download("prod-test", "template-test", ContractTemplateFileType.HTML);
    Mockito.verifyNoInteractions(contractTemplateRepository);
  }

  @Test
  void download_Error() {
    Mockito.when(contractTemplateStorage.download(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().failure(new Exception("Storage error")));
    contractTemplateService
        .download("prod-test", "template-test", ContractTemplateFileType.HTML)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed();
    Mockito.verify(contractTemplateStorage, Mockito.times(1))
        .download("prod-test", "template-test", ContractTemplateFileType.HTML);
    Mockito.verifyNoInteractions(contractTemplateRepository);
  }

  @Test
  void list_shouldReturnOk() {
    Mockito.when(
            contractTemplateRepository.listWithFilters(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().item(List.of()));
    contractTemplateService
        .list("prod-test", "template-test", "1.0.0")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
    Mockito.verify(contractTemplateRepository, Mockito.times(1))
        .listWithFilters("prod-test", "template-test", "1.0.0");
    Mockito.verifyNoInteractions(contractTemplateStorage);
  }

  @Test
  void list_Error() {
    Mockito.when(
            contractTemplateRepository.listWithFilters(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Uni.createFrom().failure(new Exception("DB error")));
    contractTemplateService
        .list("prod-test", "template-test", "1.0.0")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed();
    Mockito.verify(contractTemplateRepository, Mockito.times(1))
        .listWithFilters("prod-test", "template-test", "1.0.0");
    Mockito.verifyNoInteractions(contractTemplateStorage);
  }
}
