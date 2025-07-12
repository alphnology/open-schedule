package com.alphnology.views.schedule;

import com.alphnology.data.Event;
import com.alphnology.data.Session;
import com.alphnology.data.User;
import com.alphnology.services.SessionRatingService;
import com.alphnology.services.SessionService;
import com.alphnology.services.UserService;
import com.alphnology.utils.CommonUtils;
import com.alphnology.utils.DateTimeFormatterUtils;
import com.alphnology.utils.NotificationUtils;
import com.alphnology.utils.SpeakerHelper;
import com.alphnology.views.login.LoginView;
import com.alphnology.views.rate.RatingDialog;
import com.alphnology.views.speakers.SpeakersViewDetails;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.alphnology.utils.SessionHelper.*;
import static com.alphnology.utils.SpeakerHelper.getSocialLinks;

public class ScheduleViewDetails extends Div {

    private final transient SessionService sessionService;
    private final transient SessionRatingService sessionRatingService;
    private final transient UserService userService;

    private final Div ratingDiv = new Div();
    private final User currentUser;


    public ScheduleViewDetails(SessionService sessionService, SessionRatingService sessionRatingService, UserService userService) {
        this.sessionService = sessionService;
        this.sessionRatingService = sessionRatingService;
        this.userService = userService;
        this.currentUser = VaadinSession.getCurrent().getAttribute(User.class);
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

        Button rate = new Button();
        rate.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        updateRateButtonState(rate, session);

        Button favorite = new Button();
        favorite.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        updateFavoriteButtonState(favorite, session);

        favorite.addClickListener(e -> {
            if (currentUser == null) {
                UI.getCurrent().navigate(LoginView.class);
                return;
            }

            boolean isCurrentlyFavorite = currentUser.getFavoriteSessions() != null && currentUser.getFavoriteSessions().contains(session.getCode());

            if (isCurrentlyFavorite) {
                currentUser.removeFromFavorite(session.getCode());
                userService.save(currentUser);
                NotificationUtils.info("Removed from favorites");
            } else {
                currentUser.addToFavorite(session.getCode());
                userService.save(currentUser);
                NotificationUtils.success("Added to favorites!");
            }

            updateFavoriteButtonState(favorite, session);
        });

        rate.addClickListener(e -> {
            if (currentUser == null) {
                UI.getCurrent().navigate(LoginView.class);
                return;
            }

            sessionRatingService.findByUsersAndSession(currentUser, session)
                    .ifPresentOrElse(sessionRating -> {
                        RatingDialog ratingDialog = new RatingDialog(sessionRatingService, session, sessionRating, () -> {
                            sessionService.get(session.getCode()).ifPresent(this::calculateAverageRating);
                            updateRateButtonState(rate, session);
                        });
                        ratingDialog.open();
                    }, () -> {
                        RatingDialog ratingDialog = new RatingDialog(sessionRatingService, session, null, () -> {
                            sessionService.get(session.getCode()).ifPresent(this::calculateAverageRating);
                            updateRateButtonState(rate, session);
                        });
                        ratingDialog.open();
                    });

        });

        dialog.getHeader().add(header);

        Event event = VaadinSession.getCurrent().getAttribute(Event.class);

        String sessionName = session.getTitle();
        String room = session.getRoom().getName();
        LocalDateTime sessionDate = session.getStartTime();
        String eventUrl = CommonUtils.getBaseUrl();

        Button attendingButton = new Button("Share", VaadinIcon.SHARE.create());
        attendingButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
        attendingButton.setTooltipText("Share that you're attending this session");

        attendingButton.addClickListener(e -> {
            String message = """
                    Looking forward to the "%s" session at #%s%s. 
                    
                    See you in the %s room!
                    
                    Check out the full schedule: %s
                    """.formatted(sessionName, event.getName().replace(" ", ""), sessionDate.getYear() , room, eventUrl);
            showShareDialog(message, session, sessionName);
        });

        Div footerLayout = new Div(attendingButton, favorite, rate, close);
        footerLayout.addClassNames(
                LumoUtility.Display.GRID,
                LumoUtility.Grid.Column.COLUMNS_2,
                LumoUtility.Display.Breakpoint.Small.FLEX,
                LumoUtility.Gap.SMALL
        );

        dialog.getFooter().add(footerLayout);


        dialog.add(container(session));
        dialog.add(new Hr());
        dialog.add(sessionContainer(session));

    }

