package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.model.ContractTemplate;
import it.pagopa.selfcare.product.model.dto.request.ContractTemplateUploadRequest;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface ContractTemplateMapper {

    @Mapping(target = "name", source = "name", qualifiedByName = "normalizeName")
    ContractTemplate toContractTemplate(ContractTemplateUploadRequest request);

    @Mapping(target = "contractTemplateId", source = "id")
    @Mapping(target = "contractTemplateVersion", source = "version")
    ContractTemplateResponse toContractTemplateResponse(ContractTemplate contractTemplate);

    List<ContractTemplateResponse> toContractTemplateResponseList(List<ContractTemplate> contractTemplates);

    @Named("normalizeName")
    default String normalizeName(String name) {
        return name.trim().toLowerCase().replaceAll("\\s+", " ");
    }

}