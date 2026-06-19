package com.alphnology.configurations;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalBrandingService {

    private static final String RESOURCE_PREFIX = "/branding-overrides/";

    private final BrandingProperties brandingProperties;

    @PostConstruct
    void logConfigurationStatus() {
        String configuredPath = brandingProperties.getExternalCssPath();
        if (!StringUtils.hasText(configuredPath)) {
            return;
        }

        resolveConfiguredCssPath().ifPresentOrElse(
                path -> log.info("External branding CSS enabled: {}", path),
                () -> log.warn("External branding CSS was configured but not found: {}", configuredPath)
        );
    }

    public Optional<Path> getExternalCssPath() {
        return resolveConfiguredCssPath().filter(Files::isRegularFile);
    }

    public Optional<Path> getExternalCssDirectory() {
        return getExternalCssPath().map(Path::getParent);
    }

    public Optional<String> getExternalCssUrl() {
        return getExternalCssPath().map(path -> {
            String encodedFileName = URLEncoder.encode(path.getFileName().toString(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            try {
                long lastModified = Files.getLastModifiedTime(path).toMillis();
                return RESOURCE_PREFIX + encodedFileName + "?v=" + lastModified;
            } catch (java.io.IOException exception) {
                throw new UncheckedIOException("Failed to read branding CSS metadata for " + path, exception);
            }
        });
    }

    public boolean matchesRequestPath(String requestPath) {
        return getExternalCssPath()
                .map(path -> (RESOURCE_PREFIX + path.getFileName()).equals(requestPath))
                .orElse(false);
    }

    private Optional<Path> resolveConfiguredCssPath() {
        String configuredPath = brandingProperties.getExternalCssPath();
        if (!StringUtils.hasText(configuredPath)) {
            return Optional.empty();
        }

        Path cssPath = Paths.get(configuredPath.trim());
        if (!cssPath.isAbsolute()) {
            cssPath = Paths.get(System.getProperty("user.dir")).resolve(cssPath).normalize();
        }

        return Optional.of(cssPath);
    }
}
