package com.alphnology.views.login;

import com.alphnology.data.User;
import com.alphnology.services.UserService;
import com.alphnology.services.email.EmailOpenScheduleService;
import com.alphnology.utils.ImageUtils;
import com.alphnology.utils.NotificationUtils;
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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alphnology.utils.EmailHelper.sentEmailChangePassword;
import static com.alphnology.utils.RegexGenerator.generateMatchingString;

@PageTitle("Forgot password")
@Route(value = "forgot-password", autoLayout = false)
@AnonymousAllowed
@Slf4j
@Uses(Icon.class)
public class ForgotPasswordView extends VerticalLayout {

    private final EmailField usernameField = new EmailField("Email");
    private final Button sendResetButton = new Button("Send Temporary Password");
    private final Button backToLoginButton = new Button("Back to Login");
    private final Binder<ForgotPasswordModel> binder = new Binder<>(ForgotPasswordModel.class);

    private final transient UserService userService;

    public ForgotPasswordView(UserService userService, EmailOpenScheduleService emailOpenScheduleService, PasswordEncoder passwordEncoder) {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setPadding(true);
        setSpacing(true);

        this.userService = userService;

        Image logo = ImageUtils.getMainImage();
        logo.setHeight("100px");

        H2 title = new H2("Forgot Your Password?");
        title.getStyle().set("color", "#17222F");

        Span instructions = new Span("Enter your email address. We'll send you a temporary password if your account exists.");
        instructions.getStyle().set("color", "#1B3A4B");

        usernameField.setPlaceholder("you@open-schedule.com");
        usernameField.setClearButtonVisible(true);
        usernameField.setWidth(300, Unit.PIXELS);

        sendResetButton.setWidth(300, Unit.PIXELS);
        sendResetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        backToLoginButton.setWidth(300, Unit.PIXELS);
        backToLoginButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);


        binder.forField(usernameField)
                .asRequired("Email is required")
                .withValidator(new EmailValidator("Please enter a valid email"))
                .bind(ForgotPasswordModel::getEmail, ForgotPasswordModel::setEmail);

        sendResetButton.setEnabled(false);
        binder.addStatusChangeListener(event -> sendResetButton.setEnabled(binder.isValid()));

        sendResetButton.addClickListener(e -> {
            if (binder.validate().isOk()) {
                ForgotPasswordModel model = new ForgotPasswordModel();
                binder.writeBeanIfValid(model);

                validateAndSendTemporaryPassword(model.getEmail())
                        .ifPresentOrElse(selected -> {

                            String newPassword = generateMatchingString(6);
                            selected.setOneLogPwd(true);
                            selected.setPassword(passwordEncoder.encode(newPassword));

                            userService.save(selected);

                            sentEmailChangePassword(emailOpenScheduleService, selected, "forgot-password", "Open Schedule - Forgot password", newPassword);

                            UI.getCurrent().navigate(LoginView.class, new QueryParameters(
                                    Map.of("reset", List.of("true"))
                            ));
                        }, () -> NotificationUtils.error("No account found with that email."));
            }
        });

        backToLoginButton.addClickListener(e -> UI.getCurrent().navigate(LoginView.class));


        VerticalLayout formLayout = new VerticalLayout(logo, title, instructions, usernameField, sendResetButton, backToLoginButton);

        formLayout.setAlignItems(Alignment.CENTER);
        formLayout.setSpacing(true);
        formLayout.getStyle()
                .set("background", "#FFFFFF")
                .set("padding", "40px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 0 12px rgba(23, 34, 47, 0.1)");

        add(formLayout);
    }

    private Optional<User> validateAndSendTemporaryPassword(String email) {
        return userService.findByUsername(email);
    }

    @Setter
    @Getter
    public static class ForgotPasswordModel {
        private String email;

    }

}
