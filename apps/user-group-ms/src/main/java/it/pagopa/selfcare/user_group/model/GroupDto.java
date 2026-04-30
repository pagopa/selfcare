package it.pagopa.selfcare.user_group.model;

import it.pagopa.selfcare.user_group.api.UserGroupOperations;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
public class GroupDto implements UserGroupOperations {
    private String id;
    private String institutionId;
    private String parentInstitutionId;
    private String productId;
    private String name;
    private String description;
    private UserGroupStatus status;
    private Set<String> members;
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;
}
