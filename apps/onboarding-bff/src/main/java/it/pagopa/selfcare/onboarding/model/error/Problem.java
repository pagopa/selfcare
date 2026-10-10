package it.pagopa.selfcare.onboarding.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Problem {
    private String type;
    private String title;
    private Integer status;
    private String detail;
    private String instance;
    private List<InvalidParam> invalidParams;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "A list of invalid parameters details.")
    public static class InvalidParam {
        @Schema(description = "Invalid parameter name.", required = true)
        private String name;
        @Schema(description = "Invalid parameter reason.", required = true)
        private String reason;
    }
}
