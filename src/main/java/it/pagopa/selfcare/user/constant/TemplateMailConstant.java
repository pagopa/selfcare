package it.pagopa.selfcare.user.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.NONE)
public class TemplateMailConstant {

    public static final String ACTIVATE_SUBJECT = "User has been activated";
    public static final String DELETE_SUBJECT = "User had been deleted";
    public static final String SUSPEND_SUBJECT = "User has been suspended";
    public static final String ACTIVATE_TEMPLATE = "user_activated.ftlh";
    public static final String DELETE_TEMPLATE = "user_deleted.ftlh";
    public static final String SUSPEND_TEMPLATE = "user_suspended.ftlh";
}
