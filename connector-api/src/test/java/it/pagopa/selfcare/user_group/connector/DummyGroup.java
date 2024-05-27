package it.pagopa.selfcare.user_group.connector;

import it.pagopa.selfcare.user_group.connector.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.connector.model.UserGroupStatus;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
public class DummyGroup implements UserGroupOperations {


    private String id;
    private String institutionId;
    private String productId;
    private String name;
    private String description;
    private UserGroupStatus status = UserGroupStatus.ACTIVE;
    private Set<String> members = Set.of("string1", "string2");
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;
}
