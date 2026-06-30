package com.alphnology.services;

import com.alphnology.data.Room;
import com.alphnology.data.Session;
import com.alphnology.data.WorkshopParticipantRegistration;
import com.alphnology.data.enums.SessionType;
import com.alphnology.data.enums.WorkshopRegistrationStatus;
import com.alphnology.data.repository.WorkshopParticipantRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopRegistrationServiceTest {

    @Mock
    private WorkshopParticipantRegistrationRepository repository;
    @Mock
    private SessionService sessionService;
    @Mock
    private EventService eventService;
    @Mock
    private WorkshopRegistrationSettingsService settingsService;
    @Mock
    private AlfioTicketValidationService alfioTicketValidationService;

    private WorkshopRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new WorkshopRegistrationService(
                repository,
                sessionService,
                eventService,
                settingsService,
                alfioTicketValidationService
        );
    }

    @Test
    void validateTicketReturnsExistingRegistrationWithoutCallingAlfio() {
        var settings = configuredSettings();
        var registration = existingRegistration();

        when(settingsService.getEffectiveSettings()).thenReturn(settings);
        when(repository.findByEventSlugAndTicketReference("jd2026", "b2b2")).thenReturn(Optional.of(registration));
        when(repository.countBySession_CodeAndStatus(77L, WorkshopRegistrationStatus.ACTIVE)).thenReturn(1L);

        var outcome = service.validateTicket("b2b2");

        assertThat(outcome).isInstanceOf(WorkshopRegistrationService.TicketValidationOutcome.AlreadyRegistered.class);
        verify(alfioTicketValidationService, never()).validate(any(), any());
    }

    @Test
    void registerWorkshopRejectsWhenRoomCapacityIsReached() {
        var settings = configuredSettings();
        var workshop = workshopSession(12L, 1);

        when(settingsService.getEffectiveSettings()).thenReturn(settings);
        when(repository.findByEventSlugAndTicketReference("jd2026", "b2b2")).thenReturn(Optional.empty());
        when(sessionService.get(12L)).thenReturn(Optional.of(workshop));
        when(repository.countBySession_CodeAndStatus(12L, WorkshopRegistrationStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> service.registerWorkshop("b2b2", 12L))
                .isInstanceOf(WorkshopRegistrationService.WorkshopFullException.class)
                .hasMessageContaining("capacity");

        verify(repository, never()).save(any());
    }

    @Test
    void registerWorkshopPersistsValidatedTicketAgainstSelectedWorkshop() {
        var settings = configuredSettings();
        var workshop = workshopSession(12L, 0);
        var validatedTicket = new AlfioTicketValidationService.AlfioValidatedTicket(
                "b2b2",
                "BDB04C39",
                "Fred Pena",
                "fred@example.org",
                "COMPLETE",
                "{\"status\":\"COMPLETE\"}"
        );

        when(settingsService.getEffectiveSettings()).thenReturn(settings);
        when(repository.findByEventSlugAndTicketReference("jd2026", "b2b2")).thenReturn(Optional.empty());
        when(sessionService.get(12L)).thenReturn(Optional.of(workshop));
        when(alfioTicketValidationService.validate("b2b2", settings)).thenReturn(validatedTicket);
        when(repository.save(any(WorkshopParticipantRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.registerWorkshop("b2b2", 12L);

        ArgumentCaptor<WorkshopParticipantRegistration> captor = ArgumentCaptor.forClass(WorkshopParticipantRegistration.class);
        verify(repository).save(captor.capture());

        assertThat(saved.getTicketReference()).isEqualTo("b2b2");
        assertThat(saved.getReservationShortCode()).isEqualTo("BDB04C39");
        assertThat(saved.getAttendeeName()).isEqualTo("Fred Pena");
        assertThat(saved.getAttendeeEmail()).isEqualTo("fred@example.org");
        assertThat(saved.getSession().getCode()).isEqualTo(12L);
        assertThat(saved.getStatus()).isEqualTo(WorkshopRegistrationStatus.ACTIVE);
        assertThat(captor.getValue().getValidatedAt()).isNotNull();
        assertThat(captor.getValue().getRegisteredAt()).isNotNull();
    }

    @Test
    void changeWorkshopUpdatesExistingRegistrationWhenSelfServiceIsEnabled() {
        var settings = new WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot(
                true,
                true,
                "https://tickets.example.org",
                "jd2026",
                "token",
                null,
                true,
                false,
                1,
                true,
                true
        );
        var existing = existingRegistration();
        var newWorkshop = workshopSession(12L, 5);

        when(settingsService.getEffectiveSettings()).thenReturn(settings);
        when(repository.findByEventSlugAndTicketReference("jd2026", "b2b2")).thenReturn(Optional.of(existing));
        when(sessionService.get(12L)).thenReturn(Optional.of(newWorkshop));
        when(repository.countBySession_CodeAndStatusAndCodeNot(12L, WorkshopRegistrationStatus.ACTIVE, 100L)).thenReturn(0L);
        when(repository.save(any(WorkshopParticipantRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.changeWorkshop("b2b2", 12L);

        assertThat(saved.getSession().getCode()).isEqualTo(12L);
        verify(repository).save(existing);
    }

    private WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot configuredSettings() {
        return new WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot(
                true,
                true,
                "https://tickets.example.org",
                "jd2026",
                "token",
                null,
                false,
                false,
                1,
                true,
                true
        );
    }

    private WorkshopParticipantRegistration existingRegistration() {
        WorkshopParticipantRegistration registration = new WorkshopParticipantRegistration();
        registration.setCode(100L);
        registration.setEventSlug("jd2026");
        registration.setTicketReference("b2b2");
        registration.setReservationShortCode("BDB04C39");
        registration.setAttendeeName("Fred Pena");
        registration.setAttendeeEmail("fred@example.org");
        registration.setStatus(WorkshopRegistrationStatus.ACTIVE);
        registration.setRegisteredAt(LocalDateTime.now());
        registration.setSession(workshopSession(77L, 10));
        return registration;
    }

    private Session workshopSession(Long code, int capacity) {
        Room room = new Room();
        room.setCode(code + 100);
        room.setName("Workshop Room");
        room.setCapacity(capacity);

        Session session = new Session();
        session.setCode(code);
        session.setTitle("Deep Dive Workshop");
        session.setType(SessionType.W);
        session.setRoom(room);
        session.setStartTime(LocalDateTime.of(2026, 7, 17, 10, 0));
        session.setEndTime(LocalDateTime.of(2026, 7, 17, 12, 0));
        return session;
    }
}
