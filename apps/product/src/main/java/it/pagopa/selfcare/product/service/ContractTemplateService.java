package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.ContractTemplateFile;
import it.pagopa.selfcare.product.model.dto.request.ContractTemplateUploadRequest;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponse;
import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;

import java.util.List;

public interface ContractTemplateService {

    Uni<ContractTemplateResponse> upload(ContractTemplateUploadRequest request);

    Uni<ContractTemplateFile> download(String productId, String contractTemplateId, ContractTemplateFileType fileType);

    Uni<List<ContractTemplateResponse>> list(String productId, String name, String version);

}
