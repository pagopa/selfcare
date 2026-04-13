package it.pagopa.selfcare.commons.base.security;

import java.security.Principal;
import java.util.Objects;

public class SelfCareUser implements Principal {

    private final String id;
    private final String email;
    private final String surname;
    private final String userName;
    private final String fiscalCode;
    private final String issuer;

    private SelfCareUser(SelfCareUserBuilder builder) {
        this.id = builder.id;
        this.email = builder.email;
        this.surname = builder.surname;
        this.userName = builder.userName;
        this.fiscalCode = builder.fiscalCode;
        this.issuer = builder.issuer;
    }

    public static SelfCareUserBuilder builder(String id) {
        return new SelfCareUserBuilder(id);
    }

    @Override
    public String getName() {
        return id;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getSurname() {
        return surname;
    }

    public String getUserName() {
        return userName;
    }

    public String getFiscalCode() {
        return fiscalCode;
    }

    public String getIssuer() {
        return issuer;
    }

    @Override
    public String toString() {
        return "SelfCareUser{id='" + id + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelfCareUser that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(email, that.email)
                && Objects.equals(surname, that.surname)
                && Objects.equals(userName, that.userName)
                && Objects.equals(fiscalCode, that.fiscalCode)
                && Objects.equals(issuer, that.issuer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, surname, userName, fiscalCode, issuer);
    }

    public static final class SelfCareUserBuilder {
        private final String id;
        private String email;
        private String surname;
        private String userName;
        private String fiscalCode;
        private String issuer;

        private SelfCareUserBuilder(String id) {
            this.id = id;
        }

        public SelfCareUserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public SelfCareUserBuilder surname(String surname) {
            this.surname = surname;
            return this;
        }

        public SelfCareUserBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public SelfCareUserBuilder fiscalCode(String fiscalCode) {
            this.fiscalCode = fiscalCode;
            return this;
        }

        public SelfCareUserBuilder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public SelfCareUser build() {
            return new SelfCareUser(this);
        }
    }
}
