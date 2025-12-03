package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.exception.ConflictException;
import it.pagopa.selfcare.product.exception.InternalException;
import it.pagopa.selfcare.product.mapper.ContractTemplateMapper;
import it.pagopa.selfcare.product.model.ContractTemplate;
import it.pagopa.selfcare.product.model.ContractTemplateFile;
import it.pagopa.selfcare.product.model.dto.request.ContractTemplateUploadRequest;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponse;
import it.pagopa.selfcare.product.repository.ContractTemplateRepository;
import it.pagopa.selfcare.product.storage.ContractTemplateStorage;
import it.pagopa.selfcare.product.util.HtmlUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class ContractTemplateServiceImpl implements ContractTemplateService {

    private static final String BASE_TEMPLATE_FILE = "contract-template.html";
    private static final String BASE_TEMPLATE_FRAGMENT_PLACEHOLDER = "${contractTemplateFragment}";
    private static final String BASE_TEMPLATE_LOGO_PLACEHOLDER = "${contractTemplateLogo}";

    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractTemplateStorage contractTemplateStorage;
    private final ContractTemplateMapper contractTemplateMapper;
    private final String contractTemplateBaseHTML;

    public ContractTemplateServiceImpl(ContractTemplateRepository contractTemplateRepository,
                                       ContractTemplateStorage contractTemplateStorage,
                                       ContractTemplateMapper contractTemplateMapper) throws IOException {
        this.contractTemplateRepository = contractTemplateRepository;
        this.contractTemplateStorage = contractTemplateStorage;
        this.contractTemplateMapper = contractTemplateMapper;
        this.contractTemplateBaseHTML = Files.readString(Path.of(
            Objects.requireNonNull(getClass().getClassLoader().getResource(BASE_TEMPLATE_FILE)).getPath()
        ));
    }

    @Override
    public Uni<ContractTemplateResponse> upload(ContractTemplateUploadRequest request) {
        final ContractTemplate contractTemplate = contractTemplateMapper.toContractTemplate(request);
        final ContractTemplateFile contractTemplateFile = buildContractTemplateFile(request.getFile());
        return contractTemplateRepository.countWithFilters(request.getProductId(), request.getName(), request.getVersion())
                .onItem().transformToUni(count -> {
                    if (count > 0) {
                        log.warn("Contract template already exists (productId = {}, name = {}, version = {})", contractTemplate.getProductId(), contractTemplate.getName(), contractTemplate.getVersion());
                        return Uni.createFrom().failure(new ConflictException("Contract template version already exists", "409"));
                    }
                    log.info("Persisting new contract template (productId = {}, name = {}, version = {})", contractTemplate.getProductId(), contractTemplate.getName(), contractTemplate.getVersion());
                    return contractTemplateRepository.persist(contractTemplate);
                })
                .onItem().transformToUni(ct ->
                        contractTemplateStorage.upload(ct.getProductId(), ct.getId(), contractTemplateFile)
                                .onFailure().invoke(t -> log.error("Error uploading file for contract template with id {} -> Rolling back database entry", ct.getId()))
                                .onFailure().call(t -> contractTemplateRepository.deleteById(ct.getId())
                                        .onFailure().invoke(rt -> log.error("Error rolling back database entry for contract template with id {}", ct.getId(), rt))
                                )
                                .onItem().transform(v -> contractTemplateMapper.toContractTemplateResponse(ct))
                );
    }

    @Override
    public Uni<ContractTemplateFile> download(String productId, String contractTemplateId) {
        return contractTemplateStorage.download(productId, contractTemplateId);
    }

    @Override
    public Uni<List<ContractTemplateResponse>> list(String productId, String name, String version) {
        return contractTemplateRepository.listWithFilters(productId, name, version)
                .onItem().transform(contractTemplateMapper::toContractTemplateResponseList);
    }

    private ContractTemplateFile buildContractTemplateFile(FileUpload fileUpload) {
        if (fileUpload.contentType().equals(MediaType.TEXT_HTML)) {
            return buildContractTemplateFileFromHTML(fileUpload);
        }
        return ContractTemplateFile.builder()
                .file(fileUpload.uploadedFile().toFile())
                .contentType(fileUpload.contentType())
                .build();
    }

    private ContractTemplateFile buildContractTemplateFileFromHTML(FileUpload fileUpload) {
        return Optional.ofNullable(HtmlUtils.getCleanHTML(fileUpload.uploadedFile().toFile())).map(htmlFragment -> {
            String html = contractTemplateBaseHTML.replace(BASE_TEMPLATE_FRAGMENT_PLACEHOLDER, htmlFragment);
            html = html.replace(BASE_TEMPLATE_LOGO_PLACEHOLDER, ""); // TODO
            return ContractTemplateFile.builder()
                    .data(html.getBytes(StandardCharsets.UTF_8))
                    .contentType(MediaType.TEXT_HTML)
                    .build();
        }).orElseThrow(() -> new InternalException("Unable to generate a contract template from the provided file", "500"));
    }

}
