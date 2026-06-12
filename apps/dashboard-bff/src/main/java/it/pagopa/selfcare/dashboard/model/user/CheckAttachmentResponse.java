package it.pagopa.selfcare.dashboard.model.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CheckAttachmentResponse {

    @Schema(description = "${swagger.dashboard.institutions.model.isAttachmentAvailable}")
    private final Boolean isAttachmentAvailable;

    @JsonCreator
    public CheckAttachmentResponse(@JsonProperty("isAttachmentAvailable") Boolean isAttachmentAvailable) {
        this.isAttachmentAvailable = isAttachmentAvailable;
    }
}

