package com.alphnology.services.email;

public interface MailSenderService {

    void sendEmail(EmailMessage emailMessage) throws EmailSendException;

    void sendTestEmail(String recipient) throws EmailSendException;
}
