package com.alphnology.services.email;

import com.alphnology.data.MailSettings;
import com.alphnology.data.enums.MailProviderType;
import com.alphnology.data.enums.MailSecurityMode;
import com.alphnology.data.repository.MailSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailSettingsServiceTest {

    @Mock
    private MailSettingsRepository repository;

    @Mock
    private MailSecretCodec secretCodec;

    private MailSettingsProperties properties;
    private MailSettingsService service;

    @BeforeEach
    void setUp() {
        properties = new MailSettingsProperties();
        properties.getDefaults().setOutboundEnabled(true);
        properties.getDefaults().setProviderType(MailProviderType.SMTP);
        properties.getDefaults().setSecurityMode(MailSecurityMode.STARTTLS);
        properties.getDefaults().setAuthenticationEnabled(true);
        properties.getDefaults().setConnectionTimeoutMs(10_000);
        properties.getDefaults().setReadTimeoutMs(10_000);
        properties.getSmtp().setPort(587);
        properties.getFrom().setName("Open Schedule");
        properties.getPostal().setApiKeyHeader("X-Server-API-Key");

        service = new MailSettingsService(repository, properties, secretCodec);
    }

    @Test
    void getEffectiveSettingsAppliesSendGridDefaultsAndUsesPersistedSecret() {
        properties.getDefaults().setProviderType(MailProviderType.SENDGRID);
        properties.getSmtp().setPassword("top-secret");
        properties.getFrom().setAddress("no-reply@example.com");

        when(repository.findBySingletonKey("default")).thenReturn(Optional.empty());
        when(secretCodec.canPersistSecrets()).thenReturn(true);

        MailSettingsSnapshot snapshot = service.getEffectiveSettings();

        assertEquals(MailProviderType.SENDGRID, snapshot.providerType());
        assertEquals("smtp.sendgrid.net", snapshot.host());
        assertEquals(587, snapshot.port());
        assertEquals("apikey", snapshot.username());
        assertEquals("top-secret", snapshot.secret());
        assertEquals(MailSecurityMode.STARTTLS, snapshot.securityMode());
        assertTrue(!snapshot.secretPersisted());
    }

    @Test
    void savePostalSettingsClearsSmtpOnlyFieldsAndEncryptsSecret() {
        MailSettings stored = new MailSettings();
        when(repository.findBySingletonKey("default")).thenReturn(Optional.of(stored));
        when(secretCodec.encrypt("postal-secret")).thenReturn("enc");
        when(repository.save(any(MailSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.save(new MailSettingsService.MailSettingsUpdateRequest(
                true,
                MailProviderType.POSTAL,
                MailSecurityMode.STARTTLS,
                true,
                "smtp.example.com",
                2525,
                "user",
                "postal-secret",
                false,
                "no-reply@example.com",
                "Open Schedule",
                "https://postal.example.com",
                "X-Server-API-Key",
                "smtp.example.com",
                5000,
                7000,
                "admin@example.com"
        ));

        ArgumentCaptor<MailSettings> captor = ArgumentCaptor.forClass(MailSettings.class);
        verify(repository).save(captor.capture());
        MailSettings saved = captor.getValue();

        assertEquals(MailProviderType.POSTAL, saved.getProviderType());
        assertNull(saved.getHost());
        assertNull(saved.getPort());
        assertNull(saved.getSslTrust());
        assertEquals(MailSecurityMode.NONE, saved.getSecurityMode());
        assertEquals("enc", saved.getEncryptedSecret());
        assertEquals("https://postal.example.com", saved.getPostalBaseUrl());
    }
}
