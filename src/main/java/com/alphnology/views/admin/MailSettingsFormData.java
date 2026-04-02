package com.alphnology.views.admin;

import com.alphnology.data.enums.MailProviderType;
import com.alphnology.data.enums.MailSecurityMode;
import com.alphnology.services.email.MailSettingsService;
import com.alphnology.services.email.MailSettingsSnapshot;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailSettingsFormData {

    private boolean outboundEnabled;

    @NotNull
    private MailProviderType providerType;

    @NotNull
    private MailSecurityMode securityMode;

    private boolean authenticationEnabled;

    private String host;

    @Min(1)
    @Max(65535)
    private Integer port;

    private String username;
    private String secret;
    private boolean clearStoredSecret;

    @Email
    private String fromAddress;

    private String fromName;
    private String postalBaseUrl;
    private String postalApiKeyHeader;
    private String sslTrust;

    @Min(1000)
    private Integer connectionTimeoutMs;

    @Min(1000)
    private Integer readTimeoutMs;

    @Email
    private String testRecipient;

    public static MailSettingsFormData fromSnapshot(MailSettingsSnapshot snapshot) {
        MailSettingsFormData data = new MailSettingsFormData();
        data.setOutboundEnabled(snapshot.outboundEnabled());
        data.setProviderType(snapshot.providerType());
        data.setSecurityMode(snapshot.securityMode());
        data.setAuthenticationEnabled(snapshot.authenticationEnabled());
        data.setHost(snapshot.host());
        data.setPort(snapshot.port());
        data.setUsername(snapshot.username());
        data.setFromAddress(snapshot.fromAddress());
        data.setFromName(snapshot.fromName());
        data.setPostalBaseUrl(snapshot.postalBaseUrl());
        data.setPostalApiKeyHeader(snapshot.postalApiKeyHeader());
        data.setSslTrust(snapshot.sslTrust());
        data.setConnectionTimeoutMs(snapshot.connectionTimeoutMs());
        data.setReadTimeoutMs(snapshot.readTimeoutMs());
        data.setTestRecipient(snapshot.testRecipient());
        return data;
    }

    public MailSettingsService.MailSettingsUpdateRequest toUpdateRequest() {
        return new MailSettingsService.MailSettingsUpdateRequest(
                outboundEnabled,
                providerType,
                securityMode,
                authenticationEnabled,
                host,
                port,
                username,
                secret,
                clearStoredSecret,
                fromAddress,
                fromName,
                postalBaseUrl,
                postalApiKeyHeader,
                sslTrust,
                connectionTimeoutMs,
                readTimeoutMs,
                testRecipient
        );
    }
}
