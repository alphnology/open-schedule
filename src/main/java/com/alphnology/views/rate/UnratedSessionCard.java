package com.alphnology.views.rate;

import com.alphnology.data.Session;
import com.alphnology.services.SessionRatingService;
import com.alphnology.utils.DateTimeFormatterUtils;
import com.alphnology.utils.RendererUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.format.DateTimeFormatter;

/**
 * @author me@fredpena.dev
 * @created 25/06/2025  - 12:11
 */
public class UnratedSessionCard extends VerticalLayout {


    private final Session session;


    public UnratedSessionCard(SessionRatingService sessionRatingService, Session session, Runnable callback) {
        this.session = session;

        String starDate = session.getStartTime().format(DateTimeFormatterUtils.dateTimeFormatter);

        var titleDisplay = "%s %s".formatted(session.getTitle(), session.getRoom() == null ? " (" + starDate + ")" : "");
        H4 title = new H4(titleDisplay);
        String speakerDisplay = "%s".formatted(session.getSpeakers().isEmpty() ? "" : "by " + RendererUtils.getSessionSpeakers(session.getSpeakers()));
        Span speaker = new Span(speakerDisplay);
        speaker.addClassNames(LumoUtility.TextColor.SECONDARY);

        Button rateButton = new Button("Rate Session");
        rateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        rateButton.addClickListener(e -> {
            // Pass the RatingEventBus to the RatingDialog
            RatingDialog dialog = new RatingDialog(sessionRatingService, session, null, callback);
            dialog.open();
        });

        setPadding(true);
        getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        getStyle().set("border-radius", "var(--lumo-border-radius-m)");

        add(title, speaker, rateButton);
    }


    public boolean matches(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return true;
        }

        String lowerCaseTerm = searchTerm.toLowerCase();

        boolean titleMatches = session.getTitle().toLowerCase().contains(lowerCaseTerm);
        boolean speakerMatches = session.getSpeakers().stream().anyMatch(speaker -> speaker.getName().toLowerCase().contains(lowerCaseTerm));

        return titleMatches || speakerMatches;
    }
}