package com.example.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@PermitAll
public class MainLayout extends AppLayout {

    private static final String THEME_KEY = "dashTheme";

    private final AuthenticationContext authenticationContext;
    private final Button themeToggle = new Button();

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        createHeader();
        createDrawer();
        loadStoredTheme();
    }

    private void createHeader() {
        DrawerToggle drawerToggle = new DrawerToggle();

        H2 title = new H2("Web-Admin Tool");
        title.getStyle().set("margin", "0");

        String username = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .map(UserDetails::getUsername)
                .orElse("-");

        Span userLabel = new Span("Angemeldet: " + username);

        themeToggle.addClickListener(event -> toggleTheme());
        updateThemeButton(false);

        Button logout = new Button(
                "Logout",
                VaadinIcon.SIGN_OUT.create(),
                event -> authenticationContext.logout()
        );

        HorizontalLayout header = new HorizontalLayout(
                drawerToggle,
                title,
                themeToggle,
                userLabel,
                logout
        );
        header.setWidthFull();
        header.expand(title);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setPadding(true);

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        MenuConfiguration.getMenuEntries().stream()
                .sorted((a, b) -> Double.compare(a.order(), b.order()))
                .map(this::createSideNavItem)
                .forEach(nav::addItem);

        VerticalLayout drawer = new VerticalLayout(nav);
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);

        addToDrawer(drawer);
    }

    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        SideNavItem item = new SideNavItem(menuEntry.title(), menuEntry.path());
        item.setMatchNested(true);
        return item;
    }

    private void loadStoredTheme() {
        WebStorage.getItem(THEME_KEY, value -> {
            boolean dark = "dark".equalsIgnoreCase(value);
            applyTheme(dark);
        });
    }

    private void toggleTheme() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }

        ui.getPage()
                .executeJs("return document.documentElement.getAttribute('theme') === 'dark';")
                .then(Boolean.class, dark -> {
                    boolean newDark = !dark;
                    applyTheme(newDark);
                    WebStorage.setItem(THEME_KEY, newDark ? "dark" : "light");
                });
    }

    private void applyTheme(boolean dark) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }

        ui.getPage().executeJs(
                """
                if ($0) {
                    document.documentElement.setAttribute('theme', 'dark');
                } else {
                    document.documentElement.removeAttribute('theme');
                }
                """,
                dark
        );

        updateThemeButton(dark);
    }

    private void updateThemeButton(boolean dark) {
        themeToggle.setText(dark ? "Hell" : "Dunkel");
        themeToggle.setIcon(dark ? VaadinIcon.SUN_O.create() : VaadinIcon.MOON_O.create());
    }
}