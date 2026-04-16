package it.pagopa.selfcare.mscore.model.notification;

import lombok.Data;

import java.util.List;

@Data
public class MultipleReceiverMessageRequest {

    String content;
    String subject;
    List<String> receiverEmails;
}
