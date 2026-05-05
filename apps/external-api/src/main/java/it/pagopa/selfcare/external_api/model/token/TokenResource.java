package it.pagopa.selfcare.external_api.model.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.external_api.model.onboarding.InstitutionUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResource {
    private String id;
    private String checksum;
    private List<LegalsResource> legals = new ArrayList<>();
    private String status;
    private String institutionId;
    private String productId;
    private OffsetDateTime expiringDate;
    private String contractVersion;
    private String contractTemplate;
    private String contractSigned;
    private String contentType;
    private InstitutionUpdate institutionUpdate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime activatedAt;

    public TokenResource(String id) {
        this.id = id;
    }
}
