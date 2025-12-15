package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.ContractTemplateFile;
import it.pagopa.selfcare.product.model.dto.request.ContractTemplateUploadRequest;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponse;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponseList;
import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;

public interface ContractTemplateService {

    Uni<ContractTemplateResponse> upload(ContractTemplateUploadRequest request);

    Uni<ContractTemplateFile> download(String productId, String contractTemplateId, ContractTemplateFileType fileType);

    Uni<ContractTemplateResponseList> list(String productId, String name, String version);

}
