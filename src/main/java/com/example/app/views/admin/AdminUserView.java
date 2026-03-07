package com.example.app.views.admin;

import com.example.app.data.AppUser;
import com.example.app.service.UserService;
import com.example.app.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("Benutzerverwaltung")
@Menu(title = "Benutzerverwaltung", order = 100, icon = "vaadin:users")
@RolesAllowed("ADMIN")
public class AdminUserView extends VerticalLayout {

    private final UserService userService;
    private final Grid<AppUser> grid = new Grid<>(AppUser.class, false);
    private final TextField searchField = new TextField();

    public AdminUserView(UserService userService) {
        this.userService = userService;
        setSizeFull();
        add(new H2("Benutzerverwaltung"));

        searchField.setPlaceholder("Nach Username suchen");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> refreshGrid());
        searchField.setWidth("320px");

        grid.addColumn(AppUser::getId).setHeader("ID").setWidth("90px").setFlexGrow(0);
        grid.addColumn(AppUser::getUsername).setHeader("Username").setAutoWidth(true);
        grid.addColumn(user -> user.getRole().name()).setHeader("Rolle").setAutoWidth(true);
        grid.addColumn(user -> Boolean.TRUE.equals(user.getEnabled()) ? "Ja" : "Nein").setHeader("Aktiv").setAutoWidth(true);
        grid.addComponentColumn(user -> {
            Button edit = new Button("Bearbeiten", e -> {
                UserEditorDialog dialog = new UserEditorDialog(userService, saved -> { refreshGrid(); show("Benutzer aktualisiert."); });
                dialog.openForEdit(user);
            });
            Button pwd = new Button("Passwort", e -> {
                ChangePasswordDialog dialog = new ChangePasswordDialog(user, userService, () -> { refreshGrid(); show("Passwort geändert."); });
                dialog.open();
            });
            Button delete = new Button("Löschen", e -> {
                if ("admin".equalsIgnoreCase(user.getUsername())) { show("Der Standard-Admin wird hier nicht gelöscht."); return; }
                userService.deleteUser(user.getId());
                refreshGrid();
                show("Benutzer gelöscht.");
            });
            return new HorizontalLayout(edit, pwd, delete);
        }).setHeader("Aktionen").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        Button addUser = new Button("Benutzer anlegen", e -> {
            UserEditorDialog dialog = new UserEditorDialog(userService, saved -> { refreshGrid(); show("Benutzer gespeichert."); });
            dialog.openForCreate();
        });

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addUser);
        toolbar.setWidthFull();
        toolbar.expand(searchField);

        add(toolbar, grid);
        refreshGrid();
        expand(grid);
    }

    private void refreshGrid() {
        grid.setItems(userService.search(searchField.getValue()));
    }

    private void show(String message) {
        Notification.show(message, 3000, Position.BOTTOM_START);
    }
}
