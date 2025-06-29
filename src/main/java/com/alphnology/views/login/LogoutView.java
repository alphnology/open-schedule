package com.alphnology.views.login;


import com.alphnology.data.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

@PageTitle("Logout")
@Route(value = "logout")
@AnonymousAllowed
public class LogoutView extends Div implements AfterNavigationObserver {

    private final transient AuthenticationContext authenticationContext;

    public LogoutView(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        UI.getCurrent().access(() -> {
            VaadinSession.getCurrent().setAttribute(User.class, null);
            authenticationContext.logout();
        });
    }
}