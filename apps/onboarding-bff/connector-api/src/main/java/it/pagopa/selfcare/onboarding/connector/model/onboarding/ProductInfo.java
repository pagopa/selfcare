package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProductInfo {

    private String id;
    private String role;
    private OffsetDateTime createdAt;

}
