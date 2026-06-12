package it.pagopa.selfcare.external_api.model.token;

import it.pagopa.selfcare.external_api.model.onboarding.InstitutionUpdate;
import it.pagopa.selfcare.external_api.model.user.RelationshipState;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class Token {
    private String id;
    private TokenType type;
    private RelationshipState status;
    private String institutionId;
    private String productId;
    private OffsetDateTime expiringDate;
    private String checksum;
    private String contractVersion;
    private String contractTemplate;
    private String contractSigned;
    private List<TokenUser> users;
    private InstitutionUpdate institutionUpdate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
}
