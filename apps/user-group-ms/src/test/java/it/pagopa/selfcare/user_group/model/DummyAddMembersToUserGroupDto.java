package it.pagopa.selfcare.user_group.model;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DummyAddMembersToUserGroupDto {

    private String institutionId;
    private String parentInstitutionId;
    private String productId;
    private String name;
    private String description;
    private List<UUID> members = List.of(UUID.randomUUID());
}
