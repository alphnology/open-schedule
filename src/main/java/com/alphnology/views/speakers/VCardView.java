package com.alphnology.views.speakers;

import com.alphnology.data.Speaker;
import com.alphnology.services.QrService;
import com.alphnology.services.SpeakerService;
import com.alphnology.utils.VCardUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.alphnology.utils.SpeakerHelper.getImage;

/**
 * @author me@fredpena.dev
 * @created 19/10/2025  - 19:43
 */

@PageTitle("Contact Card")
@Route("vcard/:speakerCode?")
@AnonymousAllowed
public class VCardView extends VerticalLayout implements BeforeEnterObserver {

    private final transient SpeakerService speakerService;
    private final transient QrService qrService;
    private Optional<Speaker> speakerOpt = Optional.empty();

    public VCardView(SpeakerService speakerService, QrService qrService) {
        this.speakerService = speakerService;
        this.qrService = qrService;
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        addClassNames(LumoUtility.Padding.LARGE, LumoUtility.Background.BASE);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String speakerCode = event.getRouteParameters().get("speakerCode").orElse(null);

        if (speakerCode == null || speakerCode.isBlank()) {
            event.forwardTo(SpeakersView.class);
            return;
        }

        try {
            Long code = Long.parseLong(speakerCode);
            this.speakerOpt = speakerService.get(code);

            if (speakerOpt.isEmpty()) {
                event.forwardTo(SpeakersView.class);
                return;
            }

            buildLayout(speakerOpt.get());

        } catch (NumberFormatException e) {
            event.forwardTo(SpeakersView.class);
        }
    }

    private void buildLayout(Speaker speaker) {
        removeAll();

        Div card = new Div();
        card.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.LARGE,
                LumoUtility.BoxShadow.MEDIUM,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.MEDIUM
        );
        card.setMaxWidth("400px");

        Image image = getImage(speaker);
        image.setWidth("250px");
        image.addClassNames(LumoUtility.BorderRadius.FULL);

        H2 name = new H2(speaker.getName());
        name.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.Margin.NONE);

        Span title = new Span(speaker.getTitle());
        title.addClassNames(LumoUtility.TextColor.SECONDARY);

        Span company = new Span(speaker.getCompany());
        company.addClassNames(LumoUtility.TextColor.TERTIARY, LumoUtility.FontSize.SMALL);

        Anchor downloadVcf = createDownloadButton(speaker);
        Button shareButton = createShareButton(speaker);
        Button qrButton = createQrButton(speaker);


        Div buttonLayout = new Div(downloadVcf, shareButton, qrButton);
        buttonLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.MEDIUM, LumoUtility.Margin.Top.LARGE, LumoUtility.FlexWrap.WRAP, LumoUtility.JustifyContent.CENTER);

        card.add(image, name, title, company, buttonLayout);
        add(card);
    }

    private Anchor createDownloadButton(Speaker speaker) {
        String vcardString = VCardUtil.buildVCard(speaker);
        InputStreamDownloadHandler vcfResource = DownloadHandler.fromInputStream((event) -> {
            try {
                byte[] bytes = vcardString.getBytes(StandardCharsets.UTF_8);
                return new DownloadResponse(new ByteArrayInputStream(bytes), "contact-%s.vcf".formatted(speaker.getName().toLowerCase().replace(" ", "-")), "text/vcard", bytes.length);
            } catch (Exception e) {
                return DownloadResponse.error(500);
            }
        });

        Anchor downloadLink = new Anchor(vcfResource, "");
        downloadLink.getElement().setAttribute("download", true);

        Button button = new Button("Add to Contacts", VaadinIcon.DOWNLOAD_ALT.create());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        downloadLink.add(button);

        return downloadLink;
    }

    private Button createShareButton(Speaker speaker) {
        Button shareButton = new Button("Share Profile", VaadinIcon.SHARE.create());
        shareButton.addClickListener(e -> {
            String vCardUrl = VCardUtil.getVCardUrl(speaker);
            String text = "Check out the profile for %s, a speaker at our event.".formatted(speaker.getName());
            String title = "Speaker Profile: " + speaker.getName();

            UI.getCurrent().getPage().executeJs(
                    "if (navigator.share) { navigator.share({ title: $0, text: $1, url: $2 }); } else { alert('Web Share API not supported in your browser.'); }",
                    title, text, vCardUrl
            );
        });
//        shareButton.addClickListener(e -> {
//            String vCardUrl = VCardUtil.getVCardUrl(speaker);
//            String text = "Check out the profile for %s, a speaker at our event.".formatted(speaker.getName());
//            String title = "Speaker Profile: " + speaker.getName();
//
//            UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", "%s%s\n%s".formatted(title, text, vCardUrl));
//            NotificationUtils.info("Message copied to clipboard");
//        });
        return shareButton;
    }

    private Button createQrButton(Speaker speaker) {
        Button qrButton = new Button("Show QR", VaadinIcon.QRCODE.create());
        qrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        qrButton.addClickListener(event -> {
            String vCardUrl = VCardUtil.getVCardUrl(speaker);
            byte[] qrCodeBytes = qrService.generatePng(vCardUrl, 256);

            InputStreamDownloadHandler qrResource = DownloadHandler.fromInputStream((event1) -> {
                try {
                    return new DownloadResponse(new ByteArrayInputStream(qrCodeBytes), "vcard-qr.png", null, qrCodeBytes.length);
                } catch (Exception e) {
                    return DownloadResponse.error(500);
                }
            });

            Dialog qrDialog = new Dialog();
            qrDialog.setDraggable(true);
            qrDialog.setHeaderTitle("Scan to Share");
            Image qrImage = new Image(qrResource, "Profile QR Code");
            qrImage.setWidth("300px");
            qrDialog.add(qrImage);
            qrDialog.open();
        });
        return qrButton;
    }
}
