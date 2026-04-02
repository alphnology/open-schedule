package com.alphnology.services.email.transport;

import com.alphnology.services.email.EmailMessage;
import com.alphnology.services.email.EmailSendException;
import com.alphnology.services.email.MailSettingsSnapshot;

public interface MailTransport {

    boolean supports(MailSettingsSnapshot settings);

    void send(EmailMessage emailMessage, MailSettingsSnapshot settings) throws EmailSendException;
}
