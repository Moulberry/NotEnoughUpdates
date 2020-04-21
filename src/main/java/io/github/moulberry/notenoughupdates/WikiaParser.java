package io.github.moulberry.notenoughupdates;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import net.minecraft.util.EnumChatFormatting;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Stack;

public class WikiaParser {

    private static Stack<Formatting> formattingStack = new Stack<>();
    private static Stack<String> textStack = new Stack<>();

    public static String parse(String raw) {
        if(textStack.isEmpty()) textStack.push(get());

        raw = raw.replaceAll("\u00A0", " ");

        out:
        for(int i=0; i<raw.length(); i++) {
            if(i >= raw.length()) break;
            String lineS = raw.substring(i);
            for(Formatting f : Formatting.values()) {
                if(lineS.startsWith(f.start)) {
                    if(tryPush(f, true)) {
                        writeToStack(get());
                        i += f.start.length()-1;
                        continue out;
                    }
                } else if(lineS.startsWith(f.end)) {
                    if(tryPush(f, false)) {
                        writeToStack(get());
                        i += f.end.length()-1;
                        continue out;
                    }
                }
            }
            writeToStack(String.valueOf(lineS.charAt(0)));
            if(lineS.charAt(0) == '\n') {
                writeToStack(get());
            }
        }

        String f = trimIgnoreColour(clear());
        return f.replaceAll("(\\n(\\u00A7.)*){2,}", "$1")
                .replaceAll("\r","\n");
    }
    private enum Formatting {
        LINK("[[", "]]",  "") {
            public String apply(String ctx) {
                String[] split = ctx.split("\\|");
                String base = split[split.length-1];
                return "[["+ctx+"]]";
            }
        },
        INFOBOX("<infobox", "</infobox>", "") {
            public String apply(String ctx) {
                return "";
            }
        },
        MATH("<math>", "</math>",  "") {
            public String apply(String ctx) {
                //ctx = trimIgnoreColour(ctx).replaceAll("\\times", "")
                return ctx;
            }
        },
        TEMPLATE("{{", "}}", "") {
            @Override
            public String apply(String ctx) {
                //String[] colours = new String[]{"red","green","yellow"};

                String[] split = ctx.split("\\|");
                String after = split[split.length-1].trim();
                /*if(ctx.trim().startsWith("ItemAbility")) {
                    return "<br><br><span style=\"color:orange\">Item Ability: " + after + "</span> ";
                } else {
                    for(String col : colours) {
                        if(ctx.trim().toLowerCase().startsWith(col)) {
                            return "<p style=\"color:"+col+"\">"+after+"</p>";
                        }
                    }
                }*/
                return "{{"+ctx+"}}";
            }
        };

        private boolean ambiguous;
        private String start;
        private String end;
        private String colourCode;
        private boolean autoend;

        Formatting(String start, String end, String colourCode) {
            this.start = start;
            this.end = end;
            this.ambiguous = start.equals(end);
            this.colourCode = colourCode;
        }

        Formatting(String start, String end, String colourCode, boolean autoend) {
            this.start = start;
            this.end = end;
            this.ambiguous = start.equals(end);
            this.colourCode = colourCode;
            this.autoend = autoend;
        }

        public String apply(String ctx) {
            return ctx;
        }
        public boolean consume(boolean start) {
            return true;
        }
    }

    private static String trimIgnoreColour(String str) {
        StringBuilder sb = new StringBuilder();

        while(!str.isEmpty()) {
            str = str.trim();
            if(str.startsWith("\u00a7")) {
                sb.append(str, 0, 2);
                str = str.substring(2);
            } else {
                sb.append(str);
                break;
            }
        }

        return sb.toString();
    }

    private static int strLenNoColor(String str) {
        return noColour(str).length();
    }

    private static String noColour(String str) {
        return str.replaceAll("(?i)\\u00A7.", "");
    }

    private static boolean tryPush(Formatting formatting, boolean start) {
        if(formatting.ambiguous) {
            if(!formattingStack.isEmpty() && formattingStack.peek() == formatting) {
                pop();
            } else {
                push(formatting);
            }
        } else {
            if(start) {
                push(formatting);
            } else {
                if(!formattingStack.isEmpty()) {
                    for(int i=1; i<=formattingStack.size(); i++) {
                        Formatting f = formattingStack.get(formattingStack.size()-i);
                        if(f == formatting) {
                            pop();
                            return formatting.consume(start);
                        } else if(f.autoend) {
                            pop();
                        } else {
                            return false;
                        }
                    }
                }
                return false;
            }
        }

        return formatting.consume(start);
    }

    private static String clear() {
        while(!formattingStack.empty()) {
            pop();
        }
        String ret = textStack.peek();

        formattingStack.clear();
        textStack.clear();
        textStack.push(get());

        return ret;
    }

    private static void pop() {
        Formatting f = formattingStack.pop();
        String applied = f.apply(textStack.pop());
        writeToStack(applied);
    }

    private static void push(Formatting formatting) {
        formattingStack.push(formatting);
        textStack.push("");
    }

    private static void writeToStack(String s) {
        textStack.push(textStack.pop() + s);
    }

    private static String get() {
        StringBuilder colour = new StringBuilder("");
        for(Formatting f : formattingStack) {
            colour.append(f.colourCode);
        }
        return colour.toString();
    }

}
