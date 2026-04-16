package it.pagopa.selfcare.onboarding.dto.webhook;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRequest {
    private String productId;
    private String payload;
}
