package it.pagopa.selfcare.onboarding.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class NotificationMailRequest {
    private NotificationMailType type;
    private List<String> destinationMails;
    private String templatePath;
    private Map<String, String> mailParameters;
    private String prefixSubject;
    private FileMailData fileMailData;
}
