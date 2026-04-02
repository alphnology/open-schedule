package com.alphnology.services.email;

import com.alphnology.data.MailSettings;
import com.alphnology.data.enums.MailProviderType;
import com.alphnology.data.repository.MailSettingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MailSettingsService {

    private static final String DEFAULT_KEY = "default";

    private final MailSettingsRepository repository;
    private final MailSettingsProperties properties;
    private final MailSecretCodec secretCodec;

    @Transactional
    public MailSettingsSnapshot getEffectiveSettings() {
        MailSettings stored = repository.findBySingletonKey(DEFAULT_KEY).orElseGet(this::newDefaultsEntity);
        return toSnapshot(stored);
    }

    @Transactional
    public MailSettings save(MailSettingsUpdateRequest request) {
        MailSettings settings = repository.findBySingletonKey(DEFAULT_KEY).orElseGet(this::newDefaultsEntity);
        settings.setOutboundEnabled(request.outboundEnabled());
        settings.setProviderType(request.providerType());
        settings.setSecurityMode(request.securityMode());
        settings.setAuthenticationEnabled(request.authenticationEnabled());
        settings.setHost(trimToNull(request.host()));
        settings.setPort(request.port());
        settings.setUsername(trimToNull(request.username()));
        settings.setFromAddress(trimToNull(request.fromAddress()));
        settings.setFromName(trimToNull(request.fromName()));
        settings.setPostalBaseUrl(trimToNull(request.postalBaseUrl()));
        settings.setPostalApiKeyHeader(trimToNull(request.postalApiKeyHeader()));
        settings.setSslTrust(trimToNull(request.sslTrust()));
        settings.setConnectionTimeoutMs(request.connectionTimeoutMs());
        settings.setReadTimeoutMs(request.readTimeoutMs());
        settings.setTestRecipient(trimToNull(request.testRecipient()));

        if (request.clearStoredSecret()) {
            settings.setEncryptedSecret(null);
        } else if (StringUtils.hasText(request.secret())) {
            settings.setEncryptedSecret(secretCodec.encrypt(request.secret()));
        }

        applyProviderDefaults(settings);
        return repository.save(settings);
    }

    @Transactional
    public Optional<MailSettings> findStoredSettings() {
        return repository.findBySingletonKey(DEFAULT_KEY);
    }

    public String getSecretForRuntime(MailSettings settings) {
        String persisted = secretCodec.decrypt(settings.getEncryptedSecret());
        if (StringUtils.hasText(persisted)) {
            return persisted;
        }
        return switch (settings.getProviderType()) {
            case SMTP, SENDGRID, MAILJET -> properties.getSmtp().getPassword();
            case POSTAL -> properties.getPostal().getApiKey();
        };
    }

    private MailSettingsSnapshot toSnapshot(MailSettings settings) {
        String fallbackHost = properties.getSmtp().getHost();
        Integer fallbackPort = properties.getSmtp().getPort();
        String fallbackUser = properties.getSmtp().getUsername();
        String fallbackFromAddress = properties.getFrom().getAddress();
        String fallbackFromName = properties.getFrom().getName();
        String fallbackPostalUrl = properties.getPostal().getBaseUrl();
        String fallbackPostalHeader = properties.getPostal().getApiKeyHeader();
        String fallbackSslTrust = properties.getSmtp().getSslTrust();
        boolean secretPersisted = StringUtils.hasText(settings.getEncryptedSecret());

        return new MailSettingsSnapshot(
                settings.isOutboundEnabled(),
                settings.getProviderType(),
                settings.getSecurityMode(),
                settings.isAuthenticationEnabled(),
                coalesce(settings.getHost(), fallbackHost),
                settings.getPort() != null ? settings.getPort() : fallbackPort,
                coalesce(settings.getUsername(), fallbackUser),
                getSecretForRuntime(settings),
                coalesce(settings.getFromAddress(), fallbackFromAddress),
                coalesce(settings.getFromName(), fallbackFromName),
                coalesce(settings.getPostalBaseUrl(), fallbackPostalUrl),
                coalesce(settings.getPostalApiKeyHeader(), fallbackPostalHeader),
                coalesce(settings.getSslTrust(), fallbackSslTrust),
                settings.getConnectionTimeoutMs() != null ? settings.getConnectionTimeoutMs() : properties.getDefaults().getConnectionTimeoutMs(),
                settings.getReadTimeoutMs() != null ? settings.getReadTimeoutMs() : properties.getDefaults().getReadTimeoutMs(),
                settings.getTestRecipient(),
                secretPersisted,
                secretCodec.canPersistSecrets()
        );
    }

    private MailSettings newDefaultsEntity() {
        MailSettings settings = new MailSettings();
        settings.setSingletonKey(DEFAULT_KEY);
        settings.setOutboundEnabled(properties.getDefaults().isOutboundEnabled());
        settings.setProviderType(resolveDefaultProvider());
        settings.setSecurityMode(properties.getDefaults().getSecurityMode());
        settings.setAuthenticationEnabled(properties.getDefaults().isAuthenticationEnabled());
        settings.setHost(properties.getSmtp().getHost());
        settings.setPort(properties.getSmtp().getPort());
        settings.setUsername(properties.getSmtp().getUsername());
        settings.setFromAddress(properties.getFrom().getAddress());
        settings.setFromName(properties.getFrom().getName());
        settings.setPostalBaseUrl(properties.getPostal().getBaseUrl());
        settings.setPostalApiKeyHeader(properties.getPostal().getApiKeyHeader());
        settings.setSslTrust(properties.getSmtp().getSslTrust());
        settings.setConnectionTimeoutMs(properties.getDefaults().getConnectionTimeoutMs());
        settings.setReadTimeoutMs(properties.getDefaults().getReadTimeoutMs());
        applyProviderDefaults(settings);
        return settings;
    }

    private MailProviderType resolveDefaultProvider() {
        if (properties.getPostal().isEnabled()) {
            return MailProviderType.POSTAL;
        }
        return properties.getDefaults().getProviderType();
    }

    private void applyProviderDefaults(MailSettings settings) {
        if (settings.getProviderType() == MailProviderType.SENDGRID) {
            if (!StringUtils.hasText(settings.getHost())) {
                settings.setHost("smtp.sendgrid.net");
            }
            if (settings.getPort() == null) {
                settings.setPort(587);
            }
            if (!StringUtils.hasText(settings.getUsername())) {
                settings.setUsername("apikey");
            }
            settings.setSecurityMode(com.alphnology.data.enums.MailSecurityMode.STARTTLS);
            settings.setAuthenticationEnabled(true);
        } else if (settings.getProviderType() == MailProviderType.MAILJET) {
            if (!StringUtils.hasText(settings.getHost())) {
                settings.setHost("in-v3.mailjet.com");
            }
            if (settings.getPort() == null) {
                settings.setPort(587);
            }
            settings.setSecurityMode(com.alphnology.data.enums.MailSecurityMode.STARTTLS);
            settings.setAuthenticationEnabled(true);
        } else if (settings.getProviderType() == MailProviderType.POSTAL) {
            settings.setHost(null);
            settings.setPort(null);
            settings.setSslTrust(null);
            settings.setSecurityMode(com.alphnology.data.enums.MailSecurityMode.NONE);
            settings.setAuthenticationEnabled(true);
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String coalesce(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    public record MailSettingsUpdateRequest(
            boolean outboundEnabled,
            MailProviderType providerType,
            com.alphnology.data.enums.MailSecurityMode securityMode,
            boolean authenticationEnabled,
            String host,
            Integer port,
            String username,
            String secret,
            boolean clearStoredSecret,
            String fromAddress,
            String fromName,
            String postalBaseUrl,
            String postalApiKeyHeader,
            String sslTrust,
            Integer connectionTimeoutMs,
            Integer readTimeoutMs,
            String testRecipient
    ) {}
}
