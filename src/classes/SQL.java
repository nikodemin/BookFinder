package classes;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.util.Callback;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class SQL
{
    private static Connection conn=null;
    private String path = "src/resources/SQLdb.s3db";
    private PreparedStatement prepStatmt;

    SQL()
    {
        try
        {
            if (conn==null)
            {
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection("jdbc:sqlite:" + path);
            }
            System.out.println("SQL: База Подключена!");
        } catch (SQLException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    synchronized public void execute(String query)
    {
        try
        {
            Statement statmt;
            statmt = conn.createStatement();
            statmt.execute(query);
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    synchronized public ResultSet execQuery(String query)
    {
        try
        {
            Statement statmt;
            statmt = conn.createStatement();
            return statmt.executeQuery(query);

        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    synchronized public void prepareStatement(String query)
    {
        try
        {
            prepStatmt = this.conn.prepareStatement(query);
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    synchronized public void execStatement(String[] params)
    {
        try
        {
            for (int i = 0; i < params.length; i++)
            {
                prepStatmt.setObject(i + 1, params[i]);
            }
            prepStatmt.execute();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    synchronized public static void updatePrices() throws SQLException
    {
        System.out.println("Updating prices...");
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
                        SQL sql = new SQL();
                        ResultSet resSet = sql.execQuery("SELECT ISBN,Name,Authors FROM BooksTrack");
                        sql.prepareStatement("INSERT OR IGNORE INTO PriceStat(ISBN, store, date, price, ref)" +
                                " VALUES(?,?,?,?,?)");
                        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy");

                        while(resSet.next())
                        {
                            String isbn = resSet.getString("ISBN");
                            String altBookName = resSet.getString("Name")+" "+resSet.getString("Authors");
                            System.out.println(isbn);
                            System.out.println(altBookName);
                            for (Parser p : Parser.parsers.values())
                            {
                                Elements blocks = p.search(isbn,altBookName);
                                List<Book> books = p.parseBooks(blocks,0);
                                if(books.size()>0)
                                {
                                    String[] params = {isbn, Parser.getParserKey(p), date.format(new Date()),
                                            String.valueOf(books.get(0).price), books.get(0).ref };
                                    sql.execStatement(params);
                                }
                            }
                        }
                        return null;
                    }
                };
            }
        };
        service.start();
    }

    synchronized public static void updateImgs() throws SQLException
    {
        System.out.println("Updating imafges refs...");
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
                        SQL sql = new SQL();
                        ResultSet imgSrcSet = sql.execQuery("SELECT Image FROM BooksTrack");
                        while (imgSrcSet.next())
                        {
                            String imageSrc = imgSrcSet.getString("Image");
                            try
                            {
                                if (imageSrc.equals("file:src/resources/empty.jpg"))
                                    throw new HttpStatusException("update", 0, "");

                                Document doc = Jsoup.connect(imageSrc).ignoreContentType(true)
                                        .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; " +
                                                "x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                                        .referrer("http://www.google.com").get();
                            } catch (HttpStatusException e)
                            {
                                //update image src
                                ResultSet urlsSet = sql.execQuery("SELECT DISTINCT Ref, p.ISBN FROM PriceStat as p JOIN " +
                                        "BooksTrack as b ON p.ISBN=b.ISBN WHERE Image='" + imageSrc + "'");

                                String imageNewSrc = "file:src/resources/empty.jpg";
                                String isbn = "";
                                while (urlsSet.next())
                                {
                                    try
                                    {
                                        String url = urlsSet.getString("Ref");
                                        isbn = urlsSet.getString("ISBN");

                                        Document doc = Jsoup.connect(url)
                                                .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64;" +
                                                        " x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                                                .referrer("http://www.google.com").get();
                                        if (url.startsWith("https://www.bookvoed.ru"))
                                        {
                                            Element image = doc.selectFirst("div.j.nw img.vf");
                                            if (image == null)
                                                throw new IOException("IMAGE NOT FOUND");
                                            imageNewSrc = "https://www.bookvoed.ru" + image.attr("src");
                                        }
                                        if (url.startsWith("https://www.spbdk.ru"))
                                        {
                                            Element image = doc.selectFirst("div.product-gallery " +
                                                    "div.product-gallery__content img");
                                            if (image == null)
                                                throw new IOException("IMAGE NOT FOUND");
                                            imageNewSrc = "https://www.spbdk.ru" + image.attr("src");
                                        }
                                        if (url.startsWith("https://www.labirint.ru"))
                                        {
                                            Element image = doc.selectFirst("#product-info img.book-img-cover");
                                            if (image == null)
                                                throw new IOException("IMAGE NOT FOUND");
                                            imageNewSrc = image.attr("src");
                                        }
                                        break;
                                    } catch (HttpStatusException e2)
                                    {
                                        e2.printStackTrace();
                                    } catch (IOException e3)
                                    {
                                        e3.printStackTrace();
                                    }
                                }
                                sql.execute("UPDATE BooksTrack SET Image = '" + imageNewSrc + "'" +
                                        " WHERE ISBN='" + isbn + "'");
                                System.out.println("Image new src = " + imageNewSrc);
                            } catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }
                };
            }
        };
        service.start();
    }

    synchronized public static void updateDB() throws SQLException
    {
        System.out.println("Deleteing old from DB...");
        SQL sql = new SQL();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        ResultSet resSet = sql.execQuery("SELECT * FROM PriceStat");
        Set<String> datesToDelete = new HashSet<>();

        while(resSet.next())
        {
            Calendar date = Calendar.getInstance();
            try
            {
                date.setTime(dateFormat.parse(resSet.getString("Date")));
            } catch (ParseException e)
            {
                e.printStackTrace();
            }

            Calendar threshold = Calendar.getInstance();
            threshold.setTime(new Date());
            threshold.add(Calendar.DATE,-11);

            if(threshold.after(date))
            {
                datesToDelete.add(dateFormat.format(date.getTime()));
            }
        }

        datesToDelete.forEach(d->{
            sql.execute("DELETE FROM PriceStat WHERE DATE='"+d+"'");
        });
        sql.execute("DELETE FROM PriceStat WHERE Price=0");
        SQL.updatePrices();
        SQL.updateImgs();
    }
}
