package it.pagopa.selfcare.user.constant;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class CollectionUtil {

    public static final String CURRENT = ".$.";
    public static final String CURRENT_ANY = ".$[].";

    public static final String MAIL_ID_PREFIX = "ID_MAIL#";

    public static final String CONTACTS_ID_PREFIX = "ID_CONTACTS#";
}
