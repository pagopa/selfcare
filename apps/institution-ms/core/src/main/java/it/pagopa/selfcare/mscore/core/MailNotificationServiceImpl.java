package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.api.EmailConnector;
import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.ProductConnector;
import it.pagopa.selfcare.mscore.api.UserApiConnector;
import it.pagopa.selfcare.mscore.config.CoreConfig;
import it.pagopa.selfcare.mscore.core.util.MailParametersMapper;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailNotificationServiceImpl implements MailNotificationService {

    private static final String MAIL_PARAMETER_LOG = "mailParameters: {}";
    private static final String DESTINATION_MAIL_LOG = "destinationMails: {}";
    private final InstitutionConnector institutionConnector;
    private final UserApiConnector userApiConnector;
    private final ProductConnector productConnector;
    private final UserNotificationService userNotificationService;
    private final EmailConnector emailConnector;
    private final MailParametersMapper mailParametersMapper;
    private final CoreConfig coreConfig;

    public void sendMailForDelegation(String institutionName, String productId, String partnerId) {
        try {
            Map<String, String> mailParameters;
            Product product = productConnector.getProductById(productId);
            Institution partnerInstitution = institutionConnector.findById(partnerId);
            if(Objects.isNull(product) || Objects.isNull(partnerInstitution)) {
                log.error("create-delegation-email-notification :: Impossible to send email. Error: partner institution or product is null");
                return;
            }
            mailParameters = mailParametersMapper.getDelegationNotificationParameter(institutionName, product.getTitle(), partnerInstitution.getDescription());
            log.debug(MAIL_PARAMETER_LOG, mailParameters);
            List<String> userDestinationMail = getUsersEmailByInstitutionAndProductV2(partnerInstitution.getId(), productId);
            log.info(DESTINATION_MAIL_LOG, userDestinationMail);
            userNotificationService.sendDelegationUserNotification(userDestinationMail, mailParametersMapper.getDelegationUserNotificationPath(), product.getTitle(), mailParameters);
            log.info("create-delegation-user-email-notification :: Email successful sent");

            if (coreConfig.isEnableSendDelegationMail()) {
                List<String> institutionDestinationMail = getDestinationMails(partnerInstitution);
                log.info(DESTINATION_MAIL_LOG, institutionDestinationMail);
                emailConnector.sendMail(mailParametersMapper.getDelegationNotificationPath(), institutionDestinationMail, null, product.getTitle(), mailParameters, null);
                log.info("create-delegation-institution-email-notification :: Email successful sent");
            }
        } catch (Exception e) {
            log.error("create-delegation-email-notification :: Impossible to send email. Error: {}", e.getMessage(), e);
        }
    }

    private List<String> getDestinationMails(Institution institution) {
        if (coreConfig.isSendEmailToInstitution()) {
            return List.of(institution.getDigitalAddress());
        } else {
            return List.of(coreConfig.getInstitutionAlternativeEmail());
        }
    }


    private List<String> getUsersEmailByInstitutionAndProductV2(String institutionId, String productId){
        if (coreConfig.isSendEmailToInstitution()) {
            return userApiConnector.getUserEmails(institutionId, productId);
        }
        return List.of(coreConfig.getInstitutionAlternativeEmail());
    }
}
