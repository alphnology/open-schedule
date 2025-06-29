package com.alphnology.services.email;

public interface EmailService {

    void sendEmail(EmailMessage emailMessage) throws EmailSendException;

}
