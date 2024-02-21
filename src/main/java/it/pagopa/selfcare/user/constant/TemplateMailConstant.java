package it.pagopa.selfcare.user.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.NONE)
public class TemplateMailConstant {

    public static final String ACTIVATE_SUBJECT = "Il tuo ruolo è stato riabilitato";
    public static final String DELETE_SUBJECT = "Il tuo ruolo è stato rimosso";
    public static final String SUSPEND_SUBJECT = "Il tuo ruolo è sospeso";
    public static final String ACTIVATE_TEMPLATE = "user_activated.ftlh";
    public static final String DELETE_TEMPLATE = "user_deleted.ftlh";
    public static final String SUSPEND_TEMPLATE = "user_suspended.ftlh";
}
