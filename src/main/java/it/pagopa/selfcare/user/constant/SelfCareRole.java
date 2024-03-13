package it.pagopa.selfcare.user.constant;

public enum SelfCareRole {
    MANAGER(SelfCareAuthority.ADMIN),
    DELEGATE(SelfCareAuthority.ADMIN),
    SUB_DELEGATE(SelfCareAuthority.ADMIN),
    OPERATOR(SelfCareAuthority.LIMITED);

    private SelfCareAuthority selfCareAuthority;

    private SelfCareRole(SelfCareAuthority selfCareAuthority) {
        this.selfCareAuthority = selfCareAuthority;
    }

    public SelfCareAuthority getSelfCareAuthority() {
        return this.selfCareAuthority;
    }
}
