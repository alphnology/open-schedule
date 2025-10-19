package com.alphnology.views.schedule;

import com.alphnology.data.*;
import com.alphnology.services.EventService;
import com.alphnology.services.SessionRatingService;
import com.alphnology.services.SessionService;
import com.alphnology.services.UserService;
import com.alphnology.utils.DateTimeFormatterUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.alphnology.utils.PredicateUtils.createPredicateForDateTimeRange;
import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;


@PageTitle("Schedule")
@Route("")
@RouteAlias("schedule")
@Menu(order = 0, icon = LineAwesomeIconUrl.CALENDAR)
@AnonymousAllowed
public class ScheduleView extends VerticalLayout {

    private static final String SEARCH_PLACEHOLDER = "Search...";
    private static final String COLOR_WHITE = "white";

    private final TextField searchField = new TextField(SEARCH_PLACEHOLDER);

    private final transient SessionService sessionService;

    private final ScheduleViewDetails scheduleViewDetails;
    private final TabSheet tabSheet = new TabSheet();

    private LocalDate currentDate;


    public ScheduleView(EventService eventService, SessionService sessionService, SessionRatingService sessionRatingService, UserService userService) {
        this.sessionService = sessionService;

        scheduleViewDetails = new ScheduleViewDetails(sessionService, sessionRatingService, userService);

        setSpacing(false);
        setSizeFull();
        addClassNames(LumoUtility.Padding.MEDIUM);


        Optional<Event> optionalEvent = eventService.findAll().stream().findFirst();
        if (optionalEvent.isEmpty()) {
            NotificationUtils.error("To be able to create a section there must be an event created");
            return;
        }

        Event event = optionalEvent.get();

        VaadinSession.getCurrent().setAttribute(Event.class, event);

        Span notice = new Span("Notice: Agenda topics are subject to change.");
        notice.addClassNames(
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.FontSize.SMALL,
                LumoUtility.AlignSelf.CENTER
        );

        searchField.focus();
        searchField.addClassNames(LumoUtility.Flex.GROW, LumoUtility.MinWidth.NONE);
        searchField.setAriaLabel(SEARCH_PLACEHOLDER);
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.setPlaceholder(SEARCH_PLACEHOLDER);
        searchField.setPrefixComponent(LumoIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addClassNames(LumoUtility.MaxWidth.SCREEN_MEDIUM, LumoUtility.AlignSelf.CENTER);
        searchField.addValueChangeListener(event1 -> updateCurrentTabContent());

        add(notice, searchField);

        int tabIndex = 0;
        int selectedIndex = 0;
        LocalDate today = LocalDate.now();

        for (LocalDate date = event.getStartDate(); !date.isAfter(event.getEndDate()); date = date.plusDays(1)) {
            tabSheet.add(date.format(DateTimeFormatterUtils.dateFormatter), createLazySection(date));

            if (date.isEqual(today)) {
                selectedIndex = tabIndex;
            }
            tabIndex++;
        }

        tabSheet.addThemeVariants(TabSheetVariant.LUMO_TABS_EQUAL_WIDTH_TABS);
        tabSheet.setSelectedIndex(selectedIndex);
        tabSheet.setSizeFull();
        add(tabSheet);

    }

    private void updateCurrentTabContent() {
        Tab selectedTab = tabSheet.getSelectedTab();
        Component content = tabSheet.getComponent(selectedTab);
        if (content instanceof Div container) {
            populateSection(container, currentDate);
        }
    }

    private Component createLazySection(LocalDate date) {
        Div container = new Div();
        return new LazyComponent(() -> populateSection(container, date), container);
    }

    private Component populateSection(Div container, LocalDate date) {
        container.removeAll();
        container.setSizeFull();
        this.currentDate = date;
        List<Session> sessions = sessionService.findAll(createFilterSpecification(date));
        if (sessions.isEmpty()) {
            container.add(new Span("No sessions scheduled for this day."));
            return container;
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
            roomNameSpan.getStyle().setMinWidth("250px");
            if (room.getColor() != null) {
                roomNameSpan.getStyle().setBackgroundColor(room.getColor()).setColor(COLOR_WHITE);
            }
            roomNameSpan.addClassNames(
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

                        sessionCell.getStyle().setMinWidth("250px");
                        if (sessionForThisRoomAndTime.isPresent()) {
                            Session currentSession = sessionForThisRoomAndTime.get();
                            sessionCell.add(new ScheduleViewCard(currentSession, sessionService, scheduleViewDetails::showSession));
                            if (currentSession.getRoom() != null && currentSession.getRoom().getColor() != null) {
                                sessionCell.getStyle().setBackgroundColor(currentSession.getRoom().getColor()).setColor(COLOR_WHITE);
                            }
                            sessionCell.addClassNames(
                                    LumoUtility.Display.FLEX
                            );
                        } else {
                            sessionCell.setText("");
                            sessionCell.removeClassName("transition-card");
                            sessionCell.addClassNames(
                                    LumoUtility.Display.HIDDEN,
                                    LumoUtility.Display.Breakpoint.Small.FLEX
                            );

                        }
                        dataRow.add(sessionCell);
                    }

                    sectionLayout.add(dataRow);
                });

        container.add(sectionLayout);
        return container;
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

            final String search = searchField.getValue().toLowerCase().trim();

            Order order = builder.asc(root.get("code"));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);

            Predicate predicateTitle = predicateUnaccentLike(root, builder, "title", search);
            Predicate predicateDescription = predicateUnaccentLike(root, builder, "description", search);


            Join<Session, Room> roomJoin = root.join("room", JoinType.LEFT);
            Predicate predicateRoom = predicateUnaccentLike(roomJoin, builder, "name", search);

            Join<Session, Track> trackJoin = root.join("track", JoinType.LEFT);
            Predicate predicateTrack = predicateUnaccentLike(trackJoin, builder, "name", search);

            Join<Session, Speaker> speakerJoin = root.join("speakers", JoinType.LEFT);
            Predicate predicateSpeaker = predicateUnaccentLike(speakerJoin, builder, "name", search);

            Join<Session, Tag> tagJoin = root.join("tags", JoinType.LEFT);
            Predicate predicateTag = predicateUnaccentLike(tagJoin, builder, "name", search);

            final List<Predicate> orPredicates = new ArrayList<>(List.of(predicateTitle, predicateDescription, predicateRoom, predicateTrack, predicateSpeaker, predicateTag));

            Predicate orPredicate = orPredicates.isEmpty() ? builder.conjunction() : builder.or(orPredicates.toArray(Predicate[]::new));

            Predicate predicateDate = createPredicateForDateTimeRange(LocalDateTime.of(date, LocalDateTime.MIN.toLocalTime()), LocalDateTime.of(date, LocalDateTime.MAX.toLocalTime()), root.get("startTime"), root.get("endTime"), builder);

            return builder.and(orPredicate, predicateDate);

        };
    }

    private static Div timeSpan() {
        Div timeSpan = new Div();
        timeSpan.setWidth("80px");
        timeSpan.getStyle().set("flex-shrink", "0");
        return timeSpan;
    }

    private static class LazyComponent extends Div {
        public LazyComponent(SerializableSupplier<Component> supplier, Div content) {
            addAttachListener(e -> {
                if (getElement().getChildCount() == 0) {
                    add(content);
                    supplier.get();
                }
            });
        }
    }

    public record TimeRange(LocalDateTime minStart, LocalDateTime maxEnd) {
    }


}
