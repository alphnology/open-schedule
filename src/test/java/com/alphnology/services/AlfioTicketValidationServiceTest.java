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
    void validateOrderExtractsParticipantsFromBridgePayload() throws Exception {
        var settings = new WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot(
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
        var payload = objectMapper.readTree("""
                {
                  "resolved": true,
                  "eventSlug": "jd2026",
                  "reservationId": "bdb04c39-a8fd-4991-b721-75e099de1544",
                  "reservationCode": "BDB04C39",
                  "reservationStatus": "COMPLETE",
                  "source": "database",
                  "participants": [
                    {
                      "ticketId": "a494e570-8c78-4f8e-b03f-4a286921e341",
                      "ticketPublicId": "b2b2b49e-881e-40f9-895f-52a995e84bfd",
                      "ticketStatus": "ACQUIRED",
                      "attendeeName": "Fred Peña",
                      "attendeeEmail": "freddy.pena@alphnology.com"
                    },
                    {
                      "ticketId": "b494e570-8c78-4f8e-b03f-4a286921e342",
                      "ticketPublicId": "c2b2b49e-881e-40f9-895f-52a995e84bfd",
                      "ticketStatus": "ACQUIRED",
                      "attendeeName": "Jane Roe",
                      "attendeeEmail": "jane@example.org"
                    }
                  ]
                }
                """);
        when(client.fetchOrder("https://tickets.example.org", "jd2026", "BDB04C39", "token"))
                .thenReturn(payload);

        var result = service.validateOrder("BDB04C39", settings);

        assertThat(result.orderReference()).isEqualTo("BDB04C39");
        assertThat(result.reservationShortCode()).isEqualTo("BDB04C39");
        assertThat(result.reservationStatus()).isEqualTo("COMPLETE");
        assertThat(result.participants()).hasSize(2);
        assertThat(result.participants().getFirst().attendeeName()).isEqualTo("Fred Peña");
        assertThat(result.participants().get(1).ticketPublicId()).isEqualTo("c2b2b49e-881e-40f9-895f-52a995e84bfd");
    }

    @Test
    void validateOrderRejectsCancelledOrInvalidReservations() throws Exception {
        var settings = new WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot(
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
        var payload = objectMapper.readTree("""
                {
                  "reservationStatus": "PENDING",
                  "cancelled": true,
                  "participants": [
                    {
                      "ticketPublicId": "b2b2b49e-881e-40f9-895f-52a995e84bfd",
                      "attendeeName": "Fred Peña",
                      "attendeeEmail": "fred@example.org"
                    }
                  ]
                }
                """);
        when(client.fetchOrder("https://tickets.example.org", "jd2026", "BADREF01", "token"))
                .thenReturn(payload);

        assertThatThrownBy(() -> service.validateOrder("BADREF01", settings))
                .isInstanceOf(AlfioTicketValidationService.InvalidTicketException.class);
    }
}
