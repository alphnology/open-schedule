package com.alphnology.views.speakers;

import com.alphnology.data.Speaker;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import org.springframework.util.StringUtils;

import java.util.function.Consumer;

import static com.alphnology.utils.SpeakerHelper.getSocialLinks;

public class SpeakersViewCard extends ListItem {

    public SpeakersViewCard(Speaker speaker, String photoUrl, Consumer<Speaker> callback) {
        addClassNames("speaker-card", Display.FLEX, FlexDirection.COLUMN, AlignItems.START, Padding.MEDIUM);
        addClickListener(event -> callback.accept(speaker));

        Div photoWrap = new Div();
        photoWrap.addClassName("speaker-photo-wrap");

        Image image = new Image();
        Tooltip.forComponent(image).withText(speaker.getName()).withPosition(Tooltip.TooltipPosition.BOTTOM_END);
        if (StringUtils.hasText(photoUrl)) {
            image.setSrc(photoUrl);
            image.setAlt(speaker.getName());
        }
        photoWrap.add(image);

        Span nameSpan = new Span(speaker.getName());
        nameSpan.addClassNames("speaker-name", Margin.Top.SMALL);

        Span roleSpan = new Span();
        roleSpan.addClassName("speaker-role");
        if (StringUtils.hasText(speaker.getTitle()) || StringUtils.hasText(speaker.getCompany())) {
            roleSpan.setText("%s at %s".formatted(speaker.getTitle(), speaker.getCompany()));
        }

        Paragraph bio = new Paragraph(speaker.getBio().substring(0, Math.min(speaker.getBio().length(), 200)));
        bio.addClassNames("speaker-bio", Margin.Vertical.SMALL);

        Footer footer = new Footer();
        footer.addClassNames(Display.FLEX, JustifyContent.START, AlignItems.CENTER, Width.FULL, Gap.SMALL, Margin.Top.AUTO);

        if (StringUtils.hasText(speaker.getCountry())) {
            Image flag = new Image(
                    "https://flagcdn.com/%s.svg".formatted(speaker.getCountry().toLowerCase()),
                    speaker.getCountry());
            flag.addClassName("speaker-flag");
            Tooltip.forComponent(flag).withText(speaker.getCountry()).withPosition(Tooltip.TooltipPosition.BOTTOM_END);
            footer.add(flag);
        }

        footer.add(getSocialLinks(speaker));

        add(photoWrap, nameSpan, roleSpan, bio, footer);
    }
}
