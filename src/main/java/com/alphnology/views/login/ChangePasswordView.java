package com.alphnology.views.login;

import com.alphnology.data.User;
import com.alphnology.services.UserService;
import com.alphnology.utils.ImageUtils;
import com.alphnology.utils.UserHelper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@PageTitle("Change password")
@Route(value = "change-password", autoLayout = false)
@PermitAll
@Slf4j
@Uses(Icon.class)
public class ChangePasswordView extends VerticalLayout implements BeforeEnterObserver {

    private static final String REQUIRED = "This field cannot be null";


    private final TextField username = new TextField("Email");
    private final TextField name = new TextField("Name");
    private final PasswordField currentPassword = new PasswordField("Current Password");
    private final PasswordField newPassword = new PasswordField("New Password");
    private final PasswordField confirmPassword = new PasswordField("Confirm New Password");

    private final Button changeButton = new Button("Change Password");
    private final Button backToLoginButton = new Button("Back to Login");
    private final Binder<User> binder = new Binder<>(User.class);

    private User user;
    private final String patternPassword;

    public ChangePasswordView(UserService userService,
                              PasswordEncoder passwordEncoder,
                              AuthenticationContext authenticationContext,
                              @Value("${application.pattern.password:unknown}") String patternPassword) {

        this.patternPassword = patternPassword;

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        Image logo = ImageUtils.getMainImage();
        logo.setWidth(300, Unit.PIXELS);

        H2 title = new H2("Change Your Password");
        title.getStyle().set("color", "#17222F");

        Span instructions = new Span("Enter your current password and the new password you wish to use.");
        instructions.getStyle().set("color", "#1B3A4B");

        username.setEnabled(false);
        name.setEnabled(false);

        username.setWidth(300, Unit.PIXELS);
        name.setWidth(300, Unit.PIXELS);
        currentPassword.setWidth(300, Unit.PIXELS);
        newPassword.setWidth(300, Unit.PIXELS);
        confirmPassword.setWidth(300, Unit.PIXELS);
        changeButton.setWidth(300, Unit.PIXELS);
        backToLoginButton.setWidth(300, Unit.PIXELS);
        logo.setWidth(300, Unit.PIXELS);

        changeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        backToLoginButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        backToLoginButton.addClickListener(e -> {
            authenticationContext.logout();
        });

        VerticalLayout formLayout = new VerticalLayout(
                logo, title, instructions,
                username, name,
                currentPassword, newPassword, confirmPassword,
                changeButton, backToLoginButton
        );

        formLayout.setAlignItems(Alignment.CENTER);
        formLayout.setSpacing(true);
        formLayout.getStyle()
                .set("background", "#FFFFFF")
                .set("padding", "40px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 0 12px rgba(23, 34, 47, 0.1)");

        add(formLayout);

        String messageValidator = "Password must be at least 5 characters and include one letter and one special character (!@#$%&*()_+.)";

        UserHelper.validatedPasswordField(currentPassword, patternPassword);
        UserHelper.validatedPasswordField(newPassword, patternPassword);
        UserHelper.validatedPasswordField(confirmPassword, patternPassword);

        binder.forField(currentPassword)
                .asRequired(REQUIRED)
                .withValidator(passwordStructureValidator(messageValidator))
                .withValidator(v -> StringUtils.hasText(v) && passwordEncoder.matches(v, user.getPassword()),
                        "Current password is incorrect.")
                .bind(u -> "", (u, s) -> {
                });

        binder.forField(newPassword)
                .asRequired(REQUIRED)
                .withValidator(passwordStructureValidator(messageValidator))
                .withValidator(v -> !StringUtils.hasText(confirmPassword.getValue()) || v.equals(confirmPassword.getValue()),
                        "Passwords do not match")
                .bind(u -> "", (u, s) -> {
                });

        binder.forField(confirmPassword)
                .asRequired(REQUIRED)
                .withValidator(passwordStructureValidator(messageValidator))
                .withValidator(v -> !StringUtils.hasText(newPassword.getValue()) || v.equals(newPassword.getValue()),
                        "Passwords do not match")
                .bind(u -> "", (u, s) -> {
                });

        confirmPassword.addValueChangeListener(e -> binder.validate());
        newPassword.addValueChangeListener(e -> binder.validate());

        changeButton.setEnabled(false);
        binder.addStatusChangeListener(event -> changeButton.setEnabled(binder.isValid()));

        changeButton.addClickListener(event -> {
            if (user != null && binder.validate().isOk()) {

                binder.writeBeanIfValid(user);

                user.setPassword(passwordEncoder.encode(newPassword.getValue()));
                user.setOneLogPwd(false);
                userService.save(user);

                authenticationContext.logout();
            }
        });
    }

    private Validator<String> passwordStructureValidator(String message) {
        return (value, context) -> value != null && value.matches(patternPassword)
                ? ValidationResult.ok()
                : ValidationResult.error(message);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        user = VaadinSession.getCurrent().getAttribute(User.class);
        if (user == null || !user.isOneLogPwd()) {
            event.forwardTo("login");
            return;
        }
        username.setValue(user.getUsername());
        name.setValue(user.getName());
    }
}
