package it.pagopa.selfcare.dashboard.model.support;

import lombok.*;

@Data
@Builder
@ToString
@NoArgsConstructor @AllArgsConstructor
public class SupportResponse {
    private String redirectUrl;
    private String jwt;
    private String actionUrl;
}
