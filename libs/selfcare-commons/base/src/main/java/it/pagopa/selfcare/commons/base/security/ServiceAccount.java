package it.pagopa.selfcare.commons.base.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.util.Assert;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class ServiceAccount implements AuthenticatedPrincipal {

    private final String id;
    private String username;

    private ServiceAccount(String id) {
        Assert.notNull(id, "Service Account id is required");
        this.id = id;
    }

    public static ServiceAccountBuilder builder(String id) {
        return new ServiceAccountBuilder(id);
    }

    @Override
    public String getName() {
        return id;
    }


    public static class ServiceAccountBuilder {

        private final String id;
        private String username;

        private ServiceAccountBuilder(String id) {
            this.id = id;
        }

        public ServiceAccountBuilder username(String username) {
            this.username = username;
            return this;
        }

        public ServiceAccount build() {
            ServiceAccount user = new ServiceAccount(id);
            user.username = username;
            return user;
        }

    }

}
