package it.pagopa.selfcare.mscore.connector.email;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition.PecCredentials;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantMailSenderProvider {

    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;
    private final boolean smtpAuth;
    private final boolean smtpSsl;
    private final String protocol;
    private final Map<String, JavaMailSenderImpl> senders = new ConcurrentHashMap<>();

    public TenantMailSenderProvider(TenantRegistry tenantRegistry, TenantContext tenantContext,
                                   @Value("${spring.mail.properties.mail.smtp.auth}") boolean smtpAuth,
                                   @Value("${spring.mail.properties.mail.smtp.starttls.enable}") boolean smtpSsl,
                                   @Value("${spring.mail.protocol}") String protocol) {
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
        this.smtpAuth = smtpAuth;
        this.smtpSsl = smtpSsl;
        this.protocol = protocol;
    }

    public JavaMailSenderImpl currentSender() {
        String tenantId = tenantRegistry.normalizeAndValidate(tenantContext.requiredTenantId());
        return senders.computeIfAbsent(tenantId, id -> createSender(tenantRegistry.pecCredentials(id)));
    }

    public String currentFrom() {
        return tenantRegistry.pecCredentials(tenantContext.requiredTenantId()).sender();
    }

    private JavaMailSenderImpl createSender(PecCredentials settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setUsername(settings.username());
        sender.setPassword(settings.password());
        sender.setProtocol(protocol);
        sender.getJavaMailProperties().setProperty("mail.smtp.auth", Boolean.toString(smtpAuth));
        sender.getJavaMailProperties().setProperty("mail.smtp.starttls.enable", Boolean.toString(smtpSsl));
        return sender;
    }
}
