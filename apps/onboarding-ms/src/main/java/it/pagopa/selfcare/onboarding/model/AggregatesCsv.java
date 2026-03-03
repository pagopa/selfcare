package it.pagopa.selfcare.onboarding.model;

import java.util.ArrayList;
import java.util.List;

public class AggregatesCsv<T extends Csv>{
    private List<T> csvAggregateList;
    private List<T> validAggregates = new ArrayList<>();
    private List<RowError> rowErrorList;

    public AggregatesCsv(List<T> csvAggregateList, List<RowError> rowErrorList) {
        this.csvAggregateList = csvAggregateList;
        this.rowErrorList = rowErrorList;
    }

    public List<T> getCsvAggregateList() {
        return csvAggregateList;
    }

    public void setCsvAggregateList(List<T> Csv) {
        this.csvAggregateList = csvAggregateList;
    }

    public List<T> getValidAggregates() {
        return validAggregates;
    }

    public void setValidAggregates(List<T> Csv) {
        this.validAggregates = validAggregates;
    }

    public List<RowError> getRowErrorList() {
        return rowErrorList;
    }

    public void setRowErrorList(List<RowError> rowErrorList) {
        this.rowErrorList = rowErrorList;
    }

}
