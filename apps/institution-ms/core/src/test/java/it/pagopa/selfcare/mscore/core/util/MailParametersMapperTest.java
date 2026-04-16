package it.pagopa.selfcare.mscore.core.util;

import it.pagopa.selfcare.mscore.config.MailTemplateConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class MailParametersMapperTest {

    @InjectMocks
    private MailParametersMapper mailParametersMapper;

    @Mock
    private MailTemplateConfig mailTemplateConfig;

    @Test
    void getNotificationDelegationPath(){
        when(mailTemplateConfig.getDelegationNotificationPath()).thenReturn("path");
        Assertions.assertNotNull(mailParametersMapper.getDelegationNotificationPath());
    }

    @Test
    void getNotificationDelegationParameters(){
        Assertions.assertNotNull(mailParametersMapper.getDelegationNotificationParameter("productName","productId", "partnerName"));
    }

}

