package com.alphnology.services;

import com.alphnology.data.WorkshopRegistrationSettings;
import com.alphnology.data.repository.WorkshopRegistrationSettingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkshopRegistrationSettingsService {

    private static final String DEFAULT_KEY = "default";
    private static final Logger log = LoggerFactory.getLogger(WorkshopRegistrationSettingsService.class);

    private final WorkshopRegistrationSettingsRepository repository;
    private final WorkshopRegistrationSecretCodec secretCodec;

    @Transactional
    public WorkshopRegistrationSettingsSnapshot getEffectiveSettings() {
        WorkshopRegistrationSettings stored = repository.findBySingletonKey(DEFAULT_KEY).orElseGet(this::newDefaultsEntity);
        return toSnapshot(stored);
    }

    @Transactional
    public WorkshopRegistrationSettings save(WorkshopRegistrationSettingsUpdateRequest request) {
        WorkshopRegistrationSettings settings = repository.findBySingletonKey(DEFAULT_KEY).orElseGet(this::newDefaultsEntity);
        boolean hadStoredToken = StringUtils.hasText(settings.getEncryptedToken());
        String normalizedToken = trimToNull(request.token());
        boolean hasNewToken = StringUtils.hasText(normalizedToken);
        settings.setEnabled(request.enabled());
        settings.setActive(request.active());
        settings.setAlfioBaseUrl(trimToNull(request.alfioBaseUrl()));
        settings.setEventSlug(trimToNull(request.eventSlug()));
        settings.setPublicMessage(trimToNull(request.publicMessage()));
        settings.setAllowAttendeeWorkshopChange(request.allowAttendeeWorkshopChange());
        settings.setShowPublicMenuEntry(request.showPublicMenuEntry());
        settings.setParticipantWorkshopLimit(request.participantWorkshopLimit());

        if (request.clearStoredToken()) {
            settings.setEncryptedToken(null);
        } else if (StringUtils.hasText(normalizedToken)) {
            settings.setEncryptedToken(secretCodec.encrypt(normalizedToken));
        }

        WorkshopRegistrationSettings saved = repository.save(settings);
        log.info(
                "Workshop registration settings saved: enabled={}, active={}, eventSlug='{}', baseUrl='{}', tokenAction={}, tokenPersisted={}",
                saved.isEnabled(),
                saved.isActive(),
                saved.getEventSlug(),
                saved.getAlfioBaseUrl(),
                resolveTokenAction(request.clearStoredToken(), hasNewToken, hadStoredToken),
                StringUtils.hasText(saved.getEncryptedToken())
        );
        return saved;
    }

    @Transactional
    public Optional<WorkshopRegistrationSettings> findStoredSettings() {
        return repository.findBySingletonKey(DEFAULT_KEY);
    }

    public @Nullable String getTokenForRuntime(WorkshopRegistrationSettings settings) {
        try {
            return secretCodec.decrypt(settings.getEncryptedToken());
        } catch (IllegalStateException ex) {
            log.warn("Workshop registration token could not be loaded for runtime. Falling back to an unconfigured state: {}", ex.getMessage());
            return null;
        }
    }

    private WorkshopRegistrationSettingsSnapshot toSnapshot(WorkshopRegistrationSettings settings) {
        boolean tokenPersisted = StringUtils.hasText(settings.getEncryptedToken());
        return new WorkshopRegistrationSettingsSnapshot(
                settings.isEnabled(),
                settings.isActive(),
                settings.getAlfioBaseUrl(),
                settings.getEventSlug(),
                getTokenForRuntime(settings),
                settings.getPublicMessage(),
                settings.isAllowAttendeeWorkshopChange(),
                settings.isShowPublicMenuEntry(),
                settings.getParticipantWorkshopLimit(),
                tokenPersisted,
                secretCodec.canPersistSecrets()
        );
    }

    private WorkshopRegistrationSettings newDefaultsEntity() {
        WorkshopRegistrationSettings settings = new WorkshopRegistrationSettings();
        settings.setSingletonKey(DEFAULT_KEY);
        settings.setEnabled(false);
        settings.setActive(true);
        settings.setAllowAttendeeWorkshopChange(false);
        settings.setShowPublicMenuEntry(false);
        settings.setParticipantWorkshopLimit(1);
        return settings;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String resolveTokenAction(boolean clearStoredToken, boolean hasNewToken, boolean hadStoredToken) {
        if (clearStoredToken) {
            return "cleared";
        }
        if (hasNewToken) {
            return hadStoredToken ? "replaced" : "created";
        }
        return hadStoredToken ? "retained" : "absent";
    }

    public record WorkshopRegistrationSettingsSnapshot(
            boolean enabled,
            boolean active,
            String alfioBaseUrl,
            String eventSlug,
            String token,
            String publicMessage,
            boolean allowAttendeeWorkshopChange,
            boolean showPublicMenuEntry,
            Integer participantWorkshopLimit,
            boolean tokenPersisted,
            boolean tokenPersistenceEnabled
    ) {
        public boolean isConfigured() {
            return StringUtils.hasText(alfioBaseUrl)
                    && StringUtils.hasText(eventSlug)
                    && StringUtils.hasText(token);
        }
    }

    public record WorkshopRegistrationSettingsUpdateRequest(
            boolean enabled,
            boolean active,
            String alfioBaseUrl,
            String eventSlug,
            String token,
            boolean clearStoredToken,
            String publicMessage,
            boolean allowAttendeeWorkshopChange,
            boolean showPublicMenuEntry,
            Integer participantWorkshopLimit
    ) {
    }
}
