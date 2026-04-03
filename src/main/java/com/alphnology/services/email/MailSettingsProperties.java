package com.alphnology.services.email;

import com.alphnology.data.enums.MailProviderType;
import com.alphnology.data.enums.MailSecurityMode;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "application.email")
public class MailSettingsProperties {

    private String settingsMasterKey;
    private boolean allowUiSecretPersistence = false;
    private final Defaults defaults = new Defaults();
    private final Smtp smtp = new Smtp();
    private final Postal postal = new Postal();
    private final From from = new From();

    @Getter
    @Setter
    public static class Defaults {
        private boolean outboundEnabled = false;
        private MailProviderType providerType = MailProviderType.SMTP;
        private MailSecurityMode securityMode = MailSecurityMode.STARTTLS;
        private boolean authenticationEnabled = true;
        private Integer connectionTimeoutMs = 10000;
        private Integer readTimeoutMs = 10000;
    }

    @Getter
    @Setter
    public static class Smtp {
        private boolean enabled = true;
        private String host;
        private Integer port = 587;
        private String username;
        private String password;
        private String sslTrust;
    }

    @Getter
    @Setter
    public static class Postal {
        private boolean enabled = false;
        private String baseUrl;
        private String apiKey;
        private String apiKeyHeader = "X-Server-API-Key";
    }

    @Getter
    @Setter
    public static class From {
        @Email
        private String address;
        private String name = "Open Schedule";
    }
}
