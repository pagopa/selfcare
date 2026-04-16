package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.constant.TokenType;
import it.pagopa.selfcare.mscore.model.institution.InstitutionUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRelationships {
    private String tokenId;
    private String checksum;
    private TokenType type;
    private String institutionId;
    private String productId;
    private String contractSigned;
    private String contentType;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime activatedAt;
    private RelationshipState status;
    private List<OnboardedUser> users;
    private InstitutionUpdate institutionUpdate;
}
