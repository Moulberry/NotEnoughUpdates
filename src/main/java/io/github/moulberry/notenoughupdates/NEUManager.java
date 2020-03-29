package io.github.moulberry.notenoughupdates;

import com.google.gson.*;
import javafx.scene.control.Alert;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.Display;

import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NEUManager {

    public final NEUIO neuio;
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private TreeMap<String, JsonObject> itemMap = new TreeMap<>();

    private TreeMap<String, Set<String>> tagWordMap = new TreeMap<>();
    private TreeMap<String, HashMap<String, List<Integer>>> titleWordMap = new TreeMap<>();
    private TreeMap<String, HashMap<String, List<Integer>>> loreWordMap = new TreeMap<>();

    private File configLocation;
    private File itemsLocation;
    private File itemShaLocation;
    private JsonObject itemShaConfig;
    private File config;
    private JsonObject configJson;

    public NEUManager(NEUIO neuio, File configLocation) {
        this.configLocation = configLocation;
        this.neuio = neuio;

        this.config = new File(configLocation, "config.json");
        try {
            config.createNewFile();
            configJson = getJsonFromFile(config);
            if(configJson == null) configJson = new JsonObject();
        } catch(IOException e) { }

        this.itemsLocation = new File(configLocation, "items");
        itemsLocation.mkdir();

        this.itemShaLocation = new File(configLocation, "itemSha.json");
        try {
            itemShaLocation.createNewFile();
            itemShaConfig = getJsonFromFile(itemShaLocation);
            if(itemShaConfig == null) itemShaConfig = new JsonObject();
        } catch(IOException e) { }
    }

    /**
     * Parses a file in to a JsonObject.
     */
    public JsonObject getJsonFromFile(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        JsonObject json = gson.fromJson(reader, JsonObject.class);
        return json;
    }

    /**
     * Some convenience methods for working with the config. Should probably switch to using a more sophisticated
     * config system, but until we only use it for more than one value, this should be fine.
     */
    public void setAllowEditing(boolean allowEditing) {
        try {
            configJson.addProperty("allowEditing", allowEditing);
            writeJson(configJson, config);
        } catch(IOException e) {}
    }

    public boolean getAllowEditing() {
        try {
            return configJson.get("allowEditing").getAsBoolean();
        } catch(Exception e) {}
        return false;
    }

    /**
     * Called when the game is first loaded. Compares the local repository to the github repository and handles
     * the downloading of new/updated files. This then calls the "loadItem" method for every item in the local
     * repository.
     */
    public void loadItemInformation() {
        JOptionPane pane = new JOptionPane("Getting items to download from remote repository.");
        JDialog dialog = pane.createDialog("NotEnoughUpdates Remote Sync");
        dialog.setModal(false);
        dialog.setVisible(true);

        if(Display.isActive()) dialog.toFront();

        HashMap<String, String> oldShas = new HashMap<>();
        for(Map.Entry<String, JsonElement> entry : itemShaConfig.entrySet()) {
            if(new File(itemsLocation, entry.getKey()+".json").exists()) {
                oldShas.put(entry.getKey()+".json", entry.getValue().getAsString());
            }
        }
        Map<String, String> changedFiles = neuio.getChangedItems(oldShas);
        for(Map.Entry<String, String> changedFile : changedFiles.entrySet()) {
            itemShaConfig.addProperty(changedFile.getKey().substring(0, changedFile.getKey().length()-5),
                    changedFile.getValue());
        }
        try {
            writeJson(itemShaConfig, itemShaLocation);
        } catch(IOException e) {}

        if(Display.isActive()) dialog.toFront();

        if(changedFiles.size() <= 20) {
            Map<String, String> downloads = neuio.getItemsDownload(changedFiles.keySet());

            String startMessage = "NotEnoughUpdates: Syncing with remote repository (";
            int downloaded = 0;

            for(Map.Entry<String, String> entry : downloads.entrySet()) {
                pane.setMessage(startMessage + (++downloaded) + "/" + downloads.size() + ")\nCurrent: " + entry.getKey());
                dialog.pack();
                dialog.setVisible(true);
                if(Display.isActive()) dialog.toFront();

                File item = new File(itemsLocation, entry.getKey());
                try { item.createNewFile(); } catch(IOException e) { }
                try(BufferedInputStream inStream = new BufferedInputStream(new URL(entry.getValue()).openStream());
                    FileOutputStream fileOutputStream = new FileOutputStream(item)) {
                    byte dataBuffer[] = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inStream.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //TODO: Store hard-coded value somewhere else
            String dlUrl = "https://github.com/Moulberry/NotEnoughUpdates-REPO/archive/master.zip";

            pane.setMessage("Downloading NEU Master Archive. (DL# >20)");
            dialog.pack();
            dialog.setVisible(true);
            if(Display.isActive()) dialog.toFront();

            File itemsZip = new File(configLocation, "neu-items-master.zip");
            try { itemsZip.createNewFile(); } catch(IOException e) { }
            try(BufferedInputStream inStream = new BufferedInputStream(new URL(dlUrl).openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(itemsZip)) {
                byte dataBuffer[] = new byte[1024];
                int bytesRead;
                while ((bytesRead = inStream.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            pane.setMessage("Unzipping NEU Master Archive.");
            dialog.pack();
            dialog.setVisible(true);
            if(Display.isActive()) dialog.toFront();

            unzipIgnoreFirstFolder(itemsZip.getAbsolutePath(), configLocation.getAbsolutePath());
        }


        dialog.dispose();

        for(File f : itemsLocation.listFiles()) {
            loadItem(f.getName().substring(0, f.getName().length()-5));
        }
    }

    /**
     * Loads the item in to the itemMap and also stores various words associated with this item
     * in to titleWordMap and loreWordMap. These maps are used in the searching algorithm.
     * @param internalName
     */
    private void loadItem(String internalName) {
        try {
            JsonObject json = getJsonFromFile(new File(itemsLocation, internalName + ".json"));
            if(json == null) {
                return;
            }
            itemMap.put(internalName, json);

            if(json.has("displayname")) {
                int wordIndex=0;
                for(String str : json.get("displayname").getAsString().split(" ")) {
                    str = clean(str);
                    if(!titleWordMap.containsKey(str)) {
                        titleWordMap.put(str, new HashMap<>());
                    }
                    if(!titleWordMap.get(str).containsKey(internalName)) {
                        titleWordMap.get(str).put(internalName, new ArrayList<>());
                    }
                    titleWordMap.get(str).get(internalName).add(wordIndex);
                    wordIndex++;
                }
            }

            if(json.has("lore")) {
                int wordIndex=0;
                for(JsonElement element : json.get("lore").getAsJsonArray()) {
                    for(String str : element.getAsString().split(" ")) {
                        str = clean(str);
                        if(!loreWordMap.containsKey(str)) {
                            loreWordMap.put(str, new HashMap<>());
                        }
                        if(!loreWordMap.get(str).containsKey(internalName)) {
                            loreWordMap.get(str).put(internalName, new ArrayList<>());
                        }
                        loreWordMap.get(str).get(internalName).add(wordIndex);
                        wordIndex++;
                    }
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Searches a string for a query. This method is used to mimic the behaviour of the
     * more complex map-based search function. This method is used for the chest-item-search feature.
     */
    public boolean searchString(String toSearch, String query) {
        int lastMatch = -1;

        toSearch = clean(toSearch).toLowerCase();
        query = clean(query).toLowerCase();
        String[] splitToSeach = toSearch.split(" ");
        out:
        for(String s : query.split(" ")) {
            for(int i=0; i<splitToSeach.length; i++) {
                if(lastMatch == -1 || lastMatch == i-1) {
                    if (splitToSeach[i].startsWith(s)) {
                        lastMatch = i;
                        continue out;
                    }
                }
            }
            return false;
        }

        return true;
    }

    /**
     * Checks whether an itemstack matches a certain query, following the same rules implemented by the
     * more complex map-based search function.
     */
    public boolean doesStackMatchSearch(ItemStack stack, String query) {
        if(query.startsWith("title:")) {
            query = query.substring(6);
            return searchString(stack.getDisplayName(), query);
        } else if(query.startsWith("desc:")) {
            query = query.substring(5);
            String lore = "";
            NBTTagCompound tag = stack.getTagCompound();
            if(tag != null) {
                NBTTagCompound display = tag.getCompoundTag("display");
                if (display.hasKey("Lore", 9)) {
                    NBTTagList list = display.getTagList("Lore", 8);
                    for (int i = 0; i < list.tagCount(); i++) {
                        lore += list.getStringTagAt(i) + " ";
                    }
                }
            }
            return searchString(lore, query);
        } else if(query.startsWith("id:")) {
            query = query.substring(3);
            String internalName = getInternalNameForItem(stack);
            return query.equalsIgnoreCase(internalName);
        } else {
            boolean result = false;
            if(!query.trim().contains(" ")) {
                StringBuilder sb = new StringBuilder();
                for(char c : query.toCharArray()) {
                    sb.append(c).append(" ");
                }
                result = result || searchString(stack.getDisplayName(), sb.toString());
            }
            result = result || searchString(stack.getDisplayName(), query);

            String lore = "";
            NBTTagCompound tag = stack.getTagCompound();
            if(tag != null) {
                NBTTagCompound display = tag.getCompoundTag("display");
                if (display.hasKey("Lore", 9)) {
                    NBTTagList list = display.getTagList("Lore", 8);
                    for (int i = 0; i < list.tagCount(); i++) {
                        lore += list.getStringTagAt(i) + " ";
                    }
                }
            }

            result = result || searchString(lore, query);

            return result;
        }
    }

    /**
     * Returns the name of items which match a certain search query.
     */
    public Set<String> search(String query) {
        LinkedHashSet<String> results = new LinkedHashSet<>();
        if(query.startsWith("title:")) {
            query = query.substring(6);
            results.addAll(new TreeSet<>(search(query, titleWordMap)));
        } else if(query.startsWith("desc:")) {
            query = query.substring(5);
            results.addAll(new TreeSet<>(search(query, loreWordMap)));
        } else if(query.startsWith("id:")) {
            query = query.substring(3);
            results.addAll(new TreeSet<>(subMapWithKeysThatAreSuffixes(query.toUpperCase(), itemMap).keySet()));
        } else {
            if(!query.trim().contains(" ")) {
                StringBuilder sb = new StringBuilder();
                for(char c : query.toCharArray()) {
                    sb.append(c).append(" ");
                }
                results.addAll(new TreeSet<>(search(sb.toString(), titleWordMap)));
            }
            results.addAll(new TreeSet<>(search(query, titleWordMap)));
            results.addAll(new TreeSet<>(search(query, loreWordMap)));
        }
        return results;
    }

    /**
     * Splits a search query into an array of strings delimited by a space character. Then, matches the query to
     * the start of words in the various maps (title & lore). The small query does not need to match the whole entry
     * of the map, only the beginning. eg. "ench" and "encha" will both match "enchanted". All sub queries must
     * follow a word matching the previous sub query. eg. "ench po" will match "enchanted pork" but will not match
     * "pork enchanted".
     */
    private Set<String> search(String query, TreeMap<String, HashMap<String, List<Integer>>> wordMap) {
        HashMap<String, List<Integer>> matches = null;

        query = clean(query).toLowerCase();
        for(String queryWord : query.split(" ")) {
            HashMap<String, List<Integer>> matchesToKeep = new HashMap<>();
            for(HashMap<String, List<Integer>> wordMatches : subMapWithKeysThatAreSuffixes(queryWord, wordMap).values()) {
                if(wordMatches != null && !wordMatches.isEmpty()) {
                    if(matches == null) {
                        //Copy all wordMatches to titleMatches
                        for(String internalname : wordMatches.keySet()) {
                            if(!matchesToKeep.containsKey(internalname)) {
                                matchesToKeep.put(internalname, new ArrayList<>());
                            }
                            matchesToKeep.get(internalname).addAll(wordMatches.get(internalname));
                        }
                    } else {
                        for(String internalname : matches.keySet()) {
                            if(wordMatches.containsKey(internalname)) {
                                for(Integer newIndex : wordMatches.get(internalname)) {
                                    if(matches.get(internalname).contains(newIndex-1)) {
                                        if(!matchesToKeep.containsKey(internalname)) {
                                            matchesToKeep.put(internalname, new ArrayList<>());
                                        }
                                        matchesToKeep.get(internalname).add(newIndex);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(matchesToKeep.isEmpty()) return new HashSet<>();
            matches = matchesToKeep;
        }

        return matches.keySet();
    }

    /**
     * From https://stackoverflow.com/questions/10711494/get-values-in-treemap-whose-string-keys-start-with-a-pattern
     */
    public <T> Map<String, T> subMapWithKeysThatAreSuffixes(String prefix, NavigableMap<String, T> map) {
        if ("".equals(prefix)) return map;
        String lastKey = createLexicographicallyNextStringOfTheSameLenght(prefix);
        return map.subMap(prefix, true, lastKey, false);
    }

    String createLexicographicallyNextStringOfTheSameLenght(String input) {
        final int lastCharPosition = input.length()-1;
        String inputWithoutLastChar = input.substring(0, lastCharPosition);
        char lastChar = input.charAt(lastCharPosition) ;
        char incrementedLastChar = (char) (lastChar + 1);
        return inputWithoutLastChar+incrementedLastChar;
    }

    private String clean(String str) {
        return str.replaceAll("(\u00a7.)|[^0-9a-zA-Z ]", "").toLowerCase().trim();
    }

    /**
     * Takes an item stack and produces a JsonObject. This is used in the item editor.
     */
    public JsonObject getJsonForItem(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound() == null ? new NBTTagCompound() : stack.getTagCompound();

        //Item lore
        String[] lore = new String[0];
        if(tag.hasKey("display", 10)) {
            NBTTagCompound display = tag.getCompoundTag("display");

            if(display.hasKey("Lore", 9)) {
                NBTTagList list = display.getTagList("Lore", 8);
                lore = new String[list.tagCount()];
                for(int i=0; i<list.tagCount(); i++) {
                    lore[i] = list.getStringTagAt(i);
                }
            }
        }

        if(stack.getDisplayName().endsWith(" Recipes")) {
            stack.setStackDisplayName(stack.getDisplayName().substring(0, stack.getDisplayName().length()-8));
        }

        if(lore.length > 0 && (lore[lore.length-1].endsWith("Click to view recipes!") ||
                lore[lore.length-1].endsWith("Click to view recipe!"))) {
            String[] lore2 = new String[lore.length-2];
            System.arraycopy(lore, 0, lore2, 0, lore.length-2);
            lore = lore2;
        }

        JsonObject json = new JsonObject();
        json.addProperty("itemid", stack.getItem().getRegistryName());
        json.addProperty("displayname", stack.getDisplayName());
        json.addProperty("nbttag", tag.toString());
        json.addProperty("damage", stack.getItemDamage());

        JsonArray jsonlore = new JsonArray();
        for(String line : lore) {
            jsonlore.add(new JsonPrimitive(line));
        }
        json.add("lore", jsonlore);

        return json;
    }

    public String getInternalNameForItem(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        //Internal id
        if(tag != null && tag.hasKey("ExtraAttributes", 10)) {
            NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");

            if(ea.hasKey("id", 8)) {
                return ea.getString("id");
            }
        }
        return null;
    }

    //Currently unused in production.
    public void writeItemToFile(ItemStack stack) {
        String internalname = getInternalNameForItem(stack);

        if(internalname == null) {
            return;
        }

        JsonObject json = getJsonForItem(stack);
        json.addProperty("internalname", internalname);
        json.addProperty("clickcommand", "viewrecipe");
        json.addProperty("modver", NotEnoughUpdates.VERSION);

        if(!json.has("internalname")) {
            return;
        }

        try {
            writeJson(json, new File(itemsLocation, internalname+".json"));
        } catch (IOException e) {}

        loadItem(internalname);
    }

    public JsonObject createItemJson(String internalname, String itemid, String displayname, String[] lore, String[] info,
                                     String clickcommand, int damage, NBTTagCompound nbttag) {
        if(internalname == null || internalname.isEmpty()) {
            return null;
        }

        JsonObject json = new JsonObject();
        json.addProperty("internalname", internalname);
        json.addProperty("itemid", itemid);
        json.addProperty("displayname", displayname);
        json.addProperty("clickcommand", clickcommand);
        json.addProperty("damage", damage);
        json.addProperty("nbttag", nbttag.toString());
        json.addProperty("modver", NotEnoughUpdates.VERSION);

        if(info != null && info.length > 0) {
            JsonArray jsoninfo = new JsonArray();
            for (String line : info) {
                jsoninfo.add(new JsonPrimitive(line));
            }
            json.add("info", jsoninfo);
        }

        JsonArray jsonlore = new JsonArray();
        for(String line : lore) {
            System.out.println("Lore:"+line);
            jsonlore.add(new JsonPrimitive(line));
        }
        json.add("lore", jsonlore);

        return json;
    }

    public boolean writeItemJson(String internalname, String itemid, String displayname, String[] lore, String[] info,
                              String clickcommand, int damage, NBTTagCompound nbttag) {
        JsonObject json = createItemJson(internalname, itemid, displayname, lore, info, clickcommand, damage, nbttag);
        if(json == null) {
            return false;
        }

        try {
            writeJsonDefaultDir(json, internalname+".json");
        } catch(IOException e) {
            return false;
        }

        loadItem(internalname);
        return true;
    }

    public boolean uploadItemJson(String internalname, String itemid, String displayname, String[] lore, String[] info,
                                 String clickcommand, int damage, NBTTagCompound nbttag) {
        JsonObject json = createItemJson(internalname, itemid, displayname, lore, info, clickcommand, damage, nbttag);
        if(json == null) {
            return false;
        }

        String username = Minecraft.getMinecraft().thePlayer.getName();
        String newBranchName = UUID.randomUUID().toString().substring(0, 8) + "-" + internalname + "-" + username;
        String prTitle = internalname + "-" + username;
        String prBody = "Internal name: " + internalname + "\nSubmitted by: " + username;
        String file = "items/"+internalname+".json";
        if(!neuio.createNewRequest(newBranchName, prTitle, prBody, file, gson.toJson(json))) {
            return false;
        }

        try {
            writeJsonDefaultDir(json, internalname+".json");
        } catch(IOException e) {
            return false;
        }

        loadItem(internalname);
        return true;
    }

    private void writeJson(JsonObject json, File file) throws IOException {
        file.createNewFile();

        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(json));
        }
    }

    private void writeJsonDefaultDir(JsonObject json, String filename) throws IOException {
        File file = new File(itemsLocation, filename);
        writeJson(json, file);
    }

    public TreeMap<String, JsonObject> getItemInformation() {
        return itemMap;
    }

    /**
     * Stolen from https://www.journaldev.com/960/java-unzip-file-example
     */
    private static void unzipIgnoreFirstFolder(String zipFilePath, String destDir) {
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if(!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                if(!ze.isDirectory()) {
                    String fileName = ze.getName();
                    fileName = fileName.substring(fileName.split("/")[0].length()+1);
                    File newFile = new File(destDir + File.separator + fileName);
                    //create directories for sub directories in zip
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
