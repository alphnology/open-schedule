package com.alphnology.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Reusable empty-state panel for VirtualList, Grid, and any container that can
 * be empty. Shows an icon, a short title, a descriptive sentence, and an
 * optional call-to-action component.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * var empty = new EmptyStateComponent(
 *     VaadinIcon.PRESENTATION,
 *     "No sessions yet",
 *     "Create your first session to get started.",
 *     new Button("Add session", e -> openForm())
 * );
 *
 * // Toggle visibility in refreshList():
 * list.setVisible(!items.isEmpty());
 * empty.setVisible(items.isEmpty());
 * }</pre>
 */
public class EmptyStateComponent extends VerticalLayout {

    public EmptyStateComponent(VaadinIcon vaadinIcon, String title, String description) {
        this(vaadinIcon, title, description, null);
    }

    public EmptyStateComponent(VaadinIcon vaadinIcon, String title, String description, Component action) {
        Icon icon = vaadinIcon.create();
        icon.setSize("48px");
        icon.addClassNames(LumoUtility.TextColor.TERTIARY);

        Span titleSpan = new Span(title);
        titleSpan.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.TextColor.BODY
        );

        Span descSpan = new Span(description);
        descSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        descSpan.getStyle().set("text-align", "center").set("max-width", "280px");

        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        setSizeFull();
        setSpacing(false);
        addClassNames(LumoUtility.Gap.SMALL, LumoUtility.Padding.XLARGE);
        getStyle().set("min-height", "180px");

        add(icon, titleSpan, descSpan);
        if (action != null) {
            add(action);
        }
    }
}
