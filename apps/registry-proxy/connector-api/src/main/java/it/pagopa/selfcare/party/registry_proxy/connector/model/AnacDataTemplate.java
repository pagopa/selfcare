package it.pagopa.selfcare.party.registry_proxy.connector.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "taxCode")
public class AnacDataTemplate implements Station {
    @CsvBindByName(column = "codiceIPA")
    private String originId;
    @CsvBindByName(column = "codiceFiscaleGestore")
    private String taxCode;
    @CsvBindByName(column = "denominazioneGestore")
    private String description;
    @CsvBindByName(column = "PEC")
    private String digitalAddress;
    @CsvBindByName(column = "ANAC_incaricato")
    private String anacEngaged;
    @CsvBindByName(column = "ANAC_abilitato")
    private String anacEnabled;

    public String getTaxCode() {
        if(this.taxCode.length() < 11) {
            return StringUtils.leftPad(this.taxCode, 11, "0");
        }
        return this.taxCode;
    }
}