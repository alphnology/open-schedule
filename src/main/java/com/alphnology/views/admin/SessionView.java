package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.*;
import com.alphnology.data.enums.Language;
import com.alphnology.data.enums.Level;
import com.alphnology.data.enums.SessionType;
import com.alphnology.services.*;
import com.alphnology.utils.CommonUtils;
import com.alphnology.utils.NotificationUtils;
import com.alphnology.utils.RendererUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.alphnology.utils.CommonUtils.comboBoxItemFilter;
import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;
import static org.reflections.Reflections.log;

@PageTitle("Sessions")
@Route("admin/session")
@Menu(order = 16, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class SessionView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final Grid<Session> grid = new Grid<>(Session.class, false);

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
    private Session element;

    private final Binder<Session> binder = new BeanValidationBinder<>(Session.class);


    public SessionView(
            SessionService service,
            RoomService roomService,
            TrackService trackService,
            SpeakerService speakerService,
            EventService eventService,
            TagService tagService,
            @Value("${application.formatter.date-time-12:unknown}") String formatterDateTime,
            @Value("${application.formatter.date:unknown}") String formatterDate
    ) {
        this.service = service;


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

        Arrays.asList(startTime, endTime)
                .forEach(c -> {
                    c.setStep(Duration.ofMinutes(60));
                    c.setWeekNumbersVisible(true);
                    c.setDatePickerI18n(new DatePicker.DatePickerI18n().setFirstDayOfWeek(1));
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
        speakers.setItemLabelGenerator(l -> "%s at %s".formatted(l.getName(), l.getCompany()));
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
                            int offset = query.getOffset();
                            int limit = query.getLimit();
                            return tagService.searchTags(filter, offset, limit).stream();
                        },
                        query -> {
                            String filter = query.getFilter().orElse("");
                            return tagService.countTags(filter);
                        }
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


        // Bind fields. This where you'd define e.g. validation rules
        binder.bindInstanceFields(this);
        binder.getFields().forEach(field -> {
            if (field instanceof HasClearButton clear) {
                clear.setClearButtonVisible(true);
            }
        });


        initGrid(formatterDateTime);

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();

        Footer footer = new Footer(createFooter());
        Scroller formScroller = getScrollerVertical();
        formScroller.setContent(createFormLayout());

        VerticalLayout form = getSecondaryLayout(formScroller, footer);
        form.setWidth("50%");

        VerticalLayout gridLayout = new VerticalLayout(searchField, grid);
        gridLayout.setWidthFull();
        gridLayout.getStyle().set("gap", "0.3rem");

        splitLayout.addToPrimary(gridLayout);
        splitLayout.addToSecondary(form);

        add(splitLayout);
        setSizeFull();
        setPadding(false);
        setMargin(false);
        setSpacing(false);

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

            final List<Predicate> orPredicates = new ArrayList<>(List.of(predicateTitle, predicateDescription));

            return builder.or(orPredicates.toArray(Predicate[]::new));

        };
    }

    private void initGrid(String formatterDate) {
        searchField.focus();
        searchField.setWidthFull();
        searchField.setPlaceholder("Search...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.getStyle().setPadding("0").setMargin("0");
        searchField.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setMultiSort(true, Grid.MultiSortPriority.APPEND);
        grid.setEmptyStateText("No record found.");

        grid.setItems(query -> service.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)), createFilterSpecification()).stream());

        grid.addColumn(Session::getTitle).setHeader("Title").setWidth("300px").setSortable(true).setSortProperty("title")
                .setTooltipGenerator(Session::getTitle);

        grid.addColumn(new ComponentRenderer<>(session -> {
            Span localStartTime = new Span(session.getStartTime().format(DateTimeFormatter.ofPattern(formatterDate)));
            Span localEndTime = new Span(session.getEndTime().format(DateTimeFormatter.ofPattern(formatterDate)));

            Div speakerContainer = new Div(localStartTime, localEndTime);
            speakerContainer.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.COLUMN,
                    LumoUtility.Gap.Row.SMALL
            );
            return speakerContainer;
        })).setHeader("Time").setAutoWidth(true).setSortable(true).setSortProperty("startTime", "endTime");

        grid.addColumn(createSpeakerRenderer()).setHeader("Speakers").setAutoWidth(true)
                .setTooltipGenerator(session -> RendererUtils.getSessionSpeakers(session.getSpeakers()));

        grid.addComponentColumn(session -> {
            Image image = new Image();
            if (session.getLanguage() != null) {
                image.setSrc("https://flagcdn.com/%s.svg".formatted(session.getLanguage().name().toLowerCase()));
                image.setWidth("30px");
            }
            return image;
        }).setHeader("Language").setTextAlign(ColumnTextAlign.CENTER);

        grid.addColumn(c -> c.getRoom() != null ? c.getRoom().getName() : "").setHeader("Room").setAutoWidth(true).setSortable(true).setSortProperty("room.name");

        grid.addColumn(c -> c.getTrack() != null ? c.getTrack().getName() : "").setHeader("Track").setAutoWidth(true).setSortable(true).setSortProperty("track.name");

        grid.addColumn(c -> c.getLevel() != null ? c.getLevel().getDisplay() : "").setHeader("Level").setAutoWidth(true).setSortable(true).setSortProperty("level");

        grid.addColumn(c -> c.getType().getDisplay()).setHeader("Type").setAutoWidth(true).setSortable(true).setSortProperty("type");

        grid.asSingleSelect().addValueChangeListener(event -> populateForm(event.getValue()));
    }

    private Renderer<Session> createSpeakerRenderer() {
        return new ComponentRenderer<>(session -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            AvatarGroup avatarGroup = new AvatarGroup();
            avatarGroup.addClassNames(LumoUtility.Width.AUTO);

            Div speakerContainer = new Div();
            speakerContainer.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.FlexDirection.COLUMN,
                    LumoUtility.Width.AUTO,
                    LumoUtility.Gap.Row.SMALL
            );

            session.getSpeakers().forEach(speaker -> {
                AvatarGroup.AvatarGroupItem avatar = new AvatarGroup.AvatarGroupItem(speaker.getName(), speaker.getPhotoUrl());
                avatarGroup.add(avatar);

                speakerContainer.add(new Span(speaker.getName()));
            });


            horizontalLayout.add(avatarGroup, speakerContainer);

            return horizontalLayout;
        });
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
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, title, speakers, startTime, endTime, type, room, level, language, track, tags, description);

        return formLayout;
    }


    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {

        try {

            if (this.element == null) {
                this.element = new Session();
            }

            binder.writeBean(this.element);

            ConfirmationDialog.confirmation(event -> {
                try {
                    service.save(element);

                    populateForm(element);

                    grid.getDataProvider().refreshAll();

                    NotificationUtils.success();
                } catch (InvalidDataAccessApiUsageException ex) {
                    log.error(ex.getMessage());
                    NotificationUtils.error(ex.getMessage());
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

                populateForm(null);

                grid.getDataProvider().refreshAll();
            });
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.error(ex.getLocalizedMessage());
            NotificationUtils.error("Error delete the data. Somebody else has delete the record while you were making changes.");
        }
    }

    private HorizontalLayout createFooter() {

        HorizontalLayout buttonLayout = new HorizontalLayout(save, cancel, delete);
        buttonLayout.setFlexGrow(1, save, cancel, delete);
        buttonLayout.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Padding.MEDIUM);
        buttonLayout.addClassNames(LumoUtility.Background.CONTRAST_10);
        buttonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        return buttonLayout;
    }

    private void clearForm() {
        populateForm(null);

    }


    private void populateForm(Session value) {
        this.element = value;

        binder.readBean(this.element);

        delete.setEnabled(element != null);
    }

}
