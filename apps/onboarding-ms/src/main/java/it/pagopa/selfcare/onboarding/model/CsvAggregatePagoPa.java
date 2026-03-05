package it.pagopa.selfcare.onboarding.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class CsvAggregatePagoPa implements Csv {

    @CsvBindByPosition(position = 0)
    private String taxCode;

    @CsvBindByPosition(position = 1)
    private String vatNumber;

    @CsvBindByPosition(position = 2)
    private String iban;

    private Integer rowNumber;

    @Override
    public void setRowNumber(int lineNumber) {
        this.rowNumber = lineNumber;
    }
}
