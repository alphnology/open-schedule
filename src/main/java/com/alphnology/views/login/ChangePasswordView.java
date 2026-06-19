package com.alphnology.views.login;

import com.alphnology.data.User;
import com.alphnology.security.AuthenticatedUser;
import com.alphnology.services.UserService;
import com.alphnology.utils.ImageUtils;
import com.alphnology.utils.UserHelper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

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
    private final Button cancelButton = new Button("Cancel");
    private final Paragraph forcedNotice = new Paragraph(
            "For your security, you must set a new password before continuing.");

    private final Binder<User> binder = new Binder<>(User.class);

    private User user;
    private final String patternPassword;
    private final transient AuthenticatedUser authenticatedUser;

    public ChangePasswordView(UserService userService,
                              PasswordEncoder passwordEncoder,
                              AuthenticatedUser authenticatedUser,
                              @Value("${application.pattern.password:unknown}") String patternPassword) {
        this.patternPassword = patternPassword;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setPadding(false);
        addClassName("auth-page-bg");

        Image logo = ImageUtils.getMainImage();
        logo.addClassName("auth-logo");

        H2 title = new H2("Change Your Password");
        title.addClassName("auth-title");

        Span subtitle = new Span("Enter your current password and choose a new one.");
        subtitle.addClassName("auth-subtitle");

        forcedNotice.addClassName("auth-forced-notice");
        forcedNotice.setVisible(false);

        username.setEnabled(false);
        username.setWidthFull();

        name.setEnabled(false);
        name.setWidthFull();

        currentPassword.setWidthFull();
        newPassword.setWidthFull();
        confirmPassword.setWidthFull();

        changeButton.setWidthFull();
        changeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        cancelButton.setWidthFull();
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        cancelButton.setVisible(false);
        cancelButton.addClickListener(e -> {
            if (user != null && user.isOneLogPwd()) {
                UI.getCurrent().getPage().executeJs("window.location.replace('/logout');");
            } else {
                UI.getCurrent().navigate("");
            }
        });

        UserHelper.validatedPasswordField(currentPassword, patternPassword);
        UserHelper.validatedPasswordField(newPassword, patternPassword);
        UserHelper.validatedPasswordField(confirmPassword, patternPassword);

        String messageValidator = "Password must be at least 5 characters and include one letter and one special character (!@#$%&*()_+.)";

        binder.forField(currentPassword)
                .asRequired(REQUIRED)
                .withValidator(passwordStructureValidator(messageValidator))
                .withValidator(v -> user != null && StringUtils.hasText(v) && passwordEncoder.matches(v, user.getPassword()),
                        "Current password is incorrect.")
                .bind(u -> "", (u, s) -> {});

        binder.forField(newPassword)
                .asRequired(REQUIRED)
                .withValidator(passwordStructureValidator(messageValidator))
                .withValidator(v -> !StringUtils.hasText(confirmPassword.getValue()) || v.equals(confirmPassword.getValue()),
                        "Passwords do not match")
                .bind(u -> "", (u, s) -> {});

        binder.forField(confirmPassword)
                .asRequired(REQUIRED)
                .withValidator(passwordStructureValidator(messageValidator))
                .withValidator(v -> !StringUtils.hasText(newPassword.getValue()) || v.equals(newPassword.getValue()),
                        "Passwords do not match")
                .bind(u -> "", (u, s) -> {});

        confirmPassword.addValueChangeListener(e -> binder.validate());
        newPassword.addValueChangeListener(e -> binder.validate());

        changeButton.setEnabled(false);
        binder.addStatusChangeListener(event -> changeButton.setEnabled(binder.isValid()));

        changeButton.addClickListener(event -> {
            if (user != null && binder.validate().isOk()) {
                user.setPassword(Objects.requireNonNull(passwordEncoder.encode(newPassword.getValue())));
                user.setOneLogPwd(false);
                userService.save(user);
                UI.getCurrent().getPage().executeJs(
                        "sessionStorage.setItem('os_pwd_changed','1'); window.location.replace('/logout');");
            }
        });

        VerticalLayout card = new VerticalLayout(
                logo, title, subtitle, forcedNotice,
                username, name,
                currentPassword, newPassword, confirmPassword,
                changeButton, cancelButton
        );
        card.setAlignItems(Alignment.CENTER);
        card.setSpacing(true);
        card.setPadding(false);
        card.addClassName("auth-card");

        add(card);
    }

    private Validator<String> passwordStructureValidator(String message) {
        return (value, context) -> value != null && value.matches(patternPassword)
                ? ValidationResult.ok()
                : ValidationResult.error(message);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isEmpty()) {
            event.forwardTo("login");
            return;
        }
        user = maybeUser.get();
        username.setValue(user.getUsername());
        name.setValue(user.getName());

        boolean isForced = user.isOneLogPwd();
        forcedNotice.setVisible(isForced);
        cancelButton.setText(isForced ? "Log out" : "Cancel");
        cancelButton.setVisible(true);
    }
}
