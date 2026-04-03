package com.alphnology.services.email;

import com.alphnology.data.enums.MailProviderType;
import com.alphnology.data.enums.MailSecurityMode;

public record MailSettingsSnapshot(
        boolean outboundEnabled,
        MailProviderType providerType,
        MailSecurityMode securityMode,
        boolean authenticationEnabled,
        String host,
        Integer port,
        String username,
        String secret,
        String fromAddress,
        String fromName,
        String postalBaseUrl,
        String postalApiKeyHeader,
        String sslTrust,
        Integer connectionTimeoutMs,
        Integer readTimeoutMs,
        String testRecipient,
        boolean secretPersisted,
        boolean uiSecretPersistenceAvailable
) {
}
