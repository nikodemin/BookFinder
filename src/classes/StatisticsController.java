package classes;

import java.net.URL;
import java.security.Policy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.sun.prism.shader.Solid_TextureYV12_AlphaTest_Loader;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class StatisticsController
{
    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;

    @FXML
    private ScrollPane booksView;
    @FXML
    private Canvas canvas;
    @FXML
    private ChoiceBox<String> viewSelect;
    @FXML
    private ScrollPane booksCompare;

    class VBoxContainer extends VBox
    {
        public VBoxContainer(Node... nodes)
        {
            super(nodes);
        }

        public String ISBN;
    }

    @FXML
    void initialize() throws SQLException
    {
        SQL sql = new SQL();
        ResultSet resSet = sql.execQuery("SELECT * FROM BooksTrack");
        ListView<VBoxContainer> list = new ListView<>();
        list.setMaxWidth(200);
        booksView.setContent(list);
        booksView.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        while(resSet.next())
        {
            String  isbn = resSet.getString("ISBN");
            String  name = resSet.getString("Name");
            String  authors = resSet.getString("Authors");
            String  image = resSet.getString("Image");

            ImageView imgView = new ImageView(new Image(image));
            imgView.setPreserveRatio(true);
            imgView.setFitWidth(70);
            Label nameLbl = new Label(name);
            nameLbl.setWrapText(true);
            Label authorsLbl = new Label(authors);
            nameLbl.setWrapText(true);

            VBoxContainer container = new VBoxContainer(
                    imgView,
                    nameLbl,
                    authorsLbl
                    );
            container.ISBN = isbn;
            container.setAlignment(Pos.TOP_CENTER);
            container.setMaxWidth(200);
            list.getItems().add(container);
        }

        viewSelect.getItems().addAll("График","Сравнение");
        viewSelect.setOnAction(e->{
            if(viewSelect.getValue().equals("График"))
            {
                canvas.setVisible(true);
                booksCompare.setVisible(false);
                if(list.getSelectionModel().getSelectedItem()!=null)
                {
                    try
                    {
                        paintCanvas(list.getSelectionModel().getSelectedItem().ISBN);
                    } catch (SQLException | ParseException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
            else
            {
                canvas.setVisible(false);
                booksCompare.setVisible(true);
                if(list.getSelectionModel().getSelectedItem()!=null)
                {
                    try
                    {
                        fillBooksCompare(sql, list.getSelectionModel().getSelectedItem().ISBN);
                    } catch (SQLException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        });
        viewSelect.getSelectionModel().select(0);

        list.getSelectionModel().selectedItemProperty().addListener((ChangeListener<VBoxContainer>)
        (changed, oldValue, newValue) -> {
            try
            {
                if(canvas.isVisible())
                    paintCanvas(newValue.ISBN);
                else
                    fillBooksCompare(sql,newValue.ISBN);
            }
            catch (SQLException|ParseException e)
            {
                e.printStackTrace();
            }
        });
    }

    private void fillBooksCompare(SQL sql, String ISBN) throws SQLException
    {
        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy");
        ResultSet res = sql.execQuery("SELECT price, store, name, authors, image, ref" +
                " FROM PriceStat JOIN BooksTrack ON BooksTrack.ISBN=PriceStat.ISBN WHERE Date='"+
                date.format(new Date())+"' AND BooksTrack.ISBN='"+ISBN+"'");
        String price;
        String store;
        String name;
        String authors;
        String image;

        HBox container = new HBox();
        container.setSpacing(20);
        while(res.next())
        {
            price = res.getString("price");
            store = res.getString("store");
            name = res.getString("name");
            authors = res.getString("authors");
            image = res.getString("image");
            final String ref = res.getString("ref");

            ImageView view = new ImageView(new Image(image));
            view.setPreserveRatio(true);
            view.setFitHeight(150);
            Label priceLbl = new Label(price);
            Label nameLbl = new Label(name);
            nameLbl.setWrapText(true);
            Label authorsLbl = new Label(authors);
            authorsLbl.setWrapText(true);
            priceLbl.setFont(new Font(20));
            Button button =  new Button("В магазин");
            button.setOnAction(e->{
                Application app = new Application()
                {
                    @Override
                    public void start(Stage stage) throws Exception
                    {}
                };
                app.getHostServices().showDocument(ref);
            });
            VBox item = new VBox(
                    view,
                    nameLbl,
                    authorsLbl,
                    priceLbl,
                    new Label(store),
                    button
            );
            item.setAlignment(Pos.CENTER);
            item.setMaxWidth(150);
            container.getChildren().add(item);
        }
        BorderPane mainContainer = new BorderPane();
        mainContainer.setCenter(container);
        mainContainer.setPadding(new Insets(70,10,70,10));
        booksCompare.setContent(mainContainer);
        booksCompare.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        booksCompare.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    private void paintCanvas(String ISBN) throws SQLException, ParseException
    {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0,0,350,370);
        gc.setStroke(Color.WHEAT);
        gc.setLineWidth(2);
        gc.strokeLine(10,350,340,350);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        for(int x = 340; x>=20; x-=33)
        {
            gc.strokeLine(x,345,x,355);
            gc.strokeText(calendar.get(Calendar.DATE)+"",x-5 , 365);
            calendar.add(Calendar.DATE,-1);
        }
        gc.strokeLine(10,350,10,10);

        SQL sql = new SQL();
        ResultSet res = sql.execQuery("SELECT Store,[Date],Price FROM PriceStat WHERE ISBN = '"+ISBN+ "'");

        List<ResElement> pricesList = new LinkedList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        while(res.next())
        {
            pricesList.add(new ResElement(res.getString("Store"),
                    dateFormat.parse(res.getString("Date")),
                    res.getInt("Price")));
        }

        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -10);
        Calendar calendar2 = Calendar.getInstance();
        pricesList = pricesList.stream().filter(e->{
            calendar2.setTime(e.date);
            if(calendar.before(calendar2) && e.price!=0)
                return true;
            return false;
        }).sorted((a,b)->{
            return a.date.after(b.date)? 1:-1;
        }).collect(Collectors.toList());

        int maxPrice=Integer.MIN_VALUE, minPrice = Integer.MAX_VALUE;
        for (ResElement e : pricesList)
        {
            if(e.price>maxPrice)
                maxPrice = e.price;
            if(e.price<minPrice)
                minPrice = e.price;
        }
        if(minPrice==maxPrice)
            maxPrice = minPrice+500;
        double step = (maxPrice-minPrice)/10.0;

        for(int y = 317; y>=20; y-=(317-20)/10)
        {
            gc.strokeLine(5,y,15,y);
            gc.strokeText(minPrice+"",20 , y);
            minPrice += step;
        }

        Set<String> stores = pricesList.stream().map(e->e.store).collect(Collectors.toSet());
        List<Color> colors = new LinkedList<>();
        colors.add(Color.RED);
        colors.add(Color.BLUE);
        colors.add(Color.BLACK);
        Random rand = new Random();
        int bounds = 3;
        int textY = 20;

        for (String store: stores)
        {
            List<ResElement> storePricesList = pricesList.stream().filter(e->e.store.equals(store))
                    .collect(Collectors.toList());

            Color color = colors.get(rand.nextInt(bounds));
            --bounds;
            colors.remove(color);
            gc.setFill(color);
            gc.setStroke(color);
            gc.setLineWidth(1);
            gc.strokeText(store, 60, textY);
            textY += 20;
            gc.setLineWidth(4);

            double prevX = 0, prevY = 0;
            for (int i = storePricesList.size() - 1; i >= 0; --i)
            {
                double diff = (int) (new Date().getTime() - storePricesList.get(i).date.getTime()) / (24 * 60 * 60 * 1000);
                double x = 10 - (diff);
                x = x * 33 + 10;
                double y = (maxPrice - storePricesList.get(i).price) / step;
                y = y * (317 - 20) / 10 + 27;

                gc.fillOval(x - 3, y - 3, 6, 6);
                if (i != storePricesList.size() - 1)
                {
                    gc.strokeLine(x, y, prevX, prevY);
                }
                prevX = x;
                prevY = y;
            }
        }
    }
}
