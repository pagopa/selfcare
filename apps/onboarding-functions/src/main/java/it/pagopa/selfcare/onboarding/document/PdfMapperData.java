package it.pagopa.selfcare.onboarding.document;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PricingPlan;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.*;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PN;
import static it.pagopa.selfcare.onboarding.utils.GenericError.MANAGER_EMAIL_NOT_FOUND;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class PdfMapperData {

  private static final String UNDERSCORE = "_______________";
  private static final String CHECKBOX_X = "X";
  private static final String EMPTY_STR = "";
  private static final String[] PLAN_LIST = {"C1", "C2", "C3", "C4", "C5", "C6", "C7"};

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

    public static Map<String, Object> setUpCommonData(
      UserResource manager, List<UserResource> users, Onboarding onboarding, String baseUrl) {

    Map<String, Object> map = new HashMap<>();
    Institution institution = onboarding.getInstitution();

    mapInstitutionData(map, institution);
    mapManagerData(map, manager, onboarding.getUsers());
    mapDelegatesData(map, users, onboarding.getUsers());
    mapBillingData(map, onboarding, baseUrl);

    List<String> geographicTaxonomies = Optional.ofNullable(institution.getGeographicTaxonomies())
            .map(geoTaxonomies -> geoTaxonomies.stream().map(GeographicTaxonomy::getDesc).toList())
            .orElse(List.of());

    if (!geographicTaxonomies.isEmpty()) {
      map.put("institutionGeoTaxonomies", geographicTaxonomies);
    }

    map.put("parentInfo", Objects.nonNull(institution.getParentDescription())
            ? " ente centrale " + institution.getParentDescription()
            : EMPTY_STR);

    return map;
  }

  private static void mapInstitutionData(Map<String, Object> map, Institution institution) {
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

  private static void mapManagerData(Map<String, Object> map, UserResource managerResource, List<User> onboardingUsers) {
    User userManager = onboardingUsers.stream()
            .filter(user -> user.getId().equals(managerResource.getId().toString()))
            .findFirst()
            .orElseThrow();

    String mailManager = getMailManager(managerResource, userManager.getUserMailUuid());
    if (Objects.isNull(mailManager)) {
      throw new GenericOnboardingException(MANAGER_EMAIL_NOT_FOUND.getMessage(), MANAGER_EMAIL_NOT_FOUND.getCode());
    }

    map.put("managerName", getStringValue(managerResource.getName()));
    map.put("managerSurname", getStringValue(managerResource.getFamilyName()));
    map.put("managerTaxCode", managerResource.getFiscalCode());
    map.put("managerEmail", mailManager);
    map.put("managerPhone", UNDERSCORE);
  }

  private static void mapDelegatesData(Map<String, Object> map, List<UserResource> users, List<User> onboardingUsers) {
    map.put("delegates", delegatesToText(users, onboardingUsers));
    map.put("delegatesSend", delegatesSendToText(users, onboardingUsers));
  }

  private static void mapBillingData(Map<String, Object> map, Onboarding onboarding, String baseUrl) {
    Billing billing = onboarding.getBilling();
    map.put("institutionVatNumber", Optional.ofNullable(billing).map(Billing::getVatNumber).orElse(UNDERSCORE));
    map.put("taxCodeInvoicing", Optional.ofNullable(billing).map(Billing::getTaxCodeInvoicing).orElse(UNDERSCORE));
    addAggregatesCsvLink(onboarding, map, baseUrl);
  }

  public static Map<String, Object> setUpAttachmentData(Onboarding onboarding, UserResource userResource) {
    Map<String, Object> map = new HashMap<>();
    Institution institution = onboarding.getInstitution();

    map.put(INSTITUTION_NAME, institution.getDescription());
    map.put("institutionTaxCode", Optional.ofNullable(institution.getTaxCode()).orElse(UNDERSCORE));
    map.put("institutionMail", institution.getDigitalAddress());
    map.put("managerName", getStringValue(userResource.getName()));
    map.put("managerSurname", getStringValue(userResource.getFamilyName()));

    if (Objects.nonNull(institution.getGpuData())) {
      GPUData gpuData = institution.getGpuData();
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

  private static String getMailManager(UserResource manager, String userMailUuid) {
    return Optional.ofNullable(manager.getWorkContacts())
        .map(contacts -> contacts.get(userMailUuid))
        .map(WorkContactResource::getEmail)
        .map(CertifiableFieldResourceOfstring::getValue)
        .orElse(null);
  }

  public static void setupPSPData(Map<String, Object> map, UserResource validManager, Onboarding onboarding) {
    Institution institution = onboarding.getInstitution();
    if (Objects.nonNull(institution.getPaymentServiceProvider())) {
      PaymentServiceProvider psp = institution.getPaymentServiceProvider();
      map.put("legalRegisterNumber", psp.getLegalRegisterNumber());
      map.put("legalRegisterName", psp.getLegalRegisterName());
      map.put("vatNumberGroup", psp.isVatNumberGroup() ? "partita iva di gruppo" : EMPTY_STR);
      map.put("vatNumberGroupCheckbox1", psp.isVatNumberGroup() ? CHECKBOX_X : EMPTY_STR);
      map.put("vatNumberGroupCheckbox2", !psp.isVatNumberGroup() ? CHECKBOX_X : EMPTY_STR);
      map.put("institutionRegister", psp.getBusinessRegisterNumber());
      map.put("institutionAbi", psp.getAbiCode());
    }

    if (Objects.nonNull(institution.getDataProtectionOfficer())) {
      DataProtectionOfficer dpo = institution.getDataProtectionOfficer();
      map.put("dataProtectionOfficerAddress", dpo.getAddress());
      map.put("dataProtectionOfficerEmail", dpo.getEmail());
      map.put("dataProtectionOfficerPec", dpo.getPec());
    }

    appendRecipientCode(map, onboarding.getBilling());

    onboarding.getUsers().stream()
        .filter(user -> validManager.getId().toString().equals(user.getId()))
        .map(User::getUserMailUuid)
        .findFirst()
        .map(userMailUuid -> getMailManager(validManager, userMailUuid))
        .ifPresent(mail -> map.put("managerPEC", mail));
  }

  public static void setECData(Map<String, Object> map, Institution institution) {
    map.put(INSTITUTION_REA, Optional.ofNullable(institution.getRea()).orElse(UNDERSCORE));
    map.put(INSTITUTION_SHARE_CAPITAL, Optional.ofNullable(institution.getShareCapital()).orElse(UNDERSCORE));
    map.put(INSTITUTION_BUSINESS_REGISTER_PLACE, Optional.ofNullable(institution.getBusinessRegisterPlace()).orElse(UNDERSCORE));
  }

  public static void setupPRVData(Map<String, Object> map, Onboarding onboarding, List<UserResource> users) {
    addInstitutionRegisterLabelValue(onboarding.getInstitution(), map);
    map.put("delegatesPrv", delegatesPrvToText(users, onboarding.getUsers()));
    appendRecipientCode(map, onboarding.getBilling());
    map.put("isAggregatorCheckbox", Boolean.TRUE.equals(onboarding.getIsAggregator()) ? CHECKBOX_X : EMPTY_STR);
    setECData(map, onboarding.getInstitution());
  }

  public static void setupProdIOData(Onboarding onboarding, Map<String, Object> map, UserResource validManager) {
    Institution institution = onboarding.getInstitution();
    InstitutionType type = institution.getInstitutionType();

    map.put("institutionTypeCode", type);
    decodePricingPlan(onboarding.getPricingPlan(), onboarding.getProductId(), map);
    map.put("originIdLabelValue", Origin.IPA.equals(institution.getOrigin()) ? ORIGIN_ID_LABEL : EMPTY_STR);

    addInstitutionRegisterLabelValue(institution, map);
    appendRecipientCode(map, onboarding.getBilling());

    boolean isGsp = InstitutionType.GSP == type;
    map.put("GPSinstitutionName", isGsp ? institution.getDescription() : UNDERSCORE);
    map.put("GPSmanagerName", isGsp ? getStringValue(validManager.getName()) : UNDERSCORE);
    map.put("GPSmanagerSurname", isGsp ? getStringValue(validManager.getFamilyName()) : UNDERSCORE);
    map.put("GPSmanagerTaxCode", isGsp ? validManager.getFiscalCode() : UNDERSCORE);

    setECData(map, institution);
    addPricingPlan(onboarding.getPricingPlan(), map);
  }

  public static void setupSAProdInteropData(Map<String, Object> map, Institution institution) {
    setECData(map, institution);
    if (InstitutionType.SA.equals(institution.getInstitutionType())) {
      map.put("originId", UNDERSCORE);
    }
  }

  public static void setupProdPNData(Map<String, Object> map, Institution institution, Billing billing) {
    addInstitutionRegisterLabelValue(institution, map);
    appendRecipientCode(map, billing);
  }

  private static void addPricingPlan(String pricingPlan, Map<String, Object> map) {
    boolean isPlanInList = Objects.nonNull(pricingPlan) && Arrays.stream(PLAN_LIST).anyMatch(s -> s.equalsIgnoreCase(pricingPlan));
    map.put(PRICING_PLAN_PREMIUM, isPlanInList ? pricingPlan.replace("C", EMPTY_STR) : EMPTY_STR);
    map.put("pricingPlanPremiumBase", Optional.ofNullable(pricingPlan).orElse(EMPTY_STR));

    boolean isC0 = Objects.nonNull(pricingPlan) && "C0".equalsIgnoreCase(pricingPlan);
    map.put(PRICING_PLAN_PREMIUM_CHECKBOX, isC0 ? CHECKBOX_X : EMPTY_STR);
  }

  private static void addAggregatesCsvLink(Onboarding onboarding, Map<String, Object> map, String baseUrl) {
    String csvLink = EMPTY_STR;
    if (Boolean.TRUE.equals(onboarding.getIsAggregator())) {
      String url = String.format("%s%s/products/%s/aggregates", baseUrl, onboarding.getId(), onboarding.getProductId());
      String csvText = PROD_IO.getValue().equals(onboarding.getProductId()) ? CSV_AGGREGATES_TEXT_IO : CSV_AGGREGATES_TEXT;
      csvLink = PROD_PN.getValue().equals(onboarding.getProductId())
              ? String.format(CSV_AGGREGATES_LABEL_SEND, url, csvText)
              : String.format(CSV_AGGREGATES_LABEL, url, csvText);
    }
    map.put(CSV_AGGREGATES_LABEL_VALUE, csvLink);
  }

  private static void addInstitutionRegisterLabelValue(Institution institution, Map<String, Object> map) {
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
    return switch (institutionType) {
      case PA -> "Pubblica Amministrazione";
      case GSP -> "Gestore di servizi pubblici";
      case PT -> "Partner tecnologico";
      case SCP -> "Società a controllo pubblico";
      case PSP -> "Prestatori Servizi di Pagamento";
      default -> EMPTY_STR;
    };
  }

  private static String delegatesToText(List<UserResource> userResources, List<User> users) {
    StringBuilder builder = new StringBuilder();
    userResources.forEach(userResource -> {
      builder.append("</br>")
              .append("<p class=\"c141\"><span class=\"c6\">Nome e Cognome: ")
              .append(getStringValue(userResource.getName())).append(" ")
              .append(getStringValue(userResource.getFamilyName())).append("&nbsp;</span></p>\n")
              .append("<p class=\"c141\"><span class=\"c6\">Codice Fiscale: ")
              .append(userResource.getFiscalCode()).append("</span></p>\n")
              .append("<p class=\"c141\"><span class=\"c6\">e-mail: ");
      appendWorkEmail(users, userResource, builder);
      builder.append("&nbsp;</span></p>\n").append("</br>");
    });
    return builder.toString();
  }

  private static String delegatesPrvToText(List<UserResource> userResources, List<User> users) {
    StringBuilder builder = new StringBuilder("<p class=\"c2\"><span class=\"c1\"><ol class=\"c0\">");
    userResources.forEach(userResource -> {
      builder.append("<br><li class=\"c1\"><br>")
              .append("<p class=\"c2\"><span class=\"c1\">Cognome: ").append(getStringValue(userResource.getFamilyName())).append("&nbsp;</span></p>\n")
              .append("<p class=\"c2\"><span class=\"c1\">Nome: ").append(getStringValue(userResource.getName())).append("&nbsp;</span></p>\n")
              .append("<p class=\"c2\"><span class=\"c1\">Codice Fiscale: ").append(userResource.getFiscalCode()).append("</span></p>\n")
              .append("<p class=\"c2\"><span class=\"c1\">Posta Elettronica aziendale: ");
      appendWorkEmail(users, userResource, builder);
      builder.append("&nbsp;</span></p>\n").append("</li>\n");
    });
    return builder.append("</ol></span></p>").toString();
  }

  private static String delegatesSendToText(List<UserResource> userResources, List<User> users) {
    StringBuilder builder = new StringBuilder("<p class=\"c2\"><span class=\"c1\"><ol class=\"c34 lst-kix_list_23-0 start\" start=\"1\"");
    userResources.forEach(userResource -> {
      builder.append("<br><li class=\"c2 c16 li-bullet-3\"><span class=\"c1\">Nome e Cognome: ")
              .append(getStringValue(userResource.getName())).append(" ")
              .append(getStringValue(userResource.getFamilyName())).append("</span></li>")
              .append("<li class=\"c2 c16 li-bullet-3\"><span class=\"c1\">Codice Fiscale: ")
              .append(userResource.getFiscalCode()).append("</span></li>")
              .append("<li class=\"c2 c16 li-bullet-3\"><span class=\"c1\">Posta Elettronica aziendale: ");
      appendWorkEmail(users, userResource, builder);
      builder.append("</span></li><br>");
    });
    return builder.append("</ol></span></p>").toString();
  }

  private static void appendWorkEmail(List<User> users, UserResource userResource, StringBuilder builder) {
    users.stream()
            .filter(user -> userResource.getId().toString().equals(user.getId()))
            .map(User::getUserMailUuid)
            .findFirst()
            .ifPresent(uuid -> {
              if (Objects.nonNull(userResource.getWorkContacts()) && userResource.getWorkContacts().containsKey(uuid)) {
                builder.append(getStringValue(userResource.getWorkContacts().get(uuid).getEmail()));
              }
            });
  }

  private static String getStringValue(CertifiableFieldResourceOfstring resource) {
    return Optional.ofNullable(resource).map(CertifiableFieldResourceOfstring::getValue).orElse(EMPTY_STR);
  }

  private static void appendRecipientCode(Map<String, Object> map, Billing billing) {
    if (Objects.nonNull(billing)) {
      map.put(INSTITUTION_RECIPIENT_CODE, billing.getRecipientCode());
    }
  }

  public static void setupPaymentData(Map<String, Object> data, Payment payment) {
    data.put("holder", payment.retrieveEncryptedHolder());
    data.put("holder-iban", payment.retrieveEncryptedIban());
  }
}