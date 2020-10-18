package io.github.moulberry.notenoughupdates.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

public class HypixelApi {
    private Gson gson = new Gson();
    private ExecutorService es = Executors.newFixedThreadPool(3);

    private int myApiErrors = 0;
    private String[] myApiURLs = {"https://moulberry.codes/", "http://51.75.78.252/", "http://moulberry.codes/" };

    public void getHypixelApiAsync(String apiKey, String method, HashMap<String, String> args, Consumer<JsonObject> consumer) {
        getHypixelApiAsync(apiKey, method, args, consumer, () -> {});
    }

    public void getHypixelApiAsync(String apiKey, String method, HashMap<String, String> args, Consumer<JsonObject> consumer, Runnable error) {
        getApiAsync(generateApiUrl(apiKey.trim(), method, args), consumer, error);
    }

    private String getMyApiURL() {
        return myApiURLs[myApiErrors%myApiURLs.length];
    }

    public void getApiAsync(String urlS, Consumer<JsonObject> consumer, Runnable error) {
        es.submit(() -> {
            try {
                consumer.accept(getApiSync(urlS));
            } catch(Exception e) {
                error.run();
            }
        });
    }

    public void getMyApiAsync(String urlS, Consumer<JsonObject> consumer, Runnable error) {
        es.submit(() -> {
            try {
                consumer.accept(getApiSync(getMyApiURL()+urlS));
            } catch(Exception e) {
                myApiErrors++;
                error.run();
            }
        });
    }

    public void getMyApiGZIPAsync(String urlS, Consumer<JsonObject> consumer, Runnable error) {
        es.submit(() -> {
            try {
                consumer.accept(getApiGZIPSync(getMyApiURL()+urlS));
            } catch(Exception e) {
                myApiErrors++;
                error.run();
            }
        });
    }

    public void getApiGZIPAsync(String urlS, Consumer<JsonObject> consumer, Runnable error) {
        es.submit(() -> {
            try {
                consumer.accept(getApiGZIPSync(urlS));
            } catch(Exception e) {
                error.run();
            }
        });
    }

    public JsonObject getApiSync(String urlS) throws IOException {
        URL url = new URL(urlS);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);

        JsonObject json = gson.fromJson(response, JsonObject.class);
        return json;
    }

    public JsonObject getApiGZIPSync(String urlS) throws IOException {
        URL url = new URL(urlS);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        String response = IOUtils.toString(new GZIPInputStream(connection.getInputStream()), StandardCharsets.UTF_8);

        JsonObject json = gson.fromJson(response, JsonObject.class);
        return json;
    }

    public String generateApiUrl(String apiKey, String method, HashMap<String, String> args) {
        StringBuilder url = new StringBuilder("https://api.hypixel.net/" + method + "?key=" + apiKey);
        for(Map.Entry<String, String> entry : args.entrySet()) {
            url.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return url.toString();
    }

}
