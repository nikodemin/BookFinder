package classes;
import javafx.concurrent.Service;
import javafx.util.Callback;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Parser
{
    private Document doc;
    private String searchUrl;
    private String pathToBlock;
    private String relPathToName;
    private String relPathToImg;
    private String relPathToPrice;
    private String relPathToAuthor;
    private String relPathToBook;   //path to "a" with href
    private String pathtoISBN;
    private String delimiter;

    public static Map<String,Parser> parsers = new HashMap<>();;

    Parser(String url, String blockPath, String nameRelPath, String imgRelPath,
           String priceRelPath, String authorRelPath,String bookRelPath,
           String pathtoISBN, String delimiter)
    {
        searchUrl = url;
        pathToBlock = blockPath;
        relPathToName = nameRelPath;
        relPathToImg = imgRelPath;
        relPathToPrice = priceRelPath;
        relPathToAuthor = authorRelPath;
        relPathToBook = bookRelPath;
        this.delimiter = delimiter;
        this.pathtoISBN = pathtoISBN;
    }

    public String getISBN(String site)
    {
        String reqBody = site +"|||"+ pathtoISBN;
        try
        {
            URL url = new URL("https://booksfinderserver.herokuapp.com/getISBN");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            conn.setRequestProperty("Content-type", "text/html");
            OutputStream os = conn.getOutputStream();
            os.write(reqBody.getBytes("UTF-8"));

            conn.connect();
            int responseCode= conn.getResponseCode();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            if (responseCode == 200)
            {
                InputStream is = conn.getInputStream();

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1)
                    baos.write(buffer, 0, bytesRead);

                return new String(baos.toByteArray(), "UTF-8");
            }
            else
            {
                throw new Exception("ISBN not found! Response code:"+responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
            return null;
    }

    public static String parseNums(String str)
    {
        int[] chars =
                str.chars().filter((int c)->{
                    if (c>=(int)'0' && c<=(int)'9')
                        return true;
                    return false;
                }).limit(13).toArray();

        str = "";
        for (int c:chars)
            str+=(char)c;
        return str;
    }

    public Elements search(String bookName, String altBookName)
    {
        try
        {
            String[] split = bookName.split(" ");
            bookName = String.join(delimiter, split);

            if (bookName == null)
                return null;

            try
            {
                doc = Jsoup.connect(searchUrl + bookName)
                        .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                        .referrer("http://www.google.com").get();
            } catch (HttpStatusException e)
            {
                if (altBookName == null)
                {
                    System.out.println("Book " + bookName + " not found at: " + searchUrl);
                    return null;
                }
                try
                {
                    doc = Jsoup.connect(searchUrl + altBookName)
                            .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                            .referrer("http://www.google.com").get();
                } catch (HttpStatusException e2)
                {
                    System.out.println("Book " + bookName + " not found at: " + searchUrl);
                    return null;
                }
            }

            return doc.select(pathToBlock);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public List<Book> parseBooks(Elements blocks, int begin)
    {
        List<Book> books = new ArrayList<>();
        if (blocks == null)
            return books;

        blocks.stream().skip(begin*5).limit(5).forEach(b->{
            String str;
            Book book = new Book();

            Element elem = b.selectFirst(relPathToImg);
            if(elem != null && !elem.attr("src").equals("https://img.labirint.ru/design/emptycover.svg"))
            {
                str = elem.attr("src");
                if (!str.substring(0,4).equals("http"))
                    str = "https://"+getParserKey(this) + str;
            }
            else
                str = "file:src/resources/empty.jpg";
            book.img = str;

            elem = b.selectFirst(relPathToName);
            if(elem != null)
                str = elem.text();
            else
                str = "";
            book.name = str;

            elem = b.selectFirst(relPathToPrice);
            if(elem != null)
                book.price = Integer.parseInt(Parser.parseNums(elem.text()));
            else
                book.price = 0;

            elem = b.selectFirst(relPathToAuthor);
            if(elem != null)
                str = elem.text();
            else
                str = "";
            book.author = str;

            elem = b.selectFirst(relPathToBook);
            if(elem != null)
            {
                str = elem.attr("href");
                if (!str.substring(0,4).equals("http"))
                    str = "https://"+getParserKey(this) + str;
            }
            else
                str = "";
            book.ref = str;

            book.ISBN = getISBN(str);

            books.add(book);
            System.out.println(book);
            });
        return books;
    }

    static public String getParserKey(Parser parser)
    {
        Set<Map.Entry<String,Parser>> entrySet=parsers.entrySet();

        for (Map.Entry<String,Parser> pair : entrySet)
            if (parser.equals(pair.getValue()))
            {
                return pair.getKey();
            }
        return "";
    }

}
