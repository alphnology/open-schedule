package com.alphnology.views;

import com.alphnology.data.User;
import com.alphnology.security.AuthenticatedUser;
import com.alphnology.utils.ImageUtils;
import com.alphnology.views.login.LogoutView;
import com.alphnology.views.rate.RatingEventBus;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.io.ByteArrayInputStream;
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
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    private H1 viewTitle;

    private AuthenticatedUser authenticatedUser;

    private final RatingEventBus ratingEventBus;
    private final String eventWebsite;
    private final String appVersion;
    private Registration ratingEventRegistration;

    public MainLayout(
            AuthenticatedUser authenticatedUser,
            RatingEventBus ratingEventBus,
            @Value("${event.website}") String eventWebsite,
            @Value("${application.version}") String appVersion
    ) {
        this.authenticatedUser = authenticatedUser;
        this.ratingEventBus = ratingEventBus;
        this.eventWebsite = eventWebsite;
        this.appVersion = appVersion;

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE, LumoUtility.Margin.End.AUTO);
        viewTitle.addClassNames(LumoUtility.Display.HIDDEN, LumoUtility.Display.Breakpoint.Large.BLOCK);

        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.addClassNames(LumoUtility.Height.XLARGE);

        layout.add(toggle);

        layout.add(viewTitle);

        layout.add(createUserInfo());

        addToNavbar(true, layout);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        ratingEventRegistration = ratingEventBus.subscribe(event -> {
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Unsubscribe to prevent memory leaks when the view is detached
        if (ratingEventRegistration != null) {
            ratingEventRegistration.remove();
            ratingEventRegistration = null;
        }
    }

    private void addDrawerContent() {
        Image image = ImageUtils.getMainImage();
        image.addClassNames(LumoUtility.AlignSelf.CENTER, LumoUtility.Margin.Top.SMALL);
        image.setWidth("100%");

        Header header = new Header(image);
        header.addClassNames(LumoUtility.JustifyContent.CENTER);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();
        // Map to keep track of parent items (groups) that have already been created.
        // The key will be the group name (e.g., "admin").
        Map<String, SideNavItem> parentItems = new HashMap<>();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();

        for (MenuEntry entry : menuEntries) {
            String path = entry.path();
            if (path == null || path.isEmpty()) {
                continue; // Skip entries without a path
            }

            // Clean up the path and split it into segments.
            // e.g., "admin/event" -> ["admin", "event"]
            String[] segments = path.replaceFirst("^/", "").split("/");

            if (segments.length == 1) {
                // If there's only one segment, it's a direct link in the menu.
                // This also handles the root path "/" (where segments[0] would be empty).
                nav.addItem(createSideNavItemFromEntry(entry));

            } else if (segments.length > 1) {
                String groupName = segments[0];

                // Check if the parent group has already been created. If not, create it.
                SideNavItem parentItem = parentItems.computeIfAbsent(groupName, key -> {
                    // This code runs only the first time we encounter a new group.
                    // Capitalize the first letter for a nice title.
                    String parentTitle = key.substring(0, 1).toUpperCase() + key.substring(1);
                    SideNavItem newParent = new SideNavItem(parentTitle);
                    // Set a folder icon to visually represent it as a container.
                    newParent.setPrefixComponent(new SvgIcon("line-awesome/svg/cog-solid.svg"));


                    // Add the new group to the main navigation.
                    nav.addItem(newParent);
                    return newParent;
                });

                // Create the child item and add it to its parent.
                parentItem.addItem(createSideNavItemFromEntry(entry));
            }
        }

        SideNavItem eventLink = new SideNavItem("Event website", eventWebsite, LineAwesomeIcon.GLOBE_SOLID.create());
        eventLink.setOpenInNewBrowserTab(true);
        eventLink.addClassName("external");
        nav.addItem(eventLink);

        SideNavItem bug = new SideNavItem("Report bug", "https://github.com/alphnology/open-schedule/issues", LineAwesomeIcon.BUG_SOLID.create());
        bug.setOpenInNewBrowserTab(true);
        bug.addClassName("external");
        nav.addItem(bug);

        SideNavItem contribute = new SideNavItem("Contribute", "https://github.com/alphnology/open-schedule", LineAwesomeIcon.GITHUB.create());
        contribute.setOpenInNewBrowserTab(true);
        contribute.addClassName("external");
        nav.addItem(contribute);

        return nav;
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
        return item;
    }

    private HorizontalLayout createUserInfo() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.addClassNames(LumoUtility.Margin.Right.LARGE);

        Optional<User> maybeUser = authenticatedUser.get();

        if (maybeUser.isPresent()) {
            User user = maybeUser.get();

            StreamResource resource = new StreamResource("profile-pic", () -> new ByteArrayInputStream(new byte[]{}));

            Avatar avatar = new Avatar(user.getName());
            avatar.setImageResource(resource);
            avatar.getStyle().set("display", "block");
            avatar.getStyle().set("cursor", "pointer");
            avatar.getElement().setAttribute("tabindex", "-1");

            Button button = new Button(avatar);
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE);
            button.addClassNames(LumoUtility.Width.AUTO, LumoUtility.BorderRadius.FULL);
            button.getStyle().set("margin-inline-start", "auto");

            Popover popover = new Popover();
            popover.setModal(true);
            popover.setOverlayRole("menu");
            popover.setAriaLabel("User menu");
            popover.setTarget(button);
            popover.setPosition(PopoverPosition.BOTTOM_END);
            popover.addThemeVariants(PopoverVariant.LUMO_NO_PADDING);

            HorizontalLayout userInfo = new HorizontalLayout();
            userInfo.addClassName("userMenuHeader");
            userInfo.addClassNames(LumoUtility.Gap.SMALL);

            Avatar userAvatar = new Avatar(user.getName());
            userAvatar.setImageResource(resource);
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
                    createListItem("Sign out", LineAwesomeIcon.SIGN_OUT_ALT_SOLID, LogoutView.class)
            );
            list.addClassNames(LumoUtility.ListStyleType.NONE, LumoUtility.Margin.Vertical.NONE, LumoUtility.Padding.XSMALL, LumoUtility.Gap.MEDIUM);


            Hr hr = new Hr();
            hr.addClassNames(LumoUtility.Margin.Vertical.XSMALL);


            popover.add(userInfo, list, hr);

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
    protected void afterNavigation() {
        super.afterNavigation();
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

        if (user != null) {
            if (user.isOneLogPwd()) {
                event.forwardTo("change-password");
            }
        }
    }

//    public record MenuEntry(
//            String path,
//            String title,
//            Double order,
//            String icon,
//            Class<? extends Component> menuClass,
//            String parent
//    ) implements Serializable {
//        /**
//         * Convenience constructor for mapping from Vaadin's MenuEntry.
//         *
//         * @param menuEntry Vaadin's original MenuEntry.
//         */
//        public MenuEntry(com.vaadin.flow.server.menu.MenuEntry menuEntry) {
//            this(
//                    menuEntry.path(),
//                    menuEntry.title(),
//                    menuEntry.order(),
//                    menuEntry.icon(),
//                    menuEntry.menuClass(),
//                    null
//            );
//        }
//    }
}
