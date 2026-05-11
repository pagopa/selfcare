package it.pagopa.selfcare.dashboard.model.user_groups;

import it.pagopa.selfcare.dashboard.model.groups.UserGroupStatus;
import lombok.Data;

import java.util.List;

@Data
public class CreateUserGroupRequestDto {
    private String institutionId;
    private String productId;
    private String name;
    private String description;
    private UserGroupStatus status;
    private List<String> members;
}
