package com.alphnology.services.email;

import com.alphnology.services.template.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOpenScheduleService {

    private final EmailService emailService;
    private final TemplateService templateService;

    @Value("${application.name}")
    private String appName;
    @Value("${application.url}")
    private String appUrl;


    private String renderTemplate(EmailMessageOpenSchedule message) {

        Map<String, Object> model = new HashMap<>(message.getModel());

        model.put("environment", "production");
        model.put("currentDate", Instant.now());
        model.put("appName", appName);
        model.put("appUrl", appUrl);

        return templateService.render(message.getTemplateId(), model);
    }

    public void sendEmail(EmailMessageOpenSchedule message) throws EmailSendException {

        EmailMessage emailMessage = new EmailMessage();
        emailMessage.setTo(message.getTo());
        emailMessage.setFrom(message.getFrom());
        emailMessage.setSubject(message.getSubject());

        String body = renderTemplate(message);
        emailMessage.setBody(body);


        // Send the email using the email service
        emailService.sendEmail(emailMessage);
    }

}
