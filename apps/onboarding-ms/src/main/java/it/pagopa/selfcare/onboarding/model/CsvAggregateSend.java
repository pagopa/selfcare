package it.pagopa.selfcare.onboarding.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class CsvAggregateSend implements Csv {

    @CsvBindByPosition(position = 0)
    private String taxCode;

    @CsvBindByPosition(position = 1)
    private String vatNumber;

    @CsvBindByPosition(position = 2)
    private String recipientCode;

    @CsvBindByPosition(position = 3)
    private String subunitType;

    @CsvBindByPosition(position = 4)
    private String subunitCode;

    @CsvBindByPosition(position = 5)
    private String adminAggregateName;

    @CsvBindByPosition(position = 6)
    private String adminAggregateSurname;

    @CsvBindByPosition(position = 7)
    private String adminAggregateTaxCode;

    @CsvBindByPosition(position = 8)
    private String adminAggregateEmail;

    private Integer rowNumber;
    @Override
    public void setRowNumber(int lineNumber) {
        this.rowNumber = lineNumber;
    }

}
