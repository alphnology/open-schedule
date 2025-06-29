package com.alphnology.views.login;

import com.alphnology.security.AuthenticatedUser;
import com.alphnology.utils.ImageUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;


@AnonymousAllowed
@PageTitle("Login - Open Schedule")
@Route(value = "login")
public class LoginView extends LoginOverlay implements BeforeEnterObserver, AfterNavigationObserver {

    private static final String LOGIN_IMAGE = "login-image";
    private transient AuthenticatedUser authenticatedUser;

    public LoginView(AuthenticatedUser authenticatedUser, @Value("${application.version}") String appVersion) {
        this.authenticatedUser = authenticatedUser;

        setAction(RouteUtil.getRoutePath(VaadinService.getCurrent().getContext(), getClass()));

        getElement().getStyle()
                .set("box-shadow", "0 4px 24px rgba(0,0,0,0.15)")
                .set("border-radius", "16px");
//        IntegerField code = new IntegerField("TFA");
//        code.getElement().setAttribute("name", "code");
//        getCustomFormArea().add(code);

        Div version = new Div(new Text("Version " + appVersion));
        version.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.TextColor.TERTIARY,
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.AlignSelf.CENTER
        );

        Image image = ImageUtils.getMainImage();
        image.addClassNames(LumoUtility.AlignSelf.CENTER, LumoUtility.Margin.Top.SMALL, LOGIN_IMAGE);

        Span headerLabel = new Span();
        Span detailsLabel = new Span();

        headerLabel.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.XXLARGE, LumoUtility.TextAlignment.CENTER);
        headerLabel.addClassNames(LumoUtility.TextColor.HEADER, LumoUtility.Margin.Vertical.LARGE);

        detailsLabel.addClassNames(LumoUtility.FontWeight.LIGHT, LumoUtility.FontSize.LARGE, LumoUtility.TextAlignment.CENTER);
        detailsLabel.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.SMALL, LumoUtility.TextColor.HEADER);

        Div section = new Div(image, headerLabel, detailsLabel);
        section.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Height.AUTO);
        section.addClassNames(LumoUtility.MaxWidth.FULL, LumoUtility.Padding.LARGE, LumoUtility.Padding.Bottom.NONE);

        setTitle(section);

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.setHeader(new LoginI18n.Header());

        i18n.setForm(new LoginI18n.Form());
        i18n.getForm().setUsername("Username");
        i18n.getForm().setPassword("Password");
        i18n.getForm().setSubmit("Sign In");
        i18n.getForm().setForgotPassword("Forgot password?");

        LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
        errorMessage.setTitle("Incorrect username or password");
        errorMessage.setMessage("Try again or contact the administrator.");
        i18n.setErrorMessage(errorMessage);

        headerLabel.setText("Welcome back!");
        detailsLabel.setText("Please enter your account details");

        setI18n(i18n);
//

        Span accountLabel = new Span();

        Anchor singUpLink = createAnchor("sign-up");
        singUpLink.addClassNames(LumoUtility.TextColor.HEADER);
        singUpLink.setText("Sign Up");

        HorizontalLayout accountLayout = new HorizontalLayout(accountLabel, singUpLink);
        accountLayout.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Margin.Vertical.LARGE);

        accountLabel.getStyle().setMarginInlineStart("0").setMarginInlineEnd("0").set("margin-block-start", "1em").set("margin-block-end", "1em");
        accountLabel.addClassNames(LumoUtility.LineHeight.SMALL, LumoUtility.Margin.Bottom.NONE, LumoUtility.Display.BLOCK);
        accountLabel.setText("Don`t have an account?");

        Anchor homeLink = createAnchor("");
        homeLink.setText("Go to main page");
        homeLink.getStyle().setFontWeight("bold").setColor("var(--lumo-dark-primary-color)");
        homeLink.addClassNames(LumoUtility.AlignItems.CENTER);

        Icon icon = new Icon(VaadinIcon.HOME);
        icon.getStyle().setFontWeight("bold").setColor("var(--lumo-dark-primary-color)");

        HorizontalLayout backLayout = new HorizontalLayout(icon, homeLink);
        backLayout.addClassNames(LumoUtility.FlexWrap.WRAP,
                LumoUtility.Margin.Vertical.LARGE,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.SMALL
        );


        VerticalLayout footerLayout = new VerticalLayout(accountLayout, backLayout, new Hr(), version);
        footerLayout.addClassNames(LumoUtility.Padding.NONE, LumoUtility.Margin.NONE);
        footerLayout.addClassNames(LumoUtility.Gap.SMALL);

        getFooter().add(footerLayout);

        setForgotPasswordButtonVisible(true);

        setOpened(true);

        addForgotPasswordListener(forgotPasswordEvent -> UI.getCurrent().navigate(ForgotPasswordView.class));
    }


    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        if (authenticatedUser.get().isPresent()) {
            // Already logged in
            setOpened(false);
            event.forwardTo("");
        }

        setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }


    private Anchor createAnchor(String href) {
        Anchor anchor = new Anchor();
        anchor.setHref(href);

        anchor.getStyle().setTextDecoration("underline").set("margin-block-start", "1em").set("margin-block-end", "1em");
        anchor.addClassNames(LumoUtility.LineHeight.SMALL, LumoUtility.Margin.Bottom.NONE, LumoUtility.Display.BLOCK);
        anchor.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

        return anchor;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
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

