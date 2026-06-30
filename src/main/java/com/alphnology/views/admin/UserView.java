package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.components.ConfirmationDialogBuilder;
import com.alphnology.components.EmptyStateComponent;
import com.alphnology.data.User;
import com.alphnology.data.enums.Role;
import com.alphnology.services.UserService;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.HasClearButton;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.EmailField;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.RegexGenerator.generateMatchingString;
import static com.alphnology.utils.ViewHelper.*;

@Slf4j
@PageTitle("User")
@Route("admin/user")
@Menu(order = 17, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class UserView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final VirtualList<User> list = new VirtualList<>();
    private final Span countBadge = new Span("0");
    private final EmptyStateComponent emptyState = new EmptyStateComponent(
            VaadinIcon.USER, "No users found", "Add a new user to get started."
    );
    private User selectedItem;

    private final EmailField username = new EmailField("Username");
    private final TextField name = new TextField("Name");
    private final TextField phone = new TextField("Phone");
    private final ComboBox<Role> roles = new ComboBox<>("Rol");
    private final Checkbox oneLogPwd = new Checkbox("Change your password at the next login");
    private final Checkbox locked = new Checkbox("Locked");

    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());
    private final Button resetPassword = new Button("Reset password", VaadinIcon.KEY.create());

    private final transient UserService service;
    private final transient PasswordEncoder passwordEncoder;
    private User element;

    private final Binder<User> binder = new BeanValidationBinder<>(User.class);

    public UserView(UserService service, PasswordEncoder passwordEncoder) {
        this.service = service;
        this.passwordEncoder = passwordEncoder;

        roles.setItems(Role.values());
        roles.setPlaceholder("Choose a rol");

        oneLogPwd.getElement().getThemeList().add("switch");
        locked.getElement().getThemeList().add("switch");

        username.setEnabled(false);

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

        save.addClickListener(this::saveOrUpdate);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);

        resetPassword.addClickListener(this::confirmResetPassword);
        resetPassword.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
    }

    private Specification<User> createFilterSpecification() {
        return (root, query, builder) -> {
            final String search = searchField.getValue().toLowerCase().trim();
            Order order = builder.asc(root.get("name"));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);
            Predicate predicateUsername = predicateUnaccentLike(root, builder, "username", search);
            Predicate predicateName = predicateUnaccentLike(root, builder, "name", search);
            Predicate predicatePhone = predicateUnaccentLike(root, builder, "phone", search);
            final List<Predicate> orPredicates = new ArrayList<>(List.of(predicateUsername, predicateName, predicatePhone));
            return builder.or(orPredicates.toArray(Predicate[]::new));
        };
    }

    private void initList() {
        searchField.setWidthFull();
        searchField.setPlaceholder("Search users...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshList());

        list.setSizeFull();
        list.setRenderer(new ComponentRenderer<>(this::buildListItem));
    }

    private Div buildListItem(User user) {
        Div item = new Div();
        item.addClassName("admin-list-item");
        if (selectedItem != null && Objects.equals(selectedItem.getCode(), user.getCode())) {
            item.addClassName("selected");
        }

        // Icon: initials avatar
        Div iconDiv = new Div();
        iconDiv.addClassName("admin-item-icon");
        Icon icon = VaadinIcon.USER.create();
        icon.setSize("18px");
        iconDiv.add(icon);

        // Body
        Div body = new Div();
        body.addClassName("admin-item-body");
        Span nameSpan = new Span(StringUtils.hasText(user.getName()) ? user.getName() : user.getUsername());
        nameSpan.addClassName("admin-item-name");
        Span subSpan = new Span(user.getUsername());
        subSpan.addClassName("admin-item-sub");
        body.add(nameSpan, subSpan);

        // Side: role badge
        Div side = new Div();
        side.addClassName("admin-item-side");
        if (user.getRoles() != null) {
            Span badge = new Span(user.getRoles().toString());
            badge.addClassName("admin-badge");
            if (user.getRoles() == Role.ADMIN) badge.addClassName("error");
            else badge.addClassName("muted");
            side.add(badge);
        }

        item.add(iconDiv, body, side);
        item.addClickListener(e -> selectItem(user));
        return item;
    }

    private void selectItem(User user) {
        populateForm(user);
        list.getDataProvider().refreshAll();
    }

    private void refreshList() {
        List<User> items = service.findAll(createFilterSpecification());
        list.setItems(items);
        countBadge.setText(String.valueOf(items.size()));
        boolean isEmpty = items.isEmpty();
        list.setVisible(!isEmpty);
        emptyState.setVisible(isEmpty);
    }

    private VerticalLayout createFormLayout() {
        Header header = getSecondaryHeader("Manage users", "Manage user accounts and roles");

        username.setWidthFull();
        name.setWidthFull();
        phone.setWidthFull();
        roles.setWidthFull();
        oneLogPwd.setWidthFull();
        locked.setWidthFull();

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSizeFull();
        formLayout.setPadding(false);
        formLayout.setSpacing(false);
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, username, name, phone, roles, oneLogPwd, locked);
        return formLayout;
    }

    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {
        try {
            if (this.element == null) this.element = new User();
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

    private HorizontalLayout createFooter() {
        HorizontalLayout buttonLayout = new HorizontalLayout(resetPassword, save);
        buttonLayout.setFlexGrow(1, save);
        buttonLayout.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.CONTRAST_10);
        buttonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return buttonLayout;
    }

    private void confirmResetPassword(ClickEvent<Button> event) {
        if (element == null) return;

        ConfirmDialog dialog = new ConfirmationDialogBuilder()
                .withHeader("Reset Password")
                .withText("This will generate a new temporary password for \"%s\" and require them to change it at their next login. Continue?"
                        .formatted(StringUtils.hasText(element.getName()) ? element.getName() : element.getUsername()))
                .withConfirmText("Reset password")
                .withCancelText("Cancel")
                .withIcon(VaadinIcon.KEY)
                .onConfirm(confirmEvent -> resetUserPassword())
                .build();
        dialog.open();
    }

    private void resetUserPassword() {
        String temporaryPassword = generateMatchingString(10);
        element.setPassword(passwordEncoder.encode(temporaryPassword));
        element.setOneLogPwd(true);

        User saved = service.save(element);
        populateForm(saved);
        refreshList();

        showTemporaryPassword(temporaryPassword);
    }

    private void showTemporaryPassword(String temporaryPassword) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Temporary password generated");
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        Span info = new Span("Share this password with the user. It will not be shown again, " +
                "and they will be required to set a new password at their next login.");
        info.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        TextField passwordField = new TextField();
        passwordField.setValue(temporaryPassword);
        passwordField.setReadOnly(true);
        passwordField.setWidthFull();

        Button copyButton = new Button(VaadinIcon.COPY_O.create());
        copyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        copyButton.getElement().setAttribute("aria-label", "Copy password");
        copyButton.addClickListener(e -> {
            e.getSource().getUI().ifPresent(ui -> ui.getPage()
                    .executeJs("navigator.clipboard.writeText($0)", temporaryPassword));
            NotificationUtils.success("Password copied to clipboard.");
        });

        HorizontalLayout fieldLayout = new HorizontalLayout(passwordField, copyButton);
        fieldLayout.setWidthFull();
        fieldLayout.setAlignItems(FlexComponent.Alignment.END);
        fieldLayout.setFlexGrow(1, passwordField);

        VerticalLayout layout = new VerticalLayout(info, fieldLayout);
        layout.setPadding(false);
        layout.setWidth("400px");
        dialog.add(layout);

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(User value) {
        this.element = value;
        this.selectedItem = value;
        binder.readBean(this.element);
        save.setEnabled(element != null);
        resetPassword.setEnabled(element != null);
    }
}
