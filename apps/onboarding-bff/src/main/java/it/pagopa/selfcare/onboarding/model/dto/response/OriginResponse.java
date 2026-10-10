package it.pagopa.selfcare.onboarding.model.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.client.model.OriginEntry;
import lombok.Data;

import java.util.List;

@Data
public class OriginResponse {

    @Schema(description = "${openapi.product.model.id}")
    private List<OriginEntry> origins;

}
