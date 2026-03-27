package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.connector.model.RelationshipState;
import it.pagopa.selfcare.onboarding.connector.model.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Data
@EqualsAndHashCode(of = "id")
public class UserInfo {

    private String id;
    private User user;
    private String institutionId;
    private PartyRole role;
    private String status;

    @Data
    public static class UserInfoFilter {
        private Optional<EnumSet<PartyRole>> role = Optional.empty();
        private Optional<String> productId = Optional.empty();
        private Optional<Set<String>> productRoles = Optional.empty();
        private Optional<String> userId = Optional.empty();
        private Optional<EnumSet<RelationshipState>> allowedStates = Optional.empty();

        public void setRole(Optional<EnumSet<PartyRole>> role) {
            this.role = role == null ? Optional.empty() : role;
        }

        public void setProductId(Optional<String> productId) {
            this.productId = productId == null ? Optional.empty() : productId;
        }

        public void setProductRoles(Optional<Set<String>> productRoles) {
            this.productRoles = productRoles == null ? Optional.empty() : productRoles;
        }

        public void setUserId(Optional<String> userId) {
            this.userId = userId == null ? Optional.empty() : userId;
        }

        public void setAllowedStates(Optional<EnumSet<RelationshipState>> allowedStates) {
            this.allowedStates = allowedStates == null ? Optional.empty() : allowedStates;
        }
    }
}
