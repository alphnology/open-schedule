package com.alphnology.views.speakers;

import com.alphnology.data.Contactable;
import com.alphnology.data.Speaker;
import com.alphnology.services.QrService;
import com.alphnology.services.SessionRatingService;
import com.alphnology.services.SessionService;
import com.alphnology.services.SpeakerService;
import com.alphnology.services.UserService;
import com.alphnology.utils.DateTimeFormatterUtils;
import com.alphnology.utils.SpeakerHelper;
import com.alphnology.utils.VCardUtil;
import com.alphnology.views.schedule.ScheduleViewDetails;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.util.StringUtils;

import static com.alphnology.utils.SessionHelper.tagSession;
import static com.alphnology.utils.SpeakerHelper.getSocialLinks;

public class SpeakersViewDetails extends Div {

    private final transient SessionRatingService sessionRatingService;
    private final transient SessionService sessionService;
    private final transient SpeakerService speakerService;
    private final transient UserService userService;
    private final transient QrService qrService;

    public SpeakersViewDetails(SessionService sessionService, SessionRatingService sessionRatingService, SpeakerService speakerService, UserService userService, QrService qrService) {
        this.sessionService = sessionService;
        this.sessionRatingService = sessionRatingService;
        this.speakerService = speakerService;
        this.userService = userService;
        this.qrService = qrService;
    }


    public void showSpeaker(Speaker speaker) {
        Speaker hydratedSpeaker = speakerService.get(speaker.getCode()).orElse(speaker);

        Dialog dialog = new Dialog();
        dialog.setWidth("1024px");
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setCloseOnOutsideClick(true);
        dialog.open();

        Span title = new Span();
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.SEMIBOLD);
        title.setText(hydratedSpeaker.getName());

        Button close = new Button("Close", VaadinIcon.CLOSE.create());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        close.setTooltipText("Close this speaker information");
        close.addClickListener(event -> dialog.close());

        dialog.getHeader().add(title);
        dialog.getFooter().add(close);


        dialog.add(container(hydratedSpeaker));
        dialog.add(new Hr());
        dialog.add(sessionContainer(hydratedSpeaker));

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

                ScheduleViewDetails scheduleViewDetails = new ScheduleViewDetails(sessionService, sessionRatingService, speakerService, userService, qrService);
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

        String vCardUrl = VCardUtil.getVCardUrl(new Contactable(speaker), "speaker");

        DownloadHandler qrResource = VCardUtil.downloadHandler(qrService, vCardUrl);

        Image qrCodeImage = new Image(qrResource, "vCard QR Code");
        qrCodeImage.setWidth("200px");

        Div imageContainer = new Div(qrCodeImage);
        imageContainer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.AlignItems.CENTER
        );

        Span header = new Span();
        header.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, LumoUtility.Padding.SMALL);
        header.getElement().getThemeList().add(Lumo.DARK);
        header.addClassNames(LumoUtility.BorderRadius.MEDIUM);
        if (StringUtils.hasText(speaker.getTitle()) || StringUtils.hasText(speaker.getCompany())) {
            header.setText("%s at %s".formatted(speaker.getTitle(), speaker.getCompany()));
        }

        Paragraph description = new Paragraph(speaker.getBio());
        description.addClassNames(LumoUtility.Margin.Vertical.MEDIUM, LumoUtility.Padding.Horizontal.SMALL);
        description.getStyle().set("white-space", "pre-line").set("flex-grow", "1").set("text-align", "justify");

        Footer footer = new Footer();
        footer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.AlignItems.CENTER, LumoUtility.Width.FULL);

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

        Button contactButton = new Button("View Contact Info", VaadinIcon.CONNECT.create());
        contactButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        contactButton.addClickListener(e -> UI.getCurrent().getPage().open(vCardUrl, "_blank"));
        contactButton.addClassNames(LumoUtility.Width.AUTO, LumoUtility.Padding.SMALL);

        Div countryAndContact = new Div(qrCodeImage, country, contactButton);
        countryAndContact.addClassNames(LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.FlexDirection.Breakpoint.Small.ROW,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.SMALL);

        Div socialLinksLayout = getSocialLinks(speaker);

        footer.add(countryAndContact, socialLinksLayout);

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
