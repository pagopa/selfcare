package it.pagopa.selfcare.mscore.core.util.model;

import it.pagopa.selfcare.mscore.model.CertifiedField;
import it.pagopa.selfcare.mscore.model.institution.WorkContact;
import it.pagopa.selfcare.mscore.model.user.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DummyUser extends User {

 public DummyUser(String institutionId){
     this.setId(UUID.randomUUID().toString());
     this.setFiscalCode("fiscalCode");
     this.setName(setValue("name"));
     this.setFamilyName(setValue("familyName"));
     this.setWorkContacts(setContact(institutionId, setWorkContact("email")));
 }

 private CertifiedField<String> setValue(String value){
     CertifiedField<String> certifiedField = new CertifiedField<>();
     certifiedField.setValue(value);
     return certifiedField;
 }

 private Map<String, WorkContact> setContact(String institutionId, WorkContact workContact){
     Map<String, WorkContact> contactMap = new HashMap<>();
     contactMap.put(institutionId, workContact);
     return contactMap;
 }

 private WorkContact setWorkContact(String value){
     WorkContact contact = new WorkContact();
     contact.setEmail(setValue(value));
     return contact;
 }
}
