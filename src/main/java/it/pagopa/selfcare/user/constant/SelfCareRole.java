package it.pagopa.selfcare.user.constant;

import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

@Getter
public enum SelfCareRole {
    MANAGER(SelfCareAuthority.ADMIN),
    DELEGATE(SelfCareAuthority.ADMIN),
    SUB_DELEGATE(SelfCareAuthority.ADMIN),
    OPERATOR(SelfCareAuthority.LIMITED);

    private final SelfCareAuthority selfCareAuthority;

    private SelfCareRole(SelfCareAuthority selfCareAuthority) {
        this.selfCareAuthority = selfCareAuthority;
    }

    public static List<SelfCareRole> fromSelfCareAuthority(String selfCareAuthority) {
        return Stream.of(SelfCareRole.values())
                .filter(role -> role.getSelfCareAuthority().name().equals(selfCareAuthority))
                .toList();
    }
}
