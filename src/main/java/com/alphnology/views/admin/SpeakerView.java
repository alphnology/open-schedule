package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.Country;
import com.alphnology.data.Speaker;
import com.alphnology.exceptions.DeleteConstraintViolationException;
import com.alphnology.services.SpeakerService;
import com.alphnology.utils.CommonUtils;
import com.alphnology.utils.CountryUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.HasClearButton;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
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

import java.util.*;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;
import static org.reflections.Reflections.log;

@PageTitle("Speakers")
@Route("admin/speaker")
@Menu(order = 15, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class SpeakerView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final Grid<Speaker> grid = new Grid<>(Speaker.class, false);

    private final TextField name = new TextField("Name");
    private final TextField title = new TextField("Title");
    private final TextField company = new TextField("Company");
    private final TextField photoUrl = new TextField("Photo Url");
    private final TextArea bio = new TextArea("Biography");
    private final ComboBox<Country> countryField = new ComboBox<>("Select a country");

    private final VerticalLayout socialLinksLayout = new VerticalLayout();
    private final Button addSocialLinkButton = new Button("Add social link", VaadinIcon.PLUS.create());


    private final Button cancel = new Button("New", VaadinIcon.FILE_ADD.create());
    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());
    private final Button delete = new Button("Delete", VaadinIcon.TRASH.create());

    private final transient SpeakerService service;
    private Speaker element;

    private final Binder<Speaker> binder = new BeanValidationBinder<>(Speaker.class);


    public SpeakerView(
            SpeakerService service
    ) {
        this.service = service;

        countryField.setItems(CountryUtils.getCountryNamesWithCodes());
        countryField.setPlaceholder("Choose a country");

        // Bind fields. This where you'd define e.g. validation rules
        binder.bindInstanceFields(this);
        binder.getFields().forEach(field -> {
            if (field instanceof HasClearButton clear) {
                clear.setClearButtonVisible(true);
            }
        });


        binder.forField(countryField)
                .asRequired()
                .bind(speaker -> {
                            if (speaker == null) {
                                return null;
                            }
                            String speakerCountryCode = speaker.getCountry();
                            return CountryUtils.getCountryNamesWithCodes().stream()
                                    .filter(countryInList -> Objects.equals(speakerCountryCode, countryInList.getCode()))
                                    .findFirst()
                                    .orElse(null);
                        },
                        (speaker, selectedCountry) -> {
                            if (speaker != null) {
                                speaker.setCountry(selectedCountry != null ? selectedCountry.getCode() : null);
                            }
                        }
                );


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
        addSocialLinkButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);

        socialLinksLayout.setSpacing(false);
        socialLinksLayout.setPadding(false);
        addSocialLinkButton.addClickListener(e -> addSocialLinkField(null));

    }

    private Specification<Speaker> createFilterSpecification() {
        return (root, query, builder) -> {

            final String search = searchField.getValue().toLowerCase().trim();

            Order order = builder.asc(root.get("code"));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);

            Predicate predicateName = predicateUnaccentLike(root, builder, "name", search);
            Predicate predicateTitle = predicateUnaccentLike(root, builder, "title", search);
            Predicate predicateCompany = predicateUnaccentLike(root, builder, "company", search);
            Predicate predicateBio = predicateUnaccentLike(root, builder, "bio", search);

            final List<Predicate> orPredicates = new ArrayList<>(List.of(predicateName, predicateTitle, predicateCompany, predicateBio));

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

        grid.addComponentColumn(speaker -> {
            Image image = new Image();
            image.setSrc(speaker.getPhotoUrl());
            image.setWidth("100px");
            return image;
        }).setHeader("Picture");

        grid.addColumn(Speaker::getName).setHeader("Name").setAutoWidth(true).setSortable(true).setSortProperty("name");

        grid.addColumn(Speaker::getTitle).setHeader("Title").setAutoWidth(true).setSortable(true).setSortProperty("title");

        grid.addColumn(Speaker::getCompany).setHeader("Company").setAutoWidth(true).setSortable(true).setSortProperty("company");

        grid.addComponentColumn(speaker -> {
            Image image = new Image();
            image.setSrc("https://flagcdn.com/%s.svg".formatted(speaker.getCountry().toLowerCase()));
            image.setWidth("50px");
            return image;
        }).setHeader("Country");

        grid.asSingleSelect().addValueChangeListener(event -> populateForm(event.getValue()));
    }


    private void addSocialLinkField(String url) {
        TextField linkField = new TextField();
        linkField.setWidthFull();
        linkField.setPlaceholder("https://openschedule.com");
        if (url != null) {
            linkField.setValue(url);
        }

        Button removeLinkButton = new Button(VaadinIcon.TRASH.create(), e ->
                e.getSource().getParent()
                        .ifPresent(socialLinksLayout::remove)
        );
        removeLinkButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        removeLinkButton.addClassNames(LumoUtility.Width.AUTO);

        HorizontalLayout linkRow = new HorizontalLayout(linkField, removeLinkButton);
        linkRow.setWidthFull();
        linkRow.setAlignItems(Alignment.BASELINE);
        socialLinksLayout.add(linkRow);
    }

    private VerticalLayout createFormLayout() {
        Header header = getSecondaryHeader("Speaker management", "Manage profiles, bios, and affiliations");

        name.setWidthFull();
        title.setWidthFull();
        company.setWidthFull();
        photoUrl.setWidthFull();
        bio.setWidthFull();
        countryField.setWidthFull();
        countryField.setWidthFull();
        socialLinksLayout.setWidthFull();
        addSocialLinkButton.addClassNames(LumoUtility.Width.AUTO);

        CommonUtils.commentsFormat(bio, 3000);

        Div socialLayout = new Div(addSocialLinkButton, socialLinksLayout);
        socialLayout.setWidthFull();
        socialLayout.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN
        );

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSizeFull();
        formLayout.setPadding(false);
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, name, title, company, countryField, photoUrl, socialLayout, bio);

        return formLayout;
    }


    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {

        try {

            if (this.element == null) {
                this.element = new Speaker();
            }

            Set<String> currentSocialLinks = new HashSet<>();
            socialLinksLayout.getChildren()
                    .filter(HorizontalLayout.class::isInstance)
                    .forEach(row -> (row).getChildren()
                            .filter(TextField.class::isInstance)
                            .findFirst()
                            .ifPresent(textField -> {
                                String linkValue = ((TextField) textField).getValue();
                                if (linkValue != null && !linkValue.trim().isEmpty()) {
                                    currentSocialLinks.add(linkValue.trim());
                                }
                            }));
            this.element.setNetworking(currentSocialLinks);


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
                NotificationUtils.info(ex.getMessage());
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

    }


    private void populateForm(Speaker value) {
        this.element = value;

        binder.readBean(this.element);

        socialLinksLayout.removeAll();
        if (value != null) {
            value.getNetworking().forEach(this::addSocialLinkField);
        }

        delete.setEnabled(element != null);
    }

}
