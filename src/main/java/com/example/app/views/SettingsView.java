package com.example.app.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "settings", layout = MainLayout.class)
@PermitAll
@Menu(title = "Einstellungen", order = 3, icon = "vaadin:cog")
public class SettingsView extends VerticalLayout {
    public SettingsView() {
        add(new H2("Einstellungen"), new Paragraph("Hier könnten allgemeine Einstellungen gepflegt werden."));
    }
}
