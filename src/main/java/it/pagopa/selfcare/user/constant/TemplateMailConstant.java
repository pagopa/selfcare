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
    public static final String CREATE_TEMPLATE_MULTIPLE_ROLE = "user_added_multi_role.ftlh";
    public static final String CREATE_SUBJECT = "Hai un nuovo ruolo per un prodotto PagoPA";
    public static final String CREATE_TEMPLATE_SINGLE_ROLE = "user_added_single_role.ftlh";
}
