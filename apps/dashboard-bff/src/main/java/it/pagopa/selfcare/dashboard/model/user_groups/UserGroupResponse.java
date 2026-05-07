package it.pagopa.selfcare.dashboard.model.user_groups;

import it.pagopa.selfcare.dashboard.model.groups.UserGroupStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class UserGroupResponse {

    private String id;
    private String institutionId;
    private String productId;
    private String name;
    private String description;
    private UserGroupStatus status;
    private List<String> members;
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;

}
