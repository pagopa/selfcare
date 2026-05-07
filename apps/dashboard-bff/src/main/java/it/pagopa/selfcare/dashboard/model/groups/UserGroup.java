package it.pagopa.selfcare.dashboard.model.groups;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class UserGroup {
    private String id;
    private String institutionId;
    private String productId;
    private String description;
    private String name;
    private UserGroupStatus status;
    private List<String> members;
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;
}
