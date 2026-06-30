package com.alphnology.services;

import com.alphnology.data.Room;
import com.alphnology.data.Session;
import com.alphnology.data.WorkshopParticipantRegistration;
import com.alphnology.data.enums.SessionType;
import com.alphnology.data.enums.WorkshopRegistrationStatus;
import com.alphnology.data.repository.WorkshopParticipantRegistrationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkshopRegistrationService {

    private final WorkshopParticipantRegistrationRepository repository;
    private final SessionService sessionService;
    private final EventService eventService;
    private final WorkshopRegistrationSettingsService settingsService;
    private final AlfioTicketValidationService alfioTicketValidationService;

    @Transactional
    public PublicModuleState getPublicModuleState() {
        var settings = settingsService.getEffectiveSettings();
        return new PublicModuleState(settings.enabled(), settings.active(), settings.publicMessage(), settings.isConfigured());
    }

    @Transactional
    public TicketValidationOutcome validateTicket(String rawTicketReference) {
        String ticketReference = normalizeTicketReference(rawTicketReference);
        var settings = settingsService.getEffectiveSettings();
        if (!settings.enabled() || !settings.active()) {
            throw new WorkshopRegistrationException("Workshop registration is currently unavailable.");
        }

        Optional<WorkshopParticipantRegistration> existingRegistration =
                repository.findByEventSlugAndTicketReference(settings.eventSlug(), ticketReference);
        if (existingRegistration.isPresent()) {
            return TicketValidationOutcome.alreadyRegistered(toExistingRegistration(existingRegistration.get()));
        }

        var validatedTicket = alfioTicketValidationService.validate(ticketReference, settings);
        List<WorkshopOption> workshops = listAvailableWorkshopOptions();
        return TicketValidationOutcome.validated(new ValidatedTicketView(
                validatedTicket.ticketReference(),
                validatedTicket.reservationShortCode(),
                validatedTicket.attendeeName(),
                validatedTicket.attendeeEmail(),
                validatedTicket.reservationStatus(),
                workshops
        ));
    }

    @Transactional
    public WorkshopParticipantRegistration registerWorkshop(String rawTicketReference, Long workshopId) {
        String ticketReference = normalizeTicketReference(rawTicketReference);
        var settings = settingsService.getEffectiveSettings();
        if (!settings.enabled() || !settings.active()) {
            throw new WorkshopRegistrationException("Workshop registration is currently unavailable.");
        }

        repository.findByEventSlugAndTicketReference(settings.eventSlug(), ticketReference)
                .ifPresent(existing -> {
                    throw new DuplicateWorkshopRegistrationException(
                            "This ticket is already registered in workshop " + existing.getSession().getTitle(), existing);
                });

        Session workshop = getWorkshopSession(workshopId);
        ensureCapacity(workshop, null);

        var validatedTicket = alfioTicketValidationService.validate(ticketReference, settings);

        WorkshopParticipantRegistration registration = new WorkshopParticipantRegistration();
        registration.setEventSlug(settings.eventSlug());
        registration.setTicketReference(validatedTicket.ticketReference());
        registration.setReservationShortCode(validatedTicket.reservationShortCode());
        registration.setAttendeeName(validatedTicket.attendeeName());
        registration.setAttendeeEmail(validatedTicket.attendeeEmail());
        registration.setSession(workshop);
        registration.setStatus(WorkshopRegistrationStatus.ACTIVE);
        registration.setAlfioReservationStatus(validatedTicket.reservationStatus());
        registration.setAlfioPayloadJson(validatedTicket.payloadJson());
        registration.setValidatedAt(LocalDateTime.now());
        registration.setRegisteredAt(LocalDateTime.now());
        return repository.save(registration);
    }

    @Transactional
    public WorkshopParticipantRegistration moveRegistration(Long registrationId, Long workshopId) {
        WorkshopParticipantRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new EntityNotFoundException("Workshop registration not found."));
        Session workshop = getWorkshopSession(workshopId);
        ensureCapacity(workshop, registration.getCode());
        registration.setSession(workshop);
        registration.setStatus(WorkshopRegistrationStatus.ACTIVE);
        return repository.save(registration);
    }

    @Transactional
    public void deleteRegistration(Long registrationId) {
        repository.deleteById(registrationId);
    }

    @Transactional
    public List<WorkshopParticipantRegistration> listRegistrations(String searchTerm) {
        return listRegistrations(searchTerm, null);
    }

    @Transactional
    public List<WorkshopParticipantRegistration> listRegistrations(String searchTerm, Long workshopId) {
        return repository.findAll(createSearchSpecification(searchTerm, workshopId)).stream()
                .sorted(Comparator.comparing(WorkshopParticipantRegistration::getRegisteredAt).reversed())
                .toList();
    }

    @Transactional
    public Optional<WorkshopParticipantRegistration> getRegistration(Long registrationId) {
        return repository.findById(registrationId);
    }

    @Transactional
    public List<WorkshopOption> listAvailableWorkshopOptions() {
        return listWorkshopSessions().stream()
                .filter(this::isWorkshopAvailableForRegistration)
                .map(this::toWorkshopOption)
                .toList();
    }

    @Transactional
    public List<WorkshopOption> listAllWorkshopOptions() {
        return listWorkshopSessions().stream().map(this::toWorkshopOption).toList();
    }

    private Session getWorkshopSession(Long workshopId) {
        Session session = sessionService.get(workshopId)
                .orElseThrow(() -> new EntityNotFoundException("Workshop session not found."));
        if (session.getType() != SessionType.W) {
            throw new WorkshopRegistrationException("The selected session is not a workshop.");
        }
        if (session.getRoom() == null) {
            throw new WorkshopRegistrationException("The selected workshop does not have a room configured.");
        }
        return session;
    }

    private void ensureCapacity(Session workshop, Long excludeRegistrationId) {
        Room room = workshop.getRoom();
        if (room == null) {
            throw new WorkshopRegistrationException("The selected workshop does not have a room configured.");
        }
        int capacity = room.getCapacity();
        if (capacity <= 0) {
            return;
        }
        long activeRegistrations = excludeRegistrationId == null
                ? repository.countBySession_CodeAndStatus(workshop.getCode(), WorkshopRegistrationStatus.ACTIVE)
                : repository.countBySession_CodeAndStatusAndCodeNot(
                        workshop.getCode(),
                        WorkshopRegistrationStatus.ACTIVE,
                        excludeRegistrationId
                );
        if (activeRegistrations >= capacity) {
            throw new WorkshopFullException("This workshop has reached its room capacity.");
        }
    }

    private List<Session> listWorkshopSessions() {
        List<Session> workshops = sessionService.getRepository().findByTypeOrderByStartTimeAsc(SessionType.W);
        Optional<LocalDate> startDate = eventService.findCurrentEvent().map(event -> event.getStartDate());
        Optional<LocalDate> endDate = eventService.findCurrentEvent().map(event -> event.getEndDate());
        if (startDate.isEmpty() || endDate.isEmpty()) {
            return workshops;
        }
        return workshops.stream()
                .filter(session -> {
                    LocalDate sessionDate = session.getStartTime().toLocalDate();
                    return !sessionDate.isBefore(startDate.get()) && !sessionDate.isAfter(endDate.get());
                })
                .toList();
    }

    private boolean isWorkshopAvailableForRegistration(Session session) {
        if (session.getRoom() == null) {
            return false;
        }
        int capacity = session.getRoom().getCapacity();
        if (capacity <= 0) {
            return true;
        }
        return repository.countBySession_CodeAndStatus(session.getCode(), WorkshopRegistrationStatus.ACTIVE) < capacity;
    }

    private WorkshopOption toWorkshopOption(Session session) {
        Room room = session.getRoom();
        int capacity = room != null ? room.getCapacity() : 0;
        long registered = repository.countBySession_CodeAndStatus(session.getCode(), WorkshopRegistrationStatus.ACTIVE);
        Integer seatsLeft = capacity > 0 ? Math.max(capacity - (int) registered, 0) : null;
        return new WorkshopOption(
                session.getCode(),
                session.getTitle(),
                room != null ? room.getName() : "Room TBD",
                session.getStartTime(),
                capacity,
                seatsLeft
        );
    }

    private ExistingRegistrationView toExistingRegistration(WorkshopParticipantRegistration registration) {
        WorkshopOption workshop = toWorkshopOption(registration.getSession());
        return new ExistingRegistrationView(
                registration.getCode(),
                registration.getTicketReference(),
                registration.getReservationShortCode(),
                registration.getAttendeeName(),
                registration.getAttendeeEmail(),
                registration.getAlfioReservationStatus(),
                registration.getRegisteredAt(),
                workshop
        );
    }

    public ExistingRegistrationView toExistingRegistrationForUi(WorkshopParticipantRegistration registration) {
        return toExistingRegistration(registration);
    }

    private Specification<WorkshopParticipantRegistration> createSearchSpecification(String searchTerm, Long workshopId) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (workshopId != null) {
                predicate = builder.and(predicate, builder.equal(root.get("session").get("code"), workshopId));
            }
            if (!StringUtils.hasText(searchTerm)) {
                return predicate;
            }
            String like = "%" + searchTerm.trim().toLowerCase() + "%";
            return builder.and(predicate, builder.or(
                    builder.like(builder.lower(root.get("attendeeName")), like),
                    builder.like(builder.lower(root.get("attendeeEmail")), like),
                    builder.like(builder.lower(root.get("ticketReference")), like),
                    builder.like(builder.lower(root.get("reservationShortCode")), like)
            ));
        };
    }

    private String normalizeTicketReference(String rawTicketReference) {
        if (!StringUtils.hasText(rawTicketReference)) {
            throw new WorkshopRegistrationException("The 'Info. del pedido' code is required.");
        }
        return rawTicketReference.trim();
    }

    public record PublicModuleState(boolean enabled, boolean active, String publicMessage, boolean configured) {
        public boolean isAvailable() {
            return enabled && active && configured;
        }
    }

    public record WorkshopOption(
            Long sessionCode,
            String title,
            String roomName,
            LocalDateTime startTime,
            int roomCapacity,
            Integer seatsLeft
    ) {
        public String displayLabel() {
            String seatsLabel = seatsLeft == null ? "Unlimited seats" : seatsLeft + " seats left";
            return "%s · %s · %s".formatted(title, roomName, seatsLabel);
        }
    }

    public record ExistingRegistrationView(
            Long registrationCode,
            String ticketReference,
            String reservationShortCode,
            String attendeeName,
            String attendeeEmail,
            String alfioReservationStatus,
            LocalDateTime registeredAt,
            WorkshopOption workshop
    ) {
    }

    public record ValidatedTicketView(
            String ticketReference,
            String reservationShortCode,
            String attendeeName,
            String attendeeEmail,
            String reservationStatus,
            List<WorkshopOption> availableWorkshops
    ) {
    }

    public sealed interface TicketValidationOutcome permits TicketValidationOutcome.AlreadyRegistered, TicketValidationOutcome.Validated {
        static TicketValidationOutcome alreadyRegistered(ExistingRegistrationView registration) {
            return new AlreadyRegistered(registration);
        }

        static TicketValidationOutcome validated(ValidatedTicketView validatedTicket) {
            return new Validated(validatedTicket);
        }

        record AlreadyRegistered(ExistingRegistrationView registration) implements TicketValidationOutcome {}

        record Validated(ValidatedTicketView ticket) implements TicketValidationOutcome {}
    }

    public static class WorkshopRegistrationException extends RuntimeException {
        public WorkshopRegistrationException(String message) {
            super(message);
        }
    }

    public static class DuplicateWorkshopRegistrationException extends WorkshopRegistrationException {
        private final WorkshopParticipantRegistration existingRegistration;

        public DuplicateWorkshopRegistrationException(String message, WorkshopParticipantRegistration existingRegistration) {
            super(message);
            this.existingRegistration = existingRegistration;
        }

        public WorkshopParticipantRegistration getExistingRegistration() {
            return existingRegistration;
        }
    }

    public static class WorkshopFullException extends WorkshopRegistrationException {
        public WorkshopFullException(String message) {
            super(message);
        }
    }
}
