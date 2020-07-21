package io.github.moulberry.notenoughupdates.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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

public class HypixelApi {

    /**
     * Not currently used as of BETA-1.6.
     */

    private Gson gson = new Gson();
    private ExecutorService es = Executors.newCachedThreadPool();

    public void getHypixelApiAsync(String apiKey, String method, HashMap<String, String> args, Consumer<JsonObject> consumer) {
        getHypixelApiAsync(apiKey, method, args, consumer, () -> {});
    }

    public void getHypixelApiAsync(String apiKey, String method, HashMap<String, String> args, Consumer<JsonObject> consumer, Runnable error) {
        getHypixelApiAsync(generateApiUrl(apiKey.trim(), method, args), consumer, error);
    }

    public void getHypixelApiAsync(String urlS, Consumer<JsonObject> consumer, Runnable error) {
        es.submit(() -> {
            try {
                consumer.accept(getHypixelApiSync(urlS));
            } catch(IOException e) {
                error.run();
            }
        });
    }

    public JsonObject getHypixelApiSync(String urlS) throws IOException {

        URL url = new URL(urlS);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(3000);

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            int codePoint;
            while((codePoint = reader.read()) != -1) {
                builder.append(((char)codePoint));
            }
            String response = builder.toString();

            JsonObject json = gson.fromJson(response, JsonObject.class);
            return json;
        }
    }

    public String generateApiUrl(String apiKey, String method, HashMap<String, String> args) {
        StringBuilder url = new StringBuilder("https://api.hypixel.net/" + method + "?key=" + apiKey);
        for(Map.Entry<String, String> entry : args.entrySet()) {
            url.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return url.toString();
    }

}
