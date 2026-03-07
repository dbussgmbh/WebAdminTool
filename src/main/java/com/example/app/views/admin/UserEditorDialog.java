package com.example.app.views.admin;

import com.example.app.data.AppUser;
import com.example.app.data.Role;
import com.example.app.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Consumer;

public class UserEditorDialog extends Dialog {

    private final UserService userService;
    private final Consumer<AppUser> onSaved;
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Initiales Passwort");
    private final ComboBox<Role> role = new ComboBox<>("Rolle");
    private final Checkbox enabled = new Checkbox("Aktiv", true);
    private AppUser currentUser;

    public UserEditorDialog(UserService userService, Consumer<AppUser> onSaved) {
        this.userService = userService;
        this.onSaved = onSaved;
        setHeaderTitle("Benutzer");
        setWidth("480px");

        role.setItems(Role.values());
        role.setValue(Role.USER);

        FormLayout form = new FormLayout(username, password, role, enabled);
        Button save = new Button("Speichern", e -> save());
        Button cancel = new Button("Abbrechen", e -> close());
        add(form, new HorizontalLayout(save, cancel));
    }

    public void openForCreate() {
        currentUser = null;
        setHeaderTitle("Benutzer anlegen");
        username.clear();
        password.clear();
        password.setVisible(true);
        role.setValue(Role.USER);
        enabled.setValue(true);
        open();
    }

    public void openForEdit(AppUser user) {
        currentUser = user;
        setHeaderTitle("Benutzer bearbeiten");
        username.setValue(user.getUsername() == null ? "" : user.getUsername());
        password.clear();
        password.setVisible(false);
        role.setValue(user.getRole());
        enabled.setValue(Boolean.TRUE.equals(user.getEnabled()));
        open();
    }

    private void save() {
        try {
            AppUser saved = currentUser == null
                    ? userService.createUser(username.getValue(), password.getValue(), role.getValue(), enabled.getValue())
                    : userService.updateUser(currentUser.getId(), username.getValue(), role.getValue(), enabled.getValue());
            onSaved.accept(saved);
            close();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Position.MIDDLE);
        }
    }
}
