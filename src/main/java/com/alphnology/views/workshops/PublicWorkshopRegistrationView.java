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
import java.util.Objects;

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

    private final TextField reservationStatus = new TextField("Reservation status");
    private final TextField reservationShortCode = new TextField("Info. del pedido");
    private final ComboBox<WorkshopRegistrationService.ParticipantRegistrationView> participant = new ComboBox<>("Attendee / ticket");
    private final TextField attendeeName = new TextField("Attendee");
    private final EmailField attendeeEmail = new EmailField("Email");
    private final TextField ticketPublicId = new TextField("Ticket public ID");
    private final ComboBox<WorkshopRegistrationService.WorkshopOption> workshop = new ComboBox<>("Available workshops");
    private final Button confirmRegistration = new Button("Confirm workshop registration");
    private final Paragraph changePolicyMessage = new Paragraph();
    private final Div participantsSummary = new Div();
    private VerticalLayout detailsSection;

    private final Paragraph stateMessage = new Paragraph();
    private final Div registrationResult = new Div();

    private WorkshopRegistrationService.ValidatedOrderView validatedOrder;
    private WorkshopRegistrationService.ParticipantRegistrationView selectedParticipant;
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

        reservationStatus.setWidthFull();
        reservationStatus.setReadOnly(true);
        reservationShortCode.setWidthFull();
        reservationShortCode.setReadOnly(true);

        participant.setWidthFull();
        participant.setItemLabelGenerator(item -> "%s · %s".formatted(item.attendeeName(), item.attendeeEmail()));
        participant.setHelperText("Select which attendee from this order you want to register.");

        attendeeName.setWidthFull();
        attendeeName.setReadOnly(true);
        attendeeEmail.setWidthFull();
        attendeeEmail.setReadOnly(true);
        ticketPublicId.setWidthFull();
        ticketPublicId.setReadOnly(true);

        workshop.setWidthFull();
        workshop.setItemLabelGenerator(WorkshopRegistrationService.WorkshopOption::displayLabel);
        workshop.setHelperText("Only sessions marked as workshops and with remaining capacity are shown.");

        changePolicyMessage.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        changePolicyMessage.setVisible(false);

        participantsSummary.addClassNames("public-workshop-result", "public-workshop-participants");
        participantsSummary.setWidthFull();

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
        participant.addValueChangeListener(event -> applyParticipantSelection(event.getValue()));
    }

    private Component buildShell() {
        H2 title = new H2("Workshop registration");
        title.addClassNames("public-page-header", LumoUtility.Margin.NONE);

        Paragraph intro = new Paragraph(
                "Validate your alf.io purchase code and reserve workshop seats for each attendee included in the order."
        );
        intro.addClassNames(LumoUtility.Margin.Top.XSMALL, LumoUtility.TextColor.SECONDARY);

        HorizontalLayout actions = new HorizontalLayout(validateTicket, clear);
        actions.setWrap(true);
        actions.addClassNames(LumoUtility.Gap.SMALL);

        detailsSection = new VerticalLayout(
                createDetailsGrid(reservationStatus, reservationShortCode),
                participant,
                createDetailsGrid(attendeeName, attendeeEmail),
                createSingleColumnGrid(ticketPublicId),
                changePolicyMessage,
                workshop,
                confirmRegistration,
                participantsSummary
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
        card.setMaxWidth("980px");
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

    private Div createSingleColumnGrid(Component... components) {
        Div grid = new Div(components);
        grid.addClassNames("admin-mail-grid", "admin-mail-grid-single");
        return grid;
    }

    private void refreshModuleState() {
        moduleState = registrationService.getPublicModuleState();
        if (moduleState.isAvailable()) {
            stateMessage.setText(moduleState.publicMessage() != null
                    ? moduleState.publicMessage()
                    : "Enter your 'Info. del pedido' code to validate the order and see the attendees included in it.");
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
            validatedOrder = registrationService.validateOrder(ticketReference.getValue());
            reservationStatus.setValue(validatedOrder.reservationStatus());
            reservationShortCode.setValue(validatedOrder.reservationShortCode() != null ? validatedOrder.reservationShortCode() : "");
            participant.setItems(validatedOrder.participants());
            detailsSection.setVisible(true);
            registrationResult.removeAll();
            renderParticipantsSummary(validatedOrder.participants());

            if (validatedOrder.participants().isEmpty()) {
                NotificationUtils.warning("No attendees were found for this order.");
                return;
            }

            WorkshopRegistrationService.ParticipantRegistrationView selected = resolvePreferredParticipant(validatedOrder.participants());
            participant.setValue(selected);
            if (participant.getValue() == null) {
                participant.setValue(validatedOrder.participants().getFirst());
            }
        } catch (WorkshopRegistrationService.WorkshopRegistrationException ex) {
            resetResultState();
            NotificationUtils.error(ex.getMessage());
        } catch (Exception ex) {
            resetResultState();
            log.error("Workshop order validation failed for reference '{}'", ticketReference.getValue(), ex);
            NotificationUtils.error("We could not validate this 'Info. del pedido' code right now. Please try again later.");
        }
    }

    private WorkshopRegistrationService.ParticipantRegistrationView resolvePreferredParticipant(
            List<WorkshopRegistrationService.ParticipantRegistrationView> participants
    ) {
        if (preferredWorkshopId == null) {
            return participants.stream()
                    .filter(item -> !item.isRegistered())
                    .findFirst()
                    .orElse(participants.getFirst());
        }
        return participants.stream()
                .filter(item -> item.availableWorkshops().stream()
                        .anyMatch(option -> Objects.equals(option.sessionCode(), preferredWorkshopId)))
                .findFirst()
                .orElse(participants.stream().filter(item -> !item.isRegistered()).findFirst().orElse(participants.getFirst()));
    }

    private void applyParticipantSelection(WorkshopRegistrationService.ParticipantRegistrationView participantView) {
        selectedParticipant = participantView;
        if (participantView == null) {
            attendeeName.clear();
            attendeeEmail.clear();
            ticketPublicId.clear();
            workshop.clear();
            workshop.setItems(List.of());
            workshop.setEnabled(false);
            confirmRegistration.setEnabled(false);
            changePolicyMessage.setVisible(false);
            return;
        }

        attendeeName.setValue(participantView.attendeeName());
        attendeeEmail.setValue(participantView.attendeeEmail());
        ticketPublicId.setValue(participantView.ticketPublicId());
        workshop.setItems(participantView.availableWorkshops());
        changePolicyMessage.setVisible(false);

        if (participantView.isRegistered()) {
            WorkshopRegistrationService.ExistingRegistrationView existing = participantView.existingRegistration();
            if (moduleState != null && moduleState.allowAttendeeWorkshopChange()) {
                applyWorkshopSelection(existing.workshop().sessionCode(), participantView.availableWorkshops());
                workshop.setEnabled(!participantView.availableWorkshops().isEmpty());
                confirmRegistration.setEnabled(!participantView.availableWorkshops().isEmpty());
                confirmRegistration.setText("Change workshop");
                changePolicyMessage.setText("This attendee is already registered. You may move them while seats remain available.");
                changePolicyMessage.setVisible(true);
            } else {
                workshop.setItems(List.of(existing.workshop()));
                applyWorkshopSelection(existing.workshop().sessionCode(), List.of(existing.workshop()));
                workshop.setEnabled(false);
                confirmRegistration.setEnabled(false);
                confirmRegistration.setText("Confirm workshop registration");
                changePolicyMessage.setText("If you need to change workshops, please contact the event organization so they can assist you.");
                changePolicyMessage.setVisible(true);
            }
        } else {
            applyPreferredWorkshopSelection(participantView.availableWorkshops());
            workshop.setEnabled(!participantView.availableWorkshops().isEmpty());
            confirmRegistration.setEnabled(!participantView.availableWorkshops().isEmpty());
            confirmRegistration.setText("Confirm workshop registration");
            if (participantView.availableWorkshops().isEmpty()) {
                changePolicyMessage.setText("This attendee is valid, but every workshop is currently at capacity.");
                changePolicyMessage.setVisible(true);
            }
        }
    }

    private void registerSelectedWorkshop() {
        if (validatedOrder == null) {
            NotificationUtils.warning("Validate the 'Info. del pedido' code first.");
            return;
        }
        if (selectedParticipant == null) {
            NotificationUtils.warning("Select an attendee from the order first.");
            return;
        }
        if (workshop.getValue() == null) {
            NotificationUtils.warning("Select one workshop before confirming the registration.");
            return;
        }
        try {
            WorkshopParticipantRegistration registration = selectedParticipant.isRegistered()
                    ? registrationService.changeWorkshop(
                    validatedOrder.orderReference(),
                    selectedParticipant.ticketPublicId(),
                    workshop.getValue().sessionCode()
            )
                    : registrationService.registerWorkshop(
                    validatedOrder.orderReference(),
                    selectedParticipant.ticketPublicId(),
                    workshop.getValue().sessionCode()
            );

            registrationResult.removeAll();
            registrationResult.add(createResultNotice(
                    selectedParticipant.isRegistered() ? "Workshop updated." : "Workshop registration confirmed.",
                    "%s is assigned to %s.".formatted(
                            registration.getAttendeeName(),
                            registration.getSession().getTitle()
                    )
            ));
            NotificationUtils.success(selectedParticipant.isRegistered()
                    ? "The attendee workshop selection was updated successfully."
                    : "The attendee workshop registration was saved successfully.");

            validatedOrder = registrationService.validateOrder(validatedOrder.orderReference());
            participant.setItems(validatedOrder.participants());
            renderParticipantsSummary(validatedOrder.participants());
            participant.setValue(validatedOrder.participants().stream()
                    .filter(item -> Objects.equals(item.ticketPublicId(), selectedParticipant.ticketPublicId()))
                    .findFirst()
                    .orElse(null));
        } catch (WorkshopRegistrationService.DuplicateWorkshopRegistrationException ex) {
            NotificationUtils.info("This attendee is already registered in a workshop.");
            validatedOrder = registrationService.validateOrder(validatedOrder.orderReference());
            participant.setItems(validatedOrder.participants());
            renderParticipantsSummary(validatedOrder.participants());
            participant.setValue(validatedOrder.participants().stream()
                    .filter(item -> Objects.equals(item.ticketPublicId(), selectedParticipant.ticketPublicId()))
                    .findFirst()
                    .orElse(null));
        } catch (WorkshopRegistrationService.WorkshopRegistrationException ex) {
            NotificationUtils.error(ex.getMessage());
        } catch (Exception ex) {
            log.error("Workshop registration failed for order '{}' and participant '{}'",
                    validatedOrder.orderReference(),
                    selectedParticipant.ticketPublicId(),
                    ex);
            NotificationUtils.error("We could not complete the workshop registration.");
        }
    }

    private void renderParticipantsSummary(List<WorkshopRegistrationService.ParticipantRegistrationView> participants) {
        participantsSummary.removeAll();
        if (participants.isEmpty()) {
            return;
        }
        participants.forEach(item -> participantsSummary.add(createParticipantSummaryCard(item)));
    }

    private Component createParticipantSummaryCard(WorkshopRegistrationService.ParticipantRegistrationView item) {
        String title = "%s · %s".formatted(item.attendeeName(), item.attendeeEmail());
        String description = item.isRegistered()
                ? "Registered in %s".formatted(item.existingRegistration().workshop().title())
                : "Not registered yet";

        H3 heading = new H3(title);
        heading.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.MEDIUM);

        Paragraph copy = new Paragraph(description);
        copy.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.SECONDARY);

        Span ticketBadge = new Span(item.ticketPublicId());
        ticketBadge.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

        Div card = new Div(heading, copy, ticketBadge);
        card.addClassNames("public-workshop-result-card");
        return card;
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
        validatedOrder = null;
        selectedParticipant = null;
        attendeeName.clear();
        attendeeEmail.clear();
        ticketPublicId.clear();
        reservationStatus.clear();
        reservationShortCode.clear();
        participant.clear();
        participant.setItems(List.of());
        workshop.clear();
        workshop.setItems(List.of());
        workshop.setEnabled(false);
        confirmRegistration.setEnabled(false);
        confirmRegistration.setText("Confirm workshop registration");
        changePolicyMessage.setText("");
        changePolicyMessage.setVisible(false);
        detailsSection.setVisible(false);
        registrationResult.removeAll();
        participantsSummary.removeAll();
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
