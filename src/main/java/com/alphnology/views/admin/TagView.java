package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.Tag;
import com.alphnology.exceptions.DeleteConstraintViolationException;
import com.alphnology.services.TagService;
import com.alphnology.utils.ColorUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.vaadin.addons.tatu.ColorPicker;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static org.reflections.Reflections.log;

@PageTitle("Tag")
@Route("admin/tag")
@Menu(order = 13, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class TagView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final Grid<Tag> grid = new Grid<>(Tag.class, false);

    private final transient TagService service;


    public TagView(
            TagService service
    ) {
        this.service = service;

        initGrid();

        VerticalLayout gridLayout = new VerticalLayout(searchField, grid);
        gridLayout.setSizeFull();
        gridLayout.getStyle().set("gap", "0.3rem");
        gridLayout.addClassName("flex-wrap-layout");

        add(gridLayout);
        setSizeFull();
        setPadding(false);
        setMargin(false);
        setSpacing(false);


    }

    private Specification<Tag> createFilterSpecification() {
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

        grid.addColumn(Tag::getName).setHeader("Name").setAutoWidth(true).setSortable(true).setSortProperty("name");

        grid.addComponentColumn((ValueProvider<Tag, Component>) element -> {
            ColorPicker colorPicker = new ColorPicker();
            colorPicker.setPresets(Arrays.stream(ColorUtils.values()).map(ColorUtils::getColorPreset).toList());
            colorPicker.setValue(element.getColor());

            colorPicker.addValueChangeListener(event -> {
                element.setColor(event.getValue());
                service.save(element);
                grid.getDataProvider().refreshItem(element);
                NotificationUtils.success();
            });

            return colorPicker;
        }).setHeader("Color").setAutoWidth(true).setSortable(true).setSortProperty("color");

        grid.addComponentColumn((ValueProvider<Tag, Component>) tag -> {
            final Button delete = new Button(VaadinIcon.TRASH.create());
            delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
            delete.addClickListener(event ->
                delete(tag)
            );

            return delete;
        }).setHeader("Remove").setFlexGrow(0).setTextAlign(ColumnTextAlign.CENTER);
    }

    private void delete(Tag tag) {
        ConfirmationDialog.delete(event -> {
            try {

                service.delete(tag.getCode());

                grid.getDataProvider().refreshAll();
            } catch (DeleteConstraintViolationException ex) {
                log.error(ex.getLocalizedMessage());
                NotificationUtils.info(ex.getMessage());
            }
        });
    }

}
