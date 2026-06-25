package it.pagopa.selfcare.mscore.model.notification;

import lombok.Data;

@Data
public class MessageRequest {

    String content;
    String subject;
    String senderEmail;
    String receiverEmail;
}
