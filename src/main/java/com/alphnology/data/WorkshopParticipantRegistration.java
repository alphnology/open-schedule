package com.alphnology.data;

import com.alphnology.data.enums.WorkshopRegistrationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "workshop_participant_registration",
        uniqueConstraints = @UniqueConstraint(name = "uk_workshop_registration_ticket", columnNames = {"eventSlug", "ticketPublicId"}),
        indexes = {
                @Index(name = "idx_workshop_registration_session_code", columnList = "session_code"),
                @Index(name = "idx_workshop_registration_email", columnList = "attendeeEmail"),
                @Index(name = "idx_workshop_registration_name", columnList = "attendeeName")
        })
public class WorkshopParticipantRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long code;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String eventSlug;

    @NotBlank
    @Column(nullable = false, length = 128)
    private String orderReference;

    @Column(length = 128)
    private String reservationId;

    @Column(length = 128)
    private String ticketId;

    @Column(length = 128)
    private String ticketPublicId;

    @Column(length = 64)
    private String reservationShortCode;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String attendeeName;

    @Email
    @NotBlank
    @Column(nullable = false, length = 255)
    private String attendeeEmail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_code", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkshopRegistrationStatus status = WorkshopRegistrationStatus.ACTIVE;

    @Column(length = 64)
    private String alfioReservationStatus;

    @Column(columnDefinition = "TEXT")
    private String alfioPayloadJson;

    @Column(nullable = false)
    private LocalDateTime validatedAt;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (registeredAt == null) {
            registeredAt = now;
        }
        if (validatedAt == null) {
            validatedAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Deprecated(forRemoval = false)
    public String getTicketReference() {
        return orderReference;
    }

    @Deprecated(forRemoval = false)
    public void setTicketReference(String ticketReference) {
        this.orderReference = ticketReference;
    }
}
