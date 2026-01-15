package it.pagopa.selfcare.product.model.dto.response;

import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractTemplateResponse {

    private String contractTemplateId;

    private String contractTemplatePath;

    private String contractTemplateVersion;

    private String productId;

    private String name;

    private String description;

    private Instant createdAt;

    private String createdBy;

    private ContractTemplateFileType fileType;

}
