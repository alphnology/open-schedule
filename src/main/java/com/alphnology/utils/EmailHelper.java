package com.alphnology.utils;

import com.alphnology.data.User;
import com.alphnology.services.email.EmailMessageOpenSchedule;
import com.alphnology.services.email.EmailOpenScheduleService;
import com.alphnology.services.email.EmailSendException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

/**
 * @author me@fredpena.dev
 * @created 03/05/2025  - 16:38
 */

@Slf4j
public class EmailHelper {

    private EmailHelper() {

    }

    public static void sentEmailChangePassword(EmailOpenScheduleService emailService, User user, String templateId, String subject, String newPassword) {
        EmailMessageOpenSchedule emailMessage = new EmailMessageOpenSchedule();
        emailMessage.setTemplateId(templateId);
        emailMessage.setTo(Set.of(user.getUsername()));
        emailMessage.setSubject(subject);
        emailMessage.setFrom("OpenSchedule");
        emailMessage.setModel(Map.of(
                "username", user.getUsername(),
                "password", newPassword,
                "name", user.getName()
        ));

        try {
            emailService.sendEmail(emailMessage);
            log.info("Email sent to user {}", user.getUsername());
        } catch (EmailSendException ex) {
            log.error("Error sending email to user", ex);
        }
    }

}
