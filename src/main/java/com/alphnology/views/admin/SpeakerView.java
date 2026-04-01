package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.Contactable;
import com.alphnology.data.Country;
import com.alphnology.data.Speaker;
import com.alphnology.exceptions.DeleteConstraintViolationException;
import com.alphnology.infrastructure.storage.ObjectStorageService;
import com.alphnology.services.QrService;
import com.alphnology.services.SpeakerService;
import com.alphnology.utils.*;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
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
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.util.StringUtils;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;

@Slf4j
@PageTitle("Speakers")
@Route("admin/speaker")
@Menu(order = 15, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class SpeakerView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final VirtualList<Speaker> list = new VirtualList<>();
    private final Span countBadge = new Span("0");
    private Speaker selectedItem;

    private final TextField name = new TextField("Name");
    private final TextField title = new TextField("Title");
    private final TextField company = new TextField("Company");
    private final EmailField email = new EmailField("Email");
    private final TextField phone = new TextField("Phone");
    private final Image image = new Image();
    private Upload imageUpload;
    private final TextArea bio = new TextArea("Biography");
    private final ComboBox<Country> countryField = new ComboBox<>("Select a country");

    private final VerticalLayout socialLinksLayout = new VerticalLayout();
    private final Button addSocialLinkButton = new Button("Add social link", VaadinIcon.PLUS.create());

    private final Button removeImageButton = new Button("Remove image", VaadinIcon.TRASH.create());
    private final Button cancel = new Button("New", VaadinIcon.FILE_ADD.create());
    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());
    private final Button delete = new Button("Delete", VaadinIcon.TRASH.create());

    private final Button viewQrButton = new Button("View QR", VaadinIcon.QRCODE.create());
    private final Button downloadQrButton = new Button("Download QR", VaadinIcon.DOWNLOAD_ALT.create());
    private final Anchor downloadQrAnchor = new Anchor();

    private final transient SpeakerService service;
    private final transient QrService qrService;
    private final transient ObjectStorageService storageService;

    private Speaker element;
    private transient String photoKeyPendingDeletion;

    private final Binder<Speaker> binder = new BeanValidationBinder<>(Speaker.class);

    public SpeakerView(SpeakerService service, QrService qrService, ObjectStorageService storageService) {
        this.service = service;
        this.qrService = qrService;
        this.storageService = storageService;

        countryField.setItems(CountryUtils.getCountryNamesWithCodes());
        countryField.setPlaceholder("Choose a country");

        binder.bindInstanceFields(this);
        binder.getFields().forEach(field -> {
            if (field instanceof HasClearButton clear) clear.setClearButtonVisible(true);
        });
        binder.forField(countryField)
                .bind(speaker -> {
                            if (speaker == null) return null;
                            String code = speaker.getCountry();
                            return CountryUtils.getCountryNamesWithCodes().stream()
                                    .filter(c -> Objects.equals(code, c.getCode()))
                                    .findFirst().orElse(null);
                        },
                        (speaker, selected) -> {
                            if (speaker != null)
                                speaker.setCountry(selected != null ? selected.getCode() : null);
                        });

        initList();

        // ── Sidebar (left pane) ──
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

        // ── Form panel (right pane) ──
        Footer footer = new Footer(createFooter());
        Scroller formScroller = getScrollerVertical();
        formScroller.setContent(createFormLayout());
        VerticalLayout form = getSecondaryLayout(formScroller, footer);

        // ── Split layout ──
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

        removeImageButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        removeImageButton.addClickListener(e -> {
            if (element != null && element.getPhotoKey() != null) {
                photoKeyPendingDeletion = element.getPhotoKey();
                element.setPhotoKey(null);
            }
            image.setVisible(false);
            image.setSrc("");
            imageUpload.clearFileList();
            removeImageButton.setVisible(false);
        });
        removeImageButton.setVisible(false);

        viewQrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        downloadQrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        addSocialLinkButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);

        socialLinksLayout.setSpacing(false);
        socialLinksLayout.setPadding(false);
        addSocialLinkButton.addClickListener(e -> addSocialLinkField(null));
    }

    @NotNull
    private Anchor getAnchor() {
        DownloadHandler qrResource = DownloadHandler.fromInputStream(event1 -> {
            try {
                byte[] zipBytes = createZipBytesForAllQrs();
                return new DownloadResponse(new ByteArrayInputStream(zipBytes), "all-speakers-qrs.zip", null, zipBytes.length);
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
            List<Speaker> speakers = service.findAll(createFilterSpecification());
            Map<String, byte[]> filesToZip = new HashMap<>();
            for (Speaker speaker : speakers) {
                String vCardUrl = VCardUtil.getVCardUrl(new Contactable(speaker), "speaker");
                byte[] qrBytes = qrService.generatePng(vCardUrl, 256);
                filesToZip.put(speaker.getName().replace(" ", "_") + ".png", qrBytes);
            }
            return ZipUtils.createZip(filesToZip);
        } catch (Exception e) {
            log.error("Error creating ZIP file with QR codes", e);
            NotificationUtils.error("Could not generate ZIP file.");
            return new byte[0];
        }
    }

    private Specification<Speaker> createFilterSpecification() {
        return (root, query, builder) -> {
            final String search = searchField.getValue().toLowerCase().trim();
            Order order = builder.asc(root.get("name"));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);
            Predicate predicateName = predicateUnaccentLike(root, builder, "name", search);
            Predicate predicateTitle = predicateUnaccentLike(root, builder, "title", search);
            Predicate predicateCompany = predicateUnaccentLike(root, builder, "company", search);
            Predicate predicateEmail = predicateUnaccentLike(root, builder, "email", search);
            Predicate predicateBio = predicateUnaccentLike(root, builder, "bio", search);
            return builder.or(predicateName, predicateTitle, predicateCompany, predicateEmail, predicateBio);
        };
    }

    private void initList() {
        searchField.setWidthFull();
        searchField.setPlaceholder("Search speakers...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshList());

        list.setSizeFull();
        list.setRenderer(new ComponentRenderer<>(this::buildListItem));
    }

    private Div buildListItem(Speaker speaker) {
        Div item = new Div();
        item.addClassName("admin-list-item");
        if (selectedItem != null && Objects.equals(selectedItem.getCode(), speaker.getCode())) {
            item.addClassName("selected");
        }

        Div iconDiv = new Div();
        iconDiv.addClassName("admin-item-icon");
        if (StringUtils.hasText(speaker.getPhotoKey())) {
            Image img = new Image(storageService.getSignedUrl(speaker.getPhotoKey()), speaker.getName());
            iconDiv.add(img);
        } else {
            Icon icon = VaadinIcon.USER.create();
            icon.setSize("18px");
            iconDiv.add(icon);
        }

        Div body = new Div();
        body.addClassName("admin-item-body");
        Span nameSpan = new Span(speaker.getName());
        nameSpan.addClassName("admin-item-name");
        String sub = Stream.of(speaker.getTitle(), speaker.getCompany())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" · "));
        Span subSpan = new Span(sub);
        subSpan.addClassName("admin-item-sub");
        body.add(nameSpan, subSpan);

        Div side = new Div();
        side.addClassName("admin-item-side");
        if (StringUtils.hasText(speaker.getCountry())) {
            Image flag = new Image("https://flagcdn.com/%s.svg".formatted(speaker.getCountry().toLowerCase()), "");
            flag.addClassName("admin-item-flag");
            side.add(flag);
        }

        item.add(iconDiv, body, side);
        item.addClickListener(e -> selectItem(speaker));
        return item;
    }

    private void selectItem(Speaker speaker) {
        populateForm(speaker);
        list.getDataProvider().refreshAll();
    }

    private void refreshList() {
        List<Speaker> items = service.findAll(createFilterSpecification());
        list.setItems(items);
        countBadge.setText(String.valueOf(items.size()));
    }

    private void addSocialLinkField(String url) {
        TextField linkField = new TextField();
        linkField.setWidthFull();
        linkField.setPlaceholder("https://openschedule.com");
        if (url != null) linkField.setValue(url);

        Button removeLinkButton = new Button(VaadinIcon.TRASH.create(),
                e -> e.getSource().getParent().ifPresent(socialLinksLayout::remove));
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
        email.setWidthFull();
        phone.setWidthFull();
        image.setWidthFull();
        bio.setWidthFull();
        countryField.setWidthFull();
        socialLinksLayout.setWidthFull();
        addSocialLinkButton.addClassNames(LumoUtility.Width.AUTO);

        CommonUtils.commentsFormat(bio, 3000);

        Div socialLayout = new Div(addSocialLinkButton, socialLinksLayout);
        socialLayout.setWidthFull();
        socialLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

        var handler = UploadHandler.inMemory((meta, bytes) -> UI.getCurrent().access(() -> {
            if (element == null) element = new Speaker();
            if (element.getPhotoKey() != null) photoKeyPendingDeletion = element.getPhotoKey();
            String key = "speakers/" + UUID.randomUUID();
            String contentType = meta.contentType() != null ? meta.contentType() : "image/jpeg";
            storageService.upload(key, new ByteArrayInputStream(bytes), bytes.length, contentType);
            element.setPhotoKey(key);
            image.setVisible(true);
            image.setSrc(storageService.getSignedUrl(key));
            removeImageButton.setVisible(true);
        }));
        imageUpload = new Upload(handler);
        imageUpload.setAcceptedFileTypes("image/*");
        imageUpload.setMaxFiles(1);
        imageUpload.setDropLabel(new Span("Drag your image here"));
        imageUpload.setWidthFull();

        viewQrButton.addClickListener(event -> {
            if (element != null && element.getCode() != null) {
                VCardUtil.openQr(new Contactable(element), "speaker", qrService);
            } else {
                NotificationUtils.info("Please select a speaker first.");
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
        formLayout.add(header, name, title, company, countryField, email, phone,
                imageUpload, image, removeImageButton, socialLayout, bio, qrButtonsLayout);
        return formLayout;
    }

    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {
        try {
            if (this.element == null) this.element = new Speaker();

            Set<String> currentSocialLinks = new HashSet<>();
            socialLinksLayout.getChildren()
                    .filter(HorizontalLayout.class::isInstance)
                    .forEach(row -> row.getChildren()
                            .filter(TextField.class::isInstance)
                            .findFirst()
                            .ifPresent(tf -> {
                                String val = ((TextField) tf).getValue();
                                if (val != null && !val.trim().isEmpty())
                                    currentSocialLinks.add(val.trim());
                            }));
            this.element.setNetworking(currentSocialLinks);

            binder.writeBean(this.element);

            final String keyToDelete = photoKeyPendingDeletion;
            ConfirmationDialog.confirmation(event -> {
                service.save(element);
                if (keyToDelete != null) {
                    try { storageService.delete(keyToDelete); } catch (Exception ex) { log.warn("Could not delete old photo: {}", keyToDelete); }
                }
                photoKeyPendingDeletion = null;
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
        photoKeyPendingDeletion = null;
        populateForm(null);
        list.getDataProvider().refreshAll();
    }

    private void populateForm(Speaker value) {
        this.element = value;
        this.selectedItem = value;
        binder.readBean(this.element);

        viewQrButton.setVisible(value != null);
        downloadQrAnchor.setVisible(value != null);

        socialLinksLayout.removeAll();
        imageUpload.clearFileList();
        if (value != null) {
            value.getNetworking().forEach(this::addSocialLinkField);
            if (StringUtils.hasText(value.getPhotoKey())) {
                image.setSrc(storageService.getSignedUrl(value.getPhotoKey()));
                image.setAlt(value.getName());
                image.setVisible(true);
                removeImageButton.setVisible(true);
            } else {
                image.setVisible(false);
                removeImageButton.setVisible(false);
            }
            String vCardUrl = VCardUtil.getVCardUrl(new Contactable(element), "speaker");
            DownloadHandler qrResource = VCardUtil.downloadHandler(qrService, vCardUrl, element.getName().replace(" ", ""));
            downloadQrAnchor.setHref(qrResource);
        } else {
            image.setVisible(false);
            removeImageButton.setVisible(false);
        }
        delete.setEnabled(element != null);
    }
}
