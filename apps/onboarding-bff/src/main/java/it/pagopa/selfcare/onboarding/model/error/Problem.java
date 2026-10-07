package it.pagopa.selfcare.onboarding.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    public static class InvalidParam {
        private String name;
        private String reason;
    }
}
