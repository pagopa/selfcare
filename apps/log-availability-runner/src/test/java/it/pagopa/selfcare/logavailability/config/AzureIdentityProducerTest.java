package it.pagopa.selfcare.logavailability.config;

import com.azure.identity.ManagedIdentityCredential;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AzureIdentityProducerTest {

    @Test
    void producesAManagedIdentityCredential() {
        assertInstanceOf(ManagedIdentityCredential.class,
                new AzureIdentityProducer().managedIdentityCredential());
    }
}
