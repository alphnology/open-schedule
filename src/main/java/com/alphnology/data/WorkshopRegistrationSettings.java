package com.alphnology.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "workshop_registration_settings", uniqueConstraints = @UniqueConstraint(columnNames = "singletonKey"))
public class WorkshopRegistrationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long code;

    @NotBlank
    @Column(nullable = false, updatable = false, length = 32)
    private String singletonKey = "default";

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 255)
    private String alfioBaseUrl;

    @Column(length = 100)
    private String eventSlug;

    @Column(columnDefinition = "TEXT")
    private String encryptedToken;

    @Column(columnDefinition = "TEXT")
    private String publicMessage;

    @Column(nullable = false)
    private boolean allowAttendeeWorkshopChange = false;

    @Column(nullable = false)
    private boolean showPublicMenuEntry = false;

    @Min(1)
    @Column(nullable = false)
    private Integer participantWorkshopLimit = 1;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
