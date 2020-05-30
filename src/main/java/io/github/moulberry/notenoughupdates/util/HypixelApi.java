package io.github.moulberry.notenoughupdates.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class HypixelApi {

    private Gson gson = new Gson();
    private ExecutorService es = Executors.newSingleThreadExecutor();

    //https://api.hypixel.net/skyblock/profiles?key=06b20aa2-ff04-4851-9687-669c8111526f&uuid=d0e05de7-6067-454d-beae-c6d19d886191
    public void getHypixelApiAsync(String apiKey, String method, HashMap<String, String> args, Consumer<JsonObject> consumer) {
        getHypixelApiAsync(generateApiUrl(apiKey, method, args), consumer);
    }

    public void getHypixelApiAsync(String urlS, Consumer<JsonObject> consumer) {
        es.submit(() -> {
            consumer.accept(getHypixelApiSync(urlS));
        });
    }

    public JsonObject getHypixelApiSync(String urlS) {
        URLConnection connection;
        try {
            URL url = new URL(urlS);
            connection = url.openConnection();
            connection.setConnectTimeout(3000);
        } catch(IOException e) {
            return null;
        }

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder builder = new StringBuilder();
            int codePoint;
            while((codePoint = reader.read()) != -1) {
                builder.append(((char)codePoint));
            }
            String response = builder.toString();

            JsonObject json = gson.fromJson(response, JsonObject.class);
            return json;
        } catch(IOException e) {
            return null;
        }
    }

    public String generateApiUrl(String apiKey, String method, HashMap<String, String> args) {
        String url = "https://api.hypixel.net/" + method + "?key=" + apiKey;
        for(Map.Entry<String, String> entry : args.entrySet()) {
            url += "&" + entry.getKey() + "=" + entry.getValue();
        }
        return url;
    }

}
