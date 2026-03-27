package it.pagopa.selfcare.document.util;

import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PricingPlan;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PN;

/**
 * Utility class for mapping data into PDF template placeholders.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PdfMapperData {

    private static final String UNDERSCORE = "_______________";
    private static final String CHECKBOX_X = "X";
    private static final String EMPTY_STR = "";
    private static final String[] PLAN_LIST = {"C1", "C2", "C3", "C4", "C5", "C6", "C7"};

    // Placeholder keys
    public static final String INSTITUTION_REA = "institutionREA";
    public static final String INSTITUTION_NAME = "institutionName";
    public static final String INSTITUTION_SHARE_CAPITAL = "institutionShareCapital";
    public static final String INSTITUTION_BUSINESS_REGISTER_PLACE = "institutionBusinessRegisterPlace";
    public static final String PRICING_PLAN_PREMIUM = "pricingPlanPremium";
    public static final String PRICING_PLAN_PREMIUM_CHECKBOX = "pricingPlanPremiumCheckbox";
    public static final String PRICING_PLAN_FAST_CHECKBOX = "pricingPlanFastCheckbox";
    public static final String PRICING_PLAN_BASE_CHECKBOX = "pricingPlanBaseCheckbox";
    public static final String PRICING_PLAN = "pricingPlan";
    public static final String INSTITUTION_REGISTER_LABEL_VALUE = "institutionRegisterLabelValue";
    public static final String CSV_AGGREGATES_LABEL_VALUE = "aggregatesCsvLink";
    public static final String INSTITUTION_RECIPIENT_CODE = "institutionRecipientCode";

    public static final String ORIGIN_ID_LABEL =
            "<li class=\"c19 c39 li-bullet-0\"><span class=\"c1\">codice di iscrizione all&rsquo;Indice delle Pubbliche Amministrazioni e dei gestori di pubblici servizi (I.P.A.) <span class=\"c3\">${originId}</span> </span><span class=\"c1\"></span></li>";
    public static final String CSV_AGGREGATES_LABEL =
            "&emsp;- <span class=\"c3\" style=\"color:blue\"><a class=\"c15\" href=\"%s\"><u>%s</u></a></span>";
    public static final String CSV_AGGREGATES_LABEL_SEND =
            "<span class=\"c3\" style=\"color:blue\"><a class=\"c15\" href=\"%s\"><u>%s</u></a></span>";
    public static final String CSV_AGGREGATES_TEXT = "Dati di Enti Aggregati";
    public static final String CSV_AGGREGATES_TEXT_IO = "Dati degli Enti Aggregati_IO";

    private static final String MANAGER_EMAIL_NOT_FOUND_MESSAGE = "Manager email not found";
    private static final String MANAGER_EMAIL_NOT_FOUND_CODE = "0024";

    /**
     * Sets up common data for contract PDF generation.
     */
    public static Map<String, Object> setUpCommonData(ContractPdfRequest request) {
        Map<String, Object> map = new HashMap<>();
        InstitutionPdfData institution = request.getInstitution();
        UserPdfData manager = request.getManager();

        mapInstitutionData(map, institution);
        mapManagerData(map, manager);
        mapDelegatesData(map, request.getDelegates());
        mapBillingData(map, request);

        List<String> geographicTaxonomies = Optional.ofNullable(institution.getGeographicTaxonomies())
                .map(geoTaxonomies -> geoTaxonomies.stream().map(GeographicTaxonomyPdfData::getDesc).toList())
                .orElse(List.of());

        if (!geographicTaxonomies.isEmpty()) {
            map.put("institutionGeoTaxonomies", geographicTaxonomies);
        }

        map.put("parentInfo", Objects.nonNull(institution.getParentDescription())
                ? " ente centrale " + institution.getParentDescription()
                : EMPTY_STR);

        return map;
    }

    /**
     * Sets up data for attachment PDF generation.
     */
    public static Map<String, Object> setUpAttachmentData(AttachmentPdfRequest request) {
        Map<String, Object> map = new HashMap<>();
        InstitutionPdfData institution = request.getInstitution();
        UserPdfData manager = request.getManager();

        map.put(INSTITUTION_NAME, institution.getDescription());
        map.put("institutionTaxCode", Optional.ofNullable(institution.getTaxCode()).orElse(UNDERSCORE));
        map.put("institutionMail", institution.getDigitalAddress());
        map.put("managerName", Optional.ofNullable(manager.getName()).orElse(EMPTY_STR));
        map.put("managerSurname", Optional.ofNullable(manager.getSurname()).orElse(EMPTY_STR));

        if (Objects.nonNull(institution.getGpuData())) {
            GpuDataPdfData gpuData = institution.getGpuData();
            map.put("businessRegisterNumber", Optional.ofNullable(gpuData.getBusinessRegisterNumber()).orElse(UNDERSCORE));
            map.put("legalRegisterNumber", Optional.ofNullable(gpuData.getLegalRegisterNumber()).orElse(UNDERSCORE));
            map.put("legalRegisterName", Optional.ofNullable(gpuData.getLegalRegisterName()).orElse(UNDERSCORE));
            map.put("businessRegisterCheckbox1", StringUtils.isNotEmpty(gpuData.getBusinessRegisterNumber()) ? CHECKBOX_X : EMPTY_STR);
            map.put("businessRegisterCheckbox2", StringUtils.isEmpty(gpuData.getBusinessRegisterNumber()) ? CHECKBOX_X : EMPTY_STR);
            map.put("publicServicesCheckbox1", StringUtils.isNotEmpty(gpuData.getLegalRegisterName()) ? CHECKBOX_X : EMPTY_STR);
            map.put("publicServicesCheckbox2", StringUtils.isEmpty(gpuData.getLegalRegisterName()) ? CHECKBOX_X : EMPTY_STR);
            map.put("longTermPaymentsCheckbox1", gpuData.isLongTermPayments() ? CHECKBOX_X : EMPTY_STR);
            map.put("longTermPaymentsCheckbox2", !gpuData.isLongTermPayments() ? CHECKBOX_X : EMPTY_STR);
        }
        return map;
    }

    private static void mapInstitutionData(Map<String, Object> map, InstitutionPdfData institution) {
        map.put(INSTITUTION_NAME, institution.getDescription());
        map.put("address", institution.getAddress());
        map.put("institutionTaxCode", Optional.ofNullable(institution.getTaxCode()).orElse(UNDERSCORE));
        map.put("zipCode", Optional.ofNullable(institution.getZipCode()).orElse(EMPTY_STR));
        map.put("institutionCity", Optional.ofNullable(institution.getCity()).orElse("__"));
        map.put("institutionCountry", Optional.ofNullable(institution.getCountry()).orElse("__"));
        map.put("institutionCounty", Optional.ofNullable(institution.getCounty()).orElse("__"));
        map.put("institutionMail", institution.getDigitalAddress());
        map.put("institutionType", decodeInstitutionType(institution.getInstitutionType()));

        String extCountry = (Objects.nonNull(institution.getCountry()) && !"IT".equals(institution.getCountry()))
                ? institution.getCity() + " (" + institution.getCountry() + ")"
                : EMPTY_STR;
        map.put("extCountry", extCountry);

        map.put("originId", Optional.ofNullable(institution.getOriginId())
                .filter(id -> !id.equals(institution.getTaxCode()))
                .orElse(UNDERSCORE));
    }

    private static void mapManagerData(Map<String, Object> map, UserPdfData manager) {
        if (Objects.isNull(manager.getEmail())) {
            throw new InvalidRequestException(MANAGER_EMAIL_NOT_FOUND_MESSAGE, MANAGER_EMAIL_NOT_FOUND_CODE);
        }

        map.put("managerName", Optional.ofNullable(manager.getName()).orElse(EMPTY_STR));
        map.put("managerSurname", Optional.ofNullable(manager.getSurname()).orElse(EMPTY_STR));
        map.put("managerTaxCode", manager.getTaxCode());
        map.put("managerEmail", manager.getEmail());
        map.put("managerPhone", UNDERSCORE);
    }

    private static void mapDelegatesData(Map<String, Object> map, List<UserPdfData> delegates) {
        map.put("delegates", delegatesToText(delegates));
        map.put("delegatesSend", delegatesSendToText(delegates));
    }

    private static void mapBillingData(Map<String, Object> map, ContractPdfRequest request) {
        BillingPdfData billing = request.getBilling();
        map.put("institutionVatNumber", Optional.ofNullable(billing).map(BillingPdfData::getVatNumber).orElse(UNDERSCORE));
        map.put("taxCodeInvoicing", Optional.ofNullable(billing).map(BillingPdfData::getTaxCodeInvoicing).orElse(UNDERSCORE));
        addAggregatesCsvLink(request, map);
    }

    /**
     * Sets up PSP-specific data for PDF generation.
     */
    public static void setupPSPData(Map<String, Object> map, UserPdfData manager, ContractPdfRequest request) {
        InstitutionPdfData institution = request.getInstitution();
        if (Objects.nonNull(institution.getPaymentServiceProvider())) {
            PaymentServiceProviderPdfData psp = institution.getPaymentServiceProvider();
            map.put("legalRegisterNumber", psp.getLegalRegisterNumber());
            map.put("legalRegisterName", psp.getLegalRegisterName());
            map.put("vatNumberGroup", psp.isVatNumberGroup() ? "partita iva di gruppo" : EMPTY_STR);
            map.put("vatNumberGroupCheckbox1", psp.isVatNumberGroup() ? CHECKBOX_X : EMPTY_STR);
            map.put("vatNumberGroupCheckbox2", !psp.isVatNumberGroup() ? CHECKBOX_X : EMPTY_STR);
            map.put("institutionRegister", psp.getBusinessRegisterNumber());
            map.put("institutionAbi", psp.getAbiCode());
        }

        if (Objects.nonNull(institution.getDataProtectionOfficer())) {
            DataProtectionOfficerPdfData dpo = institution.getDataProtectionOfficer();
            map.put("dataProtectionOfficerAddress", dpo.getAddress());
            map.put("dataProtectionOfficerEmail", dpo.getEmail());
            map.put("dataProtectionOfficerPec", dpo.getPec());
        }

        appendRecipientCode(map, request.getBilling());

        if (Objects.nonNull(manager.getEmail())) {
            map.put("managerPEC", manager.getEmail());
        }
    }

    /**
     * Sets up EC (Ente Creditore) specific data.
     */
    public static void setECData(Map<String, Object> map, InstitutionPdfData institution) {
        map.put(INSTITUTION_REA, Optional.ofNullable(institution.getRea()).orElse(UNDERSCORE));
        map.put(INSTITUTION_SHARE_CAPITAL, Optional.ofNullable(institution.getShareCapital()).orElse(UNDERSCORE));
        map.put(INSTITUTION_BUSINESS_REGISTER_PLACE, Optional.ofNullable(institution.getBusinessRegisterPlace()).orElse(UNDERSCORE));
    }

    /**
     * Sets up PRV-specific data for PDF generation.
     */
    public static void setupPRVData(Map<String, Object> map, ContractPdfRequest request) {
        addInstitutionRegisterLabelValue(request.getInstitution(), map);
        map.put("delegatesPrv", delegatesPrvToText(request.getDelegates()));
        appendRecipientCode(map, request.getBilling());
        map.put("isAggregatorCheckbox", Boolean.TRUE.equals(request.getIsAggregator()) ? CHECKBOX_X : EMPTY_STR);
        setECData(map, request.getInstitution());
    }

    /**
     * Sets up prod-io specific data.
     */
    public static void setupProdIOData(ContractPdfRequest request, Map<String, Object> map, UserPdfData manager) {
        InstitutionPdfData institution = request.getInstitution();
        InstitutionType type = institution.getInstitutionType();

        map.put("institutionTypeCode", type);
        decodePricingPlan(request.getPricingPlan(), request.getProductId(), map);
        map.put("originIdLabelValue", Origin.IPA.equals(institution.getOrigin()) ? ORIGIN_ID_LABEL : EMPTY_STR);

        addInstitutionRegisterLabelValue(institution, map);
        appendRecipientCode(map, request.getBilling());

        boolean isGsp = InstitutionType.GSP == type;
        map.put("GPSinstitutionName", isGsp ? institution.getDescription() : UNDERSCORE);
        map.put("GPSmanagerName", isGsp ? Optional.ofNullable(manager.getName()).orElse(UNDERSCORE) : UNDERSCORE);
        map.put("GPSmanagerSurname", isGsp ? Optional.ofNullable(manager.getSurname()).orElse(UNDERSCORE) : UNDERSCORE);
        map.put("GPSmanagerTaxCode", isGsp ? manager.getTaxCode() : UNDERSCORE);

        setECData(map, institution);
        addPricingPlan(request.getPricingPlan(), map);
    }

    /**
     * Sets up SA prod-interop specific data.
     */
    public static void setupSAProdInteropData(Map<String, Object> map, InstitutionPdfData institution) {
        setECData(map, institution);
        if (InstitutionType.SA.equals(institution.getInstitutionType())) {
            map.put("originId", UNDERSCORE);
        }
    }

    /**
     * Sets up prod-pn specific data.
     */
    public static void setupProdPNData(Map<String, Object> map, InstitutionPdfData institution, BillingPdfData billing) {
        addInstitutionRegisterLabelValue(institution, map);
        appendRecipientCode(map, billing);
    }

    /**
     * Sets up payment data for PRV institutions.
     */
    public static void setupPaymentData(Map<String, Object> data, PaymentPdfData payment) {
        if (payment != null) {
            data.put("holder", Optional.ofNullable(payment.getHolder()).orElse(EMPTY_STR));
            data.put("holder-iban", Optional.ofNullable(payment.getIban()).orElse(EMPTY_STR));
        }
    }

    private static void addPricingPlan(String pricingPlan, Map<String, Object> map) {
        boolean isPlanInList = Objects.nonNull(pricingPlan) && Arrays.stream(PLAN_LIST).anyMatch(s -> s.equalsIgnoreCase(pricingPlan));
        map.put(PRICING_PLAN_PREMIUM, isPlanInList ? pricingPlan.replace("C", EMPTY_STR) : EMPTY_STR);
        map.put("pricingPlanPremiumBase", Optional.ofNullable(pricingPlan).orElse(EMPTY_STR));

        boolean isC0 = Objects.nonNull(pricingPlan) && "C0".equalsIgnoreCase(pricingPlan);
        map.put(PRICING_PLAN_PREMIUM_CHECKBOX, isC0 ? CHECKBOX_X : EMPTY_STR);
    }

    private static void addAggregatesCsvLink(ContractPdfRequest request, Map<String, Object> map) {
        String csvLink = EMPTY_STR;
        if (Boolean.TRUE.equals(request.getIsAggregator()) && StringUtils.isNotEmpty(request.getAggregatesCsvBaseUrl())) {
            String url = String.format("%s%s/products/%s/aggregates",
                    request.getAggregatesCsvBaseUrl(), request.getOnboardingId(), request.getProductId());
            String csvText = PROD_IO.getValue().equals(request.getProductId()) ? CSV_AGGREGATES_TEXT_IO : CSV_AGGREGATES_TEXT;
            csvLink = PROD_PN.getValue().equals(request.getProductId())
                    ? String.format(CSV_AGGREGATES_LABEL_SEND, url, csvText)
                    : String.format(CSV_AGGREGATES_LABEL, url, csvText);
        }
        map.put(CSV_AGGREGATES_LABEL_VALUE, csvLink);
    }

    private static void addInstitutionRegisterLabelValue(InstitutionPdfData institution, Map<String, Object> map) {
        String number = UNDERSCORE;
        String label = EMPTY_STR;
        if (Objects.nonNull(institution.getPaymentServiceProvider())) {
            number = Optional.ofNullable(institution.getPaymentServiceProvider().getBusinessRegisterNumber()).orElse(UNDERSCORE);
            label = "<li class=\"c19 c39 li-bullet-0\"><span class=\"c1\">codice di iscrizione all&rsquo;Indice delle Pubbliche Amministrazioni e dei gestori di pubblici servizi (I.P.A.) <span class=\"c3\">${number}</span> </span><span class=\"c1\"></span></li>\n";
        }
        map.put("number", number);
        map.put(INSTITUTION_REGISTER_LABEL_VALUE, label);
    }

    private static void decodePricingPlan(String pricingPlan, String productId, Map<String, Object> map) {
        if (PricingPlan.FA.name().equals(pricingPlan)) {
            map.put(PRICING_PLAN_FAST_CHECKBOX, CHECKBOX_X);
            map.put(PRICING_PLAN_BASE_CHECKBOX, EMPTY_STR);
            map.put(PRICING_PLAN_PREMIUM_CHECKBOX, EMPTY_STR);
            map.put(PRICING_PLAN, PricingPlan.FA.getValue());
            return;
        }
        map.put(PRICING_PLAN_FAST_CHECKBOX, EMPTY_STR);
        if (PROD_IO.getValue().equalsIgnoreCase(productId)) {
            map.put(PRICING_PLAN_BASE_CHECKBOX, CHECKBOX_X);
            map.put(PRICING_PLAN_PREMIUM_CHECKBOX, EMPTY_STR);
            map.put(PRICING_PLAN, PricingPlan.BASE.getValue());
        } else {
            map.put(PRICING_PLAN_BASE_CHECKBOX, EMPTY_STR);
            map.put(PRICING_PLAN_PREMIUM_CHECKBOX, CHECKBOX_X);
            map.put(PRICING_PLAN, PricingPlan.PREMIUM.getValue());
        }
    }

    private static String decodeInstitutionType(InstitutionType institutionType) {
        if (institutionType == null) {
            return EMPTY_STR;
        }
        return switch (institutionType) {
            case PA -> "Pubblica Amministrazione";
            case GSP -> "Gestore di servizi pubblici";
            case PT -> "Partner tecnologico";
            case SCP -> "Società a controllo pubblico";
            case PSP -> "Prestatori Servizi di Pagamento";
            default -> EMPTY_STR;
        };
    }

    private static String delegatesToText(List<UserPdfData> users) {
        if (users == null || users.isEmpty()) {
            return EMPTY_STR;
        }
        StringBuilder builder = new StringBuilder();
        users.forEach(user ->
                builder.append("</br>")
                .append("<p class=\"c141\"><span class=\"c6\">Nome e Cognome: ")
                .append(Optional.ofNullable(user.getName()).orElse(EMPTY_STR)).append(" ")
                .append(Optional.ofNullable(user.getSurname()).orElse(EMPTY_STR)).append("&nbsp;</span></p>\n")
                .append("<p class=\"c141\"><span class=\"c6\">Codice Fiscale: ")
                .append(user.getTaxCode()).append("</span></p>\n")
                .append("<p class=\"c141\"><span class=\"c6\">e-mail: ")
                .append(Optional.ofNullable(user.getEmail()).orElse(EMPTY_STR))
                .append("&nbsp;</span></p>\n").append("</br>"));
        return builder.toString();
    }

    private static String delegatesPrvToText(List<UserPdfData> users) {
        if (users == null || users.isEmpty()) {
            return EMPTY_STR;
        }
        StringBuilder builder = new StringBuilder("<p class=\"c2\"><span class=\"c1\"><ol class=\"c0\">");
        users.forEach(user -> builder.append("<br><li class=\"c1\"><br>")
                .append("<p class=\"c2\"><span class=\"c1\">Cognome: ")
                .append(Optional.ofNullable(user.getSurname()).orElse(EMPTY_STR)).append("&nbsp;</span></p>\n")
                .append("<p class=\"c2\"><span class=\"c1\">Nome: ")
                .append(Optional.ofNullable(user.getName()).orElse(EMPTY_STR)).append("&nbsp;</span></p>\n")
                .append("<p class=\"c2\"><span class=\"c1\">Codice Fiscale: ")
                .append(user.getTaxCode()).append("</span></p>\n")
                .append("<p class=\"c2\"><span class=\"c1\">Posta Elettronica aziendale: ")
                .append(Optional.ofNullable(user.getEmail()).orElse(EMPTY_STR))
                .append("&nbsp;</span></p>\n").append("</li>\n"));
        return builder.append("</ol></span></p>").toString();
    }

    private static String delegatesSendToText(List<UserPdfData> users) {
        if (users == null || users.isEmpty()) {
            return EMPTY_STR;
        }
        StringBuilder builder = new StringBuilder("<p class=\"c2\"><span class=\"c1\"><ol class=\"c34 lst-kix_list_23-0 start\" start=\"1\"");
        users.forEach(user ->
                builder.append("<br><li class=\"c2 c16 li-bullet-3\"><span class=\"c1\">Nome e Cognome: ")
                .append(Optional.ofNullable(user.getName()).orElse(EMPTY_STR)).append(" ")
                .append(Optional.ofNullable(user.getSurname()).orElse(EMPTY_STR)).append("</span></li>")
                .append("<li class=\"c2 c16 li-bullet-3\"><span class=\"c1\">Codice Fiscale: ")
                .append(user.getTaxCode()).append("</span></li>")
                .append("<li class=\"c2 c16 li-bullet-3\"><span class=\"c1\">Posta Elettronica aziendale: ")
                .append(Optional.ofNullable(user.getEmail()).orElse(EMPTY_STR))
                .append("</span></li><br>"));
        return builder.append("</ol></span></p>").toString();
    }

    private static void appendRecipientCode(Map<String, Object> map, BillingPdfData billing) {
        if (Objects.nonNull(billing)) {
            map.put(INSTITUTION_RECIPIENT_CODE, billing.getRecipientCode());
        }
    }
}
