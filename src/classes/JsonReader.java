package classes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class JsonReader
{
   private static final String settingsSite = "http://localhost:8080/getSettings";

   public static JsonArray getParsers()
   {
       JsonElement root = null;
       try
       {
           URL url = new URL(settingsSite);
           HttpURLConnection request = (HttpURLConnection) url.openConnection();
           request.connect();

           JsonParser jp = new JsonParser();
           root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
       }
       catch (Exception e)
       {
           e.printStackTrace();
       }
       return root.getAsJsonArray();
   }
}
