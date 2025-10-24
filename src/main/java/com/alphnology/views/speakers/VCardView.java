package com.alphnology.views.speakers;

import com.alphnology.data.Contactable;
import com.alphnology.services.AttenderService;
import com.alphnology.services.QrService;
import com.alphnology.services.SpeakerService;
import com.alphnology.utils.DownloadHandlerUtils;
import com.alphnology.utils.VCardUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
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

/**
 * @author me@fredpena.dev
 * @created 19/10/2025  - 19:43
 */

@PageTitle("Contact Profile")
@Route("vcard/:code?/:type?")
@AnonymousAllowed
public class VCardView extends VerticalLayout implements BeforeEnterObserver {

    private final transient SpeakerService speakerService;
    private final transient AttenderService attenderService;
    private final transient QrService qrService;
    private Optional<Contactable> contactableOpt = Optional.empty();
    private String currentType = "speaker";

    public VCardView(SpeakerService speakerService, AttenderService attenderService, QrService qrService) {
        this.speakerService = speakerService;
        this.attenderService = attenderService;
        this.qrService = qrService;
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        addClassNames(LumoUtility.Padding.LARGE, LumoUtility.Background.BASE);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String codeParam = event.getRouteParameters().get("code").orElse(null);
        String typeParam = event.getRouteParameters().get("type").orElse(null);

        if ((codeParam == null || codeParam.isBlank()) && (typeParam == null || typeParam.isBlank())) {
            event.forwardTo(SpeakersView.class);
            return;
        }

        assert typeParam != null;
        this.currentType = typeParam.toLowerCase();

        try {
            assert codeParam != null;
            Long code = Long.parseLong(codeParam);

            if ("speaker".equals(currentType)) {
                this.contactableOpt = speakerService.get(code).map(Contactable::new);
            } else if ("attender".equals(currentType)) {
                this.contactableOpt = attenderService.get(code).map(Contactable::new);
            } else {
                event.forwardTo(com.alphnology.views.speakers.SpeakersView.class);
                return;
            }

            if (contactableOpt.isEmpty()) {
                event.forwardTo(com.alphnology.views.speakers.SpeakersView.class);
                return;
            }

            buildLayout(contactableOpt.get());

        } catch (NumberFormatException e) {
            event.forwardTo(SpeakersView.class);
        }
    }

    private void buildLayout(Contactable contactable) {
        removeAll();

        Div card = new Div();
        card.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.BorderRadius.LARGE, LumoUtility.Padding.LARGE, LumoUtility.BoxShadow.MEDIUM, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.MEDIUM);
        card.setMaxWidth("400px");

        Image image = new Image();
        image.addClassNames(LumoUtility.BorderRadius.LARGE, LumoUtility.BorderRadius.FULL);
        image.setWidth("250px");
        Tooltip.forComponent(image)
                .withText(contactable.fullName())
                .withPosition(Tooltip.TooltipPosition.BOTTOM_END);

        if (contactable.photo() != null && contactable.photo().length > 0) {
            image.setSrc(DownloadHandlerUtils.fromByte(contactable.photo()));
            image.setAlt(contactable.fullName());
        } else {
            image.setVisible(false);
        }

        H2 name = new H2(contactable.fullName());
        name.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.Margin.NONE);

        Span title = new Span(contactable.title());
        title.addClassNames(LumoUtility.TextColor.SECONDARY);

        Span company = new Span(contactable.company());
        company.addClassNames(LumoUtility.TextColor.TERTIARY, LumoUtility.FontSize.SMALL);


        Anchor downloadVcf = createDownloadButton(contactable);
        Button shareButton = createShareButton(contactable);
        Button qrButton = createQrButton(contactable);


        Div buttonLayout = new Div(downloadVcf, shareButton, qrButton);
        buttonLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.MEDIUM, LumoUtility.Margin.Top.LARGE, LumoUtility.FlexWrap.WRAP, LumoUtility.JustifyContent.CENTER);

        card.add(image, name);
        if (!title.getText().isEmpty()) card.add(title);
        if (!company.getText().isEmpty()) card.add(company);
        card.add(buttonLayout);
        add(card);
    }

    private Anchor createDownloadButton(Contactable contactable) {
        String vcardString = VCardUtil.buildVCard(contactable);
        InputStreamDownloadHandler vcfResource = DownloadHandler.fromInputStream((event) -> {
            try {
                byte[] bytes = vcardString.getBytes(StandardCharsets.UTF_8);
                return new DownloadResponse(new ByteArrayInputStream(bytes), "contact-%s.vcf".formatted(contactable.fullName().toLowerCase().replace(" ", "-")), "text/vcard", bytes.length);
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

    private Button createShareButton(Contactable contactable) {
        Button shareButton = new Button("Share Profile", VaadinIcon.SHARE.create());
        shareButton.addClickListener(e -> {
            String url = VCardUtil.getVCardUrl(contactable, currentType);
            String text = "Check out the profile for %s, a speaker at our event.".formatted(contactable.fullName());
            String title = "Speaker Profile: " + contactable.fullName();

            UI.getCurrent().getPage().executeJs("if (navigator.share) { navigator.share({ title: $0, text: $1, url: $2 }); } else { alert('Web Share API not supported in your browser.'); }", title, text, url);
        });

        return shareButton;
    }

    private Button createQrButton(Contactable contactable) {
        Button qrButton = new Button("Show QR", VaadinIcon.QRCODE.create());
        qrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        qrButton.addClickListener(event -> {
            VCardUtil.openQr(contactable, currentType, qrService);
        });
        return qrButton;
    }


}
