package com.example.app.views.admin;

import com.example.app.data.AppUser;
import com.example.app.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;

public class ChangePasswordDialog extends Dialog {

    public ChangePasswordDialog(AppUser user, UserService userService, Runnable onSaved) {
        setHeaderTitle("Passwort ändern: " + user.getUsername());
        setWidth("420px");

        PasswordField password = new PasswordField("Neues Passwort");
        PasswordField confirmation = new PasswordField("Wiederholen");

        Button save = new Button("Speichern", e -> {
            String pw1 = password.getValue();
            String pw2 = confirmation.getValue();
            if (pw1 == null || pw1.isBlank()) {
                Notification.show("Passwort ist erforderlich.", 3000, Position.MIDDLE);
                return;
            }
            if (!pw1.equals(pw2)) {
                Notification.show("Die Passwörter stimmen nicht überein.", 3000, Position.MIDDLE);
                return;
            }
            try {
                userService.changePassword(user.getId(), pw1);
                onSaved.run();
                close();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Position.MIDDLE);
            }
        });

        Button cancel = new Button("Abbrechen", e -> close());
        add(password, confirmation, new HorizontalLayout(save, cancel));
    }
}
