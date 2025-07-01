package com.alphnology.views.schedule;

import com.alphnology.data.Session;
import com.alphnology.data.enums.SessionType;
import com.alphnology.services.SessionService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.theme.lumo.LumoUtility.*;

import java.util.function.Consumer;

import static com.alphnology.utils.SessionHelper.*;
import static com.alphnology.utils.SpeakerHelper.createSpeakerRenderer;

public class ScheduleViewCard extends Div {

    public ScheduleViewCard(Session session, SessionService sessionService, Consumer<Session> callback) {
        setWidthFull();
        addClassNames(Display.FLEX, FlexDirection.COLUMN, AlignItems.START, Padding.MEDIUM, Gap.Row.XSMALL);
        getStyle().setCursor("pointer");


        Span header = new Span();
        header.addClassNames(FontWeight.EXTRABOLD);
        header.setText(session.getTitle());

        Div levelDiv = getLevel(session);

        Div trackDiv = getTrack(session);

        Div roomDiv = getRoom(session);
        roomDiv.addClassNames(Display.BLOCK, Display.Breakpoint.Large.HIDDEN);

        Footer footer = new Footer();
        footer.addClassNames(JustifyContent.START, AlignItems.CENTER);
        footer.add(createSpeakerRenderer(session));

        if (session.getType() != null && session.getType() != SessionType.CB && session.getType() != SessionType.L && session.getType() != SessionType.OS) {
            addClickListener(event -> sessionService.get(session.getCode()).ifPresent(callback));

            double averageRating = session.getAverageRating();
            Div ratingDiv = new Div(createRatingDisplay(averageRating));
            ratingDiv.addClassNames(Margin.Left.AUTO);

            Tooltip.forComponent(ratingDiv)
                    .withText(averageRating > 0 ? "Rating: " + String.format("%.2f", averageRating) : "Not rating yet")
                    .withPosition(Tooltip.TooltipPosition.BOTTOM_END);

            footer.add(ratingDiv);
        }

        add(header, levelDiv, trackDiv, roomDiv, footer);

    }

}
