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
import java.util.Locale;
import java.util.Objects;
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
        return new PublicModuleState(
                settings.enabled(),
                settings.active(),
                settings.publicMessage(),
                settings.isConfigured(),
                settings.allowAttendeeWorkshopChange(),
                settings.showPublicMenuEntry()
        );
    }

    @Transactional
    public ValidatedOrderView validateOrder(String rawOrderReference) {
        String orderReference = normalizeOrderReference(rawOrderReference);
        var settings = getActiveSettings();
        var validatedOrder = alfioTicketValidationService.validateOrder(orderReference, settings);

        List<ParticipantRegistrationView> participants = validatedOrder.participants().stream()
                .sorted(Comparator.comparing(AlfioTicketValidationService.ValidatedParticipant::attendeeName, String.CASE_INSENSITIVE_ORDER))
                .map(participant -> toParticipantRegistrationView(settings.eventSlug(), validatedOrder, participant))
                .toList();

        return new ValidatedOrderView(
                validatedOrder.orderReference(),
                validatedOrder.reservationId(),
                validatedOrder.reservationShortCode(),
                validatedOrder.reservationStatus(),
                participants
        );
    }

    @Transactional
    public WorkshopParticipantRegistration registerWorkshop(String rawOrderReference, String ticketPublicId, Long workshopId) {
        String orderReference = normalizeOrderReference(rawOrderReference);
        var settings = getActiveSettings();

        var validatedOrder = alfioTicketValidationService.validateOrder(orderReference, settings);
        var participant = findParticipant(validatedOrder, ticketPublicId);

        repository.findByEventSlugAndTicketPublicId(settings.eventSlug(), participant.ticketPublicId())
                .ifPresent(existing -> {
                    throw new DuplicateWorkshopRegistrationException(
                            "This attendee is already registered in workshop " + existing.getSession().getTitle(), existing);
                });

        Session workshop = getWorkshopSession(workshopId);
        ensureCapacity(workshop, null);

        WorkshopParticipantRegistration registration = new WorkshopParticipantRegistration();
        registration.setEventSlug(settings.eventSlug());
        registration.setOrderReference(validatedOrder.orderReference());
        registration.setReservationId(validatedOrder.reservationId());
        registration.setTicketId(participant.ticketId());
        registration.setTicketPublicId(participant.ticketPublicId());
        registration.setReservationShortCode(validatedOrder.reservationShortCode());
        registration.setAttendeeName(participant.attendeeName());
        registration.setAttendeeEmail(participant.attendeeEmail());
        registration.setSession(workshop);
        registration.setStatus(WorkshopRegistrationStatus.ACTIVE);
        registration.setAlfioReservationStatus(validatedOrder.reservationStatus());
        registration.setAlfioPayloadJson(validatedOrder.payloadJson());
        registration.setValidatedAt(LocalDateTime.now());
        registration.setRegisteredAt(LocalDateTime.now());
        return repository.save(registration);
    }

    @Transactional
    public WorkshopParticipantRegistration changeWorkshop(String rawOrderReference, String ticketPublicId, Long workshopId) {
        String orderReference = normalizeOrderReference(rawOrderReference);
        var settings = getActiveSettings();
        if (!settings.allowAttendeeWorkshopChange()) {
            throw new WorkshopRegistrationException("To change workshops, please contact the event organization.");
        }

        var validatedOrder = alfioTicketValidationService.validateOrder(orderReference, settings);
        var participant = findParticipant(validatedOrder, ticketPublicId);

        WorkshopParticipantRegistration existingRegistration = repository
                .findByEventSlugAndTicketPublicId(settings.eventSlug(), participant.ticketPublicId())
                .orElseThrow(() -> new WorkshopRegistrationException("No workshop registration was found for this attendee."));

        Session workshop = getWorkshopSession(workshopId);
        ensureCapacity(workshop, existingRegistration.getCode());
        existingRegistration.setSession(workshop);
        existingRegistration.setStatus(WorkshopRegistrationStatus.ACTIVE);
        existingRegistration.setOrderReference(validatedOrder.orderReference());
        existingRegistration.setReservationId(validatedOrder.reservationId());
        existingRegistration.setTicketId(participant.ticketId());
        existingRegistration.setTicketPublicId(participant.ticketPublicId());
        existingRegistration.setReservationShortCode(validatedOrder.reservationShortCode());
        existingRegistration.setAttendeeName(participant.attendeeName());
        existingRegistration.setAttendeeEmail(participant.attendeeEmail());
        existingRegistration.setAlfioReservationStatus(validatedOrder.reservationStatus());
        existingRegistration.setAlfioPayloadJson(validatedOrder.payloadJson());
        existingRegistration.setValidatedAt(LocalDateTime.now());
        return repository.save(existingRegistration);
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
    public List<WorkshopOption> listWorkshopOptionsForExistingRegistration(Long registrationId) {
        WorkshopParticipantRegistration registration = repository.findById(registrationId)
                .orElseThrow(() -> new EntityNotFoundException("Workshop registration not found."));
        return listWorkshopOptionsForExistingRegistration(registration);
    }

    @Transactional
    public List<WorkshopOption> listAllWorkshopOptions() {
        return listWorkshopSessions().stream().map(this::toWorkshopOption).toList();
    }

    private List<WorkshopOption> listWorkshopOptionsForExistingRegistration(WorkshopParticipantRegistration registration) {
        return listWorkshopSessions().stream()
                .filter(session -> isWorkshopAvailableForRegistration(session)
                        || registration.getSession().getCode().equals(session.getCode()))
                .map(this::toWorkshopOption)
                .toList();
    }

    private ParticipantRegistrationView toParticipantRegistrationView(
            String eventSlug,
            AlfioTicketValidationService.AlfioValidatedOrder validatedOrder,
            AlfioTicketValidationService.ValidatedParticipant participant
    ) {
        WorkshopParticipantRegistration existingRegistration = repository
                .findByEventSlugAndTicketPublicId(eventSlug, participant.ticketPublicId())
                .orElse(null);

        List<WorkshopOption> options = existingRegistration != null
                ? listWorkshopOptionsForExistingRegistration(existingRegistration)
                : listAvailableWorkshopOptions();

        return new ParticipantRegistrationView(
                participant.ticketId(),
                participant.ticketPublicId(),
                participant.ticketStatus(),
                participant.attendeeName(),
                participant.attendeeEmail(),
                validatedOrder.reservationShortCode(),
                existingRegistration != null ? toExistingRegistration(existingRegistration) : null,
                options
        );
    }

    private AlfioTicketValidationService.ValidatedParticipant findParticipant(
            AlfioTicketValidationService.AlfioValidatedOrder validatedOrder,
            String rawTicketPublicId
    ) {
        String ticketPublicId = normalizeTicketPublicId(rawTicketPublicId);
        return validatedOrder.participants().stream()
                .filter(participant -> ticketPublicId.equalsIgnoreCase(participant.ticketPublicId()))
                .findFirst()
                .orElseThrow(() -> new WorkshopRegistrationException("The selected attendee does not belong to this order."));
    }

    private WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot getActiveSettings() {
        var settings = settingsService.getEffectiveSettings();
        if (!settings.enabled() || !settings.active()) {
            throw new WorkshopRegistrationException("Workshop registration is currently unavailable.");
        }
        return settings;
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
                registration.getOrderReference(),
                registration.getTicketPublicId(),
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
            String like = "%" + searchTerm.trim().toLowerCase(Locale.ROOT) + "%";
            return builder.and(predicate, builder.or(
                    builder.like(builder.lower(root.get("attendeeName")), like),
                    builder.like(builder.lower(root.get("attendeeEmail")), like),
                    builder.like(builder.lower(root.get("orderReference")), like),
                    builder.like(builder.lower(root.get("ticketPublicId")), like),
                    builder.like(builder.lower(root.get("reservationShortCode")), like)
            ));
        };
    }

    private String normalizeOrderReference(String rawOrderReference) {
        if (!StringUtils.hasText(rawOrderReference)) {
            throw new WorkshopRegistrationException("The 'Info. del pedido' code is required.");
        }
        return rawOrderReference.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTicketPublicId(String rawTicketPublicId) {
        if (!StringUtils.hasText(rawTicketPublicId)) {
            throw new WorkshopRegistrationException("A participant ticket is required.");
        }
        return rawTicketPublicId.trim();
    }

    public record PublicModuleState(
            boolean enabled,
            boolean active,
            String publicMessage,
            boolean configured,
            boolean allowAttendeeWorkshopChange,
            boolean showPublicMenuEntry
    ) {
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
            String orderReference,
            String ticketPublicId,
            String reservationShortCode,
            String attendeeName,
            String attendeeEmail,
            String alfioReservationStatus,
            LocalDateTime registeredAt,
            WorkshopOption workshop
    ) {
    }

    public record ParticipantRegistrationView(
            String ticketId,
            String ticketPublicId,
            String ticketStatus,
            String attendeeName,
            String attendeeEmail,
            String reservationShortCode,
            ExistingRegistrationView existingRegistration,
            List<WorkshopOption> availableWorkshops
    ) {
        public boolean isRegistered() {
            return existingRegistration != null;
        }
    }

    public record ValidatedOrderView(
            String orderReference,
            String reservationId,
            String reservationShortCode,
            String reservationStatus,
            List<ParticipantRegistrationView> participants
    ) {
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
