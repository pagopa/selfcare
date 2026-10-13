package it.pagopa.selfcare.onboarding.dto;

public class UserInstitutionFilters {
    private String productId;
    private String institutionId;
    private String tenantId;

    public UserInstitutionFilters() {
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return "UserInstitutionFilters{" +
                ", productId='" + productId + '\'' +
                ", institutionId='" + institutionId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String productId;
        private String institutionId;
        private String tenantId;

        public Builder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder institutionId(String institutionId) {
            this.institutionId = institutionId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public UserInstitutionFilters build() {
            UserInstitutionFilters filters = new UserInstitutionFilters();
            filters.setProductId(this.productId);
            filters.setInstitutionId(this.institutionId);
            filters.setTenantId(this.tenantId);
            return filters;
        }
    }
}