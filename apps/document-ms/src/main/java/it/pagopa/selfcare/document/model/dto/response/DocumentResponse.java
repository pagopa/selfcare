package it.pagopa.selfcare.document.model.dto.response;


import it.pagopa.selfcare.onboarding.common.DocumentType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentResponse {

    private String id;
    private DocumentType type;
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
