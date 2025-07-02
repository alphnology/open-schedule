package com.alphnology.views.favorite;

import com.alphnology.data.Session;
import com.alphnology.data.User;
import com.alphnology.services.SessionRatingService;
import com.alphnology.services.SessionService;
import com.alphnology.services.UserService;
import com.alphnology.views.login.LoginView;
import com.alphnology.views.schedule.ScheduleView;
import com.alphnology.views.schedule.ScheduleViewCard;
import com.alphnology.views.schedule.ScheduleViewDetails;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PageTitle("Favorite")
@Route("favorite")
@Menu(order = 3, icon = LineAwesomeIconUrl.HEART)
@AnonymousAllowed
public class FavoriteView extends Div {

    private ScheduleViewDetails scheduleViewDetails;

    public FavoriteView(SessionService sessionService, SessionRatingService sessionRatingService, UserService userService) {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, LumoUtility.Padding.MEDIUM);

        User currentUser = VaadinSession.getCurrent().getAttribute(User.class);

        if (currentUser == null) {
            add(createEmptyStateLayout(true));
            return;
        }

        this.scheduleViewDetails = new ScheduleViewDetails(sessionService, sessionRatingService, userService);

        buildLayout(currentUser, sessionService);
    }

    private void buildLayout(User currentUser, SessionService sessionService) {
        Div contentWrapper = new Div();
        contentWrapper.setMaxWidth("1280px");
        contentWrapper.setWidthFull();
        contentWrapper.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.LARGE);

        H2 title = new H2("My Favorite Sessions");
        title.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.TextColor.PRIMARY);

        contentWrapper.add(title);

        List<Session> favoriteSessions = currentUser.getFavoriteSessions() == null ? new ArrayList<>() : currentUser.getFavoriteSessions()
                .stream()
                .map(sessionService::get)
                .flatMap(Optional::stream)
                .toList();

        if (favoriteSessions.isEmpty()) {
            contentWrapper.add(createEmptyStateLayout(false));
        } else {
            Span notice = new Span("Notice: Agenda topics are subject to change.");
            notice.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            Div cardContainer = new Div();
            cardContainer.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexWrap.WRAP,
                    LumoUtility.Gap.MEDIUM,
                    LumoUtility.JustifyContent.CENTER
            );

            favoriteSessions.forEach(session -> {
                ScheduleViewCard card = new ScheduleViewCard(session, sessionService, scheduleViewDetails::showSession);
                card.getStyle().set("flex-basis", "480px");
                card.getStyle().set("flex-grow", "1");
                card.addClassNames(
                        "transition-card"
                );
                card.addClassNames(LumoUtility.BoxShadow.XSMALL, LumoUtility.BorderRadius.LARGE);
                cardContainer.add(card);
            });

            contentWrapper.add(notice, cardContainer);
        }

        add(contentWrapper);
    }


    private VerticalLayout createEmptyStateLayout(boolean needsLogin) {
        Icon icon = VaadinIcon.HEART_O.create();
        icon.setSize("4em");
        icon.addClassName(LumoUtility.TextColor.TERTIARY);

        Span message = new Span(needsLogin ? "Login to see your favorite sessions." : "You haven't added any favorite sessions yet.");
        message.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.LARGE);

        Button actionButton = needsLogin
                ? new Button("Login", e -> UI.getCurrent().navigate(LoginView.class))
                : new Button("Explore Schedule", e -> UI.getCurrent().navigate(ScheduleView.class));

        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        VerticalLayout emptyLayout = new VerticalLayout(icon, message, actionButton);
        emptyLayout.setAlignItems(VerticalLayout.Alignment.CENTER);
        emptyLayout.setSpacing(true);
        emptyLayout.addClassNames(LumoUtility.Padding.Vertical.LARGE, LumoUtility.Border.ALL, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.BorderRadius.LARGE);

        return emptyLayout;
    }
}