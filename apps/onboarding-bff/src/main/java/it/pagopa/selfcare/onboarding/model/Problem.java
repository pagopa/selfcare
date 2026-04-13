package it.pagopa.selfcare.onboarding.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Problem {
    private Integer status;
    private String title;
    private String detail;
    private List<InvalidParam> invalidParams;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvalidParam {
        private String name;
        private String reason;
    }
}
