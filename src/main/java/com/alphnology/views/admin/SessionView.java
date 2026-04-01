package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.components.EmptyStateComponent;
import com.alphnology.data.*;
import com.alphnology.data.enums.Language;
import com.alphnology.data.enums.Level;
import com.alphnology.data.enums.SessionType;
import com.alphnology.infrastructure.storage.ObjectStorageService;
import com.alphnology.services.*;
import com.alphnology.utils.CommonUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.HasClearButton;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.util.StringUtils;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

import static com.alphnology.utils.CommonUtils.comboBoxItemFilter;
import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;

@Slf4j
@PageTitle("Sessions")
@Route("admin/session")
@Menu(order = 16, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class SessionView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final VirtualList<Session> list = new VirtualList<>();
    private final EmptyStateComponent emptyState = new EmptyStateComponent(
            VaadinIcon.PRESENTATION, "No sessions found", "Try a different search or create a new session."
    );
    private final Span countBadge = new Span("0");
    private Session selectedItem;

    private final TextField title = new TextField("Title");
    private final TextArea description = new TextArea("Description");
    private final DateTimePicker startTime = new DateTimePicker("Start Time");
    private final DateTimePicker endTime = new DateTimePicker("End Time");
    private final ComboBox<Level> level = new ComboBox<>("Level");
    private final ComboBox<Language> language = new ComboBox<>("Language");
    private final ComboBox<SessionType> type = new ComboBox<>("Type");
    private final ComboBox<Room> room = new ComboBox<>("Room");
    private final ComboBox<Track> track = new ComboBox<>("Track");
    private final MultiSelectComboBox<Speaker> speakers = new MultiSelectComboBox<>("Speakers");
    private final MultiSelectComboBox<Tag> tags = new MultiSelectComboBox<>("Tags");

    private final Button cancel = new Button("New", VaadinIcon.FILE_ADD.create());
    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());
    private final Button delete = new Button("Delete", VaadinIcon.TRASH.create());

    private final transient SessionService service;
    private final transient ObjectStorageService storageService;
    private Session element;
    private final String formatterDateTime;

    private final Binder<Session> binder = new BeanValidationBinder<>(Session.class);

    public SessionView(
            SessionService service,
            RoomService roomService,
            TrackService trackService,
            SpeakerService speakerService,
            EventService eventService,
            TagService tagService,
            ObjectStorageService storageService,
            @Value("${application.formatter.date-time-12:unknown}") String formatterDateTime,
            @Value("${application.formatter.date:unknown}") String formatterDate
    ) {
        this.service = service;
        this.storageService = storageService;
        this.formatterDateTime = formatterDateTime;

        Optional<Event> optionalEvent = eventService.findAll().stream().findFirst();
        if (optionalEvent.isEmpty()) {
            NotificationUtils.error("To be able to create a section there must be an event created");
            return;
        }

        Event event = optionalEvent.get();
        LocalDateTime minLocalDateTime = LocalDateTime.of(event.getStartDate(), LocalDateTime.MIN.toLocalTime());
        LocalDateTime maxLocalDateTime = LocalDateTime.of(event.getEndDate(), LocalDateTime.MAX.toLocalTime());

        DatePicker.DatePickerI18n dateFormat = new DatePicker.DatePickerI18n();
        dateFormat.setDateFormat(formatterDate);
        dateFormat.setFirstDayOfWeek(1);

        Arrays.asList(startTime, endTime).forEach(c -> {
            c.setStep(Duration.ofMinutes(60));
            c.setWeekNumbersVisible(true);
            c.setDatePickerI18n(dateFormat);
            c.setMin(minLocalDateTime);
            c.setMax(maxLocalDateTime);
            c.setValue(minLocalDateTime);
        });

        level.setItems(Level.values());
        level.setItemLabelGenerator(Level::getDisplay);
        language.setItems(Language.values());
        language.setItemLabelGenerator(Language::getDisplay);
        type.setItems(SessionType.values());
        type.setItemLabelGenerator(SessionType::getDisplay);
        room.setItems(comboBoxItemFilter(Room::getName, String::contains), roomService.findAll());
        room.setItemLabelGenerator(Room::getName);
        track.setItems(comboBoxItemFilter(Track::getName, String::contains), trackService.findAll());
        track.setItemLabelGenerator(Track::getName);
        speakers.setItems(comboBoxItemFilter(Speaker::getName, String::contains), speakerService.findAll());
        speakers.setItemLabelGenerator(l -> "%s(%s) at %s".formatted(l.getName(), l.getCountry(), l.getCompany()));
        speakers.setAutoExpand(MultiSelectComboBox.AutoExpandMode.BOTH);
        speakers.setClearButtonVisible(true);
        tags.setItemLabelGenerator(Tag::getName);
        tags.setAutoExpand(MultiSelectComboBox.AutoExpandMode.BOTH);
        tags.setClearButtonVisible(true);
        tags.setAllowCustomValue(true);
        tags.setItems(
                DataProvider.fromFilteringCallbacks(
                        query -> {
                            String filter = query.getFilter().orElse("");
                            return tagService.searchTags(filter, query.getOffset(), query.getLimit()).stream();
                        },
                        query -> tagService.countTags(query.getFilter().orElse(""))
                )
        );
        tags.addCustomValueSetListener(e -> {
            String customName = e.getDetail().trim();
            if (!customName.isEmpty()) {
                Tag newTag = tagService.getOrCreate(customName);
                Set<Tag> current = new HashSet<>(tags.getValue());
                current.add(newTag);
                tags.setValue(current);
                tags.getDataProvider().refreshItem(newTag);
            }
        });

        binder.bindInstanceFields(this);
        binder.getFields().forEach(field -> {
            if (field instanceof HasClearButton clear) clear.setClearButtonVisible(true);
        });

        initList();

        // ── Sidebar ──
        countBadge.addClassName("admin-count-badge");
        HorizontalLayout toolbar = new HorizontalLayout(searchField, countBadge);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setFlexGrow(1, searchField);
        toolbar.addClassNames(LumoUtility.Padding.SMALL, "admin-toolbar");

        emptyState.setVisible(false);

        VerticalLayout sidebar = new VerticalLayout(toolbar, list, emptyState);
        sidebar.setSizeFull();
        sidebar.setPadding(false);
        sidebar.setSpacing(false);
        sidebar.setFlexGrow(1, list);

        // ── Form panel ──
        Footer footer = new Footer(createFooter());
        Scroller formScroller = getScrollerVertical();
        formScroller.setContent(createFormLayout());
        VerticalLayout form = getSecondaryLayout(formScroller, footer);

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(35);
        splitLayout.addToPrimary(sidebar);
        splitLayout.addToSecondary(form);

        add(splitLayout);
        setSizeFull();
        setPadding(false);
        setMargin(false);
        setSpacing(false);

        refreshList();
        clearForm();

        cancel.addClickListener(e -> clearForm());
        save.addClickListener(this::saveOrUpdate);
        delete.addClickListener(this::delete);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
    }

    private Specification<Session> createFilterSpecification() {
        return (root, query, builder) -> {
            final String search = searchField.getValue().toLowerCase().trim();
            Order order = builder.asc(root.get("startTime"));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);
            Predicate predicateTitle = predicateUnaccentLike(root, builder, "title", search);
            Predicate predicateDescription = predicateUnaccentLike(root, builder, "description", search);
            return builder.or(predicateTitle, predicateDescription);
        };
    }

    private void initList() {
        searchField.setWidthFull();
        searchField.setPlaceholder("Search sessions...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshList());

        list.setSizeFull();
        list.setRenderer(new ComponentRenderer<>(this::buildListItem));
    }

    private Div buildListItem(Session session) {
        Div item = new Div();
        item.addClassName("admin-list-item");
        if (selectedItem != null && Objects.equals(selectedItem.getCode(), session.getCode())) {
            item.addClassName("selected");
        }

        // Icon: track-colored circle
        Div iconDiv = new Div();
        iconDiv.addClassName("admin-item-icon");
        if (session.getTrack() != null && StringUtils.hasText(session.getTrack().getColor())) {
            iconDiv.getStyle().set("background-color", session.getTrack().getColor());
        }
        Icon icon = VaadinIcon.PRESENTATION.create();
        icon.setSize("18px");
        icon.getStyle().set("color", session.getTrack() != null ? "rgba(255,255,255,0.9)" : null);
        iconDiv.add(icon);

        // Body
        Div body = new Div();
        body.addClassName("admin-item-body");
        Span nameSpan = new Span(session.getTitle());
        nameSpan.addClassName("admin-item-name");
        String roomName = session.getRoom() != null ? session.getRoom().getName() : "";
        String timeStr = session.getStartTime() != null
                ? session.getStartTime().format(DateTimeFormatter.ofPattern(formatterDateTime)) : "";
        String sub = List.of(roomName, timeStr).stream().filter(StringUtils::hasText).reduce((a, b) -> a + " · " + b).orElse("");
        Span subSpan = new Span(sub);
        subSpan.addClassName("admin-item-sub");
        body.add(nameSpan, subSpan);

        // Side: type badge
        Div side = new Div();
        side.addClassName("admin-item-side");
        if (session.getType() != null) {
            Span badge = new Span(session.getType().getDisplay());
            badge.addClassName("admin-badge");
            side.add(badge);
        }

        item.add(iconDiv, body, side);
        item.addClickListener(e -> selectItem(session));
        return item;
    }

    private void selectItem(Session session) {
        populateForm(session);
        list.getDataProvider().refreshAll();
    }

    private void refreshList() {
        List<Session> items = service.findAll(createFilterSpecification());
        list.setItems(items);
        countBadge.setText(String.valueOf(items.size()));
        boolean isEmpty = items.isEmpty();
        list.setVisible(!isEmpty);
        emptyState.setVisible(isEmpty);
    }

    private VerticalLayout createFormLayout() {
        Header header = getSecondaryHeader("Session management", "Manage session information");

        title.setWidthFull();
        description.setWidthFull();
        startTime.setWidthFull();
        endTime.setWidthFull();
        level.setWidthFull();
        language.setWidthFull();
        type.setWidthFull();
        room.setWidthFull();
        track.setWidthFull();
        speakers.setWidthFull();
        tags.setWidthFull();

        CommonUtils.commentsFormat(description, 3000);

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSizeFull();
        formLayout.setPadding(false);
        formLayout.setSpacing(false);
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, title, speakers, startTime, endTime, type, room, level, language, track, tags, description);
        return formLayout;
    }

    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {
        try {
            if (this.element == null) this.element = new Session();
            binder.writeBean(this.element);
            ConfirmationDialog.confirmation(event -> {
                save.setEnabled(false);
                try {
                    service.save(element);
                    populateForm(element);
                    refreshList();
                    NotificationUtils.success();
                } catch (InvalidDataAccessApiUsageException ex) {
                    log.error(ex.getMessage());
                    NotificationUtils.error(ex.getMessage());
                } finally {
                    save.setEnabled(true);
                }
            });
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.error(ex.getLocalizedMessage());
            NotificationUtils.error(ex.getMessage());
        } catch (ValidationException ex) {
            log.error(ex.getLocalizedMessage());
            NotificationUtils.error(ex);
        }
    }

    private void delete(ClickEvent<Button> buttonClickEvent) {
        try {
            ConfirmationDialog.delete(event -> {
                service.delete(element.getCode());
                clearForm();
                refreshList();
            });
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.error(ex.getLocalizedMessage());
            NotificationUtils.error("Error delete the data. Somebody else has delete the record while you were making changes.");
        }
    }

    private HorizontalLayout createFooter() {
        HorizontalLayout buttonLayout = new HorizontalLayout(save, cancel, delete);
        buttonLayout.setFlexGrow(1, save, cancel, delete);
        buttonLayout.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.CONTRAST_10);
        buttonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return buttonLayout;
    }

    private void clearForm() {
        populateForm(null);
        list.getDataProvider().refreshAll();
    }

    private void populateForm(Session value) {
        this.element = value;
        this.selectedItem = value;
        binder.readBean(this.element);
        delete.setEnabled(element != null);
    }
}
