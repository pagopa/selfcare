package it.pagopa.selfcare.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class WebhookRequest {
    
    @NotBlank(message = "URL is required")
    private String url;
    
    @NotNull(message = "HTTP method is required")
    private String httpMethod;
    
    private Map<String, String> headers;

    private RetryPolicyRequest retryPolicy;
    
    @Data
    public static class RetryPolicyRequest {
        private Integer maxAttempts;
        private Long initialDelayMs;
        private Long maxDelayMs;
        private Double backoffMultiplier;
    }
}
