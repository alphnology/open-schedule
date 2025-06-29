package com.alphnology.views.schedule;

import com.alphnology.data.Session;
import com.alphnology.data.User;
import com.alphnology.services.SessionRatingService;
import com.alphnology.services.SessionService;
import com.alphnology.utils.DateTimeFormatterUtils;
import com.alphnology.utils.SpeakerHelper;
import com.alphnology.views.login.LoginView;
import com.alphnology.views.rate.RatingDialog;
import com.alphnology.views.rate.RatingEventBus;
import com.alphnology.views.speakers.SpeakersViewDetails;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;


import static com.alphnology.utils.SessionHelper.*;
import static com.alphnology.utils.SpeakerHelper.getSocialLinks;

public class ScheduleViewDetails extends Div {

    private final RatingEventBus ratingEventBus;
    private final transient SessionService sessionService;
    private final transient SessionRatingService sessionRatingService;

    private final Div ratingDiv = new Div();

    public ScheduleViewDetails(RatingEventBus ratingEventBus, SessionService sessionService, SessionRatingService sessionRatingService) {
        this.ratingEventBus = ratingEventBus;
        this.sessionService = sessionService;
        this.sessionRatingService = sessionRatingService;
    }

    public void showSession(Session session) {
        Dialog dialog = new Dialog();
        dialog.setWidth("1024px");
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setCloseOnOutsideClick(true);
        dialog.open();

        Span header = new Span();
        header.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD, LumoUtility.Padding.SMALL);
        header.getElement().getThemeList().add(Lumo.DARK);
        header.addClassNames(LumoUtility.BorderRadius.MEDIUM, LumoUtility.Width.FULL);
        header.setText(session.getTitle());

        Button close = new Button("Close", VaadinIcon.CLOSE.create());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        close.setTooltipText("Close this session information");
        close.addClickListener(event -> dialog.close());

        Button rate = new Button("Rate - Login Required", VaadinIcon.STAR.create());
        rate.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        rate.setTooltipText("Rate this session information");

        rate.addClickListener(e -> {
            User currentUser = VaadinSession.getCurrent().getAttribute(User.class);
            if (currentUser == null) {
                UI.getCurrent().navigate(LoginView.class);
                return;
            }


            // Pass the injected RatingEventBus to the RatingDialog
            sessionRatingService.findByUsersAndSession(currentUser, session)
                    .ifPresentOrElse(sessionRating -> {
                        RatingDialog ratingDialog = new RatingDialog(sessionRatingService, session, sessionRating, ratingEventBus, () -> sessionService.get(session.getCode())
                                .ifPresent(this::calculateAverageRating));
                        ratingDialog.open();
                    }, () -> {
                        RatingDialog ratingDialog = new RatingDialog(sessionRatingService, session, null, ratingEventBus, () -> sessionService.get(session.getCode())
                                .ifPresent(this::calculateAverageRating));
                        ratingDialog.open();
                    });
        });

        dialog.getHeader().add(header);
        dialog.getFooter().add(rate, close);

