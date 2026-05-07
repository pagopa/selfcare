package it.pagopa.selfcare.dashboard.model.groups;

import lombok.Data;

import java.util.Optional;
import java.util.UUID;

@Data
public class UserGroupFilter {
    private Optional<String> institutionId = Optional.empty();
    private Optional<String> productId = Optional.empty();
    private Optional<UUID> userId = Optional.empty();

    public void setInstitutionId(Optional<String> institutionId) {
        this.institutionId = institutionId == null ? Optional.empty() : institutionId;
    }

    public void setProductId(Optional<String> productId) {
        this.productId = productId == null ? Optional.empty() : productId;
    }

    public void setUserId(Optional<UUID> userId) {
        this.userId = userId == null ? Optional.empty() : userId;
    }

}
