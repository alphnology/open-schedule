package com.alphnology.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

    public AlfioValidatedOrder validateOrder(String orderReference,
                                             WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot settings) {
        if (!settings.isConfigured()) {
            throw new InvalidTicketException("Workshop registration is not configured.");
        }

        JsonNode payload = client.fetchOrder(
                settings.alfioBaseUrl(),
                settings.eventSlug(),
                orderReference,
                settings.token()
        );

        String reservationStatus = firstText(payload, "reservationStatus", "status");
        boolean cancelled = firstBoolean(payload, "cancelled", "canceled");
        boolean refunded = firstBoolean(payload, "refunded");
        String reservationId = firstText(payload, "reservationId");
        String reservationShortCode = firstText(payload,
                "reservationCode", "reservationShortID", "reservationShortId", "reservationShortCode", "shortCode");

        if (!StringUtils.hasText(reservationStatus) || !VALID_STATUSES.contains(reservationStatus.toUpperCase(Locale.ROOT))) {
            throw new InvalidTicketException("The reservation is not in a valid alf.io status.");
        }
        if (cancelled || refunded) {
            throw new InvalidTicketException("The reservation is cancelled or refunded.");
        }

        List<ValidatedParticipant> participants = extractParticipants(payload);
        if (participants.isEmpty()) {
            throw new InvalidTicketException("The order does not expose any attendee tickets.");
        }

        return new AlfioValidatedOrder(
                normalizeOrderReference(orderReference),
                reservationId,
                reservationShortCode,
                reservationStatus,
                participants,
                toJson(payload)
        );
    }

    private List<ValidatedParticipant> extractParticipants(JsonNode payload) {
        JsonNode participantsNode = payload.path("participants");
        if (!participantsNode.isArray()) {
            throw new InvalidTicketException("The workshop bridge did not return participant data.");
        }

        List<ValidatedParticipant> participants = new ArrayList<>();
        for (JsonNode participantNode : participantsNode) {
            String ticketId = firstText(participantNode, "ticketId");
            String ticketPublicId = firstText(participantNode, "ticketPublicId");
            String ticketStatus = firstText(participantNode, "ticketStatus", "status");
            String email = firstText(participantNode, "attendeeEmail", "email", "emailAddress");
            String fullName = firstText(participantNode, "attendeeName", "fullName", "name");
            if (!StringUtils.hasText(fullName)) {
                String firstName = firstText(participantNode, "firstName", "givenName");
                String lastName = firstText(participantNode, "lastName", "familyName", "surname");
                String combined = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
                fullName = StringUtils.hasText(combined) ? combined : null;
            }

            if (!StringUtils.hasText(ticketPublicId) || !StringUtils.hasText(email) || !StringUtils.hasText(fullName)) {
                throw new InvalidTicketException("One or more attendee tickets are missing required data.");
            }

            participants.add(new ValidatedParticipant(
                    ticketId,
                    ticketPublicId,
                    ticketStatus,
                    fullName,
                    email
            ));
        }
        return participants;
    }

    private String toJson(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize alf.io payload", ex);
        }
    }

    private static String normalizeOrderReference(String orderReference) {
        return orderReference != null ? orderReference.trim().toUpperCase(Locale.ROOT) : null;
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

    public record AlfioValidatedOrder(
            String orderReference,
            String reservationId,
            String reservationShortCode,
            String reservationStatus,
            List<ValidatedParticipant> participants,
            String payloadJson
    ) {
    }

    public record ValidatedParticipant(
            String ticketId,
            String ticketPublicId,
            String ticketStatus,
            String attendeeName,
            String attendeeEmail
    ) {
    }

    public static class InvalidTicketException extends RuntimeException {
        public InvalidTicketException(String message) {
            super(message);
        }
    }
}
