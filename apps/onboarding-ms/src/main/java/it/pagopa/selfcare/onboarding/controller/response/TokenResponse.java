package it.pagopa.selfcare.onboarding.controller.response;


import it.pagopa.selfcare.onboarding.common.TokenType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TokenResponse {

    private String id;
    private TokenType type;
    private String productId;
    private String checksum;
    private String contractVersion;
    private String contractTemplate;
    private String contractSigned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private LocalDateTime deletedAt;
}
