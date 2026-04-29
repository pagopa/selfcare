package it.pagopa.selfcare.mscore.api;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface EmailConnector {

    void sendMail(String templateName, List<String> destinationMail, File pdf, String productName, Map<String, String> mailParameters, String nameFile);

}
