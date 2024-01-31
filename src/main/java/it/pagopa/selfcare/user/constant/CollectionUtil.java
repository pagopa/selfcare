package it.pagopa.selfcare.user.constant;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class CollectionUtil {

    public static final String USER_INSTITUTION_COLLECTION = "userInstitutions";
    public static final String USER_INFO_COLLECTION = "userInfo";
    public static final String CURRENT = ".$.";
    public static final String CURRENT_ANY = ".$[].";
}
