package com.alphnology.data;

import com.alphnology.data.enums.MailProviderType;
import com.alphnology.data.enums.MailSecurityMode;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mail_settings", uniqueConstraints = @UniqueConstraint(columnNames = "singletonKey"))
public class MailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long code;

    @NotBlank
    @Column(nullable = false, updatable = false, length = 32)
    private String singletonKey = "default";

    @Column(nullable = false)
    private boolean outboundEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MailProviderType providerType = MailProviderType.SMTP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MailSecurityMode securityMode = MailSecurityMode.STARTTLS;

    @Column(nullable = false)
    private boolean authenticationEnabled = true;

    @Column(length = 255)
    private String host;

    @Min(1)
    @Max(65535)
    private Integer port;

    @Column(length = 255)
    private String username;

    @Column(columnDefinition = "TEXT")
    private String encryptedSecret;

    @Email
    @Column(length = 255)
    private String fromAddress;

    @Column(length = 255)
    private String fromName;

    @Column(length = 255)
    private String postalBaseUrl;

    @Column(length = 255)
    private String postalApiKeyHeader = "X-Server-API-Key";

    @Column(length = 255)
    private String sslTrust;

    @Min(1000)
    private Integer connectionTimeoutMs;

    @Min(1000)
    private Integer readTimeoutMs;

    @Email
    @Column(length = 255)
    private String testRecipient;
}
