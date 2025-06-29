package com.alphnology.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.function.Consumer;

/**
 * @author me@fredpena.dev
 * @created 02/12/2024  - 15:29
 */
public class ConfirmationDialogBuilder {

    private VaadinIcon vaadinIcon;
    private String header;
    private String text;
    private final VerticalLayout textLayout = new VerticalLayout();
    private final ConfirmDialog confirmDialog;
    private Consumer<ConfirmDialog.ConfirmEvent> confirmAction = confirmEvent -> {
    };
    private Consumer<ConfirmDialog.RejectEvent> rejectAction = rejectEvent -> {
    };
    private Consumer<ConfirmDialog.CancelEvent> cancelAction = cancelEvent -> {
    };

    public ConfirmationDialogBuilder() {
        this.confirmDialog = new ConfirmDialog();
    }

    public ConfirmationDialogBuilder withHeader(String header) {
        this.header = header;
        return this;
    }

    public ConfirmationDialogBuilder withText(String text) {
        this.text = text;
        return this;
    }

    public ConfirmationDialogBuilder withComponents(Component... components) {
        this.textLayout.add(components);
        return this;
    }

    public ConfirmationDialogBuilder withConfirmText(String confirmText) {
        confirmDialog.setConfirmText(confirmText);
        confirmDialog.setConfirmButtonTheme("tertiary primary");
        return this;
    }

    public ConfirmationDialogBuilder withRejectText(String rejectText) {
        confirmDialog.setRejectText(rejectText);
        confirmDialog.setRejectable(true);
        confirmDialog.setRejectButtonTheme("tertiary error");
        return this;
    }

    public ConfirmationDialogBuilder withCancelText(String rejectText) {
        confirmDialog.setCancelText(rejectText);
        confirmDialog.setCancelable(true);
        confirmDialog.setCancelButtonTheme("error primary");
        return this;
    }

    public ConfirmationDialogBuilder onConfirm(Consumer<ConfirmDialog.ConfirmEvent> action) {
        this.confirmAction = action;
        return this;
    }

    public ConfirmationDialogBuilder onReject(Consumer<ConfirmDialog.RejectEvent> action) {
        this.rejectAction = action;
        return this;
    }

    public ConfirmationDialogBuilder onCancel(Consumer<ConfirmDialog.CancelEvent> action) {
        this.cancelAction = action;
        return this;
    }


    public ConfirmationDialogBuilder withIcon(VaadinIcon icon) {
        this.vaadinIcon = icon;
        return this;
    }


    public ConfirmDialog build() {
        if (vaadinIcon != null && header != null && !header.isEmpty()) {
            Icon icon = this.vaadinIcon.create();
            icon.setSize("var(--lumo-size-xl)");
            VerticalLayout headerLayout = new VerticalLayout(icon, new H3(header));
            headerLayout.setPadding(false);
            headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            confirmDialog.setHeader(headerLayout);
        }
        if (vaadinIcon == null && header != null && !header.isEmpty()) {
            confirmDialog.setHeader(header);
        }

        textLayout.setSizeFull();
        textLayout.setPadding(false);

        VerticalLayout layout = new VerticalLayout(new Text(text), textLayout);
        layout.setPadding(false);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        confirmDialog.setText(layout);

        confirmDialog.addConfirmListener(event -> confirmAction.accept(event));
        confirmDialog.addRejectListener(event -> rejectAction.accept(event));
        confirmDialog.addCancelListener(event -> cancelAction.accept(event));
        return confirmDialog;
    }
}