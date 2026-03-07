package com.example.app.views;

import com.example.app.views.admin.AdminUserView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class MainLayout extends AppLayout {

    private final AuthenticationContext authenticationContext;

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle drawerToggle = new DrawerToggle();

        H2 title = new H2("Meine Vaadin App");
        title.getStyle().set("margin", "0");

        String username = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .map(UserDetails::getUsername)
                .orElse("-");

        Span userLabel = new Span("Angemeldet: " + username);

        Button logout = new Button(
                "Logout",
                VaadinIcon.SIGN_OUT.create(),
                event -> authenticationContext.logout()
        );

        HorizontalLayout header = new HorizontalLayout(drawerToggle, title, userLabel, logout);
        header.setWidthFull();
        header.expand(title);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setPadding(true);

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav sideNav = new SideNav();
        sideNav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        sideNav.addItem(new SideNavItem("Berichte", ReportsView.class, VaadinIcon.CHART.create()));
        sideNav.addItem(new SideNavItem("Einstellungen", SettingsView.class, VaadinIcon.COG.create()));

        if (isAdmin()) {
            sideNav.addItem(new SideNavItem("Benutzerverwaltung", AdminUserView.class, VaadinIcon.USERS.create()));
        }

        VerticalLayout drawer = new VerticalLayout(sideNav);
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);

        addToDrawer(drawer);
    }

    private boolean isAdmin() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .map(user -> user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_ADMIN"::equals))
                .orElse(false);
    }
}