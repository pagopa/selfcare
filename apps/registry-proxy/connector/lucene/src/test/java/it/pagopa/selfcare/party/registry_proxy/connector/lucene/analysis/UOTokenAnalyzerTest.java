package it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UOTokenAnalyzerTest {

    @Test
    void createComponents(){
        UOTokenAnalyzer uoTokenAnalyzer = new UOTokenAnalyzer();
        assertNotNull(uoTokenAnalyzer.createComponents("field"));
    }
}
