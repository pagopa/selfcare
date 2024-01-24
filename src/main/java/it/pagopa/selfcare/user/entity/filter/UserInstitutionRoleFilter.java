package it.pagopa.selfcare.user.entity.filter;

import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

import static it.pagopa.selfcare.user.entity.filter.UserInstitutionRoleFilter.UserInstitutionRoleEnum.*;

@Builder
public class UserInstitutionRoleFilter {

    private final Object institutionId;
    private final Object institutionName;
    private final Object status;
    private final Object role;

    @Getter
    @AllArgsConstructor
    public enum UserInstitutionRoleEnum{
        INSTITUTION_NAME(UserInfo.Fields.institutions.name(), UserInstitutionRole.Fields.institutionName.name()),
        INSTITUTION_ID(UserInfo.Fields.institutions.name(), UserInstitutionRole.Fields.institutionId.name()),
        STATUS(UserInfo.Fields.institutions.name(), UserInstitutionRole.Fields.status.name()),
        ROLE(UserInfo.Fields.institutions.name(), UserInstitutionRole.Fields.role.name());

        private final String parent;
        private final String child;

        public static Optional<String> retrieveParent(String child){
            return Arrays.stream(values())
                    .filter(userInstitutionRoleEnum -> userInstitutionRoleEnum.getChild().equalsIgnoreCase(child))
                    .findFirst()
                    .map(UserInstitutionRoleEnum::getParent);
        }
    }

    public Map<String, Object> constructMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(INSTITUTION_ID.getChild(), institutionId);
        map.put(INSTITUTION_NAME.getChild(), institutionName);
        map.put(STATUS.getChild(), status);
        map.put(ROLE.getChild(), role);

        map.values().removeIf(Objects::isNull);

        return map;
    }
}
