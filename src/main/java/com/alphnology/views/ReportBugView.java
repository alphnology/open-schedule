package com.alphnology.views;

import com.alphnology.services.GitHubService;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.lineawesome.LineAwesomeIcon;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.io.IOException;


/**
 * @author me@fredpena.dev
 * @created 29/06/2025  - 18:28
 */


@PageTitle("Report an Issue")
@Route(value = "bug")
@Menu(order = 5, icon = LineAwesomeIconUrl.BUG_SOLID)
@AnonymousAllowed
public class ReportBugView extends VerticalLayout {

    private final GitHubService gitHubService;

    private final TextField issueTitle = new TextField("Issue Title");
    private final TextArea issueDescription = new TextArea("Description");
    private final Button submitButton = new Button("Submit Report");

    public ReportBugView(GitHubService gitHubService) {
        this.gitHubService = gitHubService;

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        addClassNames(LumoUtility.MaxWidth.SCREEN_MEDIUM, LumoUtility.Padding.LARGE);
        getStyle().set("justify-self", "center");

        add(new H2("Report an Issue or Suggestion"));
        add(new Paragraph("Your feedback is essential for improving Open Schedule. Thank you for your time."));

        Button githubButton = new Button("Report Directly on GitHub", LineAwesomeIcon.GITHUB.create());
        githubButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        githubButton.addClickListener(e -> UI.getCurrent().getPage().open("https://github.com/alphnology/open-schedule/issues", "_blank"));
        add(githubButton);

        Span separator = new Span("or if you don't have a GitHub account, please use the form below:");
        separator.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Vertical.LARGE);
        add(separator);

        FormLayout formLayout = createBugForm();
        add(formLayout);

        submitButton.addClickListener(e -> submitBugReport());
    }

    private FormLayout createBugForm() {
        FormLayout form = new FormLayout();

        issueTitle.setClearButtonVisible(true);
        issueTitle.setPlaceholder("Example: The save button doesn't work on the X view");
        issueTitle.setTooltipText("A brief, descriptive title for the issue.");

        issueDescription.setClearButtonVisible(true);
        issueDescription.setHeight("180px");
        issueDescription.setPlaceholder("Describe the issue in as much detail as possible. Please include:\n1. What you were doing.\n2. What you expected to happen.\n3. What actually happened.");
        issueDescription.setTooltipText("A detailed description helps us resolve the issue faster.");

        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setIcon(VaadinIcon.PAPERPLANE.create());

        form.add(issueTitle, issueDescription, submitButton);
        form.setColspan(issueTitle, 2);
        form.setColspan(issueDescription, 2);
        form.setColspan(submitButton, 2);
        form.getStyle().setTextAlign(Style.TextAlign.RIGHT);

        return form;
    }


    private void submitBugReport() {
        String title = issueTitle.getValue();
        String description = issueDescription.getValue();

        if (title.isBlank() || description.isBlank()) {
            NotificationUtils.error("Please fill out both fields.");
            return;
        }

        submitButton.setEnabled(false);

        try {
            gitHubService.createIssue(title, description);

            NotificationUtils.success("Thank you! Your report has been submitted successfully.");

            issueTitle.clear();
            issueDescription.clear();

        } catch (IOException e) {
            NotificationUtils.error("Could not submit the report. Please try again or report it directly on GitHub.");
        } finally {
            submitButton.setEnabled(true);
        }
    }
}