package io.github.moulberry.notenoughupdates;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

public class WikiaParserOLD {

    private static WikiModel wikiModel = new WikiModel("https://hypixel-skyblock.fandom.com/wiki/${image}",
            "https://hypixel-skyblock.fandom.com/wiki/${title}");
    private static PlainTextConverter ptc = new PlainTextConverter();
    private static Stack<Formatting> formattingStack = new Stack<>();
    private static Stack<String> textStack = new Stack<>();

    public static String parse(String raw) {
        if(textStack.isEmpty()) textStack.push(get());

        raw = raw.replaceAll("\u00A0", " ");
        String[] split = raw.split("</infobox>");
        String afterInfobox = split[split.length-1];

        out:
        for(int i=0; i<afterInfobox.length(); i++) {
            if(i >= afterInfobox.length()) break;
            String lineS = afterInfobox.substring(i);
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
        BOLD("'''", "'''", EnumChatFormatting.WHITE+EnumChatFormatting.BOLD.toString()),
        //GREEN("{{Green|","}}",EnumChatFormatting.GREEN.toString()),
        LIST("\n*", "\n",  "", true) {
            public String apply(String ctx) {
                return "\n \u2022 " + trimIgnoreColour(ctx) + "\n";
            }
            public boolean consume(boolean start) {
                return start;
            }
        },
        LIST2("\n#", "\n",  "", true) {
            public String apply(String ctx) {
                return "\n \u2022 " + trimIgnoreColour(ctx) + "\n";
            }
            public boolean consume(boolean start) {
                return start;
            }
        },
        SMALL_HEADER("\n;", "\n",  EnumChatFormatting.WHITE.toString(), true) {
            public String apply(String ctx) {
                return "\n" + trimIgnoreColour(ctx) + "\n";
            }
            public boolean consume(boolean start) {
                return start;
            }
        },
        TAB("\n:", "\n",  "", true) {
            public String apply(String ctx) {
                return "\n    " + trimIgnoreColour(ctx) + "\n";
            }
            public boolean consume(boolean start) {
                return start;
            }
        },
        TINY("====", "====",  EnumChatFormatting.WHITE+EnumChatFormatting.BOLD.toString()) {
            public String apply(String ctx) {
                return trimIgnoreColour(ctx);
            }
        },
        SMALL("===", "===",  EnumChatFormatting.GOLD.toString()) {
            public String apply(String ctx) {
                return "  " + trimIgnoreColour(ctx);
            }
        },
        MEDIUM("==", "==",  EnumChatFormatting.GOLD+EnumChatFormatting.BOLD.toString()) {
            public String apply(String ctx) {
                return "\r  " + trimIgnoreColour(ctx);
            }
        },
        CUSTOM("{{","}}","") {
            //❤,❁,☠,✦
            public String apply(String ctx) {
                if(noColour(ctx).trim().startsWith("Green|")) {
                    return EnumChatFormatting.GREEN+ctx.split("Green\\|")[1];
                } else if(noColour(ctx).trim().startsWith("Grey|")) {
                    return EnumChatFormatting.DARK_GRAY+ctx.split("Grey\\|")[1];
                } else if(noColour(ctx).trim().equalsIgnoreCase("Statname|Crit Damage")) {
                    return EnumChatFormatting.BLUE+"☠ Crit Damage";
                } else {
                    try {
                        return wikiModel.render(ptc, "{{"+ctx+"}}");
                    } catch(Exception e) {return "";}
                }
            }
        },
        LINK("[[", "]]",  "") {
            public String apply(String ctx) {
                if(noColour(ctx).toLowerCase().startsWith("file:")) return "";

                String[] split = ctx.split("#");
                ctx = split[split.length-1];

                split = ctx.split("\\|");
                ctx = split[split.length-1];
                return ctx;
            }
        },
        MATH("<math>", "</math>",  EnumChatFormatting.BOLD.toString()+EnumChatFormatting.WHITE) {
            public String apply(String ctx) {
                //ctx = trimIgnoreColour(ctx).replaceAll("\\times", "")
                return ctx;
            }
        },CLASS("{|", "|}",  "") {
            public String apply(String ctx) {
                try {
                    return wikiModel.render(ptc, "{|"+ctx+"|}");
                } catch(Exception e) {return "";}
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
        StringBuilder colour = new StringBuilder(EnumChatFormatting.RESET.toString() + EnumChatFormatting.GRAY);
        for(Formatting f : formattingStack) {
            colour.append(f.colourCode);
        }
        return colour.toString();
    }

}
