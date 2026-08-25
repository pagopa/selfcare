package it.pagopa.selfcare.commons.base.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.AuthenticatedPrincipal;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class SelfCareUser implements AuthenticatedPrincipal {

    private final String id;
    private String email;
    private String surname;
    private String userName;
    private String fiscalCode;
    private String issuer;

    private SelfCareUser(String id) {
        this.id = id;
    }

    public static SelfCareUserBuilder builder(String id) {
        return new SelfCareUserBuilder(id);
    }

    @Override
    public String getName() {
        return id;
    }

    public static class SelfCareUserBuilder {

        private final String id;
        private String email;
        private String name;
        private String surname;
        private String fiscalCode;
        private String issuer;

        private SelfCareUserBuilder(String id) {
            this.id = id;
        }

        public SelfCareUserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public SelfCareUserBuilder name (String name){
            this.name = name;
            return this;
        }

        public SelfCareUserBuilder surname (String surname){
            this.surname = surname;
            return this;
        }

        public SelfCareUserBuilder fiscalCode(String fiscalCode){
            this.fiscalCode = fiscalCode;
            return this;
        }

        public SelfCareUserBuilder issuer(String issuer) {
          this.issuer = issuer;
          return this;
        }

        public SelfCareUser build() {
            SelfCareUser user = new SelfCareUser(id);
            user.email = email;
            user.userName = name;
            user.surname = surname;
            user.fiscalCode = fiscalCode;
            user.issuer = issuer;
            return user;
        }

    }

}
