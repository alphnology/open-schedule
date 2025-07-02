package com.alphnology.views.rate;

import com.alphnology.data.SessionRating;
import com.alphnology.services.SessionRatingService;
import com.alphnology.utils.DateTimeFormatterUtils;
import com.alphnology.utils.RendererUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * @author me@fredpena.dev
 * @created 25/06/2025  - 12:13
 */
public class RatedSessionCard extends VerticalLayout {

    public RatedSessionCard(SessionRatingService sessionRatingService, SessionRating rating, Runnable callback) {
        String starDate = rating.getSession().getStartTime().format(DateTimeFormatterUtils.dateTimeFormatter);

        var titleDisplay = "%s %s".formatted(rating.getSession().getTitle(), rating.getSession().getRoom() == null ? " (" + starDate + ")" : "");
        H4 title = new H4(titleDisplay);

        String speakerDisplay = "%s".formatted(rating.getSession().getSpeakers().isEmpty() ? "" : "by " + RendererUtils.getSessionSpeakers(rating.getSession().getSpeakers()));
        Span speaker = new Span(speakerDisplay);
        speaker.addClassNames(LumoUtility.TextColor.SECONDARY);

        StarRating scoreDisplay = new StarRating();
        scoreDisplay.setValue(rating.getScore());
        scoreDisplay.setReadOnly(true);

        Button rateButton = new Button("Rate Session");
        rateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        rateButton.addClickListener(e -> {
            // Pass the RatingEventBus to the RatingDialog
            RatingDialog dialog = new RatingDialog(sessionRatingService, rating.getSession(), rating, callback);
            dialog.open();
        });

        Paragraph commentDisplay = new Paragraph(rating.getComment());
        commentDisplay.getStyle().set("font-style", "italic");
        commentDisplay.setVisible(rating.getComment() != null && !rating.getComment().isBlank());

        setPadding(true);
        getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        getStyle().set("opacity", "0.8");

        add(title, speaker, scoreDisplay, rateButton, commentDisplay);
    }
}
