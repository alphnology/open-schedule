package com.alphnology.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AlfioTicketValidationClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode fetchReservation(String baseUrl, String eventSlug, String reservationId, String bearerToken) {
        try {
            String body = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
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
            throw new AlfioClientException("Alf.io validation failed with HTTP " + ex.getStatusCode().value(), ex);
        } catch (AlfioClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AlfioClientException("Unable to validate ticket against alf.io", ex);
        }
    }

    public static class AlfioClientException extends RuntimeException {
        public AlfioClientException(String message) {
            super(message);
        }

        public AlfioClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
