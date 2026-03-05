package it.pagopa.selfcare.onboarding.service.util;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.model.AggregatesCsv;
import it.pagopa.selfcare.onboarding.model.Csv;
import it.pagopa.selfcare.onboarding.model.RowError;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.opencsv.ICSVParser.DEFAULT_QUOTE_CHARACTER;

@ApplicationScoped
@Slf4j
public class CsvUtils {

    public static final String ERROR_READING_CSV = "Error reading CSV: ";
    public static final String MALFORMED_ROW = "Riga malformata";

    public static final String NOTE_PREFIX = "Note:";
    public static final String REQUIREMENT_PREFIX = "(*";
    public static final char SEPARATOR = ';';

    public <T extends Csv> AggregatesCsv<T> readItemsFromCsv(File file, Class<T> csv) {
        List<Csv> resultList = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            StringReader stringReader = new StringReader(new String(fileBytes, StandardCharsets.UTF_8));
            BufferedReader bufferedReader = new BufferedReader(stringReader);
            String skip = bufferedReader.readLine();
            log.info("Skip header: " + skip);
            int lineNumber = 1;
            String nextLine;

            while ((nextLine = bufferedReader.readLine()) != null) {
                if (!nextLine.startsWith(REQUIREMENT_PREFIX) && !nextLine.startsWith(NOTE_PREFIX) && !isBlankRow(nextLine)) {
                    parseLine(nextLine, lineNumber, resultList, errors, csv);
                    lineNumber++;
                }
            }

            return new AggregatesCsv(resultList, errors);

        } catch (Exception e) {
            log.error(ERROR_READING_CSV + "{}", e.getMessage(), e);
            throw new InvalidRequestException(ERROR_READING_CSV + e.getMessage());
        }
    }

    private static boolean isBlankRow(String line) {
        String[] values = line.split(String.valueOf(SEPARATOR));
        for (String value : values) {
            if (!value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private <T extends Csv> void parseLine(String nextLine, int lineNumber, List<Csv> resultList, List<RowError> errors, Class<T> csv) {
        try {
            StringReader lineReader = new StringReader(nextLine);
            CsvToBean<Csv> csvToBean = getAggregateCsvToBean(new BufferedReader(lineReader), csv);
            List<Csv> csvAggregateList = csvToBean.parse();
            if (!csvAggregateList.isEmpty()) {
                Csv csvAggregate = csvAggregateList.get(0);
                csvAggregate.setRowNumber(lineNumber);
                resultList.add(csvAggregateList.get(0));
            }
            log.debug("Row " + lineNumber + ": ");
        } catch (Exception e) {
            log.error("Error to the row " + lineNumber + ": " + e.getMessage());
            errors.add(new RowError(lineNumber, "", MALFORMED_ROW));
        }
    }

    private <T extends Csv> CsvToBean<Csv> getAggregateCsvToBean(BufferedReader bufferedReader, Class<T> csv) {
        CsvToBeanBuilder<Csv> csvToBeanBuilder = new CsvToBeanBuilder<>(bufferedReader);
        csvToBeanBuilder.withType(csv);
        csvToBeanBuilder.withSeparator(SEPARATOR);
        csvToBeanBuilder.withQuoteChar(DEFAULT_QUOTE_CHARACTER);
        csvToBeanBuilder.withOrderedResults(true);
        csvToBeanBuilder.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS);
        csvToBeanBuilder.withThrowExceptions(false);
        return csvToBeanBuilder.build();
    }
}
