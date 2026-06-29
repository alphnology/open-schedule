package com.alphnology.services;

import com.alphnology.data.WorkshopRegistrationSettings;
import com.alphnology.data.repository.WorkshopRegistrationSettingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkshopRegistrationSettingsService {

    private static final String DEFAULT_KEY = "default";

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
        settings.setEnabled(request.enabled());
        settings.setActive(request.active());
        settings.setAlfioBaseUrl(trimToNull(request.alfioBaseUrl()));
        settings.setEventSlug(trimToNull(request.eventSlug()));
        settings.setPublicMessage(trimToNull(request.publicMessage()));
        settings.setParticipantWorkshopLimit(request.participantWorkshopLimit());

        if (request.clearStoredToken()) {
            settings.setEncryptedToken(null);
        } else if (StringUtils.hasText(request.token())) {
            settings.setEncryptedToken(secretCodec.encrypt(request.token()));
        }

        return repository.save(settings);
    }

    @Transactional
    public Optional<WorkshopRegistrationSettings> findStoredSettings() {
        return repository.findBySingletonKey(DEFAULT_KEY);
    }

    public String getTokenForRuntime(WorkshopRegistrationSettings settings) {
        return secretCodec.decrypt(settings.getEncryptedToken());
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
        settings.setParticipantWorkshopLimit(1);
        return settings;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record WorkshopRegistrationSettingsSnapshot(
            boolean enabled,
            boolean active,
            String alfioBaseUrl,
            String eventSlug,
            String token,
            String publicMessage,
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
            Integer participantWorkshopLimit
    ) {
    }
}
