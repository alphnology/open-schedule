package com.alphnology.utils;

import com.alphnology.data.Session;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.lineawesome.LineAwesomeIcon;

/**
 * @author me@fredpena.dev
 * @created 16/06/2025  - 17:16
 */
public class SessionHelper {

    private SessionHelper() {

    }

    public static Div tagSession(Session session) {
        Div tagSession = new Div();
        tagSession.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.ROW,
                LumoUtility.FlexWrap.WRAP,
                LumoUtility.Gap.Column.SMALL,
                LumoUtility.Padding.Top.MEDIUM
        );

        session.getTags().forEach(tag -> {
            Span span = new Span(tag.getName());
            span.addClassNames(LumoUtility.FontSize.MEDIUM);
            span.addClassNames(LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Margin.Vertical.XSMALL, LumoUtility.BorderRadius.MEDIUM);
            span.getStyle().set("background-color", tag.getColor()).set("color", "white");
            span.getStyle().setCursor("pointer");
            span.addClickListener(event -> {
            });

            tagSession.add(span);

        });
        return tagSession;
    }

    public static Div getLevel(Session session) {
        if (session.getLevel() == null) {
            return new Div();
        }
        return getDisplay("Level:", session.getLevel().getDisplay());
    }

    public static Div getTrack(Session session) {
        if (session.getTrack() == null) {
            return new Div();
        }
        return getDisplay("Track:", session.getTrack().getName());
    }

    public static Div getRoom(Session session) {
        if (session.getRoom() == null) {
            return new Div();
        }
        return getDisplay("Room:", session.getRoom().getName());
    }

    private static Div getDisplay(String label, String display) {
        Div div = new Div();
        div.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.ROW,
//                LumoUtility.FlexDirection.Breakpoint.Small.ROW,
                LumoUtility.Gap.Column.SMALL
        );

        Span nameSpan = new Span();
        nameSpan.addClassNames(LumoUtility.FontWeight.MEDIUM);
        nameSpan.setText(label);

        Span displaySpan = new Span();
        displaySpan.addClassNames(LumoUtility.FontWeight.EXTRABOLD);
        displaySpan.setText(display);

        div.add(nameSpan, displaySpan);

        return div;
    }

    public static Div createRatingDisplay(double averageRating) {
        Div starsContainer = new Div();
        starsContainer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW);
        starsContainer.getStyle().set("gap", "var(--lumo-space-xs)");

        double normalizedRating = Math.clamp(averageRating, 0, 5);

        int fullStarsCount = (int) Math.floor(normalizedRating);
        double decimalPart = normalizedRating - fullStarsCount;
        boolean hasHalfStar = false;

        if (decimalPart >= 0.5) {
            hasHalfStar = true;
        }

        int starsAdded = 0;

        for (int i = 0; i < fullStarsCount; i++) {
            if (starsAdded < 5) {
                starsContainer.add(createStar());
                starsAdded++;
            }
        }

        if (hasHalfStar && starsAdded < 5) {
            starsContainer.add(createHalfStar());
            starsAdded++;
        }

        while (starsAdded < 5) {
            starsContainer.add(createStarEmpty());
            starsAdded++;
        }

        return starsContainer;
    }

    private static Component createStar() {
        SvgIcon star = LineAwesomeIcon.STAR_SOLID.create();
        star.addClassNames(LumoUtility.IconSize.SMALL);
        return star;
    }

    private static Component createHalfStar() {
        SvgIcon star = LineAwesomeIcon.STAR_HALF_SOLID.create();
        star.addClassNames(LumoUtility.IconSize.SMALL);
        return star;
    }

    private static Component createStarEmpty() {
        SvgIcon star = LineAwesomeIcon.STAR.create();
        star.addClassNames(LumoUtility.IconSize.SMALL);
        return star;
    }
}
