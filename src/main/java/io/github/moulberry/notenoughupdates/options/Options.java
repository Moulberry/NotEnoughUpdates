package io.github.moulberry.notenoughupdates.options;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import io.github.moulberry.notenoughupdates.Utils;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Options {

    public Option<Boolean> enableItemEditing = new Option(
            false,
            "Enable Item Editing",
            true,
            "Dev Feature. Please don't use.");
    public Option<Boolean> onlyShowOnSkyblock = new Option(
            true,
            "Only Show On Skyblock",
            false,
            "GUI Overlay only appears when you are playing Skyblock.");
    public Option<Boolean> autoupdate = new Option(
            true,
            "Automatically Update Items",
            false,
            "If true, updated items will automatically download from the remote repository when you start the game. \nHIGHLY RECOMMENDED.");
    public Option<Boolean> keepopen = new Option(
            false,
            "Keep Itempane Open",
            false,
            "If true, the itempane will stay open after the gui is closed.");
    public Option<Double> paneWidthMult = new Option(
            1.0,
            "Pane Width Multiplier",
            false,
            "Changes how wide the item and info panes are. Value between 0.5-1.5.");
    public Option<Double> bgOpacity = new Option(
            50.0,
            "Pane Background Opacity",
            false,
            "Changes the background colour opacity of item and info panes. Value between 0-255.");
    public Option<Double> fgOpacity = new Option(
            255.0,
            "Item Background Opacity",
            false,
            "Changes the opacity of item background. Value between 0-255.");

    /**
     * OPTIONS THAT DON'T SHOW IN GUI
     */
    public Option<Boolean> dev = new Option(
            false,
            "Show Dev Options",
            true,
            "Dev Feature. Please don't use.");
    public Option<Double> compareMode = new Option(
            0.0,
            "Compare Mode",
            false,
            "Compare Mode");
    public Option<Double> sortMode = new Option(
            0.0,
            "Sort Mode",
            false,
            "Sort Mode");
    public Option<ArrayList<Boolean>> compareAscending = new Option(
            Utils.createList(true, true),
            "Compare Ascending",
            false,
            "Compare Ascending");
    public Option<ArrayList<String>> favourites = new Option(
            new ArrayList<String>(),
            "Favourites",
            false,
            "Favourites");

    public List<Option> getOptions() {
        List<Option> options = new ArrayList<>();

        tryAddOption(enableItemEditing, options);
        tryAddOption(onlyShowOnSkyblock, options);
        tryAddOption(autoupdate, options);
        tryAddOption(keepopen, options);
        tryAddOption(paneWidthMult, options);
        tryAddOption(bgOpacity, options);
        tryAddOption(fgOpacity, options);

        return options;
    }

    private void tryAddOption(Option<?> option, List<Option> list) {
        if(!option.secret || dev.value) {
            list.add(option);
        }
    }

    public static class Option<T> implements Serializable {
        public T value;
        public final transient T defaultValue;
        public final transient String displayName;
        public final transient boolean secret;
        public final transient String desc;

        public Option(T defaultValue, String displayName, boolean secret, String desc) {
            this.value = defaultValue;
            this.defaultValue = defaultValue;
            this.displayName = displayName;
            this.secret = secret;
            this.desc = desc;
        }

        public void setValue(String value) {
            if(this.value instanceof Boolean) {
                ((Option<Boolean>) this).value = Boolean.valueOf(value);
            } else if(this.value instanceof Double) {
                ((Option<Double>)this).value = Double.valueOf(value);
            } else if(this.value instanceof String) {
                ((Option<String>)this).value = value;
            }
        }
    }

    public static JsonSerializer<Option<?>> createSerializer() {
        return (src, typeOfSrc, context) -> {
            if(src.secret && src.defaultValue.equals(src.value)) {
                return null;
            }
            return context.serialize(src.value);
        };
    }

    public static JsonDeserializer<Option<?>> createDeserializer() {
        return (json, typeOfT, context) -> {
            try {
                return new Option(context.deserialize(json, Object.class), "unknown", false, "unknown");
            } catch(Exception e) {
                return null;
            }
        };
    }

    public static Options loadFromFile(Gson gson, File file) throws IOException {
        InputStream in = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        Options oLoad = gson.fromJson(reader, Options.class);
        Options oDefault = new Options();
        if(oLoad == null) return oDefault;

        for(Field f : Options.class.getDeclaredFields()) {
            try {
                ((Option)f.get(oDefault)).value = ((Option)f.get(oLoad)).value;
            } catch (Exception e) { }
        }
        return oDefault;
    }

    public void saveToFile(Gson gson, File file) throws IOException {
        file.createNewFile();

        try(BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(this));
        }
    }


}
