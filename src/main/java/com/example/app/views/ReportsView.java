package com.example.app.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Berichte")
@Menu(title = "Berichte", order = 2, icon = "vaadin:chart")
@PermitAll
public class ReportsView extends VerticalLayout {
    public ReportsView() {
        add(new H2("Berichte"), new Paragraph("Hier könnten Reports oder KPIs angezeigt werden."));
    }
}
