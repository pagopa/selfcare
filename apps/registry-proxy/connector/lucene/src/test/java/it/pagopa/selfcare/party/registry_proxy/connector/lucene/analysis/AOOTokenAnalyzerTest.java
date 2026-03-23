package it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AOOTokenAnalyzerTest {

    @Test
    void createComponents(){
        AOOTokenAnalyzer aooTokenAnalyzer = new AOOTokenAnalyzer();
        assertNotNull(aooTokenAnalyzer.createComponents("field"));
    }
}
