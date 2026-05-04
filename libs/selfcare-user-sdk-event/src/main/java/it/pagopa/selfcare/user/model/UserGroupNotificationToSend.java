package it.pagopa.selfcare.user.model;

import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
public class UserGroupNotificationToSend {
    private String id;
    private String institutionId;
    private String productId;
    private String name;
    private String description;
    private String status;
    private Set<String> members;
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;
}
