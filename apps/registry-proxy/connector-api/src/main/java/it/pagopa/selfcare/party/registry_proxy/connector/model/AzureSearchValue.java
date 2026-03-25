package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

@Data
public class AzureSearchValue {
  String key;
  Boolean status;
  String errorMessage;
  Number statusCode;
}
