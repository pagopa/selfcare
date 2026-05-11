package it.pagopa.selfcare.external_api.model.token;

import it.pagopa.selfcare.external_api.model.onboarding.InstitutionUpdate;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class TokenOnboardedUsers {
    private String id;
    private String status;
    private String institutionId;
    private String productId;
    private OffsetDateTime expiringDate;
    private List<TokenUser> users;
    private InstitutionUpdate institutionUpdate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime activatedAt;
}
