package com.alphnology.views;

import com.alphnology.data.Event;
import com.alphnology.infrastructure.storage.ObjectStorageService;
import com.alphnology.data.User;
import com.alphnology.security.AuthenticatedUser;
import com.alphnology.services.EventService;
import com.alphnology.utils.ImageUtils;
import com.alphnology.views.login.ChangePasswordView;
import com.alphnology.views.login.LogoutView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The main view is a top-level placeholder for other views.
 */
@Slf4j
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout implements BeforeEnterObserver, AfterNavigationObserver {

    private H1 viewTitle;
    private boolean drawerCollapsed;
    private Button collapseToggle;
    private final List<Popover> groupPopovers = new ArrayList<>();

    private transient AuthenticatedUser authenticatedUser;
    private final transient EventService eventService;
    private final transient ObjectStorageService storageService;

    private final String eventWebsite;
    private final String appVersion;

    public MainLayout(
            AuthenticatedUser authenticatedUser,
            EventService eventService,
            ObjectStorageService storageService,
            @Value("${event.website}") String eventWebsite,
            @Value("${application.version}") String appVersion
    ) {
        this.authenticatedUser = authenticatedUser;
        this.eventService = eventService;
        this.storageService = storageService;
        this.eventWebsite = eventWebsite;
        this.appVersion = appVersion;

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");
        toggle.addClassName("drawer-toggle-mobile");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE, LumoUtility.Margin.End.AUTO);
        viewTitle.addClassNames(LumoUtility.Display.HIDDEN, LumoUtility.Display.Breakpoint.Large.BLOCK);

        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.addClassNames(LumoUtility.Height.XLARGE);

        layout.add(toggle);
        layout.add(createCollapseToggle());

        layout.add(viewTitle);

        layout.add(createUserInfo());

        addToNavbar(true, layout);
    }

    private void addDrawerContent() {
        Image image = createDrawerImage();
        image.addClassNames(LumoUtility.AlignSelf.CENTER, LumoUtility.Margin.Top.SMALL);
        image.setWidth("100%");

        Header header = new Header(image);
        header.addClassNames(LumoUtility.JustifyContent.CENTER);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());

        // Attach group popovers to the DOM — they render in the overlay layer
        if (!groupPopovers.isEmpty()) {
            Div popoverHost = new Div();
            groupPopovers.forEach(popoverHost::add);
            addToDrawer(popoverHost);
        }
    }

    private Image createDrawerImage() {
        Optional<Event> currentEvent = eventService.findCurrentEvent();
        if (currentEvent.isPresent()) {
            String photoKey = currentEvent.get().getPhotoKey();
            if (photoKey != null && !photoKey.isBlank()) {
                try {
                    return new Image(storageService.getSignedUrl(photoKey), currentEvent.get().getName());
                } catch (Exception e) {
                    log.warn("Could not load event image from storage: {}", photoKey, e);
                }
            }
        }
        return ImageUtils.getDefaultMainImage();
    }

    private Button createCollapseToggle() {
        collapseToggle = new Button(VaadinIcon.ANGLE_DOUBLE_LEFT.create());
        collapseToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        collapseToggle.addClassName("drawer-collapse-toggle");
        collapseToggle.setAriaLabel("Collapse sidebar");
        collapseToggle.addClickListener(event -> toggleDrawerCollapse());
        return collapseToggle;
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();
        Map<String, SideNavItem> parentItems = new HashMap<>();
        Map<String, List<MenuEntry>> groupChildEntries = new HashMap<>();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();

        for (MenuEntry entry : menuEntries) {
            String path = entry.path();
            if (path == null || path.isEmpty()) {
                continue;
            }

            String[] segments = path.replaceFirst("^/", "").split("/");

            if (segments.length == 1) {
                nav.addItem(createSideNavItemFromEntry(entry));
            } else if (segments.length > 1) {
                String groupName = segments[0];
                groupChildEntries.computeIfAbsent(groupName, k -> new ArrayList<>()).add(entry);

                SideNavItem parentItem = parentItems.computeIfAbsent(groupName, key -> {
                    String parentTitle = key.substring(0, 1).toUpperCase() + key.substring(1);
                    SideNavItem newParent = new SideNavItem(parentTitle);
                    newParent.setPrefixComponent(new SvgIcon("line-awesome/svg/cog-solid.svg"));
                    nav.addItem(newParent);
                    return newParent;
                });

                parentItem.addItem(createSideNavItemFromEntry(entry));
            }
        }

        // Build popovers for each group — shown only when sidebar is collapsed
        parentItems.forEach((groupName, parentItem) -> {
            List<MenuEntry> children = groupChildEntries.getOrDefault(groupName, List.of());
            if (!children.isEmpty()) {
                groupPopovers.add(buildGroupPopover(groupName, parentItem, children));
            }
        });

        SideNavItem eventLink = new SideNavItem("Event website", eventWebsite, LineAwesomeIcon.GLOBE_SOLID.create());
        eventLink.setOpenInNewBrowserTab(true);
        eventLink.addClassName("external");
        nav.addItem(eventLink);

        SideNavItem contribute = new SideNavItem("Contribute", "https://github.com/alphnology/open-schedule", LineAwesomeIcon.GITHUB.create());
        contribute.setOpenInNewBrowserTab(true);
        contribute.addClassName("external");
        nav.addItem(contribute);

        return nav;
    }

    private Popover buildGroupPopover(String groupName, SideNavItem target, List<MenuEntry> entries) {
        Popover popover = new Popover();

        popover.setTarget(target);
        popover.setPosition(PopoverPosition.END_TOP);
        popover.addThemeVariants(PopoverVariant.LUMO_NO_PADDING);
        popover.setOpenOnClick(false);
        popover.setOpenOnHover(false); // enabled only when sidebar is collapsed

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setWidth("220px");
        content.addClassNames(LumoUtility.Padding.XSMALL, "collapsed-nav-popover-content");

        Span label = new Span(groupName.substring(0, 1).toUpperCase() + groupName.substring(1));
        label.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s) 0")
                .set("letter-spacing", "0.05em")
                .set("text-transform", "uppercase");
        content.add(label);

        for (MenuEntry entry : entries) {
            content.add(createPopoverNavItem(entry));
        }

        popover.add(content);
        return popover;
    }

    private void updateGroupPopovers(boolean collapsed) {
        groupPopovers.forEach(p -> p.setOpenOnHover(collapsed));
    }

    /**
     * Helper method to create a SideNavItem from a MenuEntry,
     * avoiding code duplication.
     *
     * @param entry The MenuEntry to convert.
     * @return A configured SideNavItem.
     */
    private SideNavItem createSideNavItemFromEntry(MenuEntry entry) {
        SideNavItem item;
        if (entry.icon() != null) {
            item = new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon()));
        } else {
            item = new SideNavItem(entry.title(), entry.path());
        }
        // This is useful so the parent group remains highlighted when navigating to a child view.
        item.setMatchNested(true);
        item.addClassNames(LumoUtility.Width.FULL);
        return item;
    }

    private Div createPopoverNavItem(MenuEntry entry) {
        Div item = new Div();
        item.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.SMALL,
                LumoUtility.Padding.SMALL,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Width.FULL,
                "hover:bg-contrast-10",
                "transition-colors"
        );
        item.getStyle().set("cursor", "pointer").set("box-sizing", "border-box");
        item.addClickListener(e -> UI.getCurrent().navigate(entry.path()));

        if (entry.icon() != null) {
            SvgIcon icon = new SvgIcon(entry.icon());
            icon.getStyle()
                    .set("width", "var(--lumo-icon-size-m)")
                    .set("height", "var(--lumo-icon-size-m)")
                    .set("flex-shrink", "0");
            item.add(icon);
        }
        item.add(new Span(entry.title()));
        return item;
    }

    private HorizontalLayout createUserInfo() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.addClassNames(LumoUtility.Margin.Right.LARGE);

        Optional<User> maybeUser = authenticatedUser.get();

        if (maybeUser.isPresent()) {
            User user = maybeUser.get();

//            StreamResource resource = new StreamResource("profile-pic", () -> new ByteArrayInputStream(new byte[]{}));

            Avatar avatar = new Avatar(user.getName());
//            avatar.setImageResource(resource);
            avatar.getStyle().set("display", "block");
            avatar.getStyle().set("cursor", "pointer");
            avatar.getElement().setAttribute("tabindex", "-1");

            Button button = new Button(avatar);
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE);
            button.addClassNames(LumoUtility.Width.AUTO, LumoUtility.BorderRadius.FULL);
            button.getStyle().set("margin-inline-start", "auto");

            Popover popover = new Popover();
            popover.setModal(true);
            popover.setRole("menu");
            popover.setAriaLabel("User menu");
            popover.setTarget(button);
            popover.setPosition(PopoverPosition.BOTTOM_END);
            popover.addThemeVariants(PopoverVariant.LUMO_NO_PADDING);

            HorizontalLayout userInfo = new HorizontalLayout();
            userInfo.addClassName("userMenuHeader");
            userInfo.addClassNames(LumoUtility.Gap.SMALL);

            Avatar userAvatar = new Avatar(user.getName());
