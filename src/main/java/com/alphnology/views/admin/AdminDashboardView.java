package com.alphnology.views.admin;

import com.alphnology.data.Event;
import com.alphnology.services.*;
import com.alphnology.utils.ViewHelper;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@PageTitle("Dashboard")
@Route("admin/dashboard")
@Menu(order = 10, icon = LineAwesomeIconUrl.TACHOMETER_ALT_SOLID)
@RolesAllowed("ADMIN")
public class AdminDashboardView extends VerticalLayout {

    public AdminDashboardView(
            SessionService sessionService,
            SpeakerService speakerService,
            AttenderService attenderService,
            UserService userService,
            SessionRatingService sessionRatingService,
            EventService eventService
    ) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        Header header = ViewHelper.getSecondaryHeader("Dashboard", "Overview of your event");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Gap.LARGE);
        content.setWidthFull();

        // ── Event info card ───────────────────────────────────
        Optional<Event> maybeEvent = eventService.findAll().stream().findFirst();
        maybeEvent.ifPresent(event -> content.add(buildEventCard(event)));

        // ── KPI stats ─────────────────────────────────────────
        Div statsRow = new Div();
        statsRow.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.MEDIUM);
        statsRow.setWidthFull();

        statsRow.add(
                createStatCard(VaadinIcon.PRESENTATION,  "Sessions",  sessionService.count(),      "--lumo-primary-color"),
                createStatCard(VaadinIcon.USER,           "Speakers",  speakerService.count(),      "--lumo-success-color"),
                createStatCard(VaadinIcon.TICKET,         "Attenders", attenderService.count(),     "--lumo-warning-color"),
                createStatCard(VaadinIcon.LOCK,           "Users",     userService.count(),         "--lumo-error-color"),
                createStatCard(VaadinIcon.STAR,           "Ratings",   sessionRatingService.count(),"--lumo-contrast")
        );
        content.add(statsRow);

        Scroller scroller = ViewHelper.getScrollerVertical();
        scroller.setContent(content);

        add(header, scroller);
        setFlexGrow(1, scroller);
    }

    // ── Private builders ──────────────────────────────────────

    private Div buildEventCard(Event event) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        Span nameSpan = new Span(event.getName());
        nameSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

        String dateRange = event.getStartDate().format(fmt) + " – " + event.getEndDate().format(fmt);
        Span dates = buildMeta(VaadinIcon.CALENDAR, dateRange);
        Span tz = buildMeta(VaadinIcon.CLOCK, event.getTimeZone());

        HorizontalLayout meta = new HorizontalLayout(dates, tz);
        meta.setSpacing(false);
        meta.addClassNames(LumoUtility.Gap.MEDIUM, LumoUtility.FlexWrap.WRAP);

        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            meta.add(buildMeta(VaadinIcon.MAP_MARKER, event.getLocation()));
        }

        Div card = new Div(nameSpan, meta);
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.XSMALL,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL
        );
        card.setWidthFull();
        return card;
    }

    private Span buildMeta(VaadinIcon vaadinIcon, String text) {
        Icon icon = vaadinIcon.create();
        icon.setSize("14px");
        icon.addClassNames(LumoUtility.TextColor.SECONDARY);

        Span label = new Span(text);
        label.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Span wrapper = new Span(icon, label);
        wrapper.addClassNames(LumoUtility.Display.INLINE_FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.XSMALL);
        return wrapper;
    }

    private Div createStatCard(VaadinIcon vaadinIcon, String label, long value, String colorVar) {
        // Icon wrapper with tinted background
        Icon icon = vaadinIcon.create();
        icon.setSize("20px");
        icon.getStyle().set("color", "var(" + colorVar + ")");

        Div iconWrap = new Div(icon);
        iconWrap.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "40px")
                .set("height", "40px")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "var(" + colorVar + "-10pct)");

        // Value
        Span valueSpan = new Span(String.valueOf(value));
        valueSpan.addClassNames(
                LumoUtility.FontSize.XXXLARGE,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.LineHeight.NONE
        );
        valueSpan.getStyle().set("letter-spacing", "-0.02em");

        // Label
        Span labelSpan = new Span(label);
        labelSpan.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.FontWeight.MEDIUM,
                LumoUtility.TextColor.SECONDARY
        );
        labelSpan.getStyle().set("text-transform", "uppercase").set("letter-spacing", "0.05em");

        VerticalLayout textCol = new VerticalLayout(valueSpan, labelSpan);
        textCol.setSpacing(false);
        textCol.setPadding(false);
        textCol.setAlignItems(FlexComponent.Alignment.START);

        HorizontalLayout card = new HorizontalLayout(iconWrap, textCol);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setSpacing(false);
        card.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.XSMALL,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.MEDIUM
        );
        card.getStyle().set("flex", "1 1 180px").set("min-width", "160px");
        return card;
    }
}
