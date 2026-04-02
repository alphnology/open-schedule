package com.alphnology.services.email;

import com.alphnology.services.email.transport.MailTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMailSenderService implements MailSenderService {

    private final MailSettingsService mailSettingsService;
    private final List<MailTransport> transports;

    @Override
    public void sendEmail(EmailMessage emailMessage) throws EmailSendException {
        MailSettingsSnapshot settings = mailSettingsService.getEffectiveSettings();
        if (!settings.outboundEnabled()) {
            log.warn("Outbound email is disabled. Skipping subject '{}'", emailMessage.getSubject());
            return;
        }
        validateSettings(settings);
        resolveTransport(settings).send(emailMessage, settings);
    }

    @Override
    public void sendTestEmail(String recipient) throws EmailSendException {
        MailSettingsSnapshot settings = mailSettingsService.getEffectiveSettings();
        if (!settings.outboundEnabled()) {
            throw new EmailSendException("Outbound email is disabled");
        }
        validateSettings(settings);
        EmailMessage emailMessage = new EmailMessage();
        emailMessage.setTo(Set.of(recipient));
        emailMessage.setSubject("Open Schedule mail test");
        emailMessage.setBody("""
                <h2>Open Schedule mail test</h2>
                <p>Your outbound email configuration is working.</p>
                <p><strong>Provider:</strong> %s</p>
                <p><strong>Time:</strong> %s</p>
                """.formatted(settings.providerType(), Instant.now()));
        resolveTransport(settings).send(emailMessage, settings);
    }

    private MailTransport resolveTransport(MailSettingsSnapshot settings) throws EmailSendException {
        return transports.stream()
                .filter(transport -> transport.supports(settings))
                .findFirst()
                .orElseThrow(() -> new EmailSendException("No mail transport found for provider " + settings.providerType()));
    }

    private void validateSettings(MailSettingsSnapshot settings) throws EmailSendException {
        if (!StringUtils.hasText(settings.fromAddress())) {
            throw new EmailSendException("From email address is required");
        }
        if (settings.providerType() == com.alphnology.data.enums.MailProviderType.POSTAL) {
            if (!StringUtils.hasText(settings.postalBaseUrl())) {
                throw new EmailSendException("Postal base URL is required");
            }
            if (!StringUtils.hasText(settings.secret())) {
                throw new EmailSendException("Postal API key is required");
            }
            return;
        }
        if (!StringUtils.hasText(settings.host())) {
            throw new EmailSendException("SMTP host is required");
        }
        if (settings.port() == null) {
            throw new EmailSendException("SMTP port is required");
        }
        if (settings.authenticationEnabled()) {
            if (!StringUtils.hasText(settings.username())) {
                throw new EmailSendException("SMTP username is required when authentication is enabled");
            }
            if (!StringUtils.hasText(settings.secret())) {
                throw new EmailSendException("SMTP password/token is required when authentication is enabled");
            }
        }
    }
}
