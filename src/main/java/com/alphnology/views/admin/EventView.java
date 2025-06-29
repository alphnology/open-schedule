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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import com.vaadin.flow.data.renderer.LocalDateRenderer;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;
import static org.reflections.Reflections.log;

@PageTitle("Event")
@Route("admin/event")
@Menu(order = 10, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class EventView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final Grid<Event> grid = new Grid<>(Event.class, false);

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

    private final Binder<Event> binder = new BeanValidationBinder<>(Event.class);


    public EventView(
            EventService service,
            @Value("${application.formatter.date:unknown}") String formatterDate
    ) {
        this.service = service;


        timeZone.setItems(CountryUtils.getAvailableZoneIds());
        timeZone.setPlaceholder("Choose a time zone");

        DatePicker.DatePickerI18n dateFormat = new DatePicker.DatePickerI18n();
        dateFormat.setDateFormat(formatterDate);
        dateFormat.setFirstDayOfWeek(1);
        startDate.setI18n(dateFormat);
        endDate.setI18n(dateFormat);

        // Bind fields. This where you'd define e.g. validation rules
        binder.bindInstanceFields(this);
        binder.getFields().forEach(field -> {
            if (field instanceof HasClearButton clear) {
                clear.setClearButtonVisible(true);
            }
        });


        initGrid(formatterDate);

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();

        Footer footer = new Footer(createFooter());
        Scroller formScroller = getScrollerVertical();
        formScroller.setContent(createFormLayout());

        VerticalLayout form = getSecondaryLayout(formScroller, footer);
        form.setWidth("35%");

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

        grid.addColumn(Event::getName).setHeader("Name").setAutoWidth(true).setSortable(true).setSortProperty("name");

        grid.addColumn(new LocalDateRenderer<>(Event::getStartDate, formatterDate)).setHeader("Start date")
                .setComparator(Event::getStartDate).setAutoWidth(true).setSortable(true).setSortProperty("startDate");

        grid.addColumn(new LocalDateRenderer<>(Event::getEndDate, formatterDate)).setHeader("End date")
                .setComparator(Event::getEndDate).setAutoWidth(true).setSortable(true).setSortProperty("endDate");

        grid.addColumn(Event::getTimeZone).setHeader("TimeZone").setAutoWidth(true).setSortable(true).setSortProperty("timeZone");

        grid.addColumn(Event::getLocation).setHeader("Location").setAutoWidth(true).setSortable(true).setSortProperty("location");

        grid.asSingleSelect().addValueChangeListener(event -> populateForm(event.getValue()));
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
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, name, startDate, endDate, timeZone, location);

        return formLayout;
    }


    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {

        try {

            if (this.element == null) {
                this.element = new Event();
            }

            binder.writeBean(this.element);


            ConfirmationDialog.confirmation(event -> {
                service.save(element);

                populateForm(element);

                grid.getDataProvider().refreshAll();
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

        binder.readBean(this.element);

        delete.setEnabled(element != null);
    }

}
