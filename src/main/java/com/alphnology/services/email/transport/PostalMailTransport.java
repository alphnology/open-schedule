package com.alphnology.services.email.transport;

import com.alphnology.data.enums.MailProviderType;
import com.alphnology.services.email.EmailMessage;
import com.alphnology.services.email.EmailSendException;
import com.alphnology.services.email.MailSettingsSnapshot;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class PostalMailTransport implements MailTransport {

    @Override
    public boolean supports(MailSettingsSnapshot settings) {
        return settings.providerType() == MailProviderType.POSTAL;
    }

    @Override
    public void send(EmailMessage emailMessage, MailSettingsSnapshot settings) throws EmailSendException {
        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl(settings.postalBaseUrl().replaceAll("/+$", ""))
                    .defaultHeader(settings.postalApiKeyHeader(), settings.secret())
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            SendRequest request = new SendRequest(
                    buildFrom(emailMessage.getFrom(), settings),
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
                throw new EmailSendException("Postal API returned non-success status");
            }
        } catch (RestClientException | IOException ex) {
            throw new EmailSendException("Failed to send email via Postal", ex);
        }
    }

    private static String buildFrom(String override, MailSettingsSnapshot settings) {
        String name = StringUtils.hasText(override) ? override : settings.fromName();
        if (StringUtils.hasText(name) && StringUtils.hasText(settings.fromAddress())) {
            return name + " <" + settings.fromAddress() + ">";
        }
        return settings.fromAddress();
    }

    private static List<Attachment> buildAttachments(EmailMessage emailMessage) throws IOException {
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

    record SendResponse(String status) {}
}
