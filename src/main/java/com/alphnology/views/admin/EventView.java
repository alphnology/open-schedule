package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.Event;
import com.alphnology.services.EventService;
import com.alphnology.utils.CommonUtils;
import com.alphnology.utils.CountryUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.util.StringUtils;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;

@Slf4j
@PageTitle("Event")
@Route("admin/event")
@Menu(order = 10, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class EventView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final VirtualList<Event> list = new VirtualList<>();
    private final Span countBadge = new Span("0");
    private Event selectedItem;

    private final TextField name = new TextField("Name");
    private final DatePicker startDate = new DatePicker("Start date");
    private final DatePicker endDate = new DatePicker("End date");
    private final ComboBox<String> timeZone = new ComboBox<>("Select a time zone");
    private final TextArea location = new TextArea("Location");

    private final Button cancel = new Button("New", VaadinIcon.FILE_ADD.create());
    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());
    private final Button delete = new Button("Delete", VaadinIcon.TRASH.create());

    private final transient EventService service;
    private Event element;
    private final String formatterDate;

    private final Binder<Event> binder = new BeanValidationBinder<>(Event.class);

    public EventView(EventService service,
                     @Value("${application.formatter.date:unknown}") String formatterDate) {
        this.service = service;
        this.formatterDate = formatterDate;

        timeZone.setItems(CountryUtils.getAvailableZoneIds());
        timeZone.setPlaceholder("Choose a time zone");

        DatePicker.DatePickerI18n dateFormat = new DatePicker.DatePickerI18n();
        dateFormat.setDateFormat(formatterDate);
        dateFormat.setFirstDayOfWeek(1);
        startDate.setI18n(dateFormat);
        endDate.setI18n(dateFormat);

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

        VerticalLayout sidebar = new VerticalLayout(toolbar, list);
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

    private Specification<Event> createFilterSpecification() {
        return (root, query, builder) -> {
            final String search = searchField.getValue().toLowerCase().trim();
            Order order = builder.asc(root.get("startDate"));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);
            Predicate predicateName = predicateUnaccentLike(root, builder, "name", search);
            Predicate predicateTimeZone = predicateUnaccentLike(root, builder, "timeZone", search);
            Predicate predicateLocation = predicateUnaccentLike(root, builder, "location", search);
            final List<Predicate> orPredicates = new ArrayList<>(List.of(predicateName, predicateTimeZone, predicateLocation));
            return builder.or(orPredicates.toArray(Predicate[]::new));
        };
    }

    private void initList() {
        searchField.setWidthFull();
        searchField.setPlaceholder("Search events...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshList());

        list.setSizeFull();
        list.setRenderer(new ComponentRenderer<>(this::buildListItem));
    }

    private Div buildListItem(Event event) {
        Div item = new Div();
        item.addClassName("admin-list-item");
        if (selectedItem != null && Objects.equals(selectedItem.getCode(), event.getCode())) {
            item.addClassName("selected");
        }

        // Icon
        Div iconDiv = new Div();
        iconDiv.addClassName("admin-item-icon");
        Icon icon = VaadinIcon.CALENDAR.create();
        icon.setSize("18px");
        iconDiv.add(icon);

        // Body
        Div body = new Div();
        body.addClassName("admin-item-body");
        Span nameSpan = new Span(event.getName());
        nameSpan.addClassName("admin-item-name");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(formatterDate);
        String startStr = event.getStartDate() != null ? event.getStartDate().format(fmt) : "—";
        String endStr = event.getEndDate() != null ? event.getEndDate().format(fmt) : "—";
        Span subSpan = new Span(startStr + " → " + endStr);
        subSpan.addClassName("admin-item-sub");
        body.add(nameSpan, subSpan);

        // Side: timezone abbreviation
        Div side = new Div();
        side.addClassName("admin-item-side");
        if (StringUtils.hasText(event.getTimeZone())) {
            String tz = event.getTimeZone().contains("(") ? event.getTimeZone().substring(0, event.getTimeZone().indexOf("(")).trim() : event.getTimeZone();
            Span badge = new Span(tz.length() > 12 ? tz.substring(tz.lastIndexOf("/") + 1) : tz);
            badge.addClassNames("admin-badge", "muted");
            side.add(badge);
        }

        item.add(iconDiv, body, side);
        item.addClickListener(e -> selectItem(event));
        return item;
    }

    private void selectItem(Event event) {
        populateForm(event);
        list.getDataProvider().refreshAll();
    }

    private void refreshList() {
        List<Event> items = service.findAll(createFilterSpecification());
        list.setItems(items);
        countBadge.setText(String.valueOf(items.size()));
    }

    private VerticalLayout createFormLayout() {
        Header header = getSecondaryHeader("Configure your event", "Manage schedules, venues, and time zones.");

        name.setWidthFull();
        startDate.setWidthFull();
        endDate.setWidthFull();
        timeZone.setWidthFull();
        location.setWidthFull();

        CommonUtils.commentsFormat(location, 100);

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSizeFull();
        formLayout.setPadding(false);
        formLayout.setSpacing(false);
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, name, startDate, endDate, timeZone, location);
        return formLayout;
    }

    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {
        try {
            if (this.element == null) this.element = new Event();
            binder.writeBean(this.element);
            ConfirmationDialog.confirmation(event -> {
                service.save(element);
                populateForm(element);
                refreshList();
                NotificationUtils.success();
            });
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.error(ex.getLocalizedMessage());
            NotificationUtils.error("Error updating the data. Somebody else has updated the record while you were making changes.");
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

        UI.getCurrent().getPage().executeJs("return Intl.DateTimeFormat().resolvedOptions().timeZone;")
                .then(String.class, id -> {
                    if (id != null) {
                        ZoneId zoneId = ZoneId.of(id);
                        ZonedDateTime now = ZonedDateTime.now(zoneId);
                        ZoneOffset offset = now.getOffset();
                        timeZone.setValue(String.format("%s (UTC%s)", id, offset));
                    }
                });
    }

    private void populateForm(Event value) {
        this.element = value;
        this.selectedItem = value;
        binder.readBean(this.element);
        delete.setEnabled(element != null);
    }
}
