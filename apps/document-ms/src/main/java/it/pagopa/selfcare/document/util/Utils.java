package it.pagopa.selfcare.document.util;

import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.model.FormItem;
import java.io.File;
import java.util.Deque;
import java.util.Objects;
import java.util.function.BinaryOperator;

import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.FormValue;

public class Utils {

    private static final String DEFAULT_ATTACHMENT_FORM_DATA_NAME = "file";

    private Utils() {
    }

    public static final BinaryOperator<String> CONTRACT_FILENAME_FUNC =
            (filename, productName) ->
                    String.format(filename, StringUtils.stripAccents(productName.replaceAll("\\s+", "_")));

    public static FormItem retrieveAttachmentFromFormData(FormData formData, File file) {
        Deque<FormValue> deck = formData.get(DEFAULT_ATTACHMENT_FORM_DATA_NAME);
        if (deck.size() > 1) {
            throw new InvalidRequestException("Too many attachments", "0000");
        }

        return FormItem.builder().file(file).fileName(deck.getFirst().getFileName()).build();
    }

    public static String getContractTemplatePath(String institutionType, Product product) {
        ContractTemplate contractTemplate = product.getInstitutionContractTemplate(
                institutionType);
        return contractTemplate.getContractTemplatePath();
    }

    public static String getContractTemplateVersion(String institutionType, Product product) {
        ContractTemplate contractTemplate = product.getInstitutionContractTemplate(
                institutionType);
        return contractTemplate.getContractTemplateVersion();
    }
}
