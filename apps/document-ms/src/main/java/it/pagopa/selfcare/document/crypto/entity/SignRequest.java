package it.pagopa.selfcare.document.crypto.entity;

import com.google.api.client.util.Key;
import lombok.Getter;

import java.io.File;

@Getter
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

}
