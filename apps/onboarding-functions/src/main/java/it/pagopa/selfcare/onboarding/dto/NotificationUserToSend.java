package it.pagopa.selfcare.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.pagopa.selfcare.onboarding.utils.CustomOffsetDateTimeSerializer;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationUserToSend {

    private String id;
    private String institutionId;
    private String product;

    private String onboardingTokenId;

    private String createdAt;
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    private OffsetDateTime closedAt;

    private String updatedAt;

    private NotificationUserType type;
    private UserToNotify user;


    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }


    public NotificationUserType getType() {
        return type;
    }

    public void setType(NotificationUserType type) {
        this.type = type;
    }


    public String getOnboardingTokenId() {
        return onboardingTokenId;
    }

    public void setOnboardingTokenId(String onboardingTokenId) {
        this.onboardingTokenId = onboardingTokenId;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }


    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserToNotify getUser() {
        return user;
    }

    public void setUser(UserToNotify user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "NotificationToSend{" +
                "id='" + id + '\'' +
                ", institutionId='" + institutionId + '\'' +
                ", product='" + product + '\'' +
                ", onboardingTokenId='" + onboardingTokenId + '\'' +
                ", createdAt=" + createdAt +
                ", closedAt=" + closedAt +
                ", updatedAt=" + updatedAt +
                ", userId=" + user.getUserId() +
                '}';
    }

}