        dialog.add(container(session));
        dialog.add(new Hr());
        dialog.add(sessionContainer(session));

    }


    private void calculateAverageRating(Session session) {
        ratingDiv.removeAll();

        double averageRating = session.getAverageRating();
        ratingDiv.add(createRatingDisplay(averageRating));
        Tooltip.forComponent(ratingDiv)
                .withText(averageRating > 0 ? "Rating: " + String.format("%.2f", averageRating) : "Not rating yet")
                .withPosition(Tooltip.TooltipPosition.BOTTOM_END);
    }

    private Div sessionContainer(Session session) {
        Span title = new Span("Speakers");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD, LumoUtility.Padding.SMALL);
        title.addClassNames(LumoUtility.BorderRadius.MEDIUM);

        Div containerSpeakers = new Div(title);
        containerSpeakers.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.Row.SMALL);

        session.getSpeakers().forEach(speaker -> {

            Image image = SpeakerHelper.getImage(speaker);
            image.addClassNames("flex-wrap-image-session-speaker");

            Span speakerName = new Span(speaker.getName());
            speakerName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);

            Span speakerTitle = new Span("%s at %s".formatted(speaker.getTitle(), speaker.getCompany()));
            speakerTitle.addClassNames(LumoUtility.FontSize.MEDIUM);

            Footer footer = new Footer();
            footer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.AlignItems.CENTER, LumoUtility.Width.FULL, LumoUtility.Gap.MEDIUM);

            Image country = new Image();
            country.setWidth("30px");
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

            Div containerSpeakerInfo = new Div(speakerName, speakerTitle, footer);
            containerSpeakerInfo.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.COLUMN,
                    LumoUtility.AlignItems.START,
                    LumoUtility.Padding.MEDIUM,
                    LumoUtility.Width.FULL
            );

            Div containerSpeaker = new Div(image, containerSpeakerInfo);
            containerSpeaker.getStyle().setCursor("pointer");
            containerSpeaker.addClassNames(
                    LumoUtility.Background.CONTRAST_5,
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.COLUMN,
                    LumoUtility.AlignItems.CENTER,
                    LumoUtility.Padding.MEDIUM,
                    LumoUtility.BorderRadius.LARGE,
                    LumoUtility.FlexDirection.Breakpoint.Medium.ROW,
                    LumoUtility.Gap.Column.LARGE,
                    "transition-card"
            );
            containerSpeaker.addClickListener(event -> new SpeakersViewDetails(ratingEventBus, sessionService, sessionRatingService).showSpeaker(speaker));

            containerSpeakers.add(containerSpeaker);
        });

        return containerSpeakers;
    }

    private Div container(Session session) {
        Paragraph description = new Paragraph(session.getDescription());
        description.addClassNames(LumoUtility.Margin.Vertical.MEDIUM, LumoUtility.Padding.Horizontal.SMALL);
        description.getStyle().set("white-space", "pre-line").set("flex-grow", "1").set("text-align", "justify");

        String starDate = session.getStartTime().format(DateTimeFormatterUtils.dateFormatter);
        String startTime = session.getStartTime().format(DateTimeFormatterUtils.timeFormatter);

        Span sessionDate = new Span("%s - %s at %s".formatted(starDate, startTime, session.getRoom().getName()));
        sessionDate.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.EXTRABOLD);

        Span sessionType = new Span(session.getType().getDisplay());
        sessionType.getElement().getThemeList().add("badge contrast");
        sessionType.getStyle().setWidth("10rem");

        calculateAverageRating(session);

        Div headerLayout = new Div(sessionType, ratingDiv);
        headerLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW, LumoUtility.Width.FULL, LumoUtility.JustifyContent.BETWEEN);

        Footer footer = new Footer();
        footer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.AlignItems.CENTER, LumoUtility.Width.FULL, LumoUtility.Gap.MEDIUM);

        Image country = new Image();
        country.setWidth("40px");
        if (session.getLanguage() != null) {
            country.setSrc("https://flagcdn.com/%s.svg".formatted(session.getLanguage().name().toLowerCase()));
            country.setAlt(session.getLanguage().getDisplay());

            Tooltip.forComponent(country)
                    .withText(session.getLanguage().getDisplay())
                    .withPosition(Tooltip.TooltipPosition.BOTTOM_END);
        } else {
            country.setVisible(false);
        }

        Div tagSession = tagSession(session);

        footer.add(country, tagSession);

        Div container = new Div();
        container.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.FlexDirection.Breakpoint.Medium.ROW,
                LumoUtility.Gap.Column.LARGE
        );

        Div levelDiv = getLevel(session);

        Div trackDiv = getTrack(session);

        Div detailsContainer = new Div(headerLayout, description, sessionDate, levelDiv, trackDiv, footer);
        detailsContainer.setWidthFull();
        detailsContainer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.Row.MEDIUM);
        container.add(detailsContainer);

        return container;

    }


}
