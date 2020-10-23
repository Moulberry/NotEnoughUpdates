package io.github.moulberry.notenoughupdates.questing.requirements;

import com.google.common.base.Splitter;
import com.google.gson.*;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;

import java.util.List;

public class RequirementApi extends Requirement {

    private boolean valid = false;
    private String requirementLeft; //a.b.c
    private String op; //!=,<=,>=,=,<,>
    private String requirementRight; //true,float,etc.

    private String[] ops = new String[]{"!=","<=",">=","=","<",">"};

    public RequirementApi(String apiCheck, Requirement... preconditions) {
        super(preconditions);

        processApiCheck(apiCheck);
    }

    public void processApiCheck(String apiCheck) {
        for(String operator : ops) {
            if(apiCheck.contains(operator)) {
                String[] split = apiCheck.split(operator);
                requirementLeft = split[0];
                requirementRight = split[1];
                op = operator;
                valid = true;
                return;
            }
        }
    }

    private static Splitter PATH_SPLITTER = Splitter.on(".").omitEmptyStrings().limit(2);
    private static JsonElement getElement(JsonElement element, String path) {
        List<String> path_split = PATH_SPLITTER.splitToList(path);
        if(element instanceof JsonObject) {
            JsonElement e = element.getAsJsonObject().get(path_split.get(0));
            if(path_split.size() > 1) {
                return getElement(e, path_split.get(1));
            } else {
                return e;
            }
        } else {
            return element;
        }
    }

    private static boolean checkElementSatisfiesComparison(JsonElement element, String op, String value) {
        try {
            if(element instanceof JsonNull) {
                return value.equals("null");
            } else if(element instanceof JsonObject) {
                if(value.contains(".")) {
                    StringBuilder sb = new StringBuilder();
                    String[] split = value.split("\\.");
                    for(int i=0; i<split.length-1; i++) {
                        sb.append(split[i]).append(".");
                    }
                    return checkElementSatisfiesComparison(getElement(element, sb.toString()), op, split[split.length-1]);
                } else {
                    boolean containsValue = element.getAsJsonObject().has(value);
                    switch(op) {
                        case "=": return containsValue;
                        case "!=": return !containsValue;
                        default: return false;
                    }
                }
            } else if(element instanceof JsonArray) {
                for(int i=0; i<element.getAsJsonArray().size(); i++) {
                    JsonElement e = element.getAsJsonArray().get(i);
                    if(checkElementSatisfiesComparison(e, op, value)) return true;
                }
            } else if(element instanceof JsonPrimitive) {
                JsonPrimitive prim = element.getAsJsonPrimitive();
                if(!prim.isNumber()) {
                    return prim.getAsString().equals(value);
                } else {
                    switch(op) {
                        case "=": return prim.getAsString().equals(value);
                        case "!=": return !prim.getAsString().equals(value);
                        case ">": return prim.getAsDouble() > Double.parseDouble(value);
                        case "<": return prim.getAsDouble() < Double.parseDouble(value);
                        case "<=": return prim.getAsDouble() <= Double.parseDouble(value);
                        case ">=": return prim.getAsDouble() >= Double.parseDouble(value);
                        default: return false;
                    }
                }
            }
        } catch(Exception e) {}
        return false;
    }

    @Override
    public void updateRequirement() {
        if(valid) {
            /*JsonObject profile = NotEnoughUpdates.INSTANCE.manager.auctionManager.getPlayerInformation();
            if(profile != null) {
                System.out.println("-----------");
                JsonElement element = getElement(profile, requirementLeft);
                completed = checkElementSatisfiesComparison(element, op, requirementRight);
            }*/
        }
    }
}
