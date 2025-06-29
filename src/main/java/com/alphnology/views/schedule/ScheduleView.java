package com.alphnology.views.schedule;

import com.alphnology.data.Event;
import com.alphnology.data.Room;
import com.alphnology.data.Session;
import com.alphnology.services.EventService;
import com.alphnology.services.SessionRatingService;
import com.alphnology.services.SessionService;
import com.alphnology.utils.DateTimeFormatterUtils;
import com.alphnology.utils.NotificationUtils;
import com.alphnology.views.rate.RatingEventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.alphnology.utils.PredicateUtils.createPredicateForDateTimeRange;


@PageTitle("Schedule")
@Route("")
@RouteAlias("schedule")
@Menu(order = 0, icon = LineAwesomeIconUrl.CALENDAR)
@AnonymousAllowed
public class ScheduleView extends VerticalLayout {

    private static final String COLOR_WHITE = "white";

    private final transient SessionService sessionService;

    private final ScheduleViewDetails scheduleViewDetails;


    public ScheduleView(EventService eventService, SessionService sessionService, SessionRatingService sessionRatingService, RatingEventBus ratingEventBus) {
        this.sessionService = sessionService;

        scheduleViewDetails = new ScheduleViewDetails(ratingEventBus, sessionService, sessionRatingService);

        setWidthFull();
        getStyle().set("padding", "1rem");

        Optional<Event> optionalEvent = eventService.findAll().stream().findFirst();
        if (optionalEvent.isEmpty()) {
            NotificationUtils.error("To be able to create a section there must be an event created");
            return;
        }

        Event event = optionalEvent.get();

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        for (LocalDate date = event.getStartDate(); !date.isAfter(event.getEndDate()); date = date.plusDays(1)) {
            LocalDate finalDate = date;
            tabSheet.add(date.format(DateTimeFormatterUtils.dateFormatter), new LazyComponent(() -> createSection(finalDate)));
        }

        tabSheet.addThemeVariants(TabSheetVariant.LUMO_TABS_EQUAL_WIDTH_TABS);
        add(tabSheet);

    }

    private Component createSection(LocalDate date) {
        List<Session> sessions = sessionService.findAll(createFilterSpecification(date));
        if (sessions.isEmpty()) {
            return new Div(new Span("No sessions scheduled for this day."));
        }

        // Filter to ensure room is not null before grouping and get the sorted list of rooms
        List<Room> sortedRooms = sessions.stream()
                .map(Session::getRoom) // Make sure Room is not null
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(Room::getName))
                .toList();

        Map<LocalDateTime, List<Session>> sessionsByStartTime = sessions.stream()
                .collect(Collectors.groupingBy(Session::getStartTime));

