package com.alphnology.controller;

import com.alphnology.data.Event;
import com.alphnology.services.SessionService;
import com.alphnology.utils.CommonUtils;
import com.vaadin.flow.server.VaadinSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author me@fredpena.dev
 * @created 11/07/2025  - 16:55
 */
@Controller
@RequiredArgsConstructor
public class ShareController {

    private final SessionService sessionService;

    @GetMapping("/share/{sessionId}")
    public String sharePage(@PathVariable Long sessionId, Model model) {
        System.out.println("sessionId: " + sessionId);
        sessionService.get(sessionId)
                .ifPresent(session -> {

                    Event event = VaadinSession.getCurrent().getAttribute(Event.class);

                    String baseUrl = CommonUtils.getBaseUrl();
                    String message = """
                            Looking forward to the "%s" session at #%s%s. 
                            
                            See you in the %s room!
                            
                            Check out the full schedule: %s
                            """.formatted(session.getTitle(), event.getName().replace(" ", ""), session.getStartTime().getYear(), session.getRoom().getName(), baseUrl);

                    String url = "%s/share/%s".formatted(baseUrl, session.getCode());
                    model.addAttribute("title", "Attending: " + session.getTitle());
                    model.addAttribute("description", message);
                    model.addAttribute("pageUrl", url);
                });


        return "session-share";
    }
}
