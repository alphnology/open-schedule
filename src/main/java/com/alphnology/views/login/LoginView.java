package com.alphnology.views.login;

import com.alphnology.security.AuthenticatedUser;
import com.alphnology.utils.ImageUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.*;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;


@AnonymousAllowed
@PageTitle("Login - Open Schedule")
@Route(value = "login")
public class LoginView extends LoginOverlay implements BeforeEnterObserver, AfterNavigationObserver {

    private transient AuthenticatedUser authenticatedUser;

    public LoginView(AuthenticatedUser authenticatedUser, @Value("${application.version}") String appVersion) {
        this.authenticatedUser = authenticatedUser;

        setAction(RouteUtil.getRoutePath(VaadinService.getCurrent().getContext(), getClass()));

        // ── Brand panel (left — dark gradient) ──────────────────────────────
        Image brandLogo = ImageUtils.getMainImage();
        brandLogo.addClassNames("login-brand-logo");

        Span brandName = new Span("Open Schedule");
        brandName.addClassNames("login-brand-name");

        Span brandTagline = new Span("Discover sessions, connect with speakers,\nand build your perfect agenda.");
        brandTagline.addClassNames("login-brand-tagline");
        brandTagline.getStyle().set("white-space", "pre-line");

        Div brandTags = new Div();
        brandTags.addClassNames("login-brand-tags");
        for (String feature : new String[]{"Sessions", "Speakers", "Schedule", "Favorites", "Ratings"}) {
            Span pill = new Span(feature);
            pill.addClassNames("login-brand-tag");
            brandTags.add(pill);
        }

        Div brandContent = new Div(brandLogo, brandName, brandTagline, brandTags);
        brandContent.addClassNames("login-brand-content");
        setTitle(brandContent);

        // ── Form i18n (labels + heading in the right card) ──────────────────
        LoginI18n i18n = LoginI18n.createDefault();

        LoginI18n.Header formHeader = new LoginI18n.Header();
        formHeader.setTitle("Welcome back");
        formHeader.setDescription("Sign in to your account to continue");
        i18n.setHeader(formHeader);

        LoginI18n.Form form = new LoginI18n.Form();
        form.setUsername("Username");
        form.setPassword("Password");
        form.setSubmit("Sign In");
        form.setForgotPassword("Forgot password?");
        i18n.setForm(form);

        LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
        errorMessage.setTitle("Incorrect username or password");
        errorMessage.setMessage("Try again or contact the administrator.");
        i18n.setErrorMessage(errorMessage);

        setI18n(i18n);
        setForgotPasswordButtonVisible(true);
        addForgotPasswordListener(e -> UI.getCurrent().navigate(ForgotPasswordView.class));

        // ── Footer (right card, below the form) ─────────────────────────────

        // Sign up row
        Span noAccount = new Span("Don't have an account?");
        noAccount.addClassNames("login-footer-text");

        Anchor signUpLink = new Anchor("sign-up", "Sign Up");
        signUpLink.addClassNames("login-footer-link");

        Div signUpRow = new Div(noAccount, signUpLink);
        signUpRow.addClassNames("login-footer-row");

        // Back to home
        Icon arrowIcon = VaadinIcon.ARROW_LEFT.create();
        arrowIcon.addClassNames("login-back-icon");

        Span backText = new Span("Back to Schedule");
        backText.addClassNames("login-back-text");

        Div backHome = new Div(arrowIcon, backText);
        backHome.addClassNames("login-back-home");
        backHome.addClickListener(e -> UI.getCurrent().navigate(""));

        // Dividers + version
        Hr divider1 = new Hr();
        divider1.addClassNames("login-footer-divider");

        Hr divider2 = new Hr();
        divider2.addClassNames("login-footer-divider");

        Span version = new Span("Version " + appVersion);
        version.addClassNames("login-version");

        Div footerWrapper = new Div(signUpRow, divider1, backHome, divider2, version);
        footerWrapper.addClassNames("login-footer-wrapper");
        getFooter().add(footerWrapper);

        setOpened(true);
    }


    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticatedUser.get().isPresent()) {
            setOpened(false);
            event.forwardTo("");
        }
        setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }


    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String script = "document.querySelectorAll('vaadin-dialog-overlay').forEach(overlay => overlay.close());";
        getUI().ifPresent(ui -> ui.getPage().executeJs(script));

        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();

        if (params.containsKey("reset") && "true".equalsIgnoreCase(params.get("reset").getFirst())) {
            NotificationUtils.success("A temporary password has been sent to your email.");
        }

        if (params.containsKey("change") && "true".equalsIgnoreCase(params.get("change").getFirst())) {
            NotificationUtils.success("Password changed successfully.");
        }

        if (params.containsKey("signup") && "true".equalsIgnoreCase(params.get("signup").getFirst())) {
            NotificationUtils.success("Sign up was successful, you can now log in.");
        }
    }
}
