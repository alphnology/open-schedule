package com.alphnology.services.email;

import com.alphnology.data.enums.MailProviderType;
import com.alphnology.data.enums.MailSecurityMode;
import com.alphnology.services.email.transport.MailTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultMailSenderServiceTest {

    @Mock
    private MailSettingsService mailSettingsService;

    @Mock
    private MailTransport transport;

    @Test
    void sendEmailSkipsDeliveryWhenOutboundIsDisabled() throws Exception {
        when(mailSettingsService.getEffectiveSettings()).thenReturn(snapshot(false));

        DefaultMailSenderService service = new DefaultMailSenderService(mailSettingsService, List.of(transport));
        EmailMessage message = new EmailMessage();
        message.setTo(Set.of("user@example.com"));
        message.setSubject("Subject");
        message.setBody("<p>Body</p>");

        assertDoesNotThrow(() -> service.sendEmail(message));
        verify(transport, never()).send(any(), any());
    }

    @Test
    void sendTestEmailFailsFastWhenOutboundIsDisabled() {
        when(mailSettingsService.getEffectiveSettings()).thenReturn(snapshot(false));

        DefaultMailSenderService service = new DefaultMailSenderService(mailSettingsService, List.of(transport));

        EmailSendException ex = assertThrows(EmailSendException.class, () -> service.sendTestEmail("admin@example.com"));
        assertEquals("Outbound email is disabled", ex.getMessage());
    }

    private MailSettingsSnapshot snapshot(boolean outboundEnabled) {
        return new MailSettingsSnapshot(
                outboundEnabled,
                MailProviderType.SMTP,
                MailSecurityMode.STARTTLS,
                true,
                "smtp.example.com",
                587,
                "user",
                "secret",
                "no-reply@example.com",
                "Open Schedule",
                null,
                "X-Server-API-Key",
                null,
                10_000,
                10_000,
                "admin@example.com",
                false,
                true
        );
    }
}
