package com.alphnology.services.email.transport;

import com.alphnology.data.enums.MailProviderType;
import com.alphnology.data.enums.MailSecurityMode;
import com.alphnology.services.email.EmailMessage;
import com.alphnology.services.email.EmailSendException;
import com.alphnology.services.email.MailSettingsSnapshot;
import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

@Slf4j
@Component
public class SmtpMailTransport implements MailTransport {

    @Override
    public boolean supports(MailSettingsSnapshot settings) {
        return settings.providerType() == MailProviderType.SMTP
                || settings.providerType() == MailProviderType.SENDGRID
                || settings.providerType() == MailProviderType.MAILJET;
    }

    @Override
    public void send(EmailMessage emailMessage, MailSettingsSnapshot settings) throws EmailSendException {
        try {
            Message message = new MimeMessage(createSession(settings));
            String fromName = StringUtils.hasText(emailMessage.getFrom()) ? emailMessage.getFrom() : settings.fromName();
            String fromAddress = StringUtils.hasText(settings.fromAddress()) ? settings.fromAddress() : settings.username();
            message.setFrom(new InternetAddress(fromAddress, fromName));
            message.setRecipients(Message.RecipientType.TO, toAddresses(emailMessage));
            message.setSubject(emailMessage.getSubject());
            message.setContent(getMultipart(emailMessage));
            Transport.send(message);
        } catch (Exception ex) {
            throw new EmailSendException("Failed to send email via SMTP provider", ex);
        }
    }

    private Session createSession(MailSettingsSnapshot settings) {
        Properties props = new Properties();
        props.put("mail.smtp.host", settings.host());
        props.put("mail.smtp.port", String.valueOf(settings.port()));
        props.put("mail.smtp.connectiontimeout", String.valueOf(settings.connectionTimeoutMs()));
        props.put("mail.smtp.timeout", String.valueOf(settings.readTimeoutMs()));
        props.put("mail.smtp.writetimeout", String.valueOf(settings.readTimeoutMs()));
        props.put("mail.smtp.auth", String.valueOf(settings.authenticationEnabled()));

        if (settings.securityMode() == MailSecurityMode.STARTTLS) {
            props.put("mail.smtp.starttls.enable", "true");
        } else if (settings.securityMode() == MailSecurityMode.SSL_TLS) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        if (StringUtils.hasText(settings.sslTrust())) {
            props.put("mail.smtp.ssl.trust", settings.sslTrust());
        }

        if (settings.authenticationEnabled()) {
            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(settings.username(), settings.secret());
                }
            });
        }
        return Session.getInstance(props);
    }

    private static Address[] toAddresses(EmailMessage emailMessage) throws Exception {
        Address[] address = new InternetAddress[emailMessage.getTo().size()];
        int i = 0;
        for (String to : emailMessage.getTo()) {
            address[i++] = new InternetAddress(to);
        }
        return address;
    }

    private static Multipart getMultipart(EmailMessage emailMessage) throws jakarta.mail.MessagingException, IOException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(emailMessage.getBody(), "text/html; charset=UTF-8");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);

        Set<Path> attachments = emailMessage.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            for (Path file : attachments) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(file.toFile());
                multipart.addBodyPart(attachmentPart);
            }
        }
        return multipart;
    }
}
