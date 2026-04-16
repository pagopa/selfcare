package it.pagopa.selfcare.mscore.model.onboarding;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private String contentType;
    private List<TokenUser> users;
    private InstitutionUpdate institutionUpdate;
    private OffsetDateTime createdAt;
    private OffsetDateTime activatedAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

}
