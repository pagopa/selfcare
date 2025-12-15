package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.exception.ConflictException;
import it.pagopa.selfcare.product.exception.InternalException;
import it.pagopa.selfcare.product.mapper.ContractTemplateMapper;
import it.pagopa.selfcare.product.model.ContractTemplate;
import it.pagopa.selfcare.product.model.ContractTemplateFile;
import it.pagopa.selfcare.product.model.dto.request.ContractTemplateUploadRequest;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponse;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponseList;
import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import it.pagopa.selfcare.product.repository.ContractTemplateRepository;
import it.pagopa.selfcare.product.storage.ContractTemplateStorage;
import it.pagopa.selfcare.product.util.HtmlUtils;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class ContractTemplateServiceImpl implements ContractTemplateService {

    private static final String BASE_TEMPLATE_FILE = "contract-template.html";
    private static final String BASE_TEMPLATE_FRAGMENT_PLACEHOLDER = "${contractTemplateFragment}";
    private static final String BASE_TEMPLATE_LOGO_PLACEHOLDER = "${contractTemplateLogo}";
    private static final String BASE_TEMPLATE_LOGO_PATH = "%s/%s/logo.png"; // {contractTemplateLogoBaseUrl}/{productId}/logo.png

    private static final String BASE_TEMPLATE_HTML;
    static {
        try (InputStream is = ContractTemplateServiceImpl.class.getClassLoader().getResourceAsStream(BASE_TEMPLATE_FILE)) {
            BASE_TEMPLATE_HTML = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            log.info("Base contract template HTML loaded successfully");
        } catch (Exception ex) {
            log.error("Unable to load base contract template HTML", ex);
            throw new RuntimeException(ex);
        }
    }

    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractTemplateStorage contractTemplateStorage;
    private final ContractTemplateMapper contractTemplateMapper;
    private final String contractTemplateLogoBaseUrl;

    public ContractTemplateServiceImpl(ContractTemplateRepository contractTemplateRepository,
                                       ContractTemplateStorage contractTemplateStorage,
                                       ContractTemplateMapper contractTemplateMapper,
                                       @ConfigProperty(name = "product-ms.contract-template.logo-base-url")
                                       String contractTemplateLogoBaseUrl) throws IOException {
        this.contractTemplateRepository = contractTemplateRepository;
        this.contractTemplateStorage = contractTemplateStorage;
        this.contractTemplateMapper = contractTemplateMapper;
        this.contractTemplateLogoBaseUrl = contractTemplateLogoBaseUrl;
    }

    @Override
    public Uni<ContractTemplateResponse> upload(ContractTemplateUploadRequest request) {
        final ContractTemplate contractTemplate = contractTemplateMapper.toContractTemplate(request);
        final ContractTemplateFile contractTemplateFile = buildContractTemplateFile(contractTemplate.getProductId(), request.getFile());
        contractTemplate.setFileType(contractTemplateFile.getType());
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
                                .onItem().transform(v -> contractTemplateMapper.toContractTemplateResponse(ct,
                                        contractTemplateStorage.getContractTemplatePath(ct.getProductId(), ct.getId(), ct.getFileType().getExtension())))
                );
    }

    @Override
    public Uni<ContractTemplateFile> download(String productId, String contractTemplateId, ContractTemplateFileType fileType) {
        return contractTemplateStorage.download(productId, contractTemplateId, fileType);
    }

    @Override
    public Uni<ContractTemplateResponseList> list(String productId, String name, String version) {
        return contractTemplateRepository.listWithFilters(productId, name, version)
                .onItem().transform(l -> {
                    ContractTemplateResponseList response = new ContractTemplateResponseList();
                    response.setItems(
                            l.stream()
                                    .map(ct -> contractTemplateMapper.toContractTemplateResponse(
                                            ct,
                                            contractTemplateStorage.getContractTemplatePath(
                                                    ct.getProductId(),
                                                    ct.getId(),
                                                    ct.getFileType().getExtension())))
                                    .toList()
                    );
                    return response;
                });
    }

    private ContractTemplateFile buildContractTemplateFile(String productId, FileUpload fileUpload) {
        if (fileUpload.contentType().equals(ContractTemplateFileType.HTML.getContentType())) {
            return buildContractTemplateFileFromHTML(productId, fileUpload);
        } else if (fileUpload.contentType().equals(ContractTemplateFileType.PDF.getContentType())) {
            return ContractTemplateFile.builder().file(fileUpload.uploadedFile().toFile())
                    .type(ContractTemplateFileType.PDF).build();
        } else {
            throw new InternalException("Unsupported contract template file type: " + fileUpload.contentType(), "500");
        }
    }

    private ContractTemplateFile buildContractTemplateFileFromHTML(String productId, FileUpload fileUpload) {
        return Optional.ofNullable(HtmlUtils.getCleanHTML(fileUpload.uploadedFile().toFile())).map(htmlFragment -> {
            String html = BASE_TEMPLATE_HTML.replace(BASE_TEMPLATE_FRAGMENT_PLACEHOLDER, htmlFragment);
            html = html.replace(BASE_TEMPLATE_LOGO_PLACEHOLDER, String.format(BASE_TEMPLATE_LOGO_PATH, contractTemplateLogoBaseUrl, productId));
            return ContractTemplateFile.builder()
                    .data(html.getBytes(StandardCharsets.UTF_8))
                    .type(ContractTemplateFileType.HTML)
                    .build();
        }).orElseThrow(() -> new InternalException("Unable to generate a contract template from the provided file", "500"));
    }

}
