module com.example.semestrwithfxgradle {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires okhttp3;
    requires org.slf4j;
    requires java.prefs;

    requires kotlin.stdlib;
    requires kotlin.reflect;

    opens com.example.semestrwithfxgradle to com.google.gson, javafx.fxml;

    exports com.example.semestrwithfxgradle;
}