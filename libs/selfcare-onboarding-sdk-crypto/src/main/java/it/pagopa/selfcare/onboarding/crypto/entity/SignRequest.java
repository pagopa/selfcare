package it.pagopa.selfcare.onboarding.crypto.entity;

import com.google.api.client.util.Key;

import java.io.File;

public class SignRequest {

    @Key("file")
    private File file;

    @Key("credentials")
    private Credentials credentials;

    @Key("preferences")
    private Preferences preferences;

    // Constructors, getters, and setters
    public SignRequest(File fileContent, Credentials credentials, Preferences preferences) {
        this.file = fileContent;
        this.preferences = preferences;
        this.credentials = credentials;
    }

    public File getFile() {
        return file;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public Preferences getPreferences() {
        return preferences;
    }
}