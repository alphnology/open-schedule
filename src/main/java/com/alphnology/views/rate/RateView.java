package com.alphnology.views.rate;

import com.alphnology.data.Session;
import com.alphnology.data.SessionRating;
import com.alphnology.data.User;
import com.alphnology.services.SessionRatingService;
import com.alphnology.services.SessionService;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@PageTitle("Rate")
@Route("rate")
@Menu(order = 2, icon = LineAwesomeIconUrl.STAR)
@PermitAll
public class RateView extends VerticalLayout implements AfterNavigationObserver {

    private final transient SessionService sessionService;
    private final  transient SessionRatingService sessionRatingService;

    private final VerticalLayout unratedLayout = new VerticalLayout();
    private final VerticalLayout ratedLayout = new VerticalLayout();
    private final List<UnratedSessionCard> unratedCards = new ArrayList<>();

    private final TextField filterField = new TextField();
    private final User currentUser;

    private Tab unratedTab;
    private Tab ratedTab;
    private Span unratedCountBadge;
    private Span ratedCountBadge;

    public RateView(SessionService sessionService, SessionRatingService sessionRatingService) {
        this.sessionService = sessionService;
        this.sessionRatingService = sessionRatingService;

        currentUser = VaadinSession.getCurrent().getAttribute(User.class);

        configureUI();

        populateRatings(currentUser);
    }

    private void configureUI() {
        setSpacing(true);
        setSizeFull();

        filterField.setWidthFull();
        filterField.addClassNames(LumoUtility.MaxWidth.SCREEN_LARGE);
        filterField.setPlaceholder("Filter by title or speaker...");
        filterField.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterField.setClearButtonVisible(true);
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.addValueChangeListener(e -> filterUnratedCards(e.getValue()));

        createTabs();

        Tabs tabs = new Tabs(unratedTab, ratedTab);
        tabs.setWidthFull();

        tabs.addSelectedChangeListener(event -> {
            unratedLayout.setVisible(false);
            ratedLayout.setVisible(false);
            filterField.setVisible(false);

            if (event.getSelectedTab() == unratedTab) {
                unratedLayout.setVisible(true);
                filterField.setVisible(true);
            } else if (event.getSelectedTab() == ratedTab) {
                ratedLayout.setVisible(true);
            }
        });

        unratedLayout.setSpacing(true);
        ratedLayout.setSpacing(true);
        ratedLayout.setVisible(false);

        add(tabs, filterField, unratedLayout, ratedLayout);
    }

    private void createTabs() {
        unratedCountBadge = new Span("0");
        unratedCountBadge.getElement().getThemeList().add("badge small primary pill");

        ratedCountBadge = new Span("0");
        ratedCountBadge.getElement().getThemeList().add("badge small primary success pill");

        unratedTab = new Tab(new Span("Awaiting Rating"), unratedCountBadge);
        unratedTab.addClassNames(LumoUtility.Gap.XSMALL);
        ratedTab = new Tab(new Span("My Past Ratings"), ratedCountBadge);
        ratedTab.addClassNames(LumoUtility.Gap.XSMALL);
    }

    private void filterUnratedCards(String filterText) {
        unratedCards.forEach(card -> card.setVisible(card.matches(filterText)));
    }

    private void populateRatings(User user) {
        unratedLayout.removeAll();
        unratedCards.clear();
        ratedLayout.removeAll();

        List<Session> unratedSessions = sessionService.findUnratedSessionsForUser(user)
                .stream().sorted(Comparator.comparing(Session::getStartTime))
                .toList();
        unratedCountBadge.setText(String.valueOf(unratedSessions.size()));

        if (unratedSessions.isEmpty()) {
            unratedLayout.add(new Paragraph("You have rated all your sessions. Great job!"));
        } else {
            unratedSessions.forEach(session -> {
                UnratedSessionCard card = new UnratedSessionCard(sessionRatingService, session, () -> populateRatings(currentUser));
                unratedLayout.add(card);
                unratedCards.add(card);
            });
        }

        List<SessionRating> ratedSessions = sessionRatingService.findByUser(user)
                .stream().sorted(Comparator.comparing(s -> s.getSession().getStartTime()))
                .toList();
        ratedCountBadge.setText(String.valueOf(ratedSessions.size()));

        if (ratedSessions.isEmpty()) {
            ratedLayout.add(new Paragraph("You haven't rated any sessions yet."));
        } else {
            ratedSessions.forEach(rating -> ratedLayout.add(new RatedSessionCard(sessionRatingService, rating, () -> populateRatings(currentUser))));
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        String script = "document.querySelectorAll('vaadin-dialog-overlay').forEach(overlay => overlay.close());";
        getUI().ifPresent(ui -> ui.getPage().executeJs(script));
    }
}
