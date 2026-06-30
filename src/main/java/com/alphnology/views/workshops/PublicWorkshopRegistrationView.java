package com.alphnology.views.workshops;

import com.alphnology.data.WorkshopParticipantRegistration;
import com.alphnology.services.WorkshopRegistrationService;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Workshop Registration")
@Route("workshop-registration")
@AnonymousAllowed
@Slf4j
public class PublicWorkshopRegistrationView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final WorkshopRegistrationService registrationService;

    private final TextField ticketReference = new TextField("Info. del pedido");
    private final Button validateTicket = new Button("Validate code");
    private final Button clear = new Button("Clear");

    private final TextField attendeeName = new TextField("Attendee");
    private final EmailField attendeeEmail = new EmailField("Email");
    private final TextField reservationStatus = new TextField("Reservation status");
    private final TextField reservationShortCode = new TextField("Info. del pedido");
    private final ComboBox<WorkshopRegistrationService.WorkshopOption> workshop = new ComboBox<>("Available workshops");
    private final Button confirmRegistration = new Button("Confirm workshop registration");
    private final Paragraph changePolicyMessage = new Paragraph();
    private VerticalLayout detailsSection;

    private final Paragraph stateMessage = new Paragraph();
    private final Div registrationResult = new Div();

    private WorkshopRegistrationService.ValidatedTicketView validatedTicket;
    private WorkshopRegistrationService.ExistingRegistrationView existingRegistration;
    private WorkshopRegistrationService.PublicModuleState moduleState;
    private Long preferredWorkshopId;

    public PublicWorkshopRegistrationView(WorkshopRegistrationService registrationService) {
        this.registrationService = registrationService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassNames("public-workshop-page", LumoUtility.AlignItems.CENTER, LumoUtility.Padding.LARGE);

        configureFields();
        configureActions();

        add(buildShell());
        refreshModuleState();
        resetResultState();
    }

    private void configureFields() {
        ticketReference.setWidthFull();
        ticketReference.setPlaceholder("Enter the 8-character code shown in 'Info. del pedido'");
        ticketReference.setHelperText("Use the code shown as 'Info. del pedido' on your alf.io PDF ticket, for example BDB04C39.");

        attendeeName.setWidthFull();
        attendeeName.setReadOnly(true);
        attendeeEmail.setWidthFull();
        attendeeEmail.setReadOnly(true);
        reservationStatus.setWidthFull();
        reservationStatus.setReadOnly(true);
        reservationShortCode.setWidthFull();
        reservationShortCode.setReadOnly(true);

        workshop.setWidthFull();
        workshop.setItemLabelGenerator(WorkshopRegistrationService.WorkshopOption::displayLabel);
        workshop.setHelperText("Only sessions marked as workshops and with remaining capacity are shown.");
        changePolicyMessage.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        changePolicyMessage.setVisible(false);

        validateTicket.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmRegistration.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        registrationResult.setWidthFull();
        registrationResult.addClassNames("public-workshop-result");
    }

    private void configureActions() {
        validateTicket.addClickListener(event -> validateTicketReference());
        confirmRegistration.addClickListener(event -> registerSelectedWorkshop());
        clear.addClickListener(event -> {
            ticketReference.clear();
            resetResultState();
            refreshModuleState();
        });
    }

    private Component buildShell() {
        H2 title = new H2("Workshop registration");
        title.addClassNames("public-page-header", LumoUtility.Margin.NONE);

        Paragraph intro = new Paragraph(
                "Validate your alf.io purchase code and reserve your workshop seat without contacting the event team."
        );
        intro.addClassNames(LumoUtility.Margin.Top.XSMALL, LumoUtility.TextColor.SECONDARY);

        HorizontalLayout actions = new HorizontalLayout(validateTicket, clear);
        actions.setWrap(true);
        actions.addClassNames(LumoUtility.Gap.SMALL);

        detailsSection = new VerticalLayout(
                createDetailsGrid(attendeeName, attendeeEmail),
                createDetailsGrid(reservationStatus, reservationShortCode),
                changePolicyMessage,
                workshop,
                confirmRegistration
        );
        detailsSection.setPadding(false);
        detailsSection.setSpacing(false);
        detailsSection.addClassNames(LumoUtility.Gap.SMALL, "public-workshop-form-details");
        detailsSection.setVisible(false);

        VerticalLayout card = new VerticalLayout(
                title,
                intro,
                stateMessage,
                ticketReference,
                actions,
                detailsSection,
                registrationResult
        );
        card.setWidthFull();
        card.setMaxWidth("880px");
        card.setPadding(true);
        card.setSpacing(false);
        card.addClassNames("admin-card", "public-workshop-card", LumoUtility.Gap.SMALL);
        return card;
    }

    private Div createDetailsGrid(Component... components) {
        Div grid = new Div(components);
        grid.addClassNames("admin-mail-grid", "public-workshop-grid");
        return grid;
    }

    private void refreshModuleState() {
        moduleState = registrationService.getPublicModuleState();
        if (moduleState.isAvailable()) {
            stateMessage.setText(moduleState.publicMessage() != null
                    ? moduleState.publicMessage()
                    : "Enter your 'Info. del pedido' code to validate your reservation and see the available workshops.");
            ticketReference.setEnabled(true);
            validateTicket.setEnabled(true);
            return;
        }

        ticketReference.setEnabled(false);
        validateTicket.setEnabled(false);
        stateMessage.setText(moduleState.publicMessage() != null
                ? moduleState.publicMessage()
                : "Workshop registration is currently unavailable. Please contact the event team.");
    }

    private void validateTicketReference() {
        try {
            WorkshopRegistrationService.TicketValidationOutcome outcome = registrationService.validateTicket(ticketReference.getValue());
            if (outcome instanceof WorkshopRegistrationService.TicketValidationOutcome.AlreadyRegistered alreadyRegistered) {
                validatedTicket = null;
                showExistingRegistration(alreadyRegistered.registration());
                NotificationUtils.info("This 'Info. del pedido' code is already registered in a workshop.");
                return;
            }

            WorkshopRegistrationService.ValidatedTicketView ticket =
                    ((WorkshopRegistrationService.TicketValidationOutcome.Validated) outcome).ticket();
            validatedTicket = ticket;
            attendeeName.setValue(ticket.attendeeName());
            attendeeEmail.setValue(ticket.attendeeEmail());
            reservationStatus.setValue(ticket.reservationStatus());
            reservationShortCode.setValue(ticket.reservationShortCode() != null ? ticket.reservationShortCode() : "");
            detailsSection.setVisible(true);
            existingRegistration = null;
            changePolicyMessage.setVisible(false);
            workshop.setItems(ticket.availableWorkshops());
            applyPreferredWorkshopSelection(ticket.availableWorkshops());
            workshop.setEnabled(!ticket.availableWorkshops().isEmpty());
            confirmRegistration.setEnabled(!ticket.availableWorkshops().isEmpty());
            confirmRegistration.setText("Confirm workshop registration");
            registrationResult.removeAll();

            if (ticket.availableWorkshops().isEmpty()) {
                registrationResult.add(createResultNotice(
                        "No workshop seats are currently available.",
                        "Your ticket is valid, but every workshop is at capacity right now."
                ));
            }
        } catch (WorkshopRegistrationService.WorkshopRegistrationException ex) {
            resetResultState();
            NotificationUtils.error(ex.getMessage());
        } catch (Exception ex) {
            resetResultState();
            log.error("Workshop ticket validation failed for reference '{}'", ticketReference.getValue(), ex);
            NotificationUtils.error("We could not validate this 'Info. del pedido' code right now. Please try again later.");
        }
    }

    private void registerSelectedWorkshop() {
        if (validatedTicket == null) {
            NotificationUtils.warning("Validate the 'Info. del pedido' code before selecting a workshop.");
            return;
        }
        if (workshop.getValue() == null) {
            NotificationUtils.warning("Select one workshop before confirming the registration.");
            return;
        }
        try {
            WorkshopParticipantRegistration registration = existingRegistration != null
                    ? registrationService.changeWorkshop(existingRegistration.ticketReference(), workshop.getValue().sessionCode())
                    : registrationService.registerWorkshop(validatedTicket.ticketReference(), workshop.getValue().sessionCode());
            registrationResult.removeAll();
            registrationResult.add(createResultNotice(
                    existingRegistration != null ? "Workshop updated." : "Workshop registration confirmed.",
                    "You are registered in %s on %s.".formatted(
                            registration.getSession().getTitle(),
                            registration.getRegisteredAt().format(DATE_TIME_FORMATTER)
                    )
            ));
            NotificationUtils.success(existingRegistration != null
                    ? "Your workshop selection was updated successfully."
                    : "Your workshop registration was saved successfully.");
            confirmRegistration.setEnabled(false);
            workshop.setEnabled(false);
        } catch (WorkshopRegistrationService.DuplicateWorkshopRegistrationException ex) {
            showExistingRegistration(registrationService.getRegistration(ex.getExistingRegistration().getCode())
                    .map(registrationService::toExistingRegistrationForUi)
                    .orElse(null));
            NotificationUtils.info("This 'Info. del pedido' code is already registered in a workshop.");
        } catch (WorkshopRegistrationService.WorkshopRegistrationException ex) {
            NotificationUtils.error(ex.getMessage());
        } catch (Exception ex) {
            log.error("Workshop registration failed for reference '{}' and workshop '{}'",
                    validatedTicket.ticketReference(),
                    workshop.getValue() != null ? workshop.getValue().sessionCode() : null,
                    ex);
            NotificationUtils.error("We could not complete the workshop registration.");
        }
    }

    private void showExistingRegistration(WorkshopRegistrationService.ExistingRegistrationView registration) {
        resetResultState();
        if (registration == null) {
            return;
        }
        existingRegistration = registration;
        registrationResult.add(createResultNotice(
                "This code is already registered.",
                "%s is already assigned to %s in %s."
                        .formatted(registration.attendeeName(), registration.workshop().title(), registration.workshop().roomName())
        ));
        detailsSection.setVisible(true);
        attendeeName.setValue(registration.attendeeName());
        attendeeEmail.setValue(registration.attendeeEmail());
        reservationStatus.setValue(registration.alfioReservationStatus() != null ? registration.alfioReservationStatus() : "");
        reservationShortCode.setValue(registration.reservationShortCode() != null ? registration.reservationShortCode() : "");
        if (moduleState != null && moduleState.allowAttendeeWorkshopChange()) {
            List<WorkshopRegistrationService.WorkshopOption> options =
                    registrationService.listWorkshopOptionsForExistingRegistration(registration.registrationCode());
            workshop.setItems(options);
            if (preferredWorkshopId != null) {
                applyWorkshopSelection(preferredWorkshopId, options);
                if (workshop.getValue() == null) {
                    applyWorkshopSelection(registration.workshop().sessionCode(), options);
                }
            } else {
                applyWorkshopSelection(registration.workshop().sessionCode(), options);
            }
            workshop.setEnabled(!options.isEmpty());
            confirmRegistration.setEnabled(!options.isEmpty());
            confirmRegistration.setText("Change workshop");
            changePolicyMessage.setText("You may change your workshop while seats remain available.");
            changePolicyMessage.setVisible(true);
        } else {
            workshop.setItems(List.of(registration.workshop()));
            applyWorkshopSelection(registration.workshop().sessionCode(), List.of(registration.workshop()));
            workshop.setEnabled(false);
            confirmRegistration.setEnabled(false);
            confirmRegistration.setText("Confirm workshop registration");
            changePolicyMessage.setText("If you need to change workshops, please contact the event organization so they can assist you.");
            changePolicyMessage.setVisible(true);
        }
    }

    private Component createResultNotice(String title, String description) {
        H3 heading = new H3(title);
        heading.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.LARGE);

        Paragraph copy = new Paragraph(description);
        copy.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.SECONDARY);

        Div card = new Div(heading, copy);
        card.addClassNames("public-workshop-result-card");
        return card;
    }

    private void resetResultState() {
        validatedTicket = null;
        existingRegistration = null;
        attendeeName.clear();
        attendeeEmail.clear();
        reservationStatus.clear();
        reservationShortCode.clear();
        changePolicyMessage.setText("");
        changePolicyMessage.setVisible(false);
        workshop.clear();
        workshop.setItems(List.of());
        workshop.setEnabled(false);
        confirmRegistration.setEnabled(false);
        confirmRegistration.setText("Confirm workshop registration");
        detailsSection.setVisible(false);
        registrationResult.removeAll();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        preferredWorkshopId = event.getLocation().getQueryParameters().getParameters().getOrDefault("workshop", List.of())
                .stream()
                .findFirst()
                .flatMap(this::parseWorkshopId)
                .orElse(null);
    }

    private void applyPreferredWorkshopSelection(List<WorkshopRegistrationService.WorkshopOption> options) {
        if (preferredWorkshopId != null) {
            applyWorkshopSelection(preferredWorkshopId, options);
            return;
        }
        workshop.clear();
    }

    private void applyWorkshopSelection(Long workshopId, List<WorkshopRegistrationService.WorkshopOption> options) {
        workshop.setValue(options.stream()
                .filter(option -> option.sessionCode().equals(workshopId))
                .findFirst()
                .orElse(null));
    }

    private java.util.Optional<Long> parseWorkshopId(String value) {
        try {
            return java.util.Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }
}
