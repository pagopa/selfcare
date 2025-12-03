package it.pagopa.selfcare.product.model.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ContractTemplateResponse {

    private String contractTemplateId;

    private String contractTemplateVersion;

    private String productId;

    private String name;

    private String description;

    private Instant createdAt;

    private String createdBy;

}
