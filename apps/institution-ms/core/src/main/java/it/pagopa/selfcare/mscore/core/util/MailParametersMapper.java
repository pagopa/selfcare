package it.pagopa.selfcare.mscore.core.util;

import it.pagopa.selfcare.mscore.config.MailTemplateConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MailParametersMapper {

    @Autowired
    MailTemplateConfig mailTemplateConfig;

    public Map<String, String> getDelegationNotificationParameter(String institutionName, String productName, String partnerName) {
        Map<String, String> map = new HashMap<>();
        map.put(mailTemplateConfig.getNotificationProductName(), productName);
        map.put(mailTemplateConfig.getInstitutionDescription(), institutionName);
        map.put(mailTemplateConfig.getDelegationPartnerName(), partnerName);
        return map;
    }

    public String getDelegationNotificationPath() {
        return mailTemplateConfig.getDelegationNotificationPath();
    }

    public String getDelegationUserNotificationPath() {
        return mailTemplateConfig.getDelegationUserNotificationPath();
    }

}
