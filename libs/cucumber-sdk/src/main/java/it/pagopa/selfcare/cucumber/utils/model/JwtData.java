package it.pagopa.selfcare.cucumber.utils.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtData {

    private String username;
    private String password;

    private Map<String, Object> jwtHeader;
    private Map<String, Object> jwtPayload;

}
