package it.pagopa.selfcare.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.pagopa.selfcare.onboarding.utils.CustomOffsetDateTimeSerializer;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Objects;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationToSend {

    private String id;
    private String internalIstitutionID;
    private String institutionId;
    private String product;
    private String state;
    private String filePath;
    private String fileName;
    private String contentType;
    private String onboardingTokenId;
    private String pricingPlan;
    private InstitutionToNotify institution;
    private BillingToSend billing;
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    private OffsetDateTime createdAt;
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    private OffsetDateTime closedAt;
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    private OffsetDateTime updatedAt;
    private QueueEvent notificationType;
    private NotificationType type;
    private Boolean isAggregator;
    private RootAggregator rootAggregator;
    private Boolean testInstitution;
    private String referenceOnboardingId;

    public void setContentType(String contractSigned) {
        String contractFileName = Objects.isNull(contractSigned) ? "" : contractSigned;
        if (contractFileName.isEmpty()) {
            this.contentType = "";
        } else if (contractFileName.endsWith(".p7m")) {
            this.contentType = "application/pkcs7-mime";
        } else if (contractFileName.endsWith(".pdf")) {
            this.contentType = "application/pdf";
        } else {
            this.contentType = "application/octet-stream";
        }
    }

    @Override
    public String toString() {
        return "NotificationToSend{" +
                "id='" + id + '\'' +
                ", institutionId='" + institutionId + '\'' +
                ", product='" + product + '\'' +
                ", state='" + state + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", onboardingTokenId='" + onboardingTokenId + '\'' +
                ", pricingPlan='" + pricingPlan + '\'' +
                ", billing=" + billing +
                ", createdAt=" + createdAt +
                ", closedAt=" + closedAt +
                ", updatedAt=" + updatedAt +
                ", notificationType=" + notificationType +
                '}';
    }

}