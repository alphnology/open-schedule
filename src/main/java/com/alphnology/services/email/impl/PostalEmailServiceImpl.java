package com.alphnology.services.email.impl;

import com.alphnology.services.email.EmailMessage;
import com.alphnology.services.email.EmailSendException;
import com.alphnology.services.email.EmailService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * EmailService implementation that uses the Postal self-hosted mail server HTTP API.
 * <p>
 * Activated when {@code application.email.postal.enabled=true}.
 * When active, this implementation takes precedence over the SMTP provider ({@code @Primary}).
 * <p>
 * Required configuration:
 * <pre>
 *   application.email.postal.base-url=https://postal.yourdomain.com
 *   application.email.postal.api-key=your-server-api-key
 * </pre>
 *
 * @see <a href="https://docs.postalserver.io/developer/api">Postal API docs</a>
 */
@Slf4j
@Primary
@Service
@ConditionalOnProperty(prefix = "application.email.postal", value = "enabled", havingValue = "true")
public class PostalEmailServiceImpl implements EmailService {

    private final RestClient restClient;

    @Value("${application.email.from.address:}")
    private String fromAddress;

    @Value("${application.email.from.name:Open Schedule}")
    private String fromName;

    public PostalEmailServiceImpl(
            @Value("${application.email.postal.base-url}") String baseUrl,
            @Value("${application.email.postal.api-key}") String apiKey,
            RestClient.Builder restClientBuilder) {

        this.restClient = restClientBuilder
                .baseUrl(baseUrl.stripTrailing("/"))
                .defaultHeader("X-Server-API-Key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        log.info("Postal Email Service initialized with base-url: {}", baseUrl);
    }

    @Override
    public void sendEmail(EmailMessage emailMessage) throws EmailSendException {
        log.info("Sending email via Postal to {} recipient(s), subject: '{}'",
                emailMessage.getTo().size(), emailMessage.getSubject());
        try {
            SendRequest request = new SendRequest(
                    buildFrom(emailMessage.getFrom()),
                    new ArrayList<>(emailMessage.getTo()),
                    emailMessage.getSubject(),
                    emailMessage.getBody(),
                    buildAttachments(emailMessage)
            );

            SendResponse response = restClient.post()
                    .uri("/api/v1/send/message")
                    .body(request)
                    .retrieve()
                    .body(SendResponse.class);

            if (response == null || !"success".equals(response.status())) {
                String status = response != null ? response.status() : "null";
                throw new EmailSendException("Postal API returned non-success status: " + status);
            }

            log.debug("Email sent via Postal, message_id: {}",
                    response.data() != null ? response.data().messageId() : "unknown");

        } catch (EmailSendException e) {
            throw e;
        } catch (RestClientException | IOException e) {
            throw new EmailSendException("Failed to send email via Postal", e);
        }
    }

    private String buildFrom(String displayNameOverride) {
        String name = (displayNameOverride != null && !displayNameOverride.isBlank())
                ? displayNameOverride
                : fromName;
        String address = (fromAddress != null && !fromAddress.isBlank())
                ? fromAddress
                : "";
        return (name != null && !name.isBlank() && !address.isBlank())
                ? name + " <" + address + ">"
                : address;
    }

    private List<Attachment> buildAttachments(EmailMessage emailMessage) throws IOException {
        List<Attachment> result = new ArrayList<>();
        if (emailMessage.getAttachments() == null || emailMessage.getAttachments().isEmpty()) {
            return result;
        }
        for (Path file : emailMessage.getAttachments()) {
            byte[] bytes = Files.readAllBytes(file);
            String contentType = Files.probeContentType(file);
            result.add(new Attachment(
                    file.getFileName().toString(),
                    contentType != null ? contentType : "application/octet-stream",
                    Base64.getEncoder().encodeToString(bytes)
            ));
        }
        return result;
    }

    // ── Postal API payload models ─────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record SendRequest(
            String from,
            List<String> to,
            String subject,
            @JsonProperty("html_body") String htmlBody,
            List<Attachment> attachments
    ) {}

    record Attachment(
            String name,
            @JsonProperty("content_type") String contentType,
            String data
    ) {}

    record SendResponse(
            String status,
            SendData data
    ) {}

    record SendData(
            @JsonProperty("message_id") String messageId
    ) {}
}
