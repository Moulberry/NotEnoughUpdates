//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
package io.github.moulberry.notenoughupdates.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.StyleSheet;

public class HtmlImageGenerator {
    public JEditorPane editorPane = this.createJEditorPane();
    static final Dimension DEFAULT_SIZE = new Dimension(800, 800);

    public HtmlImageGenerator() {
    }

    public ComponentOrientation getOrientation() {
        return this.editorPane.getComponentOrientation();
    }

    public void setOrientation(ComponentOrientation orientation) {
        this.editorPane.setComponentOrientation(orientation);
    }

    public Dimension getSize() {
        return this.editorPane.getSize();
    }

    public void setSize(Dimension dimension) {
        this.editorPane.setSize(dimension);
    }

    public void loadUrl(URL url) {
        try {
            this.editorPane.setPage(url);
        } catch (IOException var3) {
            throw new RuntimeException(String.format("Exception while loading %s", url), var3);
        }
    }

    public void loadUrl(String url) {
        try {
            this.editorPane.setPage(url);
        } catch (IOException var3) {
            throw new RuntimeException(String.format("Exception while loading %s", url), var3);
        }
    }

    public void loadHtml(String html) {
        this.editorPane.setText(html);
        this.onDocumentLoad();
    }

    public void saveAsImage(String file) {
        this.saveAsImage(new File(file));
    }

    public void saveAsImage(File file) {
        BufferedImage img = this.getBufferedImage();

        try {
            ImageIO.write(img, "png", file);
        } catch (IOException var4) {
            throw new RuntimeException(String.format("Exception while saving '%s' image", file), var4);
        }
    }

    protected void onDocumentLoad() {
    }

    public Dimension getDefaultSize() {
        return DEFAULT_SIZE;
    }

    public BufferedImage getBufferedImage() {
        Dimension prefSize = this.editorPane.getPreferredSize();
        BufferedImage img = new BufferedImage(prefSize.width, this.editorPane.getPreferredSize().height, 2);
        Graphics graphics = img.getGraphics();
        this.editorPane.setSize(prefSize);
        this.editorPane.paint(graphics);
        return img;
    }

    public void addCss(String css) {
        HTMLEditorKit kit = (HTMLEditorKit) editorPane.getEditorKitForContentType("text/html");
        kit.getStyleSheet().addRule(css);
    }

    public void setScale(float factor) {
        editorPane.getDocument().putProperty("ZOOM_FACTOR", new Double(factor));
    }

    protected JEditorPane createJEditorPane() {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setSize(this.getDefaultSize());
        editorPane.setEditable(false);
        HTMLEditorKit kit = new LargeHTMLEditorKit();
        editorPane.setEditorKitForContentType("text/html", kit);
        editorPane.setBackground(new Color(0, true));
        editorPane.setContentType("text/html");
        editorPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("page")) {
                    HtmlImageGenerator.this.onDocumentLoad();
                }

            }
        });
        return editorPane;
    }
}
