package it.pagopa.selfcare.onboarding.dto;

import java.util.List;

public class ResendNotificationsFilters {
    private String productId;
    private String institutionId;
    private String onboardingId;
    private String taxCode;
    private List<String> status;
    private String from;
    private String to;
    private Integer page;
    private String notificationEventTraceId;

    public ResendNotificationsFilters() {
    }

    private ResendNotificationsFilters(Builder builder) {
        this.productId = builder.productId;
        this.institutionId = builder.institutionId;
        this.onboardingId = builder.onboardingId;
        this.taxCode = builder.taxCode;
        this.status = builder.status;
        this.from = builder.from;
        this.to = builder.to;
        this.page = builder.page;
        this.notificationEventTraceId = builder.notificationEventTraceId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String productId;
        private String institutionId;
        private String onboardingId;
        private String taxCode;
        private List<String> status;
        private String from;
        private String to;
        private Integer page;
        private String notificationEventTraceId;

        public Builder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder institutionId(String institutionId) {
            this.institutionId = institutionId;
            return this;
        }

        public Builder onboardingId(String onboardingId) {
            this.onboardingId = onboardingId;
            return this;
        }

        public Builder taxCode(String taxCode) {
            this.taxCode = taxCode;
            return this;
        }

        public Builder status(List<String> status) {
            this.status = status;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        public Builder notificationEventTraceId(String notificationEventTraceId) {
            this.notificationEventTraceId = notificationEventTraceId;
            return this;
        }

        public ResendNotificationsFilters build() {
            return new ResendNotificationsFilters(this);
        }
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public String getOnboardingId() {
        return onboardingId;
    }

    public void setOnboardingId(String onboardingId) {
        this.onboardingId = onboardingId;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public List<String> getStatus() {
        return status;
    }

    public void setStatus(List<String> status) {
        this.status = status;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public String getNotificationEventTraceId() {
        return notificationEventTraceId;
    }

    public void setNotificationEventTraceId(String notificationEventTraceId) {
        this.notificationEventTraceId = notificationEventTraceId;
    }

    @Override
    public String toString() {
        return "ResendNotificationsFilters{" +
                "productId='" + productId + '\'' +
                ", institutionId='" + institutionId + '\'' +
                ", onboardingId='" + onboardingId + '\'' +
                ", taxCode='" + taxCode + '\'' +
                ", status=" + status +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", page='" + page + '\'' +
                ", notificationEventTraceId='" + notificationEventTraceId + '\'' +
                '}';
    }
}
