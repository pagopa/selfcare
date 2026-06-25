package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PDNDImpresaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapBusinessStatusFromStatoDitta() throws Exception {
        // given
        String payload = """
                {
                  "CodiceFiscale": "12345678901",
                  "StatoDitta": "Cessata/Cancellata"
                }
                """;

        // when
        PDNDImpresa result = objectMapper.readValue(payload, PDNDImpresa.class);

        // then
        assertThat(result.getBusinessTaxId()).isEqualTo("12345678901");
        assertThat(result.getBusinessStatus()).isEqualTo("Cessata/Cancellata");
    }

    @Test
    void shouldMapBusinessStatusFromStatoImpresa() throws Exception {
        // given
        String payload = """
                {
                  "CodiceFiscale": "12345678901",
                  "StatoImpresa": "Attiva"
                }
                """;

        // when
        PDNDImpresa result = objectMapper.readValue(payload, PDNDImpresa.class);

        // then
        assertThat(result.getBusinessTaxId()).isEqualTo("12345678901");
        assertThat(result.getBusinessStatus()).isEqualTo("Attiva");
    }
}
