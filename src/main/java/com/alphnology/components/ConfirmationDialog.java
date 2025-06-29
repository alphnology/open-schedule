package com.alphnology.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.function.Consumer;

/**
 * @author me@fredpena.dev
 * @created 15/11/2024  - 13:22
 */
public final class ConfirmationDialog {

    private ConfirmationDialog() {
    }

    public static void confirmation(Consumer<ConfirmDialog.ConfirmEvent> action, Component... components) {
        ConfirmDialog dialog = new ConfirmationDialogBuilder()
                .withHeader("Confirmation of Changes")
                .withText("There are unsaved changes. Do you want to discard them or continue?")
                .withComponents(components)
                .withCancelText("Discard")
                .withConfirmText("Continue")
                .withIcon(VaadinIcon.QUESTION_CIRCLE_O)
                .onConfirm(action).build();
        dialog.open();
    }

    public static void delete(Consumer<ConfirmDialog.ConfirmEvent> action, Component... components) {
        ConfirmDialog dialog = new ConfirmationDialogBuilder()
                .withHeader("Deletion Confirmation")
                .withText("You are deleting a record. Do you want to discard it or continue?")
                .withComponents(components)
                .withCancelText("Discard")
                .withConfirmText("Continue")
                .withIcon(VaadinIcon.QUESTION_CIRCLE_O)
                .onConfirm(action).build();
        dialog.open();
    }

    public static void cancel(Consumer<ConfirmDialog.ConfirmEvent> action, Component... components) {

        ConfirmDialog dialog = new ConfirmationDialogBuilder()
                .withHeader("Cancellation Confirmation")
                .withText("You are canceling a record. Do you want to discard it or continue?")
                .withComponents(components)
                .withConfirmText("Continue")
                .withCancelText("Discard")
                .withIcon(VaadinIcon.QUESTION_CIRCLE_O)
                .onConfirm(action).build();
        dialog.open();
    }

}
