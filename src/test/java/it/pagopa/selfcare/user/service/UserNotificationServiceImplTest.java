package it.pagopa.selfcare.user.service;

import com.microsoft.applicationinsights.TelemetryClient;
import freemarker.template.Configuration;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.user.client.EventHubRestClient;
import it.pagopa.selfcare.user.conf.CloudTemplateLoader;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapi.quarkus.user_registry_json.model.EmailCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.NameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.selfcare.user.model.constants.EventsMetric.EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS;
import static it.pagopa.selfcare.user.model.constants.EventsName.EVENT_USER_MS_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class UserNotificationServiceImplTest {
    @Inject
    UserNotificationService userNotificationService;

    @InjectMock
    private MailService mailService;

    @InjectMock
    @RestClient
    private EventHubRestClient eventHubRestClient;

    @InjectMock
    TelemetryClient telemetryClient;

    private static final UserResource userResource;
    private static final UserInstitution userInstitution;
    private static final Product product;

    @Produces
    @ApplicationScoped
    Configuration freemarkerConfig() {
        return mock(Configuration.class);
    }

    static {
        userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        NameCertifiableSchema certifiedName = new NameCertifiableSchema();
        org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema certifiedFamilyName = new org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema();
        certifiedName.setValue("name");
        certifiedFamilyName.setValue("familyName");
        userResource.setName(certifiedName);
        userResource.setFamilyName(certifiedFamilyName);
        userResource.setFiscalCode("taxCode");
        EmailCertifiableSchema certifiedEmail = new EmailCertifiableSchema();
        certifiedEmail.setValue("test@test.it");
        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.setEmail(certifiedEmail);
        userResource.setEmail(certifiedEmail);
        userResource.setWorkContacts(Map.of("IdMail", workContactResource));

        userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId("userId");
        userInstitution.setUserMailUuid("IdMail");
        userInstitution.setInstitutionId("institutionId");
        userInstitution.setInstitutionRootName("institutionRootName");
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId("test");
        onboardedProduct.setProductRole("admin");
        userInstitution.setProducts(List.of(onboardedProduct));


        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        var productRole = new ProductRole();
        productRole.setCode("code");
        productRole.setDescription("description");
        productRole.setLabel("label");
        productRoleInfo.setRoles(List.of(productRole));

        ProductRoleInfo productRoleInfo2 = new ProductRoleInfo();
        var productRole2 = new ProductRole();
        productRole2.setCode("code2");
        productRole2.setDescription("description");
        productRole2.setLabel("label2");
        var productRole3 = new ProductRole();
        productRole3.setCode("code3");
        productRole3.setDescription("description");
        productRole3.setLabel("label3");
        productRoleInfo2.setRoles(List.of(productRole2, productRole3));

        product = new Product();
        product.setId("test");
        product.setRoleMappings(new HashMap<>() {{
            put(PartyRole.MANAGER, productRoleInfo);
            put(PartyRole.OPERATOR, productRoleInfo2);
        }});
    }

    @Test
    void testSendMailNotificationForActivateUserProduct() throws IOException {
        String loggedUserName = "loggedUserName";
        String loggedUserSurname = "loggedUserSurname";

        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);
        when(mailService.sendMail(anyString(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        userNotificationServiceImpl.sendEmailNotification(
                        userResource,
                        userInstitution,
                        product,
                        OnboardedProductState.ACTIVE,
                        "admin",
                        loggedUserName,
                        loggedUserSurname
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(1)).sendMail(anyString(), anyString(), anyString());

    }

    @Test
    void testSendMailNotificationForDeleteUserProduct() throws IOException {
        String loggedUserName = "loggedUserName";
        String loggedUserSurname = "loggedUserSurname";

        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);
        when(mailService.sendMail(anyString(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        userNotificationServiceImpl.sendEmailNotification(
                        userResource,
                        userInstitution,
                        product,
                        OnboardedProductState.DELETED,
                        "admin",
                        loggedUserName,
                        loggedUserSurname
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(1)).sendMail(anyString(), anyString(), anyString());

    }


    @Test
    void testSendMailNotificationForSuspendUserProduct() throws IOException {
        String loggedUserName = "loggedUserName";
        String loggedUserSurname = "loggedUserSurname";

        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);
        when(mailService.sendMail(anyString(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        userNotificationServiceImpl.sendEmailNotification(
                        userResource,
                        userInstitution,
                        product,
                        OnboardedProductState.SUSPENDED,
                        "admin",
                        loggedUserName,
                        loggedUserSurname
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(1)).sendMail(anyString(), anyString(), anyString());
    }


    @Test
    void testSendMailNotificationForRejectUserProduct() throws IOException {
        String loggedUserName = "loggedUserName";
        String loggedUserSurname = "loggedUserSurname";

        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);

        userNotificationServiceImpl.sendEmailNotification(
                        userResource,
                        userInstitution,
                        product,
                        OnboardedProductState.REJECTED,
                        "admin",
                        loggedUserName,
                        loggedUserSurname
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(0)).sendMail(anyString(), anyString(), anyString());
    }


    @Test
    void testSendMailNotificationWithNullInstitutionDescription() throws IOException {
        String loggedUserName = "loggedUserName";
        String loggedUserSurname = "loggedUserSurname";

        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);
        when(mailService.sendMail(anyString(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        userNotificationServiceImpl.sendEmailNotification(
                        userResource,
                        userInstitution,
                        product,
                        OnboardedProductState.ACTIVE,
                        "admin",
                        loggedUserName,
                        loggedUserSurname
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        userNotificationServiceImpl.sendEmailNotification(
                        userResource,
                        userInstitution,
                        product,
                        OnboardedProductState.DELETED,
                        "admin",
                        loggedUserName,
                        loggedUserSurname
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        userNotificationServiceImpl.sendEmailNotification(
                        userResource,
                        userInstitution,
                        product,
                        OnboardedProductState.SUSPENDED,
                        "admin",
                        loggedUserName,
                        loggedUserSurname
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        userNotificationServiceImpl.sendEmailNotification(
                        userResource,
                        userInstitution,
                        product,
                        OnboardedProductState.PENDING,
                        "admin",
                        loggedUserName,
                        loggedUserSurname
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(3)).sendMail(anyString(), anyString(), anyString());
    }


    @Test
    void testSendKafkaNotification(){
        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setId("userId");

        when(eventHubRestClient.sendMessage(any())).thenReturn(Uni.createFrom().voidItem());

        UniAssertSubscriber<UserNotificationToSend> subscriber = userNotificationService.sendKafkaNotification(
                userNotificationToSend
        ).subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
        verify(eventHubRestClient, times(1)).sendMessage(userNotificationToSend);
        ArgumentCaptor<Map<String, Double>> metricsName = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient, times(1)).trackEvent(eq(EVENT_USER_MS_NAME), any(), metricsName.capture());
        assertEquals(EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS, metricsName.getValue().keySet().stream().findFirst().orElse(null));

    }
    @Test
    void testSendCreateUserNotification() throws IOException {
        String loggedUserName = "loggedUserName";
        String loggedUserSurname = "loggedUserSurname";
        LoggedUser loggedUser = LoggedUser.builder()
                .name(loggedUserName)
                .familyName(loggedUserSurname)
                .build();

        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);

        when(mailService.sendMail(anyString(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());
        List<String> roleLabels = List.of("code2", "code3");
        userNotificationServiceImpl.sendCreateUserNotification(
                        userInstitution.getInstitutionDescription(),
                        roleLabels,
                        userResource,
                        userInstitution,
                        product,
                        loggedUser
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(1)).sendMail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendCreateUserNotificationWith2RoleLabel() throws IOException {
        String loggedUserName = "loggedUserName";
        String loggedUserSurname = "loggedUserSurname";
        LoggedUser loggedUser = LoggedUser.builder()
                .name(loggedUserName)
                .familyName(loggedUserSurname)
                .build();

        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);

        when(mailService.sendMail(anyString(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());
        List<String> roleLabels = List.of("code2", "code3");
        userNotificationServiceImpl.sendCreateUserNotification(
                        userInstitution.getInstitutionDescription(),
                        roleLabels,
                        userResource,
                        userInstitution,
                        product,
                        loggedUser
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(1)).sendMail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendCreateUserNotificationWithRoleNotFound() throws IOException {
        String loggedUserName = "loggedUserName";
        String loggedUserSurname = "loggedUserSurname";
        LoggedUser loggedUser = LoggedUser.builder()
                .name(loggedUserName)
                .familyName(loggedUserSurname)
                .build();

        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);

        when(mailService.sendMail(anyString(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());
        List<String> roleLabels = List.of("code5","code6");
        userNotificationServiceImpl.sendCreateUserNotification(
                        userInstitution.getInstitutionDescription(),
                        roleLabels,
                        userResource,
                        userInstitution,
                        product,
                        loggedUser
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(1)).sendMail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendCreateUserNotificationWithNullInstitutionDescription() throws IOException {
        String loggedUserName = "loggedUserName";
        String loggedUserSurname = "loggedUserSurname";
        LoggedUser loggedUser = LoggedUser.builder()
                .name(loggedUserName)
                .familyName(loggedUserSurname)
                .build();

        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);

        when(mailService.sendMail(anyString(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());
        List<String> roleLabels = List.of("label");
        userNotificationServiceImpl.sendCreateUserNotification(
                        null,
                        roleLabels,
                        userResource,
                        userInstitution,
                        product,
                        loggedUser
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(1)).sendMail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendMailNotificationForOnboardingRequest() throws IOException {
        String loggedUserName = "loggedUserName";
        String loggedUserSurname = "loggedUserSurname";

        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);
        when(mailService.sendMail(anyString(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        userNotificationServiceImpl.buildDataModelRequestAndSendEmail(
                        userResource,
                        userInstitution,
                        product,
                        PartyRole.MANAGER,
                        loggedUserName,
                        loggedUserSurname
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(1)).sendMail(anyString(), anyString(), anyString());

    }

    @Test
    void testSendMailNotificationForOtp() throws IOException {
        Configuration freemarkerConfig = mock(Configuration.class);
        CloudTemplateLoader cloudTemplateLoader = mock(CloudTemplateLoader.class);
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mock(freemarker.template.Template.class));
        when(freemarkerConfig.getTemplateLoader()).thenReturn(cloudTemplateLoader);

        UserNotificationServiceImpl userNotificationServiceImpl = new UserNotificationServiceImpl(freemarkerConfig, cloudTemplateLoader, mailService, true, telemetryClient);
        when(mailService.sendMail(anyString(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        userNotificationServiceImpl.sendOtpNotification(
                        "test@test.com", "name", "123456"
                )
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted();
        verify(mailService, times(1)).sendMail(anyString(), anyString(), anyString());

    }
}