//            userAvatar.setImageResource(resource);
            userAvatar.getElement().setAttribute("tabindex", "-1");
            userAvatar.addThemeVariants(AvatarVariant.LUMO_LARGE);

            VerticalLayout nameLayout = new VerticalLayout();
            nameLayout.setSpacing(false);
            nameLayout.setPadding(false);

            Div fullName = new Div(user.getName());
            fullName.addClassNames(LumoUtility.FontWeight.BOLD);
            Div nickName = new Div(user.getUsername().toLowerCase());
            nickName.addClassName("userMenuNickname");
            nameLayout.add(fullName, nickName);

            userInfo.add(userAvatar, nameLayout);

            UnorderedList list = new UnorderedList(
                    createListItem("Change password", LineAwesomeIcon.KEY_SOLID, ChangePasswordView.class),
                    createListItem("Sign out", LineAwesomeIcon.SIGN_OUT_ALT_SOLID, LogoutView.class)
            );
            list.addClassNames(LumoUtility.ListStyleType.NONE, LumoUtility.Margin.Vertical.NONE, LumoUtility.Padding.XSMALL, LumoUtility.Gap.MEDIUM);


            Hr hr = new Hr();
            hr.addClassNames(LumoUtility.Margin.Vertical.XSMALL);


            popover.add(userInfo, hr, list);

            layout.add(button, popover);

        } else {
            Anchor loginLink = new Anchor("login", "Sign in");

            layout.add(loginLink);
        }

        return layout;
    }

    private Footer createFooter() {
        Anchor link = new Anchor("https://alphnology.com/", "");
        link.setTarget("_blank");

        Span developed = new Span("dev and maintained by");
        developed.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.AlignSelf.CENTER);

        Image img = new Image("images/developed.png", "Alphnology");
        img.addClassNames(LumoUtility.Padding.SMALL);
        img.getStyle().setWidth("200px");

        Div content = new Div(developed, img);
        content.addClassNames(LumoUtility.AlignSelf.CENTER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

        link.add(content);

        Div version = new Div(new Text("Version " + appVersion));
        version.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.TextColor.TERTIARY,
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.AlignSelf.CENTER
        );

        Footer layout = new Footer();
        layout.add(version);
        layout.add(link);
        layout.setWidthFull();
        layout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

        return layout;

    }

    private ListItem createListItem(String text, LineAwesomeIcon icon, Class<? extends Component> navigationTarget) {
        Div item = new Div(icon.create(), new Text(text));
        item.addClassNames(LumoUtility.BorderRadius.MEDIUM, LumoUtility.LineHeight.XSMALL, LumoUtility.Padding.SMALL);
        item.addClassNames("hover:bg-contrast-10", "transition-colors");

        RouterLink link = new RouterLink(navigationTarget);
        link.addClassNames(LumoUtility.TextColor.BODY, "no-underline");
        link.add(item);

        link.getElement().addEventListener("click", domEvent -> {
            if (navigationTarget != null && navigationTarget.equals(LogoutView.class)) {
                UI.getCurrent().navigate(LogoutView.class);
            }
        });

        return new ListItem(link);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (LogoutView.class.equals(event.getNavigationTarget())) {
            return;
        }

        User user = VaadinSession.getCurrent().getAttribute(User.class);

        if (user != null && user.isOneLogPwd()) {
            event.forwardTo("change-password");
        }
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        initDrawerCollapseState();
    }

    private void toggleDrawerCollapse() {
        drawerCollapsed = !drawerCollapsed;
        getElement().executeJs(
                "this.classList.toggle('drawer-collapsed', $0);"
                        + "localStorage.setItem('drawer-collapsed', String($0));"
                        + "this.querySelectorAll('vaadin-side-nav-item').forEach(function(item) {"
                        + "  if ($0) { item.title = item.textContent.trim(); }"
                        + "  else { item.removeAttribute('title'); }"
                        + "});",
                drawerCollapsed);
        updateCollapseIcon();

        updateGroupPopovers(drawerCollapsed);
    }

    private void updateCollapseIcon() {
        Icon icon = drawerCollapsed ? VaadinIcon.ANGLE_DOUBLE_RIGHT.create() : VaadinIcon.ANGLE_DOUBLE_LEFT.create();
        collapseToggle.setIcon(icon);
        collapseToggle.setAriaLabel(drawerCollapsed ? "Expand sidebar" : "Collapse sidebar");
    }

    private void initDrawerCollapseState() {
        getElement()
                .executeJs(
                        "var collapsed = localStorage.getItem('drawer-collapsed') === 'true';"
                                + "if (collapsed) {"
                                + "  this.classList.add('drawer-collapsed');"
                                + "  this.querySelectorAll('vaadin-side-nav-item').forEach(function(item) {"
                                + "    item.title = item.textContent.trim();"
                                + "  });"
                                + "} else {"
                                + "  this.classList.remove('drawer-collapsed');"
                                + "}"
                                + "return collapsed;")
                .then(Boolean.class, collapsed -> {
                    drawerCollapsed = Boolean.TRUE.equals(collapsed);
                    updateCollapseIcon();
                    updateGroupPopovers(drawerCollapsed);
                });
    }

}
