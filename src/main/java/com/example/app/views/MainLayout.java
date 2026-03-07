package com.example.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@PermitAll
public class MainLayout extends AppLayout {

    private final AuthenticationContext authenticationContext;
    private final Button themeToggle = new Button();

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        createHeader();
        createDrawer();
        updateThemeButton();
    }

    private void createHeader() {
        DrawerToggle drawerToggle = new DrawerToggle();

        H2 title = new H2("Meine Vaadin App");
        title.getStyle().set("margin", "0");

        String username = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .map(UserDetails::getUsername)
                .orElse("-");

        Span userLabel = new Span("Angemeldet: " + username);

        themeToggle.addClickListener(event -> {
            UI ui = UI.getCurrent();
            if (ui.getElement().getThemeList().contains("dark")) {
                ui.getElement().getThemeList().remove("dark");
            } else {
                ui.getElement().getThemeList().add("dark");
            }
            updateThemeButton();
        });

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

        MenuConfiguration.getMenuEntries()
                .stream()
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

        if (menuEntry.icon() != null && !menuEntry.icon().isBlank()) {
            item.setPrefixComponent(new Icon(menuEntry.icon()));
        }

        return item;
    }

    private void updateThemeButton() {
        boolean dark = UI.getCurrent() != null
                && UI.getCurrent().getElement().getThemeList().contains("dark");

        themeToggle.setText(dark ? "Hell" : "Dunkel");
        themeToggle.setIcon(dark ? VaadinIcon.SUN_O.create() : VaadinIcon.MOON_O.create());
    }
}