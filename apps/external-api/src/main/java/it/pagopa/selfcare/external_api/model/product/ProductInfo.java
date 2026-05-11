package it.pagopa.selfcare.external_api.model.product;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProductInfo {

    private String id;
    private String role;
    private OffsetDateTime createdAt;

}
