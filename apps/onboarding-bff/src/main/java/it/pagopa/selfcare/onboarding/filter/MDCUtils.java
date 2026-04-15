package it.pagopa.selfcare.onboarding.filter;

import org.slf4j.MDC;

public class MDCUtils {

  public static void addOperationIdAndParameters(String operationId) {
    MDC.put("sc_operation_id", operationId);
  }
}
