package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.Attender;
import com.alphnology.data.Contactable;
import com.alphnology.data.Country;
import com.alphnology.exceptions.DeleteConstraintViolationException;
import com.alphnology.services.AttenderService;
import com.alphnology.services.QrService;
import com.alphnology.utils.CountryUtils;
import com.alphnology.utils.NotificationUtils;
import com.alphnology.utils.VCardUtil;
import com.alphnology.utils.ZipUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.util.StringUtils;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;

@Slf4j
@PageTitle("Attender")
@Route("admin/attender")
@Menu(order = 18, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class AttenderView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final VirtualList<Attender> list = new VirtualList<>();
    private final Span countBadge = new Span("0");
    private Attender selectedItem;

    private final TextField name = new TextField("Name");
    private final TextField lastName = new TextField("Last name");
    private final TextField company = new TextField("Company");
    private final TextField title = new TextField("Title");
    private final EmailField email = new EmailField("Email");
    private final TextField phone = new TextField("Phone");
    private final ComboBox<Country> countryField = new ComboBox<>("Select a country");

    private final Button cancel = new Button("New", VaadinIcon.FILE_ADD.create());
    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());
    private final Button delete = new Button("Delete", VaadinIcon.TRASH.create());

    private final Button viewQrButton = new Button("View QR", VaadinIcon.QRCODE.create());
    private final Button downloadQrButton = new Button("Download QR", VaadinIcon.DOWNLOAD_ALT.create());
    private final Anchor downloadQrAnchor = new Anchor();

    private final transient AttenderService service;
    private final transient QrService qrService;
    private Attender element;

    private final Binder<Attender> binder = new BeanValidationBinder<>(Attender.class);

    public AttenderView(AttenderService service, QrService qrService) {
        this.service = service;
        this.qrService = qrService;

        countryField.setItems(CountryUtils.getCountryNamesWithCodes());
        countryField.setPlaceholder("Choose a country");

        binder.bindInstanceFields(this);
        binder.getFields().forEach(field -> {
            if (field instanceof HasClearButton clear) clear.setClearButtonVisible(true);
        });
        binder.forField(countryField)
                .bind(attender -> {
                            if (attender == null) return null;
                            String code = attender.getCountry();
                            return CountryUtils.getCountryNamesWithCodes().stream()
                                    .filter(c -> Objects.equals(code, c.getCode()))
                                    .findFirst().orElse(null);
                        },
                        (attender, selected) -> {
                            if (attender != null)
                                attender.setCountry(selected != null ? selected.getCode() : null);
                        });

        initList();

        // ── Sidebar ──
        Anchor downloadAllAnchor = getAnchor();
        Button downloadAllBtn = new Button("QRs", VaadinIcon.DOWNLOAD.create());
        downloadAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        downloadAllAnchor.add(downloadAllBtn);
        downloadAllAnchor.getElement().setAttribute("download", true);

        countBadge.addClassName("admin-count-badge");
        HorizontalLayout toolbar = new HorizontalLayout(searchField, countBadge, downloadAllAnchor);
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

        cancel.addClickListener(e -> clearForm());
        save.addClickListener(this::saveOrUpdate);
        delete.addClickListener(this::delete);

        viewQrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        downloadQrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
    }

    @NotNull
    private Anchor getAnchor() {
        DownloadHandler qrResource = DownloadHandler.fromInputStream(event1 -> {
            try {
                byte[] zipBytes = createZipBytesForAllQrs();
                return new DownloadResponse(new ByteArrayInputStream(zipBytes), "all-attender-qrs.zip", null, zipBytes.length);
            } catch (Exception e) {
                return DownloadResponse.error(500);
            }
        });
        Anchor anchor = new Anchor();
        anchor.setHref(qrResource);
        return anchor;
    }

    private byte[] createZipBytesForAllQrs() {
        try {
            List<Attender> attenders = service.findAll(createFilterSpecification());
            Map<String, byte[]> filesToZip = new HashMap<>();
            for (Attender attender : attenders) {
                String vCardUrl = VCardUtil.getVCardUrl(new Contactable(attender), "attender");
                byte[] qrBytes = qrService.generatePng(vCardUrl, 256);
                String fileName = StringUtils.hasText(attender.getPhone())
                        ? attender.getPhone() + attender.getName().replace(" ", "_") + attender.getLastName().replace(" ", "_") + ".png"
                        : attender.getName().replace(" ", "_") + attender.getLastName().replace(" ", "_") + ".png";
                filesToZip.put(fileName, qrBytes);
            }
            return ZipUtils.createZip(filesToZip);
        } catch (Exception e) {
            log.error("Error creating ZIP file with QR codes", e);
            NotificationUtils.error("Could not generate ZIP file.");
            return new byte[0];
        }
    }

    private Specification<Attender> createFilterSpecification() {
        return (root, query, builder) -> {
            final String search = searchField.getValue().toLowerCase().trim();
            Order order = builder.asc(root.get("name"));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);
            Predicate predicateName = predicateUnaccentLike(root, builder, "name", search);
            Predicate predicateLastName = predicateUnaccentLike(root, builder, "lastName", search);
            Predicate predicateTitle = predicateUnaccentLike(root, builder, "title", search);
            Predicate predicateCompany = predicateUnaccentLike(root, builder, "company", search);
            Predicate predicateEmail = predicateUnaccentLike(root, builder, "email", search);
            return builder.or(predicateName, predicateLastName, predicateTitle, predicateCompany, predicateEmail);
        };
    }

    private void initList() {
        searchField.setWidthFull();
        searchField.setPlaceholder("Search attenders...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshList());

        list.setSizeFull();
        list.setRenderer(new ComponentRenderer<>(this::buildListItem));
    }

    private Div buildListItem(Attender attender) {
        Div item = new Div();
        item.addClassName("admin-list-item");
        if (selectedItem != null && Objects.equals(selectedItem.getCode(), attender.getCode())) {
            item.addClassName("selected");
        }

        // Icon
        Div iconDiv = new Div();
        iconDiv.addClassName("admin-item-icon");
        Icon icon = VaadinIcon.USER.create();
        icon.setSize("18px");
        iconDiv.add(icon);

        // Body
        Div body = new Div();
        body.addClassName("admin-item-body");
        String fullName = Stream.of(attender.getName(), attender.getLastName())
                .filter(StringUtils::hasText).collect(Collectors.joining(" "));
        Span nameSpan = new Span(fullName);
        nameSpan.addClassName("admin-item-name");
        String sub = Stream.of(attender.getCompany(), attender.getTitle())
                .filter(StringUtils::hasText).collect(Collectors.joining(" · "));
        Span subSpan = new Span(sub);
        subSpan.addClassName("admin-item-sub");
        body.add(nameSpan, subSpan);

        // Side: country flag
        Div side = new Div();
        side.addClassName("admin-item-side");
        if (StringUtils.hasText(attender.getCountry())) {
            Image flag = new Image("https://flagcdn.com/%s.svg".formatted(attender.getCountry().toLowerCase()), "");
            flag.addClassName("admin-item-flag");
            side.add(flag);
        }

        item.add(iconDiv, body, side);
        item.addClickListener(e -> selectItem(attender));
        return item;
    }

    private void selectItem(Attender attender) {
        populateForm(attender);
        list.getDataProvider().refreshAll();
    }

    private void refreshList() {
        List<Attender> items = service.findAll(createFilterSpecification());
        list.setItems(items);
        countBadge.setText(String.valueOf(items.size()));
    }

    private VerticalLayout createFormLayout() {
        Header header = getSecondaryHeader("Attender management", "Manage attender profiles");

        name.setWidthFull();
        lastName.setWidthFull();
        title.setWidthFull();
        company.setWidthFull();
        email.setWidthFull();
        phone.setWidthFull();
        countryField.setWidthFull();

        viewQrButton.addClickListener(event -> {
            if (element != null && element.getCode() != null) {
                VCardUtil.openQr(new Contactable(element), "attender", qrService);
            } else {
                NotificationUtils.info("Please select an attender first.");
            }
        });
        viewQrButton.setVisible(false);

        downloadQrAnchor.setVisible(false);
        downloadQrAnchor.getElement().setAttribute("download", true);
        downloadQrAnchor.add(downloadQrButton);

        Div qrButtonsLayout = new Div(viewQrButton, downloadQrAnchor);
        qrButtonsLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL,
                LumoUtility.JustifyContent.CENTER, LumoUtility.Width.FULL);

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSizeFull();
        formLayout.setSpacing(false);
        formLayout.setPadding(false);
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL);
        formLayout.add(header, name, lastName, title, company, countryField, email, phone, qrButtonsLayout);
        return formLayout;
    }

    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {
        try {
            if (this.element == null) this.element = new Attender();
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
                NotificationUtils.info(ex.getMessage());
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
    }

    private void populateForm(Attender value) {
        this.element = value;
        this.selectedItem = value;
        binder.readBean(this.element);

        viewQrButton.setVisible(value != null);
        downloadQrAnchor.setVisible(value != null);

        if (value != null) {
            String vCardUrl = VCardUtil.getVCardUrl(new Contactable(element), "attender");
            DownloadHandler qrResource = VCardUtil.downloadHandler(qrService, vCardUrl, element.getName().replace(" ", ""));
            downloadQrAnchor.setHref(qrResource);
        }
        delete.setEnabled(element != null);
    }
}
