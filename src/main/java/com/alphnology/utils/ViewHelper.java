package com.alphnology.utils;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * @author me@fredpena.dev
 * @created 03/05/2025  - 16:38
 */

@Slf4j
public class ViewHelper {

    private ViewHelper() {

    }

    public static Scroller getScrollerVertical() {
        Scroller scroller = new Scroller();
        scroller.setSizeFull();
        scroller.addClassNames(LumoUtility.Padding.NONE, LumoUtility.Margin.NONE);
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.addClassNames(LumoUtility.AlignContent.START);
        return scroller;
    }

    public static VerticalLayout getSecondaryLayout(Scroller formScroller, Footer footer) {
        VerticalLayout layout = new VerticalLayout(formScroller, footer);
        layout.setSizeFull();
        layout.setPadding(false);
        layout.addClassNames(LumoUtility.BoxShadow.XSMALL, LumoUtility.BorderRadius.LARGE, "admin-form-panel");
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        return layout;
    }

    public static Header getSecondaryHeader(String titleHeader, String subTitleHeader) {
        final H2 title = new H2(titleHeader);
        final Span subTitle = new Span(subTitleHeader);

        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);
        title.getStyle().set("letter-spacing", "-0.02em").set("margin", "0").set("line-height", "1.2");

        subTitle.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        subTitle.getStyle().set("margin", "0");

        Div layout = new Div(title, subTitle);
        layout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.SMALL);

        Header header = new Header(layout);
        header.setWidthFull();
        header.addClassName("admin-view-header");
        header.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Border.BOTTOM, LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.Padding.End.MEDIUM, LumoUtility.Padding.Start.LARGE, LumoUtility.Padding.Vertical.MEDIUM);
        return header;
    }

}
