package com.alphnology.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Component
public class AlfioTicketValidationClient {

    private static final String BRIDGE_API_KEY_HEADER = "X-API-Key";

    private final WorkshopRegistrationBridgeProperties bridgeProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AlfioTicketValidationClient(WorkshopRegistrationBridgeProperties bridgeProperties) {
        this.bridgeProperties = bridgeProperties;
    }

    public JsonNode fetchReservation(String baseUrl, String eventSlug, String referenceValue, String bearerToken) {
        if (bridgeProperties.isConfigured()) {
            return fetchReservationViaBridge(eventSlug, referenceValue);
        }
        return fetchReservationDirectly(baseUrl, eventSlug, referenceValue, bearerToken);
    }

    private JsonNode fetchReservationDirectly(String baseUrl, String eventSlug, String reservationId, String bearerToken) {
        try {
            String normalizedToken = StringUtils.hasText(bearerToken) ? bearerToken.trim() : null;
            log.info(
                    "Validating alf.io reservation. baseUrl='{}', eventSlug='{}', reservationId='{}', tokenPresent={}, tokenLength={}, tokenFingerprint={}",
                    baseUrl,
                    eventSlug,
                    reservationId,
                    StringUtils.hasText(normalizedToken),
                    normalizedToken != null ? normalizedToken.length() : 0,
                    fingerprint(normalizedToken)
            );
            String body = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + normalizedToken)
                    .build()
                    .get()
                    .uri("/api/v1/admin/event/{eventSlug}/reservation/{reservationId}", eventSlug, reservationId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new AlfioClientException("Alf.io returned HTTP " + response.getStatusCode().value());
                    })
                    .body(String.class);
            return objectMapper.readTree(body);
        } catch (RestClientResponseException ex) {
            log.warn("Alf.io validation returned HTTP {} for eventSlug='{}', reservationId='{}'",
                    ex.getStatusCode().value(), eventSlug, reservationId, ex);
            throw new AlfioClientException("Alf.io validation failed with HTTP " + ex.getStatusCode().value(), ex);
        } catch (AlfioClientException ex) {
            log.warn("Alf.io validation failed for eventSlug='{}', reservationId='{}': {}",
                    eventSlug, reservationId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error validating alf.io reservation for eventSlug='{}', reservationId='{}'",
                    eventSlug, reservationId, ex);
            throw new AlfioClientException("Unable to validate ticket against alf.io", ex);
        }
    }

    private JsonNode fetchReservationViaBridge(String eventSlug, String referenceValue) {
        try {
            String baseUrl = bridgeProperties.getBaseUrl().trim();
            String apiKey = bridgeProperties.getApiKey().trim();
            String referenceType = resolveReferenceType(referenceValue);

            log.info(
                    "Resolving workshop registration via bridge. bridgeBaseUrl='{}', eventSlug='{}', referenceType='{}', referenceValue='{}', apiKeyPresent={}, apiKeyLength={}, apiKeyFingerprint={}",
                    baseUrl,
                    eventSlug,
                    referenceType,
                    referenceValue,
                    StringUtils.hasText(apiKey),
                    apiKey.length(),
                    fingerprint(apiKey)
            );

            String body = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(BRIDGE_API_KEY_HEADER, apiKey)
                    .build()
                    .post()
                    .uri("/api/v1/reservations/resolve")
                    .body(new BridgeResolveRequest(eventSlug, referenceType, referenceValue))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new AlfioClientException("Workshop bridge returned HTTP " + response.getStatusCode().value());
                    })
                    .body(String.class);
            return objectMapper.readTree(body);
        } catch (RestClientResponseException ex) {
            log.warn("Workshop bridge returned HTTP {} for eventSlug='{}', referenceValue='{}'",
                    ex.getStatusCode().value(), eventSlug, referenceValue, ex);
            throw new AlfioClientException("Workshop bridge failed with HTTP " + ex.getStatusCode().value(), ex);
        } catch (AlfioClientException ex) {
            log.warn("Workshop bridge resolution failed for eventSlug='{}', referenceValue='{}': {}",
                    eventSlug, referenceValue, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error resolving workshop registration via bridge for eventSlug='{}', referenceValue='{}'",
                    eventSlug, referenceValue, ex);
            throw new AlfioClientException("Unable to validate workshop reference via bridge", ex);
        }
    }

    private static String resolveReferenceType(String referenceValue) {
        String normalizedValue = StringUtils.hasText(referenceValue) ? referenceValue.trim() : "";
        if (normalizedValue.matches("^[A-Za-z0-9]{8}$")) {
            return "order_code";
        }
        if (normalizedValue.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return "reservation_id";
        }
        throw new AlfioClientException("The supplied workshop reference is not valid.");
    }

    public static class AlfioClientException extends RuntimeException {
        public AlfioClientException(String message) {
            super(message);
        }

        public AlfioClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static String fingerprint(String value) {
        if (!StringUtils.hasText(value)) {
            return "absent";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                builder.append(String.format("%02x", digest[i]));
            }
            return builder.toString();
        } catch (Exception ex) {
            return "unavailable";
        }
    }

    private record BridgeResolveRequest(
            String eventSlug,
            String referenceType,
            String referenceValue
    ) {
    }
}
