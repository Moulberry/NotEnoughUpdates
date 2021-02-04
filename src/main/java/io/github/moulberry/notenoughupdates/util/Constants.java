package io.github.moulberry.notenoughupdates.util;

import com.google.gson.JsonObject;

public class Constants {

    public static JsonObject BONUSES;
    public static JsonObject DISABLE;
    public static JsonObject ENCHANTS;
    public static JsonObject LEVELING;
    public static JsonObject MISC;
    public static JsonObject PETNUMS;
    public static JsonObject PETS;
    public static JsonObject PARENTS;
    public static JsonObject ESSENCECOSTS;
    public static JsonObject FAIRYSOULS;
    public static JsonObject REFORGESTONES;
    
    public static void reload() {
        BONUSES = Utils.getConstant("bonuses");
        DISABLE = Utils.getConstant("disable");
        ENCHANTS = Utils.getConstant("enchants");
        LEVELING = Utils.getConstant("leveling");
        MISC = Utils.getConstant("misc");
        PETNUMS = Utils.getConstant("petnums");
        PETS = Utils.getConstant("pets");
        PARENTS = Utils.getConstant("parents");
        ESSENCECOSTS = Utils.getConstant("essencecosts");
        FAIRYSOULS = Utils.getConstant("fairy_souls");
        REFORGESTONES = Utils.getConstant("reforgestones");
    }

    static {
        reload();
    }

}
