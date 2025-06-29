package com.alphnology.utils;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.extern.slf4j.Slf4j;

/**
 * @author me@fredpena.dev
 * @created 03/05/2025  - 16:38
 */

@Slf4j
public class UserHelper {

    private UserHelper() {

    }

    public static void validatedPasswordField(PasswordField password, String patternPassword) {
        password.setRevealButtonVisible(true);
        password.setClearButtonVisible(true);
        password.setMaxLength(50);
        password.setRequiredIndicatorVisible(true);

        Icon icon = VaadinIcon.CHECK.create();
        icon.setVisible(false);
        icon.getStyle().setColor("var(--lumo-success-color)");
        password.setSuffixComponent(icon);

        Span strengthText = new Span();
        Div strength = new Div();
        strength.add(strengthText);

        strength.add(new Text("Password Security: "), strengthText);
        password.setHelperComponent(strength);
        password.setValueChangeMode(ValueChangeMode.EAGER);
        password.addValueChangeListener(e -> updateHelper(e.getValue(), patternPassword, strengthText, icon));

    }

    private static void updateHelper(String password, String patternPassword, Span passwordStrengthText, Icon checkIcon) {
        if (password.length() > 9 && password.matches(patternPassword)) {
            passwordStrengthText.setText("strong");
            passwordStrengthText.getStyle().setColor("var(--lumo-success-color)");
            checkIcon.setVisible(true);
        } else if (password.length() >= 5 && password.matches(patternPassword)) {
            passwordStrengthText.setText("moderate");
            passwordStrengthText.getStyle().setColor("var(--lumo-warning-color)");
            checkIcon.setVisible(false);
        } else if (password.isEmpty()) {
            passwordStrengthText.setText(null);
            checkIcon.setVisible(false);
        } else {
            passwordStrengthText.setText("weak");
            passwordStrengthText.getStyle().set("color", "var(--lumo-error-color)");
            checkIcon.setVisible(false);
        }
    }
}
