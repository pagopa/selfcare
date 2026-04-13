package it.pagopa.selfcare.onboarding.client.model.onboarding;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProductInfo {

    private String id;
    private String role;
    private OffsetDateTime createdAt;

}
