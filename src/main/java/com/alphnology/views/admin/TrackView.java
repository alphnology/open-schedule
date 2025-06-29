package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.Track;
import com.alphnology.exceptions.DeleteConstraintViolationException;
import com.alphnology.services.TrackService;
import com.alphnology.utils.ColorUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
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
import org.vaadin.addons.tatu.ColorPicker;
import org.vaadin.addons.tatu.ColorPickerVariant;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;
import static org.reflections.Reflections.log;

@PageTitle("Track")
@Route("admin/track")
@Menu(order = 14, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class TrackView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final Grid<Track> grid = new Grid<>(Track.class, false);

    private final TextField name = new TextField("Name");
    private final ColorPicker color = new ColorPicker();

    private final Button cancel = new Button("New", VaadinIcon.FILE_ADD.create());
    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());
    private final Button delete = new Button("Delete", VaadinIcon.TRASH.create());

    private final transient TrackService service;
    private Track element;

    private final Binder<Track> binder = new BeanValidationBinder<>(Track.class);


    public TrackView(
            TrackService service
    ) {
        this.service = service;

        color.setLabel("Color");
        color.setPresets(Arrays.stream(ColorUtils.values()).map(ColorUtils::getColorPreset).toList());

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

        cancel.addClickListener(e -> clearForm());
        save.addClickListener(this::saveOrUpdate);
        delete.addClickListener(this::delete);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);

    }

    private Specification<Track> createFilterSpecification() {
        return (root, query, builder) -> {

            final String search = searchField.getValue().toLowerCase().trim();

            Order order = builder.asc(root.get("code"));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);

            Predicate predicateName = predicateUnaccentLike(root, builder, "name", search);

            final List<Predicate> orPredicates = new ArrayList<>(List.of(predicateName));

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

        grid.addColumn(Track::getName).setHeader("Name").setAutoWidth(true).setSortable(true).setSortProperty("name");

        grid.addComponentColumn((ValueProvider<Track, Component>) room -> {
            ColorPicker colorPicker = new ColorPicker();
            colorPicker.setEnabled(false);
            colorPicker.addThemeVariants(ColorPickerVariant.COMPACT);
            colorPicker.setValue(room.getColor());
            return colorPicker;
        }).setHeader("Color").setAutoWidth(true).setSortable(true).setSortProperty("color");

        grid.asSingleSelect().addValueChangeListener(event -> populateForm(event.getValue()));
    }

    private VerticalLayout createFormLayout() {
        Header header = getSecondaryHeader("Configure your track", "Manage track information");

        name.setWidthFull();
        color.setWidthFull();

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSizeFull();
        formLayout.setPadding(false);
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, name, color);

        return formLayout;
    }


    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {

        try {

            if (this.element == null) {
                this.element = new Track();
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
        ConfirmationDialog.delete(event -> {
            try {

                service.delete(element.getCode());

                populateForm(null);

                grid.getDataProvider().refreshAll();
            } catch (DeleteConstraintViolationException ex) {
                log.error(ex.getLocalizedMessage());
                NotificationUtils.error(ex.getMessage());
            }
        });

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

        color.setValue(ColorUtils.DEFAULT.getColorPreset().getColor());
    }


    private void populateForm(Track value) {
        this.element = value;

        binder.readBean(this.element);

        delete.setEnabled(element != null);
    }

}
