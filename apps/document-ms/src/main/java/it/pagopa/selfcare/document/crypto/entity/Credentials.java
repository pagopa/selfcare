package it.pagopa.selfcare.document.crypto.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Credentials {
    public String username;
    public String password;

    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

}
