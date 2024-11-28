package it.pagopa.selfcare.user.mapper;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.CertificationEnum;
import it.pagopa.selfcare.user.controller.response.CertifiableFieldResponse;
import it.pagopa.selfcare.user.controller.response.WorkContactResponse;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_registry_json.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class UserMapperTest {

    public static final String CONTACTS_UUID = "contactsUuid";
    public static final String EMAIL = "email";
    public static final String MOBILE_PHONE = "mobilePhone";
    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    void retrieveCertifiedEmailFromWorkContacts(){
        final UserResource userResource = new UserResource();
        final EmailCertifiableSchema email = new EmailCertifiableSchema(EmailCertifiableSchema.CertificationEnum.NONE, EMAIL);
        final MobilePhoneCertifiableSchema phone = new MobilePhoneCertifiableSchema(MobilePhoneCertifiableSchema.CertificationEnum.NONE, MOBILE_PHONE);
        WorkContactResource workContactResource = new WorkContactResource(email, phone, null);
        Map<String, WorkContactResource> workContactResourceMap = Map.of(CONTACTS_UUID, workContactResource);
        userResource.setWorkContacts(workContactResourceMap);

        CertifiableFieldResponse<String> certifiedMail = userMapper.retrieveCertifiedMailFromWorkContacts(userResource, CONTACTS_UUID);

        assertEquals(CertificationEnum.NONE,certifiedMail.getCertified());
        assertEquals(EMAIL, certifiedMail.getValue());
    }

    @Test
    void retrieveCertifiedMobilePhoneFromWorkContacts(){
        final UserResource userResource = new UserResource();
        final EmailCertifiableSchema email = new EmailCertifiableSchema(EmailCertifiableSchema.CertificationEnum.NONE, EMAIL);
        String mobilePhone = MOBILE_PHONE;
        final MobilePhoneCertifiableSchema phone = new MobilePhoneCertifiableSchema(MobilePhoneCertifiableSchema.CertificationEnum.NONE, mobilePhone);
        WorkContactResource workContactResource = new WorkContactResource(email, phone, null);
        Map<String, WorkContactResource> workContactResourceMap = Map.of(CONTACTS_UUID, workContactResource);
        userResource.setWorkContacts(workContactResourceMap);

        CertifiableFieldResponse<String> certifiedPhone = userMapper.retrieveCertifiedMobilePhoneFromWorkContacts(userResource, CONTACTS_UUID);

        assertEquals(CertificationEnum.NONE,certifiedPhone.getCertified());
        assertEquals(mobilePhone, certifiedPhone.getValue());
    }

    @Test
    void retrieveCertifiedEmailFromWorkContacts_nullWorkContacts(){
        final UserResource userResource = new UserResource();

        CertifiableFieldResponse<String> certifiedMail = userMapper.retrieveCertifiedMailFromWorkContacts(userResource, CONTACTS_UUID);

        assertNull(certifiedMail);
    }

    @Test
    void retrieveCertifiedEmailFromWorkContacts_notPresent(){
        final UserResource userResource = new UserResource();
        final EmailCertifiableSchema email = new EmailCertifiableSchema(EmailCertifiableSchema.CertificationEnum.NONE, EMAIL);
        final MobilePhoneCertifiableSchema phone = new MobilePhoneCertifiableSchema(MobilePhoneCertifiableSchema.CertificationEnum.NONE, MOBILE_PHONE);

        WorkContactResource workContactResource = new WorkContactResource(email, phone, null);
        Map<String, WorkContactResource> workContactResourceMap = Map.of(CONTACTS_UUID, workContactResource);
        userResource.setWorkContacts(workContactResourceMap);

        CertifiableFieldResponse<String> certifiedMail = userMapper.retrieveCertifiedMailFromWorkContacts(userResource, "notPresent");
        assertNull(certifiedMail);

    }


    @Test
    void retrieveMailFromWorkContacts(){
        final EmailCertifiableSchema email = new EmailCertifiableSchema(EmailCertifiableSchema.CertificationEnum.NONE, EMAIL);
        final MobilePhoneCertifiableSchema phone = new MobilePhoneCertifiableSchema(MobilePhoneCertifiableSchema.CertificationEnum.NONE, MOBILE_PHONE);
        WorkContactResource workContactResource = new WorkContactResource(email, phone, null);
        Map<String, WorkContactResource> workContactResourceMap = Map.of(CONTACTS_UUID, workContactResource);

        String mailFromWorkContact = userMapper.retrieveMailFromWorkContacts(workContactResourceMap, CONTACTS_UUID);

        assertEquals(EMAIL, mailFromWorkContact);
    }

    @Test
    void retrieveMobilePhoneFromWorkContacts(){
        final EmailCertifiableSchema email = new EmailCertifiableSchema(EmailCertifiableSchema.CertificationEnum.NONE, EMAIL);
        final MobilePhoneCertifiableSchema phone = new MobilePhoneCertifiableSchema(MobilePhoneCertifiableSchema.CertificationEnum.NONE, MOBILE_PHONE);
        WorkContactResource workContactResource = new WorkContactResource(email, phone, null);
        Map<String, WorkContactResource> workContactResourceMap = Map.of(CONTACTS_UUID, workContactResource);

        String phoneFromWorkContacts = userMapper.retrieveMobilePhoneFromWorkContacts(workContactResourceMap, CONTACTS_UUID);

        assertEquals(MOBILE_PHONE, phoneFromWorkContacts);
    }

    @Test
    void retrieveMailFromWorkContacts_emptyMap(){
        Map<String, WorkContactResource> workContactResourceMap = new HashMap<>();

        String mailFromWorkContact = userMapper.retrieveMailFromWorkContacts(workContactResourceMap, CONTACTS_UUID);

        assertNull(mailFromWorkContact);
    }

    @Test
    void retrieveMobilePhoneFromWorkContacts_emptyMap(){
        Map<String, WorkContactResource> workContactResourceMap = new HashMap<>();

        String phoneFromWorkContact = userMapper.retrieveMobilePhoneFromWorkContacts(workContactResourceMap, CONTACTS_UUID);

        assertNull(phoneFromWorkContact);
    }

    @Test
    void retrieveMailFromWorkContacts_nullMap(){

        String mailFromWorkContact = userMapper.retrieveMailFromWorkContacts(null, CONTACTS_UUID);

        assertNull(mailFromWorkContact);
    }

    @Test
    void retrievePhoneFromWorkContacts_nullMap(){

        String phoneFromWorkContact = userMapper.retrieveMailFromWorkContacts(null, CONTACTS_UUID);

        assertNull(phoneFromWorkContact);
    }

    @Test
    void retrieveMailFromWorkContacts_notPresent(){
        final EmailCertifiableSchema email = new EmailCertifiableSchema(EmailCertifiableSchema.CertificationEnum.NONE, EMAIL);
        final MobilePhoneCertifiableSchema phone = new MobilePhoneCertifiableSchema(MobilePhoneCertifiableSchema.CertificationEnum.NONE, MOBILE_PHONE);

        WorkContactResource workContactResource = new WorkContactResource(email, phone, null);
        Map<String, WorkContactResource> workContactResourceMap = Map.of(CONTACTS_UUID, workContactResource);

        String mailFromWorkContact = userMapper.retrieveMailFromWorkContacts(workContactResourceMap, "notPresent");

        assertNull(mailFromWorkContact);
    }

    @Test
    void toWorkContactResponseMap(){
        final Map<String, WorkContactResource> workContactResourceMap = new HashMap<>();
        final WorkContactResource workContactResource = new WorkContactResource();
        final EmailCertifiableSchema email = new EmailCertifiableSchema(EmailCertifiableSchema.CertificationEnum.NONE, EMAIL);
        workContactResource.setEmail(email);
        workContactResourceMap.put(CONTACTS_UUID, workContactResource);
        Map<String, WorkContactResponse> responseMap = userMapper.toWorkContactResponse(workContactResourceMap);

        assertEquals(email.getValue(), responseMap.get(CONTACTS_UUID).getEmail().getValue());
    }

    @Test
    void toWorkContactResponseMap_nullMap(){
        Map<String, WorkContactResponse> responseMap = userMapper.toWorkContactResponse(null);
        assertTrue(responseMap.isEmpty());
    }

    @Test
    void toWorkContactResponseMap_empty(){
        final Map<String, WorkContactResource> workContactResourceMap = new HashMap<>();

        Map<String, WorkContactResponse> responseMap = userMapper.toWorkContactResponse(workContactResourceMap);
        assertTrue(responseMap.isEmpty());

    }

    @Test
    void toWorkContactsValidWorkContactResourceMap() {
        Map<String, WorkContactResource> workContactResourceMap = new HashMap<>();
        EmailCertifiableSchema email = new EmailCertifiableSchema(EmailCertifiableSchema.CertificationEnum.NONE, EMAIL);
        WorkContactResource workContactResource = new WorkContactResource(email, null, null);
        workContactResourceMap.put(CONTACTS_UUID, workContactResource);

        Map<String, String> result = userMapper.toWorkContacts(workContactResourceMap);

        assertEquals(1, result.size());
        assertEquals(EMAIL, result.get(CONTACTS_UUID));
    }

    @Test
    void toWorkContactswithNullWorkContactResourceMap() {
        Map<String, String> result = userMapper.toWorkContacts(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void fromSurnameCertifiableString() {
        FamilyNameCertifiableSchema certifiableField = new FamilyNameCertifiableSchema();
        certifiableField.setValue("Doe");
        String result = userMapper.fromCertifiableString(certifiableField);
        assertEquals("Doe", result);
    }

    @Test
    void fromSurnameCertifiableStringWithNullCertifiableField() {
        String result = userMapper.fromCertifiableString((FamilyNameCertifiableSchema) null);
        assertNull(result);
    }

    @Test
    void fromSurnameCertifiableStringWithNullCertifiableFieldValue() {
        org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema certifiableField = new FamilyNameCertifiableSchema();
        certifiableField.setValue(null);
        String result = userMapper.fromCertifiableString(certifiableField);

        assertNull(result);
    }

    @Test
    void fromNameCertifiableString() {
        NameCertifiableSchema certifiableField = new NameCertifiableSchema();
        certifiableField.setValue("John");
        String result = userMapper.fromCertifiableString(certifiableField);
        assertEquals("John", result);
    }

    @Test
    void fromNameCertifiableStringWithNullCertifiableField() {
        String result = userMapper.fromCertifiableString((NameCertifiableSchema) null);
        assertNull(result);
    }

    @Test
    void fromNameCertifiableStringWithNullCertifiableFieldValue() {
        NameCertifiableSchema certifiableField = new NameCertifiableSchema();
        certifiableField.setValue(null);
        String result = userMapper.fromCertifiableString(certifiableField);
        assertNull(result);
    }

    @Test
    void toNameCertifiableFieldResponse() {
        NameCertifiableSchema resource = new NameCertifiableSchema();
        resource.setValue("John");
        CertifiableFieldResponse<String> result = userMapper.toNameCertifiableFieldResponse(resource);

        assertNotNull(result);
        assertEquals("John", result.getValue());
    }

    @Test
    void toNameCertifiableFieldResponseWithNullResource() {
        CertifiableFieldResponse<String> result = userMapper.toNameCertifiableFieldResponse(null);
        assertNull(result);
    }

    @Test
    void toNameCertifiableFieldResponseWithNullValue() {
        NameCertifiableSchema resource = new NameCertifiableSchema();
        resource.setValue(null);
        CertifiableFieldResponse<String> result = userMapper.toNameCertifiableFieldResponse(resource);

        assertNotNull(result);
        assertNull(result.getValue());
    }

    @Test
    void toFamilyNameCertifiableFieldResponse() {
        FamilyNameCertifiableSchema resource = new FamilyNameCertifiableSchema();
        resource.setValue("Doe");

        CertifiableFieldResponse<String> result = userMapper.toFamilyNameCertifiableFieldResponse(resource);

        assertNotNull(result);
        assertEquals("Doe", result.getValue());
    }

    @Test
    void toFamilyNameCertifiableFieldResponseWithNullResource() {
        CertifiableFieldResponse<String> result = userMapper.toFamilyNameCertifiableFieldResponse(null);

        assertNull(result);
    }

    @Test
    void toFamilyNameCertifiableFieldResponseWithNullValue() {
        FamilyNameCertifiableSchema resource = new FamilyNameCertifiableSchema();
        resource.setValue(null);
        CertifiableFieldResponse<String> result = userMapper.toFamilyNameCertifiableFieldResponse(resource);

        assertNotNull(result);
        assertNull(result.getValue());
    }

    @Test
    void toFamilyNameCertifiableStringNotEqualsWithSameValue() {
        FamilyNameCertifiableSchema certifiableString = new FamilyNameCertifiableSchema();
        certifiableString.setValue("Doe");

        FamilyNameCertifiableSchema result = userMapper.toFamilyNameCertifiableStringNotEquals(certifiableString, "Doe");

        assertNull(result);
    }

    @Test
    void toFamilyNameCertifiableStringNotEqualsWithDifferentValue() {
        FamilyNameCertifiableSchema certifiableString = new FamilyNameCertifiableSchema();
        certifiableString.setValue("Smith");

        FamilyNameCertifiableSchema result = userMapper.toFamilyNameCertifiableStringNotEquals(certifiableString, "Doe");

        assertNotNull(result);
        assertEquals("Doe", result.getValue());
        assertEquals(FamilyNameCertifiableSchema.CertificationEnum.NONE, result.getCertification());
    }

    @Test
    void toNameCertifiableStringNotEqualsWithBlankValue() {
        NameCertifiableSchema certifiableString = new NameCertifiableSchema();
        certifiableString.setValue("John");

        NameCertifiableSchema result = userMapper.toNameCertifiableStringNotEquals(certifiableString, " ");

        assertNull(result);
    }

    @Test
    void toNameCertifiableStringNotEqualsWithSameValue() {
        NameCertifiableSchema certifiableString = new NameCertifiableSchema();
        certifiableString.setValue("John");

        NameCertifiableSchema result = userMapper.toNameCertifiableStringNotEquals(certifiableString, "John");

        assertNull(result);
    }

    @Test
    void toNameCertifiableStringNotEqualsWithDifferentValue() {
        NameCertifiableSchema certifiableString = new NameCertifiableSchema();
        certifiableString.setValue("Smith");

        NameCertifiableSchema result = userMapper.toNameCertifiableStringNotEquals(certifiableString, "John");

        assertNotNull(result);
        assertEquals("John", result.getValue());
        assertEquals(NameCertifiableSchema.CertificationEnum.NONE, result.getCertification());
    }

    @Test
    void toWorkContact() {
        Map<String, WorkContactResource> result = userMapper.toWorkContact(EMAIL, MOBILE_PHONE, CONTACTS_UUID);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EMAIL, result.get(CONTACTS_UUID).getEmail().getValue());
        assertEquals(MOBILE_PHONE, result.get(CONTACTS_UUID).getMobilePhone().getValue());
    }

    @Test
    void toWorkContactWithBlankIdContact() {
        Map<String, WorkContactResource> result = userMapper.toWorkContact(EMAIL, MOBILE_PHONE, " ");
        assertNull(result);
    }

    @Test
    void toWorkContactWithNullEmailAndPhoneNumber() {
        Map<String, WorkContactResource> result = userMapper.toWorkContact(null, null, CONTACTS_UUID);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(CONTACTS_UUID).getEmail());
        assertNull(result.get(CONTACTS_UUID).getMobilePhone());
    }

    @Test
    void toCertifiableLocalDate() {
        BirthDateCertifiableSchema result = userMapper.toLocalTime("2000-01-01");

        assertNotNull(result);
        assertEquals(LocalDate.of(2000, 1, 1), result.getValue());
        assertEquals(BirthDateCertifiableSchema.CertificationEnum.NONE, result.getCertification());
    }

    @Test
    void toCertifiableLocalDateWithNullTime() {
        BirthDateCertifiableSchema result = userMapper.toLocalTime(null);
        assertNull(result);
    }

    @Test
    void toMailCertString() {
        EmailCertifiableSchema result = userMapper.toMailCertString(EMAIL);

        assertNotNull(result);
        assertEquals(EMAIL, result.getValue());
        assertEquals(EmailCertifiableSchema.CertificationEnum.NONE, result.getCertification());
    }

    @Test
    void toMailCertStringWithBlankValue() {
        EmailCertifiableSchema result = userMapper.toMailCertString(" ");

        assertNull(result);
    }

    @Test
    void toPhoneCertString() {
        MobilePhoneCertifiableSchema result = userMapper.toPhoneCertString(MOBILE_PHONE);

        assertNotNull(result);
        assertEquals(MOBILE_PHONE, result.getValue());
        assertEquals(MobilePhoneCertifiableSchema.CertificationEnum.NONE, result.getCertification());
    }

    @Test
    void toPhoneCertStringWithBlankValue() {
        MobilePhoneCertifiableSchema result = userMapper.toPhoneCertString(" ");
        assertNull(result);
    }

    @Test
    void toNameCertString() {
        NameCertifiableSchema result = userMapper.toNameCertString("John");

        assertNotNull(result);
        assertEquals("John", result.getValue());
        assertEquals(NameCertifiableSchema.CertificationEnum.NONE, result.getCertification());
    }

    @Test
    void toNameCertStringWithBlankValue() {
        NameCertifiableSchema result = userMapper.toNameCertString(" ");

        assertNull(result);
    }

    @Test
    void toFamilyNameCertString() {
        FamilyNameCertifiableSchema result = userMapper.toFamilyNameCertString("Doe");

        assertNotNull(result);
        assertEquals("Doe", result.getValue());
        assertEquals(FamilyNameCertifiableSchema.CertificationEnum.NONE, result.getCertification());
    }

    @Test
    void toFamilyNameCertStringWithBlankValue() {
        FamilyNameCertifiableSchema result = userMapper.toFamilyNameCertString(" ");
        assertNull(result);
    }

    @Test
    void getMaxStatusWithEmptyList() {
        List<OnboardedProduct> onboardedProductList = new ArrayList<>();

        String result = userMapper.getMaxStatus(onboardedProductList);

        assertNull(result);
    }

    @Test
    void getMaxStatusWithNonEmptyList() {
        OnboardedProduct product1 = new OnboardedProduct();
        product1.setStatus(OnboardedProductState.ACTIVE);
        OnboardedProduct product2 = new OnboardedProduct();
        product2.setStatus(OnboardedProductState.SUSPENDED);
        List<OnboardedProduct> onboardedProductList = List.of(product1, product2);

        String result = userMapper.getMaxStatus(onboardedProductList);

        assertEquals(OnboardedProductState.ACTIVE.name(), result);
    }

    @Test
    void getMaxRoleWithEmptyList() {
        List<OnboardedProduct> onboardedProductList = new ArrayList<>();
        String result = userMapper.getMaxRole(onboardedProductList);

        assertNull(result);
    }

    @Test
    void getMaxRoleWithNonEmptyList() {
        OnboardedProduct product1 = new OnboardedProduct();
        product1.setRole(PartyRole.MANAGER);
        OnboardedProduct product2 = new OnboardedProduct();
        product2.setRole(PartyRole.DELEGATE);
        List<OnboardedProduct> onboardedProductList = List.of(product1, product2);

        String result = userMapper.getMaxRole(onboardedProductList);

        assertEquals(PartyRole.MANAGER.name(), result);
    }
}
