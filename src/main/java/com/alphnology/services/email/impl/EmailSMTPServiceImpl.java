package com.alphnology.services.email.impl;

import com.alphnology.services.email.EmailMessage;
import com.alphnology.services.email.EmailSendException;
import com.alphnology.services.email.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "climacall.email.smtp", value = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailSMTPServiceImpl implements EmailService {

    @Value("${application.email.smtp.host}")
    private String smtpHost;
    @Value("${application.email.smtp.port}")
    private int smtpPort;
    @Value("${application.email.smtp.username}")
    private String smtpUsername;
    @Value("${application.email.from.address}")
    private String fromAddress;
    @Value("${application.email.from.name}")
    private String fromName;
    @Value("${application.email.smtp.password}")
    private String smtpPassword;

    private Properties props;

    @PostConstruct
    public void init() {
        // Initialize SMTP settings or any other required setup
        log.info("SMTP Email Service initialized with host: {}", smtpHost);

        props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.ssl.trust", smtpHost);
    }

    private Session getSession() {
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });
    }

    @Override
    public void sendEmail(EmailMessage emailMessage) throws EmailSendException {

        log.info("Sending email to: {}", emailMessage.getTo());
        log.info("From: {}", emailMessage.getFrom());
        log.info("Subject: {}", emailMessage.getSubject());
        log.info("Body: {}", emailMessage.getBody());

        try {
            // Create a message
            Message message = new MimeMessage(getSession());
            String fn = emailMessage.getFrom() == null ? fromName : emailMessage.getFrom();
            String fe = fromAddress == null ? smtpUsername : fromAddress;
            message.setFrom(new InternetAddress(fe, fn));
            Address[] address = new InternetAddress[emailMessage.getTo().size()];

            int i = 0;
            for (String to : emailMessage.getTo()) {
                address[i++] = new InternetAddress(to);
            }

            message.setRecipients(Message.RecipientType.TO, address);
            message.setSubject(emailMessage.getSubject());

            Multipart multipart = getMultipart(emailMessage);

            message.setContent(multipart);

            Transport.send(message);
        } catch (MessagingException | IOException ex) {
            throw new EmailSendException("Failed to send email", ex);
        }
    }

    private static Multipart getMultipart(EmailMessage emailMessage) throws MessagingException, IOException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(emailMessage.getBody(), "text/html");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);

        Set<Path> attachments = emailMessage.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            // attach files to the email

            for (Path file : attachments) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(file.toFile());
                multipart.addBodyPart(attachmentPart);
            }
        }
        return multipart;
    }

}
