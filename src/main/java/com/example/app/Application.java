package com.example.app;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;

@StyleSheet(Lumo.STYLESHEET)
@SpringBootApplication
public class Application implements AppShellConfigurator {

    public static HashMap<Long, Integer> maxPoolsizeMap = new HashMap<>();


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}