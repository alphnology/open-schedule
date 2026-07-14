package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.WorkshopParticipantRegistration;
import com.alphnology.services.WorkshopRegistrationService;
import com.alphnology.services.WorkshopRegistrationSettingsService;
import com.alphnology.utils.NotificationUtils;
import com.alphnology.utils.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;
import org.vaadin.lineawesome.LineAwesomeIconUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@PageTitle("Workshop Registration")
@Route("admin/workshop-registration")
@Menu(order = 20, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class WorkshopRegistrationAdminView extends VerticalLayout {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private static final Logger log = LoggerFactory.getLogger(WorkshopRegistrationAdminView.class);

    private final WorkshopRegistrationSettingsService settingsService;
    private final WorkshopRegistrationService registrationService;
    private final Binder<WorkshopRegistrationSettingsFormData> binder = new Binder<>(WorkshopRegistrationSettingsFormData.class);

    private final Checkbox enabled = new Checkbox("Enable workshop registration");
    private final Checkbox active = new Checkbox("Public flow active");
    private final Checkbox allowAttendeeWorkshopChange = new Checkbox("Allow attendees to change workshops");
    private final Checkbox showPublicMenuEntry = new Checkbox("Show workshop registration in the main menu");
    private final TextField alfioBaseUrl = new TextField("Alf.io base URL");
    private final TextField eventSlug = new TextField("Event slug");
    private final PasswordField token = new PasswordField("Alf.io admin token");
    private final Checkbox clearStoredToken = new Checkbox("Remove stored token");
    private final TextArea publicMessage = new TextArea("Public message");
    private final IntegerField participantWorkshopLimit = new IntegerField("Workshops per attendee");

    private final Button reloadSettings = new Button("Reload", VaadinIcon.REFRESH.create());
    private final Button saveSettings = new Button("Save settings", VaadinIcon.CHECK.create());
    private final Anchor openPublicPage = new Anchor("/workshop-registration", "Open public page");

    private final Paragraph runtimeInfo = new Paragraph();
    private final Span tokenState = new Span();

    private final TextField searchField = new TextField();
    private final ComboBox<WorkshopRegistrationService.WorkshopOption> workshopFilter = new ComboBox<>("Workshop");
    private final Span registrationCount = new Span("0");
    private final Grid<WorkshopParticipantRegistration> registrationsGrid = new Grid<>(WorkshopParticipantRegistration.class, false);

    private final Span selectedRegistrationSummary = new Span("Select a registration to manage it.");
    private final ComboBox<WorkshopRegistrationService.WorkshopOption> moveTarget = new ComboBox<>("Move to workshop");
    private final Button moveRegistration = new Button("Move registration", VaadinIcon.ARROWS_LONG_H.create());
    private final Button deleteRegistration = new Button("Delete registration", VaadinIcon.TRASH.create());

    private WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot snapshot;
    private WorkshopParticipantRegistration selectedRegistration;

    public WorkshopRegistrationAdminView(WorkshopRegistrationSettingsService settingsService,
                                         WorkshopRegistrationService registrationService) {
        this.settingsService = settingsService;
        this.registrationService = registrationService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        configureBinder();
        configureFields();
        configureGrid();
        configureActions();

        Header header = ViewHelper.getSecondaryHeader(
                "Workshop registration",
                "Configure external alf.io order validation and manage workshop registrations without exposing secrets to the public flow."
        );

        VerticalLayout content = new VerticalLayout(
                buildSettingsCard(),
                buildRegistrationsCard(),
                buildSelectionCard()
        );
        content.setPadding(false);
        content.setSpacing(false);
        content.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Gap.MEDIUM, "admin-form-content");
        content.setWidthFull();

        Scroller scroller = ViewHelper.getScrollerVertical();
        scroller.setContent(content);

        add(header, scroller);
        setFlexGrow(1, scroller);

        loadSettings();
        refreshWorkshopOptions();
        refreshRegistrations();
    }

    private void configureBinder() {
        binder.forField(enabled).bind(WorkshopRegistrationSettingsFormData::isEnabled, WorkshopRegistrationSettingsFormData::setEnabled);
        binder.forField(active).bind(WorkshopRegistrationSettingsFormData::isActive, WorkshopRegistrationSettingsFormData::setActive);
        binder.forField(allowAttendeeWorkshopChange).bind(
                WorkshopRegistrationSettingsFormData::isAllowAttendeeWorkshopChange,
                WorkshopRegistrationSettingsFormData::setAllowAttendeeWorkshopChange
        );
        binder.forField(showPublicMenuEntry).bind(
                WorkshopRegistrationSettingsFormData::isShowPublicMenuEntry,
                WorkshopRegistrationSettingsFormData::setShowPublicMenuEntry
        );
        binder.forField(alfioBaseUrl).bind(WorkshopRegistrationSettingsFormData::getAlfioBaseUrl, WorkshopRegistrationSettingsFormData::setAlfioBaseUrl);
        binder.forField(eventSlug).bind(WorkshopRegistrationSettingsFormData::getEventSlug, WorkshopRegistrationSettingsFormData::setEventSlug);
        binder.forField(token).bind(WorkshopRegistrationSettingsFormData::getToken, WorkshopRegistrationSettingsFormData::setToken);
        binder.forField(publicMessage).bind(WorkshopRegistrationSettingsFormData::getPublicMessage, WorkshopRegistrationSettingsFormData::setPublicMessage);
        binder.forField(participantWorkshopLimit).bind(
                WorkshopRegistrationSettingsFormData::getParticipantWorkshopLimit,
                WorkshopRegistrationSettingsFormData::setParticipantWorkshopLimit
        );
    }

    private void configureFields() {
        alfioBaseUrl.setWidthFull();
        alfioBaseUrl.setPlaceholder("https://tickets.jconfdominicana.org");
        alfioBaseUrl.setHelperText("Only the base URL is needed. The admin reservation endpoint is assembled on the backend.");

        eventSlug.setWidthFull();
        eventSlug.setPlaceholder("jd2026");
        eventSlug.setHelperText("This must match the alf.io event slug. The public page resolves the attendee 'Info. del pedido' code against this event.");

        token.setWidthFull();
        token.setRevealButtonVisible(false);
        token.setHelperText("The bearer token is never exposed to the browser. It is encrypted before being stored.");

        clearStoredToken.setHelperText("Clears the encrypted token saved in the database. When checked, the token field is ignored on save and the module will stop validating order codes until a new token is stored later.");

        allowAttendeeWorkshopChange.setHelperText("If enabled, attendees who are already registered can move themselves to another available workshop from the public page.");
        showPublicMenuEntry.setHelperText("If enabled, the public app drawer shows a direct link to the workshop registration page.");

        publicMessage.setWidthFull();
        publicMessage.setMinHeight("120px");
        publicMessage.setMaxLength(1000);
        publicMessage.setHelperText("Optional text shown on the public workshop-registration page.");

        participantWorkshopLimit.setWidthFull();
        participantWorkshopLimit.setMin(1);
        participantWorkshopLimit.setMax(1);
        participantWorkshopLimit.setStepButtonsVisible(true);
        participantWorkshopLimit.setHelperText("The current contract is one active workshop per attendee ticket. This field is fixed at 1 for now.");

        openPublicPage.setTarget("_blank");
        openPublicPage.getElement().setAttribute("theme", "primary");
        openPublicPage.addClassNames("workshop-admin-public-link");

        clearStoredToken.addValueChangeListener(event -> {
            boolean clearing = event.getValue();
            token.setEnabled(!clearing);
            if (clearing) {
                token.clear();
            }
        });

        searchField.setWidthFull();
        searchField.setPlaceholder("Search by attendee, email, order code, ticket public ID, or reservation code...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(event -> refreshRegistrations());

        workshopFilter.setWidth("320px");
        workshopFilter.setItemLabelGenerator(WorkshopRegistrationService.WorkshopOption::displayLabel);
        workshopFilter.setClearButtonVisible(true);
        workshopFilter.addValueChangeListener(event -> refreshRegistrations());

        moveTarget.setWidthFull();
        moveTarget.setItemLabelGenerator(WorkshopRegistrationService.WorkshopOption::displayLabel);
        moveTarget.setHelperText("Choose another workshop to reassign the selected attendee ticket.");
    }

    private void configureGrid() {
        registrationsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        registrationsGrid.setWidthFull();
        registrationsGrid.setHeight("420px");
        registrationsGrid.addColumn(WorkshopParticipantRegistration::getAttendeeName)
                .setHeader("Attendee")
                .setAutoWidth(true)
                .setFlexGrow(1);
        registrationsGrid.addColumn(WorkshopParticipantRegistration::getAttendeeEmail)
                .setHeader("Email")
                .setAutoWidth(true)
                .setFlexGrow(1);
        registrationsGrid.addColumn(WorkshopParticipantRegistration::getReservationShortCode)
                .setHeader("Reservation")
                .setAutoWidth(true);
        registrationsGrid.addColumn(WorkshopParticipantRegistration::getOrderReference)
                .setHeader("Info. del pedido")
                .setAutoWidth(true);
        registrationsGrid.addColumn(WorkshopParticipantRegistration::getTicketPublicId)
                .setHeader("Ticket public ID")
                .setAutoWidth(true);
        registrationsGrid.addColumn(registration -> registration.getSession().getTitle())
                .setHeader("Workshop")
                .setAutoWidth(true)
                .setFlexGrow(1);
        registrationsGrid.addColumn(registration -> registration.getSession().getRoom() != null
                        ? registration.getSession().getRoom().getName()
                        : "Room TBD")
                .setHeader("Room")
                .setAutoWidth(true);
        registrationsGrid.addColumn(registration -> registration.getStatus().name())
                .setHeader("Status")
                .setAutoWidth(true);
        registrationsGrid.addColumn(registration -> registration.getRegisteredAt() != null
                        ? registration.getRegisteredAt().format(DATE_TIME_FORMATTER)
                        : "")
                .setHeader("Registered")
                .setAutoWidth(true);
        registrationsGrid.asSingleSelect().addValueChangeListener(event -> {
            selectedRegistration = event.getValue();
            updateSelectionState();
        });
    }

    private void configureActions() {
        reloadSettings.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        saveSettings.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        moveRegistration.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deleteRegistration.addThemeVariants(ButtonVariant.LUMO_ERROR);

        reloadSettings.addClickListener(event -> loadSettings());
        saveSettings.addClickListener(event -> saveSettings());
        moveRegistration.addClickListener(event -> moveSelectedRegistration());
        deleteRegistration.addClickListener(event -> deleteSelectedRegistration());
    }

    private Component buildSettingsCard() {
        runtimeInfo.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        tokenState.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        HorizontalLayout toggles = new HorizontalLayout(enabled, active, allowAttendeeWorkshopChange, showPublicMenuEntry);
        toggles.setWidthFull();
        toggles.setWrap(true);
        toggles.setAlignItems(FlexComponent.Alignment.CENTER);
        toggles.addClassNames(LumoUtility.Gap.LARGE);

        HorizontalLayout actions = new HorizontalLayout(reloadSettings, saveSettings, openPublicPage);
        actions.setWrap(true);
        actions.addClassNames(LumoUtility.Gap.SMALL);

        VerticalLayout content = new VerticalLayout(
                toggles,
                createGrid(alfioBaseUrl, eventSlug),
                createSingleColumnGrid(token),
                clearStoredToken,
                createSingleColumnGrid(publicMessage),
                createGrid(participantWorkshopLimit),
                runtimeInfo,
                tokenState,
                actions
        );
        content.setPadding(false);
        content.setSpacing(false);
        content.addClassNames(LumoUtility.Gap.SMALL, "admin-mail-section");

        Div card = createCard(
                "External validation settings",
                "This module validates alf.io order codes on the backend and then binds each attendee ticket to one workshop in Open Schedule."
        );
        card.add(content);
        return card;
    }

    private Component buildRegistrationsCard() {
        registrationCount.addClassName("admin-count-badge");

        HorizontalLayout toolbar = new HorizontalLayout(searchField, workshopFilter, registrationCount);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, registrationCount);
        toolbar.setFlexGrow(1, searchField);
        toolbar.addClassNames("admin-workshop-toolbar");

        VerticalLayout content = new VerticalLayout(toolbar, registrationsGrid);
        content.setPadding(false);
        content.setSpacing(false);
        content.addClassNames(LumoUtility.Gap.SMALL);

        Div card = createCard(
                "Participant registrations",
                "Search registrations, review the workshop currently assigned to each attendee ticket, and filter the grid by workshop."
        );
        card.add(content);
        return card;
    }

    private Component buildSelectionCard() {
        selectedRegistrationSummary.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        HorizontalLayout actions = new HorizontalLayout(moveRegistration, deleteRegistration);
        actions.setWrap(true);
        actions.addClassNames(LumoUtility.Gap.SMALL);

        VerticalLayout content = new VerticalLayout(selectedRegistrationSummary, moveTarget, actions);
        content.setPadding(false);
        content.setSpacing(false);
        content.addClassNames(LumoUtility.Gap.SMALL);

        Div card = createCard(
                "Selected registration",
                "Move the selected attendee to another workshop or delete the registration so that participant can register again."
        );
        card.add(content);
        return card;
    }

    private void loadSettings() {
        snapshot = settingsService.getEffectiveSettings();
        WorkshopRegistrationSettingsFormData formData = WorkshopRegistrationSettingsFormData.fromSnapshot(snapshot);
        binder.readBean(formData);
        clearStoredToken.setValue(false);
        token.clear();
        runtimeInfo.setText(buildRuntimeInfo(snapshot));
        tokenState.setText(buildTokenState(snapshot));
        token.setEnabled(true);
    }

    private void saveSettings() {
        try {
            WorkshopRegistrationSettingsFormData formData = new WorkshopRegistrationSettingsFormData();
            binder.writeBean(formData);

            List<String> issues = validateForm(formData);
            if (!issues.isEmpty()) {
                NotificationUtils.error(String.join(" ", issues));
                return;
            }

            settingsService.save(formData.toUpdateRequest(clearStoredToken.getValue()));
            NotificationUtils.success("Workshop registration settings saved successfully.");
            loadSettings();
        } catch (ValidationException ex) {
            NotificationUtils.error("Please review the highlighted fields.", ex);
        } catch (IllegalStateException ex) {
            log.warn("Workshop registration settings rejected: {}", ex.getMessage());
            NotificationUtils.error(ex.getMessage());
        } catch (Exception ex) {
            log.error("Could not save workshop registration settings from admin view", ex);
            NotificationUtils.error("Could not save the workshop registration settings.");
        }
    }

    private List<String> validateForm(WorkshopRegistrationSettingsFormData formData) {
        List<String> issues = new ArrayList<>();
        if (formData.getParticipantWorkshopLimit() == null || formData.getParticipantWorkshopLimit() != 1) {
            issues.add("The current module supports exactly one active workshop per attendee ticket, so the limit must remain at 1.");
        }
        if (!formData.isEnabled()) {
            return issues;
        }
        if (!StringUtils.hasText(formData.getAlfioBaseUrl())) {
            issues.add("Alf.io base URL is required when the module is enabled.");
        }
        if (!StringUtils.hasText(formData.getEventSlug())) {
            issues.add("Event slug is required when the module is enabled.");
        }
        if (clearStoredToken.getValue() && StringUtils.hasText(formData.getToken())) {
            issues.add("Remove stored token cannot be used while entering a new token in the same save action.");
        }
        boolean hasNewToken = StringUtils.hasText(formData.getToken());
        boolean hasStoredToken = snapshot != null && StringUtils.hasText(snapshot.token()) && !clearStoredToken.getValue();
        if (!hasNewToken && !hasStoredToken) {
            issues.add("An Alf.io admin token is required when the module is enabled.");
        }
        return issues;
    }

    private void refreshWorkshopOptions() {
        List<WorkshopRegistrationService.WorkshopOption> workshops = registrationService.listAllWorkshopOptions();
        workshopFilter.setItems(workshops);
        moveTarget.setItems(workshops);
    }

    private void refreshRegistrations() {
        Long workshopId = workshopFilter.getValue() != null ? workshopFilter.getValue().sessionCode() : null;
        List<WorkshopParticipantRegistration> items = registrationService.listRegistrations(searchField.getValue(), workshopId);
        registrationsGrid.setItems(items);
        registrationCount.setText(String.valueOf(items.size()));

        if (selectedRegistration == null) {
            updateSelectionState();
            return;
        }

        selectedRegistration = items.stream()
                .filter(item -> Objects.equals(item.getCode(), selectedRegistration.getCode()))
                .findFirst()
                .orElse(null);
        registrationsGrid.asSingleSelect().setValue(selectedRegistration);
        updateSelectionState();
    }

    private void updateSelectionState() {
        boolean hasSelection = selectedRegistration != null;
        moveRegistration.setEnabled(hasSelection);
        deleteRegistration.setEnabled(hasSelection);
        moveTarget.setEnabled(hasSelection);

        if (!hasSelection) {
            selectedRegistrationSummary.setText("Select a registration to manage it.");
            moveTarget.clear();
            return;
        }

        selectedRegistrationSummary.setText(
                "%s · %s · currently assigned to %s".formatted(
                        selectedRegistration.getAttendeeName(),
                        selectedRegistration.getOrderReference(),
                        selectedRegistration.getSession().getTitle()
                )
        );
        moveTarget.setValue(moveTarget.getListDataView().getItems()
                .filter(option -> Objects.equals(option.sessionCode(), selectedRegistration.getSession().getCode()))
                .findFirst()
                .orElse(null));
    }

    private void moveSelectedRegistration() {
        if (selectedRegistration == null || moveTarget.getValue() == null) {
            NotificationUtils.warning("Select a registration and a destination workshop first.");
            return;
        }
        if (Objects.equals(selectedRegistration.getSession().getCode(), moveTarget.getValue().sessionCode())) {
            NotificationUtils.info("The selected registration is already assigned to that workshop.");
            return;
        }
        try {
            registrationService.moveRegistration(selectedRegistration.getCode(), moveTarget.getValue().sessionCode());
            NotificationUtils.success("Workshop registration updated.");
            refreshRegistrations();
        } catch (WorkshopRegistrationService.WorkshopRegistrationException ex) {
            NotificationUtils.error(ex.getMessage());
        } catch (Exception ex) {
            NotificationUtils.error("Could not move the selected registration.");
        }
    }

    private void deleteSelectedRegistration() {
        if (selectedRegistration == null) {
            NotificationUtils.warning("Select a registration first.");
            return;
        }
        ConfirmationDialog.delete(event -> {
            registrationService.deleteRegistration(selectedRegistration.getCode());
            NotificationUtils.success("Workshop registration deleted.");
            selectedRegistration = null;
            refreshRegistrations();
        }, new Text("This will allow this attendee ticket to register again."));
    }

    private String buildRuntimeInfo(WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot effective) {
        return "Module status: " + (effective.enabled() ? "enabled" : "disabled")
                + " | public flow: " + (effective.active() ? "active" : "inactive")
                + " | self-service changes: " + (effective.allowAttendeeWorkshopChange() ? "enabled" : "disabled")
                + " | menu shortcut: " + (effective.showPublicMenuEntry() ? "visible" : "hidden")
                + " | configuration: " + (effective.isConfigured() ? "ready" : "incomplete");
    }

    private String buildTokenState(WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot effective) {
        if (effective.tokenPersisted()) {
            return "An encrypted alf.io token is stored in the database.";
        }
        if (effective.tokenPersistenceEnabled()) {
            return "No token is currently stored. Save one to activate the public order-validation flow.";
        }
        return "Encrypted UI secret persistence is disabled. Configure application.secrets.master-key and allow-ui-persistence=true before storing the alf.io token from the admin UI.";
    }

    private Div createCard(String title, String description) {
        H3 header = new H3(title);
        header.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.LARGE);

        Paragraph copy = new Paragraph(description);
        copy.addClassNames(LumoUtility.Margin.Top.XSMALL, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.TextColor.SECONDARY);

        Div card = new Div(header, copy);
        card.addClassNames(
                "admin-card",
                LumoUtility.Padding.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL
        );
        card.setWidthFull();
        return card;
    }

    private Div createGrid(Component... components) {
        Div grid = new Div(components);
        grid.addClassName("admin-mail-grid");
        return grid;
    }

    private Div createSingleColumnGrid(Component... components) {
        Div grid = new Div(components);
        grid.addClassNames("admin-mail-grid", "admin-mail-grid-single");
        return grid;
    }

    public static class WorkshopRegistrationSettingsFormData {

        private boolean enabled;
        private boolean active = true;
        private boolean allowAttendeeWorkshopChange;
        private boolean showPublicMenuEntry;

        @Size(max = 255)
        private String alfioBaseUrl;

        @Size(max = 100)
        private String eventSlug;

        private String token;

        @Size(max = 1000)
        private String publicMessage;

        @Min(1)
        @Max(1)
        private Integer participantWorkshopLimit = 1;

        public static WorkshopRegistrationSettingsFormData fromSnapshot(WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsSnapshot snapshot) {
            WorkshopRegistrationSettingsFormData data = new WorkshopRegistrationSettingsFormData();
            data.setEnabled(snapshot.enabled());
            data.setActive(snapshot.active());
            data.setAllowAttendeeWorkshopChange(snapshot.allowAttendeeWorkshopChange());
            data.setShowPublicMenuEntry(snapshot.showPublicMenuEntry());
            data.setAlfioBaseUrl(snapshot.alfioBaseUrl());
            data.setEventSlug(snapshot.eventSlug());
            data.setPublicMessage(snapshot.publicMessage());
            data.setParticipantWorkshopLimit(snapshot.participantWorkshopLimit() != null ? snapshot.participantWorkshopLimit() : 1);
            return data;
        }

        public WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsUpdateRequest toUpdateRequest(boolean clearStoredToken) {
            return new WorkshopRegistrationSettingsService.WorkshopRegistrationSettingsUpdateRequest(
                    enabled,
                    active,
                    alfioBaseUrl,
                    eventSlug,
                    token,
                    clearStoredToken,
                    publicMessage,
                    allowAttendeeWorkshopChange,
                    showPublicMenuEntry,
                    participantWorkshopLimit
            );
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getAlfioBaseUrl() {
            return alfioBaseUrl;
        }

        public boolean isAllowAttendeeWorkshopChange() {
            return allowAttendeeWorkshopChange;
        }

        public void setAllowAttendeeWorkshopChange(boolean allowAttendeeWorkshopChange) {
            this.allowAttendeeWorkshopChange = allowAttendeeWorkshopChange;
        }

        public boolean isShowPublicMenuEntry() {
            return showPublicMenuEntry;
        }

        public void setShowPublicMenuEntry(boolean showPublicMenuEntry) {
            this.showPublicMenuEntry = showPublicMenuEntry;
        }

        public void setAlfioBaseUrl(String alfioBaseUrl) {
            this.alfioBaseUrl = alfioBaseUrl;
        }

        public String getEventSlug() {
            return eventSlug;
        }

        public void setEventSlug(String eventSlug) {
            this.eventSlug = eventSlug;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getPublicMessage() {
            return publicMessage;
        }

        public void setPublicMessage(String publicMessage) {
            this.publicMessage = publicMessage;
        }

        public Integer getParticipantWorkshopLimit() {
            return participantWorkshopLimit;
        }

        public void setParticipantWorkshopLimit(Integer participantWorkshopLimit) {
            this.participantWorkshopLimit = participantWorkshopLimit;
        }
    }
}
