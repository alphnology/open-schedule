package com.alphnology.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlfioTicketValidationServiceTest {

    @Mock
    private AlfioTicketValidationClient client;

    private AlfioTicketValidationService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new AlfioTicketValidationService(client);
    }

    @Test
    void validateExtractsNestedAttendeeDataFromAlfioPayload() throws Exception {
        var settings = new WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot(
                true,
                true,
                "https://tickets.example.org",
                "jd2026",
                "token",
                null,
                1,
                true,
                true
        );
        var payload = objectMapper.readTree("""
                {
                  "status": "COMPLETE",
                  "reservationShortID": "BDB04C39",
                  "tickets": [
                    {
                      "owner": {
                        "firstName": "Fred",
                        "lastName": "Peña",
                        "emailAddress": "fred@example.org"
                      }
                    }
                  ]
                }
                """);
        when(client.fetchReservation("https://tickets.example.org", "jd2026", "b2b2", "token"))
                .thenReturn(payload);

        var result = service.validate("b2b2", settings);

        assertThat(result.ticketReference()).isEqualTo("b2b2");
        assertThat(result.reservationShortCode()).isEqualTo("BDB04C39");
        assertThat(result.attendeeName()).isEqualTo("Fred Peña");
        assertThat(result.attendeeEmail()).isEqualTo("fred@example.org");
        assertThat(result.reservationStatus()).isEqualTo("COMPLETE");
        assertThat(result.payloadJson()).contains("fred@example.org");
    }

    @Test
    void validateRejectsCancelledOrInvalidReservations() throws Exception {
        var settings = new WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot(
                true,
                true,
                "https://tickets.example.org",
                "jd2026",
                "token",
                null,
                1,
                true,
                true
        );
        var payload = objectMapper.readTree("""
                {
                  "status": "PENDING",
                  "cancelled": true,
                  "email": "fred@example.org",
                  "fullName": "Fred Peña"
                }
                """);
        when(client.fetchReservation("https://tickets.example.org", "jd2026", "bad-ref", "token"))
                .thenReturn(payload);

        assertThatThrownBy(() -> service.validate("bad-ref", settings))
                .isInstanceOf(AlfioTicketValidationService.InvalidTicketException.class);
    }
}
