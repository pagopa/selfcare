package it.pagopa.selfcare.party.registry_proxy.web.utils;

import it.pagopa.selfcare.party.registry_proxy.connector.constant.GenericError;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@NoArgsConstructor(access = AccessLevel.NONE)
public class CustomExceptionMessage {

    public static void setCustomMessage(GenericError genericError){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        request.setAttribute("errorEnum", genericError);
    }
}
