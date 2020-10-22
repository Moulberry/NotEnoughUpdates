package io.github.moulberry.notenoughupdates.infopanes;

import info.bliki.htmlcleaner.TagNode;
import info.bliki.wiki.filter.Encoder;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.ImageFormat;
import info.bliki.wiki.model.WikiModel;
import info.bliki.wiki.tags.HTMLBlockTag;
import info.bliki.wiki.tags.HTMLTag;
import info.bliki.wiki.tags.IgnoreTag;
import io.github.moulberry.notenoughupdates.AllowEmptyHTMLTag;
import io.github.moulberry.notenoughupdates.NEUManager;
import io.github.moulberry.notenoughupdates.NEUOverlay;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HTMLInfoPane extends TextInfoPane {

    private static WikiModel wikiModel;

    private final int ZOOM_FACTOR = 2;
    private final int IMAGE_WIDTH = 400;
    private final int EXT_WIDTH = 100;

    private ResourceLocation imageTexture = null;
    private BufferedImage imageTemp = null;
    private int imageHeight = 0;
    private int imageWidth = 0;

    /**
     * Creates a wiki model and sets the configuration to work with hypixel-skyblock wikia.
     */
    static {
        Configuration conf = new Configuration();
        conf.addTokenTag("img", new HTMLTag("img"));
        conf.addTokenTag("code", new HTMLTag("code"));
        conf.addTokenTag("span", new AllowEmptyHTMLTag("span"));
        conf.addTokenTag("table", new HTMLBlockTag("table", Configuration.SPECIAL_BLOCK_TAGS+"span|"));
        conf.addTokenTag("infobox", new IgnoreTag("infobox"));
        conf.addTokenTag("tabber", new IgnoreTag("tabber"));
        conf.addTokenTag("kbd", new HTMLTag("kbd"));
        wikiModel = new WikiModel(conf,"https://hypixel-skyblock.fandom.com/wiki/Special:Filepath/${image}",
                "https://hypixel-skyblock.fandom.com/wiki/${title}") {
            {
                TagNode.addAllowedAttribute("style");
                TagNode.addAllowedAttribute("src");
            }

            protected String createImageName(ImageFormat imageFormat) {
                String imageName = imageFormat.getFilename();
                if (imageName.endsWith(".svg")) {
                    imageName += ".png";
                }
                imageName = Encoder.encodeUrl(imageName);
                if (replaceColon()) {
                    imageName = imageName.replace(':', '/');
                }
                return imageName;
            }

            public void parseInternalImageLink(String imageNamespace, String rawImageLink) {
                rawImageLink = rawImageLink.replaceFirst("\\|x([0-9]+)px", "\\|$1x$1px");
                if(!rawImageLink.split("\\|")[0].toLowerCase().endsWith(".jpg")) {
                    super.parseInternalImageLink(imageNamespace, rawImageLink);
                }
            }
        };
    }

    /**
     * Takes a wiki url, uses NEUManager#getWebFile to download the web file and passed that in to #createFromWiki
     */
    public static HTMLInfoPane createFromWikiUrl(NEUOverlay overlay, NEUManager manager, String name,
                                                 String wikiUrl) {
        File f = manager.getWebFile(wikiUrl);
        if(f == null) {
            return new HTMLInfoPane(overlay, manager, "error", "error","Failed to load wiki url: "+ wikiUrl);
        };

        StringBuilder sb = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), StandardCharsets.UTF_8))) {
            String l;
            while((l = br.readLine()) != null){
                sb.append(l).append("\n");
            }
        } catch(IOException e) {
            return new HTMLInfoPane(overlay, manager, "error", "error","Failed to load wiki url: "+ wikiUrl);
        }
        return createFromWiki(overlay, manager, name, f.getName(), sb.toString());
    }

    /**
     * Takes raw wikia code and uses Bliki to generate HTML. Lot's of shennanigans to get it to render appropriately.
     * Honestly, I could have just downloaded the raw HTML of the wiki page and displayed that but I wanted
     * a more permanent solution that can be abstracted to work with arbitrary wiki codes (eg. moulberry.github.io/
     * files/neu_help.html).
     */
    public static HTMLInfoPane createFromWiki(NEUOverlay overlay, NEUManager manager, String name, String filename,
                                              String wiki) {
        String[] split = wiki.split("</infobox>");
        wiki = split[split.length - 1]; //Remove everything before infobox
        wiki = wiki.split("<span class=\"navbox-vde\">")[0]; //Remove navbox
        wiki = wiki.split("<table class=\"navbox mw-collapsible\"")[0];
        wiki = "__NOTOC__\n" + wiki; //Remove TOC
        try (PrintWriter out = new PrintWriter(new File(manager.configLocation, "debug/parsed.txt"))) {
            out.println(wiki);
        } catch (IOException e) {
        }
        String html;
        try {
            html = wikiModel.render(wiki);
        } catch(IOException e) {
            return new HTMLInfoPane(overlay, manager, "error", "error", "Could not render wiki.");
        }
        try (PrintWriter out = new PrintWriter(new File(manager.configLocation, "debug/html.txt"))) {
            out.println(html);
        } catch (IOException e) {
        }
        return new HTMLInfoPane(overlay, manager, name, filename, html);
    }

    private String spaceEscape(String str) {
        return str.replace(" ", "\\ ");
    }

    /**
     * Uses the wkhtmltoimage command-line tool to generate an image from the HTML code. This
     * generation is done asynchronously as sometimes it can take up to 10 seconds for more
     * complex webpages.
     */
    public HTMLInfoPane(NEUOverlay overlay, NEUManager manager, String name, String filename, String html) {
        super(overlay, manager, name, "");
        this.title = name;

        File cssFile = new File(manager.configLocation, "wikia.css");
        File wkHtmlToImage = new File(manager.configLocation, "wkhtmltox/bin/wkhtmltoimage");
        File input = new File(manager.configLocation, "tmp/input.html");
        String outputFileName = filename.replaceAll("(?i)\\u00A7.", "")
                .replaceAll("[^a-zA-Z0-9_\\-]", "_");
        File output = new File(manager.configLocation, "tmp/"+
                outputFileName+".png");
        File outputExt = new File(manager.configLocation, "tmp/"+
                outputFileName+"_ext.png");

        input.deleteOnExit();
        output.deleteOnExit();

        File tmp = new File(manager.configLocation, "tmp");
        if(!tmp.exists()) {
            tmp.mkdir();
        }

        if(output.exists()) {
            try {
                imageTemp = ImageIO.read(output);
                text = EnumChatFormatting.RED+"Creating dynamic texture.";
            } catch(IOException e) {
                e.printStackTrace();
                text = EnumChatFormatting.RED+"Failed to read image.";
                return;
            }
        } else {
            html = "<div id=\"mw-content-text\" lang=\"en\" dir=\"ltr\" class=\"mw-content-ltr mw-content-text\">"+html+"</div>";
            html = "<div id=\"WikiaArticle\" class=\"WikiaArticle\">"+html+"</div>";
            html = "<link rel=\"stylesheet\" href=\"file:///"+cssFile.getAbsolutePath()+"\">\n"+html;

            try(PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(input), StandardCharsets.UTF_8)), false)) {

                out.println(encodeNonAscii(html));
            } catch(IOException e) {}


            ExecutorService ste = Executors.newSingleThreadExecutor();
            try {
                text = EnumChatFormatting.GRAY+"Rendering webpage (" + name + EnumChatFormatting.RESET+
                        EnumChatFormatting.GRAY+"), please wait...";

                Runtime runtime = Runtime.getRuntime();

                String[] wkCommand = new String[]{ wkHtmlToImage.getAbsolutePath(), "--width", ""+IMAGE_WIDTH*ZOOM_FACTOR,
                        "--transparent", "--zoom", ""+ZOOM_FACTOR, input.getAbsolutePath(), output.getAbsolutePath()};
                Process p = runtime.exec(wkCommand);
                /*Process p = runtime.exec(spaceEscape(wkHtmlToImage.getAbsolutePath()) + " --width "+
                        IMAGE_WIDTH*ZOOM_FACTOR+" --transparent --zoom "+ZOOM_FACTOR + " " + spaceEscape(input.getAbsolutePath()) +
                        " " + spaceEscape(output.getAbsolutePath()));*/
                /*Process p = runtime.exec("\""+wkHtmlToImage.getAbsolutePath() + "\" --width "+
                        IMAGE_WIDTH*ZOOM_FACTOR+" --transparent --zoom "+ZOOM_FACTOR+" \"" + input.getAbsolutePath() +
                        "\" \"" + output.getAbsolutePath() + "\"");*/
                /*Process p2 = runtime.exec("\""+wkHtmlToImage.getAbsolutePath() + "\" --width "+
                        (IMAGE_WIDTH+EXT_WIDTH)*ZOOM_FACTOR+" --transparent --zoom "+ZOOM_FACTOR+" \"" + input.getAbsolutePath() +
                        "\" \"" + outputExt.getAbsolutePath() + "\"");*/
                ste.submit(() -> {
                    try {
                        if(p.waitFor(15, TimeUnit.SECONDS)) {
                            //if(p2.waitFor(5, TimeUnit.SECONDS)) {
                            if(overlay.getActiveInfoPane() != this) return;

                            try {
                                imageTemp = ImageIO.read(output);
                                /*BufferedImage imageReg = ImageIO.read(output);
                                BufferedImage imageExt = ImageIO.read(outputExt);
                                ArrayList<Integer[]> pixels = new ArrayList<>();

                                int skip = IMAGE_WIDTH/EXT_WIDTH+1;

                                for(int y=0; y<imageReg.getHeight(); y++) {
                                    pixels.add(new Integer[IMAGE_WIDTH*ZOOM_FACTOR]);
                                    if(new Color(imageReg.getRGB(IMAGE_WIDTH*ZOOM_FACTOR-1, y), true).getAlpha() == 0) {
                                        for(int x=0; x<IMAGE_WIDTH*ZOOM_FACTOR; x++) {
                                            pixels.get(y)[x] = imageReg.getRGB(x, y);
                                        }
                                    } else {
                                        for(int x=0; x<(IMAGE_WIDTH+EXT_WIDTH)*ZOOM_FACTOR; x++) {
                                            int x2 = x*IMAGE_WIDTH/(IMAGE_WIDTH+EXT_WIDTH);
                                            int y2 = y*(IMAGE_WIDTH+EXT_WIDTH)/IMAGE_WIDTH;
                                            pixels.get(y)[x2] = imageExt.getRGB(x, y2);
                                        }
                                    }
                                }
                                imageTemp = new BufferedImage(IMAGE_WIDTH*ZOOM_FACTOR, pixels.size(), imageReg.getType());
                                for(int y=0; y<pixels.size(); y++) {
                                    for(int x=0; x<IMAGE_WIDTH*ZOOM_FACTOR; x++) {
                                        int col = pixels.get(y)[x];
                                        imageTemp.setRGB(x, y, col);
                                    }
                                }*/
                                text = EnumChatFormatting.RED+"Creating dynamic texture.";
                            } catch(IOException e) {
                                e.printStackTrace();
                                text = EnumChatFormatting.RED+"Failed to read image.";
                                return;
                            }
                        } else {
                            if(overlay.getActiveInfoPane() != this) return;

                            text = EnumChatFormatting.RED+"Webpage render timed out (>15sec). Maybe it's too large?";
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch(IOException e) {
                e.printStackTrace();
                text = EnumChatFormatting.RED+"Failed to exec webpage renderer.";
            } finally {
                ste.shutdown();
            }
        }
    }

    /**
     * Renders a background, title and the image created in the ctor (if it has been generated).
     */
    @Override
    public void render(int width, int height, Color bg, Color fg, ScaledResolution scaledresolution, int mouseX, int mouseY) {
        if(imageTemp != null && imageTexture == null) {
            DynamicTexture tex = new DynamicTexture(imageTemp);
            imageTexture = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(
                    "notenoughupdates/informationPaneImage", tex);
            imageHeight = imageTemp.getHeight();
            imageWidth = imageTemp.getWidth();
        }
        if(imageTexture == null) {
            super.render(width, height, bg, fg, scaledresolution, mouseX, mouseY);
            return;
        }

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        int paneWidth = (int)(width/3*overlay.getWidthMult());
        int rightSide = (int)(width*overlay.getInfoPaneOffsetFactor());
        int leftSide = rightSide - paneWidth;

        int titleLen = fr.getStringWidth(title);
        fr.drawString(title, (leftSide+rightSide-titleLen)/2, overlay.getBoxPadding() + 5, Color.WHITE.getRGB());

        drawRect(leftSide+overlay.getBoxPadding()-5, overlay.getBoxPadding()-5, rightSide-overlay.getBoxPadding()+5,
                height-overlay.getBoxPadding()+5, bg.getRGB());

        int imageW = paneWidth - overlay.getBoxPadding()*2;
        float scaleF = IMAGE_WIDTH*ZOOM_FACTOR/(float)imageW;

        Minecraft.getMinecraft().getTextureManager().bindTexture(imageTexture);
        GlStateManager.color(1f, 1f, 1f, 1f);
        if(height-overlay.getBoxPadding()*3 < imageHeight/scaleF) {
            if(scrollHeight.getValue() > imageHeight/scaleF-height+overlay.getBoxPadding()*3) {
                scrollHeight.setValue((int)(imageHeight/scaleF-height+overlay.getBoxPadding()*3));
            }
            int yScroll = scrollHeight.getValue();

            float vMin = yScroll/(imageHeight/scaleF);
            float vMax = (yScroll+height-overlay.getBoxPadding()*3)/(imageHeight/scaleF);
            Utils.drawTexturedRect(leftSide+overlay.getBoxPadding(), overlay.getBoxPadding()*2, imageW,
                    height-overlay.getBoxPadding()*3,
                    0, 1, vMin, vMax);
        } else {
            scrollHeight.setValue(0);

            Utils.drawTexturedRect(leftSide+overlay.getBoxPadding(), overlay.getBoxPadding()*2, imageW,
                    (int)(imageHeight/scaleF));
        }
        GlStateManager.bindTexture(0);
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }

    @Override
    public void mouseInput(int width, int height, int mouseX, int mouseY, boolean mouseDown) {
        super.mouseInput(width, height, mouseX, mouseY, mouseDown);
    }

    //From https://stackoverflow.com/questions/1760766/how-to-convert-non-supported-character-to-html-entity-in-java
    public String encodeNonAscii(String c) {
        StringBuilder buf = new StringBuilder(c.length());
        CharsetEncoder enc = StandardCharsets.US_ASCII.newEncoder();
        for (int idx = 0; idx < c.length(); ++idx) {
            char ch = c.charAt(idx);
            if (enc.canEncode(ch))
                buf.append(ch);
            else {
                buf.append("&#");
                buf.append((int) ch);
                buf.append(';');
            }
        }
        return buf.toString();
    }
}