    private void updateFavoriteButtonState(Button favoriteButton, Session session) {
        if (currentUser != null) {
            if (currentUser.getFavoriteSessions() != null && currentUser.getFavoriteSessions().contains(session.getCode())) {
                favoriteButton.setText("In Favorites");
                favoriteButton.setIcon(VaadinIcon.HEART.create());
                favoriteButton.setTooltipText("Remove from your favorites");
                favoriteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                favoriteButton.removeThemeVariants(ButtonVariant.LUMO_CONTRAST);
            } else {
                favoriteButton.setText("Add to Favorites");
                favoriteButton.setIcon(VaadinIcon.HEART_O.create());
                favoriteButton.setTooltipText("Add this session to your favorites");
                favoriteButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                favoriteButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
        } else {
            favoriteButton.setText("Favorite - Login Required");
            favoriteButton.setTooltipText("Login to add favorites");
        }
    }

    private void updateRateButtonState(Button rateButton, Session session) {
        if (currentUser != null) {
            userService.get(currentUser.getCode())
                    .ifPresent(user -> {
                        if (user.hasRate(session)) {
                            rateButton.setText("Rated");
                            rateButton.setIcon(VaadinIcon.STAR.create());
                            rateButton.setTooltipText("This session is rated");
                            rateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
                            rateButton.removeThemeVariants(ButtonVariant.LUMO_CONTRAST);
                        } else {
                            rateButton.setText("Rate");
                            rateButton.setIcon(VaadinIcon.STAR_O.create());
                            rateButton.setTooltipText("Rate this session");
                            rateButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                            rateButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
                        }
                    });
        } else {
            rateButton.setText("Rate - Login Required");
            rateButton.setTooltipText("Login to rate session");
        }
    }


    private void calculateAverageRating(Session session) {
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
        ratingDetailsSpan.addClassNames(LumoUtility.FontSize.SMALL);

        ratingDiv.add(createRatingDisplay(averageRating), ratingDetailsSpan);
        ratingDiv.getStyle().set("justify-items", "center");

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
            containerSpeaker.addClickListener(event -> new SpeakersViewDetails(sessionService, sessionRatingService, userService).showSpeaker(speaker));

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

        Div roomDiv = getRoom(session);
        roomDiv.addClassNames(LumoUtility.Display.BLOCK, LumoUtility.Display.Breakpoint.Large.HIDDEN);

        Div detailsContainer = new Div(headerLayout, description, sessionDate, levelDiv, trackDiv, roomDiv, footer);
        detailsContainer.setWidthFull();
        detailsContainer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.Row.MEDIUM);
        container.add(detailsContainer);

        return container;

    }

    private void showShareDialog(String message, Session session, String sessionName) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Share Session");
        dialog.setDraggable(true);

        TextArea textArea = new TextArea("Message to share");
        textArea.setValue(message);
        textArea.setReadOnly(true);
        textArea.setWidthFull();

        Button copyButton = new Button("Copy", VaadinIcon.COPY_O.create());
        copyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        copyButton.setTooltipText("Copy message to clipboard");
        copyButton.addClickListener(e -> {
            UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", message);
            NotificationUtils.info("Message copied to clipboard");
        });

        Button twitterButton = new Button("Share on X", VaadinIcon.TWITTER.create());
        twitterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        twitterButton.setTooltipText("Share on X (formerly Twitter)");
        twitterButton.addClickListener(e -> {
            String tweetUrl = "https://twitter.com/intent/tweet?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", tweetUrl);
        });

        Button linkedinButton = new Button("Share on LinkedIn", LineAwesomeIcon.LINKEDIN.create());
        linkedinButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        linkedinButton.getStyle().set("--lumo-primary-color", "#0077B5");
        linkedinButton.getStyle().set("--lumo-primary-text-color", "#FFFFFF");
        linkedinButton.setTooltipText("Share on LinkedIn");
        linkedinButton.addClickListener(e -> {
            String url = "%s/share/%s".formatted(CommonUtils.getBaseUrl(), session.getCode());
            String linkedInUrl = "https://www.linkedin.com/shareArticle?mini=true"
                              + "&url=" + URLEncoder.encode(url, StandardCharsets.UTF_8);
            UI.getCurrent().getPage().executeJs("window.open($0, '_blank')", linkedInUrl);
        });

        Button emailButton = new Button(VaadinIcon.ENVELOPE_O.create());
        emailButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        emailButton.setTooltipText("Share via your default email client");
        emailButton.addClickListener(e -> {
            String subject = "Check out this session: " + sessionName;
            String mailtoUrl = "mailto:?subject=" + URLEncoder.encode(subject, StandardCharsets.UTF_8)
                               + "&body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            UI.getCurrent().getPage().executeJs("window.open($0, '_self')", mailtoUrl);
        });

        Div bottomBar = new Div(emailButton, copyButton, twitterButton, linkedinButton);
        bottomBar.addClassNames(
                LumoUtility.Display.GRID,
                LumoUtility.Grid.Column.COLUMNS_2,
                LumoUtility.Display.Breakpoint.Small.FLEX,
                LumoUtility.Gap.SMALL
        );

        dialog.add(textArea, bottomBar);
        dialog.open();
    }


}
