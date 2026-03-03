package it.pagopa.selfcare.onboarding.dto;

public class UserInstitutionFilters {
    private String productId;
    private String institutionId;

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

    @Override
    public String toString() {
        return "UserInstitutionFilters{" +
                ", productId='" + productId + '\'' +
                ", institutionId='" + institutionId + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String productId;
        private String institutionId;

        public Builder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder institutionId(String institutionId) {
            this.institutionId = institutionId;
            return this;
        }

        public UserInstitutionFilters build() {
            UserInstitutionFilters filters = new UserInstitutionFilters();
            filters.setProductId(this.productId);
            filters.setInstitutionId(this.institutionId);
            return filters;
        }
    }
}