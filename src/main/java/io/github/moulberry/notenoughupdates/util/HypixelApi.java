package io.github.moulberry.notenoughupdates.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

public class HypixelApi {
    private final Gson gson = new Gson();
    private final ExecutorService es = Executors.newFixedThreadPool(3);

    private static final int FAILS_BEFORE_SWITCH = 3;
    private int currentUrl = 0;
    private long lastPrimaryUrl = 0;
    private final String[] myApiURLs = {"https://moulberry.codes/"};//, "http://moulberry.codes/", "http://51.79.51.21/"};//, "http://51.75.78.252/" };
    private final Integer[] myApiSuccesses = {0, 0, 0, 0};

    public void getHypixelApiAsync(String apiKey, String method, HashMap<String, String> args, Consumer<JsonObject> consumer) {
        getHypixelApiAsync(apiKey, method, args, consumer, () -> {
        });
    }

    public void getHypixelApiAsync(String apiKey, String method, HashMap<String, String> args, Consumer<JsonObject> consumer, Runnable error) {
        getApiAsync(generateApiUrl(apiKey != null ? apiKey.trim() : null, method, args), consumer, error);
    }

    private String getMyApiURL() {
        if (currentUrl == 0) {
            lastPrimaryUrl = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastPrimaryUrl > 1000 * 60 * 30) { //Try switch back to main url after 30m
            currentUrl = 0;
        }

        myApiSuccesses[currentUrl] = Math.min(FAILS_BEFORE_SWITCH, myApiSuccesses[currentUrl] + 1);
        return myApiURLs[currentUrl];
    }

    private void myApiError(int index) {
        myApiSuccesses[index] = myApiSuccesses[index] - 2;

        if (myApiSuccesses[index] < 0) {
            myApiSuccesses[index] = 0;

            if (index == currentUrl) {
                currentUrl++;
                if (currentUrl >= myApiURLs.length) {
                    currentUrl = 0;
                }
            }
        }
    }

    public void getApiAsync(String urlS, Consumer<JsonObject> consumer, Runnable error) {
        es.submit(() -> {
            try {
                consumer.accept(getApiSync(urlS));
            } catch (Exception e) {
                error.run();
            }
        });
    }

    public void getMyApiAsync(String urlS, Consumer<JsonObject> consumer, Runnable error) {
        es.submit(() -> {
            int current = currentUrl;
            try {
                consumer.accept(getApiSync(getMyApiURL() + urlS));
            } catch (Exception e) {
                e.printStackTrace();
                myApiError(current);
                error.run();
            }
        });
    }

    public void getMyApiGZIPAsync(String urlS, Consumer<JsonObject> consumer, Runnable error) {
        es.submit(() -> {
            int current = currentUrl;
            try {
                consumer.accept(getApiGZIPSync(getMyApiURL() + urlS));
            } catch (Exception e) {
                myApiError(current);
                error.run();
            }
        });
    }

    public void getApiGZIPAsync(String urlS, Consumer<JsonObject> consumer, Runnable error) {
        es.submit(() -> {
            try {
                consumer.accept(getApiGZIPSync(urlS));
            } catch (Exception e) {
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
        if (json == null) throw new ConnectException("Invalid JSON");
        return json;
    }

    public JsonObject getApiGZIPSync(String urlS) throws IOException {
        URL url = new URL(urlS);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        String response = IOUtils.toString(new GZIPInputStream(connection.getInputStream()), StandardCharsets.UTF_8);

        return gson.fromJson(response, JsonObject.class);
    }

    public String generateApiUrl(String apiKey, String method, HashMap<String, String> args) {
        StringBuilder url = new StringBuilder("https://api.hypixel.net/" + method + (apiKey != null ? ("?key=" + apiKey.replace(" ", "")) : ""));
        boolean first = true;
        for (Map.Entry<String, String> entry : args.entrySet()) {
            if (first && apiKey == null) {
                url.append("?");
                first = false;
            } else {
                url.append("&");
            }
            url.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return url.toString();
    }

}
