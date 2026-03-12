package it.pagopa.selfcare.document.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentBuilderResponse {
    private String documentId;
    private String checksum;
    private boolean alreadyExists;
}
