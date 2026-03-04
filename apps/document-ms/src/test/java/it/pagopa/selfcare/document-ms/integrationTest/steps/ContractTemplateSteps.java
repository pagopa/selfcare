package it.pagopa.selfcare.product.integrationTest.steps;

import io.cucumber.java.en.And;
import it.pagopa.selfcare.product.repository.ContractTemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ContractTemplateSteps {

  @Inject private ContractTemplateRepository contractTemplateRepository;

  @And(
      "Finally remove the uploaded contract template with name {string} and version {string} for product {string}")
  public void removeUploadedContractTemplate(String name, String version, String productId) {
    contractTemplateRepository
        .delete("name = ?1 and version = ?2 and productId = ?3", name, version, productId)
        .await()
        .indefinitely();
  }
}
