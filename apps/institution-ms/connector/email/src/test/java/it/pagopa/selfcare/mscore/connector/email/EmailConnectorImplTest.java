package it.pagopa.selfcare.mscore.connector.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.mscore.api.FileStorageConnector;
import it.pagopa.selfcare.mscore.config.CoreConfig;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.model.onboarding.MailTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class EmailConnectorImplTest {

    @InjectMocks
    private EmailConnectorImpl emailConnector;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private FileStorageConnector fileStorageConnector;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private CoreConfig coreConfig;

    @Test
    void testSendMail() throws IOException {
        when(fileStorageConnector.getTemplateFile(any())).thenReturn("templateFile");
        MailTemplate mailTemplate = new MailTemplate();
        Path path = Files.createTempFile("contratto", ".pdf");
        File pdf = new File(path.toUri());
        String fileName = "text.pdf";
        byte [] bytes = emailConnector.zipBytes(fileName, pdf);
        assertNotNull(bytes);
        mailTemplate.setBody("body");
        mailTemplate.setSubject("subject");
        when(mapper.readValue("templateFile", MailTemplate.class)).thenReturn(mailTemplate);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(coreConfig.getSenderMail()).thenReturn("senderMail");

        ArrayList<String> destinationMail = new ArrayList<>();

        Assertions.assertDoesNotThrow(() -> emailConnector.sendMail("Template Name", destinationMail, pdf, "Product Name", new HashMap<>(), "foo.txt"));
    }

    @Test
    void testSendMailWithoutAttachment() throws JsonProcessingException {
        when(fileStorageConnector.getTemplateFile(any())).thenReturn("templateFile");
        MailTemplate mailTemplate = new MailTemplate();
        mailTemplate.setBody("body");
        mailTemplate.setSubject("subject");
        when(mapper.readValue("templateFile", MailTemplate.class)).thenReturn(mailTemplate);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(coreConfig.getSenderMail()).thenReturn("senderMail");

        ArrayList<String> destinationMail = new ArrayList<>();
        Assertions.assertDoesNotThrow(() -> emailConnector.sendMail("Template Name", destinationMail, null, "Product Name", new HashMap<>(), null));
    }

    @Test
    void testSendMail2() {
        FileStorageConnector fileStorageConnector = mock(FileStorageConnector.class);
        when(fileStorageConnector.getTemplateFile(any())).thenReturn("Template File");
        ObjectMapper mapper = new ObjectMapper();
        EmailConnectorImpl emailConnectorImpl = new EmailConnectorImpl(null, fileStorageConnector, mapper,
                new CoreConfig());
        ArrayList<String> destinationMail = new ArrayList<>();
        File pdf = Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toFile();
        assertThrows(MsCoreException.class, () -> emailConnectorImpl.sendMail("Template Name", destinationMail, pdf,
                "Product Name", new HashMap<>(), "foo.txt"));
        verify(fileStorageConnector).getTemplateFile(any());
    }

}

