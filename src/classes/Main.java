package classes;

import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.awt.*;
import java.net.URLEncoder;
import java.util.HashMap;


public class Main extends Application
{
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        JsonReader.getParsers().forEach(e->{
            JsonObject o = e.getAsJsonObject();
            Parser.parsers.put(o.get("store").getAsString(),new Parser(
                    o.get("searchUrl").getAsString(),
                    o.get("pathToBlock").getAsString(),
                    o.get("relPathToName").getAsString(),
                    o.get("relPathToImg").getAsString(),
                    o.get("relPathToPrice").getAsString(),
                    o.get("relPathToAuthor").getAsString(),
                    o.get("relPathToBook").getAsString(),
                    o.get("pathToISBN").getAsString(),
                    o.get("delimiter").getAsString()));
        });

        Parent root = FXMLLoader.load(getClass().getResource("../fxmlAndCss/main.fxml"));
        primaryStage.setTitle("Library Viewer");
        double w=300,h=300;
        if (root instanceof Pane)
        {
            w = ((Pane) root).getPrefWidth();
            h = ((Pane) root).getPrefHeight();
        }
        primaryStage.setScene(new Scene(root, w, h));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
