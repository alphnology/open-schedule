package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.User;
import com.alphnology.data.enums.Role;
import com.alphnology.services.UserService;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.List;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;
import static org.reflections.Reflections.log;

@PageTitle("User")
@Route("admin/user")
@Menu(order = 17, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class UserView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final Grid<User> grid = new Grid<>(User.class, false);

    private final EmailField username = new EmailField("Username");
    private final TextField name = new TextField("Name");
    private final TextField phone = new TextField("Phone");
    private final ComboBox<Role> roles = new ComboBox<>("Rol");
    private final Checkbox oneLogPwd = new Checkbox("Change your password at the next login");
    private final Checkbox locked = new Checkbox("Locked");

    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());

    private final transient UserService service;
    private User element;

    private final Binder<User> binder = new BeanValidationBinder<>(User.class);

    public UserView(
            UserService service
    ) {
        this.service = service;

        roles.setItems(Role.values());
        roles.setPlaceholder("Choose a rol");

        oneLogPwd.getElement().getThemeList().add("switch");
        locked.getElement().getThemeList().add("switch");

        username.setEnabled(false);

        // Bind fields. This where you'd define e.g. validation rules
        binder.bindInstanceFields(this);
        binder.getFields().forEach(field -> {
            if (field instanceof HasClearButton clear) {
                clear.setClearButtonVisible(true);
            }
        });

        initGrid();

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

        save.addClickListener(this::saveOrUpdate);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
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

    private void initGrid() {
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

        grid.addColumn(User::getUsername).setHeader("Username").setAutoWidth(true).setSortable(true).setSortProperty("username");

        grid.addColumn(User::getName).setHeader("Name").setAutoWidth(true).setSortable(true).setSortProperty("name");

        grid.addColumn(User::getPhone).setHeader("Phone").setAutoWidth(true).setSortable(true).setSortProperty("phone");

        grid.addColumn(User::getRoles).setHeader("Rol").setAutoWidth(true).setSortable(true).setSortProperty("roles");

        grid.asSingleSelect().addValueChangeListener(event -> populateForm(event.getValue()));
    }

    private VerticalLayout createFormLayout() {
        Header header = getSecondaryHeader("Manage users", "Manage users");

        username.setWidthFull();
        name.setWidthFull();
        phone.setWidthFull();
        roles.setWidthFull();
        oneLogPwd.setWidthFull();
        locked.setWidthFull();

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSizeFull();
        formLayout.setPadding(false);
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, username, name, phone, roles, oneLogPwd, locked);

        return formLayout;
    }


    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {

        try {

            if (this.element == null) {
                this.element = new User();
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


    private HorizontalLayout createFooter() {
        HorizontalLayout buttonLayout = new HorizontalLayout(save);
        buttonLayout.setFlexGrow(1, save);
        buttonLayout.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Padding.MEDIUM);
        buttonLayout.addClassNames(LumoUtility.Background.CONTRAST_10);
        buttonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        return buttonLayout;
    }

    private void clearForm() {
        populateForm(null);
    }


    private void populateForm(User value) {
        this.element = value;

        binder.readBean(this.element);

        save.setEnabled(element != null);
    }

}
