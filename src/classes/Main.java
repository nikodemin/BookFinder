package classes;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.awt.*;
import java.util.HashMap;


public class Main extends Application
{
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Parser.parsers = new HashMap<>();
        Parser.parsers.put("www.bookvoed.ru",new Parser(
                "https://www.bookvoed.ru/books?q=",
                "#books",
                "div.kE div.IE",
                "Img.Yd",
                "div.lf",
                "div.if div.kE a",
                "a.Xd.ee",
                "table.tw tr.uw",
                "%20"
        ));
        Parser.parsers.put("www.spbdk.ru",new Parser(
                "https://www.spbdk.ru/search/?q=",
                "div.catalog div.snippet",
                "a.snippet__title span",
                "a.snippet__photo img",
                "div.snippet__price-value",
                "div.snippet__authors",
                "a.snippet__photo",
                "div.row div.row div.params div.params__item",
                "+"
        ));
        Parser.parsers.put("www.labirint.ru",new Parser(
                "https://www.labirint.ru/search/",
                "div.b-search-page-content div.product",
                "span.product-title",
                "a.cover img.book-img-cover",
                "span.price-val span",
                "div.product-author span",
                "a.cover",
                "div.isbn",
                "%20"
        ));

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
