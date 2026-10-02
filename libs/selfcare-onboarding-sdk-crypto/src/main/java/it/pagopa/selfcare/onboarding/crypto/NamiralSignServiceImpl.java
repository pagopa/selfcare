package it.pagopa.selfcare.onboarding.crypto;

import it.pagopa.selfcare.onboarding.crypto.client.NamirialHttpClient;
import it.pagopa.selfcare.onboarding.crypto.entity.Credentials;
import it.pagopa.selfcare.onboarding.crypto.entity.Preferences;
import it.pagopa.selfcare.onboarding.crypto.entity.SignRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public class NamiralSignServiceImpl implements NamirialSignService {

    private final NamirialHttpClient namirialHttpClient;
    private final String username;
    private final String password;

    // Constructor for manual dependency injection
    public NamiralSignServiceImpl() {
        this(new NamirialHttpClient(),
                System.getenv("NAMIRIAL_SIGN_SERVICE_IDENTITY_USER"),
                System.getenv("NAMIRIAL_SIGN_SERVICE_IDENTITY_PASSWORD"));
    }

    public NamiralSignServiceImpl(String username, String password, String baseUrl) {
        this(new NamirialHttpClient(baseUrl), username, password);
    }

    public NamiralSignServiceImpl(NamirialHttpClient namirialHttpClient, String username, String password) {
        this.namirialHttpClient = namirialHttpClient;
        this.username = username;
        this.password = password;
    }

    @Override
    public byte[] pkcs7Signhash(InputStream is) {
        try {

            Path tempFilePath = Files.createTempFile("tempfile", ".pdf");
            File tempFile = tempFilePath.toFile();

            // Copy InputStream data to the temporary file
            Files.copy(is, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

            Credentials credentials = new Credentials(username, password);
            Preferences preferences = new Preferences("SHA256");
            SignRequest request = new SignRequest(tempFile, credentials, preferences);

            return namirialHttpClient.signDocument(request);
        } catch (IOException e) {
            throw new IllegalStateException("Something gone wrong when invoking Namirial in order to calculate pkcs7 hash sign request", e);
        }
    }
}