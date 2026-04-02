package com.alphnology.views.admin;

import com.alphnology.data.enums.MailProviderType;
import com.alphnology.data.enums.MailSecurityMode;
import com.alphnology.services.email.EmailSendException;
import com.alphnology.services.email.MailSenderService;
import com.alphnology.services.email.MailSettingsService;
import com.alphnology.services.email.MailSettingsSnapshot;
import com.alphnology.utils.NotificationUtils;
import com.alphnology.utils.ViewHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.util.StringUtils;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.List;

@PageTitle("Mail Settings")
@Route("admin/mail-settings")
@Menu(order = 19, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class AdminMailSettingsView extends VerticalLayout {

    private final MailSettingsService mailSettingsService;
    private final MailSenderService mailSenderService;

    private final BeanValidationBinder<MailSettingsFormData> binder = new BeanValidationBinder<>(MailSettingsFormData.class);

    private final Checkbox outboundEnabled = new Checkbox("Enable outbound email");
    private final ComboBox<MailProviderType> providerType = new ComboBox<>("Provider type");
    private final ComboBox<MailSecurityMode> securityMode = new ComboBox<>("Encryption / security");
    private final Checkbox authenticationEnabled = new Checkbox("Authentication required");

    private final TextField host = new TextField("SMTP host");
    private final IntegerField port = new IntegerField("SMTP port");
    private final TextField username = new TextField("Username");
    private final PasswordField secret = new PasswordField("Password / token / API key");
    private final Checkbox clearStoredSecret = new Checkbox("Remove stored secret");
    private final Span secretState = new Span();

    private final EmailField fromAddress = new EmailField("From email");
    private final TextField fromName = new TextField("From name");

    private final TextField postalBaseUrl = new TextField("Postal base URL");
    private final TextField postalApiKeyHeader = new TextField("Postal API key header");
    private final TextField sslTrust = new TextField("Trusted TLS host");
    private final IntegerField connectionTimeoutMs = new IntegerField("Connection timeout (ms)");
    private final IntegerField readTimeoutMs = new IntegerField("Read timeout (ms)");
    private final EmailField testRecipient = new EmailField("Test recipient");

    private final Button reload = new Button("Reload", VaadinIcon.REFRESH.create());
    private final Button save = new Button("Save settings", VaadinIcon.CHECK.create());
    private final Button sendTest = new Button("Send test email", VaadinIcon.ENVELOPE.create());

    private final Paragraph runtimeInfo = new Paragraph();
    private MailSettingsSnapshot snapshot;

    public AdminMailSettingsView(MailSettingsService mailSettingsService, MailSenderService mailSenderService) {
        this.mailSettingsService = mailSettingsService;
        this.mailSenderService = mailSenderService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        configureBinder();
        configureFields();
        configureActions();

        Header header = ViewHelper.getSecondaryHeader(
                "Mail settings",
                "Configure outbound email providers, sender identity, and a safe test flow for administrators."
        );

        VerticalLayout content = new VerticalLayout(
                buildIntroCard(),
                buildProviderSection(),
                buildSenderSection(),
                buildTimeoutSection(),
                buildTestSection()
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
    }

    private void configureBinder() {
        binder.bindInstanceFields(this);
    }

    private void configureFields() {
        providerType.setItems(MailProviderType.values());
        providerType.setItemLabelGenerator(type -> switch (type) {
            case SMTP -> "SMTP";
            case SENDGRID -> "SendGrid";
            case MAILJET -> "Mailjet";
            case POSTAL -> "Postal";
        });
        providerType.setHelperText("Choose the delivery provider to use for password resets, welcome emails, and shared sessions.");

        securityMode.setItems(MailSecurityMode.values());
        securityMode.setItemLabelGenerator(mode -> switch (mode) {
            case NONE -> "None";
            case STARTTLS -> "STARTTLS";
            case SSL_TLS -> "SSL/TLS";
        });

        host.setWidthFull();
        username.setWidthFull();
        fromAddress.setWidthFull();
        fromName.setWidthFull();
        postalBaseUrl.setWidthFull();
        postalApiKeyHeader.setWidthFull();
        sslTrust.setWidthFull();
        secret.setWidthFull();
        testRecipient.setWidthFull();

        port.setStepButtonsVisible(true);
        connectionTimeoutMs.setStepButtonsVisible(true);
        readTimeoutMs.setStepButtonsVisible(true);

        host.setHelperText("For SendGrid and Mailjet, defaults are prefilled automatically.");
        username.setHelperText("Use 'apikey' for SendGrid. Mailjet uses its API key as the username.");
        secret.setRevealButtonVisible(false);
        clearStoredSecret.setHelperText("Only clears the encrypted secret stored in the application database.");
        postalBaseUrl.setHelperText("Example: https://postal.yourdomain.com");
        postalApiKeyHeader.setHelperText("Postal default is X-Server-API-Key.");
        sslTrust.setHelperText("Only needed for self-signed SMTP certificates.");
        testRecipient.setHelperText("The test email is sent using the current saved configuration.");

        providerType.addValueChangeListener(event -> {
            applyProviderDefaults(event.getValue());
            refreshProviderFieldState();
        });
        authenticationEnabled.addValueChangeListener(event -> refreshProviderFieldState());
        clearStoredSecret.addValueChangeListener(event -> secret.setEnabled(!event.getValue() && isSecretEditable()));
    }

    private void configureActions() {
        reload.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendTest.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        reload.addClickListener(event -> loadSettings());
        save.addClickListener(event -> saveSettings(false));
        sendTest.addClickListener(event -> saveSettings(true));
    }

    private Component buildIntroCard() {
        runtimeInfo.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        runtimeInfo.setWidthFull();

        HorizontalLayout actions = new HorizontalLayout(reload, save, sendTest);
        actions.setWrap(true);
        actions.addClassNames(LumoUtility.Gap.SMALL);

        Div card = createCard(
                "Operational model",
                "The application reads defaults from environment variables and lets admins override non-sensitive settings from the UI. Secrets are masked and can optionally be persisted encrypted."
        );
        card.add(runtimeInfo, secretState, actions);
        return card;
    }

    private Component buildProviderSection() {
        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("680px", 2),
                new FormLayout.ResponsiveStep("980px", 3)
        );

        formLayout.add(outboundEnabled, providerType, authenticationEnabled, securityMode, host, port, username, secret,
                clearStoredSecret, postalBaseUrl, postalApiKeyHeader, sslTrust);
        formLayout.setColspan(secret, 2);
        formLayout.setColspan(postalBaseUrl, 2);

        Div card = createCard(
                "Provider and credentials",
                "Switch between SMTP, SendGrid, Mailjet, and Postal. Provider-specific defaults are applied without exposing secrets in the UI."
        );
        card.add(formLayout);
        return card;
    }

    private Component buildSenderSection() {
        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("680px", 2)
        );
        formLayout.add(fromAddress, fromName);

        Div card = createCard(
                "Sender identity",
                "These values are used in transactional emails. Keep the sender aligned with your verified domain."
        );
        card.add(formLayout);
        return card;
    }

    private Component buildTimeoutSection() {
        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("680px", 2)
        );
        formLayout.add(connectionTimeoutMs, readTimeoutMs);

        Div card = createCard(
                "Timeouts and delivery behavior",
                "Use conservative timeout values so the UI fails fast when a provider is unavailable."
        );
        card.add(formLayout);
        return card;
    }

    private Component buildTestSection() {
        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.add(testRecipient);

        Div card = createCard(
                "Test email",
                "Save the current settings and send a verification email to confirm the provider, credentials, and sender identity are working."
        );
        card.add(formLayout);
        return card;
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

    private void loadSettings() {
        snapshot = mailSettingsService.getEffectiveSettings();
        binder.readBean(MailSettingsFormData.fromSnapshot(snapshot));
        clearStoredSecret.setValue(false);
        secret.clear();
        secretState.setText(buildSecretState(snapshot));
        secretState.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        runtimeInfo.setText(buildRuntimeInfo(snapshot));
        refreshProviderFieldState();
    }

    private void saveSettings(boolean sendTestAfterSave) {
        try {
            MailSettingsFormData formData = new MailSettingsFormData();
            binder.writeBean(formData);

            List<String> issues = validateForm(formData, sendTestAfterSave);
            if (!issues.isEmpty()) {
                NotificationUtils.error(String.join(" ", issues));
                return;
            }

            mailSettingsService.save(formData.toUpdateRequest());
            snapshot = mailSettingsService.getEffectiveSettings();

            if (sendTestAfterSave) {
                mailSenderService.sendTestEmail(formData.getTestRecipient());
                NotificationUtils.success("Mail settings saved and a test email was sent successfully.");
            } else {
                NotificationUtils.success("Mail settings saved successfully.");
            }

            loadSettings();
        } catch (ValidationException ex) {
            NotificationUtils.error("Please review the highlighted fields.", ex);
        } catch (EmailSendException ex) {
            NotificationUtils.error(ex.getMessage());
        } catch (Exception ex) {
            NotificationUtils.error("Could not save the mail settings.");
        }
    }

    private List<String> validateForm(MailSettingsFormData data, boolean sendTestAfterSave) {
        List<String> issues = new ArrayList<>();

        if (!StringUtils.hasText(data.getFromAddress())) {
            issues.add("A from email address is required.");
        }

        if (sendTestAfterSave && !StringUtils.hasText(data.getTestRecipient())) {
            issues.add("A test recipient email is required to send a test message.");
        }

        if (data.getProviderType() == MailProviderType.POSTAL) {
            if (!StringUtils.hasText(data.getPostalBaseUrl())) {
                issues.add("Postal base URL is required.");
            }
            if (needsRuntimeSecret(data)) {
                issues.add("A Postal API key is required. Configure it via environment variables or enable encrypted UI secret persistence.");
            }
            return issues;
        }

        if (!StringUtils.hasText(data.getHost())) {
            issues.add("SMTP host is required.");
        }
        if (data.getPort() == null) {
            issues.add("SMTP port is required.");
        }
        if (data.isAuthenticationEnabled()) {
            if (!StringUtils.hasText(data.getUsername())) {
                issues.add("Username is required when authentication is enabled.");
            }
            if (needsRuntimeSecret(data)) {
                issues.add("A password, token, or API key is required. Configure it via environment variables or enable encrypted UI secret persistence.");
            }
        }
        return issues;
    }

    private boolean needsRuntimeSecret(MailSettingsFormData data) {
        if (StringUtils.hasText(data.getSecret())) {
            return false;
        }
        if (snapshot != null && StringUtils.hasText(snapshot.secret()) && !data.isClearStoredSecret()) {
            return false;
        }
        return true;
    }

    private void refreshProviderFieldState() {
        boolean postal = providerType.getValue() == MailProviderType.POSTAL;
        boolean auth = authenticationEnabled.getValue();

        host.setVisible(!postal);
        port.setVisible(!postal);
        username.setVisible(!postal);
        securityMode.setVisible(!postal);
        sslTrust.setVisible(!postal);
        authenticationEnabled.setVisible(!postal);

        username.setEnabled(!postal && auth);
        secret.setEnabled(!clearStoredSecret.getValue() && isSecretEditable());

        postalBaseUrl.setVisible(postal);
        postalApiKeyHeader.setVisible(postal);
    }

    private boolean isSecretEditable() {
        return snapshot != null && snapshot.uiSecretPersistenceAvailable();
    }

    private void applyProviderDefaults(MailProviderType type) {
        if (type == null) {
            return;
        }
        switch (type) {
            case SENDGRID -> {
                host.setValue("smtp.sendgrid.net");
                port.setValue(587);
                username.setValue("apikey");
                securityMode.setValue(MailSecurityMode.STARTTLS);
                authenticationEnabled.setValue(true);
            }
            case MAILJET -> {
                host.setValue("in-v3.mailjet.com");
                port.setValue(587);
                securityMode.setValue(MailSecurityMode.STARTTLS);
                authenticationEnabled.setValue(true);
            }
            case POSTAL -> {
                securityMode.setValue(MailSecurityMode.NONE);
                authenticationEnabled.setValue(true);
                host.clear();
                port.clear();
                sslTrust.clear();
                if (!StringUtils.hasText(postalApiKeyHeader.getValue())) {
                    postalApiKeyHeader.setValue("X-Server-API-Key");
                }
            }
            case SMTP -> {
                if (!StringUtils.hasText(host.getValue())) {
                    host.setPlaceholder("mail.yourdomain.com");
                }
                if (port.getValue() == null) {
                    port.setValue(587);
                }
                if (securityMode.getValue() == null) {
                    securityMode.setValue(MailSecurityMode.STARTTLS);
                }
            }
        }
    }

    private String buildRuntimeInfo(MailSettingsSnapshot effective) {
        return "Runtime provider: " + effective.providerType()
                + " | outbound: " + (effective.outboundEnabled() ? "enabled" : "disabled")
                + " | security: " + effective.securityMode()
                + " | auth: " + (effective.authenticationEnabled() ? "enabled" : "disabled");
    }

    private String buildSecretState(MailSettingsSnapshot effective) {
        if (effective.uiSecretPersistenceAvailable()) {
            if (effective.secretPersisted()) {
                return "An encrypted secret is stored in the database. Enter a new value only when you want to replace it.";
            }
            if (StringUtils.hasText(effective.secret())) {
                return "The active secret is coming from environment variables. You can replace it from the UI and store it encrypted.";
            }
            return "No secret is configured yet. Enter one to persist it securely in the database.";
        }
        if (StringUtils.hasText(effective.secret())) {
            return "The active secret is provided by environment variables. UI secret persistence is disabled until EMAIL_SETTINGS_MASTER_KEY and EMAIL_ALLOW_UI_SECRET_PERSISTENCE=true are configured.";
        }
        return "UI secret persistence is disabled. Configure the provider secret from environment variables and restart the application.";
    }
}
