package it.pagopa.selfcare.mscore.web.model.delegation;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.web.util.DecryptIfUuid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationResponse {

    @NotBlank
    private String id;
    @NotBlank
    private String institutionId;
    @NotBlank
    private String institutionName;
    private String institutionRootName;
    @NotNull
    private DelegationType type;
    @NotBlank
    private String productId;
    @DecryptIfUuid
    private String taxCode;
    private String institutionType;
    @NotBlank
    private String brokerId;
    @DecryptIfUuid
    private String brokerTaxCode;
    private String brokerType;
    private String brokerName;
    private DelegationState status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean isTest;

}
