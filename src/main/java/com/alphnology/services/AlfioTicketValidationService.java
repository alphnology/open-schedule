package com.alphnology.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class AlfioTicketValidationService {

    private static final Set<String> VALID_STATUSES = Set.of("COMPLETE", "CONFIRMED");

    private final AlfioTicketValidationClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AlfioTicketValidationService(AlfioTicketValidationClient client) {
        this.client = client;
    }

    public AlfioValidatedTicket validate(String ticketReference,
                                         WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot settings) {
        if (!settings.isConfigured()) {
            throw new InvalidTicketException("Workshop registration is not configured.");
        }

        JsonNode payload = client.fetchReservation(
                settings.alfioBaseUrl(),
                settings.eventSlug(),
                ticketReference,
                settings.token()
        );

        String reservationStatus = firstText(payload, "reservationStatus", "status", "ticketStatus");
        boolean cancelled = firstBoolean(payload, "cancelled", "canceled");
        boolean refunded = firstBoolean(payload, "refunded");
        String email = firstText(payload, "email", "emailAddress");
        String fullName = firstText(payload, "fullName", "name");
        if (!StringUtils.hasText(fullName)) {
            String firstName = firstText(payload, "firstName", "givenName");
            String lastName = firstText(payload, "lastName", "familyName", "surname");
            String combined = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            fullName = StringUtils.hasText(combined) ? combined : null;
        }
        String reservationShortCode = firstText(payload,
                "reservationShortID", "reservationShortId", "reservationShortCode", "shortCode");

        if (!StringUtils.hasText(reservationStatus) || !VALID_STATUSES.contains(reservationStatus.toUpperCase(Locale.ROOT))) {
            throw new InvalidTicketException("The ticket is not in a valid alf.io status.");
        }
        if (cancelled || refunded) {
            throw new InvalidTicketException("The ticket is cancelled or refunded.");
        }
        if (!StringUtils.hasText(email) || !StringUtils.hasText(fullName)) {
            throw new InvalidTicketException("The ticket does not expose the required attendee details.");
        }

        return new AlfioValidatedTicket(
                ticketReference,
                reservationShortCode,
                fullName,
                email,
                reservationStatus,
                toJson(payload)
        );
    }

    private String toJson(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize alf.io payload", ex);
        }
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String found = findFirstText(node, fieldName);
            if (StringUtils.hasText(found)) {
                return found.trim();
            }
        }
        return null;
    }

    private boolean firstBoolean(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Boolean found = findFirstBoolean(node, fieldName);
            if (found != null) {
                return found;
            }
        }
        return false;
    }

    private String findFirstText(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            JsonNode direct = node.get(fieldName);
            if (direct != null && direct.isValueNode()) {
                String value = direct.asText();
                return StringUtils.hasText(value) ? value : null;
            }
            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                String nested = findFirstText(children.next(), fieldName);
                if (StringUtils.hasText(nested)) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                String nested = findFirstText(child, fieldName);
                if (StringUtils.hasText(nested)) {
                    return nested;
                }
            }
        }
        return null;
    }

    private Boolean findFirstBoolean(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            JsonNode direct = node.get(fieldName);
            if (direct != null && direct.isValueNode()) {
                return direct.asBoolean();
            }
            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                Boolean nested = findFirstBoolean(children.next(), fieldName);
                if (nested != null) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                Boolean nested = findFirstBoolean(child, fieldName);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    public record AlfioValidatedTicket(
            String ticketReference,
            String reservationShortCode,
            String attendeeName,
            String attendeeEmail,
            String reservationStatus,
            String payloadJson
    ) {
    }

    public static class InvalidTicketException extends RuntimeException {
        public InvalidTicketException(String message) {
            super(message);
        }
    }
}
