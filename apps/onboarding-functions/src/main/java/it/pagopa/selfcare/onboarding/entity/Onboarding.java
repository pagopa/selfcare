package it.pagopa.selfcare.onboarding.entity;


import io.quarkus.mongodb.panache.common.MongoEntity;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDateTime;
import java.util.List;


@MongoEntity(collection = "onboardings")
@Data
public class Onboarding {

    @BsonId
    private String id;
    private String productId;
    private List<String> testEnvProductIds;
    private WorkflowType workflowType;
    private Institution institution;
    private List<User> users;
    private List<AggregateInstitution> aggregates;
    private String pricingPlan;
    private Billing billing;
    private Boolean signContract;
    private LocalDateTime expiringDate;
    private OnboardingStatus status;
    private UserRequester userRequester;
    private String workflowInstanceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime deletedAt;
    private String reasonForReject;
    private Boolean isAggregator;
    private Aggregator aggregator;
    private String delegationId;
    private Boolean sendMailForImport;
    private Payment payment;
    private Boolean toAddOnAggregates;

    //This field is used in case of workflowType USER
    private String previousManagerId;

    private String referenceOnboardingId;

    @Override
    public String toString() {
        return "Onboarding{" +
                "id='" + id + '\'' +
                ", productId='" + productId + '\'' +
                ", testEnvProductIds=" + testEnvProductIds +
                ", workflowType=" + workflowType +
                ", institution=" + institution +
                ", users=" + users +
                ", pricingPlan='" + pricingPlan + '\'' +
                ", billing=" + billing +
                ", signContract=" + signContract +
                ", expiringDate=" + expiringDate +
                ", status=" + status +
                ", payment=" + payment +
                ", workflowInstanceId='" + workflowInstanceId + '\'' +
                ", activatedAt=" + activatedAt +
                ", deletedAt=" + deletedAt +
                ", userRequester=" + userRequester +
                ", reasonForReject='" + reasonForReject + '\'' +
                ", aggregator=" + aggregator +
                ", aggregates=" + aggregates +
                ", isAggregator='" + isAggregator + '\'' +
                ", sendMailForImport='" + sendMailForImport + '\'' +
                ", referenceOnboardingId'" + referenceOnboardingId + '\'' +
                '}';
    }

}
