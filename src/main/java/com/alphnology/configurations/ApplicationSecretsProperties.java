package com.alphnology.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "application.secrets")
public class ApplicationSecretsProperties {

    private String masterKey;
    private boolean allowUiPersistence = false;
}
