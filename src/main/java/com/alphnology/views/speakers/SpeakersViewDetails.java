package com.alphnology.views.speakers;

import com.alphnology.data.Speaker;
import com.alphnology.services.SessionRatingService;
import com.alphnology.services.SessionService;
import com.alphnology.utils.DateTimeFormatterUtils;
import com.alphnology.utils.SpeakerHelper;
import com.alphnology.views.rate.RatingEventBus;
import com.alphnology.views.schedule.ScheduleViewDetails;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;

import static com.alphnology.utils.SessionHelper.tagSession;
import static com.alphnology.utils.SpeakerHelper.getSocialLinks;

public class SpeakersViewDetails extends Div {

    private final RatingEventBus ratingEventBus;
    private final transient SessionRatingService sessionRatingService;
    private final transient SessionService sessionService;

    public SpeakersViewDetails(RatingEventBus ratingEventBus, SessionService sessionService, SessionRatingService sessionRatingService) {
        this.ratingEventBus = ratingEventBus;
        this.sessionService = sessionService;
        this.sessionRatingService = sessionRatingService;
    }


    public void showSpeaker(Speaker speaker) {
        Dialog dialog = new Dialog();
        dialog.setWidth("1024px");
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setCloseOnOutsideClick(true);
        dialog.open();

        Span title = new Span();
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.SEMIBOLD);
        title.setText(speaker.getName());

        Button close = new Button("Close", VaadinIcon.CLOSE.create());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        close.setTooltipText("Close this speaker information");
        close.addClickListener(event -> dialog.close());

        dialog.getHeader().add(title);
        dialog.getFooter().add(close);


        dialog.add(container(speaker));
        dialog.add(new Hr());
        dialog.add(sessionContainer(speaker));

    }


    private Div sessionContainer(Speaker speaker) {
        Span title = new Span("Sessions");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD, LumoUtility.Padding.SMALL);
        title.addClassNames(LumoUtility.BorderRadius.MEDIUM);

        Div containerSessions = new Div(title);
        containerSessions.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.Row.SMALL);

        speaker.getSessions().forEach(session -> {
            Span sessionTitle = new Span("(%s) %s".formatted(session.getType().getDisplay(), session.getTitle()));
            sessionTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);

            String starDate = session.getStartTime().format(DateTimeFormatterUtils.dateFormatter);
            String startTime = session.getStartTime().format(DateTimeFormatterUtils.timeFormatter);

            Span sessionSubtitle = new Span("%s - %s at %s".formatted(starDate, startTime, session.getRoom().getName()));
            sessionSubtitle.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.TextColor.SECONDARY);

            Div tagSession = tagSession(session);

            Div containerSession = new Div(sessionTitle, sessionSubtitle, tagSession);
            containerSession.getStyle().setCursor("pointer");
            containerSession.addClassNames(
                    LumoUtility.Background.CONTRAST_5,
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.COLUMN,
                    LumoUtility.AlignItems.START,
                    LumoUtility.Padding.MEDIUM,
                    LumoUtility.BorderRadius.LARGE,
                    "transition-card"
            );
            containerSession.addClickListener(event -> {

                ScheduleViewDetails scheduleViewDetails = new ScheduleViewDetails(ratingEventBus, sessionService, sessionRatingService);
                sessionService.get(session.getCode())
                        .ifPresent(scheduleViewDetails::showSession);
            });
            containerSessions.add(containerSession);

        });

        return containerSessions;
    }

    private Div container(Speaker speaker) {
        Image image = SpeakerHelper.getImage(speaker);
        image.addClassNames("flex-wrap-image-speaker");

        Span header = new Span();
        header.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, LumoUtility.Padding.SMALL);
        header.getElement().getThemeList().add(Lumo.DARK);
        header.addClassNames(LumoUtility.BorderRadius.MEDIUM);
        header.setText("%s at %s".formatted(speaker.getTitle(), speaker.getCompany()));

        Paragraph description = new Paragraph(speaker.getBio());
        description.addClassNames(LumoUtility.Margin.Vertical.MEDIUM, LumoUtility.Padding.Horizontal.SMALL);
        description.getStyle().set("white-space", "pre-line").set("flex-grow", "1").set("text-align", "justify");

        Footer footer = new Footer();
        footer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.START, LumoUtility.AlignItems.CENTER, LumoUtility.Width.FULL);

        Image country = new Image();
        country.setWidth("10%");
        if (!speaker.getCountry().isEmpty()) {
            country.setSrc("https://flagcdn.com/%s.svg".formatted(speaker.getCountry().toLowerCase()));
            country.setAlt(speaker.getCountry());

            Tooltip.forComponent(country)
                    .withText(speaker.getCountry())
                    .withPosition(Tooltip.TooltipPosition.BOTTOM_END);
        } else {
            country.setVisible(false);
        }
        Div socialLinksLayout = getSocialLinks(speaker);

        footer.add(country, socialLinksLayout);

        Div container = new Div();
        container.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.FlexDirection.Breakpoint.Medium.ROW,
                LumoUtility.Gap.Column.LARGE
        );

        Div detailsContainer = new Div(header, description, footer);
        detailsContainer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.Row.MEDIUM);

        container.add(image, detailsContainer);

        return container;

    }

}
