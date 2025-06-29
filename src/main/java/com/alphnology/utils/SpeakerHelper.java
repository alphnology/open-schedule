package com.alphnology.utils;

import com.alphnology.data.Session;
import com.alphnology.data.Speaker;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * @author me@fredpena.dev
 * @created 16/06/2025  - 16:34
 */
public class SpeakerHelper {

    private SpeakerHelper() {
    }

    public static Image getImage(Speaker speaker) {
        Image image = new Image();
        image.addClassNames(LumoUtility.BorderRadius.LARGE);
        Tooltip.forComponent(image)
                .withText(speaker.getName())
                .withPosition(Tooltip.TooltipPosition.BOTTOM_END);

        if (!speaker.getPhotoUrl().isEmpty()) {
            image.setSrc(speaker.getPhotoUrl());
            image.setAlt(speaker.getPhotoUrl());
        } else {
            image.setVisible(false);
        }

        return image;
    }

    public static Div getSocialLinks(Speaker speaker) {
        Div socialLinksLayout = new Div();
        socialLinksLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW, LumoUtility.Margin.Left.AUTO, LumoUtility.Gap.Column.SMALL, LumoUtility.FlexWrap.WRAP);

        for (String link : speaker.getNetworking()) {
            Component socialIcon = SocialLinkUtil.createSocialIconLink(link);
            socialLinksLayout.add(socialIcon);
        }
        return socialLinksLayout;
    }

    public static HorizontalLayout createSpeakerRenderer(Session session) {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        AvatarGroup avatarGroup = new AvatarGroup();
        avatarGroup.addClassNames(LumoUtility.Width.AUTO);

        Div speakerContainer = new Div();
        speakerContainer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Width.AUTO
        );

        session.getSpeakers().forEach(speaker -> {
            AvatarGroup.AvatarGroupItem avatar = new AvatarGroup.AvatarGroupItem(speaker.getName(), speaker.getPhotoUrl());
            avatarGroup.add(avatar);

            Span speakerSpan = new Span();
            speakerSpan.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.SEMIBOLD);
            speakerSpan.setText(speaker.getName());
            speakerContainer.add(speakerSpan);
        });


        horizontalLayout.add(avatarGroup, speakerContainer);

        return horizontalLayout;
    }
}
