package com.alphnology.views.schedule;

import com.alphnology.data.Session;
import com.alphnology.data.enums.SessionType;
import com.alphnology.services.SessionService;
import com.alphnology.utils.Broadcaster;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility.*;

import java.util.function.Consumer;

import static com.alphnology.utils.Broadcaster.RATE_SESSION;
import static com.alphnology.utils.SessionHelper.*;
import static com.alphnology.utils.SpeakerHelper.createSpeakerRenderer;

public class ScheduleViewCard extends Div {

    private Registration broadcasterRegistration;
    private final Div ratingDiv = new Div();


    public ScheduleViewCard(Session session, SessionService sessionService, Consumer<Session> callback) {
        setWidthFull();
        addClassNames(Display.FLEX, FlexDirection.COLUMN, AlignItems.START, Padding.MEDIUM, Gap.Row.XSMALL);
        getStyle().setCursor("pointer");

        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
        }

        broadcasterRegistration = Broadcaster.register(RATE_SESSION, object ->
                getUI().ifPresent(ui -> ui.access(() -> sessionService.get(session.getCode())
                        .ifPresent(this::ratingDiv))));


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

            ratingDiv(session);

            footer.add(ratingDiv);
        }

        add(header, levelDiv, trackDiv, roomDiv, footer);

    }

    private void ratingDiv(Session session) {
        ratingDiv.removeAll();

        double averageRating = session.getAverageRating();
        int ratingCount = session.getRatingCount();

        Span ratingDetailsSpan;
        if (ratingCount > 0) {
            String plural = ratingCount == 1 ? "Rate" : "Rates";
            ratingDetailsSpan = new Span(String.format("%.2f / %d %s", averageRating, ratingCount, plural));
        } else {
            ratingDetailsSpan = new Span("(Not rating yet)");
        }
        ratingDetailsSpan.addClassNames(FontSize.SMALL);

        ratingDiv.add(createRatingDisplay(averageRating), ratingDetailsSpan);
        ratingDiv.addClassNames(Margin.Left.AUTO, Display.FLEX, FlexDirection.ROW, Gap.Column.MEDIUM);

        Tooltip.forComponent(ratingDiv)
                .withText(averageRating > 0 ? "Rating: " + String.format("%.2f", averageRating) : "Not rating yet")
                .withPosition(Tooltip.TooltipPosition.BOTTOM_END);

    }


    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

}
