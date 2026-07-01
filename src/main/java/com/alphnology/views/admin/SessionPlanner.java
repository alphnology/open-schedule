package com.alphnology.views.admin;

import com.alphnology.data.Event;
import com.alphnology.data.Room;
import com.alphnology.data.Session;
import com.alphnology.services.RoomService;
import com.alphnology.services.SessionService;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.util.StringUtils;
import org.vaadin.stefan.fullcalendar.EntryResizedEvent;
import org.vaadin.stefan.fullcalendar.FullCalendar;
import org.vaadin.stefan.fullcalendar.FullCalendarScheduler;
import org.vaadin.stefan.fullcalendar.FullCalendarVariant;
import org.vaadin.stefan.fullcalendar.Resource;
import org.vaadin.stefan.fullcalendar.ResourceEntry;
import org.vaadin.stefan.fullcalendar.Scheduler;
import org.vaadin.stefan.fullcalendar.SchedulerView;
import org.vaadin.stefan.fullcalendar.Timezone;
import org.vaadin.stefan.fullcalendar.dataprovider.EntryProvider;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.alphnology.utils.ViewHelper.getSecondaryHeader;

public class SessionPlanner extends VerticalLayout {

    private static final String UNASSIGNED_RESOURCE_ID = "__unassigned__";

    private final transient SessionService sessionService;
    private final transient RoomService roomService;
    private final Event event;
    private final Consumer<Session> sessionSelectionCallback;
    private final Runnable mutationCallback;
    private final ZoneId zoneId;

    private final FullCalendarScheduler calendar;
    private final DatePicker selectedDate = new DatePicker("Planner date");

    public SessionPlanner(
            SessionService sessionService,
            RoomService roomService,
            Event event,
            Consumer<Session> sessionSelectionCallback,
            Runnable mutationCallback
    ) {
        this.sessionService = sessionService;
        this.roomService = roomService;
        this.event = event;
        this.sessionSelectionCallback = sessionSelectionCallback;
        this.mutationCallback = mutationCallback;
        this.zoneId = resolveZoneId(event.getTimeZone());
        this.calendar = new FullCalendarScheduler();

        configureLayout();
        configureCalendar();
        add(createHeader(), calendar);

        refresh();
    }

    public void refresh() {
        List<Session> sessionsForSelectedDate = getSessionsForSelectedDate();
        Map<String, Resource> resourcesById = reloadResources(sessionsForSelectedDate);
        List<ResourceEntry> entries = sessionsForSelectedDate.stream()
                .sorted(Comparator.comparing(Session::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(session -> toEntry(session, resourcesById))
                .toList();

        calendar.setEntryProvider(EntryProvider.inMemoryFrom(entries));
        calendar.gotoDate(selectedDate.getValue());
    }

    private void configureLayout() {
        setSizeFull();
        setMinWidth("0");
        setPadding(false);
        setSpacing(false);
        setMargin(false);
        getStyle().set("overflow", "hidden");
    }

    private void configureCalendar() {
        calendar.addThemeVariants(FullCalendarVariant.VAADIN);
        calendar.setWidthFull();
        calendar.setMinWidth("0");
        calendar.setHeight("76vh");
        calendar.setOption(FullCalendar.Option.FIRST_DAY, DayOfWeek.MONDAY);
        calendar.setOption(FullCalendar.Option.LOCALE, UI.getCurrent().getLocale());
        calendar.setOption(FullCalendar.Option.NOW_INDICATOR, true);
        calendar.setOption(FullCalendar.Option.NAV_LINKS, true);
        calendar.setOption(FullCalendar.Option.EDITABLE, true);
        calendar.setOption("allDaySlot", false);
        calendar.setOption("slotDuration", "00:30:00");
        calendar.setOption("snapDuration", "00:15:00");
        calendar.setOption("scrollTime", "07:00:00");
        calendar.setOption("slotMinTime", "07:00:00");
        calendar.setOption("slotMaxTime", "22:00:00");
        calendar.setOption("slotEventOverlap", false);
        calendar.setOption("eventMaxStack", 8);
        calendar.setOption(FullCalendarScheduler.SchedulerOption.LICENSE_KEY, Scheduler.GPL_V3_LICENSE_KEY);
        calendar.setOption(FullCalendarScheduler.SchedulerOption.RESOURCE_AREA_WIDTH, "9%");
        calendar.setOption(FullCalendarScheduler.SchedulerOption.RESOURCE_AREA_HEADER_CONTENT, "Rooms");
        calendar.setOption(FullCalendarScheduler.SchedulerOption.SLOT_MIN_WIDTH, "60");
        calendar.setOption(FullCalendarScheduler.SchedulerOption.ENTRY_RESOURCES_EDITABLE, true);
        // Sessions are persisted as LocalDateTime in event-local wall clock time.
        // Rendering them in the browser's local calendar timezone preserves the exact
        // hour configured in the admin form instead of applying a second conversion.
        calendar.setOption("timeZone", "local");
        calendar.getElement().executeJs(
                "return Intl.DateTimeFormat().resolvedOptions().timeZone;"
        ).then(String.class, timezone -> calendar.setTimezone(new Timezone(ZoneId.of(timezone))));

        calendar.setEntryDidMountCallback(
                """
                function(info) {
                  if (info.event && info.event.title) {
                    info.el.setAttribute('title', info.event.title);
                  }
                }
                """
        );
        calendar.changeView(SchedulerView.RESOURCE_TIME_GRID_DAY);

        calendar.addEntryClickedListener(event -> sessionService.get(Long.valueOf(event.getEntry().getId()))
                .ifPresent(sessionSelectionCallback));
        calendar.addEntryResizedListener(this::handleResize);
        calendar.addEntryDroppedSchedulerListener(event -> {
            ResourceEntry changedEntry = event.getChangesAsEntry();
            persistCalendarChange(changedEntry, event.getNewResource().orElse(null), event::applyChangesOnEntry);
        });
    }

    private Header createHeader() {
        Header header = getSecondaryHeader(
                "Schedule planner",
                "Choose a single day, drag sessions to reschedule them, and click any block to edit full metadata in the form."
        );
        header.getStyle().set("flex-wrap", "wrap");
        header.getStyle().set("row-gap", "0.75rem");

        selectedDate.setValue(resolveInitialPlannerDate());
        selectedDate.setWeekNumbersVisible(true);
        selectedDate.setMin(event.getStartDate());
        selectedDate.setMax(event.getEndDate());
        selectedDate.setWidth("10rem");
        selectedDate.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                refresh();
            }
        });

