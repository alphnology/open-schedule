package com.alphnology.views.login;

import com.alphnology.data.User;
import com.alphnology.data.enums.Role;
import com.alphnology.services.UserService;
import com.alphnology.services.email.EmailOpenScheduleService;
import com.alphnology.utils.ImageUtils;
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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static com.alphnology.utils.EmailHelper.sentEmailChangePassword;


/**
 * @author me@fredpena.dev
 * @created 04/05/2025  - 02:28
 */
@PageTitle("Sign Up")
@Route(value = "sign-up", autoLayout = false)
@AnonymousAllowed
@Slf4j
@Uses(Icon.class)
public class SignUpView extends VerticalLayout  {
    private static final String REQUIRED = "This field cannot be null";

    private final TextField name = new TextField("Full Name");
    private final EmailField email = new EmailField("Email");
    private final PasswordField password = new PasswordField("Password");
    private final PasswordField confirmPassword = new PasswordField("Confirm Password");
    private final Button signUpButton = new Button("Sign Up");
    private final Button loginRedirect = new Button("Back to Login");

    private final String patternPassword;

    public SignUpView(UserService userService, PasswordEncoder passwordEncoder,
                      EmailOpenScheduleService emailService,
                      @Value("${application.pattern.password:unknown}") String patternPassword,
                      @Value("${application.pattern.email:unknown}") String patternEmail) {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        this.patternPassword = patternPassword;

        name.setWidth(300, Unit.PIXELS);
        email.setWidth(300, Unit.PIXELS);
        password.setWidth(300, Unit.PIXELS);
        confirmPassword.setWidth(300, Unit.PIXELS);
        signUpButton.setWidth(300, Unit.PIXELS);
        loginRedirect.setWidth(300, Unit.PIXELS);


        signUpButton.setEnabled(false);
        signUpButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        loginRedirect.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Image logo = ImageUtils.getMainImage();
        logo.setWidth(100, Unit.PIXELS);

        H2 title = new H2("Create your account");
        title.getStyle().set("color", "#17222F");

        Span subtitle = new Span("Enter your details to get access.");
        subtitle.getStyle().set("color", "#1B3A4B");

        Binder<UserModel> binder = new Binder<>(UserModel.class);
        binder.forField(name).asRequired("Name is required").bind(UserModel::getName, UserModel::setName);

        binder.forField(email)
                .asRequired("Email is required")
                .withValidator(new EmailValidator("Enter a valid email"))
                .withValidator(v -> !email.getValue().isEmpty(), REQUIRED)
                .withValidator(value -> email.getValue().matches(patternEmail), "Please enter a valid email address. The format must be: text followed by an '@' symbol, a domain, and an extension of at least two characters (e.g., info@openschedule.com).")
                .withValidator(v -> !StringUtils.hasText(email.getValue()) || userService.isThisUserNotAlreadyRegistered(v), "This user is already registered")
                .bind(UserModel::getEmail, UserModel::setEmail);

        String messageValidator = "Password must be at least 5 characters and include one letter and one special character (!@#$%&*()_+.)";

        binder.forField(password)
                .asRequired(REQUIRED)
                .withValidator(passwordStructureValidator(messageValidator))
                .withValidator(v -> !StringUtils.hasText(confirmPassword.getValue()) || v.equals(confirmPassword.getValue()),
                        "Passwords do not match")
                .bind(UserModel::getPassword, UserModel::setPassword);

        binder.forField(confirmPassword)
                .asRequired(REQUIRED)
                .withValidator(passwordStructureValidator(messageValidator))
                .withValidator(v -> !StringUtils.hasText(password.getValue()) || v.equals(password.getValue()),
                        "Passwords do not match")
                .bind(u -> "", (u, s) -> {
                });

        name.addValueChangeListener(e -> binder.validate());
        email.addValueChangeListener(e -> binder.validate());
        password.addValueChangeListener(e -> binder.validate());
        confirmPassword.addValueChangeListener(e -> binder.validate());

        binder.addStatusChangeListener(e -> signUpButton.setEnabled(binder.isValid()));

        signUpButton.addClickListener(e -> {
            if (binder.validate().isOk()) {
                UserModel model = new UserModel();
                binder.writeBeanIfValid(model);

                User user = new User();
                user.setName(model.getName());
                user.setUsername(model.getEmail());
                user.setPassword(passwordEncoder.encode(model.getPassword()));
                user.setRoles(Role.USER);
                userService.save(user);

                sentEmailChangePassword(emailService, user, "welcome-signup", "Open Schedule - Sign up successfully", "");

                UI.getCurrent().navigate(LoginView.class, new QueryParameters(
                        Map.of("signup", List.of("true"))
                ));
            }
        });

        loginRedirect.addClickListener(e -> UI.getCurrent().navigate("login"));

        VerticalLayout formLayout = new VerticalLayout(logo, title, subtitle, name, email, password, confirmPassword, signUpButton, loginRedirect);
        formLayout.setAlignItems(Alignment.CENTER);
        formLayout.setSpacing(true);
        formLayout.getStyle()
                .set("background", "#FFFFFF")
                .set("padding", "40px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 0 12px rgba(23, 34, 47, 0.1)");

        add(formLayout);
    }

    private Validator<String> passwordStructureValidator(String message) {
        return (value, context) -> value != null && value.matches(patternPassword)
                ? ValidationResult.ok()
                : ValidationResult.error(message);
    }

    @Getter
    @Setter
    public static class UserModel {
        private String name;
        private String email;
        private String password;
    }
}