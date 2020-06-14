package io.github.moulberry.notenoughupdates;

import com.google.common.io.CharSource;
import com.google.gson.*;
import io.github.moulberry.notenoughupdates.options.Options;
import io.github.moulberry.notenoughupdates.util.HypixelApi;
import javafx.scene.control.Alert;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NEUManager {

    private final NotEnoughUpdates neu;
    public final NEUIO neuio;
    public final Gson gson;

    private TreeMap<String, JsonObject> itemMap = new TreeMap<>();

    private TreeMap<String, Set<String>> tagWordMap = new TreeMap<>();
    private TreeMap<String, HashMap<String, List<Integer>>> titleWordMap = new TreeMap<>();
    private TreeMap<String, HashMap<String, List<Integer>>> loreWordMap = new TreeMap<>();

    public final KeyBinding keybindGive = new KeyBinding("Add item to inventory (Creative-only)", Keyboard.KEY_L, "NotEnoughUpdates");
    public final KeyBinding keybindFavourite = new KeyBinding("Set item as favourite", Keyboard.KEY_F, "NotEnoughUpdates");
    public final KeyBinding keybindViewUsages = new KeyBinding("Show usages for item", Keyboard.KEY_U, "NotEnoughUpdates");
    public final KeyBinding keybindViewRecipe = new KeyBinding("Show recipe for item", Keyboard.KEY_R, "NotEnoughUpdates");
    public final KeyBinding keybindToggleDisplay = new KeyBinding("Toggle NEU overlay", 0, "NotEnoughUpdates");
    public final KeyBinding keybindClosePanes = new KeyBinding("Close NEU panes", 0, "NotEnoughUpdates");
    public final KeyBinding[] keybinds = new KeyBinding[]{keybindGive, keybindFavourite, keybindViewUsages, keybindViewRecipe, keybindToggleDisplay, keybindClosePanes};

    public String viewItemAttemptID = null;
    public long viewItemAttemptTime = 0;

    public String currentProfile = "";
    public final HypixelApi hypixelApi = new HypixelApi();

    private ResourceLocation wkZip = new ResourceLocation("notenoughupdates:wkhtmltox.zip");
    private Map<String, ItemStack> itemstackCache = new HashMap<>();

    private static final String AUCTIONS_PRICE_URL = "https://moulberry.github.io/files/auc_avg_jsons/average_3day.json.gz";
    private JsonObject auctionPricesJson = null;
    private long auctionLastUpdate = 0;

    private HashMap<String, CraftInfo> craftCost = new HashMap<>();

    private HashMap<String, Set<String>> usagesMap = new HashMap<>();

    public File configLocation;
    private File itemsLocation;
    private File itemShaLocation;
    private JsonObject itemShaConfig;
    private File configFile;
    public Options config;

    public NEUManager(NotEnoughUpdates neu, NEUIO neuio, File configLocation) {
        this.neu = neu;
        this.configLocation = configLocation;
        this.neuio = neuio;

        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(Options.Option.class, Options.createSerializer());
        gsonBuilder.registerTypeAdapter(Options.Option.class, Options.createDeserializer());
        gson = gsonBuilder.create();

        this.loadConfig();

        this.itemsLocation = new File(configLocation, "items");
        itemsLocation.mkdir();

        this.itemShaLocation = new File(configLocation, "itemSha.json");
        try {
            itemShaLocation.createNewFile();
            itemShaConfig = getJsonFromFile(itemShaLocation);
            if(itemShaConfig == null) itemShaConfig = new JsonObject();
        } catch(IOException e) { }

        File wkShell = new File(configLocation, "wkhtmltox/bin/wkhtmltoimage");
        if(!wkShell.exists()) {
            try {
                InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(wkZip).getInputStream();
                unzip(is, configLocation);
            } catch (IOException e) {
            }
        }

        //Unused code, used to automatically grab items from auctions. Leaving here in case I need it.
        /*try {
            for(int j=0; j<=89; j++) {
                JsonObject auctions0 = getJsonFromFile(new File(configLocation, "auctions/auctions"+j+".json"));

                JsonArray arr = auctions0.getAsJsonArray("auctions");
                for(int i=0; i<arr.size(); i++) {
                    JsonObject item0 = arr.get(i).getAsJsonObject();
                    try {
                        String item_bytes = item0.get("item_bytes").getAsString();

                        NBTTagCompound tag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(Base64.getDecoder().decode(item_bytes)));
                        tag = tag.getTagList("i", 10).getCompoundTagAt(0);
                        int id = tag.getShort("id");
                        int damage = tag.getShort("Damage");
                        tag = tag.getCompoundTag("tag");

                        String internalname = "";
                        if(tag != null && tag.hasKey("ExtraAttributes", 10)) {
                            NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");

                            if(ea.hasKey("id", 8)) {
                                internalname = ea.getString("id");
                            }
                        }

                        String[] lore = new String[0];
                        NBTTagCompound display = tag.getCompoundTag("display");

                        if(display.hasKey("Lore", 9)) {
                            NBTTagList list = display.getTagList("Lore", 8);
                            lore = new String[list.tagCount()];
                            for(int k=0; k<list.tagCount(); k++) {
                                lore[k] = list.getStringTagAt(k);
                            }
                        }

                        if(Item.itemRegistry.getObject(new ResourceLocation(internalname.toLowerCase())) != null) {
                            continue;
                        }

                        String itemid = Item.getItemById(id).getRegistryName();
                        String displayname = display.getString("Name");
                        String[] info = new String[0];
                        String clickcommand = "";

                        File file = new File(itemsLocation, internalname+".json");
                        if(!file.exists()) {
                            writeItemJson(internalname, itemid, displayname, lore, info, clickcommand, damage, tag);
                        } else {
                            JsonObject existing = getJsonFromFile(file);
                            String nbttag = existing.get("nbttag").toString();
                            if(nbttag.length() > tag.toString().length()) {
                                writeItemJson(internalname, itemid, displayname, lore, info, clickcommand, damage, tag);
                            }
                        }

                    } catch(Exception e) {
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        throw new RuntimeException();*/
    }

    public class CraftInfo {
        public boolean fromRecipe = false;
        public boolean vanillaItem = false;
        public float craftCost = -1;
    }

    /**
     * Recursively calculates the cost of crafting an item from raw materials.
     */
    public CraftInfo getCraftCost(String internalname) {
        if(craftCost.containsKey(internalname)) {
            return craftCost.get(internalname);
        } else {
            CraftInfo ci = new CraftInfo();

            //Removes trailing numbers and underscores, eg. LEAVES_2-3 -> LEAVES
            String vanillaName = internalname.split("-")[0];
            int sub = 0;
            for(int i=vanillaName.length()-1; i>1; i--) {
                char c = vanillaName.charAt(i);
                if((int)c >= 48 && (int)c <= 57) { //0-9
                    sub++;
                } else if(c == '_') {
                    sub++;
                    break;
                } else {
                    break;
                }
            }
            vanillaName = vanillaName.substring(0, vanillaName.length()-sub);
            if(Item.itemRegistry.getObject(new ResourceLocation(vanillaName)) != null) {
                ci.vanillaItem = true;
            }

            JsonObject auctionInfo = getItemAuctionInfo(internalname);
            JsonObject bazaarInfo = getBazaarInfo(internalname);

            if(bazaarInfo != null) {
                float bazaarInstantBuyPrice = bazaarInfo.get("curr_buy").getAsFloat();
                ci.craftCost = bazaarInstantBuyPrice;
            }
            if(auctionInfo != null && !ci.vanillaItem) { //Don't use auction prices for vanilla items cuz people like to transfer money, messing up the cost of vanilla items.
                float auctionPrice = auctionInfo.get("price").getAsFloat() / auctionInfo.get("count").getAsFloat();
                if(ci.craftCost < 0 || auctionPrice < ci.craftCost) {
                    ci.craftCost = auctionPrice;
                }
            }
            JsonObject item = getItemInformation().get(internalname);
            if(item != null && item.has("recipe")) {
                float craftPrice = 0;
                JsonObject recipe = item.get("recipe").getAsJsonObject();

                String[] x = {"1","2","3"};
                String[] y = {"A","B","C"};
                for(int i=0; i<9; i++) {
                    String name = y[i/3]+x[i%3];
                    String itemS = recipe.get(name).getAsString();
                    if(itemS.length() == 0) continue;

                    int count = 1;
                    if(itemS != null && itemS.split(":").length == 2) {
                        count = Integer.valueOf(itemS.split(":")[1]);
                        itemS = itemS.split(":")[0];
                    }
                    float compCost = getCraftCost(itemS).craftCost * count;
                    if(compCost < 0) {
                        if(!getCraftCost(itemS).vanillaItem) { //If it's a vanilla item without a cost attached to it, let compCost = 0.
                            craftCost.put(internalname, ci);
                            return ci;
                        }
                    } else {
                        craftPrice += compCost;
                    }
                }

                if(ci.craftCost < 0 || craftPrice < ci.craftCost) {
                    ci.craftCost = craftPrice;
                    ci.fromRecipe = true;
                }
            }
            craftCost.put(internalname, ci);
            return ci;
        }
    }

    public void saveConfig() throws IOException {
        config.saveToFile(gson, configFile);
    }

    public void loadConfig() {
        this.configFile = new File(configLocation, "config.json");
        try {
            configFile.createNewFile();
            config = Options.loadFromFile(gson, configFile);
        } catch(Exception e) {
            config = new Options();
        }
    }

    /**
     * Downloads and sets auctionPricesJson from the URL specified by AUCTIONS_PRICE_URL.
     */
    public void updatePrices() {
        if(System.currentTimeMillis() - auctionLastUpdate > 1000*60*120) { //2 hours
            craftCost.clear();
            System.out.println("UPDATING PRICE INFORMATION");
            auctionLastUpdate = System.currentTimeMillis();
            try(Reader inReader = new InputStreamReader(new GZIPInputStream(new URL(AUCTIONS_PRICE_URL).openStream()))) {
                auctionPricesJson = gson.fromJson(inReader, JsonObject.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public JsonObject getAuctionPricesJson() {
        return auctionPricesJson;
    }

    public JsonObject getItemAuctionInfo(String internalname) {
        JsonElement e = auctionPricesJson.get("prices").getAsJsonObject().get(internalname);
        if(e == null) {
            return null;
        }
        return e.getAsJsonObject();
    }

    public JsonObject getBazaarInfo(String internalname) {
        JsonElement e = auctionPricesJson.get("bazaar").getAsJsonObject().get(internalname);
        if(e == null) {
            return null;
        }
        return e.getAsJsonObject();
    }

    /**
     * Calculates the cost of enchants + other price modifiers such as pet xp, midas price, etc.
     */
    public float getCostOfEnchants(String internalname, NBTTagCompound tag) {
        float costOfEnchants = 0;
        if(true) return 0;

        JsonObject info = getItemAuctionInfo(internalname);
        if(info == null || !info.has("price")) {
            return 0;
        }
        if(!auctionPricesJson.has("ench_prices") || !auctionPricesJson.has("ench_maximums")) {
            return 0;
        }
        JsonObject ench_prices = auctionPricesJson.getAsJsonObject("ench_prices");
        JsonObject ench_maximums = auctionPricesJson.getAsJsonObject("ench_maximums");
        if(!ench_prices.has(internalname) || !ench_maximums.has(internalname)) {
            return 0;
        }
        JsonObject iid_variables = ench_prices.getAsJsonObject(internalname);
        float ench_maximum = ench_maximums.get(internalname).getAsFloat();

        int enchants = 0;
        float price = getItemAuctionInfo(internalname).get("price").getAsFloat();
        if(tag.hasKey("ExtraAttributes")) {
            NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");
            if(ea.hasKey("enchantments")) {

                NBTTagCompound enchs = ea.getCompoundTag("enchantments");
                for(String ench : enchs.getKeySet()) {
                    enchants++;
                    int level = enchs.getInteger(ench);

                    for(Map.Entry<String, JsonElement> entry : iid_variables.entrySet()) {
                        if(matchEnch(ench, level, entry.getKey())) {
                            costOfEnchants += entry.getValue().getAsJsonObject().get("A").getAsFloat()*price +
                                    entry.getValue().getAsJsonObject().get("B").getAsFloat();
                            break;
                        }
                    }
                }
            }
        }
        return costOfEnchants;
    }

    /**
     * Checks whether a certain enchant (ench name + lvl) matches an enchant id
     * eg. PROTECTION_GE6 will match -> ench_name = PROTECTION, lvl >= 6
     */
    private boolean matchEnch(String ench, int level, String id) {
        if(!id.contains(":")) {
            return false;
        }

        String idEnch = id.split(":")[0];
        String idLevel = id.split(":")[1];

        if(!ench.equalsIgnoreCase(idEnch)) {
            return false;
        }

        if(String.valueOf(level).equalsIgnoreCase(idLevel)) {
            return true;
        }

        if(idLevel.startsWith("LE")) {
            int idLevelI = Integer.valueOf(idLevel.substring(2));
            return level <= idLevelI;
        } else if(idLevel.startsWith("GE")) {
            int idLevelI = Integer.valueOf(idLevel.substring(2));
            return level >= idLevelI;
        }

        return false;
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
     * Called when the game is first loaded. Compares the local repository to the github repository and handles
     * the downloading of new/updated files. This then calls the "loadItem" method for every item in the local
     * repository.
     */
    public void loadItemInformation() {
        if(config.autoupdate.value) {
            JOptionPane pane = new JOptionPane("Getting items to download from remote repository.");
            JDialog dialog = pane.createDialog("NotEnoughUpdates Remote Sync");
            dialog.setModal(false);
            //dialog.setVisible(true);

            if (Display.isActive()) dialog.toFront();

            HashMap<String, String> oldShas = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : itemShaConfig.entrySet()) {
                if (new File(itemsLocation, entry.getKey() + ".json").exists()) {
                    oldShas.put(entry.getKey() + ".json", entry.getValue().getAsString());
                }
            }
            Map<String, String> changedFiles = neuio.getChangedItems(oldShas);

            if (changedFiles != null) {
                for (Map.Entry<String, String> changedFile : changedFiles.entrySet()) {
                    itemShaConfig.addProperty(changedFile.getKey().substring(0, changedFile.getKey().length() - 5),
                            changedFile.getValue());
                }
                try {
                    writeJson(itemShaConfig, itemShaLocation);
                } catch (IOException e) {
                }
            }

            if (Display.isActive()) dialog.toFront();

            if (changedFiles != null && changedFiles.size() <= 20) {
                Map<String, String> downloads = neuio.getItemsDownload(changedFiles.keySet());

                String startMessage = "NotEnoughUpdates: Syncing with remote repository (";
                int downloaded = 0;

                for (Map.Entry<String, String> entry : downloads.entrySet()) {
                    pane.setMessage(startMessage + (++downloaded) + "/" + downloads.size() + ")\nCurrent: " + entry.getKey());
                    dialog.pack();
                    dialog.setVisible(true);
                    if (Display.isActive()) dialog.toFront();

                    File item = new File(itemsLocation, entry.getKey());
                    try {
                        item.createNewFile();
                    } catch (IOException e) {
                    }
                    try (BufferedInputStream inStream = new BufferedInputStream(new URL(entry.getValue()).openStream());
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
                if (Display.isActive()) dialog.toFront();

                File itemsZip = new File(configLocation, "neu-items-master.zip");
                try {
                    itemsZip.createNewFile();
                } catch (IOException e) {
                }
                try (BufferedInputStream inStream = new BufferedInputStream(new URL(dlUrl).openStream());
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
                if (Display.isActive()) dialog.toFront();

                unzipIgnoreFirstFolder(itemsZip.getAbsolutePath(), configLocation.getAbsolutePath());
            }

            dialog.dispose();
        }

        for(File f : itemsLocation.listFiles()) {
            loadItem(f.getName().substring(0, f.getName().length()-5));
        }
    }

    /**
     * Loads the item in to the itemMap and also stores various words associated with this item
     * in to titleWordMap and loreWordMap. These maps are used in the searching algorithm.
     * @param internalName
     */
    public void loadItem(String internalName) {
        itemstackCache.remove(internalName);
        try {
            JsonObject json = getJsonFromFile(new File(itemsLocation, internalName + ".json"));
            if(json == null) {
                return;
            }
            itemMap.put(internalName, json);

            if(json.has("recipe")) {
                JsonObject recipe = json.get("recipe").getAsJsonObject();

                String[] x = {"1","2","3"};
                String[] y = {"A","B","C"};
                for(int i=0; i<9; i++) {
                    String name = y[i/3]+x[i%3];
                    String itemS = recipe.get(name).getAsString();
                    if(itemS != null && itemS.split(":").length == 2) {
                        itemS = itemS.split(":")[0];
                    }

                    if(!usagesMap.containsKey(itemS)) {
                        usagesMap.put(itemS, new HashSet<>());
                    }
                    usagesMap.get(itemS).add(internalName);
                }
            }

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
     * Calls search for each query, separated by |
     * eg. search(A|B) = search(A) + search(B)
     */
    public Set<String> search(String query, boolean multi) {
        if(multi) {
            Set<String> result = new HashSet<>();
            for(String query2 : query.split("\\|")) {
                result.addAll(search(query2));
            }
            return result;
        } else {
            return search(query);
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

    public void showRecipe(JsonObject item) {
        if(item.has("useneucraft") && item.get("useneucraft").getAsBoolean()) {
            displayGuiItemRecipe(item.get("internalname").getAsString(), "");
        } else if(item.has("clickcommand")) {
            String clickcommand = item.get("clickcommand").getAsString();

            if(clickcommand.equals("viewrecipe")) {
                neu.sendChatMessage(
                        "/" + clickcommand + " " +
                                item.get("internalname").getAsString().split(";")[0]);
                viewItemAttemptID = item.get("internalname").getAsString();
                viewItemAttemptTime = System.currentTimeMillis();
            } else if(clickcommand.equals("viewpotion")) {
                neu.sendChatMessage(
                        "/" + clickcommand + " " +
                                item.get("internalname").getAsString().split(";")[0].toLowerCase());
                viewItemAttemptID = item.get("internalname").getAsString();
                viewItemAttemptTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Takes an item stack and produces a JsonObject.
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

        if(lore.length > 0 && (lore[lore.length-1].contains("Click to view recipes!") ||
                lore[lore.length-1].contains("Click to view recipe!"))) {
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
                return ea.getString("id").replaceAll(":", "-");
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
        json.addProperty("clickcommand", "");
        json.addProperty("modver", NotEnoughUpdates.VERSION);

        try {
            writeJson(json, new File(itemsLocation, internalname+".json"));
        } catch (IOException e) {}

        loadItem(internalname);
    }

    /**
     * Constructs a GuiItemUsages from the recipe usage data (see #usagesMap) of a given item
     */
    public boolean displayGuiItemUsages(String internalName, String text) {
        List<ItemStack[]> craftMatrices = new ArrayList<>();
        List<JsonObject> results =  new ArrayList<>();

        if(!usagesMap.containsKey(internalName)) {
            return false;
        }

        for(String internalNameResult : usagesMap.get(internalName)) {
            JsonObject item = getItemInformation().get(internalNameResult);
            results.add(item);

            if(item != null && item.has("recipe")) {
                JsonObject recipe = item.get("recipe").getAsJsonObject();

                ItemStack[] craftMatrix = new ItemStack[9];

                String[] x = {"1","2","3"};
                String[] y = {"A","B","C"};
                for(int i=0; i<9; i++) {
                    String name = y[i/3]+x[i%3];
                    String itemS = recipe.get(name).getAsString();
                    int count = 1;
                    if(itemS != null && itemS.split(":").length == 2) {
                        count = Integer.valueOf(itemS.split(":")[1]);
                        itemS = itemS.split(":")[0];
                    }
                    JsonObject craft = getItemInformation().get(itemS);
                    if(craft != null) {
                        ItemStack stack = jsonToStack(craft);
                        stack.stackSize = count;
                        craftMatrix[i] = stack;
                    }
                }

                craftMatrices.add(craftMatrix);
            }
        }

        if(craftMatrices.size() > 0) {
            Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(
                    Minecraft.getMinecraft().thePlayer.openContainer.windowId));
            Minecraft.getMinecraft().displayGuiScreen(new GuiItemUsages(craftMatrices, results, text, this));
            return true;
        }
        return false;
    }

    /**
     * Constructs a GuiItemRecipe from the recipe data of a given item.
     */
    public boolean displayGuiItemRecipe(String internalName, String text) {
        JsonObject item = getItemInformation().get(internalName);
        if(item != null && item.has("recipe")) {
            JsonObject recipe = item.get("recipe").getAsJsonObject();

            ItemStack[] craftMatrix = new ItemStack[9];

            String[] x = {"1","2","3"};
            String[] y = {"A","B","C"};
            for(int i=0; i<9; i++) {
                String name = y[i/3]+x[i%3];
                String itemS = recipe.get(name).getAsString();
                int count = 1;
                if(itemS != null && itemS.split(":").length == 2) {
                    count = Integer.valueOf(itemS.split(":")[1]);
                    itemS = itemS.split(":")[0];
                }
                JsonObject craft = getItemInformation().get(itemS);
                if(craft != null) {
                    ItemStack stack = jsonToStack(craft);
                    stack.stackSize = count;
                    craftMatrix[i] = stack;
                }
            }

            Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(
                    Minecraft.getMinecraft().thePlayer.openContainer.windowId));
            Minecraft.getMinecraft().displayGuiScreen(new GuiItemRecipe(craftMatrix, item, text, this));
            return true;
        }
        return false;
    }

    /**
     * Will display guiItemRecipe if a player attempted to view the recipe to an item but they didn't have the recipe
     * unlocked. See NotEnoughUpdates#onGuiChat for where this method is called.
     */
    public boolean failViewItem(String text) {
        if(viewItemAttemptID != null && !viewItemAttemptID.isEmpty()) {
            if(System.currentTimeMillis() - viewItemAttemptTime < 500) {
                return displayGuiItemRecipe(viewItemAttemptID, text);
            }
        }
        return false;
    }

    /**
     * Downloads a web file, appending some HTML attributes that makes wikia give us the raw wiki syntax.
     */
    public File getWebFile(String url) {
        File f = new File(configLocation, "tmp/"+Base64.getEncoder().encodeToString(url.getBytes())+".html");
        if(f.exists()) {
            return f;
        }

        try {
            f.getParentFile().mkdirs();
            f.createNewFile();
            f.deleteOnExit();
        } catch (IOException e) {
            return null;
        }
        try (BufferedInputStream inStream = new BufferedInputStream(new URL(url+"?action=raw&templates=expand").openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(f)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = inStream.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return f;
    }


    /**
     * Modified from https://www.journaldev.com/960/java-unzip-file-example
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

    /**
     * Modified from https://www.journaldev.com/960/java-unzip-file-example
     */
    private static void unzip(InputStream src, File dest) {
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            ZipInputStream zis = new ZipInputStream(src);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                if(!ze.isDirectory()) {
                    String fileName = ze.getName();
                    File newFile = new File(dest, fileName);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * From here to the end of the file are various helper functions for creating and writing json files,
     * in particular json files representing skyblock item data.
     */
    public JsonObject createItemJson(String internalname, String itemid, String displayname, String[] lore,
                                     String crafttext, String infoType, String[] info,
                                     String clickcommand, int damage, NBTTagCompound nbttag) {
        return createItemJson(new JsonObject(), internalname, itemid, displayname, lore, crafttext, infoType, info, clickcommand, damage, nbttag);
    }

    public JsonObject createItemJson(JsonObject base, String internalname, String itemid, String displayname, String[] lore,
                                     String crafttext, String infoType, String[] info,
                                     String clickcommand, int damage, NBTTagCompound nbttag) {
        if(internalname == null || internalname.isEmpty()) {
            return null;
        }

        JsonObject json = gson.fromJson(gson.toJson(base, JsonObject.class), JsonObject.class);
        json.addProperty("internalname", internalname);
        json.addProperty("itemid", itemid);
        json.addProperty("displayname", displayname);
        json.addProperty("crafttext", crafttext);
        json.addProperty("clickcommand", clickcommand);
        json.addProperty("damage", damage);
        json.addProperty("nbttag", nbttag.toString());
        json.addProperty("modver", NotEnoughUpdates.VERSION);
        json.addProperty("infoType", infoType.toString());

        if(info != null && info.length > 0) {
            JsonArray jsoninfo = new JsonArray();
            for (String line : info) {
                jsoninfo.add(new JsonPrimitive(line));
            }
            json.add("info", jsoninfo);
        }

        JsonArray jsonlore = new JsonArray();
        for(String line : lore) {
            jsonlore.add(new JsonPrimitive(line));
        }
        json.add("lore", jsonlore);

        return json;
    }

    public boolean writeItemJson(String internalname, String itemid, String displayname, String[] lore, String crafttext,
                                 String infoType, String[] info, String clickcommand, int damage, NBTTagCompound nbttag) {
        return writeItemJson(new JsonObject(), internalname, itemid, displayname, lore, crafttext, infoType, info, clickcommand, damage, nbttag);
    }

    public boolean writeItemJson(JsonObject base, String internalname, String itemid, String displayname, String[] lore,
                                 String crafttext, String infoType, String[] info, String clickcommand, int damage, NBTTagCompound nbttag) {
        JsonObject json = createItemJson(base, internalname, itemid, displayname, lore, crafttext, infoType, info, clickcommand, damage, nbttag);
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

    public boolean uploadItemJson(String internalname, String itemid, String displayname, String[] lore, String crafttext, String infoType, String[] info,
                                 String clickcommand, int damage, NBTTagCompound nbttag) {
        JsonObject json = createItemJson(internalname, itemid, displayname, lore, crafttext, infoType, info, clickcommand, damage, nbttag);
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

    public void writeJson(JsonObject json, File file) throws IOException {
        file.createNewFile();

        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(json));
        }
    }

    public void writeJsonDefaultDir(JsonObject json, String filename) throws IOException {
        File file = new File(itemsLocation, filename);
        writeJson(json, file);
    }

    public TreeMap<String, JsonObject> getItemInformation() {
        return itemMap;
    }

    public ItemStack jsonToStack(JsonObject json) {
        if(itemstackCache.containsKey(json.get("internalname").getAsString())) {
            return itemstackCache.get(json.get("internalname").getAsString()).copy();
        }
        ItemStack stack = new ItemStack(Item.itemRegistry.getObject(
                new ResourceLocation(json.get("itemid").getAsString())));

        if(stack.getItem() == null) {
            stack = new ItemStack(Items.diamond, 1, 10); //Purple broken texture item
        } else {
            if(json.has("damage")) {
                stack.setItemDamage(json.get("damage").getAsInt());
            }

            if(json.has("nbttag")) {
                try {
                    NBTTagCompound tag = JsonToNBT.getTagFromJson(json.get("nbttag").getAsString());
                    stack.setTagCompound(tag);
                } catch(NBTException e) {
                }
            }

            if(json.has("lore")) {
                NBTTagCompound display = new NBTTagCompound();
                if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("display")) {
                    display = stack.getTagCompound().getCompoundTag("display");
                }
                NBTTagList lore = new NBTTagList();
                for(JsonElement line : json.get("lore").getAsJsonArray()) {
                    String lineStr = line.getAsString();
                    if(!lineStr.contains("Click to view recipes!") &&
                           !lineStr.contains("Click to view recipe!")) {
                        lore.appendTag(new NBTTagString(lineStr));
                    }
                }
                display.setTag("Lore", lore);
                NBTTagCompound tag = stack.getTagCompound() != null ? stack.getTagCompound() : new NBTTagCompound();
                tag.setTag("display", display);
                stack.setTagCompound(tag);
            }
        }

        itemstackCache.put(json.get("internalname").getAsString(), stack);
        return stack;
    }

}
