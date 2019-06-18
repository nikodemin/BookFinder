package classes;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.jsoup.select.Elements;

public class MainController
{
    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;

    @FXML
    private VBox listSites;
    @FXML
    private ScrollPane booksView;
    @FXML
    private TextField searchField;
    @FXML
    private Button searchBtn;
    @FXML
    private ComboBox<String> sortComboBox;

    private Map<String,Boolean> isSiteChecked = new HashMap<>();
    private List<Book> booksList =  new ArrayList<>();
    private List<Service<Void>> searchServices = new ArrayList<>();

    @FXML
    void ExitApp(ActionEvent event) {Platform.exit();}

    @FXML
    void showAboutPopup(ActionEvent event)
    {
        final Stage stage = new Stage();
        Parent root = null;
        try
        {
            root = FXMLLoader.load(getClass().getResource("../fxmlAndCss/about.fxml"));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        stage.setTitle("About");
        stage.setScene(new Scene(root, 350, 150));
        stage.setResizable(false);
        stage.show();
    }

    @FXML
    void showStatistics(ActionEvent event)
    {
        final Stage stage = new Stage();
        Parent root = null;
        try
        {
            root = FXMLLoader.load(getClass().getResource("../fxmlAndCss/statistics.fxml"));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        stage.setTitle("Statistics");
        stage.setScene(new Scene(root, 600, 400));
        stage.setResizable(false);
        stage.show();
    }

    @FXML
    void search(ActionEvent event)
    {
        SQL sql = new SQL();
        booksList.clear();
        for (Service<Void> s:searchServices)
        {
            s.cancel();
        }
        searchServices.clear();

        List<Parser> parsers = new ArrayList<>();
        Parser.parsers.forEach((k,v)->{
            if(isSiteChecked.get(k))
            {
                parsers.add(v);
            }
        });

        ImageView loading = new ImageView(new Image("resources/loading.gif"));
        loading.setPreserveRatio(true);
        loading.setFitWidth(400);
        booksView.setContent(loading);

        for (Parser p : parsers)
        {
            Service<Void> service = new Service<Void>()
            {
                @Override
                protected Task<Void> createTask()
                {
                    return new Task<Void>()
                    {
                        @Override
                        protected Void call() throws Exception
                        {
                            Elements blocks = p.search(searchField.getText(), null);
                            int i = 0;
                            while(!isCancelled())
                            {
                                List<Book> books = p.parseBooks(blocks,i++);
                                if(!isCancelled())
                                    addF(books);
                                if(books.size() == 0)
                                {
                                    addF(books);
                                    break;
                                }
                            }
                            return null;
                        }
                    };
                }
            };
            searchServices.add(service);
            service.start();
        }
    }

    synchronized private void addF(List<Book> books)
    {
        booksList.addAll(books);
        addBooks();
    }

    private void trackedBooks()
    {
        SQL sql = new SQL();
        for (Service<Void> s:searchServices)
        {
            s.cancel();
        }
        searchServices.clear();
        booksList.clear();

        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy");
        ResultSet resSet = sql.execQuery("SELECT DISTINCT BooksTrack.ISBN as ISBN, price, name, authors, image, ref" +
                " FROM PriceStat JOIN BooksTrack ON BooksTrack.ISBN=PriceStat.ISBN WHERE Date='" +
                date.format(new Date()) + "'");
        try
        {
            while (resSet.next())
            {
                Book book = new Book();
                book.ref = resSet.getString("ref");
                book.author = resSet.getString("authors");
                book.price = resSet.getInt("price");
                book.name = resSet.getString("name");
                book.img = resSet.getString("image");
                book.ISBN = resSet.getString("ISBN");
                booksList.add(book);
                System.out.println(book);
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        addBooks();
    }

    synchronized private void addBooks()
    {
        SQL sql = new SQL();
        if (booksList.size() == 0)
        {
            Label lbl = new Label("Не найдено");
            lbl.setPadding(new Insets(10, 100, 10, 150));
            Platform.runLater(() -> booksView.setContent(lbl));
        } else
        {
            sortBooks();
            VBox container = new VBox();
            booksList.forEach(b -> {
                ImageView view = new ImageView(new Image(b.img));
                view.getStyleClass().add("imgView");
                view.setFitHeight(120);
                view.setFitWidth(120);
                view.setPreserveRatio(true);
                HBox hBox = new HBox(view);
                hBox.getStyleClass().add("item");

                Label name = new Label(b.name);
                name.getStyleClass().add("nameLbl");
                Label author = new Label(b.author);
                author.getStyleClass().add("authorLbl");
                Label price = new Label(b.price + " руб");
                price.getStyleClass().add("priceLbl");
                Button details = new Button("Подробнее");
                details.setOnAction(e -> {
                    Application app = new Application()
                    {
                        @Override
                        public void start(Stage stage) throws Exception
                        {
                        }
                    };
                    app.getHostServices().showDocument(b.ref);
                });

                Button track = new Button("Отслеживать");
                try
                {
                    if (sql.execQuery("SELECT * FROM BooksTrack WHERE ISBN = '" + b.ISBN + "'").next())
                    {
                        track.getStyleClass().add("green");
                        track.setText("Не отслеживать");
                    } else
                    {
                        track.getStyleClass().removeAll("green");
                        track.setText("Отслеживать");
                    }
                } catch (SQLException e)
                {
                    e.printStackTrace();
                }
                track.setOnAction(e ->
                {
                    try
                    {
                        if (sql.execQuery("SELECT * FROM BooksTrack WHERE ISBN = '" + b.ISBN + "'").next())
                        {
                            sql.execute("DELETE FROM BooksTrack WHERE ISBN = '" + b.ISBN + "'");
                            //sql.execute("DELETE FROM PriceStat WHERE ISBN = "+b.ISBN);
                            track.getStyleClass().removeAll("green");
                            track.setText("Отслеживать");
                        } else
                        {
                            sql.execute("INSERT INTO BooksTrack(ISBN,Name,Authors,Image) VALUES('" + b.ISBN +
                                    "','" + b.name + "','" + b.author + "','" + b.img + "')");
                            SQL.updatePrices();
                            track.getStyleClass().add("green");
                            track.setText("Не отслеживать");
                        }
                    } catch (SQLException ex)
                    {
                        ex.printStackTrace();
                    }
                });

                HBox priceAndBtns = new HBox(price, details, track);
                priceAndBtns.getStyleClass().add("priceAndBtn");
                VBox vBox = new VBox(name, author, priceAndBtns);

                hBox.getChildren().add(vBox);
                container.getChildren().add(hBox);
            });
            Platform.runLater(() -> booksView.setContent(container));
        }
    }

    private void sortBooks()
    {
        Comparator<Book> comp =null;
        if(sortComboBox.getSelectionModel().getSelectedItem() != null)
            switch (sortComboBox.getSelectionModel().getSelectedItem())
            {
                case "По названию":
                    comp = (b1, b2) -> b1.name.compareTo(b2.name);
                    break;
                case "По автору":
                    comp = (b1, b2) -> b1.author.compareTo(b2.author);
                    break;
                case "По ISBN":
                    comp = (b1, b2) -> b1.ISBN.compareTo(b2.ISBN);
                    break;
                case "По Цене(сначало дешёвые)":
                    comp = (b1, b2) -> b1.price > b2.price?1:-1;
                    break;
                case "По Цене(сначало дорогие)":
                    comp = (b1, b2) -> b1.price > b2.price?-1:1;
                    break;
            }
        if(comp != null)
            booksList.sort(comp);
    }

    @FXML
    void initialize()
    {
        assert listSites != null : "fx:id=\"listSites\" was not injected: check your FXML file 'main.fxml'.";
        assert booksView != null : "fx:id=\"booksView\" was not injected: check your FXML file 'main.fxml'.";
        assert searchField != null : "fx:id=\"searchField\" was not injected: check your FXML file 'main.fxml'.";
        assert searchBtn != null : "fx:id=\"searchBtn\" was not injected: check your FXML file 'main.fxml'.";

        sortComboBox.getItems().addAll("По названию","По автору","По ISBN",
                "По Цене(сначало дешёвые)","По Цене(сначало дорогие)");
        sortComboBox.valueProperty().addListener((o, oldVal, newVal)->{
            addBooks();
        });

        try
        {
            SQL.updateDB();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        Set sitesNames = Parser.parsers.keySet();
        sitesNames.forEach(s -> {
            CheckBox check = new CheckBox();
            check.setSelected(true);
            check.setOnAction(e->isSiteChecked.replace((String)s,check.isSelected()));

            isSiteChecked.put((String)s,true);
            HBox container = new HBox(check,new Label((String) s));
            container.setSpacing(15);
            listSites.getChildren().add(container);
        });
        Button trackedBooks = new Button("Отслеживаемые");
        listSites.getChildren().add(trackedBooks);
        listSites.setSpacing(3);
        trackedBooks.setOnAction(e->{
            trackedBooks();
        });

        booksView.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        booksView.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }
}

