package com.alphnology.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "application.branding")
public class BrandingProperties {

    /**
     * Optional filesystem path to an external CSS file that overrides the bundled theme.
     * Supports both absolute paths and paths relative to the process working directory.
     */
    private String externalCssPath;

}
