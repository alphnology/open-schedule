package com.alphnology.views.admin;

import com.alphnology.components.ConfirmationDialog;
import com.alphnology.data.News;
import com.alphnology.data.User;
import com.alphnology.infrastructure.storage.ObjectStorageService;
import com.alphnology.services.NewsService;
import com.alphnology.utils.DateTimeFormatterUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
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
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.util.StringUtils;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;
import static com.alphnology.utils.ViewHelper.*;

@Slf4j
@PageTitle("News")
@Route("admin/news")
@Menu(order = 16, icon = LineAwesomeIconUrl.FILE)
@RolesAllowed("ADMIN")
public class NewsView extends VerticalLayout {

    private final TextField searchField = new TextField();
    private final VirtualList<News> list = new VirtualList<>();
    private final Span countBadge = new Span("0");
    private News selectedItem;

    private final TextField title = new TextField("Title");
    private final TextArea content = new TextArea("Content");
    private final Image image = new Image();
    private Upload imageUpload;
    private Button removeImageButton;

    private final Button cancel = new Button("New", VaadinIcon.FILE_ADD.create());
    private final Button save = new Button("Save", VaadinIcon.HARDDRIVE_O.create());
    private final Button delete = new Button("Delete", VaadinIcon.TRASH.create());

    private final transient NewsService service;
    private final transient ObjectStorageService storageService;
    private News element;
    private transient String photoKeyPendingDeletion;

    private final Binder<News> binder = new BeanValidationBinder<>(News.class);

    public NewsView(NewsService service, ObjectStorageService storageService) {
        this.service = service;
        this.storageService = storageService;

        binder.bindInstanceFields(this);
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
        Scroller formScroller = getScrollerVertical();
        formScroller.setContent(createFormLayout());
        VerticalLayout form = getSecondaryLayout(formScroller, new Footer(createFooter()));

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

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
    }