        // Main container for this section (tab)
        Div sectionLayout = new Div();
        sectionLayout.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.Row.SMALL // Space between the header row and data rows
        );

        // 1. Create and add the header row
        Div headerRow = new Div();
        headerRow.addClassNames(
                LumoUtility.Display.HIDDEN,
                LumoUtility.FlexDirection.ROW,
                LumoUtility.JustifyContent.START,
                LumoUtility.Gap.Column.MEDIUM,
                LumoUtility.Display.Breakpoint.Large.FLEX
        );

        headerRow.add(timeSpan()); // Cell for "Time" with fixed width

        sortedRooms.forEach(room -> {
            Span roomNameSpan = new Span(room.getName());
            roomNameSpan.getStyle().set("flex-grow", "1");
            roomNameSpan.getStyle().set("flex-basis", "0"); // Allows flex-grow to distribute space
            roomNameSpan.getStyle().set("text-align", "center");
            if (room.getColor() != null) {
                roomNameSpan.getStyle().setBackgroundColor(room.getColor()).setColor(COLOR_WHITE);
            }
            roomNameSpan.addClassNames(
                    LumoUtility.Padding.SMALL,
                    LumoUtility.BorderRadius.SMALL,
                    LumoUtility.FontSize.LARGE,
                    LumoUtility.FontWeight.SEMIBOLD
            );
            headerRow.add(roomNameSpan);
        });
        sectionLayout.add(headerRow);

        // 2. Create and add data rows
        sessionsByStartTime.keySet().stream()
                .sorted(LocalDateTime::compareTo)
                .forEach(time -> {
                    sectionLayout.add(new Hr());
                    Div dataRow = new Div();
                    dataRow.addClassNames(
                            LumoUtility.Display.FLEX,
                            LumoUtility.FlexDirection.COLUMN,
                            LumoUtility.FlexDirection.Breakpoint.Large.ROW,
                            LumoUtility.JustifyContent.START,
                            LumoUtility.Gap.MEDIUM
                    );

                    List<Session> sessionsInThisTimeSlot = sessionsByStartTime.get(time);

                    TimeRange timeRange = sessionsInThisTimeSlot.stream().collect(Collectors.teeing(
                            Collectors.mapping(Session::getStartTime, Collectors.minBy(Comparator.naturalOrder())),
                            Collectors.mapping(Session::getEndTime, Collectors.maxBy(Comparator.naturalOrder())),
                            (minOpt, maxOpt) -> new TimeRange(minOpt.orElse(null), maxOpt.orElse(null))
                    ));


                    Span separator = new Span("-");
                    // Cell for the current time
                    Div timeCell = new Div(new Span(timeRange.minStart().format(DateTimeFormatterUtils.timeFormatter)), separator, new Span(timeRange.maxEnd().format(DateTimeFormatterUtils.timeFormatter)));
                    timeCell.getElement().getThemeList().add("badge contrast");
                    timeCell.setMinWidth("80px");// Same width as the header's timeSpan()
                    timeCell.getStyle().set("flex-shrink", "0"); // Prevents shrinking
                    timeCell.addClassNames(
                            LumoUtility.Display.FLEX,
                            LumoUtility.FlexDirection.ROW,
                            LumoUtility.FlexDirection.Breakpoint.Medium.COLUMN,
                            LumoUtility.AlignItems.CENTER,
                            LumoUtility.Gap.MEDIUM
                    );
                    separator.addClassNames(
                            LumoUtility.Display.BLOCK,
                            LumoUtility.Display.Breakpoint.Medium.HIDDEN
                    );

                    dataRow.add(timeCell);

                    // Cells for the sessions
                    for (Room room : sortedRooms) {

                        Div sessionCell = new Div();
                        sessionCell.getStyle().set("flex-grow", "1");
                        sessionCell.getStyle().set("flex-basis", "0");
                        sessionCell.getStyle().set("text-align", "left");
//                        sessionCell.getStyle().set("min-height", LumoUtility.Height.XLARGE); // To give some height to the cells
                        sessionCell.addClassNames(
                                LumoUtility.Display.FLEX,
                                LumoUtility.FlexDirection.ROW,
                                LumoUtility.Padding.NONE,
                                LumoUtility.BorderRadius.SMALL,
                                LumoUtility.FontSize.MEDIUM, // Can be different from the header
                                "transition-card"
                        );

                        if (sessionsInThisTimeSlot.size() == 1) {
                            Session first = sessionsInThisTimeSlot.getFirst();
                            sessionCell.add(new ScheduleViewCard(first, sessionService, scheduleViewDetails::showSession));

                            String color = getColor(first);
                            sessionCell.getStyle().setBackgroundColor(color).setColor(COLOR_WHITE);
                            dataRow.add(sessionCell);
                            break;
                        }

                        Optional<Session> sessionForThisRoomAndTime = sessionsInThisTimeSlot.stream()
                                .filter(s -> s.getRoom() != null && s.getRoom().equals(room))
                                .findFirst();

                        if (sessionForThisRoomAndTime.isPresent()) {
                            Session currentSession = sessionForThisRoomAndTime.get();
                            sessionCell.add(new ScheduleViewCard(currentSession, sessionService, scheduleViewDetails::showSession));
                            if (currentSession.getRoom() != null && currentSession.getRoom().getColor() != null) {
                                sessionCell.getStyle().setBackgroundColor(currentSession.getRoom().getColor()).setColor(COLOR_WHITE);
                            }
                        } else {
                            sessionCell.setText("");
                            sessionCell.removeClassName("transition-card");
                        }
                        dataRow.add(sessionCell);
                    }

                    sectionLayout.add(dataRow);
                });

        return sectionLayout;
    }

    private static String getColor(Session first) {
        String color;
        if (first.getRoom() != null) {
            color = first.getRoom().getColor();
        } else {
            color = switch (first.getType()) {
                case CB -> "#D2B48C";
                case L -> "#8FBC8F";
                case OS -> "#87CEEB";
                default -> "#ADD8E6";
            };
        }
        return color;
    }


    private Specification<Session> createFilterSpecification(LocalDate date) {
        return (root, query, builder) -> {

            Order order = builder.asc(root.get("code"));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);

            Predicate predicateDate = createPredicateForDateTimeRange(LocalDateTime.of(date, LocalDateTime.MIN.toLocalTime()), LocalDateTime.of(date, LocalDateTime.MAX.toLocalTime()), root.get("startTime"), root.get("endTime"), builder);

            return builder.and(predicateDate);

        };
    }

    private static Div timeSpan() {
        Div timeSpan = new Div();
        timeSpan.setWidth("80px");
        timeSpan.getStyle().set("flex-shrink", "0");
        return timeSpan;
    }

    private static class LazyComponent extends Div {
        public LazyComponent(SerializableSupplier<? extends Component> supplier) {
            addAttachListener(e -> {
                if (getElement().getChildCount() == 0) {
                    add(supplier.get());
                }
            });
        }
    }

    public record TimeRange(LocalDateTime minStart, LocalDateTime maxEnd) {
    }


}