        Button previous = new Button("Prev", VaadinIcon.ANGLE_LEFT.create(), event -> shiftDate(-1));
        Button today = new Button("Today", event -> jumpToToday());
        Button next = new Button("Next", VaadinIcon.ANGLE_RIGHT.create(), event -> shiftDate(1));
        Button refresh = new Button(VaadinIcon.REFRESH.create(), event -> refresh());

        previous.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        today.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        next.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        previous.setIconAfterText(false);
        next.setIconAfterText(true);
        refresh.setTooltipText("Refresh planner");

        HorizontalLayout controls = new HorizontalLayout(selectedDate, previous, today, next, refresh);
        controls.setSpacing(false);
        controls.setAlignItems(FlexComponent.Alignment.CENTER);
        controls.setWidthFull();
        controls.setJustifyContentMode(JustifyContentMode.START);
        controls.setAlignItems(Alignment.BASELINE);
        controls.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.SMALL);
        controls.getStyle().set("margin-left", "0");
        controls.getStyle().set("padding-right", "0");
        previous.getStyle().set("padding-inline", "0.45rem");
        today.getStyle().set("padding-inline", "0.45rem");
        next.getStyle().set("padding-inline", "0.45rem");
        header.add(controls);
        return header;
    }

    private void handleResize(EntryResizedEvent event) {
        ResourceEntry changedEntry = event.getChangesAsEntry();
        Resource currentResource = changedEntry.getResource().orElse(null);
        persistCalendarChange(changedEntry, currentResource, event::applyChangesOnEntry);
    }

    private void persistCalendarChange(ResourceEntry changedEntry, Resource selectedResource, Runnable applyClientChange) {
        Optional<Session> optionalSession = sessionService.get(Long.valueOf(changedEntry.getId()));
        if (optionalSession.isEmpty()) {
            NotificationUtils.error("The selected session no longer exists.");
            refresh();
            return;
        }

        Session session = optionalSession.get();
        session.setStartTime(changedEntry.getStart());
        session.setEndTime(changedEntry.getEnd());
        session.setRoom(Objects.requireNonNull(resolveRoom(selectedResource, session.getRoom())));

        try {
            sessionService.save(session);
            applyClientChange.run();
            mutationCallback.run();
            refresh();
        } catch (Exception ex) {
            NotificationUtils.error(StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "The session could not be updated.");
            refresh();
        }
    }

    private Map<String, Resource> reloadResources(List<Session> sessionsForSelectedDate) {
        calendar.removeAllResources();

        List<Resource> resources = new ArrayList<>();
        boolean hasUnassignedSessions = sessionsForSelectedDate.stream().anyMatch(session -> session.getRoom() == null);
        if (hasUnassignedSessions) {
            resources.add(new Resource(UNASSIGNED_RESOURCE_ID, "Unassigned", null));
        }

        sessionsForSelectedDate.stream()
                .map(Session::getRoom)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(Room::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(room -> resources.add(new Resource(resourceId(room), formatResourceLabel(room), room.getColor())));

        if (resources.isEmpty()) {
            resources.add(new Resource(UNASSIGNED_RESOURCE_ID, "No sessions", null));
        }

        calendar.addResources(resources);

        Map<String, Resource> byId = new LinkedHashMap<>();
        resources.forEach(resource -> byId.put(resource.getId(), resource));
        return byId;
    }

    private List<Session> getSessionsForSelectedDate() {
        LocalDate selectedPlannerDate = selectedDate.getValue() != null ? selectedDate.getValue() : resolveInitialPlannerDate();
        LocalDateTime startOfDay = selectedPlannerDate.atStartOfDay();
        LocalDateTime endOfDay = selectedPlannerDate.plusDays(1).atStartOfDay();

        return sessionService.findAll(null).stream()
                .filter(session -> session.getStartTime() != null)
                .filter(session -> {
                    LocalDateTime sessionEnd = session.getEndTime() != null ? session.getEndTime() : session.getStartTime();
                    return session.getStartTime().isBefore(endOfDay) && sessionEnd.isAfter(startOfDay);
                })
                .toList();
    }

    private ResourceEntry toEntry(Session session, Map<String, Resource> resourcesById) {
        ResourceEntry entry = new ResourceEntry(String.valueOf(session.getCode()));
        entry.setTitle(session.getTitle());
        entry.setStart(session.getStartTime());
        entry.setEnd(session.getEndTime());
        entry.setEditable(true);
        entry.setStartEditable(true);
        entry.setDurationEditable(true);

        if (session.getTrack() != null && StringUtils.hasText(session.getTrack().getColor())) {
            entry.setColor(session.getTrack().getColor());
        } else if (session.getRoom() != null && StringUtils.hasText(session.getRoom().getColor())) {
            entry.setColor(session.getRoom().getColor());
        }

        Resource resource = session.getRoom() != null
                ? resourcesById.getOrDefault(resourceId(session.getRoom()), resourcesById.get(UNASSIGNED_RESOURCE_ID))
                : resourcesById.get(UNASSIGNED_RESOURCE_ID);
        entry.addResources(resource);
        return entry;
    }

    private Room resolveRoom(Resource resource, Room currentRoom) {
        if (resource == null) {
            return currentRoom;
        }
        if (UNASSIGNED_RESOURCE_ID.equals(resource.getId()) || "No sessions".equals(resource.getTitle())) {
            return null;
        }
        Long roomId = Long.valueOf(resource.getId());
        return roomService.get(roomId).orElse(null);
    }

    private String resourceId(Room room) {
        return String.valueOf(room.getCode());
    }

    private String formatResourceLabel(Room room) {
        if (room == null || !StringUtils.hasText(room.getName())) {
            return "Room";
        }

        String name = room.getName().trim();
        int roomMarker = name.lastIndexOf("Room:");
        if (roomMarker >= 0) {
            String shortName = name.substring(roomMarker + "Room:".length()).trim();
            if (StringUtils.hasText(shortName)) {
                return shortName;
            }
        }

        int separator = name.lastIndexOf('|');
        if (separator >= 0 && separator < name.length() - 1) {
            String shortName = name.substring(separator + 1).trim();
            if (StringUtils.hasText(shortName)) {
                return shortName;
            }
        }

        return name;
    }

    private LocalDate resolveInitialPlannerDate() {
        LocalDate today = LocalDate.now(zoneId);
        if (!today.isBefore(event.getStartDate()) && !today.isAfter(event.getEndDate())) {
            return today;
        }
        return event.getStartDate();
    }

    private void shiftDate(int days) {
        LocalDate currentDate = selectedDate.getValue() != null ? selectedDate.getValue() : resolveInitialPlannerDate();
        LocalDate targetDate = currentDate.plusDays(days);
        if (targetDate.isBefore(event.getStartDate())) {
            targetDate = event.getStartDate();
        }
        if (targetDate.isAfter(event.getEndDate())) {
            targetDate = event.getEndDate();
        }
        selectedDate.setValue(targetDate);
        calendar.gotoDate(targetDate);
    }

    private void jumpToToday() {
        LocalDate today = resolveInitialPlannerDate();
        selectedDate.setValue(today);
        calendar.gotoDate(today);
    }

    private ZoneId resolveZoneId(String rawTimeZone) {
        if (!StringUtils.hasText(rawTimeZone)) {
            return ZoneId.systemDefault();
        }

        String trimmed = rawTimeZone.trim();
        String canonical = trimmed.replaceAll("\\s*\\(.*\\)$", "").trim();
        try {
            return ZoneId.of(canonical);
        } catch (Exception ignored) {
            int offsetStart = trimmed.indexOf("(UTC");
            int offsetEnd = trimmed.indexOf(')', offsetStart);
            if (offsetStart >= 0 && offsetEnd > offsetStart) {
                String offsetValue = trimmed.substring(offsetStart + 4, offsetEnd).trim();
                try {
                    return ZoneOffset.of(offsetValue);
                } catch (Exception secondIgnored) {
                    // fall through to system default
                }
            }
            return ZoneId.systemDefault();
        }
    }
}
