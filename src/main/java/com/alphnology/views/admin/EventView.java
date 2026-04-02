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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.HasClearButton;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import static com.alphnology.utils.ViewHelper.*;

@Slf4j
@PageTitle("Event")
@Route("admin/event")
@Menu(order = 10, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class EventView extends VerticalLayout {

    private final TextField name = new TextField("Name");
    private final DatePicker startDate = new DatePicker("Start date");
    private final DatePicker endDate = new DatePicker("End date");
    private final ComboBox<String> timeZone = new ComboBox<>("Select a time zone");
    private final TextArea location = new TextArea("Location");
    private final Paragraph hint = new Paragraph();

    private final Button cancel = new Button("Reset", VaadinIcon.REFRESH.create());
    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());
    private final Button delete = new Button("Delete", VaadinIcon.TRASH.create());

    private final transient EventService service;
    private Event element;

    private final Binder<Event> binder = new BeanValidationBinder<>(Event.class);

    public EventView(EventService service,
                     @Value("${application.formatter.date:unknown}") String formatterDate) {
        this.service = service;

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

        // ── Form panel ──
        Footer footer = new Footer(createFooter());
        Scroller formScroller = getScrollerVertical();
        formScroller.setContent(createFormLayout());
        VerticalLayout form = getSecondaryLayout(formScroller, footer);

        add(form);
        setSizeFull();
        setPadding(false);
        setMargin(false);
        setSpacing(false);

        loadEvent();

        cancel.addClickListener(e -> clearForm());
        save.addClickListener(this::saveOrUpdate);
        delete.addClickListener(this::delete);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
    }

    private VerticalLayout createFormLayout() {
        Header header = getSecondaryHeader("Configure your event", "Maintain the single event that drives schedules, venues, and time zones.");

        name.setWidthFull();
        startDate.setWidthFull();
        endDate.setWidthFull();
        timeZone.setWidthFull();
        location.setWidthFull();
        hint.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        CommonUtils.commentsFormat(location, 100);

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSizeFull();
        formLayout.setPadding(false);
        formLayout.setSpacing(false);
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, hint, name, startDate, endDate, timeZone, location);
        return formLayout;
    }

    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {
        try {
            if (this.element == null) this.element = new Event();
            binder.writeBean(this.element);
            ConfirmationDialog.confirmation(event -> {
                service.save(element);
                populateForm(element);
                updateHint();
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
            if (element == null) {
                NotificationUtils.info("There is no event saved yet.");
                return;
            }
            ConfirmationDialog.delete(event -> {
                service.delete(element.getCode());
                clearForm();
                updateHint();
                NotificationUtils.success("Event deleted successfully.");
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
        Event current = service.findAll().stream().findFirst().orElse(null);
        populateForm(current);
        updateHint();

        applyBrowserTimeZoneForNewEvent();
    }

    private void populateForm(Event value) {
        this.element = value;
        binder.readBean(this.element);
        delete.setEnabled(element != null);
    }

    private void loadEvent() {
        Event current = service.findAll().stream().findFirst().orElse(null);
        populateForm(current);
        updateHint();
        if (current == null) {
            applyBrowserTimeZoneForNewEvent();
        }
    }

    private void updateHint() {
        if (element == null) {
            hint.setText("No event has been created yet. Complete the form and save it to initialize the schedule.");
            return;
        }
        hint.setText("You are editing the active event. This screen intentionally manages a single event record.");
    }

    private void applyBrowserTimeZoneForNewEvent() {
        UI.getCurrent().getPage().executeJs("return Intl.DateTimeFormat().resolvedOptions().timeZone;")
                .then(String.class, id -> {
                    if (id != null && element == null) {
                        ZoneId zoneId = ZoneId.of(id);
                        ZonedDateTime now = ZonedDateTime.now(zoneId);
                        ZoneOffset offset = now.getOffset();
                        timeZone.setValue(String.format("%s (UTC%s)", id, offset));
                    }
                });
    }
}
