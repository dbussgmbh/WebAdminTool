package com.example.app.views.list;

import com.example.app.data.entity.fvm_monitoring;
import com.example.app.utils.myCallback;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
/*import com.wontlost.ckeditor.Constants;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;*/

public class MonitoringForm extends FormLayout {


    TextField Titel = new TextField ("Titel");
    TextArea SQL = new TextArea ("SQL-Abfrage");
    TextArea SQL_Detail = new TextArea ("SQL-Detail Abfrage");
   // Checkbox is_active = new Checkbox("aktiv");
    TextField is_active = new TextField("aktiv");
    TextField Beschreibung = new TextField("Beschreibung");
    TextField Handlungs_INFO = new TextField("Handlungsinformation");

 //   RichTextEditor rtf_Beschreibung = new RichTextEditor("Beschreibung");


    RichTextEditor rtf_Handlungs_INFO = new RichTextEditor();


    IntegerField Check_Intervall = new IntegerField("Check-Intervall");

    IntegerField Warning_Schwellwert = new IntegerField("Warning Schwellwert");
    IntegerField Error_Schwellwert = new IntegerField("Error Schwellwert");

    Binder<fvm_monitoring> binder = new BeanValidationBinder<>(fvm_monitoring.class);
    myCallback callback;
    private fvm_monitoring mon;
    Button save = new Button("save", event ->{
        if (binder.validate().isOk()){
         //   System.out.println("callback Save aufrufen");
            try {
                binder.writeBean(mon);
            } catch (ValidationException e) {
                throw new RuntimeException(e);
            }
            callback.save(mon);


        }
    });
    Button delete = new Button("delete", event ->{
        if (binder.validate().isOk()){
            callback.delete(mon);


        }
    });
    Button cancel = new Button("cancel", event ->{
        if (binder.validate().isOk()){
            callback.cancel();


        }
    });





    // myCallback callback;

   // RichTextEditor rte;
 /*   static Tab details = new Tab("Beschreibung");
    static Tab payment= new Tab("Handlungsanweisung");*/



    //fvm_monitoring monitor;

  //  static VerticalLayout content = new VerticalLayout();;
    //public MonitoringForm(fvm_monitoring monitor, myCallback callback) {
    public MonitoringForm(myCallback callback) {
        addClassName("monitoring-form");
        this.callback=callback;

        binder.bindInstanceFields(this);
    //    VerticalLayout dialogInhalt;
        //this.callback=callback;
        //this.monitor=monitor;


    //    VerticalLayout editorInhalt = new VerticalLayout();



      //  Tabs tabs = new Tabs();

      //  tabs.addSelectedChangeListener(
      //          event -> setContent(event.getSelectedTab(),monitor));


        //tabs.add(details, payment);


        //content.setSpacing(false);
        //content.add(tabs);

        //setContent(tabs.getSelectedTab(),monitor);

      //  dialogInhalt = new VerticalLayout();
        //dialogInhalt.add(tabs,content);



        Span descriptionLb = new Span("Beschreibung");
        Span directiveLb = new Span("Handlungsanweisung");

        add(
                Titel,
                SQL,
                SQL_Detail,
                Check_Intervall,
                Warning_Schwellwert,
               Error_Schwellwert,
                is_active,
                Beschreibung,
                //descriptionLb,
                //rtf_Beschreibung,
                //directiveLb,
                //rtf_Handlungs_INFO,
                Handlungs_INFO,
                createButtonLayout()
        );

        setColspan(Titel, 2);
        setColspan(SQL, 2);
        setColspan(SQL_Detail, 2);
        //setColspan(rtf_Beschreibung, 2);


        setColspan(rtf_Handlungs_INFO, 2);
        setColspan(Beschreibung, 2);
        setColspan(Handlungs_INFO, 2);

    }

    public void setContact(fvm_monitoring mon) {
        this.mon = mon;



        binder.readBean(mon);
        //rtf_Beschreibung.setValue(Beschreibung.getValue());
      //  classicEditor.setValue(Beschreibung.getValue());
        rtf_Handlungs_INFO.setValue(Handlungs_INFO.getValue());

    }


  /*  private static void setContent(Tab tab,fvm_monitoring inhalt ) {

        if(content != null ) {
            content.removeAll();
        }

        if (tab == null) {
            return;
        }

        rte_Handlungsanweisung.setWidthFull();
        rte_Handlungsanweisung.setHeightFull();
        rte_Beschreibung.setWidthFull();
        rte_Beschreibung.setHeightFull();

        if (tab.equals(details)) {

            if (inhalt.getBeschreibung() != null) {
                rte_Beschreibung.asHtml().setValue(inhalt.getBeschreibung());
            }
            else
            {
                rte_Beschreibung.setValue("noch keine Beschreibung vorhanden...");
            }
            content.add(rte_Beschreibung);

        } else if (tab.equals(payment)) {

            if (inhalt.getHandlungs_INFO() != null) {
                rte_Handlungsanweisung.asHtml().setValue(inhalt.getHandlungs_INFO());
            }
            else
            {
                rte_Handlungsanweisung.setValue("noch keine Handlungsanweisung vorhanden...");
            }

            content.add(rte_Handlungsanweisung);
        } else  {
            if (inhalt.getSQL() != null) {
                sql_text.setValue(inhalt.getSQL());
            }
            else
            {
                sql_text.setValue("Noc kein SQL hinterlegt...");
            }
            content.add(sql_text);}

        //content.add(inhalt.getHandlungs_INFO());


    }*/


    private Component createButtonLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
   //     delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
      //  save.addClickListener(e ->saveMonitor());
        cancel.addClickShortcut(Key.ESCAPE);

        return new HorizontalLayout(save,cancel);
    }




}

//rte.setMaxHeight("400px");
//rte.setMinHeight("200px");
