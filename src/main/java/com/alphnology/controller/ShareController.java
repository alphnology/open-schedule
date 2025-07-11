package com.alphnology.controller;

import com.alphnology.data.Event;
import com.alphnology.services.EventService;
import com.alphnology.services.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

/**
 * @author me@fredpena.dev
 * @created 11/07/2025  - 16:55
 */
@Controller
public class ShareController {

    private final EventService eventService;
    private final SessionService sessionService;
    private final String baseUrl;

    public ShareController(EventService eventService, SessionService sessionService, @Value("${event.schedule}") String schedule) {
        this.eventService = eventService;
        this.sessionService = sessionService;
        this.baseUrl = schedule;
    }

    @GetMapping("/share/{sessionId}")
    public String sharePage(@PathVariable Long sessionId, Model model) {
        sessionService.get(sessionId)
                .ifPresent(session -> {

                    Optional<Event> optionalEvent = eventService.findAll().stream().findFirst();
                    if (optionalEvent.isEmpty()) {
                        return;
                    }

                    String message = """
                            Looking forward to the "%s" session at #%s%s. 
                            
                            See you in the %s room!
                            
                            Check out the full schedule: %s
                            """.formatted(session.getTitle(), optionalEvent.get().getName().replace(" ", ""), session.getStartTime().getYear(), session.getRoom().getName(), baseUrl);

                    String url = "%s/share/%s".formatted(baseUrl, session.getCode());
                    model.addAttribute("title", "Attending: " + session.getTitle());
                    model.addAttribute("description", message);
                    model.addAttribute("pageUrl", url);
                });


        return "session-share";
    }
}
