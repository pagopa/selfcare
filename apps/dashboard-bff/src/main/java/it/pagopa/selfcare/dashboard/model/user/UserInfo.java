package it.pagopa.selfcare.dashboard.model.user;

import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import it.pagopa.selfcare.dashboard.model.institution.RelationshipState;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(of = "id")
public class UserInfo {

    private String id;
    private User user;
    private SelfCareAuthority role;
    private Map<String, ProductInfo> products;
    private String status;
    private String institutionId;
    private String userMailUuid;

    @Data
    public static class UserInfoFilter {
        private SelfCareAuthority role;
        private String productId;
        private List<String> productRoles;
        private List<String> roles;
        private String userId;
        private List<RelationshipState> allowedStates;
    }

}
