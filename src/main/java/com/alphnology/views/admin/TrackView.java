package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.Track;
import com.alphnology.exceptions.DeleteConstraintViolationException;
import com.alphnology.services.TrackService;
import com.alphnology.utils.ColorUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.HasClearButton;
import com.vaadin.flow.component.splitlayout.SplitLayout;
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
import org.springframework.util.StringUtils;
import org.vaadin.addons.tatu.ColorPicker;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;

@Slf4j
@PageTitle("Track")
@Route("admin/track")
@Menu(order = 14, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class TrackView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final VirtualList<Track> list = new VirtualList<>();
    private final Span countBadge = new Span("0");
    private Track selectedItem;

    private final TextField name = new TextField("Name");
    private final ColorPicker color = new ColorPicker();

    private final Button cancel = new Button("New", VaadinIcon.FILE_ADD.create());
    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());
    private final Button delete = new Button("Delete", VaadinIcon.TRASH.create());

    private final transient TrackService service;
    private Track element;

    private final Binder<Track> binder = new BeanValidationBinder<>(Track.class);

    public TrackView(TrackService service) {
        this.service = service;

        color.setLabel("Color");
        color.setPresets(Arrays.stream(ColorUtils.values()).map(ColorUtils::getColorPreset).toList());

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

        VerticalLayout sidebar = new VerticalLayout(toolbar, list);
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
        splitLayout.setSplitterPosition(40);
        splitLayout.addToPrimary(sidebar);
        splitLayout.addToSecondary(form);

        add(splitLayout);
        setSizeFull();
        setPadding(false);
        setMargin(false);
        setSpacing(false);

        refreshList();
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

    private void initList() {
        searchField.setWidthFull();
        searchField.setPlaceholder("Search tracks...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshList());

        list.setSizeFull();
        list.setRenderer(new ComponentRenderer<>(this::buildListItem));
    }

    private Div buildListItem(Track track) {
        Div item = new Div();
        item.addClassName("admin-list-item");
        if (selectedItem != null && Objects.equals(selectedItem.getCode(), track.getCode())) {
            item.addClassName("selected");
        }

        // Icon: colored circle
        Div iconDiv = new Div();
        iconDiv.addClassName("admin-item-icon");
        if (StringUtils.hasText(track.getColor())) {
            iconDiv.getStyle().set("background-color", track.getColor());
        }
        Icon icon = VaadinIcon.TAGS.create();
        icon.setSize("18px");
        icon.getStyle().set("color", StringUtils.hasText(track.getColor()) ? "rgba(255,255,255,0.9)" : null);
        iconDiv.add(icon);

        // Body
        Div body = new Div();
        body.addClassName("admin-item-body");
        Span nameSpan = new Span(track.getName());
        nameSpan.addClassName("admin-item-name");
        body.add(nameSpan);

        item.add(iconDiv, body);
        item.addClickListener(e -> selectItem(track));
        return item;
    }

    private void selectItem(Track track) {
        populateForm(track);
        list.getDataProvider().refreshAll();
    }

    private void refreshList() {
        List<Track> items = service.findAll(createFilterSpecification());
        list.setItems(items);
        countBadge.setText(String.valueOf(items.size()));
    }

    private VerticalLayout createFormLayout() {
        Header header = getSecondaryHeader("Configure your track", "Manage track information");

        name.setWidthFull();
        color.setWidthFull();

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSizeFull();
        formLayout.setPadding(false);
        formLayout.setSpacing(false);
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, name, color);
        return formLayout;
    }

    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {
        try {
            if (this.element == null) this.element = new Track();
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

    private void delete(ClickEvent<Button> buttonClickEvent) {
        ConfirmationDialog.delete(event -> {
            try {
                service.delete(element.getCode());
                clearForm();
                refreshList();
            } catch (DeleteConstraintViolationException ex) {
                log.error(ex.getLocalizedMessage());
                NotificationUtils.error(ex.getMessage());
            }
        });
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
        populateForm(null);
        list.getDataProvider().refreshAll();
        color.setValue(ColorUtils.DEFAULT.getColorPreset().getColor());
    }

    private void populateForm(Track value) {
        this.element = value;
        this.selectedItem = value;
        binder.readBean(this.element);
        delete.setEnabled(element != null);
    }
}