    private Specification<News> createFilterSpecification() {
        return (root, query, builder) -> {
            final String search = searchField.getValue().toLowerCase().trim();
            Order order = builder.desc(root.get("publishedAt"));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);
            Predicate predicateTitle = predicateUnaccentLike(root, builder, "title", search);
            final List<Predicate> orPredicates = new ArrayList<>(List.of(predicateTitle));
            return builder.or(orPredicates.toArray(Predicate[]::new));
        };
    }

    private void initList() {
        searchField.setWidthFull();
        searchField.setPlaceholder("Search news...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshList());

        list.setSizeFull();
        list.setRenderer(new ComponentRenderer<>(this::buildListItem));
    }

    private Div buildListItem(News news) {
        Div item = new Div();
        item.addClassName("admin-list-item");
        if (selectedItem != null && Objects.equals(selectedItem.getCode(), news.getCode())) {
            item.addClassName("selected");
        }

        // Icon: thumbnail or newspaper icon
        Div iconDiv = new Div();
        iconDiv.addClassName("admin-item-icon");
        if (StringUtils.hasText(news.getPhotoKey())) {
            Image img = new Image(storageService.getSignedUrl(news.getPhotoKey()), news.getTitle());
            iconDiv.add(img);
        } else {
            Icon icon = VaadinIcon.NEWSPAPER.create();
            icon.setSize("18px");
            iconDiv.add(icon);
        }

        // Body
        Div body = new Div();
        body.addClassName("admin-item-body");
        Span nameSpan = new Span(news.getTitle());
        nameSpan.addClassName("admin-item-name");
        String authorName = news.getAuthor() != null ? news.getAuthor().getName() : "";
        String dateStr = news.getPublishedAt() != null
                ? news.getPublishedAt().format(DateTimeFormatterUtils.dateTimeFormatter) : "";
        String sub = List.of(authorName, dateStr).stream()
                .filter(StringUtils::hasText).reduce((a, b) -> a + " · " + b).orElse("");
        Span subSpan = new Span(sub);
        subSpan.addClassName("admin-item-sub");
        body.add(nameSpan, subSpan);

        item.add(iconDiv, body);
        item.addClickListener(e -> selectItem(news));
        return item;
    }

    private void selectItem(News news) {
        populateForm(news);
        list.getDataProvider().refreshAll();
    }

    private void refreshList() {
        List<News> items = service.findAll(createFilterSpecification());
        list.setItems(items);
        countBadge.setText(String.valueOf(items.size()));
    }

    private VerticalLayout createFormLayout() {
        Header header = getSecondaryHeader("News Management", "Create, edit, and publish news");

        title.setWidthFull();
        content.setWidthFull();
        content.setHeight("200px");
        image.setWidthFull();

        var handler = UploadHandler.inMemory((meta, bytes) -> UI.getCurrent().access(() -> {
            if (element == null) element = new News();
            if (element.getPhotoKey() != null) photoKeyPendingDeletion = element.getPhotoKey();
            String key = "news/" + UUID.randomUUID();
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

        removeImageButton = new Button("Remove image", VaadinIcon.TRASH.create());
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

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSizeFull();
        formLayout.setPadding(false);
        formLayout.setSpacing(false);
        formLayout.setMargin(false);
        formLayout.addClassNames(LumoUtility.Padding.SMALL, LumoUtility.Gap.SMALL);
        formLayout.add(header, title, content, imageUpload, image, removeImageButton);
        return formLayout;
    }

    private void saveOrUpdate(ClickEvent<Button> buttonClickEvent) {
        try {
            if (this.element == null) this.element = new News();
            if (element.getCode() == null) {
                this.element.setPublishedAt(LocalDateTime.now());
                this.element.setAuthor(VaadinSession.getCurrent().getAttribute(User.class));
            }
            binder.writeBean(this.element);
            final String keyToDelete = photoKeyPendingDeletion;
            ConfirmationDialog.confirmation(event -> {
                service.save(element);
                if (keyToDelete != null) {
                    try { storageService.delete(keyToDelete); } catch (Exception ex) { log.warn("Could not delete old news photo: {}", keyToDelete); }
                }
                photoKeyPendingDeletion = null;
                populateForm(element);
                refreshList();
                NotificationUtils.success();
            });
        } catch (ObjectOptimisticLockingFailureException ex) {
            NotificationUtils.error("Error updating the data. Somebody else has updated the record.");
        } catch (ValidationException ex) {
            NotificationUtils.error(ex);
        }
    }

    private void delete(ClickEvent<Button> buttonClickEvent) {
        ConfirmationDialog.delete(event -> {
            try {
                service.delete(element.getCode());
                clearForm();
                refreshList();
                NotificationUtils.success("News deleted successfully.");
            } catch (Exception e) {
                NotificationUtils.error("Error deleting news: " + e.getMessage());
            }
        });
    }

    private HorizontalLayout createFooter() {
        HorizontalLayout buttonLayout = new HorizontalLayout(save, cancel, delete);
        buttonLayout.setFlexGrow(1, save, cancel, delete);
        buttonLayout.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.CONTRAST_5);
        buttonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return buttonLayout;
    }

    private void clearForm() {
        photoKeyPendingDeletion = null;
        populateForm(null);
        list.getDataProvider().refreshAll();
    }

    private void populateForm(News value) {
        this.element = value;
        this.selectedItem = value;
        binder.readBean(this.element);
        imageUpload.clearFileList();

        if (value != null) {
            if (StringUtils.hasText(value.getPhotoKey())) {
                image.setSrc(storageService.getSignedUrl(value.getPhotoKey()));
                image.setAlt(value.getTitle());
                image.setVisible(true);
                removeImageButton.setVisible(true);
            } else {
                image.setVisible(false);
                removeImageButton.setVisible(false);
            }
        } else {
            image.setVisible(false);
            removeImageButton.setVisible(false);
        }
        delete.setEnabled(element != null);
    }
}
