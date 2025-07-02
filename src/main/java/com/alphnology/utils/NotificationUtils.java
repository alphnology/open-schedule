package com.alphnology.utils;

import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.util.StringUtils;

public final class NotificationUtils {

    private static final int DURATION = 5_000;

    private NotificationUtils() {
    }

    private static void show(NotificationVariant themeVariant, VaadinIcon vaadinIcon, String msg) {
        final Notification notification = new Notification();
        notification.addThemeVariants(themeVariant);
        notification.setDuration(DURATION);
        notification.setPosition(Notification.Position.TOP_END);

        final Div text = new Div(new Text(msg));
        text.addClassNames(LumoUtility.Margin.End.AUTO);

        final Button closeButton = new Button(new Icon("lumo", "cross"), event -> notification.close());
        closeButton.addClassNames(LumoUtility.Width.AUTO);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        closeButton.getElement().setAttribute("aria-label", "Close");
        closeButton.getStyle().setMargin("0 0 0 var(--lumo-space-l)");

        String size = "var(--lumo-size-m)";
        Icon icon = vaadinIcon.create();
        icon.getStyle().setMaxWidth(size);
        icon.getStyle().setMinWidth(size);
        icon.getStyle().setMaxHeight(size);
        icon.getStyle().setMinHeight(size);

        final HorizontalLayout layout = new HorizontalLayout(icon, text, closeButton);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setMaxWidth("500px");
        layout.setMinWidth("500px");

        notification.add(layout);
        notification.open();
    }

    public static void error(ValidationException validationException) {
        error(null, validationException);
    }

    public static void error(String msg, ValidationException validationException) {

        if (msg != null) {
            show(NotificationVariant.LUMO_ERROR, VaadinIcon.CLOSE_CIRCLE_O, msg);
        }

        validationException.getFieldValidationErrors()
                .forEach(err -> err.getMessage()
                        .ifPresent(msg2 -> {
                            String label = ((HasLabel) err.getBinding().getField()).getLabel();
                            show(NotificationVariant.LUMO_ERROR, VaadinIcon.CLOSE_CIRCLE_O, (StringUtils.hasText(label) ? "(%s) -> %s".formatted(label, msg2) : msg2));
                        }));
    }

    public static void error() {
        error("There was an error in the transaction.");
    }

    public static void error(String msg) {
        show(NotificationVariant.LUMO_ERROR, VaadinIcon.CLOSE_CIRCLE_O, msg);
    }


    public static void success() {
        success("The transaction was successful.");
    }

    public static void success(String msg) {
        show(NotificationVariant.LUMO_SUCCESS, VaadinIcon.CHECK_CIRCLE_O, msg);
    }

    public static void warning(String msg) {
        show(NotificationVariant.LUMO_WARNING, VaadinIcon.EXCLAMATION_CIRCLE_O, msg);
    }

    public static void info(String msg) {
        show(NotificationVariant.LUMO_PRIMARY, VaadinIcon.INFO_CIRCLE_O, msg);
    }

    public static void question(String msg) {
        show(NotificationVariant.LUMO_CONTRAST, VaadinIcon.QUESTION_CIRCLE_O, msg);
    }
}
