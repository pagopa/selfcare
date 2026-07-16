package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProductInfo {

    private String id;
    private String role;
    private OffsetDateTime createdAt;

}
