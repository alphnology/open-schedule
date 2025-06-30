package com.alphnology.views.speakers;

import com.alphnology.data.Speaker;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.theme.lumo.LumoUtility.*;

import java.util.function.Consumer;

import static com.alphnology.utils.SpeakerHelper.getSocialLinks;

public class SpeakersViewCard extends ListItem {


    public SpeakersViewCard(Speaker speaker, Consumer<Speaker> callback) {
        addClassNames(Background.CONTRAST_5, Display.FLEX, FlexDirection.COLUMN, AlignItems.START, Padding.NONE,
                BorderRadius.LARGE);

        getStyle().setCursor("pointer");
        addClickListener(event -> callback.accept(speaker));

        Div div = getDiv(speaker);

        Span header = new Span();
        header.addClassNames(FontSize.XLARGE, FontWeight.SEMIBOLD);
        header.setText(speaker.getName());

        Span subtitle = new Span();
        subtitle.addClassNames(FontSize.SMALL, TextColor.SECONDARY);
        subtitle.setText("%s at %s".formatted(speaker.getTitle(), speaker.getCompany()));

        Paragraph description = new Paragraph(speaker.getBio().substring(0, Math.min(speaker.getBio().length(), 200)));
        description.addClassNames(Margin.Vertical.MEDIUM);
        description.getStyle()
                .set("flex-grow", "1")
                .set("text-align", "justify");


        Footer footer = new Footer();
        footer.addClassNames(Display.FLEX, JustifyContent.START, AlignItems.CENTER, Width.FULL);
        Image country = new Image();
        country.setWidth("20%");
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

        add(div, header, subtitle, description, footer);

    }

    private static Div getDiv(Speaker speaker) {
        Div div = new Div();
        div.addClassNames(Background.CONTRAST, Display.FLEX, AlignItems.CENTER, JustifyContent.CENTER,
                Margin.Bottom.MEDIUM, Overflow.HIDDEN, BorderRadius.MEDIUM, Width.FULL);

        Image image = new Image();
        image.setWidth("100%");

        Tooltip.forComponent(image)
                .withText(speaker.getName())
                .withPosition(Tooltip.TooltipPosition.BOTTOM_END);
        if (!speaker.getPhotoUrl().isEmpty()) {
            image.setSrc(speaker.getPhotoUrl());
            image.setAlt(speaker.getPhotoUrl());
        } else {
            image.setVisible(false);
        }

        div.add(image);
        return div;
    }
}
