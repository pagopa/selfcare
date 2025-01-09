package it.pagopa.selfcare.user_group.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtData {

    private String username;
    private String password;

    Map<String, Object> jwtHeader;
    Map<String, String> jwtPayload;

}
