package it.pagopa.selfcare.user.entity.filter;

import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter.OnboardedProductFilterField.*;
import static it.pagopa.selfcare.user.entity.filter.UserInstitutionRoleFilter.UserInstitutionRoleEnum.INSTITUTION_ID;
import static it.pagopa.selfcare.user.entity.filter.UserInstitutionRoleFilter.UserInstitutionRoleEnum.INSTITUTION_NAME;

@Builder
public class UserInstitutionRoleFilter {

    private final Object institutionId;
    private final Object institutionName;
    private final Object status;
    private final Object role;

    @Getter
    @AllArgsConstructor
    public enum UserInstitutionRoleEnum{
        INSTITUTION_NAME(UserInfo.Fields.institutions.name() + "." + UserInstitutionRole.Fields.institutionName.name()),
        INSTITUTION_ID(UserInfo.Fields.institutions.name() + "." + UserInstitutionRole.Fields.institutionId.name()),
        STATUS(UserInfo.Fields.institutions.name() + "." + UserInstitutionRole.Fields.status.name()),
        ROLE(UserInfo.Fields.institutions.name() + "." + UserInstitutionRole.Fields.role.name());
        private final String description;
    }

    public Map<String, Object> constructMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(INSTITUTION_ID.getDescription(), institutionId);
        map.put(INSTITUTION_NAME.getDescription(), institutionName);
        map.put(STATUS.getDescription(), status);
        map.put(ROLE.getDescription(), role);

        map.values().removeIf(Objects::isNull);

        return map;
    }
}
