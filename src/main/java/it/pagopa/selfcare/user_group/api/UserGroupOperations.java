package it.pagopa.selfcare.user_group.api;


import it.pagopa.selfcare.user_group.model.UserGroupStatus;

import java.time.Instant;
import java.util.Set;

public interface UserGroupOperations {

    String getId();

    void setId(String id);

    String getInstitutionId();

    void setInstitutionId(String institutionId);

    String getParentInstitutionId();

    void setParentInstitutionId(String parentInstitutionId);

    String getProductId();

    void setProductId(String productId);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    UserGroupStatus getStatus();

    void setStatus(UserGroupStatus status);

    Set<String> getMembers();

    void setMembers(Set<String> members);

    Instant getCreatedAt();

    void setCreatedAt(Instant createdAt);

    String getCreatedBy();

    void setCreatedBy(String createdBy);

    Instant getModifiedAt();

    void setModifiedAt(Instant modifiedAt);

    String getModifiedBy();

    void setModifiedBy(String modifiedBy);

}
