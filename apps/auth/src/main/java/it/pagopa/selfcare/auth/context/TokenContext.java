package it.pagopa.selfcare.auth.context;

import jakarta.enterprise.context.RequestScoped;
import lombok.Getter;

@RequestScoped
@Getter
public class TokenContext {
  private String token;

  public String setToken(String token) {
    this.token = token;
    return token;
  }
}